(ns sixsq.nuvla.ui.edges.utils-test
  (:require [clojure.test :refer [are deftest is]]
            [sixsq.nuvla.ui.pages.edges.utils :as t]))

(deftest score-vulnerability-test
  (are [result input]
    (= result
       (t/score-vulnerability input))
    {:severity "UNKNOWN", :color "#949494"} nil
    {:severity "UNKNOWN", :color "#949494"} {}
    {:vulnerability-score 1.4, :severity "LOW", :color "#21b802"} {:vulnerability-score 1.4}
    {:vulnerability-score 9.5, :severity "CRITICAL", :color "#f41906"} {:vulnerability-score 9.5}))

(def test-versions-sort-data
  [{:release "2.7.10"}
   {:release "2.9.9"}
   {:release "2.7.8"}
   {:release "2.7.9"}
   {:release "1.2.3"}
   {:release "2.1.0"}
   {:release "3.0.0"}
   {:release "2.7.11"}
   {:release "2.11.8"}
   {:release "2.8.11"}
   {:release "2.10.7"}
   {:no-version ""}])

(deftest version-sort-test
  (is '({:release "3.0.0"}
        {:release "2.11.8"}
        {:release "2.10.7"}
        {:release "2.9.9"}
        {:release "2.8.11"}
        {:release "2.7.11"}
        {:release "2.7.10"}
        {:release "2.7.9"}
        {:release "2.7.8"}
        {:release "2.1.0"}
        {:release "1.2.3"}
        {:no-version ""})
      (t/sort-by-version test-versions-sort-data)))

(deftest version-difference-test
  (are [result args]
    (= result (t/version-difference (first args) (second args)))
    nil ["2.1.0" "2.1.0"]
    nil ["something" "2.1.0"]
    nil ["2.1.0" "something"]
    nil ["something" "something"]
    {:major 1} ["3.1.0" "2.1.0"]
    {:major 2} ["4.1.0" "2.1.0"]
    {:major -2} ["2.1.0" "4.1.1"]
    {:patch 4} ["2.1.4" "2.1.0"]
    {:minor -25} ["0.1.0" "0.26.1"]))

(deftest ne-version-outdated-test
  (are [result args]
    (= result (t/ne-version-outdated (first args) (second args)))
    nil [nil nil]
    nil [nil "2.1.0"]
    nil ["2.1.0" "2.1.0"]
    nil ["something" "2.1.0"]
    nil ["2.1.0" "something"]
    nil ["something" "something"]
    nil ["2.1.0" "2.5.1"]                                   ; case NE version is beyond latest version
    :outdated-major-version ["3.1.0" "2.1.0"]
    :outdated-major-version ["4.1.0" "2.1.0"]
    :outdated-major-version ["0.26.1" "0.1.0"]
    :outdated-major-version ["4.1.1" "2.1.0"]
    :outdated-minor-version ["2.1.1" "2.1.0"]
    :outdated-minor-version ["2.4.1" "2.1.0"]               ; up to 3 minor away it's still minor warning
    :outdated-major-version ["2.5.1" "2.1.0"]))

(deftest older-version?
  (are [result args]
    (= result (t/older-version? (first args) (second args)))
    true [nil nil]
    true ["" [2 1 0]]
    true ["2.14.0" [2 14 4]]
    true ["1.0.0" [2 14 4]]
    false ["2.14.4" [2 14 4]]
    false ["2.14.5" [2 14 4]]
    false ["2.15.5" [2 14 4]]
    false ["3.0.0" [2 14 4]]))
