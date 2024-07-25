(ns sixsq.nuvla.ui.pages.apps.apps-applications-sets.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.common-components.plugins.module :as module-plugin]
    [sixsq.nuvla.ui.common-components.plugins.module-selector :as module-selector]
    [sixsq.nuvla.ui.common-components.plugins.nav-tab :as nav-tab]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.pages.apps.apps-applications-sets.events :as events]
    [sixsq.nuvla.ui.pages.apps.apps-applications-sets.spec :as spec]
    [sixsq.nuvla.ui.pages.apps.apps-applications-sets.subs :as subs]
    [sixsq.nuvla.ui.pages.apps.apps-applications-sets.utils :as utils]
    [sixsq.nuvla.ui.pages.apps.events :as apps-events]
    [sixsq.nuvla.ui.pages.apps.spec :as apps-spec]
    [sixsq.nuvla.ui.pages.apps.subs :as apps-subs]
    [sixsq.nuvla.ui.pages.apps.utils :as apps-utils]
    [sixsq.nuvla.ui.pages.apps.views-detail :as apps-views-detail]
    [sixsq.nuvla.ui.pages.apps.views-versions :as apps-views-versions]
    [sixsq.nuvla.ui.pages.deployments.subs :as deployments-subs]
    [sixsq.nuvla.ui.pages.deployments.views :as deployments-views]
    [sixsq.nuvla.ui.utils.forms :as utils-forms]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.icons :as icons]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.tooltip :as tt]
    [sixsq.nuvla.ui.utils.values :as values]))

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
                                 [ui/MenuItem
                                  {:on-click on-open}
                                  [icons/Icon {:name  icons/i-plus-full
                                               :color "green"}]])
                   :header     "Select applications"
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

(defn ConfigureApplication
  [id module-id]
  (let [tr        (subscribe [::i18n-subs/tr])
        editable? (subscribe [::apps-subs/editable?])
        db-path   [::spec/apps-sets id]]
    [ui/TabPane
     [ui/Popup {:trigger (r/as-element
                           [:span
                            [module-plugin/LinkToApp
                             {:db-path  db-path
                              :href     module-id
                              :children [:<>
                                         [ui/Icon {:class icons/i-link}]
                                         (@tr [:go-to-app])]}]])
                :content (@tr [:open-app-in-new-window])}]
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
       {:db-path           db-path
        :href              module-id
        :change-event      [::main-events/changes-protection? true]
        :read-only?        (not @editable?)
        :highlight-errors? false
        :show-required?    false}]
      :label (@tr [:env-variables])
      :default-open true]
     [uix/Accordion
      [module-plugin/Files
       {:db-path           db-path
        :href              module-id
        :change-event      [::main-events/changes-protection? true]
        :read-only?        (not @editable?)}]
      :label (@tr [:module-files])
      :default-open true]
     [uix/Accordion
      [module-plugin/RegistriesCredentials
       {:db-path      db-path
        :href         module-id
        :required?    false
        :change-event [::main-events/changes-protection? true]}]
      :label (@tr [:private-registries])
      :default-open true]]))

(defn DeleteModal
  [{:keys [header content on-confirm]}]
  (let [tr (subscribe [::i18n-subs/tr])]
    [uix/ModalDanger
     {:header      header
      :content     content
      :trigger     (r/as-element
                     [:span [icons/CloseIcon {:color "red" :link true}]])
      :button-text (@tr [:delete])
      :on-confirm  on-confirm}]))

(defn DeleteApp
  [on-delete]
  [DeleteModal
   {:header     "Delete application"
    :content    "Remove application ?"
    :on-confirm on-delete}])

(defn DeleteAppSet
  [on-delete]
  [DeleteModal
   {:header     "Delete application bouquet"
    :content    "Delete application bouquet and configuration related to it?"
    :on-confirm on-delete}])

(defn ApplicationTabHeader
  [{:keys [id module-id name subtype editable? on-delete]}]
  (let [tr                                  (subscribe [::i18n-subs/tr])
        is-behind-latest-published-version? (subscribe [::module-plugin/is-behind-latest-published-version? [::spec/apps-sets id] module-id])]
    [:<>
     [icons/Icon {:name (apps-utils/subtype-icon subtype)}]
     (or name id)
     (when @is-behind-latest-published-version?
       (tt/with-tooltip
         [:span [icons/TriangleExclamationIcon {:style {:margin-left "5px"}
                                                :color :orange}]]
         (@tr [:warning-not-latest-app-version])))
     general-utils/nbsp
     (when @editable?
       [DeleteApp on-delete])]))

