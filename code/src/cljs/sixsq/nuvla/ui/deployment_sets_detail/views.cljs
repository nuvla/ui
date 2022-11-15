(ns sixsq.nuvla.ui.deployment-sets-detail.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.acl.views :as acl]
    [sixsq.nuvla.ui.apps.utils :as apps-utils]
    [sixsq.nuvla.ui.cimi-detail.views :as cimi-detail-views]
    [sixsq.nuvla.ui.deployment-sets-detail.events :as events]
    [sixsq.nuvla.ui.deployment-sets-detail.spec :as spec]
    [sixsq.nuvla.ui.deployment-sets-detail.subs :as subs]
    [sixsq.nuvla.ui.deployments.subs :as deployments-subs]
    [sixsq.nuvla.ui.deployments.views :as deployments-views]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.job.subs :as job-subs]
    [sixsq.nuvla.ui.job.views :as job-views]
    [sixsq.nuvla.ui.main.components :as components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.plugins.events :as events-plugin]
    [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
    [sixsq.nuvla.ui.plugins.module :as module-plugin]
    [sixsq.nuvla.ui.plugins.pagination :as pagination]
    [sixsq.nuvla.ui.plugins.step-group :as step-group]
    [sixsq.nuvla.ui.plugins.tab :as tab]
    [sixsq.nuvla.ui.session.subs :as session-subs]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as values]
    [sixsq.nuvla.ui.plugins.bulk-progress :as bulk-progress-plugin]
    [sixsq.nuvla.ui.utils.style :as style]))


(def refresh-action-id :deployment-set-get-deployment-set)


(defn refresh
  [uuid]
  (dispatch [::main-events/action-interval-start
             {:id        refresh-action-id
              :frequency 10000
              :event     [::events/get-deployment-set (str "deployment-set/" uuid)]}]))

(defn StartButton
  [{:keys [id] :as deployment-set}]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/MenuItem
     {:on-click (fn [_]
                  (dispatch [::events/operation id "start" {}
                             #(dispatch [::bulk-progress-plugin/monitor
                                         [::spec/bulk-jobs] (:location %)])
                             #()]))
      :disabled (not (general-utils/can-operation?
                       "start" deployment-set))}
     [ui/Icon {:name "play"}]
     (@tr [:start])]))

(defn MenuBar [uuid]
  (let [deployment-set (subscribe [::subs/deployment-set])
        loading?       (subscribe [::subs/loading?])]
    (fn []
      (let [MenuItems (cimi-detail-views/format-operations
                        @deployment-set
                        #{"start"})]
        [components/StickyBar
         [components/ResponsiveMenuBar
          (conj MenuItems
                ^{:key "start"}
                [StartButton @deployment-set])
          [components/RefreshMenu
           {:action-id  refresh-action-id
            :loading?   @loading?
            :on-refresh #(refresh uuid)}]]]))))


(defn EditableCell
  [attribute]
  (let [tr             (subscribe [::i18n-subs/tr])
        deployment-set (subscribe [::subs/deployment-set])
        can-edit?      (subscribe [::subs/can-edit?])
        id             (:id @deployment-set)
        on-change-fn   #(dispatch [::events/edit
                                   id {attribute %}
                                   (@tr [:updated-successfully])])]
    (if @can-edit?
      [components/EditableInput attribute @deployment-set on-change-fn]
      [ui/TableCell (get @deployment-set attribute)])))


(defn TabOverviewDeploymentSet
  [{:keys [id created updated created-by state]}]
  (let [tr     (subscribe [::i18n-subs/tr])
        locale (subscribe [::i18n-subs/locale])]
    [ui/Segment {:secondary true
                 :color     "blue"
                 :raised    true}
     [:h4 "Deployment set"]
     [ui/Table {:basic  "very"
                :padded false}
      [ui/TableBody
       [ui/TableRow
        [ui/TableCell "Id"]
        (when id
          [ui/TableCell [values/as-link id :label (general-utils/id->uuid id)]])]
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:name]))]
        [EditableCell :name]]
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:description]))]
        [EditableCell :description]]
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:state]))]
        [ui/TableCell state]]
       (when created-by
         [ui/TableRow
          [ui/TableCell (str/capitalize (@tr [:created-by]))]
          [ui/TableCell @(subscribe [::session-subs/resolve-user created-by])]])
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:created]))]
        [ui/TableCell (time/ago (time/parse-iso8601 created) @locale)]]
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:updated]))]
        [ui/TableCell (time/ago (time/parse-iso8601 updated) @locale)]]]]]))


