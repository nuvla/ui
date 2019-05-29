(ns sixsq.nuvla.ui.dashboard-detail.utils
  (:require
    [sixsq.nuvla.ui.utils.time :as time]))


(defn assoc-delta-time
  "Given the start (as a string), this adds a :delta-time entry in minutes."
  [start {end :timestamp :as evt}]
  (assoc evt :delta-time (time/delta-minutes start end)))


(defn has-action?
  [action deployment]
  (->> deployment
       :operations
       (filter #(= action (:rel %)))
       not-empty
       boolean))


(def stop-action? (partial has-action? "stop"))


(def delete-action? (partial has-action? "delete"))


(defn is-started?
  [state]
  (= state "STARTED"))
