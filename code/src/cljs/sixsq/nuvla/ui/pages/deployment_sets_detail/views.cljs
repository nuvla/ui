(ns sixsq.nuvla.ui.pages.deployment-sets-detail.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.filter-comp.views :as filter-comp]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.job.subs :as job-subs]
            [sixsq.nuvla.ui.common-components.job.views :as job-views]
            [sixsq.nuvla.ui.common-components.plugins.bulk-progress :as bulk-progress-plugin]
            [sixsq.nuvla.ui.common-components.plugins.duration-picker :as duration-picker]
            [sixsq.nuvla.ui.common-components.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.common-components.plugins.module :as module-plugin]
            [sixsq.nuvla.ui.common-components.plugins.nav-tab :as tab]
            [sixsq.nuvla.ui.common-components.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.common-components.plugins.step-group :as step-group]
            [sixsq.nuvla.ui.common-components.plugins.table :as table-plugin :refer [Table]]
            [sixsq.nuvla.ui.common-components.plugins.target-selector :as target-selector]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.pages.apps.apps-store.spec :as apps-store-spec]
            [sixsq.nuvla.ui.pages.apps.apps-store.subs :as apps-store-subs]
            [sixsq.nuvla.ui.pages.apps.apps-store.views :as apps-store-views]
            [sixsq.nuvla.ui.pages.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.pages.apps.views-detail :refer [AuthorVendorForModule]]
            [sixsq.nuvla.ui.pages.dashboard.views :as dashboard-views]
            [sixsq.nuvla.ui.pages.deployment-sets-detail.events :as events]
            [sixsq.nuvla.ui.pages.deployment-sets-detail.spec :as spec]
            [sixsq.nuvla.ui.pages.deployment-sets-detail.subs :as subs]
            [sixsq.nuvla.ui.pages.deployment-sets-detail.utils :as utils]
            [sixsq.nuvla.ui.pages.deployments.subs :as deployments-subs]
            [sixsq.nuvla.ui.pages.deployments.utils :as deployment-utils]
            [sixsq.nuvla.ui.pages.deployments.views :as dv]
            [sixsq.nuvla.ui.pages.edges.spec :as edges-spec]
            [sixsq.nuvla.ui.pages.edges.utils :as edges-utils]
            [sixsq.nuvla.ui.pages.edges.views :as edges-views]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.subs :as route-subs]
            [sixsq.nuvla.ui.routing.utils :as routes-utils :refer [name->href pathify]]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.forms :as utils-forms]
            [sixsq.nuvla.ui.utils.general :as general-utils :refer [format-money]]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.tooltip :as tt]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
            [sixsq.nuvla.ui.utils.validation :as utils-validation]
            [sixsq.nuvla.ui.utils.values :as values]
            [sixsq.nuvla.ui.utils.values :as utils-values]
            [sixsq.nuvla.ui.utils.view-components :as vc]))

(defn JobDetailParse
  [job]
  (let [parsed-job (bulk-progress-plugin/append-parsed-job-status-message job)]
    (if (or (get-in parsed-job [:parsed-status-message :skipped_count])
            (bulk-progress-plugin/job-queued? job)
            (bulk-progress-plugin/job-running? job))
      [bulk-progress-plugin/JobDetail parsed-job]
      [job-views/DefaultJobCell job])))

(defmethod job-views/JobCell "bulk_deployment_set_start"
  [resource]
  [JobDetailParse resource])

(defmethod job-views/JobCell "bulk_deployment_set_update"
  [resource]
  [JobDetailParse resource])

(defmethod job-views/JobCell "bulk_deployment_set_stop"
  [resource]
  [JobDetailParse resource])


(defn- ops-status-start-str
  [tr-fn {:keys [deployments-to-add deployments-to-update] :as _ops-status}
   apps-count edges-count]
  (tr-fn [:depl-group-start-warning-msg]
         [(+ (count deployments-to-add) (count deployments-to-update))
          (str apps-count " " (str/lower-case (tr-fn (if (> apps-count 1) [:apps] [:app]))))
          (str edges-count " " (str/lower-case (tr-fn (if (> edges-count 1) [:edges] [:edge]))))]))

(defn- ops-status-pending-str [tr-fn {:keys [deployments-to-add
                                             deployments-to-update
                                             deployments-to-remove] :as _ops-status}]
  (str/join ", "
            (remove nil?
                    [(some-> deployments-to-add count
                             (str " " (tr-fn [:deployments-to-add])))
                     (some-> deployments-to-update count
                             (str " " (tr-fn [:deployments-to-update])))
                     (some-> deployments-to-remove count
                             (str " " (tr-fn [:deployments-to-remove])))])))

(defn- create-stop-wrng-msg
  [tr-fn {:keys [aggregations] :as _deployments-stats}]
  (let [count-by-state (->> aggregations :terms:state :buckets
                            (group-by :key)
                            (reduce-kv (fn [m k v] (assoc m k (:doc_count (first v)))) {}))]
    (tr-fn [:depl-group-stop-warning-msg] [(+ (or (count-by-state deployment-utils/STARTING) 0)
                                              (or (count-by-state deployment-utils/STARTED) 0)
                                              (or (count-by-state deployment-utils/ERROR) 0)
                                              (or (count-by-state deployment-utils/UPDATING) 0)
                                              (or (count-by-state deployment-utils/PENDING) 0))])))

(defn- ops-status-delete-str [tr-fn {:keys [deployments-to-add deployments-to-remove] :as _ops-status}
                              apps-count edges-count]
  (let [n-deployments-to-delete (-> (* apps-count edges-count)
                                    (- deployments-to-add)
                                    (+ deployments-to-remove))]
    (if (pos? n-deployments-to-delete)
      (tr-fn [:depl-group-delete-warning-msg] [n-deployments-to-delete])
      (tr-fn [:dep-group-delete-no-current-deployments]))))

(defn edit-not-allowed-msg
  [{:keys [TR can-edit-data? edit-op-allowed? edit-not-allowed-in-state? is-controlled-by-apps-set?]}]
  (when (and can-edit-data? (or (not edit-op-allowed?) is-controlled-by-apps-set?))
    (TR (cond
          is-controlled-by-apps-set? [:dep-group-app-version-changes-not-allowed]
          edit-not-allowed-in-state? [:dep-group-edit-not-allowed-in-state]
          :else [:dep-group-edit-not-allowed]))))

(defn- depl-set->modal-content
  [{:keys [name id description]}]
  (str (or name id) (when description " - ") description))

(defn guarded-menu-item
  [{:keys [validation enabled on-click] :or {enabled true}} & children]
  (let [{:keys [valid? errors]} validation
        tr        (subscribe [::i18n-subs/tr])
        enabled?  (and valid? enabled)
        menu-item (into [ui/MenuItem
                         {:as       :a
                          :on-click on-click
                          :disabled (not enabled?)
                          :class    (when enabled? "primary-menu-item")}]
                        children)]
    (if valid?
      (r/as-element menu-item)
      (r/as-element
        [ui/Popup
         {:trigger (r/as-element menu-item)
          :content (str/join ". " (map (comp @tr :message) errors))}]))))

(def start-modal-id :modal/start-deployment-group)

