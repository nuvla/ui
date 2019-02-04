(ns sixsq.slipstream.webui.deployment-detail.utils
  (:require
    [sixsq.slipstream.webui.utils.time :as time]))


(defn assoc-delta-time
  "Given the start (as a string), this adds a :delta-time entry in minutes."
  [start {end :timestamp :as evt}]
  (assoc evt :delta-time (time/delta-minutes start end)))


(defn category-icon
  [category]
  (case category
    "PROJECT" "folder"
    "APPLICATION" "sitemap"
    "IMAGE" "file"
    "COMPONENT" "microchip"
    "question circle"))


(defn has-action?
  [action deployment]
  (->> deployment
       :operations
       (filter #(= action (:rel %)))
       not-empty
       boolean))


(def stop-action? (partial has-action? "http://schemas.dmtf.org/cimi/2/action/stop"))


(def delete-action? (partial has-action? "delete"))
