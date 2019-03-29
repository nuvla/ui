(ns sixsq.nuvla.ui.apps.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.apps.effects :as apps-fx]
    [sixsq.nuvla.ui.apps.spec :as spec]
    [sixsq.nuvla.ui.apps.utils :as utils]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.cimi-detail.events :as cimi-detail-events]
    [sixsq.nuvla.ui.client.spec :as client-spec]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.main.spec :as main-spec]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.response :as response]))


(reg-event-db
  ::set-version-warning
  (fn [db [_]]
    (assoc db ::spec/version-warning? true)))


(reg-event-db
  ::clear-version-warning
  (fn [db [_]]
    (assoc db ::spec/version-warning? false)))


(reg-event-db
  ::set-module
  (fn [db [_ module]]
    (assoc db ::spec/completed? true
              ::spec/module-path (:path module)
              ::spec/module (if (nil? module) {} module))))


(reg-event-db
  ::open-add-modal
  (fn [db _]
    (assoc db ::spec/add-modal-visible? true)))


(reg-event-db
  ::close-add-modal
  (fn [db _]
    (assoc db ::spec/add-modal-visible? false
              ::spec/active-tab :project
              ::spec/add-data nil)))


(reg-event-db
  ::clear-module
  (fn [db [_]]
    (assoc db ::spec/module {})))


(reg-event-fx
  ::get-module
  (fn [{{:keys [::client-spec/client ::main-spec/nav-path] :as db} :db} [_ version]]
    (when client
      (let [path (utils/nav-path->module-path nav-path)]
        {:db                  (assoc db ::spec/completed? false
                                        ::spec/module-path nil
                                        ::spec/module nil)
         ::apps-fx/get-module [client path version #(dispatch [::set-module %])]}))))


(reg-event-db
  ::update-add-data
  (fn [{:keys [::spec/add-data] :as db} [_ path value]]
    (assoc-in db (concat [::spec/add-data] path) value)))


(reg-event-db
  ::set-active-tab
  (fn [db [_ active-tab]]
    (assoc db ::spec/active-tab active-tab)))


(reg-event-db
  ::page-changed?
  (fn [db [_ has-change?]]
    (assoc db ::spec/page-changed? has-change?)))


(reg-event-db
  ::is-new?
  (fn [db [_ is-new?]]
    (assoc db ::spec/is-new? is-new?)))


(reg-event-db
  ::name
  (fn [db [_ name]]
    (assoc-in db [::spec/module :name] name)))


(reg-event-db
  ::description
  (fn [db [_ description]]
    (assoc-in db [::spec/module :description] description)))


(reg-event-db
  ::type
  (fn [db [_ type]]
    (assoc-in db [::spec/module :type] (if (nil? type) nil (str/upper-case type)))))


(reg-event-db
  ::path
  (fn [db [_ path]]
    (assoc-in db [::spec/module :path] path)))


(reg-event-db
  ::parent
  (fn [db [_ parent]]
    (assoc-in db [::spec/module :parent-path] parent)))


(reg-event-db
  ::docker-image
  (fn [db [_ docker-image]]
    (assoc-in db [::spec/module :content :image] docker-image)))


(reg-event-db
  ::commit-message
  (fn [db [_ msg]]
    (assoc db ::spec/commit-message msg)))


(reg-event-db
  ::open-save-modal
  (fn [db _]
    (assoc db ::spec/save-modal-visible? true)))


(reg-event-db
  ::close-save-modal
  (fn [db _]
    (assoc db ::spec/save-modal-visible? false)))


(reg-event-db
  ::save-logo-url
  (fn [db [_ logo-url]]
    (dispatch [::page-changed? true])
    (-> db
        (assoc-in [::spec/module :logo-url] logo-url)
        (assoc-in [::spec/logo-url-modal-visible?] false))))


(reg-event-db
  ::open-logo-url-modal
  (fn [db _]
    (assoc db ::spec/logo-url-modal-visible? true)))


(reg-event-db
  ::close-logo-url-modal
  (fn [db _]
    (assoc db ::spec/logo-url-modal-visible? false)))

(reg-event-fx
  ::edit-module
  (fn [{{:keys [::spec/module ::client-spec/client] :as db} :db :as cofx} [_ commit-map]]
    (let [id (:id module)
          sanitized-module (utils/sanitize-module module commit-map)]
      (if (nil? id)
        {:db               db
         ::cimi-api-fx/add [client "module" sanitized-module
                            #(if (instance? js/Error %)
                               (let [{:keys [status message]} (response/parse-ex-info %)]
                                 (dispatch [::messages-events/add
                                            {:header  (cond-> (str "error editing " id)
                                                              status (str " (" status ")"))
                                             :content message
                                             :type    :error}]))
                               (do (dispatch [::cimi-detail-events/get (:id %)])
                                   (dispatch [::set-module sanitized-module])
                                   (dispatch [::page-changed? false])
                                   (dispatch [::history-events/navigate (str "apps/" (:path sanitized-module))])
                                   ))]}
        {:db                db
         ::cimi-api-fx/edit [client id sanitized-module
                             #(if (instance? js/Error %)
                                (let [{:keys [status message]} (response/parse-ex-info %)]
                                  (dispatch [::messages-events/add
                                             {:header  (cond-> (str "error editing " id)
                                                               status (str " (" status ")"))
                                              :content message
                                              :type    :error}]))
                                (do (dispatch [::cimi-detail-events/get (:id %)])
                                    (dispatch [::get-module])
                                    (dispatch [::page-changed? false])
                                    ))]}))))
