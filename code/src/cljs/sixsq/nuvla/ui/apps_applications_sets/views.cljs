(ns sixsq.nuvla.ui.apps-applications-sets.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.apps-applications-sets.events :as events]
    [sixsq.nuvla.ui.apps-applications-sets.spec :as spec]
    [sixsq.nuvla.ui.apps-applications-sets.subs :as subs]
    [sixsq.nuvla.ui.apps-applications-sets.utils :as utils]
    [sixsq.nuvla.ui.apps.events :as apps-events]
    [sixsq.nuvla.ui.apps.spec :as apps-spec]
    [sixsq.nuvla.ui.apps.subs :as apps-subs]
    [sixsq.nuvla.ui.apps.utils :as apps-utils]
    [sixsq.nuvla.ui.apps.views-detail :as apps-views-detail]
    [sixsq.nuvla.ui.apps.views-versions :as apps-views-versions]
    [sixsq.nuvla.ui.deployments.subs :as deployments-subs]
    [sixsq.nuvla.ui.deployments.views :as deployments-views]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.plugins.module :as module-plugin]
    [sixsq.nuvla.ui.plugins.module-selector :as module-selector]
    [sixsq.nuvla.ui.plugins.nav-tab :as nav-tab]
    [sixsq.nuvla.ui.utils.forms :as utils-forms]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.values :as values]
    [sixsq.nuvla.ui.utils.icons :as icons]))

(defn SelectSubtype
  [subtype!]
  (let [on-click #(reset! subtype! %)]
    [:<>
     [:p "Targeting Docker or Kubernetes"]
     [ui/CardGroup {:centered true}
      [ui/Card
       {:on-click (partial on-click spec/app-set-docker-subtype)}
       [ui/CardContent {:text-align :center}
        [ui/IconGroup {:size :massive}
         [icons/DockerIcon]]]]
      [ui/Card
       {:on-click (partial on-click spec/app-set-k8s-subtype)}
       [ui/CardContent {:text-align :center}
        [ui/IconGroup {:size :massive}
         [ui/Image {:src   "/ui/images/kubernetes.svg"
                    :style {:width "100px"}}]]]]]]))

(defn SelectAppsModal
  [_id]
  (let [subtype!      (r/atom nil)
        reset-subtype #(reset! subtype! nil)]
    (fn [id]
      (let [saved-subtype @(subscribe [::subs/apps-set-subtype id])
            apps-selected @(subscribe [::subs/apps-selected id])
            db-path       [::spec/apps-sets id ::spec/apps-selector]
            on-open       #(dispatch [::module-selector/restore-selected
                                      db-path (map :id apps-selected)])
            on-done       #(do
                             (dispatch [::events/set-apps-selected id db-path])
                             (when @subtype!
                               (dispatch [::events/set-apps-set-subtype id @subtype!]))
                             (dispatch [::main-events/changes-protection? true])
                             (dispatch [::apps-events/validate-form])
                             (reset-subtype))
            subtypes      (get utils/app-set-app-subtypes
                               (or saved-subtype @subtype!))]
        [ui/Modal {:close-icon true
                   :trigger    (r/as-element
                                 [icons/AddIconFull {:color    "green"
                                                     :on-click on-open}])
                   :header     "New apps set"
                   :content    (r/as-element
                                 [ui/ModalContent
                                  (if (seq subtypes)
                                    [module-selector/AppsSelectorSection
                                     {:db-path  db-path
                                      :subtypes subtypes}]
                                    [SelectSubtype subtype!])])
                   :actions    [{:key     "cancel", :content "Cancel"
                                 :onClick reset-subtype}
                                {:key     "done", :content "Done" :positive true
                                 :onClick on-done}]}]))))

