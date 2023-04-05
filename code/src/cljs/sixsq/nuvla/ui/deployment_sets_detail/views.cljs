(ns sixsq.nuvla.ui.deployment-sets-detail.views
  (:require [clojure.string :as str]
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
            [sixsq.nuvla.ui.plugins.target-selector :as target-selector]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.plugins.bulk-progress :as bulk-progress-plugin]
            [sixsq.nuvla.ui.plugins.events :as events-plugin]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.plugins.module :as module-plugin]
            [sixsq.nuvla.ui.plugins.pagination :as pagination]
            [sixsq.nuvla.ui.plugins.step-group :as step-group]
            [sixsq.nuvla.ui.plugins.nav-tab :as tab]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
            [sixsq.nuvla.ui.utils.values :as values]))


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

(defn StopButton
  [{:keys [id] :as deployment-set}]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/MenuItem
     {:on-click (fn [_]
                  (dispatch [::events/operation id "stop" {}
                             #(dispatch [::bulk-progress-plugin/monitor
                                         [::spec/bulk-jobs] (:location %)])
                             #()]))
      :disabled (not (general-utils/can-operation?
                       "stop" deployment-set))}
     [ui/Icon {:name "stop"}]
     (@tr [:stop])]))


(defn DeleteButton
  [{:keys [id name description] :as _deployment-set}]
  (let [tr      (subscribe [::i18n-subs/tr])
        content (str (or name id) (when description " - ") description)]
    [uix/ModalDanger
     {:on-confirm  #(dispatch [::events/delete])
      :trigger     (r/as-element [ui/MenuItem
                                  [ui/Icon {:name "trash"}]
                                  (@tr [:delete])])
      :content     [:h3 content]
      :header      (@tr [:delete-deployment-set])
      :danger-msg  (@tr [:danger-action-cannot-be-undone])
      :button-text (@tr [:delete])}]))

(defn MenuBar [uuid]
  (let [deployment-set (subscribe [::subs/deployment-set])
        loading?       (subscribe [::subs/loading?])]
    (fn []
      (let [MenuItems (cimi-detail-views/format-operations
                        @deployment-set
                        #{"start" "stop" "delete"})]
        [components/StickyBar
         [components/ResponsiveMenuBar
          (conj MenuItems
                ^{:key "delete"}
                [DeleteButton @deployment-set]
                ^{:key "stop"}
                [StopButton @deployment-set]
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
          [ui/TableCell [values/AsLink id :label (general-utils/id->uuid id)]])]
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
             #(dispatch [::tab/change-tab {:db-path [::spec/tab]
                                           :tab-key :deployments}])]]]

          (when (seq tags)
            [ui/GridColumn
             [TabOverviewTags @deployment-set]])]]))))

(defn TabsDeploymentSet
  []
  (let [tr             @(subscribe [::i18n-subs/tr])
        deployment-set (subscribe [::subs/deployment-set])
        can-edit?      @(subscribe [::subs/can-edit?])]
    (when @deployment-set
      [tab/Tab
       {:db-path [::spec/tab]
        :panes   [{:menuItem {:content (str/capitalize (tr [:overview]))
                              :key     :overview
                              :icon    "info"}
                   :render   #(r/as-element [TabOverview])}
                  (events-plugin/events-section
                    {:db-path [::spec/events]
                     :href    (:id @deployment-set)})
                  {:menuItem {:content (str/capitalize (tr [:deployments]))
                              :key     :deployments
                              :icon    "rocket"}
                   :render   #(r/as-element [deployments-views/DeploymentTable
                                             {:fetch-event [::events/get-deployments-for-deployment-sets (:id @deployment-set)]
                                              :no-actions  true
                                              :empty-msg   (tr [:empty-deployemnt-msg])}])}
                  (job-views/jobs-section)
                  (acl/TabAcls {:e          deployment-set
                                :can-edit?  can-edit?
                                :edit-event ::events/edit})]
        :menu    {:secondary true
                  :pointing  true}}])))

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
       [apps-utils/SubtypeIconInfra subtype selected?]
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
  (let [tr @(subscribe [::i18n-subs/tr])]
    [ui/Grid {:stackable true}
     [ui/GridRow {:columns   2
                  :stretched true}
      [ui/GridColumn
       [ui/Segment
        [:h2 (tr [:applications])]
        [SelectApps]]]
      [ui/GridColumn
       [ui/Segment
        [:h2 (tr [:targets])]
        [SelectTargets]]]]]))

