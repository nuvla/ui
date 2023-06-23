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
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.plugins.bulk-progress :as bulk-progress-plugin]
            [sixsq.nuvla.ui.plugins.events :as events-plugin]
            [sixsq.nuvla.ui.plugins.module :as module-plugin]
            [sixsq.nuvla.ui.plugins.nav-tab :as tab]
            [sixsq.nuvla.ui.plugins.step-group :as step-group]
            [sixsq.nuvla.ui.plugins.target-selector :as target-selector]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.events :as routes-events]
            [sixsq.nuvla.ui.routing.utils :as routes-utils]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
            [sixsq.nuvla.ui.utils.values :as values]
            [sixsq.nuvla.ui.utils.icons :as icons]))


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
     [icons/PlayIcon]
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
     [icons/StopIcon]
     (@tr [:stop])]))


(defn DeleteButton
  [{:keys [id name description] :as _deployment-set}]
  (let [tr      (subscribe [::i18n-subs/tr])
        content (str (or name id) (when description " - ") description)]
    [uix/ModalDanger
     {:on-confirm  #(dispatch [::events/delete])
      :trigger     (r/as-element [ui/MenuItem
                                  [icons/TrashIconFull]
                                  (@tr [:delete])])
      :content     [:h3 content]
      :header      (@tr [:delete-deployment-set])
      :danger-msg  (@tr [:danger-action-cannot-be-undone])
      :button-text (@tr [:delete])}]))

(defn MenuBar
  [uuid]
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
      (let [{:keys [id tags]} @deployment-set]
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
             #(dispatch [::routes-events/navigate
                         (routes-utils/pathify [(routes-utils/name->href routes/deployments)
                                                (str "?deployment=deployment-set='" id "'")])])]]]

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
                  (job-views/jobs-section)
                  (acl/TabAcls {:e          deployment-set
                                :can-edit?  can-edit?
                                :edit-event ::events/edit})]
        :menu    {:secondary true
                  :pointing  true}}])))