(defn AppNameIcon
  [id {module-id :id} on-delete]
  [module-plugin/ModuleNameIcon
   {:db-path       [::spec/apps-sets id]
    :href          module-id
    :show-version? true
    :children      (when on-delete
                     [:<>
                      " "
                      [icons/CloseIcon {:color "red" :link true
                        :on-click #(on-delete module-id)}]])}])


(defn AppsList
  [id]
  (let [selected  @(subscribe [::subs/apps-selected id])
        on-delete #(do
                     (dispatch [::events/remove-app id %])
                     (dispatch [::main-events/changes-protection? true])
                     (dispatch [::apps-events/validate-form]))]
    [ui/ListSA
     (for [module selected]
       ^{:key (:id module)}
       [AppNameIcon id module on-delete])]))

(defn AddApps
  [id]
  [:div {:style {:padding-top 10}}
   [SelectAppsModal id]])

(defn AddAppsSet
  []
  [ui/Button {:primary  true
              :on-click #(do
                           (dispatch [::events/add-apps-set])
                           (dispatch [::main-events/changes-protection? true])
                           (dispatch [::apps-events/validate-form]))}
   "New set of applications"])

(defn AccordionAppSet
  [_opts]
  (let [tr             (subscribe [::i18n-subs/tr])
        editable?      (subscribe [::apps-subs/editable?])
        validate-form? (subscribe [::apps-subs/validate-form?])
        on-change      (fn [id event-update value]
                         (dispatch [event-update id value])
                         (dispatch [::main-events/changes-protection? true])
                         (dispatch [::apps-events/validate-form]))]
    (fn [{:keys [i id apps-set-name apps-set-description]}]
      [uix/Accordion
       [:<>
        [ui/Table {:compact    true
                   :definition true}
         [ui/TableBody
          [uix/TableRowField (@tr [:name]), :key "app-set-name", :editable? @editable?,
           :spec ::spec/apps-set-name, :validate-form? @validate-form?, :required? true,
           :default-value apps-set-name, :on-change (partial on-change id ::events/update-apps-set-name)
           :on-validation ::events/set-apps-validation-error]
          [uix/TableRowField (@tr [:description]), :key "app-set-description", :editable? @editable?,
           :spec ::spec/apps-set-description, :validate-form? @validate-form?, :required? false,
           :default-value apps-set-description, :on-change (partial on-change id ::events/update-apps-set-description)
           :on-validation ::events/set-apps-validation-error]
          [ui/TableRow
           [ui/TableCell {:collapsing true} "subtype"]
           ^{:key (str/join "-" ["set" id "subtype"])}
           [ui/TableCell @(subscribe [::subs/apps-set-subtype id])]]]]

        [AppsList id]

        (when @editable?
          [AddApps id])]
       :label [:<>
               (str i " | " apps-set-name)
               (when @editable?
                 [icons/TrashIconFull {:color    "red"
                                       :style    {:cursor :pointer
                                                  :float  "right"}
                                       :on-click #(do
                                                    (dispatch [::events/remove-apps-set id])
                                                    (dispatch [::main-events/changes-protection? true])
                                                    (dispatch [::apps-events/validate-form]))}])]
       :default-open true]
      )))

(defn AppsSetsSection
  []
  (let [editable? @(subscribe [::apps-subs/editable?])
        apps-sets @(subscribe [::subs/apps-sets])]
    [:<>
     (doall
       (for [[i [id {:keys [::spec/apps-set-name
                            ::spec/apps-set-description] :as _apps-group}]]
             (map-indexed vector apps-sets)]
         (let [i (inc i)]
           ^{:key (str "apps-set-" i)}
           [AccordionAppSet {:i                    i
                             :id                   id
                             :apps-set-name        apps-set-name
                             :apps-set-description apps-set-description}])))
     (when editable?
       [AddAppsSet])]))


(defn up-to-date?
  [v versions published?]
  (when v
    (let [tr                     (subscribe [::i18n-subs/tr])
          last-version           (ffirst versions)
          last-published-version (apps-utils/latest-published-index versions)]
      (if published?
        (if (= v last-published-version)
          [:span [icons/CheckIconFull {:color "green"}] " (" (@tr [:up-to-date-published]) ")"]
          [:span [icons/WarningIcon {:color "orange"}]
           (str (@tr [:not-up-to-date-published]))])
        (if (= v last-version)
          [:span [icons/CheckIconFull {:color "green"}] " (" (@tr [:up-to-date-latest]) ")"]
          [:span [icons/WarningIcon {:color "orange"}]
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
  (let [error? (subscribe [::subs/apps-error?])]
    [:span {:style {:color (if @error? utils-forms/dark-red "black")}}
     [:<>
      [icons/ListIcon]
      "Applications"]]))


(defn ApplicationsPane
  []
  [:<>
   [:h2
    [:<>
     [icons/ListIcon]
     "Applications"]]
   [AppsSetsSection]])


(defn TabMenuDetails
  []
  (let [error? (subscribe [::subs/details-validation-error?])]
    [:span {:style {:color (if @error? utils-forms/dark-red "black")}}
     [apps-views-detail/DeploymentsTitle]]))


(defn DetailsPane []
  (let [active-tab (subscribe [::apps-subs/active-tab])]
    @active-tab
    ^{:key (random-uuid)}
    [apps-views-detail/Details
     {:extras           [^{:key "module_subtype"}
                         [apps-views-detail/SubtypeRow]]
      :validation-event ::apps-events/set-details-validation-error}]))


(defn TabMenuOverview
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [:span (str/capitalize (@tr [:overview]))]))


(defn OverviewPane
  []
  (let [device  (subscribe [::main-subs/device])
        is-new? (subscribe [::apps-subs/is-new?])
        subtype (subscribe [::apps-subs/module-subtype])]
    [ui/Grid {:columns   (if (contains? #{:wide-screen} @device) 2 1)
              :stackable true
              :padded    true
              :centered  true}
     [ui/GridRow {:centered true}
      (when-not (or @is-new? (= @subtype apps-utils/subtype-applications-sets))
        [ui/GridColumn
         [deployments-views/DeploymentsOverviewSegment
          ::deployments-subs/deployments ::apps-events/set-active-tab :deployments]])]
     [ui/GridRow {:centered true}
      [ui/GridColumn
       [apps-views-detail/OverviewDescription]]]
     [ui/GridRow
      [ui/GridColumn
       [OverviewModuleSummary]]]]))

(defn ConfigureSetApplications
  [id]
  (let [tr           (subscribe [::i18n-subs/tr])
        applications (subscribe [::subs/apps-selected id])
        editable?    (subscribe [::apps-subs/editable?])
        db-path      [::spec/apps-sets id]
        read-only?   (not @editable?)]
    (if (seq @applications)
      ^{:key (str "tab-apps-" id)}
      [ui/Tab
       {:panes (map
                 (fn [{:keys [name subtype] module-id :id}]
                   {:menuItem {:content (or name id)
                               :icon    (r/as-element
                                          [icons/Icon {:name (apps-utils/subtype-icon subtype)}])
                               :key     (str id "-" module-id)}
                    :render   #(r/as-element
                                 [ui/TabPane
                                  [ui/TabPane
                                   [uix/Accordion
                                    [module-plugin/ModuleVersions
                                     {:db-path      db-path
                                      :href         module-id
                                      :change-event [::main-events/changes-protection? true]
                                      :read-only?   (not @editable?)}]
                                    :label (@tr [:select-version])
                                    :default-open true]
                                   [uix/Accordion
                                    [module-plugin/EnvVariables
                                     {:db-path      db-path
                                      :href         module-id
                                      :change-event [::main-events/changes-protection? true]
                                      :read-only?   read-only?}]
                                    :label (@tr [:env-variables])
                                    :default-open true]
                                   [uix/Accordion
                                    [module-plugin/RegistriesCredentials
                                     {:db-path      db-path
                                      :href         module-id
                                      :required?     false
                                      :change-event [::main-events/changes-protection? true]}]
                                    :label (@tr [:private-registries])
                                    :default-open true]]])})
                 @applications)}]
      [ui/Message {:info true} "No applications for this set yet"])))

(defn Configuration
  []
  (let [apps-sets (subscribe [::subs/apps-sets])]
    (if (seq @apps-sets)
      [ui/Tab {:menu  {:secondary true
                       :pointing  true}
               :panes (map-indexed
                        (fn [i [id {:keys [::spec/apps-set-name]}]]
                          (let [i (inc i)]
                            {:menuItem {:content (str i " | " apps-set-name)
                                        :key     (str apps-set-name "-" i)}
                             :render   #(r/as-element [ConfigureSetApplications id])}))
                        @apps-sets)}]
      [ui/Message {:info true} "No apps sets created yet"])))

(defn module-detail-panes
  []
  (let [module    (subscribe [::apps-subs/module])
        editable? (subscribe [::apps-subs/editable?])]
    (remove nil? [{:menuItem {:content (r/as-element [TabMenuOverview])
                              :key     :overview
                              :icon    icons/i-eye}
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
                   :pane     {:content (r/as-element [Configuration])
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
        is-new?       (subscribe [::apps-subs/is-new?])]
    (if (true? @is-new?) (dispatch [::apps-events/set-active-tab :details])
                         (dispatch [::apps-events/set-active-tab :overview]))
    (dispatch [::apps-events/set-form-spec ::spec/apps-sets])
    (fn []
      (let [name   (get @module-common ::apps-spec/name)
            panes  (module-detail-panes)]
        [ui/Container {:fluid true}
         [uix/PageHeader icons/i-app-sets name :inline true]
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
