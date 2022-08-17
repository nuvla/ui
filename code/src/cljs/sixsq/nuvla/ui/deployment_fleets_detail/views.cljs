(ns sixsq.nuvla.ui.deployment-fleets-detail.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.acl.views :as acl]
    [sixsq.nuvla.ui.cimi-detail.views :as cimi-detail-views]
    [sixsq.nuvla.ui.deployments.subs :as deployments-subs]
    [sixsq.nuvla.ui.deployments.views :as deployments-views]
    [sixsq.nuvla.ui.deployment-fleets-detail.events :as events]
    [sixsq.nuvla.ui.deployment-fleets-detail.subs :as subs]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.job.subs :as job-subs]
    [sixsq.nuvla.ui.job.views :as job-views]
    [sixsq.nuvla.ui.deployment-fleets-detail.spec :as spec]
    [sixsq.nuvla.ui.main.components :as components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.session.subs :as session-subs]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as values]
    [sixsq.nuvla.ui.plugins.tab :as tab]
    [sixsq.nuvla.ui.plugins.step-group :as step-group]))


(def refresh-action-id :deployment-fleet-get-deployment-fleet)


(defn refresh
  [uuid]
  (dispatch [::main-events/action-interval-start
             {:id        refresh-action-id
              :frequency 10000
              :event     [::events/get-deployment-fleet (str "deployment-fleet/" uuid)]}]))


