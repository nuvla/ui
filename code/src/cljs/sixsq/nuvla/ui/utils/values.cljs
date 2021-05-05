(ns sixsq.nuvla.ui.utils.values
  "General functions for rendering values."
  (:require
    [clojure.pprint :refer [pprint]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.history.views :as history]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [markdown-to-hiccup.core :as md]))


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
  [href & {:keys [label page]}]
  [history/link (str (or page "api") "/" href) (or label href)])


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


(defn status->color
  [status]
  (case status
    :online "green"
    :offline "red"
    :unknown "yellow"
    nil))


(defn copy-value-to-clipboard
  ([value value-to-copy popup-text] (copy-value-to-clipboard value value-to-copy popup-text true))
  ([value value-to-copy popup-text show?]
   [ui/CopyToClipboard {:text value-to-copy}
    [:span value " "
     [ui/Popup {:content (r/as-element [:p popup-text])
                :trigger (r/as-element [ui/Icon
                                        {:class [(when (not show?) "hide")]
                                         :name  "clipboard outline"
                                         :color "blue"
                                         :style {:color "black"}}])}]]]))


(defn markdown->hiccup
  [markdown]
  (->> markdown (md/md->hiccup) (md/component)))


(defn hiccup->first-p
  [hiccup]
  (some #(when (= :p (first %)) (nth % 2)) (drop 2 hiccup)))


(defn markdown->summary
  [markdown]
  (-> markdown markdown->hiccup hiccup->first-p))