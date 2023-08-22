(ns sixsq.nuvla.ui.routing.events
  (:require [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [reitit.frontend :refer [match-by-path]]
            [reitit.frontend.controllers :as rfc]
            [sixsq.nuvla.ui.main.spec :as main-spec]
            [sixsq.nuvla.ui.routing.effects :as fx]
            [sixsq.nuvla.ui.routing.utils :as utils]
            [sixsq.nuvla.ui.utils.helpers :refer [update-values]]
            [taoensso.timbre :as log]))

(def after-nav-cb-key ::after-nav-cb)

(reg-event-fx
  ::navigate-back
  (fn []
    {::fx/navigate-back! []}))

(reg-event-fx
  ::push-state-by-path
  (fn [{{:keys [current-route
                router]} :db} [_ new-path]]
    (let [new-match (dissoc (match-by-path router new-path) :controllers)]
      (when-not (= new-match (dissoc current-route :controllers))
        {::fx/push-state new-path}))))

(reg-event-fx
  ::navigated
  (fn [{db :db} [_ {:keys [path query-params] :as new-match}]]
    (let [old-match                  (:current-route db)
          controllers                (rfc/apply-controllers (:controllers old-match) new-match)
          new-match-with-controllers (assoc new-match :controllers controllers)
          view-changed?              (not= (:view (:data old-match))
                                           (:view (:data new-match-with-controllers)))]
      {:db                   (-> db
                                 (assoc :current-route new-match-with-controllers
                                        ::main-spec/nav-path (utils/split-path-alias path)
                                        ::main-spec/nav-query-params query-params)
                                 (dissoc after-nav-cb-key))
       :fx                   [(when view-changed?
                                [:dispatch [:sixsq.nuvla.ui.main.events/bulk-actions-interval-after-navigation]])
                              [::fx/after-nav-cb (after-nav-cb-key db)]]
       ::fx/set-window-title [(utils/strip-base-path (:path new-match))]})))

(reg-event-db
  ::reset-ignore-changes-protection
  (fn [db]
    (assoc db ::ignore-changes-protection false)))

(reg-event-fx
  ;; In case of normal anchor tag click, we do not fire ::history-events/navigate
  ;; but let reitit/browser handle the .pushState to the history stack,
  ;; which then fires `on-navigate` after URL changed already.
  ;; That's why we test here for changes-protection? (which we also do in ::history-events/navigate)
  ;; and revert by navigating back if changes-protection? is true.
  ::navigated-protected
  (fn [{{:keys [::main-spec/changes-protection?
                ::ignore-changes-protection] :as db} :db} [_ new-match]]
    (let [event  {:fx [[:dispatch [::navigated new-match]]
                       [:dispatch [::reset-ignore-changes-protection]]]}
          revert {:fx [[:dispatch [::navigate-back]]
                       [:dispatch [::reset-ignore-changes-protection]]]}]
      (if (and changes-protection? (not ignore-changes-protection))
        {:db (assoc db
               ::main-spec/ignore-changes-modal event
               ::main-spec/do-not-ignore-changes-modal revert

               ;; In case of not confirming ignore-chagnes-modal,
               ;; `revert` event navigates back, again triggering this
               ;; protected naviation event: ::ingore-changes-protection temporarily
               ;; disables the protection.
               ::ignore-changes-protection true)}
        (merge {:db (assoc db ::ignore-changes-protection false)}
               event)))))

(reg-event-fx
  ::navigate
  (fn [{{:keys [::main-spec/changes-protection?] :as db} :db}
       [_ navigate-to path-params query-params
        {change-event            :change-event
         ignore-chng-protection? :ignore-chng-protection?}]]
    (let [nav-effect {:db (cond-> (assoc db ::ignore-changes-protection ignore-chng-protection?)
                                  change-event (assoc after-nav-cb-key #(dispatch change-event)))
                      :fx [[:dispatch [::push-state-by-path
                                       (if (string? navigate-to)
                                         (utils/add-base-path navigate-to)
                                         (utils/name->href navigate-to path-params query-params))]]]}]
      (if (and changes-protection? (not ignore-chng-protection?))
        {:db (assoc db ::main-spec/ignore-changes-modal nav-effect
                       ::ignore-changes-protection ignore-chng-protection?)}
        (do
          (log/info "triggering navigate effect " (str {:relative-url navigate-to}))
          nav-effect)))))

(reg-event-fx
  ::navigate-partial
  (fn [{{:keys [current-route]} :db} [_ {:keys [change-event
                                                ignore-chng-protection?]
                                         :as   new-partial-route-data}]]
    (let [{:keys [route-name
                  path-params
                  query-params]} (utils/new-route-data current-route new-partial-route-data)]
      {:fx [[:dispatch [::navigate route-name path-params query-params
                        {:change-event            change-event
                         :ignore-chng-protection? ignore-chng-protection?}]]]})))

(defn keyword->string [k] (if (keyword? k) (name k) k))

(reg-event-fx
  ::change-query-param
  (fn [{{:keys [current-route] :as db} :db} [_ new-partial-route-data]]
    (let [{:keys [route-name
                  path-params
                  query-params]} (utils/new-route-data current-route new-partial-route-data)
          push-state?            (:push-state? new-partial-route-data)]
      {:db (assoc db ::ignore-changes-protection true)
       :fx [[(if push-state? ::fx/push-state ::fx/replace-state)
             (utils/name->href
               route-name
               path-params
               (update-values query-params #(some-> %
                                                    keyword->string
                                                    js/encodeURIComponent)))]]})))

(reg-event-fx
  ::store-in-query-param
  (fn [{{:keys [current-route] :as db} :db} [_ {:keys [db-path query-key value after-nav-cb]
                                                :as   route-instructions}]]
    (let [query-key-calculated   (or query-key (utils/db-path->query-param-key db-path))
          new-partial-route-data (if (seq value)
                                   {:partial-query-params
                                    {query-key-calculated value}}
                                   {:query-params (dissoc (:query-params current-route) query-key-calculated)})]
      {:db (assoc db after-nav-cb-key after-nav-cb)
       :fx [[:dispatch
             [::change-query-param (merge route-instructions new-partial-route-data)]]]})))