(defn TabOverviewTags
  [{:keys [id] :as deployment-set}]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Segment {:secondary true
                 :color     "teal"
                 :raised    true}
     [:h4 "Tags"]
     [components/EditableTags
      deployment-set #(dispatch [::events/edit id {:tags %}
                                 (@tr [:updated-successfully])])]]))

(defn TabOverview
  []
  (let [deployment-set (subscribe [::subs/deployment-set])]
    (fn []
      (let [{:keys [tags]} @deployment-set]
        [ui/TabPane
         [ui/Grid {:columns   2
                   :stackable true
                   :padded    true}
          [ui/GridRow
           [ui/GridColumn {:stretched true}
            [TabOverviewDeploymentSet @deployment-set]]
           [ui/GridColumn {:stretched true}
            [deployments-views/DeploymentsOverviewSegment
             ::deployments-subs/deployments nil nil
             #(dispatch [::tab/change-tab [::spec/tab] :deployments])]]]

          (when (seq tags)
            [ui/GridColumn
             [TabOverviewTags @deployment-set]])]]))))

(defn TabsDeploymentSet
  []
  (let [tr             @(subscribe [::i18n-subs/tr])
        deployment-set (subscribe [::subs/deployment-set])
        can-edit?      @(subscribe [::subs/can-edit?])]
    [tab/Tab
     {:db-path [::spec/tab]
      :panes   [{:menuItem {:content "Overview"
                            :key     :overview
                            :icon    "info"}
                 :render   #(r/as-element [TabOverview])}
                (events-plugin/events-section
                  {:db-path [::spec/events]
                   :href    (:id @deployment-set)})
                {:menuItem {:content "Deployments"
                            :key     :deployments
                            :icon    "rocket"}
                 :render   #(r/as-element [deployments-views/DeploymentTable
                                           {:no-actions true
                                            :empty-msg  (tr [:empty-deployemnt-msg])}])}
                (job-views/jobs-section)
                (acl/TabAcls deployment-set can-edit? ::events/edit)]
      :menu    {:secondary true
                :pointing  true}}]))

(defn Application
  [{:keys [id name subtype description] :as module}]
  (let [selected? @(subscribe [::subs/apps-selected? module])]
    [ui/ListItem {:on-click #(dispatch [::events/toggle-select-app module])
                  :style    {:cursor :pointer}}
     [ui/ListIcon {:name (if selected?
                           "check square outline"
                           "square outline")}]
     [ui/ListContent
      [ui/ListHeader (when selected? {:as :a})
       (apps-utils/subtype-icon-infra subtype selected?)
       " "
       (or name id)]
      [ui/ListDescription
       (general-utils/truncate description 100)]]]))

(defn Applications
  [applications]
  [:<>
   (for [{:keys [id] :as child} applications]
     ^{:key id}
     [Application child])])

(declare Node)

(defn Project
  [path {:keys [applications] :as content}]
  [ui/ListItem
   [ui/ListIcon {:name "folder"}]
   [ui/ListContent
    [ui/ListHeader path]
    [ui/ListList
     [Node
      (dissoc content :applications)
      applications]]]])

(defn Projects
  [projects]
  [:<>
   (for [[path content] projects]
     ^{:key path}
     [Project path content])])

(defn Node
  [projects applications]
  [:<>
   [Projects (sort-by first projects)]
   [Applications (sort-by (juxt :id :name) applications)]])

(defn SelectApps
  []
  (dispatch [::events/search-apps])
  (fn []
    (let [{:keys [count]} @(subscribe [::subs/apps])
          apps     @(subscribe [::subs/apps-tree])
          loading? @(subscribe [::subs/apps-loading?])
          tr       @(subscribe [::i18n-subs/tr])
          render   (fn []
                     (r/as-element
                       [ui/TabPane {:loading loading?}
                        [full-text-search-plugin/FullTextSearch
                         {:db-path      [::spec/apps-search]
                          :change-event [::events/search-apps]}]
                        [ui/ListSA
                         [Node (dissoc apps :applications) (:applications apps)]]
                        [pagination/Pagination
                         {:db-path      [::spec/apps-pagination]
                          :total-items  count
                          :change-event [::events/search-apps]}]]))]
      [tab/Tab
       {:db-path      [::spec/tab-new-apps]
        :panes        [{:menuItem {:content (general-utils/capitalize-words (tr [:appstore]))
                                   :key     :app-store
                                   :icon    (r/as-element [ui/Icon {:className "fas fa-store"}])}
                        :render   render}
                       {:menuItem {:content (general-utils/capitalize-words (tr [:all-apps]))
                                   :key     :all-apps
                                   :icon    "grid layout"}
                        :render   render}
                       {:menuItem {:content (general-utils/capitalize-words (tr [:my-apps]))
                                   :key     :my-apps
                                   :icon    "user"}
                        :render   render}]
        :change-event [::events/search-apps]}])))

(defn CredentialItem
  [{:keys [id name description] :as credential} credentials]
  (let [selected? @(subscribe [::subs/targets-selected? [credential]])]
    [ui/ListItem {:style    {:cursor :pointer}
                  :on-click #(dispatch [::events/toggle-select-target
                                        credential credentials])}
     [ui/ListIcon
      [ui/Icon {:name (if selected?
                        "check square outline"
                        "square outline")}]]
     [ui/ListContent
      [ui/ListHeader (when selected? {:as :a})
       [ui/Icon {:name "key"}] " "
       (or name id)]
      (when description
        [ui/ListDescription (general-utils/truncate description 100)])]]))

