(ns sixsq.nuvla.ui.log-resource.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.edge-detail.spec :as spec]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.utils.time :as time]))


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
    (cond-> {:db (assoc db ::spec/since since)}
            id (assoc :dispatch [::delete]))))


(reg-event-fx
  ::set-play?
  (fn [{{:keys [::spec/id] :as db} :db} [_ play?]]
    (cond-> {:db       (assoc db ::spec/play? play?)
             :dispatch (if play?
                         (if id
                           [::fetch]
                           [::create-log])
                         [::main-events/action-interval-delete
                          {:id   :get-resource-log}])}
            (and play?
                 id)
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


(reg-event-fx
  ::set-resource-log
  (fn [{{:keys [::spec/resource-log] :as db} :db} [_ new-resource-log]]
    (let [new-log               (:log new-resource-log)
          old-log               (:log resource-log)
          all-in-one?           (empty? (:components new-resource-log))
          concatenated-log      (if all-in-one?
                                  {:_all-in-one (concat
                                                  (:_all-in-one old-log)
                                                  (map
                                                    (fn [el]
                                                      (str (name (first el)) "  | " (second el)))
                                                    (sort-by second
                                                             (mapcat
                                                               (fn [[k v]]
                                                                 (map
                                                                   (fn [s]
                                                                     [k s]) v)) new-log))))}

                                  (into {}
                                        (map
                                          (fn [[k v]]
                                            {k (concat (remove (set (get new-log k [])) (get old-log k [])) v)})
                                          new-log)))]
      {:db       (assoc db ::spec/resource-log
                           (assoc new-resource-log :log concatenated-log))
       :dispatch [::fetch]})))


(reg-event-fx
  ::fetch
  (fn [{{:keys [::spec/id]} :db} _]
    {::cimi-api-fx/operation [id "fetch" #()]}))


(reg-event-fx
  ::create-log
  (fn [{{:keys [::spec/nuvlabox
                ::spec/since
                ::spec/components]} :db} _]
    {::cimi-api-fx/operation [(:id nuvlabox) "create-log"
                              #(if (instance? js/Error %)
                                 (cimi-api-fx/default-error-message % "Create log action failed!")
                                 (dispatch [::set-log-id (:resource-id %)]))
                              {:since       (time/time->utc-str since)
                               :components  (or components [])}]}))


(reg-event-fx
  ::set-log-id
  (fn [{{:keys [::spec/play?] :as db} :db} [_ resource-log-id]]
    {:db       (assoc db ::spec/id resource-log-id)
     :dispatch [::set-play? play?]}))


(reg-event-db
  ::clear
  (fn [db [_ current-log]]
    (assoc-in db [::spec/resource-log :log] (into {} (map (fn [[k _]] {k []}) current-log)))))


(reg-event-fx
  ::set-components
  (fn [{{:keys [::spec/id] :as db} :db} [_ components]]
    (cond-> {:db (assoc db ::spec/components components)}
            id (assoc :dispatch [::delete]))))