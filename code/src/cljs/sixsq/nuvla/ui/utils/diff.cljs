(ns sixsq.nuvla.ui.utils.diff
  (:require
    ["diff" :as diff]))


(defn diff-chars
  [& opts]
  (apply diff/diffChars opts))

