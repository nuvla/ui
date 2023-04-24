(ns sixsq.nuvla.ui.apps-project.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.apps-project.spec :as spec]
            [sixsq.nuvla.ui.apps.events :as apps-events]
            [sixsq.nuvla.ui.apps.spec :as apps-spec]
            [sixsq.nuvla.ui.apps.subs :as apps-subs]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.apps.views-detail :as apps-views-detail]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.plugins.nav-tab :as nav-tab]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.utils :refer [name->href str-pathify]]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.values :as values]))


(defn FormatModule
  [{:keys [subtype name path description] :as module}]
  (when module
    (let [on-click  #(dispatch [::routing-events/navigate
                                (str-pathify (name->href routes/apps) path)])
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


(defn FormatModuleChildren
  [module-children]
  (when (pos? (count module-children))
    (let [ordered-children (sort-by :name module-children)]
      [ui/Segment style/basic
       (vec (concat [ui/ListSA {:divided   true
                                :relaxed   true
                                :selection true}]
                    (map (fn [{:keys [id] :as module}]
                           ^{:key id}
                           [FormatModule module]) ordered-children)))])))


(defn ModulesView []
  (let [tr     (subscribe [::i18n-subs/tr])
        module (subscribe [::apps-subs/module])]
    (fn []
      (let [children (:children @module)]
        [ui/Segment {:secondary true
                     :color     "blue"
                     :raised    true}
         [:h4 (str/capitalize (@tr [:content]))]
         (if (empty? children)
           [ui/Message {:warning true}
            [ui/Icon {:name "warning sign"}]
            (@tr [:no-children-modules])]
           [FormatModuleChildren children])]))))


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
            [ui/TableCell [values/AsLink path :label name :page "apps"]]])
         (when (seq parent-path)
           [ui/TableRow
            [ui/TableCell (str/capitalize (@tr [:project]))]
            [ui/TableCell [values/AsLink parent-path :label parent-path :page "apps"]]])
         [ui/TableRow
          [ui/TableCell (str/capitalize (@tr [:created]))]
          [ui/TableCell (if created (time/ago (time/parse-iso8601 created) @locale) (@tr [:soon]))]]
         [ui/TableRow
          [ui/TableCell (str/capitalize (@tr [:updated]))]
          [ui/TableCell (if updated (time/ago (time/parse-iso8601 updated) @locale) (@tr [:soon]))]]
         (when id
           [ui/TableRow
            [ui/TableCell (str/capitalize (@tr [:id]))]
            [ui/TableCell [values/AsLink id :label (general-utils/id->uuid id)]]])
         [apps-views-detail/AuthorVendor]]]]]]))

(defn- sub-apps-projects-tab
  []
  (subscribe [::apps-subs/active-tab [::spec/tab]]))

(defn DetailsPane
  []
  (let [active-tab (sub-apps-projects-tab)]
    @active-tab
    ^{:key (random-uuid)}
    [apps-views-detail/Details
     {:validation-event
      ::apps-events/set-details-validation-error}]))


(defn OverviewPane
  []
  (let [device (subscribe [::main-subs/device])]
    [ui/TabPane
     [ui/Grid {:columns   (if (contains? #{:wide-screen} @device) 2 1)
               :stackable true
               :padded    true
               :centered  true}
      [ui/GridRow {:centered true}
       [ui/GridColumn
        [apps-views-detail/OverviewDescription [::spec/tab]]]]
      [ui/GridRow
       [ui/GridColumn
        [ModulesView]]]
      [ui/GridRow
       [ui/GridColumn
        [OverviewModuleSummary]]]]]))


(defn module-detail-panes
  []
  (let [module    (subscribe [::apps-subs/module])
        editable? (subscribe [::apps-subs/editable?])]
    [{:menuItem {:content (r/as-element [:span "Overview"])
                 :key     :overview
                 :icon    "info"}
      :pane     {:content (r/as-element [OverviewPane])
                 :key     :overview-pane}}
     {:menuItem {:content (r/as-element [apps-views-detail/TabMenuDetails])
                 :key     :details}
      :pane     {:content (r/as-element [DetailsPane])
                 :key     :details-pane}}
     (apps-views-detail/TabAcls module @editable? #(do (dispatch [::apps-events/acl %])
                                                       (dispatch [::main-events/changes-protection? true])))]))


(defn ViewEdit
  []
  (let [module-common (subscribe [::apps-subs/module-common])
        is-new?       (subscribe [::apps-subs/is-new?])]
    (dispatch [::apps-events/init-view {:tab-key (if (true? @is-new?) :details :overview)
                                        :db-path [::spec/tab]}])
    (dispatch [::apps-events/set-form-spec ::spec/module-project])
    (fn []
      (let [name   (get @module-common ::apps-spec/name)
            parent (get @module-common ::apps-spec/parent-path)
            panes  (module-detail-panes)]
        [ui/Container {:fluid true}
         [uix/PageHeader "folder" (str parent (when (not-empty parent) "/") name) :inline true]
         [apps-views-detail/paste-modal]
         [apps-views-detail/MenuBar]
         [nav-tab/Tab
          {:db-path          [::spec/tab]
           :menu             {:secondary true
                              :pointing  true
                              :style     {:display        "flex"
                                          :flex-direction "row"
                                          :flex-wrap      "wrap"}}
           :panes            panes
           :renderActiveOnly false}]])
      )))
