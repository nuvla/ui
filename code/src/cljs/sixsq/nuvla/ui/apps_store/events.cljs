(ns sixsq.nuvla.ui.apps-store.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.apps-store.spec :as spec]
    [sixsq.nuvla.ui.apps-store.utils :as utils]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]))


(reg-event-db
  ::set-modules
  (fn [db [_ modules]]
    (assoc db ::spec/modules modules)))


(defn search-modules-cofx
  [cofx full-text-search elements-per-page page]
  (assoc cofx ::cimi-api-fx/search
              [:module (utils/get-query-params full-text-search page elements-per-page)
               #(dispatch [::set-modules %])]))


(reg-event-fx
  ::get-modules
  (fn [{{:keys [::spec/full-text-search
                ::spec/page
                ::spec/elements-per-page] :as db} :db} _]
    (-> {:db (assoc db ::spec/modules nil)}
        (search-modules-cofx full-text-search elements-per-page page))))


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
