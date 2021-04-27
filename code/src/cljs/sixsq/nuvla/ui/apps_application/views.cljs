(ns sixsq.nuvla.ui.apps-application.views
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [markdown-to-hiccup.core :as md]
    [sixsq.nuvla.ui.acl.views :as acl]
    [sixsq.nuvla.ui.apps-application.events :as events]
    [sixsq.nuvla.ui.apps-application.spec :as spec]
    [sixsq.nuvla.ui.apps-application.subs :as subs]
    [sixsq.nuvla.ui.apps-application.utils :as utils]
    [sixsq.nuvla.ui.apps.events :as apps-events]
    [sixsq.nuvla.ui.apps.spec :as apps-spec]
    [sixsq.nuvla.ui.apps.subs :as apps-subs]
    [sixsq.nuvla.ui.apps.views-versions :as apps-views-versions]
    [sixsq.nuvla.ui.apps.views-detail :as apps-views-detail]
    [sixsq.nuvla.ui.deployment.views :as deployment-views]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.profile.subs :as profile-subs]
    [sixsq.nuvla.ui.utils.form-fields :as ff]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.values :as values]
    [sixsq.nuvla.ui.utils.time :as time]
    [clojure.string :as str]
    [sixsq.nuvla.ui.apps.utils :as apps-utils]
    [taoensso.timbre :as log]
    [sixsq.nuvla.ui.utils.forms :as utils-forms]))


(def application-kubernetes-subtype "application_kubernetes")
(def docker-compose-subtype "application")


(defn clear-module
  []
  (dispatch [::events/clear-module]))


(defn single-file [{:keys [id ::spec/file-name ::spec/file-content]}]
  (let [form-valid?     (subscribe [::apps-subs/form-valid?])
        editable?       (subscribe [::apps-subs/editable?])
        local-validate? (r/atom false)]
    (fn [{:keys [id ::spec/file-name ::spec/file-content]}]
      (let [validate? (or @local-validate? (not @form-valid?))]
        [ui/TableRow {:key id, :vertical-align "top"}
         [ui/TableCell {:floated :left
                        :width   3}
          (if @editable?
            [apps-views-detail/input id (str "file-name-" id) file-name
             "file-name" ::events/update-file-name
             ::spec/file-name true]
            [:span file-name])]
         [ui/TableCell {:floated :left
                        :width   12
                        :error   (and validate? (not (s/valid? ::spec/file-content file-content)))}
          [ui/Form
           [ui/TextArea {:rows          10
                         :read-only     (not @editable?)
                         :default-value file-content
                         :on-change     (ui-callback/value
                                          #(do
                                             (reset! local-validate? true)
                                             (dispatch [::events/update-file-content id %])
                                             (dispatch [::main-events/changes-protection? true])
                                             (dispatch [::apps-events/validate-form])))}]]]
         (when @editable?
           [ui/TableCell {:floated :right
                          :width   1
                          :align   :right}
            [apps-views-detail/trash id ::events/remove-file]])]))))


(defn files-section []
  (let [tr            (subscribe [::i18n-subs/tr])
        files         (subscribe [::subs/files])
        editable?     (subscribe [::apps-subs/editable?])
        module-app    (subscribe [::apps-subs/module])
        compatibility (:compatibility @module-app)]
    (when (not= compatibility "docker-compose")
      (fn []
        [uix/Accordion
         [:<>
          [:div (@tr [:module-files])
           [:span ff/nbsp (ff/help-popup (@tr [:module-files-help]))]]
          (if (empty? @files)
            [ui/Message
             (str/capitalize (str (@tr [:no-files]) "."))]
            [:div [ui/Table {:style {:margin-top 10}}
                   [ui/TableHeader
                    [ui/TableRow
                     [ui/TableHeaderCell {:content (str/capitalize (@tr [:filename]))}]
                     [ui/TableHeaderCell {:content (str/capitalize (@tr [:content]))}]
                     (when @editable?
                       [ui/TableHeaderCell {:content (str/capitalize (@tr [:action]))}])]]
                   [ui/TableBody
                    (for [[id file] @files]
                      ^{:key (str "file_" id)}
                      [single-file file])]]])
          (when @editable?
            [:div {:style {:padding-top 10}}
             [apps-views-detail/plus ::events/add-file]])]
         :label (@tr [:module-files])
         :count (count @files)
         :default-open true]))))


