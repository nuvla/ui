(ns sixsq.nuvla.ui.apps-application.views
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.apps-application.events :as events]
            [sixsq.nuvla.ui.apps-application.spec :as spec]
            [sixsq.nuvla.ui.apps-application.subs :as subs]
            [sixsq.nuvla.ui.apps.events :as apps-events]
            [sixsq.nuvla.ui.apps.spec :as apps-spec]
            [sixsq.nuvla.ui.apps.subs :as apps-subs]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.apps.views-detail :as apps-views-detail]
            [sixsq.nuvla.ui.apps.views-versions :as apps-views-versions]
            [sixsq.nuvla.ui.deployments.subs :as deployments-subs]
            [sixsq.nuvla.ui.deployments.views :as deployments-views]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.plugins.nav-tab :as nav-tab]
            [sixsq.nuvla.ui.profile.subs :as profile-subs]
            [sixsq.nuvla.ui.utils.form-fields :as ff]
            [sixsq.nuvla.ui.utils.forms :as utils-forms]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
            [sixsq.nuvla.ui.utils.values :as values]))

(def docker-docu-link "https://docs.docker.com/compose/compose-file/compose-file-v3/#not-supported-for-docker-stack-deploy")

(defn sub-apps-tab
  []
  (subscribe [::apps-subs/active-tab]))


(defn SingleFile
  #_{:clj-kondo/ignore [:unused-binding]}
  [{:keys [id ::spec/file-name ::spec/file-content]}]
  (let [tr              (subscribe [::i18n-subs/tr])
        validate-form?  (subscribe [::apps-subs/validate-form?])
        editable?       (subscribe [::apps-subs/editable?])
        form-valid?     (subscribe [::apps-subs/form-valid?])
        local-validate? (r/atom false)]
    (fn [{:keys [id ::spec/file-name ::spec/file-content]}]
      (let [validate? (or @local-validate? (not @form-valid?))]
        [ui/TableRow {:key id, :vertical-align "top"}
         (if @editable?
           [uix/TableRowCell {:key            (str "file-name-" id)
                              :placeholder    (@tr [:filename])
                              :editable?      editable?,
                              :spec           ::spec/file-name
                              :validate-form? @validate-form?
                              :required?      true
                              :default-value  (or file-name "")
                              :on-change      #(do
                                                 (reset! local-validate? true)
                                                 (dispatch [::events/update-file-name id %])
                                                 (dispatch [::main-events/changes-protection? true])
                                                 (dispatch [::apps-events/validate-form]))
                              :on-validation  ::events/set-configuration-validation-error}]
           [ui/TableCell {:floated :left
                          :width   3}
            [:span file-name]])
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


(defn FilesSection []
  (let [tr            (subscribe [::i18n-subs/tr])
        files         (subscribe [::subs/files])
        editable?     (subscribe [::apps-subs/editable?])
        module-app    (subscribe [::apps-subs/module])
        compatibility (:compatibility @module-app)]
    (fn []
      [uix/Accordion
       (if (not= compatibility "docker-compose")
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
                      [SingleFile file])]]])
          (when @editable?
            [:div {:style {:padding-top 10}}
             [apps-views-detail/plus ::events/add-file]])]
         [:div (@tr [:apps-file-config-warning])
          [:a {:href docker-docu-link} (str " " (@tr [:apps-file-config-warning-options-link]))]])
       :label (@tr [:module-files])
       :count (count @files)
       :default-open true])))


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
        editable?      (subscribe [::apps-subs/editable?])]
    (fn []
      (let [valid? (s/valid? ::spec/docker-compose @docker-compose)]
        [uix/Accordion
         [:<>
          [:div {:style {:margin-bottom "10px"}} "Env substitution"
           [:span ff/nbsp (ff/help-popup (@tr [:module-docker-compose-help]))]
           [DockerComposeCompatibility compatibility unsupp-opts]]
          [uix/EditorYaml {:value     @docker-compose
                           :on-change (fn [value]
                                        (dispatch [::events/update-docker-compose value])
                                        (dispatch [::main-events/changes-protection? true])
                                        (dispatch [::apps-events/validate-form]))
                           :read-only (not @editable?)}]
          (when @validate-form?
            (dispatch [::events/set-docker-validation-error
                       apps-views-detail/application-kubernetes-subtype (not valid?)])
            (when (not valid?)
              (let [error-msg (-> @docker-compose general-utils/check-yaml second)]
                [ui/Label {:pointing "above", :basic true, :color "red"}
                 (if (str/blank? error-msg)
                   (@tr [:module-k8s-manifest-error])
                   error-msg)])))]
         :label "Manifest"
         :default-open true]))))