(defn TargetItem
  [{:keys [id name description credentials subtype] :as _infrastructure}]
  (let [selected?      (subscribe [::subs/targets-selected? credentials])
        multiple-cred? (> (count credentials) 1)]
    [ui/ListItem (when-not multiple-cred?
                   {:style    {:cursor :pointer}
                    :disabled (-> credentials count zero?)
                    :on-click #(dispatch [::events/toggle-select-target
                                          (first credentials)])})
     [ui/ListIcon
      [ui/Icon {:name (if @selected?
                        "check square outline"
                        "square outline")}]]
     [ui/ListContent
      [ui/ListHeader (when (and (not multiple-cred?) @selected?) {:as :a})
       (case subtype
         "swarm" [ui/Icon {:name "docker"}]
         "kubernetes" [ui/Image {:src   (if @selected?
                                          "/ui/images/kubernetes.svg"
                                          "/ui/images/kubernetes-grey.svg")
                                 :style {:width   "1.18em"
                                         :margin  "0 .25rem 0 0"
                                         :display :inline-block}}])
       " " (or name id)]
      (when description
        [ui/ListDescription (general-utils/truncate description 100)])
      (when multiple-cred?
        [ui/ListList
         [:<>
          (for [{:keys [id] :as credential} credentials]
            ^{:key id}
            [CredentialItem credential credentials])]])]]))

(defn TargetEdges
  []
  (dispatch [::events/search-edges])
  (fn []
    (let [{:keys [count]} @(subscribe [::subs/edges])
          infrastructures @(subscribe [::subs/infrastructures-with-credentials])
          loading?        @(subscribe [::subs/targets-loading?])]
      [ui/TabPane {:loading loading?}
       [full-text-search-plugin/FullTextSearch
        {:db-path      [::spec/edges-search]
         :change-event [::events/search-edges]}]
       [ui/ListSA
        [:<>
         (for [{:keys [id] :as infrastructures} infrastructures]
           ^{:key id}
           [TargetItem infrastructures])]]
       [pagination/Pagination {:db-path      [::spec/edges-pagination]
                               :total-items  count
                               :change-event [::events/search-edges]}]])))

(defn TargetClouds
  []
  (dispatch [::events/search-clouds])
  (fn []
    (let [{:keys [count]} @(subscribe [::subs/infrastructures])
          infrastructures @(subscribe [::subs/infrastructures-with-credentials])
          loading?        @(subscribe [::subs/targets-loading?])]
      [ui/TabPane {:loading loading?}
       [full-text-search-plugin/FullTextSearch
        {:db-path      [::spec/clouds-search]
         :change-event [::events/search-clouds]}]
       [ui/ListSA
        [:<>
         (for [{:keys [id] :as infrastructures} infrastructures]
           ^{:key id}
           [TargetItem infrastructures])]]
       [pagination/Pagination {:db-path      [::spec/clouds-pagination]
                               :total-items  count
                               :change-event [::events/search-clouds]}]])))