(defn close-modal [] (dispatch [::events/set-opened-modal nil]))

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
        edges-count              @(subscribe [::subs/edges-count])
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
          (for [{:keys [i total-price application]} apps-targets-total-price]
            ^{:key (str "price-" i "-" (:id application))}
            [ui/TableRow
             [ui/TableCell [utils-values/AsLink (:path application)
                            :label (or (:name application)
                                       (:id application)) :page "apps"]]
             [ui/TableCell {:text-align "right"} (general-utils/format-money
                                                   (/ (get-in application [:price :cent-amount-daily]) 100))]
             [ui/TableCell {:text-align "right"} edges-count]
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

(defn RequirementsMessage
  [{{:keys [architectures]}            :minimum-requirements
    {:keys [n-edges first-mismatches]} :unmet-requirements
    :as                                _requirements}]
  (let [tr                          @(subscribe [::i18n-subs/tr])
        unmet-requirements-accepted (subscribe [::subs/unmet-requirements-accepted])
        min-requirements-met?       (zero? n-edges)
        no-arch-supported?          (and (some? architectures) (empty? architectures))]
    [ui/Segment {:attached true}
     [ui/Message {:size     "tiny"
                  :info     (and min-requirements-met? (not no-arch-supported?))
                  :warning  (and (not min-requirements-met?) (not no-arch-supported?))
                  :negative no-arch-supported?}
      [:div {:style {:display :flex, :flex-direction :row}}
       (if no-arch-supported?
         (tr [:no-architecture-supported])
         [:<>
          (if min-requirements-met? [icons/InfoIcon] [icons/WarningIcon])
          (if min-requirements-met?
            (tr [:edge-meets-app-minimum-requirements])
            [:div
             (if (> n-edges (count first-mismatches))
               (tr [:showing-first-mismatches-only] [n-edges (count first-mismatches)])
               (tr [:edges-do-not-meet-minimum-requirements] [n-edges]))
             (doall
               (for [{:keys [edge-id edge-name architecture cpu ram disk]} first-mismatches]
                 ^{:key edge-id}
                 [:div (values/AsPageLink edge-id :label edge-name)
                  [:ul {:style {:margin 0, :padding-left "20px"}}
                   (when architecture [:li (tr [:edge-architecture-not-supported] [(str/join ", " (:supported architecture))
                                                                                   (:edge-architecture architecture)])])
                   (when cpu [:li (tr [:edge-does-not-meet-min-cpu-requirements] [(:min cpu) (:available cpu)])])
                   (when ram [:li (tr [:edge-does-not-meet-min-ram-requirements] [(:min ram) (:available ram)])])
                   (when disk [:li (tr [:edge-does-not-meet-min-disk-requirements] [(:min disk) (:available disk)])])]]))
             [ui/Checkbox {:style     {:margin-top "10px", :font-weight "normal"}
                           :label     (tr [:deploy-anyway])
                           :checked   @unmet-requirements-accepted
                           :on-change (ui-callback/checked #(dispatch [::events/accept-unmet-requirements %]))}]])])]]]))

(defn Requirements
  []
  (let [tr           (subscribe [::i18n-subs/tr])
        requirements (subscribe [::subs/requirements])]
    [:div
     [ui/Header {:as :h5 :attached "top"}
      (@tr [:requirements])]
     [RequirementsMessage @requirements]]))

(defn StartButton
  [_deployment-set _warn-msg]
  (let [tr                    (subscribe [::i18n-subs/tr])
        enabled?              (subscribe [::subs/operation-enabled? "start"])
        validation            (subscribe [::subs/deployment-set-validation])
        open?                 (subscribe [::subs/modal-open? start-modal-id])
        eula-prices-accepted? (subscribe [::subs/eula-prices-accepted?])
        requirements-met?     (subscribe [::subs/requirements-met?])
        confirmed?            (r/atom false)
        close-start-modal     (fn []
                                (close-modal)
                                (dispatch [::events/set ::spec/licenses-accepted? false])
                                (dispatch [::events/set ::spec/prices-accepted? false])
                                (dispatch [::events/set-requirements nil])
                                (reset! confirmed? false))]
    (fn [{:keys [id] :as deployment-set} warn-msg]
      [uix/ModalDanger
       {:on-confirm         #(do (dispatch [::events/operation
                                            {:resource-id id
                                             :operation   "start"}])
                                 (close-start-modal))
        :trigger            (guarded-menu-item
                              {:enabled    @enabled?
                               :on-click   (fn []
                                             (dispatch [::events/check-requirements])
                                             (dispatch [::events/set-opened-modal start-modal-id]))
                               :validation @validation}
                              [icons/PlayIcon]
                              (str/capitalize (@tr [:start])))
        :open               @open?
        :on-close           close-start-modal
        :content            [:div
                             [:h3 (depl-set->modal-content deployment-set)]
                             [EulaPrices]
                             [Requirements]
                             [ui/Message {:error true}
                              [ui/MessageHeader {:style {:margin-bottom 10}}
                               (@tr [:are-you-sure-you-want-to-continue?])]
                              [ui/MessageContent [ui/Checkbox {:label     warn-msg
                                                               :checked   @confirmed?
                                                               :fitted    true
                                                               :on-change (ui-callback/checked (partial reset! confirmed?))}]]]]
        :control-confirmed? confirmed?
        :all-confirmed?     (and @eula-prices-accepted? @requirements-met? @confirmed?)
        :header             (@tr [:start-deployment-set])
        :button-text        (@tr [:start])
        :with-confirm-step? true}])))

(def stop-modal-id :modal/stop-deployment-group)

(defn StopButton
  [{:keys [id] :as deployment-set} warn-msg]
  (let [tr       (subscribe [::i18n-subs/tr])
        enabled? (subscribe [::subs/operation-enabled? "stop"])
        open?    (subscribe [::subs/modal-open? stop-modal-id])]
    [uix/ModalDanger
     {:on-confirm         #(dispatch [::events/operation
                                      {:resource-id id
                                       :operation   "stop"}])
      :trigger            (r/as-element
                            [ui/MenuItem
                             {:disabled (not @enabled?)
                              :on-click (fn [] (dispatch [::events/set-opened-modal stop-modal-id]))}
                             [icons/StopIcon]
                             (str/capitalize (@tr [:stop]))])
      :open               @open?
      :on-close           close-modal
      :content            [:h3 (depl-set->modal-content deployment-set)]
      :header             (@tr [:stop-deployment-set])
      :danger-msg-header  (@tr [:are-you-sure-you-want-to-continue?])
      :danger-msg         warn-msg
      :button-text        (@tr [:stop])
      :with-confirm-step? true}]))

(def update-modal-id :modal/update-deployment-group)

(defn UpdateButton
  [_deployment-set _warn-msg]
  (let [tr                    (subscribe [::i18n-subs/tr])
        enabled?              (subscribe [::subs/operation-enabled? "update"])
        validation            (subscribe [::subs/deployment-set-validation])
        open?                 (subscribe [::subs/modal-open? update-modal-id])
        eula-prices-accepted? (subscribe [::subs/eula-prices-accepted?])
        requirements-met?     (subscribe [::subs/requirements-met?])
        confirmed?            (r/atom false)
        close-update-modal    (fn []
                                (close-modal)
                                (dispatch [::events/set ::spec/licenses-accepted? false])
                                (dispatch [::events/set ::spec/prices-accepted? false])
                                (dispatch [::events/set-requirements nil])
                                (reset! confirmed? false))]
    (fn [{:keys [id] :as deployment-set} warn-msg]
      [uix/ModalDanger
       {:on-confirm         #(do (dispatch [::events/operation
                                            {:resource-id id
                                             :operation   "update"}])
                                 (close-update-modal))
        :trigger            (guarded-menu-item
                              {:enabled    @enabled?
                               :on-click   (fn []
                                             (dispatch [::events/check-requirements])
                                             (dispatch [::events/set-opened-modal update-modal-id]))
                               :validation @validation}
                              [icons/RedoIcon]
                              (str/capitalize (@tr [:update])))
        :open               @open?
        :on-close           close-update-modal
        :content            [:div
                             [:h3 (depl-set->modal-content deployment-set)]
                             [EulaPrices]
                             [Requirements]
                             [ui/Message {:error true}
                              [ui/MessageHeader {:style {:margin-bottom 10}}
                               (@tr [:are-you-sure-you-want-to-continue?])]
                              [ui/MessageContent [ui/Checkbox {:label     warn-msg
                                                               :checked   @confirmed?
                                                               :fitted    true
                                                               :on-change (ui-callback/checked (partial reset! confirmed?))}]]]]
        :control-confirmed? confirmed?
        :all-confirmed?     (and @eula-prices-accepted? @requirements-met? @confirmed?)
        :header             (@tr [:update-deployment-set])
        :button-text        (@tr [:update])
        :modal-action       [:p warn-msg]
        :with-confirm-step? true}])))

(def cancel-modal-id :modal/cancel-deployment-group)

(defn CancelOperationButton
  [{:keys [id] :as deployment-set}]
  (let [tr       (subscribe [::i18n-subs/tr])
        enabled? (subscribe [::subs/operation-enabled? "cancel"])
        open?    (subscribe [::subs/modal-open? cancel-modal-id])]
    [uix/ModalDanger
     {:on-confirm         #(dispatch [::events/operation
                                      {:resource-id id
                                       :operation   "cancel"}])
      :trigger            (r/as-element
                            [ui/MenuItem
                             {:disabled (not @enabled?)
                              :on-click (fn [] (dispatch [::events/set-opened-modal cancel-modal-id]))}
                             [icons/BanIcon]
                             (str/capitalize (@tr [:cancel]))])
      :open               @open?
      :on-close           close-modal
      :content            [:h3 (depl-set->modal-content deployment-set)]
      :header             (@tr [:cancel-deployment-set-operation])
      :danger-msg-header  (@tr [:are-you-sure-you-want-to-continue?])
      :danger-msg         (@tr [:cancel-deployment-set-operation-warn-msg])
      :button-text        (@tr [:cancel])
      :with-confirm-step? true}]))

(def recompute-fleet-modal-id :modal/recompute-fleet)

(defn RecomputeFleetMenuItem
  [deployment-set]
  (let [tr                   (subscribe [::i18n-subs/tr])
        can-recompute-fleet? (general-utils/can-operation? "recompute-fleet" deployment-set)
        unsaved-changes?     (subscribe [::subs/unsaved-changes?])
        open?                (subscribe [::subs/modal-open? recompute-fleet-modal-id])
        confirm-fn           (fn []
                               (dispatch [::events/recompute-fleet close-modal]))]
    [uix/ModalDanger
     {:on-confirm  confirm-fn
      :trigger     (r/as-element [ui/MenuItem
                                  {:disabled (or @unsaved-changes? (not can-recompute-fleet?))
                                   :on-click (fn [] (dispatch [::events/set-opened-modal recompute-fleet-modal-id]))}
                                  [icons/ArrowRotateIcon]
                                  (str/capitalize (@tr [:recompute-fleet]))])
      :open        @open?
      :on-close    close-modal
      :header      (@tr [:recompute-deployment-set-fleet])
      :danger-msg  (@tr [:recompute-fleet-warning])
      :button-text (@tr [:recompute-fleet])}]))

(defn RecomputeFleetButton
  [deployment-set]
  (let [tr                   (subscribe [::i18n-subs/tr])
        can-recompute-fleet? (general-utils/can-operation? "recompute-fleet" deployment-set)
        unsaved-changes?     (subscribe [::subs/unsaved-changes?])
        open?                (subscribe [::subs/modal-open? recompute-fleet-modal-id])
        confirm-fn           (fn []
                               (dispatch [::events/recompute-fleet close-modal]))]
    (when (and can-recompute-fleet? (not @unsaved-changes?))
      [uix/ModalDanger
       {:on-confirm  confirm-fn
        :trigger     (r/as-element [uix/Button
                                    {:disabled (or @unsaved-changes? (not can-recompute-fleet?))
                                     :on-click (fn [] (dispatch [::events/set-opened-modal recompute-fleet-modal-id]))
                                     :icon     icons/i-arrow-rotate
                                     :style    {:margin     "5px"
                                                :align-self "center"}
                                     :size     :tiny
                                     :content  (str/capitalize (@tr [:recompute-fleet]))}])
        :open        @open?
        :on-close    close-modal
        :header      (@tr [:recompute-deployment-set-fleet])
        :danger-msg  (@tr [:recompute-fleet-warning])
        :button-text (@tr [:recompute-fleet])}])))

(defn RecomputeFleetLink
  [deployment-set]
  (let [tr                   (subscribe [::i18n-subs/tr])
        can-recompute-fleet? (general-utils/can-operation? "recompute-fleet" deployment-set)
        unsaved-changes?     (subscribe [::subs/unsaved-changes?])]
    (when (and can-recompute-fleet? (not @unsaved-changes?))
      [:a {:href     "#"
           :style    {:align-self :center}
           :on-click (fn [] (dispatch [::events/set-opened-modal recompute-fleet-modal-id]))}
       [:span {:style {:margin-left "0.5rem"}}
        [icons/ArrowRotateIcon]]
       (@tr [:recompute-fleet])])))

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
           :danger-msg  (str (@tr [:warn-force-delete-orphan-containers])
                             " " warn-msg)}
          {:header      (@tr [:delete-deployment-set])
           :button-text (str/capitalize (@tr [:delete]))
           :danger-msg  warn-msg})]
    [uix/ModalDanger
     {:on-confirm         #(dispatch [::events/delete {:deletable? deletable?
                                                       :forceable? forceable?}])
      :trigger            (r/as-element [ui/MenuItem
                                         {:data-testid "deployment-group-delete-button"
                                          :disabled    (and
                                                         (not forceable?)
                                                         (not deletable?))}
                                         [icons/TrashIconFull]
                                         button-text])
      :content            [:h3 content]
      :header             header
      :danger-msg         danger-msg
      :button-text        button-text
      :with-confirm-step? true}]))

