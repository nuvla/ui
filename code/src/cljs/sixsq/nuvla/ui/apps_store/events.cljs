(ns sixsq.nuvla.ui.apps-store.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.apps-store.spec :as spec]
    [sixsq.nuvla.ui.apps.events :as apps-events]
    [sixsq.nuvla.ui.apps.spec :as apps-spec]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.main.spec :as main-spec]
    [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
    [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
    [sixsq.nuvla.ui.plugins.tab :as tab-plugin]
    [sixsq.nuvla.ui.session.spec :as session-spec]
    [sixsq.nuvla.ui.utils.general :as general-utils]))

(reg-event-fx
  ::init
  (fn [{db :db}]
    {:db (merge db spec/defaults)
     :fx [[:dispatch [::apps-events/reset-version]]
          [:dispatch [::apps-events/module-not-found false]]
          [:dispatch [::get-modules]]]}))

(reg-event-db
  ::set-modules
  (fn [db [_ modules]]
    (assoc db ::spec/modules modules
              ::main-spec/loading? false)))

(reg-event-fx
  ::get-modules
  (fn [{{:keys [::session-spec/session
                ::spec/tab] :as db} :db}]
    (-> {:db (assoc db ::apps-spec/module nil)
         ::cimi-api-fx/search
         [:module
          (->> {:orderby "created:desc"
                :filter  (general-utils/join-and
                           (general-utils/join-or
                             "subtype='component'"
                             "subtype='application'"
                             "subtype='application_kubernetes'")
                           (case (::tab-plugin/active-tab tab)
                             :appstore (general-utils/published-query-string)
                             :myapps (general-utils/owner-like-query-string
                                       (or (:active-claim session)
                                           (:user session)))
                             nil)
                           (full-text-search-plugin/filter-text
                             db [::spec/modules-search]))}
               (pagination-plugin/first-last-params db [::spec/pagination]))
          #(dispatch [::set-modules %])]})))

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
                      (general-utils/join-or
                        "subtype='component'"
                        "subtype='application'"
                        "subtype='application_kubernetes'")
                      (general-utils/fulltext-query-string
                        full-text-search))}
      #(dispatch [::set-modules %])]}))
