(ns sixsq.nuvla.ui.apps-project.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.apps-project.events :as events]
    [sixsq.nuvla.ui.apps-project.spec :as spec]
    [sixsq.nuvla.ui.apps-project.utils :as utils]
    [sixsq.nuvla.ui.apps.events :as apps-events]
    [sixsq.nuvla.ui.apps.spec :as apps-spec]
    [sixsq.nuvla.ui.apps.subs :as apps-subs]
    [sixsq.nuvla.ui.apps.utils :as apps-utils]
    [sixsq.nuvla.ui.apps.views-detail :as apps-views-detail]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.values :as values]))


(defn clear-module
  []
  (dispatch [::events/clear-module]))


(defn format-module
  [{:keys [subtype name path description] :as module}]
  (when module
    (let [on-click  #(dispatch [::history-events/navigate (str "apps/" path)])
          icon-name (apps-utils/subtype-icon subtype)
          summary   (values/markdown->summary description)]
      [ui/ListItem {:on-click on-click}
       [ui/ListIcon {:name           icon-name
                     :size           "large"
                     :vertical-align "middle"}]
       [ui/ListContent
        [ui/ListHeader [:a {:on-click on-click} name]]
        [ui/ListDescription
         [uix/SpanBlockJustified summary]]]])))


(defn format-module-children
  [module-children]
  (when (pos? (count module-children))
    (let [ordered-children (sort-by :name module-children)]
      [ui/Segment style/basic
       (vec (concat [ui/ListSA {:divided   true
                                :relaxed   true
                                :selection true}]
                    (map (fn [{:keys [id] :as module}]
                           ^{:key id}
                           [format-module module]) ordered-children)))])))


(defn modules-view []
  (let [tr     (subscribe [::i18n-subs/tr])
        module (subscribe [::apps-subs/module])]
    (fn []
      (let [children (:children @module)]
        [ui/Segment {:secondary true
                     :color     "blue"
                     :raised    true}
         [:h4 (str/capitalize (@tr [:apps-sub-modules]))]
         (if (empty? children)
           [ui/Message {:warning true}
            [ui/Icon {:name "warning sign"}]
            (@tr [:no-children-modules])]
           [format-module-children children])]))))


(defn OverviewModuleSummary
  []
  (let [tr     (subscribe [::i18n-subs/tr])
        locale (subscribe [::i18n-subs/locale])
        module (subscribe [::apps-subs/module])
        {:keys [id created updated name parent-path path]} @module]
    [ui/Segment {:secondary true
                 :color     "blue"
                 :raised    true}
     [:h4 (str/capitalize (@tr [:project]))]
     [ui/Grid {:columns 2}
      [ui/GridColumn
       [ui/Table {:basic  "very"
                  :padded false}
        [ui/TableBody
         (when name
           [ui/TableRow
            [ui/TableCell (str/capitalize (@tr [:name]))]
            [ui/TableCell [values/as-link path :label name :page "apps"]]])
         (when (seq parent-path)
           [ui/TableRow
            [ui/TableCell (str/capitalize (@tr [:project]))]
            [ui/TableCell [values/as-link parent-path :label parent-path :page "apps"]]])
         [ui/TableRow
          [ui/TableCell (str/capitalize (@tr [:created]))]
          [ui/TableCell (if created (time/ago (time/parse-iso8601 created) @locale) (@tr [:soon]))]]
         [ui/TableRow
          [ui/TableCell (str/capitalize (@tr [:updated]))]
          [ui/TableCell (if updated (time/ago (time/parse-iso8601 updated) @locale) (@tr [:soon]))]]
         (when id
           [ui/TableRow
            [ui/TableCell (str/capitalize (@tr [:id]))]
            [ui/TableCell [values/as-link id :label (subs id 11)]]])
         [apps-views-detail/AuthorVendor]]]]]]))


(defn DetailsPane
  []
  (let [active-index (subscribe [::apps-subs/active-tab-index])]
    @active-index
    ^{:key (random-uuid)}
    [apps-views-detail/Details
     {:validation-event
      ::apps-events/set-details-validation-error}]))


(defn OverviewPane
  []
  [ui/TabPane
   [ui/Grid {:columns   2,
             :stackable true
             :padded    true
             :centered  true}
    [ui/GridRow {:centered true}
     [ui/GridColumn
      [apps-views-detail/OverviewDescription utils/tab-details]]]
    [ui/GridRow
     [ui/GridColumn
      [modules-view]]]
    [ui/GridRow
     [ui/GridColumn
      [OverviewModuleSummary]]]]])


(defn overview
  []
  {:menuItem {:content (r/as-element [:span "Overview"])
              :key     "overview"
              :icon    "info"}
   :pane     {:key "overview" :content (r/as-element [OverviewPane])}})


(defn details
  []
  {:menuItem {:content (r/as-element [apps-views-detail/TabMenuDetails])
              :key     "details"}
   :pane     {:key "details-pane" :content (r/as-element [DetailsPane])}})


(defn module-detail-panes
  []
  (let [module    (subscribe [::apps-subs/module])
        editable? (subscribe [::apps-subs/editable?])]
    [(overview)
     (details)
     (apps-views-detail/TabAcls module @editable? #(do (dispatch [::apps-events/acl %])
                                                       (dispatch [::main-events/changes-protection? true])))]))


(defn ViewEdit
  []
  (let [module-common (subscribe [::apps-subs/module-common])
        active-index  (subscribe [::apps-subs/active-tab-index])
        is-new?       (subscribe [::apps-subs/is-new?])]
    (if (true? @is-new?) (dispatch [::apps-events/set-active-tab-index utils/tab-details])
                         (dispatch [::apps-events/set-active-tab-index 0]))
    (dispatch [::apps-events/set-form-spec ::spec/module-project])
    (fn []
      (let [name   (get @module-common ::apps-spec/name)
            parent (get @module-common ::apps-spec/parent-path)]
        [ui/Container {:fluid true}
         [uix/PageHeader "folder" (str parent (when (not-empty parent) "/") name) :inline true]
         [apps-views-detail/paste-modal]
         [apps-views-detail/MenuBar]
         [ui/Tab
          {:menu             {:secondary true
                              :pointing  true
                              :style     {:display        "flex"
                                          :flex-direction "row"
                                          :flex-wrap      "wrap"}}
           :panes            (module-detail-panes)
           :activeIndex      @active-index
           :renderActiveOnly false
           :onTabChange      (fn [_ data]
                               (let [active-index (. data -activeIndex)]
                                 (dispatch [::apps-events/set-active-tab-index active-index])))}]]))))
