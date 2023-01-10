(ns sixsq.nuvla.ui.data.events
  (:require [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.data-set.spec :as data-set-spec]
            [sixsq.nuvla.ui.data-set.utils :as data-set-utils]
            [sixsq.nuvla.ui.data.spec :as spec]
            [sixsq.nuvla.ui.deployment-dialog.events :as dialog-events]
            [sixsq.nuvla.ui.deployment-dialog.spec :as dialog-spec]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.messages.events :as messages-events]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.plugins.tab :as tab-plugin]
            [sixsq.nuvla.ui.utils.general :as general-utils]))

(reg-event-fx
  ::refresh
  (fn []
    {:fx [[:dispatch [::get-data-sets]]]}))

(reg-event-db
  ::reset-add-data-set-form
  (fn [db]
    (assoc db ::spec/add-data-set-form {})))

(reg-event-fx
  ::set-modal-open?
  (fn [{db :db} [_ modal-open?]]
    (cond-> {:db (assoc db ::spec/modal-open? modal-open?)}
            (false? modal-open?)
            (assoc :fx [[:dispatch [::reset-add-data-set-form]]]))))

(reg-event-db
  ::set-add-data-set-form
  (fn [db [_ k v]]
    (assoc-in db [::spec/add-data-set-form k] v)))

(reg-event-db
  ::set-dataset-stats
  (fn [db [_ data-set-id response]]
    (let [doc-count   (get-in response [:aggregations :value_count:id :value])
          total-bytes (get-in response [:aggregations :sum:bytes :value])]
      (-> db
          (assoc-in [::spec/counts data-set-id] doc-count)
          (assoc-in [::spec/sizes data-set-id] total-bytes)))))