(defn SelectTargets
  []
  [tab/Tab
   {:db-path [::spec/tab-new-targets]
    :panes   [{:menuItem {:content "Edges"
                          :key     :edges
                          :icon    "box"}
               :render   #(r/as-element [TargetEdges])}
              {:menuItem {:content "Clouds"
                          :key     :clouds
                          :icon    "cloud"}
               :render   #(r/as-element [TargetClouds])}]}])

(defn StepApplicationsTargets
  []
  [ui/Grid {:stackable true}
   [ui/GridRow {:columns   2
                :stretched true}
    [ui/GridColumn
     [ui/Segment
      [:h2 "Applications"]
      [SelectApps]]]
    [ui/GridColumn
     [ui/Segment
      [:h2 "Targets"]
      [SelectTargets]]]]])

(defn ConfigureApplications
  []
  (let [apps-selected (subscribe [::subs/apps-selected])]
    [:div
     [tab/Tab
      {:db-path [::spec/config-apps-tab]
       :panes   (map
                  (fn [{:keys [id name subtype]}]
                    {:menuItem {:content (or name id)
                                :key     id
                                :icon    (r/as-element
                                           (apps-utils/subtype-icon-infra
                                             subtype false))}
                     :render   #(r/as-element
                                  [ui/TabPane
                                   [uix/Accordion
                                    ^{:key id}
                                    [module-plugin/ModuleVersions
                                     {:db-path [::spec/module-versions]
                                      :href    id}]
                                    :label "Select version"
                                    :default-open true]
                                   [uix/Accordion
                                    [module-plugin/EnvVariables
                                     {:db-path [::spec/module-versions]
                                      :href    id}]
                                    :label "Environment variables"
                                    :default-open true]
                                   [uix/Accordion
                                    [module-plugin/AcceptLicense
                                     {:db-path [::spec/module-versions]
                                      :href    id}]
                                    :label "License"
                                    :default-open true]
                                   [uix/Accordion
                                    [module-plugin/AcceptPrice
                                     {:db-path [::spec/module-versions]
                                      :href    id}]
                                    :label "Price"
                                    :default-open true]
                                   ])}
                    ) @apps-selected)}]]))

(defn AddSelectedTargetsApps
  [header apps-names targets-names]
  (let [warning? (not (and (seq apps-names)
                           (seq targets-names)))]
    (when (or (seq apps-names)
              (seq targets-names))
      [:<>
       [ui/Header {:as :h4, :attached :top} header]
       [ui/Segment {:attached (or warning? :bottom)}
        [ui/Table {:basic :very :celled true}
         [ui/TableBody
          [ui/TableRow
           [ui/TableCell {:collapsing true}
            [ui/Header {:as :h5 :content "Targets:"}]]
           [ui/TableCell
            (for [target-name targets-names]
              ^{:key target-name}
              [ui/Label {:content target-name}])]]
          [ui/TableRow
           [ui/TableCell {:collapsing true}
            [ui/Header {:as :h5 :content "Apps:"}]]
           [ui/TableCell
            (for [app-name apps-names]
              ^{:key app-name}
              [ui/Label {:content app-name}])]]]]]
       (when warning?
         [ui/Message {:warning true :attached :bottom}
          [ui/Icon {:name "warning"}]
          (if (seq apps-names)
            "You selected an app without selecting at least one compatible target!"
            "You selected a target without selecting a compatible application!")])])))

(defn Summary
  []
  (let [tr                    (subscribe [::i18n-subs/tr])
        create-disabled?      (subscribe [::subs/create-disabled?])
        apps-selected         (subscribe [::subs/apps-selected])
        targets-selected      (subscribe [::subs/targets-selected])
        license-not-accepted? (subscribe [::subs/some-license-not-accepted?])
        price-not-accepted? (subscribe [::subs/some-price-not-accepted?])
        create-name-descr     (r/atom {:start false})
        on-change-input       (fn [key]
                                (ui-callback/input-callback
                                  #(swap! create-name-descr assoc key %)))]
    (fn []
      (let [dep-set-name                 (:name @create-name-descr)
            resource-names-of-subtype    (fn [resources subtype]
                                           (map #(or (:name %) (:id %)) (get (group-by :subtype resources)
                                                                             subtype)))
            selected-swarm-targets-names (resource-names-of-subtype @targets-selected "infrastructure-service-swarm")
            selected-swarm-apps-names    (map :name (remove #(= (:subtype %) "application_kubernetes") @apps-selected))
            selected-k8s-targets-names   (resource-names-of-subtype @targets-selected "infrastructure-service-kubernetes")
            selected-k8s-apps-names      (resource-names-of-subtype @apps-selected "application_kubernetes")]
        [ui/Segment (merge style/basic {:clearing true})
         [ui/Form
          [ui/FormInput
           {:label       (str/capitalize (@tr [:name]))
            :placeholder "Name your deployment set"
            :required    true
            :value       (or dep-set-name "")
            :on-change   (on-change-input :name)}]
          [ui/FormInput
           {:label       (str/capitalize (@tr [:description]))
            :placeholder "Describe your deployment set"
            :value       (or (:description @create-name-descr) "")
            :on-change   (on-change-input :description)}]
          [ui/FormCheckbox
           {:label     "Start deployment automatically directly after creation"
            :checked   (:start @create-name-descr)
            :on-change (ui-callback/checked
                         #(swap! create-name-descr update :start not))}]]
         [AddSelectedTargetsApps "Kubernetes" selected-k8s-apps-names selected-k8s-targets-names]
         [AddSelectedTargetsApps "Docker" selected-swarm-apps-names selected-swarm-targets-names]
         (when @license-not-accepted?
           [ui/Message {:warning true}
            [ui/Icon {:name "warning"}]
            "You have to accept all applications licenses!"])
         (when @price-not-accepted?
           [ui/Message {:warning true}
            [ui/Icon {:name "warning"}]
            "You have to accept all applications prices!"])
         [ui/Button
          {:positive true
           :on-click #(dispatch [::events/create @create-name-descr])
           :disabled (or @create-disabled?
                         (str/blank? dep-set-name))
           :floated  :right} "Create"]]))))

(defn AddPage
  []
  (let [tr                  (subscribe [::i18n-subs/tr])
        configure-disabled? (subscribe [::subs/configure-disabled?])]
    (dispatch [::events/new])
    (fn []
      [ui/Container {:fluid true}
       [uix/PageHeader "add" "Add"]
       [step-group/StepGroup
        {:db-path [::spec/steps]
         :size    :mini
         :fluid   true
         :items   [{:key         :select-apps-targets
                    :icon        "bullseye"
                    :content     [StepApplicationsTargets]
                    :title       "Applications/Targets"
                    :description "Select applications and targets"}
                   {:key         :configure
                    :icon        "configure"
                    :content     [ConfigureApplications]
                    :title       "Configure"
                    :disabled    @configure-disabled?
                    :description "Configure applications"}
                   #_{:key         :license-price
                      :icon        "book"
                      :content     [:div "Accept applications licenses"]
                      :title       "License"
                      :disabled    @configure-disabled?
                      :description "Accept licenses"}
                   #_{:key         :price
                      :icon        "eur"
                      :content     [:div "Accept prices"]
                      :title       "Prices"
                      :disabled    @configure-disabled?
                      :description "Accept prices"}
                   {:key         :summary
                    :icon        "info"
                    :content     [Summary]
                    :title       "Summary"
                    :description "Overall summary"}]}]])))

(defn DeploymentSet
  [uuid]
  (refresh uuid)
  (let [{:keys [id name]} @(subscribe [::subs/deployment-set])]
    [components/LoadingPage {:dimmable? true}
     [:<>
      [components/NotFoundPortal
       ::subs/deployment-set-not-found?
       :no-deployment-set-message-header
       :no-deployment-set-message-content]
      [ui/Container {:fluid true}
       [uix/PageHeader "bullseye" (or name id)]
       [MenuBar uuid]
       [job-views/ProgressJobAction]
       [bulk-progress-plugin/MonitoredJobs
        {:db-path [::spec/bulk-jobs]}]
       [components/ErrorJobsMessage
        ::job-subs/jobs nil nil
        #(dispatch [::tab/change-tab [::spec/tab] :jobs])]
       [TabsDeploymentSet]]]]))


(defn Details
  [uuid]
  (if (= (str/lower-case uuid) "new")
    [AddPage]
    [DeploymentSet uuid]))
