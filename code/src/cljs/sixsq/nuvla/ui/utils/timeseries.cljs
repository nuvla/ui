(ns sixsq.nuvla.ui.utils.timeseries
  (:require [clojure.string :as str]
            [sixsq.nuvla.ui.utils.time :as time]))


(def timespan-last-15m "last 15 minutes")
(def timespan-last-hour "last hour")
(def timespan-last-6h "last 6 hours")
(def timespan-last-day "last day")
(def timespan-last-week "last week")
(def timespan-last-month "last month")
(def timespan-last-3-month "last 3 months")
(def timespan-last-year "last year")
(def timespan-custom "custom period")
(def timespan-options [timespan-last-15m
                       timespan-last-hour
                       timespan-last-6h
                       timespan-last-day
                       timespan-last-week
                       timespan-last-month
                       timespan-last-3-month
                       timespan-last-year
                       timespan-custom])

(def timespan-options-master [timespan-last-6h
                              timespan-last-day
                              timespan-last-week
                              timespan-last-month
                              timespan-last-3-month
                              timespan-last-year
                              timespan-custom])
(defn custom-timespan? [timespan-option]
  (= timespan-custom timespan-option))

(defn timespan-to-period [timespan]
  (let [now (time/now)]
    (case timespan
      "last 15 minutes" [(time/subtract-minutes now 15) now]
      "last hour" [(time/subtract-minutes now 60) now]
      "last 6 hours" [(time/subtract-minutes now 360) now]
      "last day" [(time/subtract-days now 1) now]
      "last week" [(time/subtract-days now 7) now]
      "last month" [(time/subtract-months now 1) now]
      "last 3 months" [(time/subtract-months now 3) now]
      "last year" [(time/subtract-years now 1) now])))
(def fixed-timespan->granularity
  {"last 15 minutes" "1-minutes"
   "last hour"       "2-minutes"
   "last 6 hours"    "3-minutes"
   "last day"        "30-minutes"
   "last week"       "1-hours"
   "last month"      "6-hours"
   "last 3 months"   "2-days"
   "last year"       "7-days"})

(defn custom-timespan->granularity [{:keys [from to]}]
  (let [distance (time/distance-between from to)]
    (cond
      (str/includes? distance "minutes") "1-minutes"
      (str/includes? distance "hours") "10-minutes"
      (str/includes? distance "days") "6-hours"
      (str/includes? distance "months") "2-days"
      (str/includes? distance "month") "6-hours"
      (str/includes? distance "day") "30-minutes"
      (str/includes? distance "year") "7-days")))

(defn granularity-for-timespan [{:keys [timespan-option] :as timespan}]
  (if (custom-timespan? timespan-option)
    (custom-timespan->granularity timespan)
    (get fixed-timespan->granularity timespan-option)))

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
