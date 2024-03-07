(ns sixsq.nuvla.ui.utils.timeseries
  (:require [clojure.string :as str]
            [sixsq.nuvla.ui.utils.time :as time]))

(def timespan-options ["last 15 minutes" "last hour" "last 12 hours" "last day" "last week" "last month" "last 3 months" "last year" "custom period"])
(defn custom-timespan? [timespan]
  (not (contains? (set timespan-options) timespan)))

(defn timespan-to-period [timespan]
  (let [now (time/now)]
    (case timespan
      "last 15 minutes" [(time/subtract-minutes now 15) now]
      "last hour" [(time/subtract-minutes now 60) now]
      "last 12 hours" [(time/subtract-minutes now 360) now]
      "last day" [(time/subtract-days now 1) now]
      "last week" [(time/subtract-days now 7) now]
      "last month" [(time/subtract-months now 1) now]
      "last 3 months" [(time/subtract-months now 3) now]
      "last year" [(time/subtract-years now 1) now])))
(def fixed-timespan->granularity {"last 15 minutes"       "1-minutes"
                                  "last hour"             "2-minutes"
                                  "last 12 hours"         "3-minutes"
                                  "last day"              "30-minutes"
                                  "last week"             "1-hours"
                                  "last month"            "6-hours"
                                  "last 3 months"         "2-days"
                                  "last year"             "7-days"})

(defn custom-timespan->granularity [timespan]
  (let [[from to] timespan
        distance (time/distance-between from to)]
        (cond
          (str/includes? distance "minutes") "1-minutes"
          (str/includes? distance "hours") "10-minutes"
          (str/includes? distance "days") "6-hours"
          (str/includes? distance "months") "2-days"
          (str/includes? distance "month") "6-hours"
          (str/includes? distance "day") "30-minutes"
          (str/includes? distance "year") "7-days")))



(defn granularity-for-timespan [timespan]
  (if (custom-timespan? timespan)
    (custom-timespan->granularity timespan)
    (get fixed-timespan->granularity timespan)))

(defn format-option [option-str]
  (-> option-str
      (str/replace #" " "-")
      (keyword)))
(defn data->ts-data [data]
  (-> data first :ts-data))

(defn add-time [timestamp granularity]
  (let [[amount unit] (str/split granularity #"-")]
    (cond
      (= unit "minutes") (time/add-minutes timestamp (js/parseInt amount))
      (= unit "hours") (time/add-hours timestamp (js/parseInt amount))
      (= unit "days") (time/add-days timestamp (js/parseInt amount)))))
