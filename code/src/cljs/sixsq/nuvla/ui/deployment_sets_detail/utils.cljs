(ns sixsq.nuvla.ui.deployment-sets-detail.utils
  (:require [clojure.string :as str]
            [sixsq.nuvla.ui.filter-comp.utils :as filter-comp]))

(def editable-keys [:name :description :applications-sets])

(defn unsaved-changes?
  [deployment-set deployment-set-edited]
  (and (some? deployment-set-edited)
       (not= (select-keys deployment-set-edited editable-keys)
             (select-keys deployment-set editable-keys))))

;; Edge filter manipulation utilities

(def fulltext-attribute "fulltext")

(defn drop-trailing-star
  [s]
  (some-> s (str/replace #"\*$" "")))

(defn extract-fulltext-filter
  [fleet-filter]
  (->> fleet-filter
       filter-comp/filter-str->data
       (filter #(= fulltext-attribute (:attribute %)))
       first
       :value
       drop-trailing-star))

(def not-null-id-predicate
  {:el "attribute", :attribute "id", :operation "!=", :value "<NULL>"})

(def empty-element "empty")

(defn remove-empty-elements
  [filter-data]
  (->> filter-data
       (remove #(= empty-element (:el %)))))

(def and-element {:el "logic", :value "and"})

(defn remove-initial-not-null-predicate
  [filter-data]
  (->> filter-data
       (remove #(= not-null-id-predicate %))
       (drop-while #(= and-element %))))

(defn drop-while-rev
  [x coll]
  (->> coll
       reverse
       (drop-while #(= % x))
       reverse))

(defn remove-fulltext-filter
  [filter-data]
  (->> filter-data
       (remove #(= fulltext-attribute (:attribute %)))
       (drop-while-rev and-element)))

(defn extract-additional-filter
  [fleet-filter]
  (->> fleet-filter
       filter-comp/filter-str->data
       remove-empty-elements
       remove-initial-not-null-predicate
       remove-fulltext-filter
       filter-comp/data->filter-str))

