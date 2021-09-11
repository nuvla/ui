(ns sixsq.nuvla.ui.apps-store.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.apps-store.spec :as spec]
    [sixsq.nuvla.ui.apps-store.utils :as utils]
    [sixsq.nuvla.ui.apps.spec :as apps-spec]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.main.spec :as main-spec]
    [sixsq.nuvla.ui.session.spec :as session-spec]))


(reg-event-db
  ::set-modules
  (fn [db [_ modules]]
    (assoc db ::spec/modules modules
              ::main-spec/loading? false)))


;; Published modules

(reg-event-db
  ::set-published-modules
  (fn [db [_ modules]]
    (assoc db ::spec/published-modules modules
              ::main-spec/loading? false)))


(defn published-modules-cofx
  [cofx full-text-search elements-per-page page]
  (assoc cofx ::cimi-api-fx/search
              [:module (utils/get-published-modules-query-params full-text-search page elements-per-page)
               #(dispatch [::set-published-modules %])]))


(reg-event-fx
  ::get-published-modules
  (fn [{{:keys [::spec/full-text-search-published
                ::spec/page
                ::spec/elements-per-page] :as db} :db} _]
    (published-modules-cofx db full-text-search-published elements-per-page page)))


(reg-event-fx
  ::set-full-text-search-published
  (fn [{db :db} [_ full-text-search]]
    (let [new-page 1]
      {:db       (assoc db ::spec/full-text-search-published full-text-search
                           ::spec/page new-page)
       :dispatch [::get-published-modules]})))


(reg-event-fx
  ::set-page-published-modules
  (fn [{{:keys [::spec/full-text-search
                ::spec/elements-per-page] :as db} :db} [_ page]]
    (-> {:db (assoc db ::spec/page page)}
        (published-modules-cofx full-text-search elements-per-page page))))


;; My modules

(reg-event-db
  ::set-my-modules
  (fn [db [_ modules]]
    (assoc db ::spec/my-modules modules
              ::main-spec/loading? false)))


(defn my-modules-cofx
  [cofx owner full-text-search elements-per-page page]
  (assoc cofx ::cimi-api-fx/search
              [:module (utils/get-my-modules-query-params owner full-text-search page elements-per-page)
               #(dispatch [::set-my-modules %])]))


(reg-event-fx
  ::get-my-modules
  (fn [{{:keys [::session-spec/session
                ::spec/full-text-search-my
                ::spec/page
                ::spec/elements-per-page] :as db} :db} _]
    (let [user-id (:user session)]
      (my-modules-cofx {:db db} user-id full-text-search-my elements-per-page page))))


(reg-event-fx
  ::set-full-text-search-my
  (fn [{db :db} [_ full-text-search]]
    (let [new-page 1]
      {:db       (assoc db ::spec/full-text-search-my full-text-search
                           ::spec/page new-page)
       :dispatch [::get-my-modules]})))


(reg-event-fx
  ::set-page-my-modules
  (fn [{{:keys [::spec/full-text-search
                ::session-spec/session
                ::spec/elements-per-page] :as db} :db} [_ page]]
    (let [user-id (:user session)]
      (-> {:db (assoc db ::spec/page page)}
          (my-modules-cofx user-id full-text-search elements-per-page page)))))


;; All modules

(defn search-modules-cofx
  [cofx full-text-search elements-per-page page]
  (assoc cofx ::cimi-api-fx/search
              [:module (utils/get-query-params full-text-search page elements-per-page)
               #(dispatch [::set-modules %])]))


(reg-event-fx
  ::get-modules
  (fn [{{:keys [::spec/full-text-search-all-apps
                ::spec/page
                ::spec/elements-per-page] :as db} :db} _]
    (-> {:db (assoc db ::apps-spec/module nil)}
        (search-modules-cofx full-text-search-all-apps elements-per-page page))))


(defn summary-modules-cofx
  [cofx full-text-search]
  (assoc cofx ::cimi-api-fx/search
              [:module (utils/get-query-summary-params full-text-search)
               #(dispatch [::set-modules %])]))


(reg-event-fx
  ::get-modules-summary
  (fn [{{:keys [::spec/full-text-search] :as db} :db} _]
    (-> {:db (assoc db ::spec/modules nil)}
        (summary-modules-cofx full-text-search))))


(reg-event-fx
  ::set-full-text-search-all-apps
  (fn [{db :db} [_ full-text-search]]
    (let [new-page 1]
      {:db       (assoc db ::spec/full-text-search-all-apps full-text-search
                           ::spec/page new-page)
       :dispatch [::get-modules]})))


(reg-event-fx
  ::set-page-all-modules
  (fn [{{:keys [::spec/full-text-search
                ::spec/elements-per-page] :as db} :db} [_ page]]
    (-> {:db (assoc db ::spec/page page)}
        (search-modules-cofx full-text-search elements-per-page page))))


(reg-event-db
  ::set-active-tab-index
  (fn [db [_ active-tab-index]]
    (assoc db ::spec/active-tab-index active-tab-index)))


(reg-event-fx
  ::set-state-selector
  (fn [{db :db} [_ state-selector]]
    (dispatch [::get-modules])
    {:db (assoc db ::spec/state-selector state-selector
                   ::spec/page 1)}))


(reg-event-db
  ::reset-page
  (fn [db _]
    (assoc db ::spec/page 1)))
