(ns sixsq.nuvla.ui.apps-applications-set.views
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.apps-applications-set.events :as events]
    [sixsq.nuvla.ui.apps-applications-set.spec :as spec]
    [sixsq.nuvla.ui.apps-applications-set.subs :as subs]
    [sixsq.nuvla.ui.apps.events :as apps-events]
    [sixsq.nuvla.ui.apps.spec :as apps-spec]
    [sixsq.nuvla.ui.apps.subs :as apps-subs]
    [sixsq.nuvla.ui.apps.utils :as apps-utils]
    [sixsq.nuvla.ui.apps.views-detail :as apps-views-detail]
    [sixsq.nuvla.ui.apps.views-versions :as apps-views-versions]
    [sixsq.nuvla.ui.deployments.subs :as deployments-subs]
    [sixsq.nuvla.ui.deployments.views :as deployments-views]
    [sixsq.nuvla.ui.deployment-sets-detail.views :as deployment-sets-detail-views]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.plugins.tab :as tab-plugin]
    [sixsq.nuvla.ui.profile.subs :as profile-subs]
    [sixsq.nuvla.ui.utils.form-fields :as ff]
    [sixsq.nuvla.ui.utils.forms :as utils-forms]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.tab :as tab]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as values]))

(def docker-docu-link "https://docs.docker.com/compose/compose-file/compose-file-v3/#not-supported-for-docker-stack-deploy")

(defn clear-module
  []
  (dispatch [::events/clear-module]))


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


(defn SelectAppsModal
  []
  (let [subtype (r/atom nil)]
    (fn []
      [ui/Modal {:on-close #(reset! subtype nil)
                 :close-icon true
                 :trigger (r/as-element
                            [ui/Icon {:name     "add"
                                      :color    "green"}])}
       [ui/ModalHeader "New apps set"]
       [ui/ModalContent
        (if (nil? @subtype)
          [:<>
           [:p "Targeting Docker or Kubernetes"]
           [ui/CardGroup {:centered true}
            [ui/Card
             {:on-click #(reset! subtype "docker")}
             [ui/CardContent {:text-align :center}
              [ui/IconGroup {:size :massive}
               [ui/Icon {:name "docker"}]]]
             ]
            [ui/Card
             {:on-click #(reset! subtype "kubernetes")}
             [ui/CardContent {:text-align :center}
              [ui/IconGroup {:size :massive}
               [ui/Image {:src   "/ui/images/kubernetes.svg"
                          ;:floated "right"
                          :style {:width "100px"}
                          }]]]

             ]]]
          [deployment-sets-detail-views/SelectApps]
          )
        ]
       (when @subtype
         [ui/ModalActions
         [ui/Button {:primary true}
          "Validate"]])
       ]
      )
    )
  )

(defn AppGroupSection []
  (let [tr             (subscribe [::i18n-subs/tr])
        apps-groups    (subscribe [::subs/apps-groups])
        editable?      (subscribe [::apps-subs/editable?])
        validate-form? (subscribe [::apps-subs/validate-form?])
        on-change      (fn [id name]
                         (dispatch [::events/update-apps-set-name id name])
                         (dispatch [::main-events/changes-protection? true])
                         (dispatch [::apps-events/validate-form]))]
    (fn []
      [:<>
       [:div "Applications grouping"
        [:span ff/nbsp (ff/help-popup "Each of this applications group you will be able to deploy it on a targets")]]
       (if (empty? @apps-groups)
         [ui/Message "No applications group to show"]
         [:<>
          (for [[i [id {:keys [::spec/apps-group-name] :as apps-group}]] (map-indexed vector @apps-groups)]
            ^{:key (str "apps-group-" (inc i))}
            [uix/Accordion
             [:<>
              [ui/Table {:compact    true
                         :definition true}
               [ui/TableBody
                [uix/TableRowField (@tr [:name]), :key "app-group-name-", :editable? @editable?,
                 :spec ::spec/apps-group-name, :validate-form? @validate-form?, :required? true,
                 :default-value apps-group-name, :on-change (partial on-change id)
                 :on-validation ::events/set-details-validation-error]]]

              [ui/ListSA
               [ui/ListItem
                [ui/ListIcon {:name "docker"}]
                [ui/ListContent "App 1"]]
               [ui/ListItem
                [ui/ListIcon {:name "docker"}]
                [ui/ListContent "App 2"]]
               [ui/ListItem
                [ui/ListIcon {:name "docker"}]
                [ui/ListContent "App 3"]]
               ]

              (when @editable?
                [:div {:style {:padding-top 10}}
                 [SelectAppsModal]
                 #_[apps-views-detail/plus ::events/add-file]])]
             :label [:<>
                     (str (inc i) " | " apps-group-name)
                     [ui/Icon {:name     "trash"
                               :color    "red"
                               :style    {:cursor :pointer
                                          :float "right"}
                               :on-click #(dispatch [::events/remove-apps-set id])}]
                     ]
             :count 0
             :default-open true]
            )]
         )
       [ui/Button {:primary  true
                   :on-click #(dispatch [::events/add-apps-set])}
        "New group of applications"]
       ]

      )))


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
      (let [validate? @validate-form?
            valid?    (s/valid? ::spec/docker-compose @docker-compose)]
        [uix/Accordion
         [:<>
          [:div {:style {:margin-bottom "10px"}} "Env substitution"
           [:span ff/nbsp (ff/help-popup (@tr [:module-docker-compose-help]))]
           [DockerComposeCompatibility compatibility unsupp-opts]]
          [uix/EditorYaml
           default-value
           (fn [_editor _data value]
             (dispatch [::events/update-docker-compose value])
             (dispatch [::main-events/changes-protection? true])
             (dispatch [::apps-events/validate-form]))
           @editable?]
          (when validate?
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
           (fn [_editor _data value]
             (dispatch [::events/update-docker-compose value])
             (dispatch [::main-events/changes-protection? true])
             (dispatch [::apps-events/validate-form]))
           @editable?]
          (when validate?
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
     [:h4 (str/capitalize (@tr [:summary]))]
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
          [ui/TableCell [values/as-link id :label (general-utils/id->uuid id)]]])
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:version-number]))]
        [ui/TableCell version-index " " (up-to-date? version-index @versions-map @is-module-published?)]]
       [apps-views-detail/AuthorVendor]
       [Tags @module]]]]))


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


