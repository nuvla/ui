(ns sixsq.nuvla.ui.utils.time-test
  (:require [cljs.test :refer [is are deftest testing]]
            [sixsq.nuvla.ui.utils.time :as time :refer [now]]
            [clojure.string :as str]))

(def year 2022)
(def month 10)
(def date 5)
(def hour 16)
(def minute 45)
(def seconds 40)
(def milliseconds 400)
(def test-date (js/Date. year month date hour minute seconds milliseconds))
(def test-moment (time/date->moment test-date))
(def test-date-as-iso-string (.toISOString test-date))
(def test-date-as-iso-string-only-date (-> test-date-as-iso-string (str/split "T") (first)))
(def test-date-as-unix-timestamp (Math/floor (/ (.getTime test-date) 1000)))
(def this-moment (js/Date.))

(print test-date)
(print test-moment)
(print test-date-as-iso-string)
(print test-date-as-iso-string-only-date)
(print test-date-as-unix-timestamp)


(deftest add-or-subtract-ms-test
  (are [result input]
       (= result
          (-> (time/add-milliseconds test-moment input) .toDate .getMilliseconds)
          (-> (time/subtract-milliseconds test-moment (- input)) .toDate .getMilliseconds))
    600 200
    200 -200
    900 500
    900 -500
    0   600
    100 700))

(deftest parse-iso8601-test
  (are [result iso-string]
       (= result
          (.toString (.toDate (time/parse-iso8601 iso-string))))
    "Sat Nov 05 2022 16:45:40 GMT+0100 (Central European Standard Time)" test-date-as-iso-string
    "Sat Nov 05 2022 00:00:00 GMT+0100 (Central European Standard Time)" test-date-as-iso-string-only-date))

(deftest parse-unix-test
  (is (= "Sat Nov 05 2022 16:45:40 GMT+0100 (Central European Standard Time)"
         (.toString (.toDate (time/parse-unix test-date-as-unix-timestamp)))))
  (is (= "Sat Nov 05 2022 16:45:40 GMT+0100 (Central European Standard Time)"
         (.toString (js/Date. (* 1000 test-date-as-unix-timestamp))))))

(def time-ago-test-cases
  {1000 {:en "a few seconds ago"               :fr "il y a quelques secondes"}
   (* 30 1000) {:en "a few seconds ago"        :fr "il y a quelques secondes"}
   (* 60 1000) {:en "a minute ago"             :fr "il y a une minute"}
   (* 2 60 1000) {:en "2 minutes ago"          :fr "il y a 2 minutes"}
   (* 30 60 1000) {:en "30 minutes ago"        :fr "il y a 30 minutes"}
   (* 60 60 1000) {:en "an hour ago"           :fr "il y a une heure"}
   (* 2 60 60 1000) {:en "2 hours ago"         :fr "il y a 2 heures"}
   (* 20 60 60 1000) {:en "20 hours ago"       :fr "il y a 20 heures"}
   (* 24 60 60 1000) {:en "a day ago"          :fr "il y a un jour"}
   (* 30 24 60 60 1000) {:en "a month ago"     :fr "il y a un mois"}
   (* 180 24 60 60 1000) {:en "6 months ago"   :fr "il y a 6 mois"}
   (* 360 24 60 60 1000) {:en "a year ago"     :fr "il y a un an"}
   (* 2000 24 60 60 1000) {:en "5 years ago"   :fr "il y a 5 ans"}})

(deftest ago-test
  (doseq [test-case time-ago-test-cases]
    (let [past-moment (time/subtract-milliseconds (time/date->moment this-moment) (key test-case))]
      (is (= (time/ago past-moment) (:en (val test-case))))
      (is (= (time/ago past-moment "en") (:en (val test-case))))
      (is (= (time/ago past-moment "fr") (:fr (val test-case)))))))

(deftest before-and-after-now?-tests
  (let [before (time/subtract-milliseconds (now) 1000)
        after (time/add-milliseconds (now) (* 60 1000))]
    (is (= (time/before-now? before) (time/after-now? after) true))
    (is (= (time/before-now? after) (time/after-now? before) false))))


(def time-to-go-test-cases
  {172800000000 {:en "5 years", :fr "5 ans"},
   31104000000 {:en "a year", :fr "un an"},
   3600000 {:en "an hour", :fr "une heure"},
   72000000 {:en "20 hours", :fr "20 heures"},
   15552000000 {:en "6 months", :fr "6 mois"},
   1000 {:en "a few seconds", :fr "quelques secondes"},
   86400000 {:en "a day", :fr "un jour"},
   7200000 {:en "2 hours", :fr "2 heures"},
   120000 {:en "2 minutes", :fr "2 minutes"},
   60000 {:en "a minute", :fr "une minute"},
   30000 {:en "a few seconds", :fr "quelques secondes"},
   1800000 {:en "30 minutes", :fr "30 minutes"},
   2592000000 {:en "a month", :fr "un mois"}})


(deftest remaining-test
  (doseq [test-case time-to-go-test-cases]
    (let [past-moment (time/subtract-milliseconds (time/date->moment this-moment) (key test-case))]
      (is (= (:en (val test-case)) (time/remaining past-moment)))
      (is (= (:en (val test-case)) (time/remaining past-moment "en")))
      (is (= (:fr (val test-case)) (time/remaining past-moment "fr"))))))


(deftest delta-tests
  (let [mom (now)
        thirty-thousand (* 5 60 1000)
        five-minutes-ago (time/subtract-milliseconds mom thirty-thousand)]
    (is (= 5 (time/delta-minutes five-minutes-ago)))
    (is (= 5 (time/delta-minutes five-minutes-ago mom)))
    (is (= thirty-thousand (time/delta-milliseconds five-minutes-ago mom)))))

;; TODO ?
(deftest delta-humanize)

(deftest days-before-test
  (is (= (js/Date. (time/days-before 30)) (time/days-before-date 30))))

(deftest time-value-test
  (let [five-days-ago (time/add-milliseconds (now) (* 1000 60 60 24 5))
        five-days-ago-iso (.toISOString five-days-ago)]
    (is (= (str "in 5 days (" five-days-ago-iso ")") (time/time-value five-days-ago-iso)))))

;; TODO ?
(deftest range-equals)

(deftest time->utc-str-test
  (is (= (time/time->utc-str test-moment) "2022-11-05T15:45:40Z")))

(deftest js-date->utc-str)

(deftest time->format-test
  (doseq [[format expected]  {"YYYY/MM/DD, hh:mm:ss" {:en "2022/11/05, 04:45:40" :fr  "2022/11/05, 04:45:40" }
                              "LL" {:en "November 5, 2022" :fr  "5 novembre 2022" }
                              "LLL" {:en "November 5, 2022 4:45 PM" :fr "5 novembre 2022 16:45" }}]
    (is (= (:en expected) (time/time->format test-date-as-iso-string format)))
    (is (= (:en expected) (time/time->format test-date-as-iso-string format "en")))
    (is (= (:fr expected) (time/time->format test-date-as-iso-string format "fr")))))
(deftest moment-format)

(deftest parse-ago)