(defn DockerComposeValidationPopup
  [{:keys [loading? valid? error-msg]}]
  [ui/Popup
   {:trigger        (r/as-element [ui/Icon {:name    (cond
                                                       loading? "circle notched"
                                                       (not valid?) "bug"
                                                       valid? "checkmark")
                                            :color   (cond
                                                       loading? "black"
                                                       (not valid?) "red"
                                                       valid? "green")
                                            :loading loading?}])
    :header         "Validation of docker-compose"
    :content        (cond
                      loading? "Validation of docker-compose in progress..."
                      (not valid?) (some-> error-msg (str/replace #"^.*is invalid because:" ""))
                      valid? "Docker-compose is valid :)")
    :on             "hover"
    :position       "top center"
    :wide           true
    :hide-on-scroll true}])


(defn DockerComposeCompatibility
  [compatibility unsupp-opts]
  (let [popup-disabled? (empty? unsupp-opts)]
    [:div {:style {:float "right"}}
     [:span {:style {:font-variant "small-caps"}} "compatibility: "]
     [ui/Label {:color      "blue"
                :horizontal true} compatibility]
     (when-not popup-disabled?
       [ui/Popup
        {:trigger        (r/as-element [ui/Icon {:color "yellow"
                                                 :name  "exclamation triangle"}])
         :header         "Unsupported options"
         :content        (str "Swarm doesn't support and will ignore the following options: "
                              (str/join "; " unsupp-opts))

         :on             "hover"
         :position       "top right"
         :wide           true
         :hide-on-scroll true}])]))


(defn KubernetesSection []
  (let [tr             (subscribe [::i18n-subs/tr])
        docker-compose (subscribe [::subs/docker-compose])
        module-app     (subscribe [::apps-subs/module])
        unsupp-opts    (:unsupported-options (:content @module-app))
        compatibility  (:compatibility @module-app)
        validate-form? (subscribe [::apps-subs/validate-form?])
        editable?      (subscribe [::apps-subs/editable?])
        default-value  @docker-compose]
    (fn []
      (let [validate-dc (subscribe [::apps-subs/validate-docker-compose])
            validate?   @validate-form?
            valid?      (s/valid? ::spec/docker-compose @docker-compose)]
        [uix/Accordion
         [:<>
          [:div {:style {:margin-bottom "10px"}} "Env substitution"
           [:span ff/nbsp (ff/help-popup (@tr [:module-docker-compose-help]))]
           [DockerComposeCompatibility compatibility unsupp-opts]]
          [uix/EditorYaml
           default-value
           (fn [editor data value]
             (dispatch [::events/update-docker-compose value])
             (dispatch [::main-events/changes-protection? true])
             (dispatch [::apps-events/validate-form]))
           @editable?]
          (when validate?
            (dispatch [::events/set-docker-validation-error application-kubernetes-subtype (not valid?)])
            (when (not valid?)
              (let [error-msg (-> @docker-compose general-utils/check-yaml second)]
                [ui/Label {:pointing "above", :basic true, :color "red"}
                 (if (str/blank? error-msg)
                   (@tr [:module-k8s-manifest-error])
                   error-msg)])))]
         :label "Manifes"
         :default-open true]))))


(defn DockerComposeSection []
  (let [tr             (subscribe [::i18n-subs/tr])
        docker-compose (subscribe [::subs/docker-compose])
        module-app     (subscribe [::apps-subs/module])
        unsupp-opts    (:unsupported-options (:content @module-app))
        compatibility  (:compatibility @module-app)
        validate-form? (subscribe [::apps-subs/validate-form?])
        editable?      (subscribe [::apps-subs/editable?])
        default-value  @docker-compose]
    (fn []
      (let [validate-dc (subscribe [::apps-subs/validate-docker-compose])
            validate?   @validate-form?
            valid?      (s/valid? ::spec/docker-compose @docker-compose)]
        [uix/Accordion
         [:<>
          [:div {:style {:margin-bottom "10px"}} "Env substitution"
           [:span ff/nbsp (ff/help-popup (@tr [:module-docker-compose-help]))]
           [DockerComposeCompatibility compatibility unsupp-opts]]
          [uix/EditorYaml
           default-value
           (fn [editor data value]
             (dispatch [::events/update-docker-compose value])
             (dispatch [::main-events/changes-protection? true])
             (dispatch [::apps-events/validate-form]))
           @editable?]
          (when validate?
            (dispatch [::events/set-docker-validation-error docker-compose-subtype (not valid?)])
            (when (not valid?)
              (let [error-msg (-> @docker-compose general-utils/check-yaml second)]
                [ui/Label {:pointing "above", :basic true, :color "red"}
                 (if (str/blank? error-msg)
                   (@tr [:module-docker-compose-error])
                   error-msg)])))]
         :label [:span "Docker compose" ff/nbsp
                 (when @validate-dc
                   [DockerComposeValidationPopup @validate-dc])]
         :default-open true]))))


(defn up-to-date?
  [v versions published?]
  (when v
    (let [tr                     (subscribe [::i18n-subs/tr])
          last-version           (ffirst versions)
          last-published-version (apps-utils/latest-published-index versions)]
      (if published?
        (if (= v last-published-version)
          [:span [ui/Icon {:name "check", :color "green"}] " (" (@tr [:up-to-date-published]) ")"]
          [:span [ui/Icon {:name "warning", :color "orange"}]
           (str (@tr [:not-up-to-date-published]))])
        (if (= v last-version)
          [:span [ui/Icon {:name "check", :color "green"}] " (" (@tr [:up-to-date-latest]) ")"]
          [:span [ui/Icon {:name "warning", :color "orange"}]
           (str " (" (@tr [:behind-version-1]) " " (- last-version v) " " (@tr [:behind-version-2]) ")")])))))


(defn OverviewModuleSummary
  "Fixme: add license and price"
  []
  (let [tr                     (subscribe [::i18n-subs/tr])
        locale                 (subscribe [::i18n-subs/locale])
        module                 (subscribe [::apps-subs/module])
        versions-map           (subscribe [::apps-subs/versions])
        module-content-id      (subscribe [::apps-subs/module-content-id])
        version-index          (apps-utils/find-current-version @versions-map @module-content-id)
        is-module-published?   (subscribe [::apps-subs/is-module-published?])
        {:keys [id created updated name parent-path path logo-url]} @module]
    [ui/Segment {:secondary true
                 :color     "blue"
                 :raised    true}
     [:h4 (str/capitalize (@tr [:module]))]
     [ui/Grid {:columns 2}
      [ui/GridColumn
       [ui/Table {:basic  "very"
                  :padded false}
        [ui/TableBody
         (when name
           [ui/TableRow
            [ui/TableCell (str/capitalize (@tr [:name]))]
            [ui/TableCell [values/as-link path :label name :page "apps"]]])
         (when parent-path
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
         [ui/TableRow
          [ui/TableCell (str/capitalize (@tr [:version-number]))]
          [ui/TableCell version-index " " (up-to-date? version-index @versions-map @is-module-published?)]]]]]
      [ui/GridColumn
       [ui/Segment (merge style/basic {:floated "right"})
        [ui/Image {:src      (or logo-url "")
                   :bordered true
                   :style    {:object-fit "contain"}}]]]]]))


(defn TabMenuDeployments
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [:span
     (str/capitalize (@tr [:deployments]))]))


(defn DeploymentsPane
  []
  (let [is-new? (subscribe [::apps-subs/is-new?])]
    (if @is-new?
      [uix/WarningMsgNoElements]
      [deployment-views/DeploymentTable {:no-actions     true
                                         :no-selection   true
                                         :no-module-name true}])))


(defn deployments
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    {:menuItem {:content (r/as-element [TabMenuDeployments])
                :key     "deployments"
                :icon    "rocket"}
     :pane     {:key "deplyment-pane" :content (r/as-element [DeploymentsPane])}}))


(defn TabMenuVersions
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [:span
     (str/capitalize (@tr [:versions]))]))