(defn ConfigureApplications
  []
  (let [tr            (subscribe [::i18n-subs/tr])
        apps-selected (subscribe [::subs/apps-selected])]
    [:div
     [tab/Tab
      {:db-path [::spec/config-apps-tab]
       :panes   (map
                  (fn [{:keys [id name subtype]}]
                    {:menuItem {:content (or name id)
                                :key     id
                                :icon    (r/as-element
                                           (apps-utils/SubtypeIconInfra
                                             subtype false))}
                     :render   #(r/as-element
                                  [ui/TabPane
                                   [uix/Accordion
                                    ^{:key id}
                                    [module-plugin/ModuleVersions
                                     {:db-path [::spec/module-versions]
                                      :href    id}]
                                    :label (@tr [:select-version])
                                    :default-open true]
                                   [uix/Accordion
                                    [module-plugin/EnvVariables
                                     {:db-path [::spec/module-versions]
                                      :href    id}]
                                    :label (@tr [:env-variables])
                                    :default-open true]
                                   [uix/Accordion
                                    [module-plugin/AcceptLicense
                                     {:db-path [::spec/module-versions]
                                      :href    id}]
                                    :label (@tr [:eula-full])
                                    :default-open true]
                                   [uix/Accordion
                                    [module-plugin/AcceptPrice
                                     {:db-path [::spec/module-versions]
                                      :href    id}]
                                    :label (str/capitalize (@tr [:price]))
                                    :default-open true]
                                   ])}
                    ) @apps-selected)}]]))

(defn AddSelectedTargetsApps
  [header apps-names targets-names]
  (let [tr       (subscribe [::i18n-subs/tr])
        warning? (not (and (seq apps-names)
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
            [ui/Header {:as :h5 :content (str (str/capitalize (@tr [:targets]))
                                              ":")}]]
           [ui/TableCell
            (for [target-name targets-names]
              ^{:key target-name}
              [ui/Label {:content target-name}])]]
          [ui/TableRow
           [ui/TableCell {:collapsing true}
            [ui/Header {:as :h5 :content (str (str/capitalize (@tr [:apps]))
                                              ":")}]]
           [ui/TableCell
            (for [app-name apps-names]
              ^{:key app-name}
              [ui/Label {:content app-name}])]]]]]
       (when warning?
         [ui/Message {:warning true :attached :bottom}
          [ui/Icon {:name "question"}]
          (if (seq apps-names)
            (@tr [:warning-app-selected-not-compatible-targets])
            (@tr [:warning-target-selected-not-compatible-apps]))])])))

