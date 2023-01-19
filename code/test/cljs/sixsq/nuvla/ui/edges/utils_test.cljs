(ns sixsq.nuvla.ui.edges.utils-test
  (:require [clojure.test :refer [deftest are]]
            [sixsq.nuvla.ui.edges.utils :as t]))

(deftest score-vulnerability-test
  (are [result input]
    (= result
       (t/score-vulnerability input))
    {:severity "UNKNOWN", :color "#949494"} nil
    {:severity "UNKNOWN", :color "#949494"} {}
    {:vulnerability-score 1.4, :severity "LOW", :color "#21b802"} {:vulnerability-score 1.4}
    {:vulnerability-score 9.5, :severity "CRITICAL", :color "#f41906"} {:vulnerability-score 9.5}))
