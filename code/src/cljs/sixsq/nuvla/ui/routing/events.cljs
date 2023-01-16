(ns sixsq.nuvla.ui.routing.events
  (:require [re-frame.core :refer [reg-event-fx]]
            [reitit.frontend.controllers :as rfc]
            [sixsq.nuvla.ui.main.spec :as main-spec]
            [sixsq.nuvla.ui.routing.effects :as fx]
            [sixsq.nuvla.ui.routing.utils :as utils :refer [name->href]]
            [taoensso.timbre :as log]))

(reg-event-fx
  ::navigate-back
  (fn []
    {::fx/navigate-back! []}))

(reg-event-fx
  ::push-state-by-path
  (fn [_ [_ new-path]]
    {::fx/push-state new-path}))

(reg-event-fx
  ::navigated
  (fn [{db :db} [_ {:keys [path query-params] :as new-match}]]
    (let [old-match                  (:current-route db)
          controllers                (rfc/apply-controllers (:controllers old-match) new-match)
          new-match-with-controllers (assoc new-match :controllers controllers)]
      {:db                   (-> db (assoc :current-route new-match-with-controllers
                                           ::main-spec/nav-path (utils/split-path-alias path)
                                           ::main-spec/nav-query-params query-params))
       :fx                   [[:dispatch [:sixsq.nuvla.ui.main.events/bulk-actions-interval-after-navigation]]]
       ::fx/set-window-title [(utils/strip-base-path (:path new-match))]})))

(reg-event-fx
  ;; In case of normal anchor tag click, we do not fire ::history-events/navigate
  ;; but let reitit/browser handle the .pushState to the history stack,
  ;; which then fires `on-navigate` after URL changed already.
  ;; That's why we test here for changes-protection? (which we also do in ::history-events/navigate)
  ;; and revert by navigating back if changes-protection? is true.
  ::navigated-protected
  (fn [{{:keys [::main-spec/changes-protection?
                ::ignore-changes-protection] :as db} :db} [_ new-match]]
    (let [new-db (assoc db ::ignore-changes-protection false)
          event  {:fx [[:dispatch [::navigated new-match]]]
                  :db new-db}
          revert {:fx [[:dispatch [::navigate-back]]]
                  :db new-db}]
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
  (fn [{{:keys [::main-spec/changes-protection?] :as db} :db} [_ navigate-to path-params query-params]]
    (let [nav-effect {:fx [[:dispatch [::push-state-by-path (if (string? navigate-to)
                                                                (utils/add-base-path navigate-to)
                                                                (name->href navigate-to path-params query-params))]]]}]
      (if changes-protection?
        {:db (assoc db ::main-spec/ignore-changes-modal nav-effect)}
        (do
          (log/info "triggering navigate effect " (str {:relative-url navigate-to}))
          nav-effect)))))