(ns sixsq.nuvla.ui.session.events
  (:require
    [ajax.core :as ajax]
    [clojure.string :as str]
    [day8.re-frame.http-fx]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.cimi.events :as cimi-events]
    [sixsq.nuvla.ui.config :as config]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.intercom.events :as intercom-events]
    [sixsq.nuvla.ui.main.spec :as main-spec]
    [sixsq.nuvla.ui.session.effects :as fx]
    [sixsq.nuvla.ui.session.spec :as spec]
    [sixsq.nuvla.ui.utils.response :as response]))


(reg-event-fx
  ::initialize
  (fn [{db :db}]
    {:db                   (assoc db ::spec/loading-session? true
                                     ::spec/error-message nil)
     ::cimi-api-fx/session [(fn [session]
                              (dispatch [::set-session session])
                              (when session
                                #_(dispatch [:sixsq.nuvla.ui.main.events/check-bootstrap-message])
                                (dispatch [:sixsq.nuvla.ui.main.events/notifications-polling])
                                (dispatch [:sixsq.nuvla.ui.profile.events/search-existing-customer])
                                (dispatch [::search-groups])
                                (dispatch [::get-peers])))]}))


(reg-event-fx
  ::set-session
  (fn [{{:keys [::spec/session
                ::main-spec/nav-path
                ::main-spec/pages] :as db} :db} [_ session-arg]]
    (let [query-str (.-search (.-location js/window))
          redirect  (when (and (nil? session-arg)
                               (->> nav-path first (get pages) :protected?))
                      (str (str/join "/" nav-path)
                           (when-not (str/blank? query-str)
                             (js/encodeURIComponent query-str))))
          navigate  (str "sign-in" (when redirect
                                     (str "?redirect=" redirect)))]
      (cond-> {:db (assoc db ::spec/session session-arg
                             ::spec/session-loading? false)}
              session-arg (assoc ::fx/automatic-logout-at-session-expiry
                                 [session-arg])

              redirect (update :fx conj [:dispatch [::history-events/navigate navigate]])
              ;; force refresh templates collection cache when not the same user (different session)
              (not= session session-arg) (assoc :fx
                                                [[:dispatch [::cimi-events/get-cloud-entry-point]]
                                                 [:dispatch [:sixsq.nuvla.ui.main.events/force-refresh-content]]
                                                 [:dispatch [::get-session-groups]]])))))


(reg-event-fx
  ::logout
  (fn [{:keys [db]} _]
    {:db                  (assoc db :sixsq.nuvla.ui.main.spec/bootstrap-message nil)
     ::cimi-api-fx/logout [#(do (dispatch [::set-session nil])
                                (dispatch [::intercom-events/clear-events])
                                (dispatch [::history-events/navigate "sign-in"]))]}))


(reg-event-db
  ::clear-loading
  (fn [db _]
    (assoc db ::spec/loading? false)))


(reg-event-db
  ::set-error-message
  (fn [db [_ error-message]]
    (assoc db ::spec/error-message error-message)))


(reg-event-fx
  ::set-success-message
  (fn [{db :db} [_ success-message]]
    {:db       (assoc db ::spec/success-message success-message)
     :dispatch [::clear-loading]}))


(reg-event-fx
  ::code-validation-2fa-failed
  (fn []
    {:fx [[:dispatch [::set-error-message :code-validation-failed]]]}))


(reg-event-fx
  ::validate-2fa-activation
  (fn [{{:keys [::spec/callback-2fa]} :db} [_ token]]
    (when-not (str/blank? token)
      {:http-xhrio {:method          :put
                    :uri             callback-2fa
                    :format          (ajax/json-request-format)
                    :params          {:token token}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::initialize]
                    :on-failure      [::code-validation-2fa-failed]}})))


(reg-event-db
  ::set-callback-2fa
  (fn [db [_ {callback-url :location :as _reponse}]]
    (assoc db ::spec/callback-2fa callback-url)))