(defn DockerComposeSection []
  (let [tr             (subscribe [::i18n-subs/tr])
        docker-compose (subscribe [::subs/docker-compose])
        module-app     (subscribe [::apps-subs/module])
        unsupp-opts    (:unsupported-options (:content @module-app))
        compatibility  (:compatibility @module-app)
        validate-form? (subscribe [::apps-subs/validate-form?])
        editable?      (subscribe [::apps-subs/editable?])]
    (fn []
      (let [validate-dc (subscribe [::apps-subs/validate-docker-compose])
            valid?      (s/valid? ::spec/docker-compose @docker-compose)]
        [uix/Accordion
         [:<>
          [:div {:style {:margin-bottom "10px"}} "Env substitution"
           [:span ff/nbsp (ff/help-popup (@tr [:module-docker-compose-help]))]
           [DockerComposeCompatibility compatibility unsupp-opts]]
          [uix/EditorYaml {:value     @docker-compose
                           :on-change (fn [value]
                                        (dispatch [::events/update-docker-compose value])
                                        (dispatch [::main-events/changes-protection? true])
                                        (dispatch [::apps-events/validate-form]))
                           :read-only (not @editable?)}]
          (when @validate-form?
            (dispatch [::events/set-docker-validation-error apps-views-detail/docker-compose-subtype (not valid?)])
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


(defn Tags
  [module]
  (let [tr   (subscribe [::i18n-subs/tr])
        tags (:tags module)]
    (when tags
      [ui/TableRow
       [ui/TableCell (str/capitalize (@tr [:tags]))]
       [ui/TableCell [apps-views-detail/Tags]]])))


(defn OverviewModuleSummary
  []
  (let [tr                   (subscribe [::i18n-subs/tr])
        locale               (subscribe [::i18n-subs/locale])
        module               (subscribe [::apps-subs/module])
        versions-map         (subscribe [::apps-subs/versions])
        module-content-id    (subscribe [::apps-subs/module-content-id])
        version-index        (apps-utils/find-current-version @versions-map @module-content-id)
        is-module-published? (subscribe [::apps-subs/is-module-published?])
        {:keys [id created updated name parent-path]} @module]
    [ui/Segment {:secondary true
                 :color     "blue"
                 :raised    true}
     [:h4 {:class "tab-app-detail"} (str/capitalize (@tr [:summary]))]
     [ui/Table {:basic  "very"
                :padded false}
      [ui/TableBody
       (when name
         [ui/TableRow
          [ui/TableCell (str/capitalize (@tr [:name]))]
          [ui/TableCell name]])
       (when parent-path
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
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:version-number]))]
        [ui/TableCell version-index " " (up-to-date? version-index @versions-map @is-module-published?)]]
       [apps-views-detail/AuthorVendor]
       [Tags @module]]]]))


(defn TabMenuDeployments
  []
  [:span
   [apps-views-detail/DeploymentsTitle]])


(defn DeploymentsPane
  []
  (let [tr      (subscribe [::i18n-subs/tr])
        is-new? (subscribe [::apps-subs/is-new?])]
    [:div {:class :uix-apps-details-deployments}
     [:h2 [apps-views-detail/DeploymentsTitle]]
     (if @is-new?
       [uix/WarningMsgNoElements]
       [deployments-views/DeploymentTable
        {:no-actions         true
         :no-module-name     true
         :empty-msg          (@tr [:empty-deployment-module-msg])
         :pagination-db-path ::spec/deployment-pagination
         :fetch-event        [::apps-events/get-deployments-for-module]}])]))


(defn TabMenuVersions
  []
  [:span
   [apps-views-versions/VersionsTitle]])


(defn VersionsPane
  []
  [apps-views-versions/Versions])


(defn TabMenuConfiguration
  []
  (let [error? (subscribe [::subs/configuration-error?])]
    [:span {:style {:color (if @error? utils-forms/dark-red "black")}}
     [apps-views-detail/ConfigurationTitle]]))


(defn ConfigurationPane
  []
  [:div {:class :uix-apps-details-configuration}
   [:h2 [apps-views-detail/ConfigurationTitle]]
   [apps-views-detail/EnvVariablesSection]
   [FilesSection]
   [apps-views-detail/UrlsSection]
   [apps-views-detail/OutputParametersSection]
   [apps-views-detail/DataTypesSection]])


(defn TabMenuLicense
  []
  (let [error? (subscribe [::subs/license-error?])]
    [:span {:style {:color (if @error? utils-forms/dark-red "black")}}
     [apps-views-detail/LicenseTitle]]))


(defn LicensePane
  []
  [apps-views-detail/LicenseSection])


(defn TabMenuPricing
  []
  [:span
   [apps-views-detail/PricingTitle]])


(defn PricingPane
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        editable? (subscribe [::apps-subs/editable?])
        price     (subscribe [::apps-subs/price])
        vendor    (subscribe [::profile-subs/vendor])]
    (fn []
      [:div {:class :uix-apps-details-pricing}
       [:h2 [apps-views-detail/PricingTitle]]
       (if (or (and @editable? @vendor) (some? @price))
         [apps-views-detail/Pricing]
         [:<>
          [ui/Message {:info true} (@tr [:no-pricing-free-app])]
          (when @editable?
            [ui/Message {:info true} (@tr [:become-a-vendor])])])])))


(defn TabMenuDocker
  []
  (let [error? (subscribe [::subs/docker-compose-validation-error?])]
    [:span {:style {:color (if @error? utils-forms/dark-red "black")}}
     [apps-views-detail/DockerTitle]]))


(defn DockerPane
  "Force the component to be re-mounted for each tab change by dereferencing active-index.
  This is required because:
  1. The CodeMirror component used must be reloaded to render correctly
  2. The component must be loaded even if not visible to run the validation"
  []
  (let [active-tab     (sub-apps-tab)
        module-subtype (subscribe [::apps-subs/module-subtype])]
    @active-tab
    [:div {:class :uix-apps-details-docker}
     [:h2 [apps-views-detail/DockerTitle]]
     [apps-views-detail/registries-section]
     ^{:key (random-uuid)}
     [:div
      (cond
        (= @module-subtype apps-views-detail/docker-compose-subtype) [DockerComposeSection]
        (= @module-subtype apps-views-detail/application-kubernetes-subtype) [KubernetesSection])]]))


(defn RequiresUserRightsCheckbox
  []
  (let [editable?   (subscribe [::apps-subs/editable?])
        tr          (subscribe [::i18n-subs/tr])
        user-rights (subscribe [::subs/module-requires-user-rights])
        default     @user-rights]
    (fn []
      [:div
       [ui/Checkbox {:name           (@tr [:module-requires-user-rights])
                     :toggle         true
                     :defaultChecked default
                     :disabled       (not @editable?)
                     :on-change      (ui-callback/checked
                                       #(do (dispatch [::main-events/changes-protection? true])
                                            (dispatch [::events/update-requires-user-rights %])))
                     :align          :middle}]])))