(defn ConfigureSetApplications
  [id]
  (let [rerender-atom (r/atom 0)
        applications  (subscribe [::subs/apps-selected id])
        editable?     (subscribe [::apps-subs/editable?])
        on-delete     #(do
                         (dispatch [::events/remove-app %1 %2])
                         (dispatch [::main-events/changes-protection? true])
                         (dispatch [::apps-events/validate-form])
                         (swap! rerender-atom inc))]
    (fn []
      (if (or (seq @applications) @editable?)
        ^{:key (str "tab-apps-" id "-" @rerender-atom)}
        [ui/Tab {:menu {:style {:display "flex"
                                :flex-wrap "wrap"}
                        :stackable true
                        :pointing true
                        :secondary true}
                 :panes (cond->
                          (mapv
                            (fn [{:keys [name subtype] module-id :id}]
                              {:menuItem
                               {:content (r/as-element
                                           [ApplicationTabHeader
                                            {:id        id
                                             :module-id module-id
                                             :name      name
                                             :subtype   subtype
                                             :editable? editable?
                                             :on-delete (partial on-delete id module-id)}])
                                :key     (str id "-" module-id)}
                               :render #(r/as-element
                                          [ConfigureApplication id module-id])})
                            @applications)
                          @editable? (conj {:menuItem (r/as-element
                                                        ^{:key (str "add-apps-" id)}
                                                        [SelectAppsModal id])}))}]
        [ui/Message {:info true} "No applications for this set yet"]))))

(defn AppSetTab
  [_opts]
  (let [tr             (subscribe [::i18n-subs/tr])
        editable?      (subscribe [::apps-subs/editable?])
        validate-form? (subscribe [::apps-subs/validate-form?])
        on-change      (fn [id event-update value]
                         (dispatch [event-update id value])
                         (dispatch [::main-events/changes-protection? true])
                         (dispatch [::apps-events/validate-form]))]
    (fn [{:keys [id apps-set-name apps-set-description]}]
      [ui/TabPane
       [:p (str "Application bouquet is a named group of apps intended to be deployed on a target fleet. "
                "Target fleets are defined at deployment time. ")]
       [:p "To add apps to your application bouquet, click the plus button below."]
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
       [ConfigureSetApplications id]])))

(defn SingleAppsSetPanel
  [id]
  (dispatch [::events/update-apps-set-name id "Main"])
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [id]
      [:div
       [:p (@tr [:application-bouquet-is-named-group])]
       [:p (@tr [:add-apps-to-app-bouquet])]
       [ui/Table {:compact    true
                  :definition true}
        [ui/TableBody
         [ui/TableRow
          [ui/TableCell {:collapsing true} "subtype"]
          ^{:key (str/join "-" ["set" id "subtype"])}
          [ui/TableCell @(subscribe [::subs/apps-set-subtype id])]]]]
       [ConfigureSetApplications id]])))

(defn SingleAppsSetSection
  []
  (let [rerender-atom (r/atom 0)
        apps-sets     (subscribe [::subs/apps-sets])]
    (fn []
      [:<>
       (let [[id] (first @apps-sets)]
         ^{:key (str "apps-sets-rerender-" @rerender-atom)}
         [SingleAppsSetPanel id])])))