(reg-event-fx
  ::submit
  (fn [{{:keys [::spec/server-redirect-uri] :as db} :db} [_ form-id form-data opts]]
    (let [{success-msg  :success-msg,
           callback-add :callback-add,
           redirect-url :redirect-url
           navigate-to  :navigate-to
           :or          {redirect-url server-redirect-uri}} opts

          on-success    (or callback-add
                            #(do
                               (dispatch [::clear-loading])
                               (if (= (:status %1) 201)
                                 (do
                                   (dispatch [::initialize])
                                   (when success-msg
                                     (dispatch [::set-success-message success-msg]))
                                   (when navigate-to
                                     (dispatch [::history-events/navigate navigate-to])))
                                 (do
                                   (dispatch [::set-callback-2fa %1])
                                   (dispatch [::history-events/navigate "sign-in-token"])))))

          on-error      #(let [{:keys [message]} (response/parse-ex-info %)]
                           (dispatch [::clear-loading])
                           (dispatch [::set-error-message (or message %)]))

          template      {:template (assoc form-data :href form-id
                                                    :redirect-url redirect-url)}
          collection-kw (cond
                          (str/starts-with? form-id "session-template/") :session
                          (str/starts-with? form-id "user-template/") :user)]
      {:db               (assoc db ::spec/loading? true
                                   ::spec/success-message nil
                                   ::spec/error-message nil)
       ::cimi-api-fx/add [collection-kw template on-success :on-error on-error]})))


(reg-event-fx
  ::set-password-success
  (fn [_ [_ success-message]]
    {:dispatch-n [[::clear-loading]
                  [::initialize]
                  [::set-success-message success-message]
                  [::history-events/navigate "sign-in"]]}))


(reg-event-fx
  ::set-password-error
  (fn [_ [_ response]]
    {:dispatch-n [[::clear-loading]
                  [::set-error-message (get-in response [:response :message])]]}))


(reg-event-fx
  ::reset-password
  (fn [{db :db} [_ form]]
    {:db         (assoc db ::spec/loading? true
                           ::spec/success-message nil
                           ::spec/error-message nil)
     :http-xhrio {:method          :put
                  :uri             (str @cimi-api-fx/NUVLA_URL "/api/hook/reset-password")
                  :format          (ajax/json-request-format)
                  :params          (assoc form :redirect-url
                                               (str @config/path-prefix "/set-password"))
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::set-password-success :reset-password-sucess-inst]
                  :on-failure      [::set-password-error]}}))


(reg-event-fx
  ::set-password
  (fn [{db :db} [_ callback form]]
    {:db         (assoc db ::spec/loading? true
                           ::spec/success-message nil
                           ::spec/error-message nil)
     :http-xhrio {:method          :put
                  :uri             callback
                  :format          (ajax/json-request-format)
                  :params          form
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::set-password-success :set-password-success]
                  :on-failure      [::set-password-error]}}))


(reg-event-db
  ::set-session-groups
  (fn [db [_ groups-hierarchies]]
    (assoc db ::spec/groups-hierarchies groups-hierarchies)))


(reg-event-fx
  ::get-session-groups
  (fn [{{:keys [::spec/session]} :db}]
    (let [on-success #(dispatch [::set-session-groups %])]
      {::cimi-api-fx/operation [(:id session) "get-groups" on-success]})))


(reg-event-fx
  ::switch-group
  (fn [{{:keys [::spec/session]} :db} [_ claim extended]]
    (let [claim    (if (= (:identifier session) claim) (:user session) claim)
          data     {:claim    claim
                    :extended extended}
          callback #(dispatch [::initialize])]
      {::cimi-api-fx/operation [(:id session) "switch-group" callback :on-error callback :data data]})))


(reg-event-db
  ::set-peers
  (fn [db [_ response]]
    (assoc db ::spec/peers
              (->> response
                   (map (fn [[k v]] [(str (namespace k) "/" (name k)) v]))
                   (sort-by (juxt second first))
                   (into {})))))


(reg-event-fx
  ::get-peers
  (fn [{{:keys [::spec/session]} :db}]
    (let [on-success #(dispatch [::set-peers %])
          on-error   #(let [{:keys [status message]} (response/parse-ex-info %)]
                        (js/console.error "Get peers failed (" status "): " message))]
      {::cimi-api-fx/operation [(:id session) "get-peers" on-success :on-error on-error]})))


(reg-event-db
  ::set-groups
  (fn [db [_ {:keys [resources]}]]
    (when resources
      (assoc db ::spec/groups resources))))

;; FIXME Consider replacement of this call by get-groups
(reg-event-fx
  ::search-groups
  (fn []
    {::cimi-api-fx/search [:group {:select  "id, name, acl, users, description"
                                   :last    10000
                                   :orderby "name:asc,id:asc"}
                           #(dispatch [::set-groups %])]
     :fx                  [[:dispatch [::get-session-groups]]]}))