(defn subtype->pretty
  [subtype]
  (case subtype
    "application" "Docker"
    "application_kubernetes" "Kubernetes"
    "Docker"))


(defn DetailsPane []
  (let [tr             (subscribe [::i18n-subs/tr])
        module-subtype (subscribe [::apps-subs/module-subtype])
        active-tab     (sub-apps-tab)
        editable?      (subscribe [::apps-subs/editable?])]
    @active-tab
    ^{:key (random-uuid)}
    [apps-views-detail/Details
     {:extras           [^{:key "module_subtype"}
                         [ui/TableRow
                          [ui/TableCell {:collapsing true
                                         :style      {:padding-bottom 8}} "subtype"]
                          [ui/TableCell {:style
                                         {:padding-left (when @editable? apps-views-detail/edit-cell-left-padding)}}
                           (subtype->pretty @module-subtype)]]
                         ^{:key "nuvla-access"}
                         [ui/TableRow
                          [ui/TableCell {:collapsing true
                                         :style      {:padding-bottom 8}}
                           (@tr [:grant-nuvla-access]) ff/nbsp
                           [components/InfoPopup (@tr [:module-requires-user-rights])]]
                          [ui/TableCell [RequiresUserRightsCheckbox]]]]
      :validation-event ::apps-events/set-details-validation-error}]))