(defn MenuBar [uuid]
  (let [deployment-fleet (subscribe [::subs/deployment-fleet])
        loading?         (subscribe [::subs/loading?])]
    (fn []
      (let [MenuItems (cimi-detail-views/format-operations
                        @deployment-fleet
                        #{})]
        [components/StickyBar
         [components/ResponsiveMenuBar
          MenuItems
          [components/RefreshMenu
           {:action-id  refresh-action-id
            :loading?   @loading?
            :on-refresh #(refresh uuid)}]]]))))


(defn EditableCell
  [attribute]
  (let [tr               (subscribe [::i18n-subs/tr])
        deployment-fleet (subscribe [::subs/deployment-fleet])
        can-edit?        (subscribe [::subs/can-edit?])
        id               (:id @deployment-fleet)
        on-change-fn     #(dispatch [::events/edit
                                     id {attribute %}
                                     (@tr [:updated-successfully])])]
    (if @can-edit?
      [components/EditableInput attribute @deployment-fleet on-change-fn]
      [ui/TableCell (get @deployment-fleet attribute)])))


(defn TabOverviewDeploymentFleet
  [{:keys [id created updated created-by]}]
  (let [tr     (subscribe [::i18n-subs/tr])
        locale (subscribe [::i18n-subs/locale])]
    [ui/Segment {:secondary true
                 :color     "blue"
                 :raised    true}
     [:h4 "Deployment fleet"]
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
  [{:keys [id] :as deployment-fleet}]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Segment {:secondary true
                 :color     "teal"
                 :raised    true}
     [:h4 "Tags"]
     [components/EditableTags
      deployment-fleet #(dispatch [::events/edit id {:tags %}
                                   (@tr [:updated-successfully])])]]))



(defn TabOverview
  []
  (let [deployment-fleet (subscribe [::subs/deployment-fleet])]
    (fn []
      (let [{:keys [tags]} @deployment-fleet]
        [ui/TabPane
         [ui/Grid {:columns   2
                   :stackable true
                   :padded    true}
          [ui/GridRow
           [ui/GridColumn {:stretched true}
            [TabOverviewDeploymentFleet @deployment-fleet]]
           [ui/GridColumn {:stretched true}
            [deployments-views/DeploymentsOverviewSegment
             ::deployments-subs/deployments nil nil
             #(tab/change-tab ::spec/tab :deployments)]]]

          (when (seq tags)
            [ui/GridColumn
             [TabOverviewTags @deployment-fleet]])]]))))


(defn TabEvents
  []
  (let [tr                (subscribe [::i18n-subs/tr])
        deployment-fleet  (subscribe [::subs/deployment-fleet])
        all-events        (subscribe [::subs/deployment-fleet-events])
        elements-per-page (subscribe [::subs/elements-per-page])
        total-elements    (get @all-events :count 0)
        total-pages       (general-utils/total-pages total-elements @elements-per-page)
        page              (subscribe [::subs/page])]
    (fn []
      (let [events (:resources @all-events)]
        [ui/TabPane
         (if (and (pos? total-elements) (= (count events) 0))
           [ui/Loader {:active true
                       :inline "centered"}]
           [ui/Table {:basic "very"}
            [ui/TableHeader
             [ui/TableRow
              [ui/TableHeaderCell [:span (@tr [:event])]]
              [ui/TableHeaderCell [:span (@tr [:timestamp])]]
              [ui/TableHeaderCell [:span (@tr [:category])]]
              [ui/TableHeaderCell [:span (@tr [:state])]]]]
            [ui/TableBody
             (for [{:keys [id content timestamp category]} events]
               ^{:key id}
               [ui/TableRow
                [ui/TableCell [values/as-link id :label (general-utils/id->short-uuid id)]]
                [ui/TableCell timestamp]
                [ui/TableCell category]
                [ui/TableCell (:state content)]])]])


         [uix/Pagination {:totalPages   total-pages
                          :activePage   @page
                          :onPageChange (ui-callback/callback
                                          :activePage #(do
                                                         (dispatch [::events/set-page %])
                                                         (refresh (:id @deployment-fleet))))}]]))))


(defn TabsDeploymentFleet
  []
  (let [tr               @(subscribe [::i18n-subs/tr])
        deployment-fleet (subscribe [::subs/deployment-fleet])
        can-edit?        @(subscribe [::subs/can-edit?])]
    [tab/Tab
     {:db-location ::spec/tab
      :panes       [{:menuItem {:content "Overview"
                                :key     :overview
                                :icon    "info"}
                     :render   #(r/as-element [TabOverview])}
                    {:menuItem {:content "Events"
                                :key     :events
                                :icon    "bolt"}
                     :render   #(r/as-element [TabEvents])}
                    {:menuItem {:content "Deployments"
                                :key     :deployments
                                :icon    "rocket"}
                     :render   #(r/as-element [deployments-views/DeploymentTable
                                               {:empty-msg (tr [:empty-deployemnt-msg])}])}
                    (job-views/jobs-section)
                    (acl/TabAcls deployment-fleet can-edit? ::events/edit)]
      :menu        {:secondary true
                    :pointing  true}}]))

(defn Application
  [{:keys [id name description]}]
  (let [selected? @(subscribe [::subs/app-selected? id])]
    [ui/ListItem {:on-click #(dispatch [::events/toggle-select-app id])
                  :style    {:cursor :pointer}}
     [ui/ListIcon {:name (if selected?
                           "check square outline"
                           "square outline")}]
     [ui/ListContent
      [ui/ListHeader (when selected? {:as :a}) name]
      [ui/ListDescription description]]]))

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
  (let [apps     @(subscribe [::subs/apps-tree])
        fulltext @(subscribe [::subs/apps-fulltext-search])
        loading? @(subscribe [::subs/apps-loading?])
        render   (fn []
                   (r/as-element
                     [ui/TabPane {:loading loading?}
                      [components/SearchInput
                       {:on-change     (ui-callback/input-callback #(dispatch [::events/set-apps-fulltext-search %]))
                        :default-value fulltext}]
                      [ui/ListSA
                       [Node (dissoc apps :applications) (:applications apps)]]]))]
    [tab/Tab
     {:db-location ::spec/tab-new-apps
      :panes       [{:menuItem {:content "My apps"
                                :key     :my-apps
                                :icon    "user"}
                     :render   render}
                    {:menuItem {:content "App Store"
                                :key     :app-store
                                :icon    (r/as-element [ui/Icon {:className "fas fa-store"}])}
                     :render   render}
                    {:menuItem {:content "All apps"
                                :key     :all-apps
                                :icon    "grid layout"}
                     :render   render}]
      :on-change   #(dispatch [::events/search-apps])}]))

(defn CredentialItem
  [{:keys [id name description] :as _credential} cred-ids]
  (let [selected? @(subscribe [::subs/creds-selected? [id]])]
    [ui/ListItem {:style    {:cursor :pointer}
                  :on-click #(dispatch [::events/toggle-select-cred id cred-ids])}
     [ui/ListIcon
      [ui/Icon {:name (if selected?
                        "check square outline"
                        "square outline")}]]
     [ui/ListContent
      [ui/ListHeader (when selected? {:as :a}) [ui/Icon {:name "key"}] " " (or name id)]
      (when description
        [ui/ListDescription description])]]))

(defn TargetItem
  [{:keys [id name description credentials] :as _infra-cred}]
  (let [cred-ids       (map :id credentials)
        selected?      (subscribe [::subs/creds-selected? cred-ids])
        multiple-cred? (> (count credentials) 1)]
    [ui/ListItem (when-not multiple-cred?
                   {:style    {:cursor :pointer}
                    :on-click #(dispatch [::events/toggle-select-cred
                                          (-> credentials first :id)])})
     [ui/ListIcon
      [ui/Icon {:name (if @selected?
                        "check square outline"
                        "square outline")}]]
     [ui/ListContent
      [ui/ListHeader (when (and (not multiple-cred?) @selected?) {:as :a})

       [ui/Icon {:name "docker"}] " " (or name id)]
      (when description
        [ui/ListDescription description])
      (when multiple-cred?
        [ui/ListList
         [:<>
          (for [{:keys [id] :as credential} credentials]
            ^{:key id}
            [CredentialItem credential cred-ids])]])]]))

(defn SelectTargets
  []
  (let [apps     @(subscribe [::subs/apps-tree])
        fulltext @(subscribe [::subs/apps-fulltext-search])
        render   (fn []
                   (r/as-element
                     [ui/TabPane
                      [components/SearchInput
                       {:on-change     (ui-callback/input-callback #(dispatch [::events/set-apps-fulltext-search %]))
                        :default-value fulltext}]
                      [ui/ListSA
                       [Node (dissoc apps :applications) (:applications apps)]]]))]
    [tab/Tab
     {:db-location ::spec/tab-new-apps
      :panes       [{:menuItem {:content "My apps"
                                :key     :my-apps
                                :icon    "user"}
                     :render   render}
                    {:menuItem {:content "App Store"
                                :key     :app-store
                                :icon    (r/as-element [ui/Icon {:className "fas fa-store"}])}
                     :render   render}
                    {:menuItem {:content "All apps"
                                :key     :all-apps
                                :icon    "grid layout"}
                     :render   render}]}])
  (let [infra-creds @(subscribe [::subs/creds])
        fulltext    @(subscribe [::subs/creds-fulltext-search])
        render      (fn []
                      (r/as-element
                        [ui/TabPane
                         [components/SearchInput
                          {:on-change     (ui-callback/input-callback #(dispatch [::events/set-creds-fulltext-search %]))
                           :default-value fulltext}]
                         [ui/ListSA
                          [:<>
                           (for [{:keys [id] :as infra-cred} infra-creds]
                             ^{:key id}
                             [TargetItem infra-cred])]]]))]
    [tab/Tab
     {:db-location ::spec/tab-new-targets
      :panes       [{:menuItem {:content "Edges"
                                :key     :edges
                                :icon    "box"}
                     :render   render}
                    {:menuItem {:content "Clouds"
                                :key     :clouds
                                :icon    "cloud"}
                     :render   render}]}]))

(defn CreateButton
  []
  (let [disabled? @(subscribe [::subs/create-disabled?])]
    [ui/Button {:floated  :right
                :primary  true
                :on-click #(step-group/change-step ::spec/steps :billing)
                :disabled disabled?} "create"]))

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
      [SelectTargets]]]]
   [ui/GridRow
    [ui/GridColumn
     [step-group/PreviousNextButtons ::spec/steps]
     [CreateButton]]]])

(defn AddPage
  []
  (let [disabled? (subscribe [::subs/create-disabled?])]
    (dispatch [::events/new])
    (fn []
      (let [items [{:key         :select-apps-targets
                    :icon        "bullseye"
                    :render      (fn [] (r/as-element [StepApplicationsTargets]))
                    :title       "Applications/Targets"
                    :description "Select applications and targets"}
                   {:key         :configure
                    :icon        "configure"
                    :render      (fn [] (r/as-element [:div
                                                       "Configure the applications here"
                                                       [step-group/PreviousNextButtons ::spec/steps]]))
                    :title       "Configure"
                    :disabled    @disabled?
                    :description "Configure applications"}
                   {:key         :license
                    :icon        "book"
                    :render      (fn [] (r/as-element [:div "Accept applications licenses"
                                                       [step-group/PreviousNextButtons ::spec/steps]]))
                    :title       "License"
                    :disabled    @disabled?
                    :description "Accept licenses"}
                   {:key         :price
                    :icon        "euro"
                    :render      (fn [] (r/as-element [:div "Accept prices"
                                                       [step-group/PreviousNextButtons ::spec/steps]]))
                    :title       "Prices"
                    :disabled    @disabled?
                    :description "Accept prices"}
                   {:key         :summary
                    :icon        "info"
                    :render      (fn [] (r/as-element [:div "This will contain a summary"
                                                       [step-group/PreviousNextButtons ::spec/steps]]))
                    :title       "Summary"
                    :description "Enter billing information"}]]
        [ui/Container {:fluid true}
         [uix/PageHeader "add" "Add"]
         [step-group/StepGroup
          {:db-location ::spec/steps
           :size        :mini
           :fluid       true
           :items       items}]
         [step-group/StepPane
          {:db-location ::spec/steps
           :items       items}]]))))

(defn DeploymentFleet
  [uuid]
  (refresh uuid)
  (let [{:keys [id name]} @(subscribe [::subs/deployment-fleet])]
    [components/LoadingPage {:dimmable? true}
     [:<>
      [components/NotFoundPortal
       ::subs/deployment-fleet-not-found?
       :no-deployment-fleet-message-header
       :no-deployment-fleet-message-content]
      [ui/Container {:fluid true}
       [uix/PageHeader "bullseye" (or name id)]
       [MenuBar uuid]
       [components/ErrorJobsMessage
        ::job-subs/jobs nil nil #(tab/change-tab ::spec/tab :jobs)]
       [TabsDeploymentFleet]]]]))


(defn Details
  [uuid]
  (if (= (str/lower-case uuid) "new")
    [AddPage]
    [DeploymentFleet uuid]))