(defn TabMenuApplications
  []
  (let [error? (subscribe [::subs/configuration-error?])]
    [:span {:style {:color (if @error? utils-forms/dark-red "black")}}
     [:<>
      [uix/Icon {:name "list layout"}]
      "Applications"]]))


(defn ApplicationsPane
  []
  [:<>
   [:h2
    [:<>
     [uix/Icon {:name "list layout"}]
     "Applications"]]
   [AppGroupSection]])


(defn TabMenuDetails
  []
  (let [error? (subscribe [::subs/details-validation-error?])]
    [:span {:style {:color (if @error? utils-forms/dark-red "black")}}
     [apps-views-detail/DeploymentsTitle]]))


(defn subtype->pretty
  [subtype]
  (case subtype
    "application" "Docker"
    "application_kubernetes" "Kubernetes"
    "applications_set" "Applications set"
    "Docker"))


(defn DetailsPane []
  (let [tr             (subscribe [::i18n-subs/tr])
        module-subtype (subscribe [::apps-subs/module-subtype])
        active-tab     (subscribe [::apps-subs/active-tab])
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
                           (subtype->pretty @module-subtype)]]]
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
              :centered  true}
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

(defn ConfigureDemo
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [tab-plugin/Tab
     {:db-path [::spec/x]
      :menu    {:secondary true
                :pointing  true}
      :panes   (map
                 (fn [i]
                   {:menuItem {:content (str i)
                               :key     (random-uuid)}
                    :render   #(r/as-element
                                 [tab-plugin/Tab
                                  {:db-path [::spec/y]
                                   :panes   (map
                                              (fn [{:keys [text icon]}]
                                                {:menuItem {:content text
                                                            :icon    icon
                                                            :key     (random-uuid)}
                                                 :render   (fn []
                                                             (r/as-element
                                                               [ui/TabPane

                                                                ]))}
                                                ) [{:text "App 1" :icon "cubes"}
                                                   {:text "App 2" :icon "cubes"}
                                                   {:text "App 3" :icon "cubes"}])}])}
                   ) ["1 | Blackbox"
                      "2 | Whitebox"
                      "3 | Monitoring"
                      ])}]))
(defn module-detail-panes
  []
  (let [module    (subscribe [::apps-subs/module])
        editable? (subscribe [::apps-subs/editable?])
        stripe    (subscribe [::main-subs/stripe])]
    (remove nil? [{:menuItem {:content (r/as-element [TabMenuOverview])
                              :key     :overview
                              :icon    "info"}
                   :pane     {:content (r/as-element [OverviewPane])
                              :key     :overview-pane}}
                  {:menuItem {:content (r/as-element [apps-views-detail/TabMenuDetails])
                              :key     :details}
                   :pane     {:content (r/as-element [DetailsPane])
                              :key     :details-pane}}
                  {:menuItem {:content (r/as-element [TabMenuApplications])
                              :key     :applications}
                   :pane     {:content (r/as-element [ApplicationsPane])
                              :key     :applications-pane}}
                  {:menuItem {:content (r/as-element [TabMenuConfiguration])
                              :key     :configuration}
                   :pane     {:content (r/as-element [ConfigureDemo])
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
        active-tab    (subscribe [::apps-subs/active-tab])
        is-new?       (subscribe [::apps-subs/is-new?])]
    (if (true? @is-new?) (dispatch [::apps-events/set-active-tab :details])
                         (dispatch [::apps-events/set-active-tab :overview]))
    (dispatch [::apps-events/reset-version])
    (dispatch [::apps-events/set-form-spec ::spec/module-application])
    (fn []
      (let [name   (get @module-common ::apps-spec/name)
            parent (get @module-common ::apps-spec/parent-path)
            panes  (module-detail-panes)]
        [ui/Container {:fluid true}
         [uix/PageHeader "th large" (str parent (when (not-empty parent) "/") name) :inline true]
         [apps-views-detail/MenuBar]
         [ui/Tab
          {:menu             {:secondary true
                              :pointing  true
                              :style     {:display        "flex"
                                          :flex-direction "row"
                                          :flex-wrap      "wrap"}}
           :panes            panes
           :activeIndex      (tab/key->index panes @active-tab)
           :renderActiveOnly false
           :onTabChange      (tab/on-tab-change
                               panes
                               #(dispatch [::apps-events/set-active-tab %]))}]
         ]))))
