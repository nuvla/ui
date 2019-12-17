(ns sixsq.nuvla.ui.ocre.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.cimi.spec :as cimi-spec]
    [sixsq.nuvla.ui.ocre.spec :as spec]))


(reg-event-fx
  ::fetch-distributor-terms
  (fn [{{:keys [::cimi-spec/query-params]} :db} _]
    {::cimi-api-fx/search [:voucher {:first       0
                                     :last        10000
                                     :select      "platform, state"
                                     :filter      (:filter query-params)
                                     :aggregation "terms:distributor"}
                           #(dispatch [::set-distributor-terms %])]
     :dispatch            [::fetch-global-aggregations]}))


(defn re-group-by-state
  [platform-all-states all-states]
  (->> platform-all-states
       (group-by :state)
       (into (sorted-map))
       (merge (into (sorted-map) all-states))
       (map (fn [[k v]]
              (count v)))
       (vec)))


(reg-event-db
  ::set-distributor-terms
  (fn [db [_ response]]
    (let [all-states {"ACTIVATED" [], "DISTRIBUTED" [], "EXPIRED" [], "NEW" [], "REDEEMED" []}
          platforms  (->> response
                          :resources
                          (group-by :platform)
                          (map (fn [[k v]]
                                 {:label           k
                                  :data            (re-group-by-state v all-states)
                                  :backgroundColor (str "#" (.toString (rand-int 16rFFFFFF) 16))}
                                 )))]
      (assoc db
        ::spec/distributor-terms (-> response :aggregations :terms:distributor :buckets)
        ::spec/platforms-radar platforms))))


(reg-event-fx
  ::fetch-global-aggregations
  (fn [_ _]
    {::cimi-api-fx/search [:voucher {:first       0
                                     :last        0
                                     :aggregation (str/join ","
                                                            ["value_count:id"
                                                             "cardinality:platform"
                                                             "cardinality:supplier"
                                                             "cardinality:distributor"])}
                           #(dispatch [::set-global-aggregations %])]}))


(reg-event-db
  ::set-global-aggregations
  (fn [db [_ response]]
    (assoc db ::spec/global-aggregations (:aggregations response))))