(defn BasicSaveButton
  [{:keys [creating?]}]
  (let [tr            (subscribe [::i18n-subs/tr])
        save-enabled? (subscribe [::subs/save-enabled? creating?])]
    (fn [{:keys [on-click]}]
      [uix/MenuItem
       (cond->
         {:name     (@tr [:save])
          :icon     icons/i-floppy
          :disabled (not @save-enabled?)
          :class    (when @save-enabled? "primary-menu-item")}
         on-click (assoc :on-click on-click))])))

(defn SaveButton
  [{:keys [creating?] :as opts}]
  (let [tr                         (subscribe [::i18n-subs/tr])
        validation                 (subscribe [::subs/deployment-set-validation])
        is-controlled-by-apps-set? (subscribe [::subs/is-controlled-by-apps-set?])
        server-side-changes        (subscribe [::subs/server-side-changes])]
    (fn [{:keys [deployment-set]}]
      (let [save-fn (if (:valid? @validation)
                      #(dispatch [::events/do-edit {:deployment-set deployment-set
                                                    :success-msg    (@tr [:updated-successfully])}])
                      #(dispatch [::events/enable-form-validation]))]
        (if @server-side-changes
          [uix/ModalDanger
           {:on-confirm         save-fn
            :trigger            (r/as-element [:span [BasicSaveButton opts]])
            :content            [:h3 "Resource was changed server side"]
            :header             "Server side changes detected"
            :danger-msg         (str "If you save the current view the following server side changes will be overridden:\n\n"
                                     (utils/pprint-server-side-changes-str @server-side-changes))
            :button-text        "Save"
            :with-confirm-step? false}]
          [BasicSaveButton
           (assoc opts
             :on-click (if creating?
                         #(dispatch [::events/create @is-controlled-by-apps-set?])
                         save-fn))])))))

(def missing-edges-modal-id :modal/missing-edges)

(defn RemoveMissingEdgesLink
  []
  (let [tr               (subscribe [::i18n-subs/tr])
        creating?        false
        can-edit-data?   (subscribe [::subs/can-edit-data? creating?])
        edit-op-allowed? (subscribe [::subs/edit-op-allowed? creating?])
        unsaved-changes? (subscribe [::subs/unsaved-changes?])
        open?            (subscribe [::subs/modal-open? missing-edges-modal-id])
        confirm-fn       #(dispatch [::events/remove-missing-edges (@tr [:updated-successfully]) close-modal])]
    (when (and @can-edit-data? @edit-op-allowed? (not @unsaved-changes?))
      [uix/ModalDanger
       {:on-confirm  confirm-fn
        :trigger     (r/as-element [:a {:href     "#"
                                        :style    {:align-self :center}
                                        :on-click (fn [] (dispatch [::events/set-opened-modal missing-edges-modal-id]))}
                                    (@tr [:remove-missing-edges])])
        :open        @open?
        :on-close    close-modal
        :header      (@tr [:remove-missing-edges])
        :danger-msg  (@tr [:remove-missing-edges-warning])
        :button-text (@tr [:remove-missing-edges])}])))

(defn MissingEdgesPanel
  [deployment-set missing-edges]
  (when (seq missing-edges)
    (let [tr           (subscribe [::i18n-subs/tr])
          n            (count missing-edges)
          fleet-filter (subscribe [::subs/fleet-filter])]
      [:div.missing-edges-panel
       [icons/TriangleExclamationIcon {:color :orange}]
       (str
         n " " (@tr (if (> n 1) [:edges] [:edge]))
         " " (@tr (if (> n 1) [:are-missing] [:is-missing])) ". ")
       (if @fleet-filter
         [RecomputeFleetLink deployment-set]
         [RemoveMissingEdgesLink])])))

(defn MenuBar
  [_creating?]
  (let [deployment-set    (subscribe [::subs/deployment-set])
        op-status         (subscribe [::subs/operational-status])
        loading?          (subscribe [::subs/loading?])
        apps-count        (subscribe [::subs/apps-count])
        edges-count       (subscribe [::subs/edges-count])
        fleet-filter      (subscribe [::subs/fleet-filter])
        deployments-stats (subscribe [::deployments-subs/deployments-summary-all])
        tr                (subscribe [::i18n-subs/tr])]
    (fn [creating?]
      [components/StickyBar
       [components/ResponsiveMenuBar
        [^{:key "save"}
         [SaveButton {:creating?      creating?
                      :deployment-set @deployment-set}]
         ^{:key "start"}
         [StartButton @deployment-set (ops-status-start-str @tr @op-status @apps-count @edges-count)]
         ^{:key "update"}
         [UpdateButton @deployment-set
          (str
            (@tr [:about-starting-these-updates]) ": "
            (ops-status-pending-str @tr @op-status) ". "
            (@tr [:proceed?]))]
         ^{:key "stop"}
         [StopButton @deployment-set (create-stop-wrng-msg @tr @deployments-stats)]
         ^{:key "cancel"}
         [CancelOperationButton @deployment-set]
         (when @fleet-filter
           ^{:key "recompute-fleet"}
           [RecomputeFleetMenuItem @deployment-set])
         ^{:key "delete"}
         [DeleteButton @deployment-set (ops-status-delete-str @tr @op-status @apps-count @edges-count)]]
        [components/RefreshMenu
         {:action-id  events/refresh-action-depl-set-id
          :loading?   @loading?
          :on-refresh #(events/refresh)}]
        {:max-items-to-show 4}]])))

(defn EditableCell
  [attribute creating?]
  (let [deployment-set   (subscribe [::subs/deployment-set])
        can-edit-data?   (subscribe [::subs/can-edit-data? creating?])
        edit-op-allowed? (subscribe [::subs/edit-op-allowed? creating?])
        on-change-fn     #(dispatch [::events/edit attribute %])]
    [ui/TableCell
     (if (and @can-edit-data? (or creating? @edit-op-allowed?))
       [ui/Input
        {:default-value (get @deployment-set attribute)
         :on-change     (ui-callback/input-callback on-change-fn)
         :style         {:width "100%"}}]
       (get @deployment-set attribute))]))

(defn DGTypeChangeModalDanger
  [{:keys [on-confirm]}]
  (r/with-let [tr    (subscribe [::i18n-subs/tr])
               open? (subscribe [::subs/dg-type-change-modal-danger-open?])]
    [uix/ModalDanger
     {:header      (@tr [:dg-type-change-modal-danger-header])
      :content     (@tr [:dg-type-change-modal-danger-content])
      :open        @open?
      :on-close    #(dispatch [::events/close-dg-type-change-modal-danger])
      :button-text (@tr [:dg-type-change-modal-danger-confirm])
      :on-confirm  (fn []
                     (dispatch [::events/close-dg-type-change-modal-danger])
                     (on-confirm))}]))

(defn DGTypeCell
  [creating?]
  (let [deployment-set      (subscribe [::subs/deployment-set])
        apps                (subscribe [::subs/apps-creation])
        edges               (subscribe [::subs/edges-documents])
        fleet-filter        (subscribe [::subs/fleet-filter])
        new-subtype         (r/atom nil)
        on-chg-confirmed-fn (fn []
                              (dispatch [::events/edit :subtype @new-subtype])
                              (dispatch [::events/clear-apps])
                              (if @fleet-filter
                                (when creating?
                                  (dispatch [::events/update-fleet-filter-edge-ids
                                             (assoc @deployment-set :subtype @new-subtype) creating?]))
                                (dispatch [::events/clear-edges])))
        on-change-fn        (fn [subtype]
                              (reset! new-subtype subtype)
                              (if (and (empty? @apps) (or @fleet-filter (empty? @edges)))
                                (on-chg-confirmed-fn)
                                (dispatch [::events/open-dg-type-change-modal-danger])))
        opts                [[spec/subtype-docker-compose "Docker"]
                             [spec/subtype-docker-swarm "Docker Clustered"]
                             [spec/subtype-kubernetes "Kubernetes"]]]
    [ui/TableCell
     (if creating?
       [:<>
        [ui/Dropdown {:value     (get @deployment-set :subtype)
                      :options   (map (fn [[k t]] {:key k, :value k, :text t}) opts)
                      :selection true
                      :on-change (ui-callback/value on-change-fn)}]
        [DGTypeChangeModalDanger {:on-confirm on-chg-confirmed-fn}]]
       (->> (get @deployment-set :subtype)
            (get (into {} opts))))]))

(def ops-status->color
  {"OK"  "green"
   "NOK" "red"})

(defn OperationalStatusSummary
  [deployment-set {:keys [status missing-edges] :as ops-status}]
  (let [tr (subscribe [::i18n-subs/tr])]
    (if (= status "OK")
      [:div
       [ui/Icon {:name :circle :color (ops-status->color status)}]
       (@tr [:everything-is-up-to-date])
       [MissingEdgesPanel deployment-set missing-edges]]
      [:div
       [ui/Icon {:name :circle :color (ops-status->color status)}]
       (str (@tr [:divergence]) ": "
            (ops-status-pending-str @tr ops-status))
       [MissingEdgesPanel deployment-set missing-edges]])))

(defn AutoUpdateControl
  [{:keys [auto-update auto-update-interval] :as _deployment-set} creating?]
  (r/with-let [tr                               (subscribe [::i18n-subs/tr])
               !auto-update-interval-in-seconds (r/atom nil)
               !minutes-options                 (r/atom nil)
               can-edit-data?                   (subscribe [::subs/can-edit-data? creating?])
               edit-op-allowed?                 (subscribe [::subs/edit-op-allowed? creating?])
               !disabled?                       (r/atom false)]
    (reset! !auto-update-interval-in-seconds (if (pos? auto-update-interval) (* auto-update-interval 60) 300))
    (reset! !minutes-options (into (if (>= @!auto-update-interval-in-seconds 3600) [0] [])
                                   [1 2 5 10 20 30 40 50]))
    (let [enabled? (and @can-edit-data? (or creating? @edit-op-allowed?))]
      (reset! !disabled? (or (not enabled?) (not auto-update)))
      [:div {:style {:display     :flex
                     :align-items :center}}
       [ui/Checkbox (cond-> {:checked  (boolean auto-update)
                             :basic    "true"
                             :label    ""
                             :disabled (not enabled?)}
                            enabled? (assoc :on-click #(dispatch [::events/set-auto-update (not auto-update)])))]
       [:div {:style {:display     :flex
                      :width       "90%"
                      :align-items :center
                      :gap         10}}
        [:span (str/capitalize (@tr [:interval])) ":"]
        [:span {:style {:width "80%"}}
         [duration-picker/DurationPickerController
          {:!value           !auto-update-interval-in-seconds
           :set-value-fn     #(dispatch [::events/set-auto-update-interval %])
           :!show-days?      (r/atom false)
           :!show-seconds?   (r/atom false)
           :!hours-options   (r/atom (range 0 24))
           :!minutes-options !minutes-options
           :!disabled?       !disabled?}]]]])))

(defn TabOverviewDeploymentSet
  [{:keys [id created updated created-by state operational-status] :as deployment-set} creating?]
  (r/with-let [tr (subscribe [::i18n-subs/tr])]
    [ui/Segment {:secondary true
                 :color     "blue"}
     [:h4 (if creating?
            (str (@tr [:creating-new]) (str/lower-case (@tr [:deployment-group])))
            (@tr [:deployment-group]))]
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
           [ui/TableCell [OperationalStatusSummary deployment-set operational-status]]]
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
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:dg-type]))]
        ^{:key (or id "dg-type")}
        [DGTypeCell creating?]]
       (when-not creating?
         [:<>
          (when created-by
            [ui/TableRow
             [ui/TableCell (str/capitalize (@tr [:created-by]))]
             [ui/TableCell @(subscribe [::session-subs/resolve-user created-by])]])
          [ui/TableRow
           [ui/TableCell (str/capitalize (@tr [:created]))]
           [ui/TableCell [uix/TimeAgo created]]]
          [ui/TableRow
           [ui/TableCell (str/capitalize (@tr [:updated]))]
           [ui/TableCell [uix/TimeAgo updated]]]])
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:auto-update]))]
        [ui/TableCell
         [AutoUpdateControl deployment-set creating?]]]]]]))