(defn on-change-input
  [k]
  (ui-callback/input-callback
    #(dispatch [::events/set k %])))

(defn Summary
  []
  (let [tr                    (subscribe [::i18n-subs/tr])
        create-disabled?      (subscribe [::subs/create-disabled?])
        apps-selected         (subscribe [::subs/apps-selected])
        targets-selected      (subscribe [::subs/targets-selected])
        license-not-accepted? (subscribe [::subs/some-license-not-accepted?])
        price-not-accepted?   (subscribe [::subs/some-price-not-accepted?])
        create-name           (subscribe [::subs/get ::spec/create-name])
        create-description    (subscribe [::subs/get ::spec/create-description])
        create-start          (subscribe [::subs/get ::spec/create-start])]
    (fn []
      (let [resource-names-of-subtype    (fn [resources subtype]
                                           (map #(or (:name %) (:id %)) (get (group-by :subtype resources)
                                                                             subtype)))
            selected-swarm-targets-names (resource-names-of-subtype @targets-selected "infrastructure-service-swarm")
            selected-swarm-apps-names    (map :name (remove #(= (:subtype %) "application_kubernetes") @apps-selected))
            selected-k8s-targets-names   (resource-names-of-subtype @targets-selected "infrastructure-service-kubernetes")
            selected-k8s-apps-names      (resource-names-of-subtype @apps-selected "application_kubernetes")]
        [ui/Segment (merge style/basic {:clearing true})
         [ui/Form
          [ui/FormInput
           {:label         (str/capitalize (@tr [:name]))
            :placeholder   (@tr [:name-deployment-set])
            :required      true
            :default-value @create-name
            :on-change     (on-change-input ::spec/create-name)}]
          [ui/FormInput
           {:label         (str/capitalize (@tr [:description]))
            :placeholder   (@tr [:describe-deployment-set])
            :default-value @create-description
            :on-change     (on-change-input ::spec/create-description)}]
          [ui/FormCheckbox
           {:label     (@tr [:start-deployment-set-after-creation])
            :checked   @create-start
            :on-change (ui-callback/checked
                         #(dispatch [::events/set ::spec/create-start (not @create-start)]))}]]
         [AddSelectedTargetsApps "Kubernetes" selected-k8s-apps-names selected-k8s-targets-names]
         [AddSelectedTargetsApps "Docker" selected-swarm-apps-names selected-swarm-targets-names]
         (when @license-not-accepted?
           [ui/Message {:error true}
            [ui/Icon {:name "warning"}]
            (@tr [:accept-applications-licenses])])
         (when @price-not-accepted?
           [ui/Message {:error true}
            [ui/Icon {:name "warning"}]
            (@tr [:accept-applications-prices])])
         [ui/Button
          {:positive true
           :on-click #(dispatch [::events/create])
           :disabled (or @create-disabled?
                         (str/blank? @create-name))
           :floated  :right} (str/capitalize (@tr [:create]))]]))))

(defn NameInput
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        create-name (subscribe [::subs/get ::spec/create-name])]
    [ui/FormInput
     {:label         (str/capitalize (@tr [:name]))
      :placeholder   (@tr [:name-deployment-set])
      :required      true
      :default-value @create-name
      :on-change     (on-change-input ::spec/create-name)}]))

(defn DescriptionInput
  []
  (let [tr                 (subscribe [::i18n-subs/tr])
        create-description (subscribe [::subs/get ::spec/create-description])]
    [ui/FormInput
     {:label         (str/capitalize (@tr [:description]))
      :placeholder   (@tr [:describe-deployment-set])
      :default-value @create-description
      :on-change     (on-change-input ::spec/create-description)}]))

(defn NameDescriptionStep
  []
  [ui/Form
   [NameInput]
   [DescriptionInput]])

; FIXME sketch
(defn DropdownDemo
  [{:keys [text read-only children]}]
  [ui/TableCell
   (if read-only
     [ui/Header text]
     [:<>
      [ui/Dropdown {:selection true
                    :fluid     true
                    :text      text
                    :value     "Hello"
                    :clearable true}]
      ]
     )
   [ui/ListSA
    (for [{:keys [icon text]} children]
      ^{:key (random-uuid)}
      [ui/ListItem
       (if (= icon "kubernetes")
         [ui/Image {:src   "/ui/images/kubernetes-grey.svg"
                    :style {:width   "1.18em"
                            :margin  "0 .25rem 0 0"
                            :display :inline-block}}]
         [ui/ListIcon {:name icon}])
       [ui/ListContent text]])
    #_[ui/ListItem
       [ui/Button {:floated        :right
                   :icon           true
                   :primary        true
                   :basic          true
                   :size           :tiny
                   :label-position :left}
        [ui/Icon {:name "pencil"}]
        "Update"
        ]
       ]
    ]])

; FIXME sketch
(defn RowDemo
  [{:keys [i read-only app-text app-children fleet-text fleet-children]}]
  [ui/TableRow {:vertical-align :top}
   [ui/TableCell (when-not read-only {:style {:padding-top 18}}) [ui/Header i]]
   [DropdownDemo {:read-only read-only
                  :text      app-text
                  :children  app-children}]
   [ui/TableCell (when-not read-only {:style {:padding-top 15}}) "➔"]
   [DropdownDemo {:read-only read-only
                  :text      fleet-text
                  :children  fleet-children}]])

(defn AppsList
  [i applications]
  [ui/ListSA
   (for [{:keys [id]} applications]
     ^{:key (str "apps-set-" i "-" id)}
     [module-plugin/ModuleNameIcon
      {:db-path [::spec/apps-sets i]
       :href    id}])])

(defn AppsSet
  [i {:keys [name description applications] :as apps-set}]
  [:<>
   [ui/Header {:as "h4"} name
    (when description
      [ui/HeaderSubheader description])]
   [AppsList i applications]])

(defn SelectTargetsModal
  [_i]
  (fn [i]
    (let [targets-selected @(subscribe [::subs/targets-selected i])
          db-path          [::spec/apps-sets i ::spec/targets]
          on-open          #(dispatch [::target-selector/restore-selected
                                       db-path (map :id targets-selected)])
          on-done          #(dispatch [::events/set-targets-selected i db-path])
          ]
      [ui/Modal
       {:close-icon true
        :trigger    (r/as-element
                      [ui/Icon {:name     "add"
                                :color    "green"
                                :on-click on-open}])
        :header     "New apps set"
        :content    (r/as-element
                      [ui/ModalContent
                       [target-selector/TargetsSelectorSection
                        {:db-path db-path}]])
        :actions    [{:key "cancel", :content "Cancel"}
                     {:key     "done", :content "Done" :positive true
                      :onClick on-done}]}])))


(defn TargetIcon
  [subtype]
  (condp = subtype
    "infrastructure-service-swarm" [ui/Icon {:name "docker"}]
    "infrastructure-service-kubernetes" [apps-utils/IconK8s false]
    [ui/Icon {:name "question circle"}]))
(defn TargetNameIcon
  [{:keys [subtype name] target-id :id} on-delete]
  [ui/ListItem
   [TargetIcon subtype]
   [ui/ListContent (or name target-id) " "
    (when on-delete
      [ui/Icon {:name     "close" :color "red" :link true
                :on-click #(on-delete target-id)}])]])


(defn TargetsList
  [i & {:keys [editable?]
        :or   {editable? true} :as _opts}]
  (let [selected  @(subscribe [::subs/targets-selected i])
        on-delete #(dispatch [::events/remove-target i %])]
    [ui/ListSA
     (for [target selected]
       ^{:key (:id target)}
       [TargetNameIcon target on-delete])]))

(defn TargetsSet
  [i apps-set]
  [:<>
   [:h4]
   [TargetsList i]
   (when (-> apps-set count pos?)
     [SelectTargetsModal i])]
  #_[DropdownDemo {:read-only read-only
                   :text      fleet-text
                   :children  fleet-children}])

(defn AppsSetRow
  [{:keys [i apps-set read-only fleet-text fleet-children]}]
  [ui/TableRow {:vertical-align :top}
   [ui/TableCell {:width 2}
    [ui/Header (inc i)]]
   [ui/TableCell {:width 6}
    [AppsSet i apps-set]]
   [ui/TableCell
    (cond-> {:width 2}
            (not read-only)
            (assoc-in [:style :padding-top] 15)) "➔"]
   [ui/TableCell {:width 6}
    [TargetsSet i apps-set]]])

(defn AppsSets
  [{:keys [read-only]
    :or   {read-only false}}]
  (let [applications-sets (subscribe [::subs/applications-sets])
        content           [ui/Table {:compact    true
                                     :definition true}
                           [ui/TableHeader
                            [ui/TableRow
                             [ui/TableHeaderCell]
                             [ui/TableHeaderCell "Apps sets"]
                             [ui/TableHeaderCell]
                             [ui/TableHeaderCell "Targets sets"]]]

                           [ui/TableBody
                            (for [[i apps-set] (map-indexed vector @applications-sets)]
                              ^{:key (str "apps-set-" i)}
                              [AppsSetRow {:i              i
                                           :apps-set       apps-set
                                           :read-only      read-only
                                           :fleet-text     "Geneva"
                                           :fleet-children [
                                                            {:text "NuvlaEdge demo 1" :icon "box"}
                                                            {:text "NuvlaEdge demo 2" :icon "box"}
                                                            {:text "NuvlaEdge demo 3" :icon "box"}
                                                            {:text "NuvlaEdge demo 4" :icon "box"}
                                                            {:text "NuvlaEdge demo 5" :icon "box"}
                                                            ]
                                           }])
                            #_[AppsSet {:i              1
                                        :read-only      read-only
                                        :app-text       "Blackbox"
                                        :app-children   [
                                                         {:text "App 1" :icon "cubes"}
                                                         {:text "App 2" :icon "cubes"}
                                                         {:text "App 3" :icon "cubes"}
                                                         ]
                                        :fleet-text     "Geneva"
                                        :fleet-children [
                                                         {:text "NuvlaEdge demo 1" :icon "box"}
                                                         {:text "NuvlaEdge demo 2" :icon "box"}
                                                         {:text "NuvlaEdge demo 3" :icon "box"}
                                                         {:text "NuvlaEdge demo 4" :icon "box"}
                                                         {:text "NuvlaEdge demo 5" :icon "box"}
                                                         ]
                                        }]
                            #_[AppsSet {:i              2
                                        :read-only      read-only
                                        :app-text       "Whitebox"
                                        :app-children   [
                                                         {:text "App 1" :icon "cubes"}
                                                         {:text "App 5" :icon "cubes"}
                                                         ]
                                        :fleet-text     "Zurich"
                                        :fleet-children [
                                                         {:text "NuvlaEdge demo 6" :icon "box"}
                                                         {:text "NuvlaEdge demo 7" :icon "box"}
                                                         ]
                                        }]
                            #_[AppsSet {:i              3
                                        :read-only      read-only
                                        :app-text       "Monitoring"
                                        :app-children   [
                                                         {:text "App 6" :icon "kubernetes"}
                                                         {:text "App 7" :icon "kubernetes"}
                                                         ]
                                        :fleet-text     "Exoscale Cloud"
                                        :fleet-children [
                                                         {:text "Cloud demo 1" :icon "cloud"}
                                                         {:text "Cloud demo 2" :icon "cloud"}
                                                         ]
                                        }]
                            #_(when-not read-only
                                [ui/TableRow
                                 [ui/TableCell [ui/Header "4"]]
                                 [ui/TableCell
                                  [ui/Grid
                                   [ui/GridRow {:columns 2}
                                    [ui/GridColumn
                                     [ui/Dropdown {:selection   true
                                                   :fluid       true
                                                   :placeholder "Select apps set"}]]
                                    [ui/GridColumn
                                     [ui/Modal {:trigger (r/as-element
                                                           [ui/Button {:primary  true
                                                                       :circular true}
                                                            "New apps set"]
                                                           )}
                                      [ui/ModalHeader "New apps set"]
                                      [ui/ModalContent
                                       [ui/Input]
                                       [SelectApps]
                                       ]
                                      ]]
                                    ]]]
                                 [ui/TableCell "➔"]
                                 [ui/TableCell
                                  [ui/Grid
                                   [ui/GridRow {:columns 2}
                                    [ui/GridColumn
                                     [ui/Dropdown {:selection   true
                                                   :fluid       true
                                                   :placeholder "Select targets set"}]]
                                    [ui/GridColumn
                                     [ui/Button {:primary  true
                                                 :circular true}
                                      "New targets set"]]
                                    ]]]
                                 ])
                            ]
                           ]]
    (if read-only
      [ui/Segment (merge style/basic {:clearing true})
       content
       [ui/ButtonGroup {:floated "right" :positive true}
        [ui/Button "Create"]
        [ui/ButtonOr]
        [ui/Button {:icon "play" :content "Start"}]
        ]]
      content)))

