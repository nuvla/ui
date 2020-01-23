(ns sixsq.nuvla.ui.utils.collapsible-card
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.acl.views :as acl]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.table :as table]
    [sixsq.nuvla.ui.utils.time :as time]))


(defn more-or-less
  [state-atom]
  (let [tr    (subscribe [::i18n-subs/tr])
        more? state-atom]
    (fn [state-atom]
      (let [label     (@tr (if @more? [:less-details] [:more-details]))
            icon-name (if @more? "caret down" "caret right")]
        [:a {:style    {:cursor "pointer"}
             :on-click #(reset! more? (not @more?))}
         [ui/Icon {:name icon-name}]
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
  [{:keys [title subtitle description logo icon updated properties] :as meta} rows]
  (let [more? (r/atom false)]
    (fn [{:keys [title subtitle description logo icon updated properties] :as meta} rows]
      [ui/Card {:fluid true}
       [ui/CardContent
        (when logo
          [ui/Image {:floated "right", :size :tiny, :src logo}])
        [ui/CardHeader
         [ui/Icon {:name icon}]
         (cond-> title
                 (not (str/blank? subtitle)) (str " (" subtitle ")"))]
        [ui/CardMeta
         (when description [:p description])
         (when updated [:p (-> updated time/parse-iso8601 time/ago)])]
        [ui/CardDescription
         (when (or (seq rows) (seq properties))
           [more-or-less more?])
         (when @more?
           [table/definition-table rows])
         (when @more? [properties-table properties])]]])))


(defn metadata-simple
  [rows]
  (let [more? (r/atom false)]
    (fn [rows]
      [:div {:style {:padding-top    5
                     :padding-bottom 5}}
       (when (or (seq rows))
         [more-or-less more?])
       (when @more?
         [table/definition-table rows])])))


(defn title-card
  [title & children]
  [ui/Card {:fluid true}
   [ui/CardContent {:extra true}
    [ui/CardHeader
     [:h1 title]]]
   (when children
     [ui/CardContent
      (vec (concat [ui/CardDescription] children))])])


(defn collapsible-card
  [title & children]
  (let [visible? (r/atom true)]
    (fn [title & children]
      [ui/Card {:fluid true}
       [ui/CardContent
        [ui/Label {:as       :a
                   :corner   "right"
                   :size     "mini"
                   :icon     (if @visible? "chevron down" "chevron up")
                   :on-click #(reset! visible? (not @visible?))}]
        [ui/CardHeader title]
        (when @visible?
          (vec (concat [ui/CardDescription] children)))]])))


(defn collapsible-segment
  [title & children]
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
