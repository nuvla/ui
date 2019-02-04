(ns sixsq.slipstream.webui.utils.values
  "General functions for rendering values."
  (:require
    [clojure.pprint :refer [pprint]]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.slipstream.webui.history.views :as history]
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]))


(defn href?
  "Returns true if the map contains an href attribute."
  [value]
  (and (map? value) (:href value)))


(defn as-href
  "Renders a link to the API detail page associated with the href. Ignores
   other values of the map (if any)."
  [{:keys [href]}]
  [history/link (str "api/" href) (str href)])


(defn as-link
  "Renders a link to the API detail page associated with the href."
  [href & [label]]
  [history/link (str "api/" href) (or label href)])


(defn href-coll?
  [value]
  (and (coll? value)
       (every? href? value)))


(defn as-href-coll
  [value]
  (vec (concat [:span] (interpose " " (map as-href value)))))


(defn format-value
  "This will format a value for presentation in the UI. Note that this assumes
   that vectors are already a visual element and will return the vector
   unmodified. If you need to transform the vector into a list, use the
   format-collection function."
  [value]
  (cond
    (href-coll? value) (as-href-coll value)
    (href? value) (as-href value)
    (vector? value) value
    (map? value) (with-out-str (pprint value))
    :else (str value)))


(defn stringify-value
  [v]
  (if (or (map? v) (vector? v) (coll? v))
    (with-out-str (pprint v))
    (str v)))


(defn format-item
  [v]
  (let [s (stringify-value v)]
    ^{:key s} [ui/ListItem s]))


(defn format-collection
  "Transforms a collection into a Semantic UI list. The elements of the
   collection are turned into strings. If the argument is not a collection,
   then the value is returned unchanged."
  [v]
  (if (coll? v)
    (vec (concat [ui/ListSA] (map format-item v)))
    v))