(defn AppsInAppsSetsCard
  [ids]
  (let [tr   (subscribe [::i18n-subs/tr])
        apps (subscribe [::subs/select-apps-by-id ids])]
    [:div
     [:div
      (@tr [:application-bouquet-this-is-an]) " "
      [:span {:style {:font-weight :bold}} (@tr [:application-bouquet])]
      " " (@tr [:application-bouquet-containing-these-apps]) ":"]
     [:ul
      (doall
        (for [id ids
              :let [app (get @apps id)]]
          ^{:key id}
          [:li
           (:name app)]))]]))

(defn AppsPickerAppsSetsCard
  [{:keys [subtype name id desc-summary published target
           show-published-tick? detail-href on-click button-ops]}]
  (let [tr       (subscribe [::i18n-subs/tr])
        apps-set (subscribe [::subs/app-by-id id])]
    (fn []
      [uix/Card
       {:header        [:div.nuvla-apps [:h3 {:style {:background-color "#2185d0"}
                                              :class [:ui-header :ui-apps-picker-card-header]}
                                         [icons/Icon {:name (apps-utils/subtype-icon subtype)}]
                                         (@tr [:application-bouquet])]]
        :description   [:<>
                        [:h4 [icons/Icon {:name (apps-utils/subtype-icon subtype)}]
                         (or name id)]
                        [:div
                         [AppsInAppsSetsCard (events/apps-set->app-ids @apps-set)]
                         desc-summary]]
        :corner-button (when (and published show-published-tick?)
                         [ui/Label {:corner true} [icons/Icon {:name apps-utils/publish-icon}]])
        :href          detail-href
        :on-click      on-click
        :button        [uix/Button button-ops]
        :target        target}])))

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
        button-content (@tr [:add-to-selection])
        button-ops     {:fluid   true
                        :color   "blue"
                        :icon    button-icon
                        :content button-content}
        desc-summary   (-> description
                           utils-values/markdown->summary
                           (general-utils/truncate 60))
        is-apps-set?   (= subtype
                          apps-utils/subtype-applications-sets)
        props          {:logo-url     logo-url
                        :subtype      subtype
                        :name         name
                        :id           id
                        :desc-summary [:<>
                                       [:p desc-summary]
                                       [:div
                                        [:p (str (str/capitalize (@tr [:project])) ": "
                                                 (-> (or (:path app) "")
                                                     (str/split "/")
                                                     first))]
                                        [:p (str (str/capitalize (@tr [:vendor])) ": ") [AuthorVendorForModule app :span]]
                                        (when-not is-apps-set? [:p (str (str/capitalize (@tr [:price])) ": " deploy-price)])]]
                        :tags         tags
                        :published    published
                        :detail-href  detail-href
                        :button-ops   button-ops
                        :on-click     (fn [event]
                                        (dispatch [::events/add-app-from-picker app])
                                        (close-modal)
                                        (dispatch [::full-text-search-plugin/search [::apps-store-spec/modules-search]])
                                        (.preventDefault event)
                                        (.stopPropagation event))}]
    (if is-apps-set?
      [AppsPickerAppsSetsCard
       props]
      [apps-store-views/ModuleCardView
       props])))

(defn AddButton
  [{:keys [modal-id enabled tooltip data-testid] :or {enabled true}}]
  [tt/WithTooltip [:div [uix/Button {:on-click    (fn [] (dispatch [::events/set-opened-modal modal-id]))
                                     :disabled    (not enabled)
                                     :data-testid data-testid
                                     :icon        icons/i-plus-large
                                     :class       "add-button"
                                     :style       {:align-self "center"}}]]
   tooltip])

(defn RemoveButton
  [{:keys [enabled tooltip on-click] :or {enabled true}}]
  [tt/WithTooltip
   [:span [icons/XMarkIcon
           {:style    {:cursor (if enabled :pointer :default)}
            :disabled (not enabled)
            :color    "red"
            :on-click on-click}]]
   tooltip])

(defn EditEdgeFilterButton
  [id {:keys [creating? disabled]}]
  (let [tr                         (subscribe [::i18n-subs/tr])
        can-edit-data?             (subscribe [::subs/can-edit-data? creating?])
        edit-op-allowed?           (subscribe [::subs/edit-op-allowed? creating?])
        edit-not-allowed-in-state? (subscribe [::subs/edit-not-allowed-in-state?])
        fleet-filter               (subscribe [::subs/fleet-filter])]
    [tt/WithTooltip
     [:span [uix/Button
             {:disabled (or disabled (and (not creating?) (not @edit-op-allowed?)))
              :on-click (fn []
                          (dispatch [::events/init-edge-picker-with-dynamic-filter @fleet-filter])
                          (dispatch [::events/set-opened-modal id]))
              :icon     icons/i-filter
              :style    {:align-self "center"}}]]
     (edit-not-allowed-msg {:TR                         @tr
                            :can-edit-data?             @can-edit-data?
                            :edit-op-allowed?           @edit-op-allowed?
                            :edit-not-allowed-in-state? @edit-not-allowed-in-state?})]))

(defn AppsPicker
  [tab-key pagination-db-path]
  (let [modules (subscribe [::apps-store-subs/modules])]
    (fn []
      ^{:key tab-key}
      [ui/TabPane
       [:div {:style {:display :flex}}
        [full-text-search-plugin/FullTextSearch
         {:db-path      [::apps-store-spec/modules-search]
          :change-event [::pagination-plugin/change-page [pagination-db-path] 1]}]]
       [:div {:style {:margin-top "1rem"}}
        [apps-store-views/ModulesCardsGroupView
         (for [{:keys [id] :as module} (get @modules :resources [])]
           ^{:key id}
           [AppPickerCard module])]
        [pagination-plugin/Pagination
         {:db-path      [pagination-db-path]
          :total-items  (:count @modules)
          :change-event [::events/fetch-app-picker-apps pagination-db-path]}]]])))

(defn AppsPickerModal
  [creating?]
  (let [tr      (subscribe [::i18n-subs/tr])
        open?   (subscribe [::subs/modal-open? events/apps-picker-modal-id])
        tab-key apps-store-spec/allapps-key]
    (fn []
      [ui/Modal {:size       :fullscreen
                 :open       @open?
                 :close-icon true
                 :on-close   close-modal}
       [uix/ModalHeader {:header (@tr (if creating?
                                        [:create-deployment-group]
                                        [:edit-deployment-group]))}]
       [ui/ModalContent
        [AppsPicker tab-key ::spec/pagination-apps-picker]]])))