(defn on-change-input
  [k]
  (ui-callback/input-callback
    #(dispatch [::events/set k %])))

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

(defn AppsList
  [i applications]
  [ui/ListSA
   (for [{:keys [id]} applications]
     ^{:key (str "apps-set-" i "-" id)}
     [module-plugin/ModuleNameIcon
      {:db-path [::spec/apps-sets i]
       :href    id}])])

(defn AppsSet
  [i {:keys [name description applications] :as _apps-set}]
  [:<>
   [ui/Header {:as "h4"} name
    (when description
      [ui/HeaderSubheader description])]
   [AppsList i applications]])

(defn SelectTargetsModal
  [i subtype]
  (let [targets-selected @(subscribe [::subs/targets-selected i])
        db-path          [::spec/apps-sets i ::spec/targets]
        on-open          #(dispatch [::target-selector/restore-selected
                                     db-path (map :id targets-selected)])
        on-done          #(dispatch [::events/set-targets-selected i db-path])]
    [ui/Modal
     {:close-icon true
      :trigger    (r/as-element
                    [ui/Icon {:class    icons/i-plus-full
                              :color    "green"
                              :on-click on-open}])
      :header     "Select targets sets"
      :content    (r/as-element
                    [ui/ModalContent
                     [target-selector/TargetsSelectorSection
                      {:db-path db-path
                       :subtype subtype}]])
      :actions    [{:key "cancel", :content "Cancel"}
                   {:key     "done", :content "Done" :positive true
                    :onClick on-done}]}]))


(defn TargetIcon
  [subtype]
  (condp = subtype
    "infrastructure-service-swarm" [icons/DockerIcon]
    "infrastructure-service-kubernetes" [apps-utils/IconK8s false]
    [icons/QuestionCircleIcon]))
(defn TargetNameIcon
  [{:keys [subtype name] target-id :id} on-delete]
  [ui/ListItem
   [TargetIcon subtype]
   [ui/ListContent (or name target-id) " "
    (when on-delete
      [icons/CloseIcon {:color    "red" :link true
                        :on-click #(on-delete target-id)}])]])


(defn TargetsList
  [i & {:keys [editable?]
        :or   {editable? true} :as _opts}]
  (let [selected  @(subscribe [::subs/targets-selected i])
        on-delete (when editable?
                    #(dispatch [::events/remove-target i %]))]
    (when (seq selected)
      [ui/ListSA
       (for [target selected]
         ^{:key (:id target)}
         [TargetNameIcon target on-delete])])))

(defn TargetsSet
  [i apps-set editable?]
  [:<>
   [TargetsList i :editable? editable?]
   (when (and (-> apps-set count pos?)
              editable?)
     [SelectTargetsModal i (:subtype apps-set)])])

(defn AppsSetRow
  [{:keys [i apps-set summary-page]}]
  [ui/TableRow {:vertical-align :top}
   [ui/TableCell {:width 2}
    [ui/Header (inc i)]]
   [ui/TableCell {:width 6}
    [AppsSet i apps-set]]
   [ui/TableCell {:width 2} "➔"]
   [ui/TableCell {:width 6}
    [TargetsSet i apps-set (not summary-page)]]])

(defn MenuBarNew
  []
  (let [tr        @(subscribe [::i18n-subs/tr])
        disabled? @(subscribe [::subs/create-start-disabled?])
        on-click  #(dispatch [::events/save-start %])]
    [ui/Menu
     [ui/MenuItem {:disabled disabled?
                   :on-click (partial on-click false)}
      [icons/FloppyIcon]
      (str/capitalize (tr [:save]))]
     [ui/MenuItem {:disabled disabled?
                   :on-click (partial on-click true)}
      [icons/PlayIcon]
      (str/capitalize (tr [:start]))]]))

(defn AppsSets
  [{:keys [summary-page]
    :or   {summary-page false}}]
  (let [applications-sets (subscribe [::subs/applications-sets])]
    [ui/Segment (merge style/basic {:clearing true})
     [ui/Table {:compact    true
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
         [AppsSetRow {:i            i
                      :apps-set     apps-set
                      :summary-page summary-page}])]]]))

(defn EnvVariablesApp
  [i module-id]
  (let [tr (subscribe [::i18n-subs/tr])]
    [uix/Accordion
     [module-plugin/EnvVariables
      {:db-path [::spec/apps-sets i]
       :href    module-id}]
     :label (@tr [:env-variables])]))

(defn RegistriesCredsApp
  [i module-id]
  (let [tr (subscribe [::i18n-subs/tr])]
    [uix/Accordion
     [module-plugin/RegistriesCredentials
      {:db-path [::spec/apps-sets i]
       :href    module-id}]
     :label (@tr [:private-registries])]))

(defn ConfigureApps
  [i applications]
  ^{:key (str "set-" i)}
  [ui/Tab
   {:menu  {:attached false}
    :panes (map
             (fn [{:keys [id]}]
               (let [app @(subscribe [::module-plugin/module
                                      [::spec/apps-sets i] id])]
                 {:menuItem {:content (or (:name app) (:id app))
                             :icon    "cubes"
                             :key     (keyword (str "configure-set-" i "-app-" id))}
                  :render   #(r/as-element
                               [ui/TabPane
                                [EnvVariablesApp i id]
                                [RegistriesCredsApp i id]])})
               ) applications)}])

(defn BoldLabel
  [txt]
  [:label [:b txt]])

(defn EULA
  []
  (let [tr       @(subscribe [::i18n-subs/tr])
        licenses @(subscribe [::subs/deployment-set-licenses])
        checked? @(subscribe [::subs/get ::spec/licenses-accepted?])]
    [ui/Segment {:attached true}
     (if (seq licenses)
       [:<>
        [ui/ListSA {:divided true}
         (for [[{:keys [name description url] :as license} sets-apps-targets] licenses]
           ^{:key (str "accept-eula-" license)}
           [ui/ListItem
            [ui/ListIcon {:name "book"}]
            [ui/ListContent
             [ui/ListHeader {:as     :a
                             :target "_blank"
                             :href   url
                             } name]
             (when description
               [ui/ListDescription description])
             [ui/ListList
              (for [{i            :i
                     {:keys [id]} :application} sets-apps-targets]
                ^{:key (str "license-" i "-" id)}
                [module-plugin/ModuleNameIcon
                 {:db-path [::spec/apps-sets i]
                  :href    id}])]]])]
        [ui/Form
         [ui/FormCheckbox {:label     (r/as-element [BoldLabel (tr [:accept-eulas])])
                           :required  true
                           :checked   checked?
                           :on-change (ui-callback/checked
                                        #(dispatch [::events/set
                                                    ::spec/licenses-accepted? %]))}]]]
       [ui/Message (tr [:eula-not-defined])])]))

(defn Prices
  []
  (let [tr                       @(subscribe [::i18n-subs/tr])
        apps-targets-total-price @(subscribe [::subs/deployment-set-apps-targets-total-price])
        checked?                 @(subscribe [::subs/get ::spec/prices-accepted?])
        dep-set-total-price      @(subscribe [::subs/deployment-set-total-price])]
    [ui/Segment {:attached true}
     (if (seq apps-targets-total-price)
       [:<>
        [ui/Table
         [ui/TableHeader
          [ui/TableRow
           [ui/TableHeaderCell (str/capitalize (tr [:application]))]
           [ui/TableHeaderCell {:text-align "right"} (tr [:daily-unit-price])]
           [ui/TableHeaderCell {:text-align "right"} (tr [:quantity])]
           [ui/TableHeaderCell {:text-align "right"} (tr [:daily-price])]]]
         [ui/TableBody
          (for [{:keys [i targets-count total-price application]} apps-targets-total-price]
            ^{:key (str "price-" i "-" (:id application))}
            [ui/TableRow
             [ui/TableCell [values/AsLink (:path application)
                            :label (or (:name application)
                                       (:id application)) :page "apps"]]
             [ui/TableCell {:text-align "right"} (general-utils/format-money
                                                   (/ (get-in application [:price :cent-amount-daily]) 100))]
             [ui/TableCell {:text-align "right"} targets-count]
             [ui/TableCell {:text-align "right"} (general-utils/format-money (/ total-price 100))]])
          [ui/TableRow {:active true}
           [ui/TableCell [:b (str/capitalize (tr [:total]))]]
           [ui/TableCell]
           [ui/TableCell]
           [ui/TableCell {:text-align "right"}
            [:b (str (tr [:total-price]) ": " (general-utils/format-money (/ dep-set-total-price 100)) "/" (tr [:day]))]]]]]
        [ui/Form {:size "big"}
         [ui/FormCheckbox {:label     (r/as-element [BoldLabel (tr [:accept-prices])])
                           :required  true
                           :checked   checked?
                           :on-change (ui-callback/checked
                                        #(dispatch [::events/set
                                                    ::spec/prices-accepted? %]))}]]]
       [ui/Message (tr [:free-app])])]))

(defn EulaPrices
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [:div
     [ui/Header {:as :h5 :attached "top"}
      (@tr [:eula-full])]
     [EULA]
     [ui/Header {:as :h5 :attached "top"}
      (str/capitalize (@tr [:total-price]))]
     [Prices]]))

(defn SelectTargetsConfigureSets
  []
  [ui/Tab
   {:menu  {:secondary true
            :pointing  true}
    :panes (map-indexed
             (fn [i {:keys [name applications] :as apps-set}]
               {:menuItem {:content (str (inc i) " | " name)
                           :key     (keyword (str "configure-set-" i))}
                :render   #(r/as-element
                             [:div
                              [ui/Header {:as :h5 :attached "top"}
                               "Targets sets"]
                              [ui/Segment {:attached true}
                               [TargetsSet i apps-set true]]
                              [ui/Header {:as :h5 :attached "top"}
                               "Configure"]
                              [ui/Segment {:attached true}
                               [ConfigureApps i applications]]])}
               ) @(subscribe [::subs/applications-sets]))}])

(defn Summary
  []
  [:<>
   [MenuBarNew]
   [AppsSets {:summary-page true}]])

(defn StepDescription
  [description]
  [:div {:style {:overflow-wrap "break-word"
                 :width         "16ch"}}
   description])

(defn AddPage
  []
  (let [tr    (subscribe [::i18n-subs/tr])
        items [{:key         :name
                :icon        icons/i-bullseye
                :content     [NameDescriptionStep]
                :title       "New deployment set"
                :description "Give it a name"
                :subs        ::subs/step-name-complete?}
               {:key         :select-targets-and-configure-sets
                :icon        icons/i-list
                :content     [SelectTargetsConfigureSets]
                :title       "Apps / Targets"
                :description (@tr [:select-targets-and-configure-applications])
                :subs        ::subs/step-apps-targets-complete?}
               {:key         :eula-price
                :icon        icons/i-book
                :content     [EulaPrices]
                :title       (@tr [:eula-price])
                :description (@tr [:eula-and-total-price])
                :subs        ::subs/step-licenses-prices-complete?}
               {:key         :summary
                :icon        icons/i-info
                :content     [Summary]
                :title       (str/capitalize (@tr [:summary]))
                :description (@tr [:overall-summary])}]]
    (dispatch [::events/new])
    (fn []
      [ui/Container {:fluid true}
       [uix/PageHeader "add" (str/capitalize (@tr [:add]))]
       [step-group/StepGroup
        {:db-path [::spec/steps]
         :size    :mini
         :style   {:flex-wrap "wrap"}
         :fluid   true
         :items   (mapv (fn [{:keys [description icon subs] :as item}]
                          (assoc item
                            :description
                            (r/as-element
                              [StepDescription description])
                            :icon (r/as-element [icons/Icon {:name icon}])
                            :completed (when subs @(subscribe [subs]))))
                        items)}]])))

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
