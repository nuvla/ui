(ns sixsq.nuvla.ui.utils.timeseries
  (:require [clojure.string :as str]
            [sixsq.nuvla.ui.utils.time :as time]))

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

(def timespan->granularity {"last 15 minutes" "1-minutes"
                            "last hour"       "2-minutes"
                            "last 12 hours"   "3-minutes"
                            "last day"        "30-minutes"
                            "last week"       "1-hours"
                            "last month"      "6-hours"
                            "last 3 months"   "2-days"
                            "last year"       "7-days"})

(defn format-option [option-str]
  (-> option-str
      (str/replace #" " "-")
      (keyword)))

(def timespan-options ["last 15 minutes" "last hour" "last 12 hours" "last day" "last week" "last month" "last 3 months" "last year"])

(defn data->ts-data [data]
  (-> data first :ts-data))
