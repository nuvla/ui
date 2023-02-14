(ns sixsq.nuvla.ui.utils.values
  "General functions for rendering values."
  (:require [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [markdown-to-hiccup.core :as md]
            [reagent.core :as r]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))


(defn href?
  "Returns true if the map contains an href attribute."
  [value]
  (and (map? value) (:href value)))

(defn id?
  "Returns true if the value look like an id."
  [value]
  (and (string? value) (re-find #"^[a-z-]+/[a-zA-Z0-9-]+$" value)))

(defn href-coll?
  [value]
  (and (coll? value)
       (every? href? value)))

(defn id-coll?
  [value]
  (and (coll? value)
       (every? id? value)))

(defn AsHref
  "Renders a link to the API detail page associated with the href. Ignores
   other values of the map (if any)."
  [{:keys [href]}]
  [uix/Link (str "api/" href) (str href)])

(defn AsLink
  "Renders a link to the API detail page associated with the href."
  [href & {:keys [label page]}]
  [uix/Link (str (or page "api") "/" href) (or label href)])

(defn FormatValue
  [value]
  (cond
    (href? value) [AsHref value]
    (id? value) [AsLink value]
    (vector? value) value
    (map? value) (with-out-str (pprint value))
    :else (str value)))


(defn stringify-value
  [v]
  (if (or (map? v) (vector? v) (coll? v))
    (with-out-str (pprint v))
    (str v)))

(defn ListValues
  [values FormatValue]
  [ui/ListSA
   (for [value values]
     ^{:key (random-uuid)}
     [ui/ListItem [FormatValue value]])])


(defn FormatCollection
  "Transforms a collection into a Semantic UI list. The elements of the
   collection are turned into strings. If the argument is not a collection,
   then the value is returned unchanged."
  [v]
  (cond
    (href-coll? v) [ListValues v AsHref]
    (id-coll? v) [ListValues v AsLink]
    (coll? v) [ListValues v stringify-value]
    :else v))


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
    [:span {:style {:cursor :pointer}} value " "
     [ui/Popup {:content (r/as-element [:p popup-text])
                :trigger (r/as-element [ui/Icon
                                        {:class [(when (not show?) "hide")]
                                         :name  "clipboard outline"
                                         :color "blue"
                                         :style {:color "black"}}])}]]]))


(defn markdown->hiccup
  [markdown]
  (some-> markdown (md/md->hiccup {:encode? false}) md/component))


(defn hiccup->first-p
  [hiccup]
  (->> hiccup
       (drop 2)
       (some (fn [[html-tag :as html-element]]
               (when (= :p html-tag) html-element)))
       flatten
       (filter string?)
       str/join))


(defn markdown->summary
  [markdown]
  (-> markdown markdown->hiccup hiccup->first-p))


(defn resource->id
  [resource]
  (last (str/split resource #"/")))
