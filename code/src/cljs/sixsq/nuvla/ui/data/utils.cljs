(ns sixsq.nuvla.ui.data.utils
  (:require [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.utils :refer [name->href str-pathify]]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.time :as time]))

(defn matches-parameter-name?
  [parameter-name parameter]
  (and parameter-name (= parameter-name (:parameter parameter))))

(defn update-parameter-in-list
  [name value parameters]
  (let [f       (partial matches-parameter-name? name)
        current (first (filter f parameters))
        others  (remove f parameters)]
    (when current
      (->> (assoc current :value value)
           (conj others)
           (sort-by :parameter)
           vec))))

(defn update-parameter-in-deployment
  [name value deployment]
  (->> deployment
       :module
       :content
       :inputParameters
       (update-parameter-in-list name value)
       (assoc-in deployment [:module :content :inputParameters])))

(defn create-time-period-filter
  [[time-start time-end]]
  (str "(timestamp>='"
       (time/time->utc-str time-start)
       "' and timestamp<'"
       (time/time->utc-str time-end)
       "')"))

(defn format-bytes
  [bytes]
  (if (number? bytes)
    (let [scale 1024
          units ["B" "KiB" "MiB" "GiB" "TiB" "PiB" "EiB"]]
      (if (< bytes scale)
        (str bytes " B")
        (let [exp    (int (/ (js/Math.log bytes) (js/Math.log scale)))
              prefix (get units exp)
              v      (/ bytes (js/Math.pow scale exp))]
          (str (general-utils/round-up v :n-decimal 1) prefix))))
    "..."))

(defn data-record-href
  [id]
  (str-pathify (name->href routes/data) (general-utils/id->uuid id)))
