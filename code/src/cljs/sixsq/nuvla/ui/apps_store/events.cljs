(ns sixsq.nuvla.ui.apps-store.events
  (:require [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [sixsq.nuvla.ui.apps-store.spec :as spec]
            [sixsq.nuvla.ui.apps.events :as apps-events]
            [sixsq.nuvla.ui.apps.spec :as apps-spec]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.main.spec :as main-spec]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.session.spec :as session-spec]
            [sixsq.nuvla.ui.utils.general :as general-utils]))

(reg-event-fx
  ::init
  (fn [{db :db}]
    {:db (merge db spec/defaults)
     :fx [[:dispatch [::apps-events/reset-version]]
          [:dispatch [::apps-events/module-not-found false]]]}))

(reg-event-db
  ::set-default-tab
  (fn [db [_ active-tab]]
    (update db ::spec/tab assoc :default-tab active-tab)))

(reg-event-db
  ::set-modules
  (fn [db [_ modules]]
    (assoc db ::spec/modules modules
              ::main-spec/loading? false)))

(def subtypes-apps-or-filter (general-utils/filter-eq-subtypes
                               [apps-utils/subtype-component
                                apps-utils/subtype-application
                                apps-utils/subtype-application-k8s
                                apps-utils/subtype-applications-sets]))

(reg-event-fx
  ::get-modules
  (fn [{{:keys [::session-spec/session] :as db} :db}
       [_ active-tab
        {:keys [order-by
                pagination-db-path
                external-filter
                additional-cb-fn
                replacing-cb-fn]
         :or   {additional-cb-fn #()}}]]
    (-> {:db (assoc db ::apps-spec/module nil)
         ::cimi-api-fx/search
         [:module
          (->> {:orderby (or order-by "created:desc")
                :filter  (general-utils/join-and
                           "parent-path!='apps-sets'"
                           external-filter
                           subtypes-apps-or-filter
                           (case active-tab
                             :appstore (general-utils/published-query-string)
                             :myapps (general-utils/owner-like-query-string
                                       (or (:active-claim session)
                                           (:user session)))
                             nil)
                           (full-text-search-plugin/filter-text
                             db [::spec/modules-search]))}
               (pagination-plugin/first-last-params db (or pagination-db-path
                                                           [(spec/page-keys->pagination-db-path active-tab)])))
          (if replacing-cb-fn
            #(replacing-cb-fn %)
            #(do
               (dispatch [::set-modules %])
               (additional-cb-fn %)))]})))

(reg-event-fx
  ::get-modules-summary
  (fn [{{:keys [::spec/full-text-search] :as db} :db} _]
    {:db (assoc db ::spec/modules nil)
     ::cimi-api-fx/search
     [:module
      {:first       0
       :last        0
       :orderby     "created:desc"
       :aggregation "terms:subtype"
       :filter      (general-utils/join-and
                      subtypes-apps-or-filter
                      (general-utils/fulltext-query-string
                        full-text-search))}
      #(dispatch [::set-modules %])]}))
