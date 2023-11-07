(ns sixsq.nuvla.ui.deployment-sets-detail.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.apps-store.spec :as apps-store-spec]
            [sixsq.nuvla.ui.apps-store.subs :as apps-store-subs]
            [sixsq.nuvla.ui.apps-store.views :as apps-store-views]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.apps.views-detail :refer [AuthorVendorForModule]]
            [sixsq.nuvla.ui.cimi-detail.views :as cimi-detail-views]
            [sixsq.nuvla.ui.dashboard.views :as dashboard-views]
            [sixsq.nuvla.ui.deployment-sets-detail.events :as events]
            [sixsq.nuvla.ui.deployment-sets-detail.spec :as spec]
            [sixsq.nuvla.ui.deployment-sets-detail.subs :as subs]
            [sixsq.nuvla.ui.deployments.subs :as deployments-subs]
            [sixsq.nuvla.ui.deployments.views :as dv]
            [sixsq.nuvla.ui.edges.spec :as edges-spec]
            [sixsq.nuvla.ui.edges.utils :as edges-utils]
            [sixsq.nuvla.ui.edges.views :as edges-views]
            [sixsq.nuvla.ui.filter-comp.views :as filter-comp]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.job.subs :as job-subs]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.plugins.bulk-progress :as bulk-progress-plugin]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.plugins.module :as module-plugin]
            [sixsq.nuvla.ui.plugins.nav-tab :as tab]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.plugins.step-group :as step-group]
            [sixsq.nuvla.ui.plugins.table :as table-plugin :refer [Table]]
            [sixsq.nuvla.ui.plugins.target-selector :as target-selector]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.subs :as route-subs]
            [sixsq.nuvla.ui.routing.utils :as routes-utils :refer [name->href
                                                                   pathify]]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.forms :as utils-forms]
            [sixsq.nuvla.ui.utils.general :as general-utils :refer [format-money]]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
            [sixsq.nuvla.ui.utils.validation :as utils-validation]
            [sixsq.nuvla.ui.utils.values :as utils-values]
            [sixsq.nuvla.ui.utils.view-components :as vc]
            [sixsq.nuvla.ui.deployment-sets-detail.events :as depl-group-events]))

(defn- create-wrng-msg
  [apps-count edges-count action]
  (str "You're about to " action " " apps-count " app"
       (if (< 1 apps-count) "s " " ") "on "
       edges-count " device"
       (if (< 1 edges-count) "s. " ". ") "Proceed?"))

(defn- ops-status-pending-str [tr-fn ops-status]
  (str/join ", "
            (map (fn [[k v]]
                   (str (count v) " " (tr-fn [k])))
                 (dissoc ops-status :status))))

(defn- depl-set->modal-content
  [{:keys [name id description]}]
  (str (or name id) (when description " - ") description))

(defn GuardedMenuItem
  [{:keys [validation-sub]} & _children]
  (let [tr         (subscribe [::i18n-subs/tr])
        validation (subscribe validation-sub)]
    (fn [{:keys [enabled] :or {enabled true}} & children]
      (let [{:keys [valid? errors]} @validation
            enabled?  (and valid? enabled)
            menu-item (into [ui/MenuItem
                             {:as       :a
                              :disabled (not enabled?)
                              :class    (when enabled? "primary-menu-item")}]
                            children)]
        (if valid?
          menu-item
          [ui/Popup
           {:trigger (r/as-element menu-item)
            :content (str/join ". " (map (comp @tr :message) errors))}])))))

(defn StartButton
  [{:keys [id] :as deployment-set} warn-msg]
  (let [tr       (subscribe [::i18n-subs/tr])
        enabled? @(subscribe [::subs/operation-enabled? "start"])]
    [uix/ModalDanger
     {:on-confirm         (fn [_]
                            (dispatch [::events/operation
                                       {:resource-id id
                                        :operation   "start"}]))
      :trigger            (r/as-element
                            [:div
                             [GuardedMenuItem
                              {:enabled        enabled?
                               :validation-sub [::subs/deployment-set-validation]}
                              [icons/PlayIcon]
                              (str/capitalize (@tr [:start]))]])
      :content            [:h3 (depl-set->modal-content deployment-set)]
      :header             (@tr [:start-deployment-set])
      :danger-msg-header  (@tr [:are-you-sure-you-want-to-continue?])
      :danger-msg         warn-msg
      :button-text        (@tr [:start])
      :with-confirm-step? true}]
    ))

(defn StopButton
  [{:keys [id] :as deployment-set} warn-msg]
  (let [tr       (subscribe [::i18n-subs/tr])
        enabled? (subscribe [::subs/operation-enabled? "stop"])]
    [uix/ModalDanger
     {:on-confirm         (fn [_]
                            (dispatch [::events/operation
                                       {:resource-id id
                                        :operation   "stop"}]))
      :trigger            (r/as-element
                            [ui/MenuItem
                             {:disabled (not @enabled?)}
                             [icons/StopIcon]
                             (str/capitalize (@tr [:stop]))])
      :content            [:h3 (depl-set->modal-content deployment-set)]
      :header             (@tr [:stop-deployment-set])
      :danger-msg-header  (@tr [:are-you-sure-you-want-to-continue?])
      :danger-msg         warn-msg
      :button-text        (@tr [:stop])
      :with-confirm-step? true}]))


(defn UpdateButton
  [{:keys [id] :as deployment-set} warn-msg]
  (let [tr       (subscribe [::i18n-subs/tr])
        enabled? @(subscribe [::subs/operation-enabled? "update"])]
    [uix/ModalDanger
     {:on-confirm         (fn [_]
                            (dispatch [::events/operation
                                       {:resource-id id
                                        :operation   "update"}]))
      :trigger            (r/as-element
                            [:div
                             [GuardedMenuItem
                              {:enabled        enabled?
                               :validation-sub [::subs/deployment-set-validation]}
                              [icons/RedoIcon]
                              (str/capitalize (@tr [:update]))]])
      :content            [:h3 (depl-set->modal-content deployment-set)]
      :header             (@tr [:update-deployment-set])
      :danger-msg-header  (@tr [:are-you-sure-you-want-to-continue?])
      :danger-msg         warn-msg
      :button-text        (@tr [:update])
      :modal-action       [:p warn-msg]
      :with-confirm-step? true}]))