(defn- create-app-config-query-key [i id]
  (keyword (str "configure-set-" i "-app-" id)))

(defn ModuleVersion
  [label created]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Popup
     {:content (r/as-element [:p (str/capitalize (@tr [:created]))
                              " "
                              [uix/TimeAgo created]])
      :trigger (r/as-element [:span label " " [icons/InfoIconFull]])}]))

(defn LinkToAppConfig
  [creating? i cell-data row-data]
  (let [tr                                  (subscribe [::i18n-subs/tr])
        is-controlled-by-apps-set?          (subscribe [::subs/is-controlled-by-apps-set?])
        is-behind-latest-published-version? (subscribe [::module-plugin/is-behind-latest-published-version? [::spec/apps-sets i] (:href row-data)])]
    (if creating?
      [tt/WithTooltip [:span cell-data] (@tr [:configure-app])]
      [:span
       [tt/WithTooltip
        [:a
         {:href     "#"
          :on-click #(dispatch [::events/navigate-internal
                                {:query-params
                                 (merge
                                   {(routes-utils/db-path->query-param-key [::apps-config])
                                    (create-app-config-query-key i (:href row-data))}
                                   {:deployment-groups-detail-tab :apps})}])
          :children [icons/StoreIcon]
          :target   :_self}
         cell-data
         [:span {:style {:margin-left "0.5rem"}}
          [icons/GearIcon]]]
        (@tr [:configure-app])]
       (when (and @is-behind-latest-published-version? (not @is-controlled-by-apps-set?))
         [tt/WithTooltip
          [:span [icons/TriangleExclamationIcon {:style {:margin-left "5px"}
                                                 :color :orange}]]
          (@tr [:version-behind-published])])])))

(defn LinkToModuleDetails
  [trigger]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Popup
     {:content (r/as-element [:p (@tr [:go-to-app])])
      :trigger (r/as-element [:span trigger])}]))

(defn LinkToAppSetConfig
  [creating? app-set-id name-component]
  (let [tr                                  (subscribe [::i18n-subs/tr])
        is-behind-latest-published-version? (subscribe [::module-plugin/is-behind-latest-published-version? [::spec/apps-sets 0 :apps-set] app-set-id])]
    (if creating?
      name-component
      [:div {:style {:display :flex :align-items :center}}
       [:a
        {:href     "#"
         :on-click #(dispatch [::events/navigate-internal
                               {:query-params {:deployment-groups-detail-tab :apps}}])
         :children [icons/StoreIcon]

         :target   :_self}
        [:div {:style {:display :flex :align-items :center}}
         name-component
         [:span {:style {:margin-left "0.5rem"}}
          [icons/GearIcon]]]]
       (when @is-behind-latest-published-version?
         [tt/WithTooltip
          [:span [icons/TriangleExclamationIcon {:style {:margin-left "5px"}
                                                 :color :orange}]]
          (@tr [:version-behind-published])])])))

(defn AppsSetHeader
  [creating? no-apps?]
  (let [tr               (subscribe [::i18n-subs/tr])
        apps-set-id      (subscribe [::subs/apps-set-id])
        apps-set-name    (subscribe [::subs/apps-set-name])
        apps-set-version (subscribe [::subs/apps-set-version])
        apps-set-created (subscribe [::subs/apps-set-created])
        apps-set-path    (subscribe [::subs/apps-set-path])
        name-component   [:p {:style {:margin 0}} @apps-set-name]]
    [:div
     [:div {:style {:display :flex :align-items :center :font-size :large :justify-content :space-between}}
      [LinkToAppSetConfig creating? @apps-set-id name-component]
      [ModuleVersion (str "v" @apps-set-version) @apps-set-created]
      [LinkToModuleDetails
       [module-plugin/LinkToAppView {:path       @apps-set-path
                                     :version-id @apps-set-version}
        [icons/ArrowRightFromBracketIcon]]]
      (when creating?
        [RemoveButton {:enabled  true
                       :on-click #(dispatch [::events/remove-apps-set])}])]

     [:div {:style {:margin-top "10px"}}
      (if-not no-apps?
        (@tr [:app-bouquet-includes-apps])
        (@tr [:app-bouquet-has-no-apps]))]]))

(defn- AppsOverviewTable
  [creating?]
  (let [tr                         (subscribe [::i18n-subs/tr])
        apps-row                   (subscribe [::subs/apps-row-data])
        is-controlled-by-apps-set? (subscribe [::subs/is-controlled-by-apps-set?])
        apps-validation-error?     (subscribe [::subs/apps-validation-error?])
        can-edit-data?             (subscribe [::subs/can-edit-data? creating?])
        edit-op-allowed?           (subscribe [::subs/edit-op-allowed? creating?])
        edit-not-allowed-in-state? (subscribe [::subs/edit-not-allowed-in-state?])
        k->tr-k                    {:app :name}]
    (fn []
      (let [no-apps? (empty? @apps-row)]
        [:<>
         [:div {:style {:height "100%"}}
          (when @is-controlled-by-apps-set?
            [AppsSetHeader creating? no-apps?])
          (when-not no-apps?
            [:div {:style {:margin-top "8px"}}
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
                                                  [LinkToAppConfig creating? i cell-data row-data])
                                                :version
                                                (fn [{{:keys [label created]} :cell-data}]
                                                  [ModuleVersion label created])
                                                nil)})
                           (keys (dissoc (first @apps-row) :id :idx :href :module))))
                       (remove nil?
                               [{:field-key      :details
                                 :header-content (str/capitalize (@tr [:details]))
                                 :cell           (fn [{:keys [row-data]}]
                                                   [LinkToModuleDetails
                                                    [module-plugin/LinkToAppView
                                                     {:version-id (deployment-utils/get-version-number
                                                                    (:versions (:module row-data))
                                                                    (:content (:module row-data)))
                                                      :path       (:path (:module row-data))}
                                                     [icons/ArrowRightFromBracketIcon]]])}
                                (when (and @can-edit-data? (not @is-controlled-by-apps-set?))
                                  {:field-key      :remove
                                   :header-content (str/capitalize (@tr [:remove]))
                                   :cell           (fn [{:keys [row-data]}]
                                                     [RemoveButton {:enabled  @edit-op-allowed?
                                                                    :tooltip  (edit-not-allowed-msg
                                                                                {:TR                         @tr
                                                                                 :can-edit-data?             @can-edit-data?
                                                                                 :edit-op-allowed?           @edit-op-allowed?
                                                                                 :edit-not-allowed-in-state? @edit-not-allowed-in-state?})
                                                                    :on-click #(dispatch [::events/remove-app-from-creation-data row-data])}])})]))
                     :rows @apps-row}]])]
         (when (and @can-edit-data? (not @is-controlled-by-apps-set?))
           [:<>
            [:div {:style {:display :flex :justify-content :center :align-items :center}}
             [:<>
              [AppsPickerModal creating?]
              [:div {:style {:margin-top   "1rem"
                             :margin-bottm "1rem"}}
               [AddButton {:modal-id events/apps-picker-modal-id
                           :enabled  @edit-op-allowed?
                           :tooltip  (edit-not-allowed-msg
                                       {:TR                         @tr
                                        :can-edit-data?             @can-edit-data?
                                        :edit-op-allowed?           @edit-op-allowed?
                                        :edit-not-allowed-in-state? @edit-not-allowed-in-state?})}]]]]
            [:div {:style {:margin-top   "1rem"
                           :margin-left  "auto"
                           :margin-right "auto"}}
             (if @apps-validation-error?
               [:span {:style {:color :red}} (@tr [:select-at-least-one-app])]
               (if no-apps?
                 (@tr [:add-your-first-app])
                 (@tr [:add-app])))]])]))))


(defn StatisticStatesEdgeView [{:keys [total online offline unknown]}]
  (let [current-route     @(subscribe [::route-subs/current-route])
        to-edges-tab      {:deployment-groups-detail-tab :edges}
        edges             (subscribe [::subs/all-edges-ids])
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
     [components/StatisticState {:clickable? (seq @edges)
                                 :value      total
                                 :stacked?   true
                                 :icons      [icons/i-box]
                                 :label      "TOTAL"
                                 :color      "black"
                                 :on-click   #(dispatch [::routing-events/navigate
                                                         (:resource (create-target-url ""))
                                                         nil nil {:ignore-chng-protection? true}])}]
     [dashboard-views/Statistic (cond-> {:value          online
                                         :icon           icons/i-power
                                         :label          edges-utils/status-online
                                         :positive-color "green"
                                         :color          "green"}
                                        (seq @edges)
                                        (assoc :on-click #(dispatch [::routing-events/navigate
                                                                     (:resource (create-target-url "ONLINE"))
                                                                     nil nil {:ignore-chng-protection? true}])))]
     [dashboard-views/Statistic (cond-> {:value offline
                                         :icon  icons/i-power
                                         :label edges-utils/status-offline
                                         :color "red"}
                                        (seq @edges)
                                        (assoc :on-click #(dispatch [::routing-events/navigate
                                                                     (:resource (create-target-url "OFFLINE"))
                                                                     nil nil {:ignore-chng-protection? true}])))]
     [dashboard-views/Statistic (cond-> {:value unknown
                                         :icon  icons/i-power
                                         :label edges-utils/status-unknown
                                         :color "orange"}
                                        (seq @edges)
                                        (assoc :on-click #(dispatch [::routing-events/navigate
                                                                     (:resource (create-target-url "UNKNOWN"))
                                                                     nil nil {:ignore-chng-protection? true}])))]]))

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
  (let [deployments (subscribe [::deployments-subs/deployments])]
    [dv/StatisticStates (pos? (:count @deployments)) ::deployments-subs/deployments-summary-all
     (mapv (fn [state] (cond-> (assoc state
                                 :selected? (or
                                              (= state-filter (:label state))
                                              (and
                                                (nil? state-filter)
                                                (= "TOTAL" (:label state))))
                                 :on-click (create-nav-fn "deployments" {:depl-state (:label state)}))))
           dv/default-states)]))