(defn TabMenuOverview
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [:span (str/capitalize (@tr [:overview]))]))


(defn OverviewPane
  []
  (let [device  (subscribe [::main-subs/device])
        is-new? (subscribe [::apps-subs/is-new?])]
    [ui/Grid {:columns   (if (contains? #{:wide-screen} @device) 2 1)
              :stackable true
              :padded    true
              :centered  true
              :class     :uix-apps-details-overview}
     [ui/GridRow {:centered true}
      (when (not @is-new?)
        [ui/GridColumn
         [deployments-views/DeploymentsOverviewSegment
          ::deployments-subs/deployments ::apps-events/set-active-tab :deployments]])]
     [ui/GridRow {:centered true}
      [ui/GridColumn
       [apps-views-detail/OverviewDescription]]]
     [ui/GridRow
      [ui/GridColumn
       [OverviewModuleSummary]]]]))


(defn module-detail-panes
  []
  (let [module    (subscribe [::apps-subs/module])
        editable? (subscribe [::apps-subs/editable?])
        stripe    (subscribe [::main-subs/stripe])]
    (remove nil? [{:menuItem {:content (r/as-element [TabMenuOverview])
                              :key     :overview
                              :icon    "fal fa-eye"}
                   :pane     {:content (r/as-element [OverviewPane])
                              :key     :overview-pane}}
                  {:menuItem {:content (r/as-element [apps-views-detail/TabMenuDetails])
                              :key     :details}
                   :pane     {:content (r/as-element [DetailsPane])
                              :key     :details-pane}}
                  {:menuItem {:content (r/as-element [TabMenuDeployments])
                              :key     :deployments}
                   :pane     {:content (r/as-element [DeploymentsPane])
                              :key     :deployments-pane}}
                  {:menuItem {:content (r/as-element [TabMenuLicense])
                              :key     :license}
                   :pane     {:content (r/as-element [LicensePane])
                              :key     :license-pane}}
                  (when @stripe
                    {:menuItem {:content (r/as-element [TabMenuPricing])
                                :key     :pricing}
                     :pane     {:content (r/as-element [PricingPane])
                                :key     :pricing-pane}})
                  {:menuItem {:content (r/as-element [TabMenuDocker])
                              :key     :docker}
                   :pane     {:content (r/as-element [DockerPane])
                              :key     :docker-pane}}
                  {:menuItem {:content (r/as-element [TabMenuConfiguration])
                              :key     :configuration}
                   :pane     {:content (r/as-element [ConfigurationPane])
                              :key     :configuration-pane}}
                  {:menuItem {:content (r/as-element [TabMenuVersions])
                              :key     :versions}
                   :pane     {:content (r/as-element [VersionsPane])
                              :key     :versions-pane}}
                  (apps-views-detail/TabAcls
                    module
                    @editable?
                    #(do (dispatch [::apps-events/acl %])
                         (dispatch [::main-events/changes-protection? true])))])))


(defn ViewEdit
  []
  (let [module-common (subscribe [::apps-subs/module-common])
        active-tab    (sub-apps-tab)
        is-new?       (subscribe [::apps-subs/is-new?])]
    (dispatch [::apps-events/init-view {:tab-key (when (true? @is-new?) :details)}])
    (dispatch [::apps-events/reset-version])
    (dispatch [::apps-events/set-form-spec ::spec/module-application])
    (fn []
      (when @active-tab (dispatch [::apps-events/set-default-tab @active-tab]))
      (let [name   (get @module-common ::apps-spec/name)
            parent (get @module-common ::apps-spec/parent-path)
            panes  (module-detail-panes)]
        [ui/Container {:fluid true
                       :class :uix-apps-details}
         [uix/PageHeader "fal fa-layer-group" (str parent (when (not-empty parent) "/") name) :inline true]
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
