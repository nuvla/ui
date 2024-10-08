(ns sixsq.nuvla.ui.pages.apps.apps-store.events
  (:require [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.common-components.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.common-components.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.main.spec :as main-spec]
            [sixsq.nuvla.ui.pages.apps.apps-store.spec :as spec]
            [sixsq.nuvla.ui.pages.apps.events :as apps-events]
            [sixsq.nuvla.ui.pages.apps.spec :as apps-spec]
            [sixsq.nuvla.ui.pages.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.session.spec :as session-spec]
            [sixsq.nuvla.ui.session.utils :as session-utils]
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

(def not-project-filter (str "subtype!='" apps-utils/subtype-project "'"))

(reg-event-fx
  ::get-modules
  (fn [{{:keys [::session-spec/session] :as db} :db}
       [_ active-tab
        {:keys [order-by
                pagination-db-path
                external-filter
                additional-cb-fn]
         :or   {additional-cb-fn #()}}]]
    (-> {:db (assoc db ::apps-spec/module nil)
         ::cimi-api-fx/search
         [:module
          (->> {:orderby (or order-by "created:desc")
                :filter  (general-utils/join-and
                           (str "parent-path!='" spec/virtual-apps-set-parent-path "'")
                           external-filter
                           not-project-filter
                           (case active-tab
                             :appstore (general-utils/published-query-string)
                             :myapps (general-utils/owner-like-query-string
                                       (session-utils/get-active-claim session))
                             nil)
                           (full-text-search-plugin/filter-text
                             db [::spec/modules-search]))}
               (pagination-plugin/first-last-params db (or pagination-db-path
                                                           [(spec/page-keys->pagination-db-path active-tab)])))
          #(do
             (dispatch [::set-modules %])
             (additional-cb-fn %))]})))

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
                      not-project-filter
                      (general-utils/fulltext-query-string
                        full-text-search))}
      #(dispatch [::set-modules %])]}))