(reg-event-fx
  ::fetch-dataset-stats
  (fn [_ [_ data-set-id filter]]
    {::cimi-api-fx/search [:data-record
                           {:filter      filter
                            :select      "id"
                            :aggregation "value_count:id, sum:bytes"
                            :last        0}
                           #(dispatch [::set-dataset-stats data-set-id %])]}))

(reg-event-fx
  ::fetch-all-datasets-stats
  (fn [{{:keys [::spec/data-sets
                ::data-set-spec/geo-operation
                ::data-set-spec/map-selection
                ::data-set-spec/time-period-filter] :as db} :db}]
    (let [data-sets-vals (vals data-sets)]
      {:fx (map (fn [data-set]
                  [:dispatch [::fetch-dataset-stats
                              (:id data-set)
                              (general-utils/join-and
                                time-period-filter
                                (full-text-search-plugin/filter-text
                                  db [::spec/data-search])
                                (data-set-utils/data-record-geometry-filter
                                  geo-operation map-selection)
                                (:data-record-filter data-set))]])
                data-sets-vals)})))

(reg-event-db
  ::set-data-records
  (fn [db [_ data-records]]
    (assoc db ::spec/data-records data-records)))

(reg-event-db
  ::set-applications
  (fn [db [_ applications]]
    (assoc db ::spec/applications (:resources applications)
              ::spec/loading-applications? false)))

(reg-event-fx
  ::set-selected-application-id
  (fn [{db :db} [_ application-id]]
    (dispatch [::dialog-events/create-deployment application-id :data true])
    {:db (assoc db ::spec/selected-application-id application-id)}))

(reg-event-fx
  ::search-application
  (fn [{db :db} [_ query-application]]
    {:db                  (assoc db
                            ::spec/applications nil
                            ::spec/loading-applications? true)
     ::cimi-api-fx/search [:module {:filter query-application}
                           #(dispatch [::set-applications %])]}))

(reg-event-fx
  ::data-sets-to-process
  (fn [{db :db} [_ data-sets]]
    (let [query-application (apply general-utils/join-and
                                   (conj (map :module-filter data-sets)
                                         "subtype!='project'"))
          query-objects     (apply general-utils/join-or
                                   (map :data-record-filter data-sets))]
      {:db (assoc db ::spec/content-type-filter query-objects)
       :fx [[:dispatch [::search-application query-application]]]})))

(reg-event-fx
  ::data-records-to-process
  (fn [{db :db} [_ data-records]]
    (let [query-application (apply general-utils/join-and
                                   (conj
                                     (->> data-records
                                          (map :content-type)
                                          distinct
                                          (map #(when % (str "data-accept-content-types='" % "'"))))
                                     "subtype!='project'"))]
      {:db (assoc db ::spec/content-type-filter nil)
       :fx [[:dispatch [::search-application query-application]]]})))

(reg-event-fx
  ::open-application-select-modal
  (fn [{{:keys [::spec/selected-data-set-ids
                ::data-set-spec/selected-data-record-ids
                ::spec/tab] :as db} :db}]
    (let [tab-data-set-selected? (= (::tab-plugin/active-tab tab) :data-sets)
          {:keys [resource
                  selected-data
                  dispatch-event]} (if tab-data-set-selected?
                                     {:resource       :data-set
                                      :selected-data  selected-data-set-ids
                                      :dispatch-event ::data-sets-to-process}
                                     {:resource       :data-record
                                      :selected-data  selected-data-record-ids
                                      :dispatch-event ::data-records-to-process})
          filter-str             (apply general-utils/join-or
                                        (map #(str "id='" % "'") selected-data))]
      {:db                  (assoc db ::spec/application-select-visible? true
                                      ::spec/selected-application-id nil)
       ::cimi-api-fx/search [resource {:filter filter-str
                                       :last   10000}
                             #(dispatch [dispatch-event (:resources %)])]})))

(reg-event-fx
  ::close-application-select-modal
  (fn [{db :db} _]
    {:db (assoc db ::spec/applications nil
                   ::spec/application-select-visible? false)}))

(reg-event-fx
  ::delete-deployment
  (fn [{{:keys [::dialog-spec/deployment]} :db} _]
    {::cimi-api-fx/delete [(:id deployment)
                           #(dispatch [::dialog-events/set-deployment nil])]}))

(reg-event-fx
  ::set-data-sets
  (fn [{db :db} [_ {:keys [resources count]}]]
    (let [data-sets (into {} (map (juxt :id identity) resources))]
      {:db (assoc db ::spec/total count
                     ::spec/counts nil
                     ::spec/sizes nil
                     ::spec/data-sets data-sets)
       :fx [[:dispatch [::fetch-all-datasets-stats]]]})))

(reg-event-fx
  ::get-data-sets
  (fn [{db :db}]
    {::cimi-api-fx/search
     [:data-set (->>
                  {:orderby "created:desc"
                   :filter  (full-text-search-plugin/filter-text
                              db [::spec/data-search])}
                  (pagination-plugin/first-last-params
                    db [::spec/pagination]))
      #(do (dispatch [::set-data-sets %])
           (dispatch [::main-events/set-loading? false]))]}))

(reg-event-db
  ::toggle-data-set-id
  (fn [{:keys [::spec/selected-data-set-ids] :as db} [_ id]]
    (let [f (if (get selected-data-set-ids id) disj conj)]
      (assoc db ::spec/selected-data-set-ids (f selected-data-set-ids id)))))

(reg-event-db
  ::tab-changed
  (fn [{{:keys [::tab-plugin/active-tab]} ::spec/tab :as db}]
    (cond-> db
            (= active-tab :data-records)
            (assoc ::data-set-spec/data-set-id nil
                   ::data-set-spec/data-record-filter nil
                   ::data-set-spec/map-selection nil
                   ::data-set-spec/data-set nil))))

(reg-event-fx
  ::add-data-set
  (fn [{{:keys [::spec/add-data-set-form]} :db}]
    {::cimi-api-fx/add
     [:data-set add-data-set-form
      #(do
         (dispatch [::set-modal-open? false])
         (let [{:keys [status message resource-id]} %]
           (dispatch [::messages-events/add
                      {:header  (cond-> (str "added "
                                             (or (:name add-data-set-form)
                                                 resource-id))
                                        status (str " (" status ")"))
                       :content message
                       :type    :success}]))
         (dispatch [::refresh])
         (dispatch [::reset-add-data-set-form]))]}))