(defn VersionsPane
  []
  [apps-views-versions/Versions])


(defn versions
  []
  (let []
    {:menuItem {:content (r/as-element [TabMenuVersions])
                :key     "versions"
                :icon    "tag"}
     :pane     {:key "versions-pane" :content (r/as-element [VersionsPane])}}))


(defn TabMenuConfiguration
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [:span
     (str/capitalize (@tr [:configuration]))]))


(defn ConfigurationPane
  []
  (let []
    [:<>
     [apps-views-detail/env-variables-section]
     [files-section]
     [apps-views-detail/urls-section]
     [apps-views-detail/output-parameters-section]
     [apps-views-detail/data-types-section]]))


(defn configuration
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    {:menuItem {:content (r/as-element [TabMenuConfiguration])
                :key     "configuration"
                :icon    "cog"}
     :pane     {:key "configuration-pane" :content (r/as-element [ConfigurationPane])}}))


(defn TabMenuLicense
  []
  (let [tr     (subscribe [::i18n-subs/tr])
        error? (subscribe [::subs/license-error?])]
    [:span {:style {:color (if (true? @error?) utils-forms/dark-red "black")}}
     [uix/Icon {:name "drivers license"}]
     (str/capitalize (@tr [:license]))]))


(defn LicensePane
  []
  [apps-views-detail/LicenseSection])


