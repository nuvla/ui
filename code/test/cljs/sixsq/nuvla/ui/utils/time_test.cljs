(ns sixsq.nuvla.ui.utils.time-test
  (:require
    ["mockdate" :as mock-date]
    [cljs.test :refer [are deftest is testing]]
    [clojure.string :as str]
    [sixsq.nuvla.ui.utils.time :as time :refer [now]]))

(def year 2022)
(def month 10)
(def date 5)
(def hour 16)
(def minute 45)
(def seconds 40)
(def milliseconds 400)
(def test-date (js/Date. year month date hour minute seconds milliseconds))
(def test-date-as-iso-string (.toISOString test-date))
(def test-date-as-iso-string-only-date (-> test-date-as-iso-string (str/split "T") (first)))
(def test-date-as-unix-timestamp (Math/floor (/ (.getTime test-date) 1000)))
(.set mock-date test-date)


(deftest add-or-subtract-ms-test
  (are [result input]
    (= result
       (-> (time/add-milliseconds test-date input) .getMilliseconds)
       (-> (time/subtract-milliseconds test-date (- input)) .getMilliseconds))
    600 200
    200 -200
    900 500
    900 -500
    0 600
    100 700))

(deftest parse-iso8601-test
  (are [result iso-string]
    (= result
       (.toString (time/parse-iso8601 iso-string)))
    "Sat Nov 05 2022 16:45:40 GMT+0100 (Central European Standard Time)" test-date-as-iso-string
    "Sat Nov 05 2022 00:00:00 GMT+0100 (Central European Standard Time)" test-date-as-iso-string-only-date))

(deftest parse-unix-test
  (is (= "Sat Nov 05 2022 16:45:40 GMT+0100 (Central European Standard Time)"
         (.toString (time/parse-unix test-date-as-unix-timestamp))))
  (is (= "Sat Nov 05 2022 16:45:40 GMT+0100 (Central European Standard Time)"
         (.toString (js/Date. (* 1000 test-date-as-unix-timestamp))))))

(deftest ago-test
  (doseq [[milliseconds {expected-en :en
                         expected-fr :fr}]
          {1000                   {:en "1 second ago" :fr "il y a 1 seconde"}
           (* 30 1000)            {:en "30 seconds ago" :fr "il y a 30 secondes"}
           (* 60 1000)            {:en "1 minute ago" :fr "il y a 1 minute"}
           (* 2 60 1000)          {:en "2 minutes ago" :fr "il y a 2 minutes"}
           (* 30 60 1000)         {:en "30 minutes ago" :fr "il y a 30 minutes"}
           (* 60 60 1000)         {:en "1 hour ago" :fr "il y a 1 heure"}
           (* 2 60 60 1000)       {:en "2 hours ago" :fr "il y a 2 heures"}
           (* 20 60 60 1000)      {:en "20 hours ago" :fr "il y a 20 heures"}
           (* 24 60 60 1000)      {:en "1 day ago" :fr "il y a 1 jour"}
           (* 30 24 60 60 1000)   {:en "30 days ago" :fr "il y a 30 jours"}
           (* 185 24 60 60 1000)  {:en "6 months ago" :fr "il y a 6 mois"}
           (* 365 24 60 60 1000)  {:en "1 year ago" :fr "il y a 1 an"}
           (* 720 24 60 60 1000)  {:en "2 years ago" :fr "il y a 2 ans"}
           (* 2000 24 60 60 1000) {:en "5 years ago" :fr "il y a 5 ans"}}]
    (let [past-moment (time/subtract-milliseconds test-date milliseconds)]
      (is (= expected-en (time/ago past-moment)))
      (is (= expected-en (time/ago past-moment "en")))
      (is (= expected-fr (time/ago past-moment "fr"))))))

(deftest before-and-after-now?-tests
  (let [before (time/subtract-milliseconds (now) 1000)
        after  (time/add-milliseconds (now) (* 60 1000))]
    (is (= (time/before-now? before) (time/after-now? after) true))
    (is (= (time/before-now? after) (time/after-now? before) false))))

