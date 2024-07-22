(ns sixsq.nuvla.ui.utils.collapsible-card
  (:require [clojure.string :as str]
            [re-frame.core :refer [subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.table :as table]))


(defn more-or-less
  [state-atom]
  (let [tr    (subscribe [::i18n-subs/tr])
        more? state-atom]
    (fn [_state-atom]
      (let [label     (@tr (if @more? [:less-details] [:more-details]))
            icon-name (if @more? icons/i-angle-down icons/i-angle-right)]
        [:a {:style    {:cursor "pointer"}
             :on-click #(reset! more? (not @more?))}
         [icons/Icon {:name icon-name}]
         label]))))


(defn definition-row
  ([[k v]]
   (definition-row k v))
  ([k v]
   [ui/TableRow
    [ui/TableCell {:collapsing true} k]
    [ui/TableCell v]]))


(defn properties-table
  [properties]
  (when (seq properties)
    (->> properties
         (map (fn [[k v]] [(str k) (str v)]))
         (sort-by first)
         (map definition-row)
         (table/definition-table "tags" "properties"))))


(defn metadata
  [_meta _rows]
  (let [more? (r/atom false)]
    (fn [{:keys [title subtitle description logo icon updated properties] :as _meta} rows]
      [ui/Card {:fluid true}
       [ui/CardContent
        (when logo
          [ui/Image {:floated "right", :size :tiny, :src logo}])
        [ui/CardHeader
         [ui/Icon {:class icon}]
         (cond-> title
                 (not (str/blank? subtitle)) (str " (" subtitle ")"))]
        [ui/CardMeta
         (when description [:p description])
         (when updated [:p [uix/TimeAgo updated]])]
        [ui/CardDescription
         (when (or (seq rows) (seq properties))
           [more-or-less more?])
         (when @more?
           [table/definition-table rows])
         (when @more? [properties-table properties])]]])))


(defn collapsible-segment
  [_title & _children]
  (let [visible? (r/atom true)]
    (fn [title & children]
      [ui/Segment style/basic
       [ui/Menu {:attached "top", :borderless true, :class "nuvla-ui-section-header"}
        [ui/MenuItem {:position "left"
                      :header   true}
         title]
        [uix/MenuItemSectionToggle {:position "right"
                                    :visible? @visible?
                                    :on-click #(reset! visible? (not @visible?))}]]
       [ui/Transition {:visible       @visible?
                       :animation     "fade"
                       :duration      300
                       :unmountOnHide true}
        (vec (concat [ui/Segment {:attached true}]
                     children))]])))
