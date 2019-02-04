(ns sixsq.slipstream.webui.dashboard.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.slipstream.webui.cimi-api.effects :as cimi-api-fx]
    [sixsq.slipstream.webui.client.spec :as client-spec]
    [sixsq.slipstream.webui.dashboard.effects :as dashboard-fx]
    [sixsq.slipstream.webui.dashboard.spec :as dashboard-spec]
    [sixsq.slipstream.webui.main.effects :as main-fx]
    [sixsq.slipstream.webui.utils.general :as general-utils]))


(def stat-info {:count                          {:label "VMs", :order 0}
                :cardinality:connector/href     {:label "clouds", :order 1}
                :sum:serviceOffer/resource:vcpu {:label "vCPU", :order 2}
                :sum:serviceOffer/resource:ram  {:label "RAM (MB)", :order 3}
                :sum:serviceOffer/resource:disk {:label "Disk (GB)", :order 4}})


(reg-event-db
  ::set-statistics
  (fn [db [_ {:keys [_ count aggregations]}]]
    (let [stats (->> aggregations
                     (merge {:count {:value count}})
                     (map (fn [[k v]] (merge v (stat-info k)))))]
      (assoc db ::dashboard-spec/loading? false
                ::dashboard-spec/statistics stats))))


(reg-event-fx
  ::get-statistics
  (fn [{{:keys [::client-spec/client] :as db} :db} _]
    (let [collection-name "virtualMachines"
          params {:$first       1
                  :$last        0
                  :$filter      "state='Running'"
                  :$orderby     nil
                  :$aggregation (str/join "," ["sum:serviceOffer/resource:disk"
                                               "sum:serviceOffer/resource:ram"
                                               "sum:serviceOffer/resource:vcpu"
                                               "cardinality:connector/href"])
                  :$select      nil}]
      {:db                  (assoc db ::dashboard-spec/loading? true)
       ::cimi-api-fx/search [client
                             collection-name
                             (general-utils/prepare-params params)
                             #(dispatch [::set-statistics %])]})))

(defn fetch-vms-cofx [{:keys [::client-spec/client
                              ::dashboard-spec/filtered-cloud
                              ::dashboard-spec/page
                              ::dashboard-spec/records-displayed] :as db}]
  (let [last (* page records-displayed)
        first (inc (- last records-displayed))]
    {::dashboard-fx/get-virtual-machines [client {:$filter (when filtered-cloud
                                                             (str "connector/href=\"connector/" filtered-cloud "\""))
                                                  :$order  "created:desc"
                                                  :$first  first
                                                  :$last   last}]}))

(defn fetch-deployments-cofx [{:keys [::client-spec/client
                                      ::dashboard-spec/filtered-cloud
                                      ::dashboard-spec/page
                                      ::dashboard-spec/records-displayed
                                      ::dashboard-spec/active-deployments-only] :as db}]
  (let [offset (* (dec page) records-displayed)]
    {::dashboard-fx/get-deployments [client {:offset     offset
                                             :limit      records-displayed
                                             :cloud      (or filtered-cloud "")
                                             :activeOnly (if active-deployments-only 1 0)}]}))

(defn fetch-tab-records-cofx [{:keys [::dashboard-spec/selected-tab] :as db}]
  (case selected-tab
    "deployments" (fetch-deployments-cofx db)
    "virtual-machines" (fetch-vms-cofx db)))

(reg-event-fx
  ::fetch-tab-records
  (fn [{:keys [db]} _]
    (fetch-tab-records-cofx db)))

(reg-event-fx
  ::set-selected-tab
  (fn [{:keys [db]} [_ tab-index]]
    {:db                       (-> db
                                   (assoc ::dashboard-spec/selected-tab tab-index)
                                   (assoc ::dashboard-spec/page 1)
                                   (assoc ::dashboard-spec/loading-tab? true))
     ::main-fx/action-interval [{:action    :start
                                 :id        :dashboard-tab
                                 :frequency 10000
                                 :event     [::fetch-tab-records]}]}))

(reg-event-fx
  ::set-filtered-cloud
  (fn [{:keys [db]} [_ cloud]]
    {:db (-> db
             (assoc ::dashboard-spec/filtered-cloud (if (= cloud "All Clouds") nil cloud))
             (assoc ::dashboard-spec/loading-tab? true))}))

(reg-event-db
  ::set-virtual-machines
  (fn [{:keys [::dashboard-spec/records-displayed] :as db} [_ virtual-machines]]
    (let [total-pages (general-utils/total-pages (:count virtual-machines) records-displayed)
          new-db (-> db
                     (assoc ::dashboard-spec/virtual-machines virtual-machines)
                     (assoc ::dashboard-spec/total-pages total-pages)
                     (assoc ::dashboard-spec/loading-tab? false))]
      (cond-> new-db
              (> (:page db) total-pages) (assoc ::dashboard-spec/page total-pages)))))

(reg-event-db
  ::set-deployments
  (fn [{:keys [::dashboard-spec/records-displayed] :as db} [_ deployments]]
    (let [deployments-count (get-in deployments [:runs :totalCount] 0)
          total-pages (general-utils/total-pages deployments-count records-displayed)
          new-db (-> db
                     (assoc ::dashboard-spec/deployments deployments)
                     (assoc ::dashboard-spec/total-pages total-pages)
                     (assoc ::dashboard-spec/loading-tab? false))]
      (cond-> new-db
              (> (:page db) total-pages) (assoc ::dashboard-spec/page total-pages)))))

(reg-event-fx
  ::set-page
  (fn [{{:keys [::dashboard-spec/selected-tab] :as db} :db} [_ page]]
    (let [db (-> db
                 (assoc ::dashboard-spec/page page)
                 (assoc ::dashboard-spec/loading-tab? true))]
      (merge (fetch-tab-records-cofx db) {:db db}))))

(reg-event-fx
  ::active-deployments-only
  (fn [{{:keys [::dashboard-spec/selected-tab] :as db} :db} [_ v]]
    (let [db (-> db
                 (assoc ::dashboard-spec/active-deployments-only v)
                 (assoc ::dashboard-spec/loading-tab? true))]
      (merge (fetch-tab-records-cofx db) {:db db}))))

;; FIXME: Generalize this so that it can be used elsewhere.
(reg-event-db
  ::delete-deployment-modal
  (fn [db [_ deployment]]
    (assoc db ::dashboard-spec/delete-deployment-modal deployment)))

(reg-event-db
  ::set-error-message-deployment
  (fn [db [_ error]]
    (assoc db ::dashboard-spec/error-message-deployment error)))

(reg-event-db
  ::clear-error-message-deployment
  (fn [db _]
    (assoc db ::dashboard-spec/error-message-deployment nil)))

(reg-event-fx
  ::deleted-deployment
  (fn [{:keys [db]} [_ uuid]]
    {:db                                   (update db ::dashboard-spec/deleted-deployments conj uuid)
     ::dashboard-fx/pop-deleted-deployment [uuid]}))

(reg-event-fx
  ::delete-deployment
  (fn [{{:keys [::client-spec/client] :as db} :db} [_ uuid]]
    {:db                              (-> db
                                          (assoc ::dashboard-spec/delete-deployment-modal nil)
                                          (update ::dashboard-spec/deleted-deployments conj uuid))
     ::dashboard-fx/delete-deployment [client uuid]}))

(reg-event-db
  ::pop-deleted-deployment
  (fn [db [_ uuid]]
    (update db ::dashboard-spec/deleted-deployments disj uuid)))