(defn- DeploymentsStatesCard
  [state-filter]
  (let [deployments (subscribe [::deployments-subs/deployments])
        tr          (subscribe [::i18n-subs/tr])]
    (fn []
      [dv/TitledCardDeployments
       [DeploymentStatesFilter state-filter]
       [uix/Button {:class    "center"
                    :color    "blue"
                    :icon     icons/i-rocket
                    :disabled (or
                                (nil? (:count @deployments))
                                (= 0 (:count @deployments)))
                    :content  (@tr [:show-me])
                    :on-click (create-nav-fn "deployments" nil)}]])))

(defn polish-fleet-filter
  [tr fleet-filter]
  (let [polished (some-> fleet-filter
                         (str/replace (str utils/fleet-filter-catch-all " and ") "")
                         (str/replace utils/fleet-filter-catch-all ""))]
    (if (empty? polished)
      (tr [:deploy-with-catch-all-edges-filter])
      polished)))

(defn FleetFilterPanel [{:keys [show-edit-filter-button? creating? disabled]}]
  (let [tr                   (subscribe [::i18n-subs/tr])
        can-edit-data?       (subscribe [::subs/can-edit-data? creating?])
        fleet-filter         (subscribe [::subs/fleet-filter])
        fleet-filter-edited? (subscribe [::subs/fleet-filter-edited?])
        deployment-set       (subscribe [::subs/deployment-set])]
    [ui/Grid {:columns 1}
     [ui/GridColumn {:stretched true
                     :style     {:textAlign "center"}}
      [ui/Popup {:trigger  (r/as-element
                             [:div {:style {:margin :auto}}
                              (when (and @can-edit-data? show-edit-filter-button?)
                                [EditEdgeFilterButton events/edges-picker-modal-id {:creating? creating?
                                                                                    :disabled  disabled}])])
                 :disabled disabled
                 :content  (str (str/capitalize (@tr [:filter])) ": " (polish-fleet-filter @tr @fleet-filter))}]
      (when-not creating?
        [:div {:style {:margin-top "0.1rem"}}
         (@tr [:recompute-fleet-info])])
      [:div {:style {:margin-top "0.5rem"}}
       [RecomputeFleetButton @deployment-set]
       (when (and (not creating?) @fleet-filter-edited?)
         [:p {:style {:align-self :center}}
          (@tr [:fleet-filter-edited])])]]]))


(defn- ResolvedUser
  [user-id]
  (fn []
    (let [user (subscribe [::session-subs/resolve-user user-id])]
      @user)))

(defn EdgePickerContent
  []
  (let [edges             (subscribe [::subs/edge-picker-edges-resources])
        edges-count       (subscribe [::subs/edge-picker-filtered-edges-count])
        edges-stats       (subscribe [::subs/edge-picker-edges-summary-stats])
        selected-state    (subscribe [::subs/state-selector])
        additional-filter (subscribe [::subs/edge-picker-additional-filter])
        filter-open?      (r/atom false)
        fleet-filter      (subscribe [::subs/fleet-filter])]
    (fn []
      (let [select-fn (fn [id] (dispatch [::table-plugin/select-id id [::spec/edge-picker-select] (map :id @edges)]))]
        [:<>
         [:div {:style {:display :flex}}
          [:div
           (when-not @fleet-filter
             [full-text-search-plugin/FullTextSearch
              {:db-path      [::spec/edge-picker-full-text-search]
               :change-event [::pagination-plugin/change-page [::spec/edge-picker-pagination] 1]}])
           ^{:key @additional-filter}
           [:div {:style {:margin-top    "0.4rem"
                          :margin-bottom "0.4rem"}}
            [filter-comp/ButtonFilter
             {:resource-name                    edges-spec/resource-name
              :default-filter                   @additional-filter
              :open?                            filter-open?
              :on-done                          #(dispatch [::events/set-edge-picker-additional-filter %])
              :show-clear-button-outside-modal? true
              :persist?                         false}]]]
          (when-not @fleet-filter
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
              true true]])]
         [Table (cond->
                  {:row-click-handler #(select-fn (:id %))
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
                                       :style {:cursor "pointer"}}}
                  (not @fleet-filter)
                  (assoc :select-config {:total-count-sub-key [::subs/edge-picker-filtered-edges-count]
                                         :resources-sub-key   [::subs/edge-picker-edges-resources]
                                         :select-db-path      [::spec/edge-picker-select]}))]
         [pagination-plugin/Pagination
          {:db-path                [::spec/edge-picker-pagination]
           :change-event           [::events/get-picker-edges]
           :total-items            @edges-count
           :i-per-page-multipliers [1 2 4]}]]))))

(defn EdgesPickerModal
  [creating?]
  (let [tr             (subscribe [::i18n-subs/tr])
        open?          (subscribe [::subs/modal-open? events/edges-picker-modal-id])
        deployment-set (subscribe [::subs/deployment-set])
        add-to-select  (fn []
                         (dispatch [::events/get-selected-edge-ids @deployment-set creating?]))
        update-filter  (fn []
                         (dispatch [::events/update-fleet-filter creating?])
                         (dispatch [::events/set-opened-modal nil]))
        fleet-filter   (subscribe [::subs/fleet-filter])]
    (fn []
      [ui/Modal {:open       @open?
                 :close-icon true
                 :on-close   close-modal}
       [uix/ModalHeader {:header (if @fleet-filter (@tr [:edit-fleet-filter]) (@tr [:add-edges]))}]
       [ui/ModalContent
        [EdgePickerContent]]
       [ui/ModalActions
        (if @fleet-filter
          [uix/Button {:text     (@tr [:set-new-dynamic-edge-filter])
                       :positive true
                       :active   true
                       :on-click update-filter}]
          [uix/Button {:text     (@tr [:add-to-depl-group])
                       :positive true
                       :active   true
                       :on-click add-to-select}])]])))

(defn- UnstoredEdgeChanges
  [fleet-changes]
  (let [tr      (subscribe [::i18n-subs/tr])
        removed (:removed fleet-changes)
        added   (:added fleet-changes)]
    [:span
     (str (@tr [:unsaved-fleet-changes]) ": "
          (when removed (str (count removed) (str " " (@tr [:removed]))))
          (when (and removed added) ", ")
          (when added (str (count added) (str " " (@tr [:added])))))]))

(defn EdgeModeChangeModalDanger
  [{:keys [on-confirm]}]
  (r/with-let [tr    (subscribe [::i18n-subs/tr])
               open? (subscribe [::subs/edge-mode-change-modal-danger-open?])]
    [uix/ModalDanger
     {:header      (@tr [:edge-mode-change-modal-danger-header])
      :content     (@tr [:edge-mode-change-modal-danger-content])
      :open        @open?
      :on-close    #(dispatch [::events/close-edge-mode-change-modal-danger])
      :button-text (@tr [:edge-mode-change-modal-danger-confirm])
      :on-confirm  (fn []
                     (dispatch [::events/close-edge-mode-change-modal-danger])
                     (on-confirm))}]))

(defn EdgeOverviewContent
  [_edges-stats creating?]
  (let [tr                         (subscribe [::i18n-subs/tr])
        deployment-set             (subscribe [::subs/deployment-set])
        edges                      (subscribe [::subs/edges-documents])
        missing-edges              (subscribe [::subs/missing-edges])
        can-edit-data?             (subscribe [::subs/can-edit-data? creating?])
        edit-op-allowed?           (subscribe [::subs/edit-op-allowed? creating?])
        edit-not-allowed-in-state? (subscribe [::subs/edit-not-allowed-in-state?])
        fleet-filter               (subscribe [::subs/fleet-filter])
        fleet-changes              (subscribe [::subs/fleet-changes])
        requested-edge-mode        (r/atom nil)
        on-chg-confirmed-fn        (fn []
                                     (if (= :static @requested-edge-mode)
                                       (do
                                         (dispatch [::events/set-fleet-filter nil (:id deployment-set)])
                                         (dispatch [::events/clear-edges]))
                                       (do
                                         (dispatch [::events/set-fleet-filter utils/fleet-filter-catch-all (:id deployment-set)])
                                         (dispatch [::events/update-fleet-filter creating?]))))
        no-selection?-fn           (fn []
                                     (or (and (= :dynamic @requested-edge-mode)
                                              (empty? @edges))
                                         (and (= :static @requested-edge-mode)
                                              (or (nil? @fleet-filter)
                                                  (= utils/fleet-filter-catch-all @fleet-filter)))))
        on-edge-mode-change-fn     (fn [edge-mode]
                                     (fn [_]
                                       (when (or (and (= :static edge-mode) (some? @fleet-filter))
                                                 (and (= :dynamic edge-mode) (not @fleet-filter)))
                                         (reset! requested-edge-mode edge-mode)
                                         (if (no-selection?-fn)
                                           (on-chg-confirmed-fn)
                                           (dispatch [::events/open-edge-mode-change-modal-danger])))))]
    (fn [edges-stats creating?]
      [:<>
       [StatisticStatesEdgeView edges-stats]
       (when @fleet-changes
         [:div {:style {:margin "1.4rem auto"}}
          [UnstoredEdgeChanges @fleet-changes]])
       [uix/Button {:class    "center"
                    :icon     icons/i-box
                    :content  (@tr [:show-me])
                    :disabled (or (nil? (:total edges-stats))
                                  (= 0 (:total edges-stats)))
                    :on-click (create-nav-fn "edges" {:edges-state nil})}]
       [:div {:style {:display         :flex
                      :margin-top      "15px"
                      :justify-content :center
                      :align-items     :top
                      :flex-direction  :row
                      :gap             "20px"}}
        (when (and @can-edit-data? (or creating? (nil? @fleet-filter)))
          ;; TODO when implementing creation flow from apps page: Always show button and use temp-id for storing
          ;; and retrieving deployment-set and deployment-set-edited
          [:div {:style {:display        :flex
                         :align-items    :center
                         :flex-direction :column}}
           (when creating?
             [ui/Radio {:checked  (nil? @fleet-filter)
                        :enabled  @edit-op-allowed?
                        :on-click (on-edge-mode-change-fn :static)
                        :label    (@tr [:pick-edges-manually])
                        :style    {:margin-bottom "10px"}}])
           [AddButton {:modal-id    events/edges-picker-modal-id
                       :data-testid "add-edges-button"
                       :enabled     (and @edit-op-allowed? (nil? @fleet-filter))
                       :tooltip     (edit-not-allowed-msg
                                      {:TR                         @tr
                                       :can-edit-data?             @can-edit-data?
                                       :edit-op-allowed?           @edit-op-allowed?
                                       :edit-not-allowed-in-state? @edit-not-allowed-in-state?})}]
           (when-not creating?
             [:div {:style {:margin-top "1rem"}}
              (@tr [:add-edges])])])
        (when (or creating? (some? @fleet-filter))
          [:div {:style {:display        :flex
                         :align-items    :center
                         :flex-direction :column}}
           (when creating?
             [ui/Radio {:checked  (some? @fleet-filter)
                        :enabled  @edit-op-allowed?
                        :on-click (on-edge-mode-change-fn :dynamic)
                        :label    (@tr [:recompute-fleet-info])
                        :style    {:margin-bottom "10px"}}])
           [FleetFilterPanel {:show-edit-filter-button? true
                              :creating?                creating?
                              :disabled                 (nil? @fleet-filter)}]])
        [EdgesPickerModal creating?]
        [MissingEdgesPanel @deployment-set @missing-edges]
        [EdgeModeChangeModalDanger {:on-confirm on-chg-confirmed-fn}]]])))