(defn license
  []
  {:menuItem {:content (r/as-element [TabMenuLicense])
              :key     "license"}
   :pane     {:key "license-pane" :content (r/as-element [LicensePane])}})


(defn TabMenuPricing
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [:span
     (str/capitalize (@tr [:pricing]))]))


(defn PricingPane
  []
  (let [tr     (subscribe [::i18n-subs/tr])
        stripe (subscribe [::main-subs/stripe])
        vendor (subscribe [::profile-subs/vendor])]
    (if (and @stripe @vendor)
      [apps-views-detail/price-section]
      [ui/Message {:info true} (@tr [:no-pricing-free-app])])))


(defn pricing
  []
  (let []
    {:menuItem {:content (r/as-element [TabMenuPricing])
                :key     "pricing"
                :icon    "euro"}
     :pane     {:key "pricing-pane" :content (r/as-element [PricingPane])}}))


(defn TabMenuDocker
  []
  (let [module-subtype (subscribe [::apps-subs/module-subtype])
        error?         (subscribe [::subs/docker-compose-validation-error?])
        tab-name       (if (= application-kubernetes-subtype @module-subtype) "Kubernetes" "Docker")]
    [:span {:style {:color (if (true? @error?) utils-forms/dark-red "black")}}
     [uix/Icon {:name "docker"}]
     tab-name]))


(defn DockerPane
  "Force the component to be re-mounted for each tab change by dereferencing active-index.
  This is required because:
  1. The CodeMirror component used must be reloaded to render correctly
  2. The component must be loaded even if not visible to run the validation"
  []
  (let [active-index   (subscribe [::apps-subs/active-tab-index])
        module-subtype (subscribe [::apps-subs/module-subtype])]
    @active-index
    [:<>
     [apps-views-detail/registries-section]
     ^{:key (random-uuid)}
     [:div
      (cond
        (= @module-subtype docker-compose-subtype) [DockerComposeSection]
        (= @module-subtype application-kubernetes-subtype) [KubernetesSection])]]))


