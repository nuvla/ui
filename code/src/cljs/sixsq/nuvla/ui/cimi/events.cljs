(ns sixsq.nuvla.ui.cimi.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.cimi.spec :as spec]
    [sixsq.nuvla.ui.cimi.utils :as utils]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.response :as response]
    [taoensso.timbre :as log]))


(reg-event-db
  ::set-first
  (fn [db [_ first-value]]
    (update db ::spec/query-params merge {:first first-value})))


(reg-event-db
  ::set-last
  (fn [db [_ last-value]]
    (update db ::spec/query-params merge {:last last-value})))


(reg-event-db
  ::set-filter
  (fn [db [_ filter-value]]
    (update db ::spec/query-params merge {:filter filter-value})))


(reg-event-db
  ::set-orderby
  (fn [db [_ orderby-value]]
    (update db ::spec/query-params merge {:orderby orderby-value})))


(reg-event-db
  ::set-select
  (fn [db [_ select-value]]
    (update db ::spec/query-params merge {:select select-value})))

(reg-event-db
  ::set-query-params
  (fn [db [_ params]]
    (update db ::spec/query-params merge params)))

(reg-event-db
  ::show-add-modal
  (fn [db _]
    (assoc db ::spec/show-add-modal? true)))


(reg-event-db
  ::hide-add-modal
  (fn [db _]
    (assoc db ::spec/show-add-modal? false)))


(reg-event-db
  ::set-aggregation
  (fn [db [_ aggregation-value]]
    (update db ::spec/query-params merge {:aggregation aggregation-value})))


(reg-event-db
  ::set-collection-name
  (fn [{:keys [::spec/cloud-entry-point
               ::spec/collection-name] :as db} [_ coll-name]]
    (if (or (empty? coll-name) (-> cloud-entry-point
                                   :collection-key
                                   (get coll-name)))
      (cond-> db
              (not= coll-name collection-name) (assoc ::spec/collection-name coll-name
                                                      ::spec/selected-rows #{}
                                                      ::spec/collection nil))
      (let [msg-map {:header  (cond-> (str "invalid resource type: " coll-name))
                     :content (str "The resource type '" coll-name "' is not valid. "
                                   "Please choose another resource type.")
                     :type    :error}]

        ;; only send error message when the cloud-entry-point is actually set, otherwise
        ;; we don't yet know if this is a valid resource or not
        (when cloud-entry-point
          (dispatch [::messages-events/add msg-map]))
        db))))


(reg-event-db
  ::set-selected-fields
  (fn [db [_ fields]]
    (assoc db ::spec/selected-fields (sort (vec fields)))))


(reg-event-db
  ::remove-field
  (fn [{:keys [::spec/selected-fields] :as db} [_ field]]
    (->> selected-fields
         (remove #{field})
         vec
         sort
         (assoc db ::spec/selected-fields))))


(reg-event-fx
  ::get-results
  (fn [{{:keys [::spec/collection-name
                ::spec/cloud-entry-point
                ::spec/query-params] :as db} :db} _]
    (let [resource-type (-> cloud-entry-point
                            :collection-key
                            (get collection-name))]
      {:db                  (assoc db ::spec/loading? true
                                      ::spec/aggregations nil
                                      ::spec/collection nil)
       ::cimi-api-fx/search [resource-type
                             (general-utils/prepare-params query-params)
                             #(dispatch [::set-results resource-type %])]})))


(reg-event-fx
  ::create-resource
  (fn [{{:keys [::spec/collection-name
                ::spec/cloud-entry-point]} :db} [_ data]]
    (let [resource-type (-> cloud-entry-point
                            :collection-key
                            (get collection-name))
          on-success    #(let [{:keys [status message resource-id]} (response/parse %)]
                           (dispatch [::get-results])
                           (dispatch [::messages-events/add
                                      {:header  (cond-> (str "added " resource-id)
                                                        status (str " (" status ")"))
                                       :content message
                                       :type    :success}]))]
      {::cimi-api-fx/add [resource-type data on-success]})))



(reg-event-db
  ::set-results
  (fn [db [_ resource-type listing]]
    (let [error?       (instance? js/Error listing)
          entries      (get listing :resources [])
          aggregations (get listing :aggregations nil)
          fields       (general-utils/merge-keys (conj entries {:id "id"}))]
      (when error?
        (dispatch [::messages-events/add
                   (let [{:keys [status message]} (response/parse-ex-info listing)]
                     {:header  (cond-> (str "failure getting " (name resource-type))
                                       status (str " (" status ")"))
                      :content message
                      :type    :error})]))
      (assoc db ::spec/aggregations aggregations
                ::spec/collection (when-not error? listing)
                ::spec/loading? false
                ::spec/available-fields fields))))


(reg-event-db
  ::set-cloud-entry-point
  (fn [db [_ {:keys [base-uri] :as cep}]]
    (let [href-map (utils/collection-href-map cep)
          key-map  (utils/collection-key-map cep)]
      (-> db
          (assoc ::spec/cloud-entry-point {:base-uri        base-uri
                                           :collection-href href-map
                                           :collection-key  key-map})
          (assoc ::spec/collections-templates-cache (utils/collections-template-map href-map))))))


(reg-event-fx
  ::get-cloud-entry-point
  (fn [_ _]
    {::cimi-api-fx/cloud-entry-point [#(dispatch [::set-cloud-entry-point %])]}))


(reg-event-fx
  ::get-templates
  (fn [{{:keys [::spec/cloud-entry-point
                ::spec/collections-templates-cache]} :db}
       [_ template-href]]
    (let [resource-type (-> cloud-entry-point
                            :collection-key
                            (get (name template-href)))]
      {::cimi-api-fx/search [resource-type {:orderby "id"}
                             #(dispatch [::set-templates template-href (:resources %)])]})))


(reg-event-db
  ::set-templates
  (fn [db [_ template-href templates]]
    (if (instance? js/Error templates)
      (dispatch [::messages-events/add
                 (let [{:keys [status message]} (response/parse-ex-info templates)]
                   {:header  (cond-> (str "failure getting " (name template-href))
                                     status (str " (" status ")"))
                    :content message
                    :type    :error})])
      (assoc-in db [::spec/collections-templates-cache template-href]
                (->> templates
                     (map (juxt :id identity))
                     (into {}))))))


(reg-event-db
  ::select-row
  (fn [{:keys [::spec/selected-rows] :as db} [_ checked? id]]
    (let [f (if checked? disj conj)]
      (assoc db ::spec/selected-rows (f selected-rows id)))))


(reg-event-db
  ::select-all-row
  (fn [{:keys [::spec/collection] :as db} [_ checked?]]
    (let [selected-rows (if checked?
                          (->> collection
                               :resources
                               (map :id)
                               set)
                          #{})]
      (assoc db ::spec/selected-rows selected-rows))))