(defn AppsSetsSection
  []
  (let [rerender-atom (r/atom 0)
        editable?     (subscribe [::apps-subs/editable?])
        apps-sets     (subscribe [::subs/apps-sets])
        on-delete     #(do
                         (dispatch [::events/remove-apps-set %])
                         (dispatch [::main-events/changes-protection? true])
                         (dispatch [::apps-events/validate-form])
                         (swap! rerender-atom inc))]
    (fn []
      [:<>
       ^{:key (str "apps-sets-rerender-" @rerender-atom)}
       [ui/Tab
        {:menu  {:secondary true
                 :pointing  true}
         :panes (cond-> (vec (map-indexed
                               (fn [i [id {:keys [::spec/apps-set-name
                                                  ::spec/apps-set-description] :as _apps-group}]]
                                 {:menuItem {:content (r/as-element
                                                        [:<>
                                                         (str (inc i) " | " apps-set-name)
                                                         general-utils/nbsp
                                                         (when @editable?
                                                           [DeleteAppSet (partial on-delete id)])])
                                             :key     (keyword (str "apps-set-" i))}
                                  :render   #(r/as-element
                                               ^{:key (keyword (str "apps-set-" i))}
                                               [AppSetTab {:i                    i
                                                           :id                   id
                                                           :apps-set-name        apps-set-name
                                                           :apps-set-description apps-set-description}])})
                               @apps-sets))
                        @editable? (conj {:menuItem
                                          {:key     "new-apps-set"
                                           :content (r/as-element [icons/Icon {:name  icons/i-plus-full
                                                                               :size  "small"
                                                                               :color "grey"}])
                                           :active  false
                                           :onClick (fn []
                                                      (dispatch [::events/add-apps-set])
                                                      (dispatch [::main-events/changes-protection? true])
                                                      (dispatch [::apps-events/validate-form]))}}))}]])))
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
        module               (subscribe [::apps-subs/module])
        versions-map         (subscribe [::apps-subs/versions])
        module-content-id    (subscribe [::apps-subs/module-content-id])
        version-index        (apps-utils/find-current-version @versions-map @module-content-id)
        is-module-published? (subscribe [::apps-subs/is-module-published?])
        {:keys [id created updated name parent-path path]} @module]
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
        [ui/TableCell (if created [uix/TimeAgo created] (@tr [:soon]))]]
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:updated]))]
        [ui/TableCell (if updated [uix/TimeAgo updated] (@tr [:soon]))]]
       (when id
         [ui/TableRow
          [ui/TableCell (str/capitalize (@tr [:id]))]
          [ui/TableCell [values/AsLink id :label (general-utils/id->uuid id)]]])
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:app-version]))]
        [ui/TableCell
         [module-plugin/LinkToAppView {:path path :version-id version-index} version-index]
         " "
         (apps-utils/up-to-date? version-index @versions-map @is-module-published? @tr)]]
       [apps-views-detail/AuthorVendor]
       [Tags @module]]]]))


(defn TabMenuVersions
  []
  [:span
   [apps-views-versions/VersionsTitle]])


(defn VersionsPane
  []
  [apps-views-versions/Versions])


(defn TabMenuApplications
  []
  (let [tr                 (subscribe [::i18n-subs/tr])
        error?             (subscribe [::subs/apps-error?])
        has-outdated-apps? (subscribe [::subs/has-outdated-apps?])]
    [:span {:style {:color (if @error? utils-forms/dark-red "black")}}
     [:<>
      [icons/ListIcon]
      "Applications"
      (when @has-outdated-apps?
        (tt/with-tooltip
          [:span [icons/TriangleExclamationIcon {:style {:margin-left "5px"}
                                                 :color :orange}]]
          (@tr [:warning-has-outdated-apps])))]]))

(defn WarningVersionBehind
  [content-i18n-key]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Message {:warning true}
     [ui/MessageHeader (@tr [:warning])]
     [ui/MessageContent (@tr content-i18n-key)]]))

(defn ApplicationsPane
  []
  (let [has-outdated-apps? (subscribe [::subs/has-outdated-apps?])]
    [:div {:class :uix-apps-details-details}
     [:h4 {:class :tab-app-detail} "Applications"]
     [:<>
      (when @has-outdated-apps?
        [WarningVersionBehind [:warning-has-outdated-apps]])
      [SingleAppsSetSection]]]))


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
          {:sub-key       ::deployments-subs/deployments
           :show-me-event [::apps-events/set-active-tab :deployments]}]])]
     [ui/GridRow {:centered true}
      [ui/GridColumn
       [apps-views-detail/OverviewDescription]]]
     [ui/GridRow
      [ui/GridColumn
       [OverviewModuleSummary]]]]))

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
      (let [name  (get @module-common ::apps-spec/name)
            panes (module-detail-panes)]
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
