(ns sixsq.nuvla.ui.edges.utils-test
  (:require [clojure.test :refer [are deftest is]]
            [sixsq.nuvla.ui.edges.utils :as t]))

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


;; (deftest version-sort-test
;;   (is '({:release "3.0.0"}
;;         {:release "2.11.8"}
;;         {:release "2.10.7"}
;;         {:release "2.9.9"}
;;         {:release "2.8.11"}
;;         {:release "2.7.11"}
;;         {:release "2.7.10"}
;;         {:release "2.7.9"}
;;         {:release "2.7.8"}
;;         {:release "2.1.0"}
;;         {:release "1.2.3"}
;;         {:no-version ""})
;;       (t/sort-by-version test-versions-sort-data)))