(defn CancelOperationButton
  [{:keys [id] :as deployment-set}]
  (let [tr       (subscribe [::i18n-subs/tr])
        enabled? (subscribe [::subs/operation-enabled? "cancel"])]
    [uix/ModalDanger
     {:on-confirm         (fn [_]
                            (dispatch [::events/operation
                                       {:resource-id id
                                        :operation   "cancel"}]))
      :trigger            (r/as-element
                            [ui/MenuItem
                             {:disabled (not @enabled?)}
                             [icons/BanIcon]
                             (str/capitalize (@tr [:cancel]))])
      :content            [:h3 (depl-set->modal-content deployment-set)]
      :header             (@tr [:cancel-deployment-set-operation])
      :danger-msg-header  (@tr [:are-you-sure-you-want-to-continue?])
      :danger-msg         (@tr [:cancel-deployment-set-operation-warn-msg])
      :button-text        (@tr [:cancel])
      :with-confirm-step? true}]))

(def recompute-fleet-modal-id :modal/recompute-fleet)

(defn RecomputeFleetButton
  [deployment-set]
  (let [tr                   (subscribe [::i18n-subs/tr])
        can-recompute-fleet? (general-utils/can-operation? "recompute-fleet" deployment-set)
        unsaved-changes?     (subscribe [::subs/unsaved-changes?])
        open?                (subscribe [::subs/modal-open? recompute-fleet-modal-id])
        confirm-fn           (fn []
                               (dispatch [::events/recompute-fleet
                                          #(dispatch [::events/set-opened-modal nil])]))
        close-fn             #(dispatch [::events/set-opened-modal nil])]
    [uix/ModalDanger
     {:on-confirm  confirm-fn
      :trigger     (r/as-element [ui/MenuItem
                                  {:disabled (or @unsaved-changes? (not can-recompute-fleet?))
                                   :on-click (fn [] (dispatch [::events/set-opened-modal recompute-fleet-modal-id]))}
                                  [icons/ArrowRotateIcon]
                                  (str/capitalize (@tr [:recompute-fleet]))])
      :open        @open?
      :on-close    close-fn
      :header      (@tr [:recompute-deployment-set-fleet])
      :danger-msg  (@tr [:recompute-fleet-warning])
      :button-text (@tr [:recompute-fleet])}]))

(defn DeleteButton
  [deployment-set warn-msg]
  (let [tr         (subscribe [::i18n-subs/tr])
        content    (depl-set->modal-content deployment-set)
        deletable? (general-utils/can-operation? "delete" deployment-set)
        forceable? (general-utils/can-operation? "force-delete" deployment-set)
        {:keys [header danger-msg button-text]}
        (if forceable?
          {:header      (@tr [:force-delete-deployment-set])
           :button-text (str/capitalize (@tr [:force-delete]))
           :danger-msg  (str "Warning! Doing a force delete will leave orphaned containers! "
                             warn-msg)}
          {:header      (@tr [:delete-deployment-set])
           :button-text (str/capitalize (@tr [:delete]))
           :danger-msg  warn-msg})]
    [uix/ModalDanger
     {:on-confirm         #(dispatch [::events/delete {:deletable? deletable?
                                                       :forceable? forceable?}])
      :trigger            (r/as-element [ui/MenuItem
                                         {:disabled (and
                                                      (not forceable?)
                                                      (not deletable?))}
                                         [icons/TrashIconFull]
                                         button-text])
      :content            [:h3 content]
      :header             header
      :danger-msg         danger-msg
      :button-text        button-text
      :with-confirm-step? true}]))

(def save-modal-id ::save-modal)

(defn- UnstoredEdgeChanges
  [fleet-changes]
  (let [removed (:removed fleet-changes)
        added   (:added fleet-changes)]
    [:span
     (str "You have unsaved fleet changes" ": "
       (when removed (str (count removed) " removed"))
       (when (and removed added) ", ")
       (when added (str (count added) " added")))]))

(defn- ChangedStuff
  []
  (let [fleet-changes (subscribe [::subs/fleet-changes])
        depl-group-selected-fields (subscribe [::subs/select-keys-stored-and-edited [:name :description]])]
    (fn []
      (let [{:keys [stored edited]} @depl-group-selected-fields
            name-changed? (not= (:name stored) (:name edited))
            desc-chagned? (not= (:description stored) (:description edited))]
        [:div
         (when (or name-changed? desc-chagned?)
           [:div [:h3 "Name or description changes"]
            [ui/Table
             [ui/TableHeader
              [ui/TableRow
               [ui/TableCell "Field"]
               [ui/TableCell "Saved value"]
               [ui/TableCell "Changed to"]]
              ]
             [ui/TableBody
              (when desc-chagned?
                [ui/TableRow
                 [ui/TableCell "Description"]
                 [ui/TableCell (:description stored)]
                 [ui/TableCell (:description edited)]])
              (when name-changed?
                [ui/TableRow
                 [ui/TableCell "Name"]
                 [ui/TableCell (:name stored)]
                 [ui/TableCell (:name edited)]])]]])
         [:div [:h3 "Fleet changes"]
          (if @fleet-changes
            [UnstoredEdgeChanges @fleet-changes]
            [:div "no changes in your fleet"])]]))))

(defn SaveButton
  [{:keys [creating?]}]
  (let [tr            (subscribe [::i18n-subs/tr])
        save-enabled? (subscribe [::subs/save-enabled? creating?])
        validation    (subscribe [::subs/deployment-set-validation])
        modal-open?   (subscribe [::subs/modal-open? save-modal-id])]
    (fn [{:keys [deployment-set]}]
      (let [on-confirm    (if creating?
                            #(dispatch [::events/create])
                            (if (:valid? @validation)
                              #(dispatch [::events/do-edit {:deployment-set deployment-set
                                                            :success-msg    (@tr [:updated-successfully])}])
                              #(dispatch [::events/enable-form-validation])))
            menu-item     (r/as-element
                            [:div
                             [uix/MenuItem
                              {:name     (@tr [:save])
                               :icon     icons/i-floppy
                               :disabled (not @save-enabled?)
                               :class    (when @save-enabled? "primary-menu-item")
                               :on-click (if creating?
                                           on-confirm
                                           #(dispatch [::events/set-opened-modal save-modal-id]))}]])]
        (if creating? [ui/Popup
                       {:trigger menu-item
                        :content (@tr [:depl-group-required-fields-before-save])}]
          [:<>
           [uix/ModalDanger
            {:on-confirm         on-confirm
             :open               @modal-open?
             :on-close           #(dispatch [::events/set-opened-modal nil])
             :content            [ChangedStuff]
             :header             [:h3 "Saving changes to deployment group"]
              ;;  :danger-msg         "Are yu sure?"
             :button-text        "sure sure?"
             :with-confirm-step? true}]
           menu-item])))))

(defn CancelButton
  []
  (let [tr              (subscribe [::i18n-subs/tr])
        cancel-enabled? (subscribe [::subs/cancel-enabled?])]
    (fn []
      (let [on-confirm #(dispatch [::events/cancel-editing])]
        [uix/MenuItem
         {:name     (@tr [:cancel])
          :icon     icons/i-eraser
          :disabled (not @cancel-enabled?)
          :on-click #(dispatch [::main-events/revert-changes-modal on-confirm])}]))))

(defn MenuBar
  []
  (let [deployment-set (subscribe [::subs/deployment-set])
        loading?       (subscribe [::subs/loading?])
        save-enabled?  (subscribe [::subs/save-enabled?])
        apps-count     (subscribe [::subs/apps-count])
        edges-count    (subscribe [::subs/edges-count])
        fleet-filter   (subscribe [::subs/fleet-filter])
        tr             (subscribe [::i18n-subs/tr])]
    (fn []
      (let [warn-msg-fn (partial create-wrng-msg @apps-count @edges-count)]
        [components/StickyBar
         [components/ResponsiveMenuBar
          [^{:key "save"}
           [SaveButton {:deployment-set @deployment-set}]
           (when @save-enabled?
             ^{:key "cancel"}
             [CancelButton])
           ^{:key "start"}
           [StartButton @deployment-set (warn-msg-fn "start")]
           ^{:key "update"}
           [UpdateButton @deployment-set
            (str
              "You're about to start these updates: "
              (ops-status-pending-str
                @tr
                (:operational-status @deployment-set))
              ". Proceed?")]
           ^{:key "stop"}
           [StopButton @deployment-set (warn-msg-fn "stop")]
           [CancelOperationButton @deployment-set]
           (when @fleet-filter
             ^{:key "recompute-fleet"}
             [RecomputeFleetButton @deployment-set])
           ^{:key "delete"}
           [DeleteButton @deployment-set (warn-msg-fn "delete")]]
          [components/RefreshMenu
           {:action-id  events/refresh-action-depl-set-id
            :loading?   @loading?
            :on-refresh #(events/refresh)}]
          {:max-items-to-show 4}]]))))


(defn MenuBarCreate
  []
  (let [deployment-set (subscribe [::subs/deployment-set])]
    (fn []
      (let [MenuItems (cimi-detail-views/format-operations
                        @deployment-set
                        #{"start" "stop" "delete"})]
        [components/StickyBar
         [components/ResponsiveMenuBar
          (conj MenuItems
                ^{:key "delete"}
                [SaveButton {:creating? true}])]]))))

(defn EditableCell
  [attribute creating?]
  (let [deployment-set (subscribe [::subs/deployment-set])
        can-edit?      (subscribe [::subs/can-edit?])
        on-change-fn   #(dispatch [::events/edit attribute %])]
    [ui/TableCell
     (if (or creating? @can-edit?)
       [components/EditableInput
        {:resource     @deployment-set
         :attribute    attribute
         :on-change-fn on-change-fn}]
       (get @deployment-set attribute))]))

(def ops-status->color
  {"OK"  "green"
   "NOK" "red"})

(defn OperationalStatusSummary
  [ops-status]
  (let [tr     (subscribe [::i18n-subs/tr])
        status (:status ops-status)]
    (if (= status
           "OK")
      [:div
       [ui/Icon {:name :circle :color (ops-status->color status)}]
       "Everything is up-to-date"]
      [:div
       [ui/Icon {:name :circle :color (ops-status->color status)}]
       (str "Pending: "
            (ops-status-pending-str @tr ops-status))])))

(defn TabOverviewDeploymentSet
  [{:keys [id created updated created-by state operational-status]} creating?]
  (let [tr     (subscribe [::i18n-subs/tr])
        locale (subscribe [::i18n-subs/locale])]
    [ui/Segment {:secondary true
                 :color     "blue"
                 :raised    true}
     [:h4 (str (when creating? "Creating a new ") "Deployment group")]
     [ui/Table {:basic  "very"
                :padded false}
      [ui/TableBody
       (when-not creating?
         [:<>
          [ui/TableRow
           [ui/TableCell (str/capitalize (@tr [:state]))]
           [ui/TableCell state]]
          [ui/TableRow
           [ui/TableCell (str/capitalize (@tr [:operational-status]))]
           [ui/TableCell [OperationalStatusSummary operational-status]]]
          [ui/TableRow
           [ui/TableCell "Id"]
           (when id
             [ui/TableCell [utils-values/AsLink id :label (general-utils/id->uuid id)]])]])
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:name]))]
        ^{:key (or id "name")}
        [EditableCell :name creating?]]
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:description]))]
        ^{:key (or id "description")}
        [EditableCell :description creating?]]
       (when-not creating?
         [:<>
          (when created-by
            [ui/TableRow
             [ui/TableCell (str/capitalize (@tr [:created-by]))]
             [ui/TableCell @(subscribe [::session-subs/resolve-user created-by])]])
          [ui/TableRow
           [ui/TableCell (str/capitalize (@tr [:created]))]
           [ui/TableCell (time/ago (time/parse-iso8601 created) @locale)]]
          [ui/TableRow
           [ui/TableCell (str/capitalize (@tr [:updated]))]
           [ui/TableCell (time/ago (time/parse-iso8601 updated) @locale)]]])]]]))


(defn AppPickerCard
  [{:keys [id name description path subtype logo-url price published versions tags] :as app}]
  (let [tr             (subscribe [::i18n-subs/tr])
        map-versions   (apps-utils/map-versions-index versions)
        module-index   (apps-utils/latest-published-index map-versions)
        detail-href    (pathify [(name->href routes/apps) path (when (true? published) (str "?version=" module-index))])
        follow-trial?  (get price :follow-customer-trial false)
        button-icon    (if (and price (not follow-trial?)) :cart icons/i-rocket)
        deploy-price   (str (@tr [(if follow-trial?
                                    :free-trial-and-then
                                    :deploy-for)])
                            (format-money (/ (:cent-amount-daily price) 100)) "/"
                            (@tr [:day]))
        button-content "Add to selection"
        button-ops     {:fluid   true
                        :color   "blue"
                        :icon    button-icon
                        :content button-content}
        desc-summary   (-> description
                           utils-values/markdown->summary
                           (general-utils/truncate 60))]
    [apps-store-views/ModuleCardView
     {:logo-url     logo-url
      :subtype      subtype
      :name         name
      :id           id
      :desc-summary [:<>
                     [:p desc-summary]
                     [:div
                      [:p (str "Project: " (-> (or (:path app) "")
                                               (str/split "/")
                                               first))]
                      [:p "Vendor: " [AuthorVendorForModule app :span]]
                      [:p (str "Price: " deploy-price)]]]
      :tags         tags
      :published    published
      :detail-href  detail-href
      :button-ops   button-ops
      :target       :_blank
      :on-click     (fn [event]
                      (dispatch [::events/add-app-from-picker app])
                      (dispatch [::events/set-opened-modal nil])
                      (dispatch [::full-text-search-plugin/search [::apps-store-spec/modules-search]])
                      (.preventDefault event)
                      (.stopPropagation event))}]))

(defn AddButton
  [id]
  [uix/Button {:on-click (fn [] (dispatch [::events/set-opened-modal id]))
               :icon     icons/i-plus-large
               :style    {:align-self "center"}}])

(defn AppsPicker
  [tab-key pagination-db-path]
  (let [modules (subscribe [::apps-store-subs/modules])]
    (fn []
      ^{:key tab-key}
      [ui/TabPane
       [ui/Menu {:secondary true}
        [ui/MenuMenu {:position "left"}
         [full-text-search-plugin/FullTextSearch
          {:db-path      [::apps-store-spec/modules-search]
           :change-event [::pagination-plugin/change-page [pagination-db-path] 1]}]]]
       [apps-store-views/ModulesCardsGroupView
        (for [{:keys [id] :as module} (get @modules :resources [])]
          ^{:key id}
          [AppPickerCard module])]
       [pagination-plugin/Pagination
        {:db-path      [pagination-db-path]
         :total-items  (:count @modules)
         :change-event [::events/fetch-app-picker-apps pagination-db-path]}]])))

(defn AppsPickerModal
  [creating?]
  (let [tr       (subscribe [::i18n-subs/tr])
        open?    (subscribe [::subs/modal-open? events/apps-picker-modal-id])
        close-fn #(dispatch [::events/set-opened-modal nil])
        tab-key  apps-store-spec/allapps-key]
    (dispatch [::events/fetch-app-picker-apps ::spec/pagination-apps-picker])
    (fn []
      [ui/Modal {:size       :fullscreen
                 :open       @open?
                 :close-icon true
                 :on-close   close-fn}
       [uix/ModalHeader {:header (@tr (if creating?
                                        [:create-deployment-group]
                                        [:edit-deployment-group]))}]
       [ui/ModalContent
        [AppsPicker tab-key ::spec/pagination-apps-picker]]])))

(defn- create-app-config-query-key [i id]
  (keyword (str "configure-set-" i "-app-" id)))

(defn- AppsOverviewTable
  [creating?]
  (let [tr                     (subscribe [::i18n-subs/tr])
        locale                 (subscribe [::i18n-subs/locale])
        apps-row               (subscribe [::subs/apps-row-data])
        apps-validation-error? (subscribe [::subs/apps-validation-error?])
        k->tr-k                {:app :name}]
    (fn []
      [:<>
       (when (seq @apps-row)
         [:div {:style {:height "100%"}}
          [Table {:columns
                  (into
                    (vec
                      (map-indexed
                        (fn [i k]
                          {:field-key      k
                           :header-content (-> (or (@tr [(k->tr-k k)]) k)
                                               name
                                               str/capitalize)
                           :cell           (case k
                                             :app
                                             (fn [{:keys [cell-data row-data]}]
                                               [:<>
                                                [ui/Popup
                                                 {:content (r/as-element [:p "Configure app"])
                                                  :trigger
                                                  (r/as-element
                                                    [:a
                                                     {:style (when
                                                               (= :removed (:edit-status row-data))
                                                               {:opacity 0.5
                                                                :text-decoration :line-through})
                                                      :href     "#"
                                                      :on-click #(dispatch [::events/navigate-internal
                                                                            {:query-params
                                                                             (merge
                                                                               {(routes-utils/db-path->query-param-key [::apps-config])
                                                                                (create-app-config-query-key i (:href row-data))}
                                                                               {:deployment-sets-detail-tab :apps})}])
                                                      :children [icons/StoreIcon]
                                                      :target   :_self}
                                                     cell-data
                                                     [:span {:style {:margin-left "0.5rem"}}
                                                      [icons/GearIcon]]])}]])
                                             :version
                                             (fn [{{:keys [label created]} :cell-data}]
                                               [ui/Popup
                                                {:content (r/as-element [:p (str (str/capitalize (@tr [:created]))
                                                                              " "
                                                                              (time/ago (time/parse-iso8601 created) @locale))])
                                                 :trigger (r/as-element [:p label " " [icons/InfoIconFull]])}])
                                             nil)})
                        (keys (dissoc (first @apps-row) :idx :href :edit-status))))
                    (remove nil?
                      [{:field-key      :details
                        :header-content (general-utils/capitalize-words (@tr [:details]))
                        :cell           (fn [{:keys [row-data]}]
                                          [ui/Popup
                                           {:content (r/as-element [:p "Open app details"])
                                            :trigger (r/as-element [:span
                                                                    [module-plugin/LinkToApp
                                                                     {:db-path  [::spec/apps-sets (:idx row-data)]
                                                                      :href     (:href row-data)
                                                                      :children [icons/ArrowRightFromBracketIcon]
                                                                      :target   :_self}]])}])}
                       (when (some :edit-status @apps-row)
                         {:field-key :unstored-status
                          :header-cell-props {:style {:width "150px"}}
                          :cell (fn [{{:keys [edit-status]} :row-data}]
                                  (when edit-status (@tr [edit-status])))})
                       {:field-key :remove
                        :header-content ""
                        :header-cell-props {:style {:width "30px"}}
                        :cell      (fn [{:keys [row-data]}]
                                     (let [status (:edit-status row-data)]
                                       (if (= status :removed)
                                         [icons/AddIconFull
                                          {:style    {:cursor :pointer}
                                           :color    "green"
                                           :on-click #(dispatch [::events/re-add-removed-app row-data])}]
                                         [icons/XMarkIcon
                                          {:style    {:cursor :pointer}
                                           :color    "red"
                                           :on-click #(dispatch [::events/remove-app-from-depl-group row-data])}])))}]))
                  :rows (mapv (fn [app]
                                (assoc app :table-row-prop
                                  nil #_{:style {:opacity (when (= (:edit-status app)
                                                             :removed) 0.5)}}))
                          @apps-row)}]])
       [:div {:style {:display :flex :justify-content :center :align-items :center}}
        [:<>
         [AppsPickerModal creating?]
         [:div {:style {:margin-top   "1rem"
                        :margin-bottm "1rem"}}
          [AddButton events/apps-picker-modal-id]]]]
       [:div {:style {:margin-top   "1rem"
                      :margin-left  "auto"
                      :margin-right "auto"}}
        (if @apps-validation-error?
          [:span {:style {:color :red}} (@tr [:select-at-least-one-app])]
          (if (empty? @apps-row)
            (@tr [:add-your-first-app])
            (@tr [:add-app])))]])))


(defn StatisticStatesEdgeView [{:keys [total online offline unknown]}]
  (let [current-route     @(subscribe [::route-subs/current-route])
        to-edges-tab      {:deployment-sets-detail-tab :edges}
        create-target-url (fn [status-filter]
                            {:resource (routes-utils/gen-href
                                         current-route
                                         {:partial-query-params
                                          (cond->
                                            to-edges-tab
                                            status-filter
                                            (assoc events/edges-state-filter-key status-filter))})})]
    [ui/StatisticGroup {:size  "tiny"
                        :style {:padding "0.2rem"}}
     [components/StatisticState {:clickable? true
                                 :value      total
                                 :stacked?   true
                                 :icons      [icons/i-box]
                                 :label      "TOTAL"
                                 :color      "black"
                                 :on-click   #(dispatch [::routing-events/navigate
                                                         (:resource (create-target-url nil))])}]
     [dashboard-views/Statistic {:value          online
                                 :icon           icons/i-power
                                 :label          edges-utils/status-online
                                 :positive-color "green"
                                 :color          "green"
                                 :target         (create-target-url "ONLINE")}]
     [dashboard-views/Statistic {:value  offline
                                 :icon   icons/i-power
                                 :label  edges-utils/status-offline
                                 :color  "red"
                                 :target (create-target-url "OFFLINE")}]
     [dashboard-views/Statistic {:value  unknown
                                 :icon   icons/i-power
                                 :label  edges-utils/status-unknown
                                 :color  "orange"
                                 :target (create-target-url "UNKNOWN")}]]))

(defn create-nav-fn
  ([tab added-params]
   #(dispatch [::routing-events/change-query-param
               {:push-state? true
                :partial-query-params
                (merge
                  {(routes-utils/db-path->query-param-key [::spec/tab])
                   tab}
                  added-params)}])))

(defn- DeploymentStatesFilter [state-filter]
  [dv/StatisticStates true ::deployments-subs/deployments-summary-all
   (mapv (fn [state] (assoc state
                       :on-click
                       (create-nav-fn "deployments" {:depl-state (:label state)})
                       :selected? (or
                                    (= state-filter (:label state))
                                    (and
                                      (nil? state-filter)
                                      (= "TOTAL" (:label state))))))
         dv/default-states)])

(defn- DeploymentsStatesCard
  [state-filter]
  (let [deployments (subscribe [::deployments-subs/deployments])]
    (fn []
      [dv/TitledCardDeployments
       [DeploymentStatesFilter state-filter]
       [uix/Button {:class    "center"
                    :color    "blue"
                    :icon     icons/i-rocket
                    :disabled (or
                                (nil? (:count @deployments))
                                (= 0 (:count @deployments)))
                    :content  "Show me"
                    :on-click (create-nav-fn "deployments" nil)}]])))

(defn FleetFilterMessage []
  (let [tr                   (subscribe [::i18n-subs/tr])
        fleet-filter         (subscribe [::subs/fleet-filter])
        deployment-set       (subscribe [::subs/deployment-set])
        can-recompute-fleet? (general-utils/can-operation? "recompute-fleet" @deployment-set)
        unsaved-changes?     (subscribe [::subs/unsaved-changes?])]
    (when @fleet-filter
      [:p (@tr [:recompute-fleet-info]) " "
       (when (and can-recompute-fleet? (not @unsaved-changes?))
         [:a {:href "#"
              :on-click (fn [] (dispatch [::events/set-opened-modal recompute-fleet-modal-id]))}
          (@tr [:recompute-fleet])])])))


(defn- ResolvedUser
  [user-id]
  (fn []
    (let [user (subscribe [::session-subs/resolve-user user-id])]
      @user)))

(defn EdgePickerContent
  []
  (let [edges             (subscribe [::subs/edge-picker-edges-resources])
        edges-count       (subscribe [::subs/edge-picker-edges-count])
        edges-stats       (subscribe [::subs/edge-picker-edges-summary-stats])
        selected-state    (subscribe [::subs/state-selector])
        additional-filter (subscribe [::subs/edge-picker-additional-filter])
        filter-open?      (r/atom false)]
    (fn []
      (let [select-fn (fn [id] (dispatch [::table-plugin/select-id id [::spec/edge-picker-select] (map :id @edges)]))]
        [:<>
         [:div {:style {:display :flex}}
          [:div
           [full-text-search-plugin/FullTextSearch
            {:db-path [::spec/edge-picker-full-text-search]
             :change-event [::pagination-plugin/change-page [::spec/edge-picker-pagination] 1]}]
           ^{:key @additional-filter}
           [:div {:style {:margin-top "0.4rem"}}
            [filter-comp/ButtonFilter
             {:resource-name                    edges-spec/resource-name
              :default-filter                   @additional-filter
              :open?                            filter-open?
              :on-done                          #(dispatch [::events/set-edge-picker-additional-filter %])
              :show-clear-button-outside-modal? true
              :persist? false}]]]
          [:div {:class :nuvla-edges
                 :style {:margin "0 auto 0 6rem"}}
           [edges-views/StatisticStatesEdgeView
            (assoc @edges-stats
              :states (mapv (fn [state]
                              (let [label (:label state)]
                                (assoc state
                                  :selected?
                                  (or
                                    (= label @selected-state)
                                    (and
                                      (= label "TOTAL")
                                      (empty? @selected-state)))
                                  :on-click
                                  #(dispatch
                                     [::events/set-edge-picker-selected-state label])))) edges-views/edges-states))
            true true]]]
         [Table {:row-click-handler #(select-fn (:id %))
                 :sort-config       {:db-path     ::spec/edge-picker-ordering
                                     :fetch-event [::events/get-edges-for-edge-picker-modal]}
                 :columns           [{:field-key :name}
                                     {:field-key :state}
                                     {:field-key :created-by
                                      :cell      (fn [data] [ResolvedUser (:cell-data data)])}]
                 :rows              @edges
                 :table-props       {:compact "very" :selectable true}
                 :cell-props        {:header {:single-line true}}
                 :row-props         {:role  "link"
                                     :style {:cursor "pointer"}}
                 :select-config     {:total-count-sub-key [::subs/edge-picker-edges-count]
                                     :resources-sub-key   [::subs/edge-picker-edges-resources]
                                     :select-db-path      [::spec/edge-picker-select]}}]
         [pagination-plugin/Pagination
          {:db-path                [::spec/edge-picker-pagination]
           :change-event           [::events/get-picker-edges]
           :total-items            @edges-count
           :i-per-page-multipliers [1 2 4]}]]))))

(defn EdgesPickerModal
  []
  (let [tr            (subscribe [::i18n-subs/tr])
        open?         (subscribe [::subs/modal-open? events/edges-picker-modal-id])
        close-fn      #(do (dispatch [::events/set-opened-modal nil]))
        add-to-select  (fn []
                         (dispatch [::events/get-selected-edge-ids]))]
    (fn []
      [ui/Modal {:size       :medium
                 :open        @open?
                 :close-icon true
                 :on-close   close-fn}
       [uix/ModalHeader {:header (@tr [:add-edges])}]
       [ui/ModalContent
        [EdgePickerContent]]
       [ui/ModalActions
        [uix/Button {:text     (@tr [:add-to-depl-group])
                     :positive true
                     :active   true
                     :on-click add-to-select}]]])))



(defn EdgeOverviewContent
  [edges-stats creating?]
  (let [tr (subscribe [::i18n-subs/tr])
        fleet-filter (subscribe [::subs/fleet-filter])
        fleet-changes (subscribe [::subs/fleet-changes])]
    [:<>
     (when (pos? (:total edges-stats))
       [:<>
        [StatisticStatesEdgeView edges-stats]
        (when @fleet-changes
          [:div {:style {:margin "1.4rem auto"}}
           [UnstoredEdgeChanges @fleet-changes]])
        [uix/Button {:class    "center"
                     :icon     icons/i-box
                     :content  "Show me"
                     :disabled (or (nil? (:total edges-stats))
                                 (= 0 (:total edges-stats)))
                     :on-click (create-nav-fn "edges" {:edges-state nil})}]])
     [:div
      {:style {:display :flex :justify-content :center :align-items :center :flex-direction :column}}
      (when-not (or
                  creating?
                  @fleet-filter)
        ;; TODO when implementing creation flow from apps page: Always show button and use temp-id for storing
        ;; and retrieving deployment-set and deployment-set-edited
        [:div
         [AddButton events/edges-picker-modal-id]])
      [:div {:style {:margin-top "1rem"}}
       (if (pos? (:total edges-stats))
         (@tr [:add-your-first-edge])
         (@tr [:add-an-edge]))]
      [EdgesPickerModal]
      [FleetFilterMessage]]]))

(defn TabOverview
  [uuid creating?]
  (dispatch [::events/get-deployments-for-deployment-sets uuid])
  (let [deployment-set (subscribe [::subs/deployment-set])
        edges-stats    (subscribe [::subs/edges-summary-stats])]
    (fn []
      (let [tr (subscribe [::i18n-subs/tr])]
        [ui/TabPane
         [ui/Grid {:columns   2
                   :stackable true
                   :padded    true}
          [ui/GridColumn {:stretched true}
           [TabOverviewDeploymentSet @deployment-set creating?]]
          [ui/GridColumn {:stretched true}
           [vc/TitledCard
            {:class :nuvla-apps
             :icon  icons/i-layer-group
             :label (str/capitalize (@tr [:apps]))}
            [AppsOverviewTable creating?]]]

          [ui/GridColumn {:stretched true}
           [vc/TitledCard
            {:class :nuvla-edges
             :icon  icons/i-box
             :label (str (@tr [:nuvlaedge]) "s")}
            [EdgeOverviewContent @edges-stats creating?]]]
          [ui/GridColumn {:stretched true}
           [DeploymentsStatesCard]]]]))))


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

(defn ModuleVersionsApp
  [i module-id]
  (let [tr (subscribe [::i18n-subs/tr])]
    [uix/Accordion
     [module-plugin/ModuleVersions
      {:db-path      [::spec/apps-sets i]
       :href         module-id
       :change-event [::events/edit-config]}]
     :label (@tr [:select-version])]))

(defn EnvVariablesApp
  [i module-id]
  (let [tr (subscribe [::i18n-subs/tr])]
    [uix/Accordion
     [module-plugin/EnvVariables
      {:db-path      [::spec/apps-sets i]
       :href         module-id
       :change-event [::events/edit-config]}]
     :label (@tr [:env-variables])]))


(defn- AppName [{:keys [idx id]}]
  (let [app    (subscribe [::module-plugin/module
                           [::spec/apps-sets idx] id])
        error? (subscribe [::subs/app-config-validation-error? idx id])]
    (fn []
      [:span {:style {:color (if @error? utils-forms/dark-red "black")}}
       (or (:name @app) (:id @app))])))


(defn ConfigureApps
  [i applications]
  ^{:key (str "set-" i)}
  [tab/Tab
   {:db-path                 [::apps-config]
    :ignore-chng-protection? true
    :panes                   (map
                               (fn [{id :href}]
                                 {:menuItem {:content (r/as-element
                                                        [AppName {:idx i :id id}])
                                             :icon    "cubes"
                                             :key     (create-app-config-query-key i id)}
                                  :render   #(r/as-element
                                               [ui/TabPane
                                                [ui/Popup {:trigger (r/as-element
                                                                      [:span
                                                                       [module-plugin/LinkToApp
                                                                        {:db-path  [::spec/apps-sets i]
                                                                         :href     id
                                                                         :children [:<>
                                                                                    [ui/Icon {:class icons/i-link}]
                                                                                    "Go to app"]}]])
                                                           :content "Open application in a new window"}]
                                                [ModuleVersionsApp i id]
                                                [EnvVariablesApp i id]])})
                               applications)}])

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
             [ui/TableCell [utils-values/AsLink (:path application)
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

(defn- EdgeTabStatesFilterView
  []
  (let [current-route (subscribe [::route-subs/current-route])
        edges-stats (subscribe [::subs/edges-summary-stats])]
    (fn [selected-state]
      [edges-views/StatisticStatesEdgeView
       (assoc @edges-stats
         :states (mapv (fn [state]
                         (let [label (:label state)]
                           (assoc state
                             :selected?
                             (or
                               (= label selected-state)
                               (and
                                 (= label "TOTAL")
                                 (empty? selected-state)))
                             :on-click
                             #(dispatch
                                [::routing-events/navigate
                                 (routes-utils/gen-href @current-route
                                   {:partial-query-params
                                    {events/edges-state-filter-key
                                     (if (= "TOTAL" label)
                                       nil
                                       label)}}) nil nil])))) edges-views/edges-states))
       true true])))

(defn- EdgeTabStatesFilter
  []
  (let [selected-state (subscribe [::route-subs/query-param events/edges-state-filter-key])]
    (fn []
      (dispatch [::events/get-edges])
      [EdgeTabStatesFilterView @selected-state])))

(defn EdgesTabView
  []
  (let [tr                (subscribe [::i18n-subs/tr])
        edges             (subscribe [::subs/edges-documents-response])
        fleet-changes     (subscribe [::subs/fleet-changes])
        only-changes?     (subscribe [::subs/show-only-changed-fleet?])
        columns          (mapv (fn [col-config]
                                 (assoc col-config :cell edges-views/NuvlaboxRow))
                           [{:field-key :online :header-content [icons/HeartbeatIcon] :cell-props {:collapsing true}}
                            {:field-key :state :cell-props {:collapsing true}}
                            {:field-key :name}
                            {:field-key :description}
                            {:field-key :created}
                            {:field-key :created-by}
                            {:field-key      :refresh-interval
                             :header-content (some-> (@tr [:report-interval]) str/lower-case)}
                            {:field-key :last-online :no-sort? true}
                            {:field-key :version :no-sort? true}
                            {:field-key :tags :no-sort? true}])
        filter-open?      (r/atom false)
        additional-filter (subscribe [::subs/edges-additional-filter])]
    (fn []
      [:div {:style {:padding-top "10px"}
             :class :nuvla-edges}
       [ui/Grid {:stackable true
                 :reversed "mobile"}
        [ui/GridColumn {:width 4}
         [:div
          [full-text-search-plugin/FullTextSearch
           {:db-path [::spec/edges-full-text-search]
            :change-event [::pagination-plugin/change-page [::spec/edges-pagination] 1]}]
          ^{:key @additional-filter}
          [:div {:style {:margin-top "0.4rem"}}
           [filter-comp/ButtonFilter
            {:resource-name                    edges-spec/resource-name
             :default-filter                   @additional-filter
             :open?                            filter-open?
             :on-done                          #(dispatch [::events/set-edges-additional-filter %])
             :show-clear-button-outside-modal? true
             :persist? false}]]]]
        [ui/GridColumn {:width 7}
         [:div {:class :nuvla-edges
                :style {:margin "0 auto 0 6rem"}}
          [EdgeTabStatesFilter]]]]
       (when @fleet-changes
         [:div {:style {:margin-top "1rem"
                        :margin-bottom "1rem"}}
          [:div [UnstoredEdgeChanges @fleet-changes]]
          [:div [ui/Checkbox {:checked @only-changes?
                              :basic true
                              :label "Show only unsaved changes"
                              :on-click #(dispatch [::events/show-fleet-changes-only @fleet-changes])}]]])
       [edges-views/NuvlaEdgeTableView
        {:edges   (mapv (fn [row]
                          (if
                            (some #{(:id row)} (:removed @fleet-changes))
                            (assoc row :table-row-prop {:style {:text-decoration "line-through"
                                                                :opacity 0.5}})
                            row))
                    (:resources @edges))
         :columns columns
         :sort-config {:db-path    ::spec/edges-ordering
                       :fetch-event [::events/get-edges]}
         :select-config {:bulk-actions [{:event (fn [select-data]
                                                  (dispatch [::events/remove-edges select-data]))
                                          :name "Remove edges"
                                          :icon icons/BoxIcon}]
                         :total-count-sub-key [::subs/edges-count]
                         :resources-sub-key   [::subs/edges-documents-response]
                         :select-db-path      [::spec/edges-select]}}]
       [pagination-plugin/Pagination
        {:db-path                [::spec/edges-pagination]
         :change-event           [::events/get-edges]
         :total-items            (-> @edges :count)
         :i-per-page-multipliers [1 2 4]}]
       [FleetFilterMessage]])))

(defn EdgesTab
  []
  (dispatch [::events/init-edges-tab])
  (fn []
    [EdgesTabView]))

(defn DeploymentsTab
  [uuid]
  (let [tr                @(subscribe [::i18n-subs/tr])
        depl-state-filter (subscribe [::route-subs/query-param events/deployments-state-filter-key])
        count             (subscribe [::deployments-subs/deployments-count])]
    (dispatch [::events/get-deployments-for-deployment-sets uuid])
    [:div {:class :nuvla-deployments}
     [DeploymentStatesFilter @depl-state-filter]
     [dv/DeploymentTable
      {:no-actions              true
       :empty-msg               (tr [:empty-deployment-module-msg])
       :pagination-db-path      ::spec/pagination-deployments
       :pagination              (fn []
                                  [pagination-plugin/Pagination
                                   {:db-path                [::spec/pagination-deployments]
                                    :total-items            @count
                                    :change-event           [::events/get-deployments-for-deployment-sets uuid]
                                    :i-per-page-multipliers [1 2 4]}])
       :fetch-event             [::events/get-deployments-for-deployment-sets uuid]
       ;; FIXME: Make this more generic by passing all columns to show/hide
       :hide-depl-group-column? true}]]))

(defn TabsDeploymentSet
  [{:keys [uuid creating?]}]
  (let [tr             @(subscribe [::i18n-subs/tr])
        deployment-set (subscribe [::subs/deployment-set])
        apps-sets      (subscribe [::subs/applications-sets])
        apps           (subscribe [::subs/apps-row-data])
        edges          (subscribe [::subs/all-edges-ids])
        depl-all       (subscribe [::deployments-subs/deployments-summary-all])]
    (fn []
      (when (or @deployment-set creating?)
        [tab/Tab
         {:db-path                 [::spec/tab]
          :panes                   [{:menuItem {:content (str/capitalize (tr [:overview]))
                                                :key     :overview
                                                :icon    icons/i-eye}
                                     :render   #(r/as-element [TabOverview uuid creating?])}
                                    {:menuItem {:key      :apps
                                                :content
                                                (let [tab-title (tr [:apps-config])]
                                                  (if creating?
                                                    (r/as-element
                                                      [ui/Popup {:trigger (r/as-element
                                                                            [:span
                                                                             tab-title])
                                                                 :content (tr [:save-before-configuring-apps])}])
                                                    (let [error? @(subscribe [::subs/apps-config-validation-error?])]
                                                      (r/as-element
                                                        [:span
                                                         {:style {:color (if error? utils-forms/dark-red "black")}}
                                                         tab-title]))))
                                                :icon     icons/i-gear
                                                :disabled (empty? @apps-sets)}
                                     :render   #(r/as-element
                                                  [ConfigureApps
                                                   0
                                                   (remove (comp (partial = :removed) :edit-status) @apps)])}
                                    {:menuItem {:key      :edges
                                                :content
                                                (let [tab-title (str/capitalize (tr [:edges]))]
                                                  (if creating?
                                                    (r/as-element
                                                      [ui/Popup {:trigger (r/as-element [:span tab-title])
                                                                 :content (tr [:depl-group-add-one-edge-to-enable-tab])}])
                                                    tab-title))
                                                :icon     icons/i-box
                                                :disabled (empty? @edges)}
                                     :render   #(r/as-element
                                                  [EdgesTab])}
                                    {:menuItem {:key      :deployments
                                                :content
                                                (let [tab-title (str/capitalize (str/capitalize (tr [:deployments])))]
                                                  (if creating?
                                                    (r/as-element
                                                      [ui/Popup {:trigger (r/as-element
                                                                            [:span
                                                                             tab-title])
                                                                 :content (tr [:depl-group-save-and-start-to-enable-tab])}])
                                                    tab-title))
                                                :icon     icons/i-rocket
                                                :disabled (zero? (:total @depl-all))}
                                     :render   #(r/as-element
                                                  [DeploymentsTab uuid])}]
          :ignore-chng-protection? true
          :menu                    {:secondary true
                                    :pointing  true}}]))))

(defn- DeploymentSetView
  [uuid]
  (dispatch [::events/init uuid])
  (let [depl-set (subscribe [::subs/deployment-set])]
    (fn []
      (let [{:keys [id name]} @depl-set]
        [components/LoadingPage {:dimmable? true}
         [:<>
          [components/NotFoundPortal
           ::subs/deployment-set-not-found?
           :no-deployment-set-message-header
           :no-deployment-set-message-content]
          [ui/Container {:fluid true}
           [uix/PageHeader "bullseye" (or name id) :color (ops-status->color
                                                            (-> @depl-set
                                                                :operational-status
                                                                :status))]
           [utils-validation/validation-error-message ::subs/form-valid?]
           [MenuBar uuid]
           [bulk-progress-plugin/MonitoredJobs
            {:db-path [::spec/bulk-jobs]}]
           [components/ErrorJobsMessage
            ::job-subs/jobs nil nil
            #(dispatch [::tab/change-tab {:db-path [::spec/tab]
                                          :tab-key :jobs}])]
           [TabsDeploymentSet {:uuid uuid}]]]]))))

(defn DeploymentSetCreate
  []
  (dispatch [::events/init-create])
  (let [tr            (subscribe [::i18n-subs/tr])
        name          (subscribe [::route-subs/query-param :name])
        depl-set-name (subscribe [::subs/deployment-set-name])]
    (fn []
      [:<>
       [components/NotFoundPortal
        ::subs/deployment-set-not-found?
        :no-deployment-set-message-header
        :no-deployment-set-message-content]
       [ui/Container {:fluid true}
        [uix/PageHeader "bullseye" (or @depl-set-name @name (@tr [:set-a-name]))]
        [MenuBarCreate]
        [TabsDeploymentSet {:creating? true}]]])))


(defn Details
  [uuid]
  (case (str/lower-case uuid)
    "new"
    [AddPage]

    "create"
    [DeploymentSetCreate]

    [DeploymentSetView uuid]))
