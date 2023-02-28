(ns sixsq.nuvla.ui.resource-log.events
  (:require [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.resource-log.spec :as spec]))

(reg-event-fx
  ::reset
  (fn [{{:keys [::spec/id] :as db} :db}]
    {:db                  (merge db spec/defaults)
     ::cimi-api-fx/delete [id #()]}))

(reg-event-fx
  ::delete
  (fn [{{:keys [::spec/id] :as db} :db} _]
    (cond-> {:db (assoc db ::spec/id nil
                           ::spec/resource-log nil)}
            id (assoc ::cimi-api-fx/delete [id #()]
                      :dispatch [::set-play? false]))))

(reg-event-fx
  ::set-since
  (fn [{{:keys [::spec/id] :as db} :db} [_ since]]
    {:db (assoc db ::spec/since since)
     :fx [(when id [:dispatch [::delete]])]}))

(reg-event-fx
  ::set-play?
  (fn [{{:keys [::spec/id] :as db} :db} [_ play?]]
    (cond-> {:db       (assoc db ::spec/play? play?)
             :dispatch (if play?
                         (if id
                           [::fetch]
                           [::create-log])
                         [::main-events/action-interval-delete
                          {:id :get-resource-log}])}
            (and play? id)
            (assoc :dispatch-later
                   [{:ms       5000
                     :dispatch [::main-events/action-interval-start
                                {:id        :get-resource-log
                                 :frequency 10000
                                 :event     [::get-resource-log]}]}]))))

(reg-event-fx
  ::get-resource-log
  (fn [{{:keys [::spec/id]} :db} _]
    (when id
      {::cimi-api-fx/get [id #(dispatch [::set-resource-log %])]})))

(defn join-component-name-to-line
  [[component-name line]]
  (str component-name "  | " line))

(defn prepend-component-name
  [[component lines]]
  (let [component-name (name component)]
    (map
      #(vector component-name %1)
      lines)))

(defn build-all-in-one-log
  [new-log]
  (->> new-log
       (mapcat prepend-component-name)
       (sort-by second)
       (map join-component-name-to-line)
       (assoc {} :_all-in-one)))

(defn concat-deduplicate-log-lines
  [new-lines old-lines]
  (-> (set new-lines)
      (remove old-lines)
      (concat new-lines)))

(defn merge-each-component-logs
  [old-log [component new-lines]]
  [component (concat-deduplicate-log-lines
               (or new-lines [])
               (get old-log component []))])

(defn merge-logs
  [new-log old-log]
  (->> new-log
       (map (partial merge-each-component-logs old-log))
       (into {})))

(reg-event-fx
  ::set-resource-log
  (fn [{{:keys [::spec/resource-log
                ::spec/play?] :as db} :db} [_ new-resource-log]]
    (let [new-log     (:log new-resource-log)
          old-log     (:log resource-log)
          all-in-one? (empty? (:components new-resource-log))
          merged-logs (-> new-log
                          (cond-> all-in-one? (build-all-in-one-log))
                          (merge-logs old-log))]
      {:db       (assoc db ::spec/resource-log
                           (assoc new-resource-log :log merged-logs))
       :fx       [(when play? [:dispatch [::fetch]])]})))

(reg-event-fx
  ::fetch
  (fn [{{:keys [::spec/id]} :db}]
    {::cimi-api-fx/operation [id "fetch" #()]}))

(reg-event-fx
  ::create-log
  (fn [{{:keys [::spec/parent
                ::spec/since
                ::spec/components]} :db} _]
    (let [on-success #(dispatch [::set-log-id (:resource-id %)])
          data       {:since      since
                      :components (or components [])}]
      {::cimi-api-fx/operation [parent "create-log" on-success :data data]})))

(reg-event-fx
  ::set-log-id
  (fn [{{:keys [::spec/play?] :as db} :db} [_ resource-log-id]]
    {:db       (assoc db ::spec/id resource-log-id)
     :dispatch [::set-play? play?]}))

(reg-event-db
  ::set-parent
  (fn [db [_ resource-id]]
    (assoc db ::spec/parent resource-id)))

(reg-event-db
  ::clear
  (fn [db [_ current-log]]
    (assoc-in db [::spec/resource-log :log] (into {} (map (fn [[k _]] {k []}) current-log)))))

(reg-event-fx
  ::set-components
  (fn [{{:keys [::spec/id] :as db} :db} [_ components]]
    (cond-> {:db (assoc db ::spec/components components)}
            id (assoc :dispatch [::delete]))))

(reg-event-db
  ::set-available-components
  (fn [db [_ components]]
    (assoc db ::spec/available-components components)))