(deftest invalid-test
  (is (= "Invalid date" (time/invalid)))
  (is (= "Invalid date" (time/invalid "en")))
  (is (= "Invalid date" (time/invalid "fr"))))


(deftest remaining-test
  (doseq [[milliseconds {expected-en :en
                         expected-fr :fr}]
          {172800000000 {:en "over 5 years", :fr "plus de 5 ans"},
           31104000000  {:en "12 months", :fr "12 mois"},
           3600000      {:en "about 1 hour", :fr "environ 1 heure"},
           72000000     {:en "about 20 hours", :fr "environ 20 heures"},
           15552000000  {:en "6 months", :fr "6 mois"},
           1000         {:en "less than 5 seconds", :fr "moins de 5 secondes"},
           86400000     {:en "1 day", :fr "1 jour"},
           7200000      {:en "about 2 hours", :fr "environ 2 heures"},
           120000       {:en "2 minutes", :fr "2 minutes"},
           60000        {:en "1 minute", :fr "1 minute"},
           30000        {:en "half a minute", :fr "30 secondes"},
           1800000      {:en "30 minutes", :fr "30 minutes"},
           2592000000   {:en "about 1 month", :fr "environ 1 mois"}}]
    (let [past-moment (time/add-milliseconds test-date milliseconds)]
      (is (= expected-en (time/remaining past-moment)))
      (is (= expected-en (time/remaining past-moment "en")))
      (is (= expected-fr (time/remaining past-moment "fr"))))))


(deftest delta-tests
  (let [mom              (now)
        thirty-thousand  (* 5 60 1000)
        five-minutes-ago (time/subtract-milliseconds mom thirty-thousand)]

    ;; Explaning Math/floor here: time/delta-minutes without second date
    ;; input is not pure because it takes current time as default
    ;; If execution is slow, returned minutes can e.g. be 5.0000033 and test fails
    (is (= 5 (Math/floor (time/delta-minutes five-minutes-ago))))
    (is (= 5 (time/delta-minutes five-minutes-ago mom)))
    (is (= thirty-thousand (time/delta-milliseconds five-minutes-ago mom)))))

;; TODO ? not used atm
(deftest delta-humanize)


(deftest time-value-test
  (let [five-days-ago     (time/add-milliseconds (now) (* 1000 60 60 24 5))
        five-days-ago-iso (.toISOString five-days-ago)]
    (is (= (str "in 5 days (" five-days-ago-iso ")") (time/time-value five-days-ago-iso)))))

;; TODO ? not used atm
(deftest range-equals)

(deftest time->utc-str-test
  (is (= (time/time->utc-str test-date) "2022-11-05T15:45:40Z")))


(deftest time->format-test
  (testing "No locale and no format"
    (is (= "2022/11/05, 04:45:40" (time/time->format test-date-as-iso-string))))
  (doseq [[format expected] {"yyyy/MM/dd, hh:mm:ss" {:en "2022/11/05, 04:45:40" :fr "2022/11/05, 04:45:40"}
                             "LL"                   {:en "November 5th, 2022" :fr "5 novembre 2022"}
                             "LLL"                  {:en "November 5th, 2022 4:45 PM" :fr "5 novembre 2022 16:45"}}]
    (testing (str "time->format with no locale for format: " format)
      (is (= (:en expected) (time/time->format test-date-as-iso-string format))))
    (testing (str "time->format with locale 'en' for format: " format)
      (is (= (:en expected) (time/time->format test-date-as-iso-string format "en"))))
    (testing (str "time->format with locale 'fr' for format: " format)
      (is (= (:fr expected) (time/time->format test-date-as-iso-string format "fr"))))))

;; TODO ? not used atm
(deftest moment-format)

(deftest parse-ago
  (let [this-moment       (now)
        five-days-ago     (time/subtract-milliseconds this-moment (* 5 24 60 60 1000))
        five-days-ago-iso (.toISOString (js/Date. five-days-ago))]
    (is (= nil (time/parse-ago nil "en")))
    (is (= "5 days ago" (time/parse-ago five-days-ago-iso "en")))
    (is (= "il y a 5 jours" (time/parse-ago five-days-ago-iso "fr")))))