(defn docker
  []
  {:menuItem {:content (r/as-element [TabMenuDocker])
              :key     "docker"}
   :pane     {:key "docker-pane" :content (r/as-element [DockerPane])}})


(defn TabMenuDetails
  []
  (let [tr     (subscribe [::i18n-subs/tr])
        error? (subscribe [::subs/details-validation-error?])]
    [:span {:style {:color (if (true? @error?) utils-forms/dark-red "black")}}
     [uix/Icon {:name "info"}]
     (str/capitalize (@tr [:details]))]))


(defn DetailsPane []
  (let [is-new?        (subscribe [::apps-subs/is-new?])
        module-subtype (subscribe [::apps-subs/module-subtype])
        active-index   (subscribe [::apps-subs/active-tab-index])]
    @active-index
    ^{:key (random-uuid)}
    [apps-views-detail/Details
     [^{:key "module_subtype"}
      [ui/TableRow
       [ui/TableCell {:collapsing true
                      :style      {:padding-bottom 8}} "subtype"]
       [ui/TableCell
        [ui/Dropdown {:disabled  (-> @is-new? true? not)
                      :selection true
                      :fluid     true
                      :value     @module-subtype
                      :on-change (ui-callback/value
                                   #(do (dispatch [::apps-events/subtype %])
                                        (dispatch [::main-events/changes-protection? true])))
                      :options   [{:key docker-compose-subtype, :text "Docker", :value docker-compose-subtype}
                                  {:key   application-kubernetes-subtype, :text "Kubernetes",
                                   :value application-kubernetes-subtype}]}]]]]
     ::apps-events/set-details-validation-error]))


(defn details
  []
  (let []
    {:menuItem {:content (r/as-element [apps-views-detail/TabMenuDetails])
                :key     "details"}
     :pane     {:key "details-pane" :content (r/as-element [DetailsPane])}}))


(defn TabMenuOverview
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [:span (str/capitalize (@tr [:overview]))]))


(defn OverviewPane
  []
  [ui/Grid {:columns   2,
            :stackable true
            :padded    true
            :centered  true}
   [ui/GridRow {:centered true}
    [ui/GridColumn
     [OverviewModuleSummary]]]
   [ui/GridRow
    [ui/GridColumn
     [apps-views-detail/OverviewDescription utils/tab-details]]]
   [ui/GridRow
    [ui/GridColumn
     [apps-views-detail/OverviewVendorSummary]]]])


(defn overview
  []
  (let []
    {:menuItem {:content (r/as-element [TabMenuOverview])
                :key     "overview"
                :icon    "info"}
     :pane     {:key "overview-pane" :content (r/as-element [OverviewPane])}}))


(defn module-detail-panes
  []
  (let [module    (subscribe [::apps-subs/module])
        editable? (subscribe [::apps-subs/editable?])]
    [(overview)
     (license)
     (pricing)
     (deployments)
     (versions)
     (details)
     (docker)
     (configuration)
     (apps-views-detail/TabAcls module @editable? #(do (dispatch [::apps-events/acl %])
                                                      (dispatch [::main-events/changes-protection? true])))]))


(defn ViewEdit
  []
  (let [module-common (subscribe [::apps-subs/module-common])
        active-index  (subscribe [::apps-subs/active-tab-index])
        is-new?       (subscribe [::apps-subs/is-new?])]
    (if (true? @is-new?) (dispatch [::apps-events/set-active-tab-index utils/tab-details])
                         (dispatch [::apps-events/set-active-tab-index 0]))
    (dispatch [::apps-events/reset-version])
    (dispatch [::apps-events/set-form-spec ::spec/module-application])
    (fn []
      (let [name   (get @module-common ::apps-spec/name)
            parent (get @module-common ::apps-spec/parent-path)]
        [ui/Container {:fluid true}
         [uix/PageHeader "cubes" (str parent (when (not-empty parent) "/") name) :inline true]
         [apps-views-detail/VersionWarning]
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
