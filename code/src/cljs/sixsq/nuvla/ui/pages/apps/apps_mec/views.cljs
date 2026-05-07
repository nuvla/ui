(ns sixsq.nuvla.ui.pages.apps.apps-mec.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.plugins.nav-tab :as nav-tab]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.pages.apps.apps-application.views :as apps-application-views]
            [sixsq.nuvla.ui.pages.apps.events :as apps-events]
            [sixsq.nuvla.ui.pages.apps.spec :as apps-spec]
            [sixsq.nuvla.ui.pages.apps.subs :as apps-subs]
            [sixsq.nuvla.ui.pages.apps.views-detail :as apps-views-detail]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))

(defn descriptor-summary-row
  [label value]
  (when (some? value)
    [ui/TableRow
     [ui/TableCell (str/capitalize label)]
     [ui/TableCell value]]))

(defn MecDescriptorSummary
  []
  (let [module (subscribe [::apps-subs/module])]
    (fn []
      (let [{:keys [appDId appDVersion appName appProvider appSoftVersion mecVersion
                    swImageDescriptor appServiceRequired trafficRuleDescriptor dnsRuleDescriptor]} (:content @module)]
        [ui/Segment {:secondary true}
         [:h4 {:class "tab-app-detail"} "ETSI MEC"]
         [ui/Table {:basic  "very"
                    :padded false}
          [ui/TableBody
           [descriptor-summary-row "AppD ID" appDId]
           [descriptor-summary-row "AppD version" appDVersion]
           [descriptor-summary-row "App name" appName]
           [descriptor-summary-row "App provider" appProvider]
           [descriptor-summary-row "App software version" appSoftVersion]
           [descriptor-summary-row "MEC version" mecVersion]
           [descriptor-summary-row "Software images" (some-> swImageDescriptor count)]
           [descriptor-summary-row "Required MEC services" (some-> appServiceRequired count)]
           [descriptor-summary-row "Traffic rules" (some-> trafficRuleDescriptor count)]
           [descriptor-summary-row "DNS rules" (some-> dnsRuleDescriptor count)]]]]))))

(defn DetailsPane
  []
  [apps-views-detail/Details
   {:extras           [^{:key "module_subtype"}
                       [apps-views-detail/SubtypeRow]]
    :validation-event ::apps-events/set-details-validation-error}])

(defn OverviewPane
  []
  [:<>
   [apps-application-views/OverviewPane]
   [MecDescriptorSummary]])

(defn AppDPane
  []
  (let [appd-json      (subscribe [::apps-subs/mec-appd-json])
        appd-valid?    (subscribe [::apps-subs/mec-appd-json-valid?])
        validate-form? (subscribe [::apps-subs/validate-form?])
        editable?      (subscribe [::apps-subs/editable?])]
    (fn []
      [:div {:class :uix-apps-details-details}
       [:h4 {:class :tab-app-detail} "AppD JSON"]
       [ui/Message {:info true}
        "This editor stores the ETSI MEC AppD payload that will be sent as the MEC module content."]
       [uix/EditorJson {:value     @appd-json
                        :on-change #(do (dispatch [::apps-events/set-mec-appd-json %])
                                        (dispatch [::main-events/changes-protection? true]))
                        :read-only (not @editable?)}]
       (when (and @validate-form? (not @appd-valid?))
         [ui/Label {:pointing "above" :basic true :color "red"}
          "The AppD JSON must be valid JSON."])])))

(defn TabMenuAppD
  []
  (let [appd-valid? (subscribe [::apps-subs/mec-appd-json-valid?])]
    [:span {:style {:color (if @appd-valid? "black" "#9f3a38")}}
     [icons/FileCodeIcon]
     "AppD"]))

(defn module-detail-panes
  []
  (let [module    (subscribe [::apps-subs/module])
        editable? (subscribe [::apps-subs/editable?])]
    [{:menuItem {:content (r/as-element [apps-application-views/TabMenuOverview])
                 :key     :overview
                 :icon    (r/as-element [icons/EyeIcon])}
      :pane     {:content (r/as-element [OverviewPane])
                 :key     :overview-pane}}
     {:menuItem {:content (r/as-element [apps-views-detail/TabMenuDetails])
                 :key     :details}
      :pane     {:content (r/as-element [DetailsPane])
                 :key     :details-pane}}
     {:menuItem {:content (r/as-element [TabMenuAppD])
                 :key     :configuration}
      :pane     {:content (r/as-element [AppDPane])
                 :key     :configuration-pane}}
     {:menuItem {:content (r/as-element [apps-application-views/TabMenuDeployments])
                 :key     :deployments}
      :pane     {:content (r/as-element [apps-application-views/DeploymentsPane])
                 :key     :deployments-pane}}
     {:menuItem {:content (r/as-element [apps-application-views/TabMenuVersions])
                 :key     :versions}
      :pane     {:content (r/as-element [apps-application-views/VersionsPane])
                 :key     :versions-pane}}
     (apps-views-detail/TabAcls
       module
       @editable?
       #(do (dispatch [::apps-events/acl %])
            (dispatch [::main-events/changes-protection? true])))]))

(defn ViewEdit
  []
  (let [module-common (subscribe [::apps-subs/module-common])
        active-tab    (subscribe [::apps-subs/active-tab])
        is-new?       (subscribe [::apps-subs/is-new?])]
    (dispatch [::apps-events/init-view {:tab-key (if (true? @is-new?) :details :overview)}])
    (dispatch [::apps-events/set-form-spec nil])
    (fn []
      (when @active-tab
        (dispatch [::apps-events/set-default-tab @active-tab]))
      (let [name  (get @module-common ::apps-spec/name)
            panes (module-detail-panes)]
        [ui/Container {:fluid true
                       :class :uix-apps-details}
         [uix/PageHeader icons/i-layer-group name :inline true]
         [apps-views-detail/MenuBar]
         [nav-tab/Tab
          {:db-path                 [::apps-spec/tab]
           :menu                    {:secondary true
                                     :pointing  true
                                     :style     {:display        "flex"
                                                 :flex-direction "row"
                                                 :flex-wrap      "wrap"}
                                     :class     :uix-tab-nav}
           :panes                   panes
           :renderActiveOnly        false
           :ignore-chng-protection? true}]]))))