(defn TabOverview
  [uuid creating?]
  (dispatch [::events/get-deployments-for-deployment-sets uuid])
  (let [deployment-set             (subscribe [::subs/deployment-set])
        edges-stats                (subscribe [::subs/edges-summary-stats])
        is-controlled-by-apps-set? (subscribe [::subs/is-controlled-by-apps-set?])]
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
             :label (if @is-controlled-by-apps-set?
                      (@tr [:application-bouquet])
                      (str/capitalize (@tr [:apps])))}
            [AppsOverviewTable creating?]]]
          [ui/GridColumn {:stretched true}
           [vc/TitledCard
            {:class :nuvla-edges
             :icon  icons/i-box
             :label (@tr [:edges])}
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
  [i module-id creating?]
  (let [tr                         (subscribe [::i18n-subs/tr])
        can-edit-data?             (subscribe [::subs/can-edit-data? creating?])
        edit-op-allowed?           (subscribe [::subs/edit-op-allowed?])
        edit-not-allowed-in-state? (subscribe [::subs/edit-not-allowed-in-state?])
        is-controlled-by-apps-set? (subscribe [::subs/is-controlled-by-apps-set?])]
    [uix/Accordion
     [tt/WithTooltip
      [:div [module-plugin/ModuleVersions
             {:db-path      [::spec/apps-sets i]
              :href         module-id
              :read-only?   (or (not @can-edit-data?) (not @edit-op-allowed?) @is-controlled-by-apps-set?)
              :change-event [::events/edit-config]}]]
      (edit-not-allowed-msg {:TR                         @tr
                             :can-edit-data?             @can-edit-data?
                             :edit-op-allowed?           @edit-op-allowed?
                             :edit-not-allowed-in-state? @edit-not-allowed-in-state?
                             :is-controlled-by-apps-set? @is-controlled-by-apps-set?})]
     :label (if @is-controlled-by-apps-set? (str/capitalize (@tr [:version])) (@tr [:select-version]))]))

(defn EnvVariablesApp
  [i module-id creating?]
  (let [tr                         (subscribe [::i18n-subs/tr])
        can-edit-data?             (subscribe [::subs/can-edit-data? creating?])
        edit-op-allowed?           (subscribe [::subs/edit-op-allowed? creating?])
        edit-not-allowed-in-state? (subscribe [::subs/edit-not-allowed-in-state?])]
    [uix/Accordion
     [tt/WithTooltip
      [:div [module-plugin/EnvVariables
             {:db-path           [::spec/apps-sets i]
              :href              module-id
              :read-only?        (or (not @can-edit-data?) (not @edit-op-allowed?))
              :highlight-errors? true
              :change-event      [::events/edit-config]}]]
      (edit-not-allowed-msg {:TR                         @tr
                             :can-edit-data?             @can-edit-data?
                             :edit-op-allowed?           @edit-op-allowed?
                             :edit-not-allowed-in-state? @edit-not-allowed-in-state?})]
     :label (@tr [:env-variables])]))

(defn FilesApp
  [i module-id creating?]
  (let [tr                         (subscribe [::i18n-subs/tr])
        can-edit-data?             (subscribe [::subs/can-edit-data? creating?])
        edit-op-allowed?           (subscribe [::subs/edit-op-allowed? creating?])
        edit-not-allowed-in-state? (subscribe [::subs/edit-not-allowed-in-state?])]
    [uix/Accordion
     [tt/WithTooltip
      [:div [module-plugin/Files
             {:db-path      [::spec/apps-sets i]
              :href         module-id
              :change-event [::events/edit-config]
              :read-only?   (or (not @can-edit-data?) (not @edit-op-allowed?))}]]
      (edit-not-allowed-msg {:TR                         @tr
                             :can-edit-data?             @can-edit-data?
                             :edit-op-allowed?           @edit-op-allowed?
                             :edit-not-allowed-in-state? @edit-not-allowed-in-state?})]
     :label (@tr [:module-files])]))


(defn- AppName [{:keys [idx id]}]
  (let [app    (subscribe [::module-plugin/module
                           [::spec/apps-sets idx] id])
        error? (subscribe [::subs/app-config-validation-error? idx id])]
    (fn []
      [:span {:style {:color (if @error? utils-forms/dark-red "black")}}
       (or (:name @app) (:id @app))])))

(defn LinkToModule
  [db-path module-id dictionary-key]
  (let [tr (subscribe [::i18n-subs/tr])]
    [module-plugin/LinkToApp
     {:db-path  db-path
      :href     module-id
      :children [:<>
                 [ui/Icon {:class icons/i-link}]
                 (@tr dictionary-key)]}]))

(defn AppLicense
  [license]
  (let [tr (subscribe [::i18n-subs/tr])]
    [uix/Accordion
     [ui/Table {:compact true, :definition true}
      [ui/TableBody
       [uix/TableRowField (@tr [:name])
        :key "license-name"
        :editable? false
        :default-value (:name license)]
       [uix/TableRowField (@tr [:description])
        :key "license-description"
        :editable? false
        :default-value (:description license)]
       [uix/TableRowField (@tr [:url])
        :key "license-url"
        :editable? false
        :default-value [:a {:href   (:url license)
                            :target :_blank}
                        (:url license)]]]]
     :label (@tr [:eula])]))

(defn AppPricing
  [pricing]
  (let [tr          (subscribe [::i18n-subs/tr])
        edges-count (subscribe [::subs/edges-count])]
    [uix/Accordion
     [ui/Table {:compact true, :definition true}
      [ui/TableBody
       [uix/TableRowField (@tr [:daily-unit-price])
        :key "daily-unit-price"
        :editable? false
        :default-value (general-utils/format-money (/ (:cent-amount-daily pricing) 100))]
       [uix/TableRowField (@tr [:quantity])
        :key "quantity"
        :editable? false
        :default-value @edges-count]
       [uix/TableRowField (@tr [:daily-price])
        :key "license-url"
        :editable? false
        :default-value (general-utils/format-money
                         (/ (* (:cent-amount-daily pricing) @edges-count) 100))]]]
     :label (str/capitalize (@tr [:pricing]))]))

(defn WarningVersionBehind
  [content-i18n-key]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Message {:warning true}
     [ui/MessageHeader (@tr [:warning])]
     [ui/MessageContent (@tr content-i18n-key)]]))

(defn ConfigureAppTabHeader
  [i id]
  (let [is-controlled-by-apps-set?          (subscribe [::subs/is-controlled-by-apps-set?])
        is-behind-latest-published-version? (subscribe [::module-plugin/is-behind-latest-published-version? [::spec/apps-sets i] id])]
    [:<>
     [AppName {:idx i :id id}]
     (when (and @is-behind-latest-published-version? (not @is-controlled-by-apps-set?))
       [icons/TriangleExclamationIcon {:style {:margin-left "5px"}
                                       :color :orange}])]))

(defn ConfigureApp
  [i {id :href :as _application} creating?]
  (let [tr                                  (subscribe [::i18n-subs/tr])
        licenses-by-module-id               (subscribe [::subs/license-by-module-id])
        pricing-by-module-id                (subscribe [::subs/pricing-by-module-id])
        is-controlled-by-apps-set?          (subscribe [::subs/is-controlled-by-apps-set?])
        is-behind-latest-published-version? (subscribe [::module-plugin/is-behind-latest-published-version? [::spec/apps-sets i] id])]
    [ui/TabPane {:attached true}
     (when (and @is-behind-latest-published-version? (not @is-controlled-by-apps-set?))
       [WarningVersionBehind [:warning-not-latest-app-version]])
     [ui/Popup {:trigger (r/as-element
                           [LinkToModule [::spec/apps-sets i] id [:go-to-app]])
                :content (@tr [:open-app-in-new-window])}]
     [ModuleVersionsApp i id creating?]
     [EnvVariablesApp i id creating?]
     [FilesApp i id creating?]
     (when-let [license (get @licenses-by-module-id id)]
       [AppLicense license])
     (when-let [pricing (get @pricing-by-module-id id)]
       [AppPricing pricing])]))

