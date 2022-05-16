(ns sixsq.nuvla.ui.utils.tab
  (:require [taoensso.timbre :as log]))


(defn key->index
  [panes k]
  (loop [i 0
         next-panes panes]
    (if (seq next-panes)
      (if (= ((comp :key :menuItem) (first next-panes)) k)
        i
        (recur (inc i) (next next-panes)))
      (do
        (log/error "tab-key not found: " k panes)
        0))))


(defn index->key
  [panes i]
  (if-let [k (some-> panes vec (get i) :menuItem :key)]
    k
    (log/error "tab-index not found:" i panes)))


(defn on-tab-change
  [panes callback]
  (fn [_ data]
    (callback (index->key panes (. data -activeIndex)))))
