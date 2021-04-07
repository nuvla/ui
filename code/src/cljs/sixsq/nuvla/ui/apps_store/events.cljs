(ns sixsq.nuvla.ui.apps-store.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.apps-store.spec :as spec]
    [sixsq.nuvla.ui.apps-store.utils :as utils]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.session.spec :as session-spec]
    [taoensso.timbre :as log]))


(reg-event-db
  ::set-modules
  (fn [db [_ modules]]
    (assoc db ::spec/modules modules)))


(defn getting-started-modules-cofx
  [cofx elements-per-page page]
  (assoc cofx ::cimi-api-fx/search
              [:module (utils/get-modules-by-tag-query-params "getting-started" page elements-per-page)
               #(dispatch [::set-modules %])]))


(reg-event-fx
  ::get-getting-started-modules
  (fn [{{:keys [::session-spec/session
                ::spec/page
                ::spec/elements-per-page] :as db} :db} _]
    (-> {:db (assoc db ::spec/modules nil)}
        (getting-started-modules-cofx elements-per-page page))))


;; My modules

(reg-event-db
  ::set-my-modules
  (fn [db [_ modules]]
    (assoc db ::spec/my-modules modules)))


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
                ::spec/elements-per-page] :as db} :db} [_ full-text-search]]
    (let [user-id (:user session)
          search (or full-text-search full-text-search-my)]
      (-> {:db (assoc db ::spec/my-modules nil)}
          (my-modules-cofx user-id search elements-per-page page)))))


(reg-event-fx
  ::set-full-text-search-my
  (fn [{{:keys [::spec/elements-per-page] :as db} :db} [_ full-text-search]]
    (let [new-page 1]
      {:db (assoc db ::spec/full-text-search-my full-text-search
                        ::spec/page new-page)
       :dispatch [::get-my-modules]})))


;(defn search-my-modules-cofx
;  [cofx full-text-search elements-per-page page]
;  (assoc cofx ::cimi-api-fx/search
;              [:module (utils/get-query-params full-text-search page elements-per-page)
;               #(dispatch [::set-my-modules %])]))


(defn search-modules-cofx
  [cofx full-text-search elements-per-page page]
  (assoc cofx ::cimi-api-fx/search
              [:module (utils/get-query-params full-text-search page elements-per-page)
               #(dispatch [::set-modules %])]))


(reg-event-fx
  ::get-modules
  (fn [{{:keys [::spec/full-text-search
                ::spec/page
                ::spec/elements-per-page] :as db} :db} [_ full-text]]
    (let [search (or full-text full-text-search )]
      (-> {:db (assoc db ::spec/modules nil)}
          (search-modules-cofx search elements-per-page page)))))


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
  ::set-full-text-search
  (fn [{{:keys [::spec/elements-per-page] :as db} :db} [_ full-text-search]]
    (let [new-page 1]
      (-> {:db (assoc db ::spec/full-text-search full-text-search
                         ::spec/page new-page)}
          (search-modules-cofx full-text-search elements-per-page new-page)))))


(reg-event-fx
  ::set-page
  (fn [{{:keys [::spec/full-text-search
                ::spec/page
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