(defn ConfigureApps
  [i applications creating?]
  ^{:key (str "set-" i)}
  [ui/Segment
   [tab/Tab
    {:db-path                 [::apps-config]
     :ignore-chng-protection? true
     :attached                true
     :tabular                 true
     :panes                   (map
                                (fn [{id :href :as application}]
                                  {:menuItem {:content (r/as-element
                                                         [ConfigureAppTabHeader i id])
                                              :icon    "cubes"
                                              :key     (create-app-config-query-key i id)}
                                   :render   #(r/as-element
                                                [ConfigureApp i application creating?])})
                                applications)}]])

(defn ConfigureAppsSetHeader
  [db-path id apps-set-name]
  (let [tr                                  (subscribe [::i18n-subs/tr])
        is-behind-latest-published-version? (subscribe [::module-plugin/is-behind-latest-published-version? db-path id])]
    [:<>
     [:h2 apps-set-name
      (when @is-behind-latest-published-version?
        [tt/WithTooltip
         [:span [icons/TriangleExclamationIcon {:style {:margin-left "5px"}
                                                :color :orange}]]
         (@tr [:version-behind-published])])]
     (when @is-behind-latest-published-version?
       [WarningVersionBehind [:warning-not-latest-app-set-version]])]))

(defn ConfigureAppsSetWrapper
  [configure-apps creating?]
  (let [tr                         (subscribe [::i18n-subs/tr])
        can-edit-data?             (subscribe [::subs/can-edit-data? creating?])
        edit-op-allowed?           (subscribe [::subs/edit-op-allowed?])
        edit-not-allowed-in-state? (subscribe [::subs/edit-not-allowed-in-state?])
        is-controlled-by-apps-set? (subscribe [::subs/is-controlled-by-apps-set?])
        db-path                    [::spec/apps-sets 0 :apps-set]
        apps-set                   (subscribe [::subs/apps-set])]
    [:div.nuvla-apps
     (when @is-controlled-by-apps-set?
       [:div {:style {:margin-bottom "5px"}}
        [ConfigureAppsSetHeader db-path (:id @apps-set) (:name @apps-set)]
        [ui/Popup {:trigger (r/as-element
                              [LinkToModule db-path (:id @apps-set) [:go-to-app-set]])
                   :content (@tr [:open-app-in-new-window])}]
        [uix/Accordion
         [tt/WithTooltip
          [:div [module-plugin/ModuleVersions
                 {:db-path      [::spec/apps-sets 0 :apps-set]
                  :href         (:id @apps-set)
                  :read-only?   (or (not @can-edit-data?) (not @edit-op-allowed?))
                  :change-event [::events/change-apps-set-version]}]]
          (edit-not-allowed-msg {:TR                         @tr
                                 :can-edit-data?             @can-edit-data?
                                 :edit-op-allowed?           @edit-op-allowed?
                                 :edit-not-allowed-in-state? @edit-not-allowed-in-state?})]
         :label (@tr [:select-app-set-version])]
        [:h4 {:class :tab-app-detail} (@tr [:applications])]])
     configure-apps]))

(defn ConfigureAppsSet
  [creating?]
  (let [apps (subscribe [::subs/apps-row-data])]
    [ConfigureAppsSetWrapper
     [ConfigureApps 0 @apps creating?]
     creating?]))

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
                               [ConfigureApps i applications true]]])}
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
                :title       "New deployment group"
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
        edges-stats   (subscribe [::subs/edges-summary-stats])]
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
                                [::events/navigate-internal
                                 (routes-utils/new-route-data @current-route
                                                              {:partial-query-params
                                                               {events/edges-state-filter-key
                                                                (if (= "TOTAL" label)
                                                                  nil
                                                                  label)}}) nil nil])))) edges-views/edges-states))
       true true])))

(defn- EdgeTabStatesFilter
  [_creating?]
  (let [selected-state (subscribe [::route-subs/query-param events/edges-state-filter-key])]
    (fn [creating?]
      (dispatch [::events/get-edges creating?])
      [EdgeTabStatesFilterView @selected-state])))

(defn EdgesTabView
  [creating?]
  (let [tr                         (subscribe [::i18n-subs/tr])
        can-edit-data?             (subscribe [::subs/can-edit-data? creating?])
        edit-op-allowed?           (subscribe [::subs/edit-op-allowed? creating?])
        edit-not-allowed-in-state? (subscribe [::subs/edit-not-allowed-in-state?])
        edges                      (subscribe [::subs/edges-documents-response])
        fleet-filter               (subscribe [::subs/fleet-filter])
        fleet-changes              (subscribe [::subs/fleet-changes])
        only-changes?              (subscribe [::subs/show-only-changed-fleet?])
        columns                    (mapv (fn [col-config]
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
        filter-open?               (r/atom false)
        additional-filter          (subscribe [::subs/edges-additional-filter])]
    (fn []
      [:div {:style {:padding-top "10px"}
             :class :nuvla-edges}
       [ui/Grid {:stackable true
                 :reversed  "mobile"}
        [ui/GridColumn {:width 4}
         (when-not @fleet-filter
           [:div
            [full-text-search-plugin/FullTextSearch
             {:db-path      [::spec/edges-full-text-search]
              :change-event [::pagination-plugin/change-page [::spec/edges-pagination] 1]}]
            ^{:key @additional-filter}
            [:div {:style {:margin-top "0.4rem"}}
             [filter-comp/ButtonFilter
              {:resource-name                    edges-spec/resource-name
               :default-filter                   @additional-filter
               :open?                            filter-open?
               :on-done                          #(dispatch [::events/set-edges-additional-filter %])
               :show-clear-button-outside-modal? true
               :persist?                         false}]]])]
        [ui/GridColumn {:width 7}
         [:div {:class :nuvla-edges
                :style {:margin "0 auto 0 6rem"}}
          [EdgeTabStatesFilter creating?]]]]
       (when @fleet-changes
         [:div {:style {:margin-top    "1rem"
                        :margin-bottom "1rem"}}
          [:div [UnstoredEdgeChanges @fleet-changes]]
          [:div [ui/Checkbox {:checked  @only-changes?
                              :basic    true
                              :label    (@tr [:show-only-unsaved-changes])
                              :on-click #(dispatch [::events/show-fleet-changes-only @fleet-changes])}]]])
       [edges-views/NuvlaEdgeTableView
        (cond->
          {:edges       (mapv (fn [row]
                                (if
                                  (some #{(:id row)} (:removed @fleet-changes))
                                  (assoc row :table-row-prop {:style {:text-decoration "line-through"
                                                                      :opacity         0.5}})
                                  row))
                              (:resources @edges))
           :columns     columns
           :sort-config {:db-path     ::spec/edges-ordering
                         :fetch-event [::events/get-edges creating?]}}
          (and @can-edit-data? (not @fleet-filter))
          (assoc :select-config {:disabled-tooltip    (edit-not-allowed-msg
                                                        {:TR                         @tr
                                                         :can-edit-data?             @can-edit-data?
                                                         :edit-op-allowed?           @edit-op-allowed?
                                                         :edit-not-allowed-in-state? @edit-not-allowed-in-state?})
                                 :bulk-actions        [{:event (fn [select-data]
                                                                 (dispatch [::events/remove-edges creating? select-data]))
                                                        :key   :remove-edges
                                                        :name  (str (str/capitalize (@tr [:remove]))
                                                                    " "
                                                                    (str/lower-case (@tr [:edges])))
                                                        :icon  icons/BoxIcon}]
                                 :total-count-sub-key [::subs/edges-count]
                                 :resources-sub-key   [::subs/edges-documents]
                                 :select-db-path      [::spec/edges-select]}))]
       [pagination-plugin/Pagination
        {:db-path                [::spec/edges-pagination]
         :change-event           [::events/get-edges creating?]
         :total-items            (-> @edges :count)
         :i-per-page-multipliers [1 2 4]}]
       [FleetFilterPanel {:show-edit-filter-button? false :creating? creating?}]])))

(defn EdgesTab
  [creating?]
  (dispatch [::events/init-edges-tab])
  (fn []
    [EdgesTabView creating?]))

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
        apps-row       (subscribe [::subs/apps-row-data])
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
                                                         {:style {:color (when error? utils-forms/dark-red)}}
                                                         tab-title]))))
                                                :icon     icons/i-gear
                                                :disabled (or creating? (empty? @apps-row))}
                                     :render   #(r/as-element [ConfigureAppsSet creating?])}
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
                                                  [EdgesTab creating?])}
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
                                                :disabled (not (pos? (:count @depl-all)))}
                                     :render   #(r/as-element
                                                  [DeploymentsTab uuid])}
                                    (when-not creating? (job-views/jobs-section))]
          :ignore-chng-protection? true
          :menu                    {:secondary true
                                    :pointing  true}}]))))

(defn WarningServerSideChanges
  []
  (let [tr                  (subscribe [::i18n-subs/tr])
        server-side-changes (subscribe [::subs/server-side-changes])]
    (when @server-side-changes
      [ui/Message {:warning true}
       [ui/MessageHeader (@tr [:warning])]
       [ui/MessageContent (@tr [:warning-resource-changed-server-side])]])))

(defn- DeploymentSetView
  [uuid]
  (let [depl-set (subscribe [::subs/deployment-set])]
    (dispatch [::bulk-progress-plugin/set-target-resource [::spec/bulk-jobs] (str "deployment-set/" uuid)])
    (fn []
      (let [{:keys [id name]} @depl-set]
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
          [utils-validation/validation-error-message ::subs/not-ready-or-valid?]
          [MenuBar false]
          [bulk-progress-plugin/MonitoredJobs
           {:db-path [::spec/bulk-jobs]}]
          [job-views/ErrorJobsMessage
           ::job-subs/jobs nil nil
           #(dispatch [::tab/change-tab {:db-path [::spec/tab]
                                         :tab-key :jobs}])]
          [WarningServerSideChanges]
          [TabsDeploymentSet {:uuid uuid}]]]))))

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
        [MenuBar true]
        [TabsDeploymentSet {:creating? true}]]])))


(defn Details
  [uuid]
  (case (str/lower-case uuid)
    "new"
    [AddPage]

    "create"
    [DeploymentSetCreate]

    [DeploymentSetView uuid]))