(defn ConfigureApps
  [i applications]
  [tab/Tab
   {:db-path [::spec/apps-sets i ::spec/configure-set-tab]
    :panes   (map
               (fn [{:keys [id]}]
                 (let [app @(subscribe [::module-plugin/module
                                        [::spec/apps-sets i] id])]
                   {:menuItem {:content (or (:name app) (:id app))
                               :icon    "cubes"
                               :key     (keyword (str "configure-set-" i "-app-" id))}
                    :render   #(r/as-element
                                 [ui/TabPane
                                  [module-plugin/EnvVariables
                                   {:db-path [::spec/apps-sets i]
                                    :href    id}]])})
                 ) applications)}])

(defn ConfigureSets
  []
  (let [panes (map-indexed
                (fn [i {:keys [name applications]}]
                  {:menuItem {:content (str (inc i) " | " name)
                              :key     (keyword (str "configure-set-" i))}
                   :render   #(r/as-element
                                [ConfigureApps i applications])}
                  ) @(subscribe [::subs/applications-sets]))]
    [tab/Tab
     {:db-path                 [::spec/tab-configure-sets]
      :menu                    {:secondary true
                                :pointing  true}
      :panes                   panes
      :ignore-chng-protection? true}]))

(defn AddPage
  []
  (let [tr                  (subscribe [::i18n-subs/tr])
        configure-disabled? (subscribe [::subs/configure-disabled?])]
    (dispatch [::events/new])
    (fn []
      [ui/Container {:fluid true}
       [uix/PageHeader "add" (str/capitalize (@tr [:add]))]
       [step-group/StepGroup
        {:db-path [::spec/steps]
         :size    :mini
         :fluid   true
         :items   [{:key         :name
                    :icon        "bullseye"
                    :content     [NameDescriptionStep]
                    :title       "New deployment set"
                    :description "Give it a name"}
                   {:key         :select-apps-targets
                    :icon        "list"
                    :content     [AppsSets]
                    :title       "Apps / Targets"
                    :description (@tr [:select-applications-targets])}
                   ; FIXME sketch
                   {:key         :configure-new
                    :icon        "configure"
                    :content     [ConfigureSets]
                    :title       (str/capitalize (@tr [:configure]))
                    :description (@tr [:configure-applications])}
                   ; FIXME sketch
                   {:key         :summary
                    :icon        "info"
                    :content     [AppsSets {:read-only true}]
                    :title       (str/capitalize (@tr [:summary]))
                    :description (@tr [:overall-summary])}
                   ; FIXME sketch uncomment
                   #_{:key         :select-apps-targets
                      :icon        "list"
                      :content     [StepApplicationsTargets]
                      :title       (@tr [:applications-targets])
                      :description (@tr [:select-applications-targets])}
                   ; FIXME sketch uncomment
                   #_{:key         :configure
                      :icon        "configure"
                      :content     [ConfigureApplications]
                      :title       (str/capitalize (@tr [:configure]))
                      :disabled    @configure-disabled?
                      :description (@tr [:configure-applications])}
                   ; FIXME sketch uncomment
                   #_{:key         :summary
                      :icon        "info"
                      :content     [Summary]
                      :title       (str/capitalize (@tr [:summary]))
                      :description (@tr [:overall-summary])}]}]])))

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
        #(dispatch [::tab/change-tab {:db-path [::spec/tab]
                                      :tab-key :jobs}])]
       [TabsDeploymentSet]]]]))


(defn Details
  [uuid]
  (if (= (str/lower-case uuid) "new")
    [AddPage]
    [DeploymentSet uuid]))
