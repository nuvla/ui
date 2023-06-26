(ns sixsq.nuvla.ui.notifications.views
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch dispatch-sync subscribe]]
            [re-frame.db]
            [reagent.core :as r]
            [sixsq.nuvla.ui.cimi.events :as cimi-events]
            [sixsq.nuvla.ui.filter-comp.events :as fc-events]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.notifications.events :as events]
            [sixsq.nuvla.ui.notifications.spec :as spec]
            [sixsq.nuvla.ui.notifications.subs :as subs]
            [sixsq.nuvla.ui.notifications.utils :as utils]
            [sixsq.nuvla.ui.utils.form-fields :as ff]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
            [sixsq.nuvla.ui.utils.validation :as utils-validation]
            [taoensso.timbre :as timbre]))


(defn save-callback-notification-subscription-config
  []
  (dispatch-sync [::events/set-validate-form? true])
  (dispatch-sync [::events/validate-notification-subscription-config-form])
  (let [form-valid? (get @re-frame.db/app-db ::spec/form-valid?)]
    (when form-valid?
      (dispatch [::events/set-validate-form? false])
      (dispatch [::events/edit-subscription-config]))))


(defn save-callback-notification-method
  [form-validation-spec]
  (dispatch-sync [::events/set-validate-form? true])
  (dispatch-sync [::events/validate-notification-method-form form-validation-spec])
  (let [form-valid? (get @re-frame.db/app-db ::spec/form-valid?)]
    (when form-valid?
      (dispatch [::events/set-validate-form? false])
      (dispatch [::events/edit-notification-method]))))


(defn save-callback-subscription
  [form-validation-spec]
  (dispatch-sync [::events/set-validate-form? true])
  (dispatch-sync [::events/validate-subscription-form form-validation-spec])
  (let [form-valid? (get @re-frame.db/app-db ::spec/form-valid?)]
    (when form-valid?
      (dispatch [::events/set-validate-form? false])
      (dispatch [::events/edit-subscription])
      (dispatch [::events/close-edit-subscription-modal]))))


(defn save-callback-add-subscription
  [form-validation-spec]
  (dispatch-sync [::events/set-validate-form? true])
  (dispatch-sync [::events/validate-subscription-form form-validation-spec])
  (let [form-valid? (get @re-frame.db/app-db ::spec/form-valid?)]
    (when form-valid?
      (dispatch [::events/set-validate-form? false])
      (dispatch [::events/edit-subscription])
      (dispatch [::events/close-edit-subscription-modal]))))


(defn save-callback-add-subscription-config
  [form-validation-spec]
  (dispatch-sync [::events/set-validate-form? true])
  (dispatch-sync [::events/validate-notification-subscription-config-form form-validation-spec])
  (let [form-valid? (get @re-frame.db/app-db ::spec/form-valid?)]
    (when form-valid?
      (dispatch [::events/set-validate-form? false])
      (dispatch [::events/edit-subscription-config])
      (dispatch [::events/reset-subscription-config-all])
      (dispatch [::events/close-add-subscription-config-modal]))))


(defn save-callback-edit-subscription-config
  [form-validation-spec]
  (dispatch-sync [::events/set-validate-form? true])
  (dispatch-sync [::events/validate-notification-subscription-config-form form-validation-spec])
  (let [form-valid? (get @re-frame.db/app-db ::spec/form-valid?)]
    (when form-valid?
      (dispatch [::events/set-validate-form? false])
      (dispatch [::events/edit-subscription-config])
      (dispatch [::events/reset-subscription-config-all])
      (dispatch [::events/close-edit-subscription-config-modal]))))


(defn SubsNotifMethodCreateButton
  []
  [ui/FormField
   [uix/Button {:text     "create new"
                :positive true
                :disabled false
                :on-click #(dispatch [::events/open-add-update-notification-method-modal {} true])}]])


(defn SubsNotifMethodDropdown
  [current-value notif-methods save? collection subs-conf-id]
  (let [tr             (subscribe [::i18n-subs/tr])
        validate-form? (subscribe [::subs/validate-form?])]
    ^{:key current-value}
    [ui/FormDropdown
     {:selection     true
      :multiple      true
      :placeholder   (@tr [:notification-methods])
      :fluid         false
      :error         (and @validate-form? (not (seq current-value)))
      :default-value current-value
      :on-change     (ui-callback/value
                       #(do
                          (dispatch-sync [::events/update-notification-subscription-config :method-ids %])
                          (when collection
                            (dispatch-sync [::events/update-notification-subscription-config :collection collection]))
                          (when (and (> (count %) 0) save?)
                            (dispatch [::events/set-notif-method-ids subs-conf-id %]))))
      :options       (doall
                       (map (fn [{id :id, method-name :name}]
                              {:key id, :value id, :text method-name})
                            @notif-methods))}]))


(defn SingleNotificationSubscription
  [{:keys [enabled resource-id method-ids] :as notif-subs} notif-methods]
  (let [method-names (str/join ", " (for [method-id method-ids]
                                      (-> (filter #(= method-id (:id %)) @notif-methods)
                                          first
                                          :name)))]
    [ui/TableRow
     [ui/TableCell {:floated :left
                    :width   2}
      [:span (if enabled "enabled" "disabled")]]
     [ui/TableCell {:floated :left
                    :width   2}
      [:span method-names]]
     [ui/TableCell {:floated :left
                    :width   9}
      [:span [uix/Link (str "api/" resource-id) resource-id]]]
     [ui/TableCell {:floated :right
                    :width   1
                    :align   :right}

      (when (general-utils/can-edit? notif-subs)
        [icons/GearIcon
         {:color    :blue
          :style    {:cursor :pointer}
          :on-click #(dispatch [::events/open-edit-subscription-modal notif-subs false])}])]]))


(defn ManageSubscriptionsModal
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        notif-methods  (subscribe [::subs/notification-methods])
        subscriptions  (subscribe [::subs/subscriptions-for-parent])
        subs-config-id (subscribe [::subs/notification-subscription-config-id])
        visible?       (subscribe [::subs/notification-subscriptions-modal-visible?])]
    (dispatch [::events/get-notification-methods])
    (dispatch [::events/get-notification-subscriptions @subs-config-id])
    (fn []
      (let [header       (@tr [:subscriptions-manage])
            current-subs (fn [sc-id ss]
                           (filter #(= @sc-id (:parent %)) @ss))]
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-notification-subscription-modal])}

         [uix/ModalHeader {:header header}]

         [ui/ModalContent {:scrolling false}
          [ui/Table {:style {:margin-top 10}}
           [ui/TableHeader
            [ui/TableRow
             [ui/TableHeaderCell {:content (str/capitalize (@tr [:status]))}]
             [ui/TableHeaderCell {:content (str/capitalize (@tr [:methods]))}]
             [ui/TableHeaderCell {:content (str/capitalize (@tr [:resource]))}]
             [ui/TableHeaderCell {:content (str/capitalize (@tr [:action]))}]]]
           [ui/TableBody
            (doall
              (for [sub (current-subs subs-config-id subscriptions)]
                ^{:key (:id sub)}
                [SingleNotificationSubscription sub notif-methods]))]]

          [ui/Menu {:borderless true
                    :secondary  true}
           (when (empty? (current-subs subs-config-id subscriptions))
             [uix/MenuItem
              {:name  (@tr [:no-subscriptions-available])
               :fixed "right"
               :text  true}])
           [components/RefreshMenu
            {:on-refresh #(dispatch [::events/get-notification-subscriptions @subs-config-id])}]]


          [utils-validation/validation-error-message ::subs/form-valid?]]]))))


(defn subs-method-dropdown
  [current-value notif-methods disabled?]
  ^{:key current-value}
  [ui/Dropdown
   {:selection     true
    :multiple      true
    :fluid         false
    :disabled      disabled?
    :default-value (if (not (nil? current-value))
                     current-value
                     (-> @notif-methods first :id))
    :on-change     (ui-callback/value
                     #(dispatch-sync [::events/update-subscription :method %]))
    :options       (map (fn [{id :id, method-name :name}]
                          {:key id, :value id, :text method-name})
                        @notif-methods)}])


(defn subs-status-dropdown
  [current-value]
  ^{:key current-value}
  [ui/Dropdown
   {:selection     true
    :fluid         false
    :default-value current-value
    :on-change     (ui-callback/value
                     #(dispatch-sync [::events/update-subscription :status %]))
    :options       (map (fn [v] {:key v, :value v, :text v}) ["enabled" "disabled"])}])


(defn EditSubscriptionModal
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        notif-methods  (subscribe [::subs/notification-methods])
        visible?       (subscribe [::subs/edit-subscription-modal-visible?])
        validate-form? (subscribe [::subs/validate-form?])
        subscription   (subscribe [::subs/subscription])
        on-change      (fn [name-kw value]
                         (dispatch [::events/update-subscription name-kw value])
                         (dispatch [::events/validate-subscription-form]))]
    (dispatch [::events/get-notification-methods])
    (fn []
      (let [header (str/capitalize (str (@tr [:edit]) " " (@tr [:subscription])))
            {:keys [name description method-ids enabled category resource-id resource-kind]} @subscription]
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-edit-subscription-modal])}


         [uix/ModalHeader {:header header}]

         [ui/ModalContent {:scrolling false}
          [utils-validation/validation-error-message ::subs/form-valid?]
          [ui/Table style/definition
           [ui/TableBody
            [uix/TableRowField "name", :editable? false, :default-value name,
             :required? false, :spec ::spec/name, :on-change (partial on-change :name),
             :validate-form? @validate-form?]
            [uix/TableRowField "description", :editable? false, :required? false,
             :default-value description, :spec ::spec/description,
             :on-change (partial on-change :description), :validate-form? @validate-form?]]
           [ui/TableRow
            [ui/TableCell {:collapsing true
                           :style      {:padding-bottom 8}} (@tr [:methods])]
            [ui/TableCell
             [subs-method-dropdown method-ids notif-methods true]]]
           [uix/TableRowField "enabled", :editable? false, :default-value (str enabled), :required? false]
           [uix/TableRowField "resource-kind", :editable? false, :default-value resource-kind, :required? false]
           [uix/TableRowField "category", :editable? false, :default-value category, :required? false]
           [uix/TableRowField "resource"
            :editable? false
            :default-value [uix/Link (str "api/" resource-id) resource-id]
            :required? false]]]

         [ui/ModalActions
          [uix/Button {:text     (@tr [:save])
                       :positive true
                       :disabled true
                       :active   true
                       :on-click #(save-callback-subscription ::spec/subscription)}]]]))))


(defn select-resources
  []
  (let [component-type (subscribe [::subs/collection])]
    [^{:key "component_type"}
     [ui/TableRow
      [ui/TableCell {:collapsing true
                     :style      {:padding-bottom 8}} "Component"]
      [ui/TableCell
       [ui/Dropdown {:selection true,
                     :fluid     true
                     :value     @component-type
                     :on-change (ui-callback/value #(dispatch [::events/set-collection %]))
                     :options   [{:key   "nuvlabox-status"
                                  :text  "NuvlaEdge Telemetry"
                                  :value "nuvlabox-status"}
                                 {:key   "infrastructure-service"
                                  :text  "Infrastructure Service"
                                  :value "infrastructure-service"}]}]]]]))


(def criteria-metric-options
  {"nuvlabox"               [{:key "cpu load" :text "CPU load %" :value utils/cpu-load}
                             {:key "ram usage" :text "RAM usage %" :value utils/ram}
                             {:key "disk usage" :text "Disk usage %" :value utils/disk}
                             {:key "state" :text "NuvlaEdge online" :value utils/state}
                             {:key "network rx" :text "Network Rx GiB" :value utils/network-rx}
                             {:key "network tx" :text "Network Tx GiB" :value utils/network-tx}
                             ]
   "infrastructure-service" [{:key "status" :text "status" :value utils/status}]
   "data-record"            [{:key "content-type" :text "content-type" :value utils/content-type}]})


(def criteria-conditions
  {:numeric (map (fn [x] {:key x :value x :text x}) [">" "<" "=" "!="])
   :boolean (map (fn [x] {:key x :value x :text x}) ["yes" "no"])
   :set     (map (fn [x] {:key x :value x :text x}) ["not equal" "equal" "in" "not in"])
   :string  (map (fn [x] {:key x :value x :text x}) ["is" "is not" "starts with" "contains" "ends with"])})


(def criteria-condition-type
  {"nuvlabox"               {:load       :numeric
                             :ram        :numeric
                             :disk       :numeric
                             :network-rx :numeric
                             :network-tx :numeric
                             :state      :boolean}
   "infrastructure-service" {:status :set}
   "data-record"            {:content-type :string}})


(def criteria-condition-options
  {"nuvlabox"               {:load       (map (fn [x] {:key x :value x :text x}) [">" "<"])
                             :ram        (map (fn [x] {:key x :value x :text x}) [">" "<"])
                             :disk       (map (fn [x] {:key x :value x :text x}) [">" "<"])
                             :network-rx (map (fn [x] {:key x :value x :text x}) [">"])
                             :network-tx (map (fn [x] {:key x :value x :text x}) [">"])
                             :state      ((get-in criteria-condition-type ["nuvlabox" :state])
                                          criteria-conditions)}
   "infrastructure-service" {:status ((get-in criteria-condition-type ["infrastructure-service" :status])
                                      criteria-conditions)}
   "data-record"            {:content-type ((get-in criteria-condition-type ["data-record" :content-type])
                                            criteria-conditions)}})


(def component-options
  [{:key "nuvlabox", :text "NuvlaEdge Telemetry", :value "nuvlabox"}
   ;; FIXME: Enable creation of IS notification configurations when IS starts producing metrics.
   #_{:key "infrastructure-service", :text "Infrastructure Service", :value "infrastructure-service"}
   {:key "data-record", :text "Data Record", :value "data-record"}])


(defn resource-tag-options
  []
  (let [resource-tags (subscribe [::subs/resource-tags-available])]
    (->> @resource-tags
         (map (fn [v] {:key   (:key v)
                       :value (:key v)
                       :text  (str (:key v) " :: " (:doc_count v))}))
         vec)))


(defn criteria-metric-kind
  [component metric-name]
  (->> [component (keyword metric-name)]
       (get-in criteria-condition-type)
       name))


(defn ResourceDropdown
  [resource metric-name _value value-options _validate-form? _on-change]
  (dispatch [::fc-events/terms-attribute resource metric-name value-options])
  (fn [_resource _metric-name value value-options validate-form? on-change]
    (let [options (map (fn [v] {:key v :text v :value v}) @value-options)]
      [ui/Dropdown {:selection      true
                    :value          value
                    :error          (and validate-form? (not (seq @value-options)))
                    :search         true
                    :name           "resource-attribute"
                    :allowAdditions true
                    :clearable      true
                    :on-change      on-change
                    :on-add-item    (ui-callback/value
                                      (fn [value] (swap! value-options #(conj @value-options value))))
                    :options        options}])))

(def metric-name->default-condition
  {utils/network-rx ">"
   utils/network-tx ">"})

(defn ConditionRow
  [metric-name condition collection on-change validate-form?]
  [ui/TableRow
   [ui/TableCell {:collapsing true
                  :style      {:padding-bottom 8}} "Condition"]
   [ui/TableCell
    [ui/Dropdown {:selection true
                  :disabled  (boolean (metric-name->default-condition metric-name))
                  :options   (vec
                               ((keyword metric-name)
                                (get criteria-condition-options collection)))
                  :error     (and validate-form? (not (seq condition)))
                  :value     (or (metric-name->default-condition metric-name) condition)
                  :on-change (ui-callback/value
                               #(on-change :criteria {:condition %}))}]]])

(def metric-name->use-other-translation-key
  {utils/disk       [:subs-notif-disk-use-other]
   utils/network-rx [:subs-notif-network-use-other]
   utils/network-tx [:subs-notif-network-use-other]})

(def metrics-with-customizable-dev-name #{utils/network-rx utils/network-tx utils/disk})

(defn- DeviceNameOptions
  [{disabled? :disabled?}]
  (let [tr                      (subscribe [::i18n-subs/tr])
        criteria                (subscribe [::subs/criteria])
        use-other-than-default? (r/atom (or (:dev-name @criteria) false))]
    (fn []
      (let [metric-name (:metric @criteria)]
        (when (metrics-with-customizable-dev-name metric-name)
          [ui/TableCell {:col-span 2
                         :class    "font-weight-400"}
           [:div {:style {:display       :flex
                          :justify-items :stretch-between
                          :align-items   :center
                          :height        40
                          :gap           24
                          :opacity       (if disabled? 0.5 1)}}
            [ui/Checkbox {:style           {:margin-right 2}
                          :label           (some->> (metric-name->use-other-translation-key metric-name) (@tr))
                          :default-checked @use-other-than-default?
                          :read-only       disabled?
                          :on-change       (ui-callback/checked
                                             (fn [checked?]
                                               (when (not checked?)
                                                 (dispatch [::events/remove-custom-name]))
                                               (reset! use-other-than-default? checked?)))}]
            (when @use-other-than-default?
              [ui/Input {:type          :text
                         :placeholder   (@tr [(keyword (str "subs-notif-name-of-" metric-name "-to-monitor"))])
                         :name          :other-disk-name
                         :read-only     disabled?
                         :default-value (or (:dev-name @criteria) "")
                         :on-change     (ui-callback/value #(dispatch [::events/update-custom-device-name %]))
                         :style         {:flex 1}}])]])))))


(defn- ResetIntervalOptions
  [{disabled? :disabled?}]
  (let [tr             (subscribe [::i18n-subs/tr])
        criteria       (subscribe [::subs/criteria])
        validate-form? (subscribe [::subs/validate-form?])]
    (fn []
      (let [reset-interval       (:reset-interval @criteria)
            monthly-reset?       (or (nil? reset-interval) (= reset-interval "month"))
            custom-reset?        (not monthly-reset?)
            start-date-of-month  (or (:reset-start-date @criteria) 1)
            custom-interval-days @(subscribe [::subs/custom-days])]
        (when (utils/metrics-with-reset-windows (:metric @criteria))
          [ui/TableCell {:col-span 2
                         :class    "font-weight-400"}
           [:div {:style {:min-height 40
                          :opacity    (if disabled? 0.5 1)}
                  :class "grid-2-cols-responsive"}
            [:div {:on-click #(dispatch (when (not disabled?) [::events/choose-monthly-reset]))
                   :style    {:display :flex :align-items :center}}
             [:input {:type      :radio
                      :name      :reset
                      :checked   monthly-reset?
                      :read-only true
                      :id        :monthly}]
             [:label {:for   :monthly
                      :style {:margin-left "0.5rem"}} (@tr [:subs-notif-reset-on-day])]
             [ui/Input {:type           :number
                        :error          (and monthly-reset?
                                             @validate-form?
                                             (not (s/valid? ::spec/reset-start-date start-date-of-month)))
                        :default-value  start-date-of-month
                        :disabled       custom-reset?
                        :read-only      disabled?
                        :min            1
                        :max            31
                        :style          {:justify-self :start
                                         :margin-left  "0.5rem"
                                         :max-width    "100px"
                                         :margin-right "90px"
                                         :opacity      (if monthly-reset? 1 0.4)}
                        :label          (@tr [:of-month])
                        :label-position :right
                        :on-change      (ui-callback/value #(dispatch [::events/update-notification-subscription-config
                                                                       :criteria
                                                                       {:reset-start-date (js/Number %)}]))}]
             [:div (ff/help-popup "min: 1, max: 31")]]
            [:div {:on-click #(dispatch (when (not disabled?) [::events/choose-custom-reset]))
                   :style    {:display :flex :align-items :center :align-self "end"}}
             [:input {:type      :radio
                      :name      :reset
                      :checked   custom-reset?
                      :read-only true
                      :id        :custom}]
             [:label {:for   :custom
                      :style {:margin-left "0.5rem"}} [:span (@tr [:subs-notif-custom-reset-after])]]
             [ui/Input {:type           :number
                        :error          (and custom-reset? @validate-form? (not (s/valid? ::spec/reset-interval reset-interval)))
                        :default-value  (or custom-interval-days 1)
                        :disabled       monthly-reset?
                        :read-only      disabled?
                        :min            1
                        :max            999
                        :style          {:justify-self :start
                                         :margin-left  "0.5rem"
                                         :max-width    "100px"
                                         :margin-right "60px"
                                         :opacity      (if custom-reset? 1 0.4)}
                        :label          (@tr [:days])
                        :label-position :right
                        :on-change      (ui-callback/value #(dispatch [::events/update-custom-days %]))}]
             [:div (ff/help-popup "min: 1, max: 999")]]]])))))

(def default-start-date 1)
(def default-custom-interval 1)

(defn- get-network-info-text [criteria tr]
  (let [start-day-of-month    (or (:reset-start-date criteria) default-start-date)
        reset-in-days         (or (:reset-in-days criteria) default-custom-interval)
        reset-interval-string (:reset-interval criteria)
        interface-text        (if
                                (str/blank? (:dev-name criteria))
                                (@tr [:subs-notif-network-info-default])
                                (@tr [:subs-notif-network-info-specific]))
        interval-text         (if (or (str/blank? reset-interval-string) (= "month" reset-interval-string))
                                (str (@tr [:subs-notif-network-reset-monthly]) " ("
                                     (@tr [:subs-notif-reset-on-day]) " "
                                     start-day-of-month ")")
                                (str (@tr [:subs-notif-network-reset-custom]) " ("
                                     (@tr [:subs-notif-custom-reset-after]) " "
                                     reset-in-days " "
                                     (@tr (if (= reset-in-days 1) [:day] [:days])) ")"))]
    (str interface-text " " interval-text)))

(defn- get-info-text [criteria tr]
  (let [metric-name (:metric criteria)]
    (case metric-name
      utils/disk (when (str/blank? (:dev-name criteria)) (@tr [:subs-notif-disk-info]))
      utils/network-tx (get-network-info-text criteria tr)
      utils/network-rx (get-network-info-text criteria tr)
      "")))


(defn- NetworkUnit [criteria]
  (when (#{"network-tx" "network-rx"} (:metric criteria)) [:span {:style {:margin-left "0.5rem"}} "GiB"]))

(defn AddSubscriptionConfigModal
  []
  (let [tr                  (subscribe [::i18n-subs/tr])
        visible?            (subscribe [::subs/subscription-config-modal-visible?])
        validate-form?      (subscribe [::subs/validate-form?])
        form-valid?         (subscribe [::subs/form-valid?])
        on-change           (fn [name-kw value]
                              (dispatch [::events/update-notification-subscription-config name-kw value]))
        notif-methods       (subscribe [::subs/notification-methods])
        collection          (subscribe [::subs/collection])
        criteria-metric     (subscribe [::subs/criteria-metric])
        components-number   (subscribe [::subs/components-number])
        metric-name         (r/atom "")
        component-option    (r/atom "")
        value-options       (r/atom "")
        subscription-config (subscribe [::subs/notification-subscription-config])]

    (dispatch [::events/get-notification-methods])
    (dispatch [::events/reset-subscription-config-all])

    (fn []
      (let [header          (str/capitalize (str (@tr [:add]) " " (@tr [:subscription])))
            criteria-metric (get-in criteria-condition-type [@collection (keyword @criteria-metric)])
            {:keys [name description criteria method-ids]} @subscription-config
            {:keys [_metric _kind condition value]} criteria]

        (when (seq @component-option) (dispatch [::cimi-events/get-resource-metadata @component-option]))
        (dispatch [::events/update-notification-subscription-config :enabled true])
        (dispatch [::events/update-notification-subscription-config :category "notification"])
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(do
                                  (dispatch [::events/reset-subscription-config-all])
                                  (dispatch [::events/close-add-subscription-config-modal]))}

         [uix/ModalHeader {:header header}]

         [ui/ModalContent {:scrolling false}
          [utils-validation/validation-error-message ::subs/form-valid?]

          [ui/Header {:as "h3"} "General"]

          [ui/Table style/definition
           [ui/TableBody
            [uix/TableRowField (@tr [:name]), :editable? true, :default-value name,
             :required? true, :spec ::spec/name, :on-change (partial on-change :name),
             :validate-form? @validate-form?]
            [uix/TableRowField (@tr [:description]), :editable? true, :default-value description,
             :required? true, :spec ::spec/description, :on-change (partial on-change :description),
             :validate-form? @validate-form?]]]

          [ui/Header {:as "h3"} "Components"
           [:span ff/nbsp ff/nbsp [ui/Label {:circular true} @components-number]]]

          [ui/Table style/definition
           [ui/TableBody
            [ui/TableRow
             [ui/TableCell {:collapsing true
                            :style      {:padding-bottom 8}} "Component"]
             [ui/TableCell
              [ui/Dropdown {:selection true
                            :error     (and @validate-form? (not (seq @component-option)))
                            :on-change (ui-callback/value
                                         #(do
                                            (reset! component-option %)
                                            (dispatch [::events/fetch-components-number %])
                                            (dispatch [::events/fetch-tags-available %])
                                            (on-change :collection %)))
                            :options   component-options}]]]
            [ui/TableRow
             [ui/TableCell {:collapsing true
                            :style      {:padding-bottom 8}} "Tag"]
             [ui/TableCell
              [ui/Dropdown {:selection true
                            :disabled  (= "data-record" @component-option)
                            :name      "tag"
                            :clearable true
                            :on-change (ui-callback/value
                                         #(do
                                            (dispatch [::events/set-components-tagged-number %])
                                            (on-change :resource-filter (if (empty? %)
                                                                          ""
                                                                          (str "tags='" % "'")))))
                            :options   (resource-tag-options)}]]]]]

          [ui/Header {:as "h3"} "Criteria"]
          [ui/Table style/definition
           [ui/TableBody
            [ui/TableRow
             [ui/TableCell {:collapsing true
                            :style      {:padding-bottom 8}} "Metric"]
             [ui/TableCell
              [:div {:style {:display     :flex
                             :align-items :center
                             :gap         "0.3rem"}}
               [ui/Dropdown {:selection true
                             :options   (get criteria-metric-options @collection)
                             :error     (and @validate-form? (not (seq @metric-name)))
                             :on-change (ui-callback/value
                                          #(do
                                             (reset! metric-name %)
                                             (on-change
                                               :criteria (if (utils/metrics-with-reset-windows %)
                                                           {:metric           %
                                                            :kind             (criteria-metric-kind @collection %)
                                                            :reset-interval   "month"
                                                            :reset-start-date 1
                                                            :condition        ">"}
                                                           {:metric %
                                                            :kind   (criteria-metric-kind @collection %)}))))}]
               [:div {:style {:white-space :normal
                              :font-size   "0.9rem"}}
                (get-info-text criteria tr)]]]]

            (case criteria-metric
              :string [:<>
                       [ConditionRow @metric-name condition @collection on-change @validate-form?]
                       [ui/TableRow
                        [ui/TableCell {:collapsing true
                                       :style      {:padding-bottom 8}} "Value"]
                        [ui/TableCell
                         (when (and (seq @component-option) (seq @metric-name))
                           [ResourceDropdown
                            @component-option
                            @metric-name
                            value
                            value-options
                            @validate-form?
                            (ui-callback/value #(on-change :criteria {:value %}))])]]]
              :set [:<>
                    [ConditionRow @metric-name condition @collection on-change @validate-form?]
                    [ui/TableRow
                     [ui/TableCell {:collapsing true
                                    :style      {:padding-bottom 8}} "Value"]
                     [ui/TableCell
                      [ui/Input
                       {:type      "text"
                        :name      "Value"
                        :error     (and @validate-form? (not (seq value)))
                        :read-only false
                        :on-change (ui-callback/value #(on-change :criteria {:value %}))}]]]]
              :numeric [:<>
                        [ConditionRow @metric-name condition @collection on-change @validate-form?]
                        [ui/TableRow
                         [ui/TableCell {:collapsing true
                                        :style      {:padding-bottom 8}} "Value"]
                         [ui/TableCell
                          [ui/Input
                           {:type        "text"
                            :name        "Value"
                            :error       (and @validate-form? (js/isNaN (js/parseInt value)))
                            :placeholder (@tr [:number])
                            :read-only   false
                            :on-change   (ui-callback/value #(on-change :criteria {:value %}))}]
                          [NetworkUnit criteria]]]]
              :boolean nil
              nil)

            [ui/TableRow
             [ResetIntervalOptions]]

            [ui/TableRow
             [DeviceNameOptions]]]
           ]

          [ui/Header {:as "h3"} "Notification"]
          [ui/Form
           [ui/FormGroup
            [SubsNotifMethodDropdown method-ids notif-methods false]
            [SubsNotifMethodCreateButton]]]]

         [ui/ModalActions
          [uix/Button {:text     (@tr [:create])
                       :positive true
                       :disabled (when-not @form-valid? true)
                       :active   true
                       :on-click #(save-callback-add-subscription-config ::spec/notification-subscription-config)}]]]))))

(defn EditSubscriptionConfigModal
  []
  (let [tr                  (subscribe [::i18n-subs/tr])
        visible?            (subscribe [::subs/edit-subscription-config-modal-visible?])
        validate-form?      (subscribe [::subs/validate-form?])
        form-valid?         (subscribe [::subs/form-valid?])
        on-change           (fn [name-kw value]
                              (dispatch [::events/update-notification-subscription-config name-kw value]))
        notif-methods       (subscribe [::subs/notification-methods])
        criteria-metric     (subscribe [::subs/criteria-metric])
        components-number   (subscribe [::subs/components-number])
        subscription-config (subscribe [::subs/notification-subscription-config])]
    (dispatch [::events/get-notification-methods])
    (dispatch [::events/reset-subscription-config-all])
    (fn []
      (let [header     (str/capitalize (str (@tr [:edit]) " " (@tr [:subscription])))
            {:keys [name description method-ids collection resource-filter criteria]} @subscription-config
            filter-tag (-> (re-find #"'(.*)'" (or resource-filter "")) last)]
        (dispatch [::events/fetch-tags-available collection])
        (dispatch [::events/set-components-tagged-number filter-tag])
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(do
                                  (dispatch [::events/reset-subscription-config-all])
                                  (dispatch [::events/close-edit-subscription-config-modal]))}

         [uix/ModalHeader {:header header}]

         [ui/ModalContent {:scrolling false}
          [utils-validation/validation-error-message ::subs/form-valid?]
          [ui/Header {:as "h3"} "General"]
          [ui/Table style/definition
           [ui/TableBody
            [uix/TableRowField (@tr [:name]), :editable? true, :default-value name,
             :required? true, :spec ::spec/name, :on-change (partial on-change :name),
             :validate-form? @validate-form?]
            [uix/TableRowField (@tr [:description]), :editable? true, :default-value description,
             :spec ::spec/description, :on-change (partial on-change :description),
             :validate-form? false]
            ]]
          [ui/Header {:as "h3"} "Components"
           [:span ff/nbsp ff/nbsp [ui/Label {:circular true} @components-number]]]

          [ui/Table style/definition
           [ui/TableBody
            [ui/TableRow
             [ui/TableCell {:collapsing true
                            :style      {:padding-bottom 8}} "Component"]
             [ui/TableCell
              [ui/Dropdown {:selection true
                            :on-change (ui-callback/value
                                         #(do
                                            (dispatch [::events/fetch-components-number %])
                                            (dispatch [::events/fetch-tags-available %])
                                            (on-change :collection %)))
                            :value     collection
                            :disabled  true
                            :options   component-options}]]]
            [ui/TableRow
             [ui/TableCell {:collapsing true
                            :style      {:padding-bottom 8}} "Tag"]
             [ui/TableCell
              [ui/Input {:value    filter-tag
                         :disabled true}]]]]]

          [ui/Header {:as "h3"} "Criteria"]
          [ui/Table style/definition
           [ui/TableBody
            [ui/TableRow
             [ui/TableCell {:collapsing true
                            :style      {:padding-bottom 8}} "Metric"]
             [ui/TableCell
              [:div {:style {:display     :flex
                             :align-items :center
                             :gap         "0.3rem"}}
               [ui/Dropdown {:selection true
                             :disabled  true
                             :value     (:metric criteria)
                             :options   (get criteria-metric-options collection)
                             :on-change (ui-callback/value
                                          #(on-change
                                             :criteria {:metric %
                                                        :kind   (criteria-metric-kind collection %)}))}]
               [:div {:style {:white-space :normal
                              :font-size   "0.9rem"}}
                (get-info-text criteria tr)]]]]

            (if-not (and (= collection "nuvlabox") (= (:metric criteria) "state"))
              [ui/TableRow
               [ui/TableCell {:collapsing true
                              :style      {:padding-bottom 8}} "Condition"]
               [ui/TableCell
                [ui/Dropdown {:selection true
                              :disabled  true
                              :value     (:condition criteria)
                              :options   (if (nil? (keyword @criteria-metric))
                                           []
                                           ((keyword @criteria-metric) (get criteria-condition-options collection)))
                              :on-change (ui-callback/value
                                           #(on-change :criteria {:condition %}))}]]]
              (on-change :criteria {:condition "no"}))

            (when-not (= :boolean (get-in criteria-condition-type [collection (keyword @criteria-metric)]))
              [ui/TableRow
               [ui/TableCell {:collapsing true
                              :style      {:padding-bottom 8}} "Value"]
               [ui/TableCell
                [ui/Input
                 {:type      "text"
                  :name      "Value"
                  :read-only false
                  :value     (:value criteria)
                  :on-change (ui-callback/value #(on-change :criteria {:value %}))}]
                [NetworkUnit criteria]]])
            [ui/TableRow
             [ResetIntervalOptions {:disabled? true}]]
            [ui/TableRow
             [DeviceNameOptions {:disabled? true}]]]]
          [ui/Header {:as "h3"} "Notification"]
          [ui/Form
           [ui/FormGroup
            [SubsNotifMethodDropdown method-ids notif-methods false]
            [SubsNotifMethodCreateButton]]]]
         [ui/ModalActions
          [uix/Button {:text     (@tr [:save])
                       :positive true
                       :disabled (when-not @form-valid? true)
                       :active   true
                       :on-click #(save-callback-edit-subscription-config
                                    ::spec/notification-subscription-config)}]]]))))


(defn notif-method-select-dropdown
  [_method _on-change]
  (let [tr              (subscribe [::i18n-subs/tr])
        local-validate? (r/atom false)
        validate-form?  (subscribe [::subs/validate-form?])]
    (fn [method on-change]
      (let [validate? (or @local-validate? @validate-form?)
            valid?    (s/valid? ::spec/method method)]
        [ui/TableRow
         [ui/TableCell {:collapsing true
                        :style      {:padding-bottom 8}}
          (general-utils/mandatory-name (@tr [:method]))]
         [ui/TableCell {:error (and validate? (not valid?))}
          [ui/Dropdown {:selection true
                        :fluid     true
                        :value     method
                        :on-change (ui-callback/value
                                     #(do
                                        (reset! local-validate? true)
                                        (dispatch [::events/method (str/lower-case %)])
                                        (on-change :method (str/lower-case %))))
                        :options   [{:key "slack", :text "Slack", :value "slack"}
                                    {:key "email", :text "Email", :value "email"}]}]]]))))


(defn AddNotificationMethodModal
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        visible?       (subscribe [::subs/notification-method-modal-visible?])
        validate-form? (subscribe [::subs/validate-form?])
        form-valid?    (subscribe [::subs/form-valid?])
        notif-method   (subscribe [::subs/notification-method])
        on-change      (fn [name-kw value]
                         (dispatch [::events/update-notification-method name-kw value])
                         (dispatch [::events/validate-notification-method-form]))
        is-new?        (subscribe [::subs/is-new?])]
    (fn []
      (let [editable? (general-utils/editable? @notif-method @is-new?)
            header    (str/capitalize (str (if (true? @is-new?) (@tr [:new]) (@tr [:update])) " " (@tr [:notification-method])))
            {:keys [name description method destination]} @notif-method]
        [:div]
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-add-update-notification-method-modal])}

         [uix/ModalHeader {:header header}]

         [ui/ModalContent {:scrolling false}
          [utils-validation/validation-error-message ::subs/form-valid?]
          [ui/Table style/definition
           [ui/TableBody
            [uix/TableRowField (@tr [:name]), :editable? editable?, :default-value name,
             :required? true, :spec ::spec/name, :on-change (partial on-change :name),
             :validate-form? @validate-form?]
            [uix/TableRowField (@tr [:description]), :editable? editable?, :required? true,
             :default-value description, :spec ::spec/description,
             :on-change (partial on-change :description), :validate-form? @validate-form?]
            [notif-method-select-dropdown method on-change]
            [uix/TableRowField (@tr [:destination]), :editable? editable?, :required? true,
             :default-value destination, :spec ::spec/destination,
             :on-change (partial on-change :destination), :validate-form? @validate-form?,
             :input-help-msg (case (:method @notif-method)
                               "slack" [:a {:href "https://api.slack.com/messaging/webhooks" :target "_blank"} "Slack webhook help page"]
                               "email" "Space separated list of email addresses"
                               "")]]]]

         [ui/ModalActions
          [uix/Button {:text     (if (true? @is-new?) (@tr [:create]) (@tr [:save]))
                       :positive true
                       :disabled (when-not @form-valid? true)
                       :active   true
                       :on-click #(save-callback-notification-method ::spec/notification-method)}]]]))))


(defn MenuBarSubscription
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [components/StickyBar
     [ui/Menu {:borderless true}
      [uix/MenuItem
       {:name     (@tr [:add])
        :icon     "add"
        :on-click #(dispatch [::events/open-add-subscription-config-modal {}])}]
      [components/RefreshMenu
       {:on-refresh #(do
                       (dispatch-sync [::events/get-notification-subscriptions])
                       (dispatch [::events/get-notification-subscription-configs]))}]]]))


(defn MenuBarNotificationMethod
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [components/StickyBar
     [ui/Menu {:borderless true}
      [uix/MenuItem
       {:name     (@tr [:add])
        :icon     "add"
        :on-click #(dispatch [::events/open-add-update-notification-method-modal {} true])}]
      [components/RefreshMenu
       {:on-refresh #(dispatch [::events/get-notification-methods])}]]]))


(defn DeleteButtonNotificationMethod
  [notif-method]
  (let [tr      (subscribe [::i18n-subs/tr])
        {:keys [id name description]} notif-method
        content (str (or name id) (when description " - ") description)]
    [uix/ModalDanger
     {:on-confirm  #(dispatch [::events/delete-notification-method id])
      :trigger     (r/as-element [ui/Icon {:class icons/i-trash-full
                                           :style {:cursor "pointer"}
                                           :color "red"}])
      :content     [:h3 content]
      :header      (@tr [:delete-notification-method])
      :danger-msg  (@tr [:notification-method-delete-warning])
      :button-text (@tr [:delete])}]))


(defn single-notification-method
  [{:keys [method name description destination] :as notif-method}]
  [ui/TableRow
   [ui/TableCell {:floated :left}
    [:span name]]
   [ui/TableCell {:floated :left}
    [:span description]]
   [ui/TableCell {:floated :left}
    [:span method]]
   [ui/TableCell {:floated :left
                  :style   {:overflow      "hidden"
                            :text-overflow "ellipsis"
                            :max-width     "20ch"}}
    [:span destination]]
   [ui/TableCell {:floated :right
                  :width   1
                  :align   :right}
    (when (general-utils/can-delete? notif-method)
      [DeleteButtonNotificationMethod notif-method])
    (when (general-utils/can-edit? notif-method)
      [icons/GearIcon {:color    :blue
                       :style    {:cursor :pointer}
                       :on-click #(dispatch [::events/open-add-update-notification-method-modal notif-method false])}])]])


(defn DeleteButtonSubscriptionConfig
  [sub]
  (let [tr      (subscribe [::i18n-subs/tr])
        {:keys [id name description]} sub
        content (str (or name id) (when description " - ") description)]
    [uix/ModalDanger
     {:on-confirm  #(dispatch [::events/delete-subscription-config id])
      :trigger     (r/as-element [ui/Icon {:class icons/i-trash-full
                                           :style {:cursor "pointer"}
                                           :color "red"}])
      :content     [:h3 content]
      :header      (@tr [:delete-notification-subscription-conf])
      :danger-msg  (@tr [:notification-subscription-conf-delete-warning])
      :button-text (@tr [:delete])}]))


(defn subscription-configs-table-header
  [tr]
  [ui/TableHeader
   {:style {:font-weight 600}}
   [ui/TableRow
    [ui/TableCell [:span (str/capitalize (@tr [:name]))]
     [:span ff/nbsp (ff/help-popup (@tr [:subscription-name]))]]
    [ui/TableCell [:span (str/capitalize (@tr [:criteria]))]
     [:span ff/nbsp (ff/help-popup (@tr [:criteria-for-notifications]))]]
    [ui/TableCell
     [:span (str/capitalize (@tr [:enable]))]
     [:span ff/nbsp (ff/help-popup (@tr [:notifications-enable-disable-help]))]]
    [ui/TableCell
     [:span (str/capitalize (@tr [:notification-methods]))]
     [:span ff/nbsp (ff/help-popup (@tr [:notifications-methods-help]))]]
    [ui/TableCell
     [:span (str/capitalize "action")]]]])


(defn criteria-popup
  [subs-conf]
  (let [{:keys [metric condition kind value]} (:criteria subs-conf)]
    (r/as-element
      [:span (str metric " ")
       [:span {:style {:font-weight "bold"}} condition]
       (when-not (= "boolean" kind) (str " " value))])))


(defn beautify-name
  "Transform kebab names into spaced camel case, with special exceptions"
  [name]
  (case name
    "nuvlabox" "NuvlaEdge"
    (str/join " " (map str/capitalize (str/split name #"-")))))


(defn TabSubscriptions
  []
  (let [tr                   (subscribe [::i18n-subs/tr])
        subscription-configs (subscribe [::subs/notification-subscription-configs-grouped])
        notif-methods        (subscribe [::subs/notification-methods])
        on-change            (fn [name-kw value]
                               (dispatch [::events/update-notification-subscription-config name-kw value]))]
    (dispatch [::events/get-notification-subscription-configs])
    (dispatch-sync [::events/get-notification-subscriptions])
    (dispatch [::events/get-notification-methods])
    (fn []
      (let [grouped-subscriptions @subscription-configs]
        [ui/TabPane
         [MenuBarSubscription]
         (if (empty? @subscription-configs)
           [ui/Message (str/capitalize (@tr [:no-subscription-configs-defined]))]
           (doall
             (for [[resource-kind resource-subs-confs] grouped-subscriptions]
               (when-not (empty? resource-subs-confs)
                 ^{:key resource-kind}
                 [uix/Accordion
                  [ui/Table {:basic   "very"
                             :compact true
                             :striped true
                             :style   {:margin-top 10}}

                   [subscription-configs-table-header tr]

                   [ui/TableBody
                    (for [subs-conf resource-subs-confs]
                      ^{:key subs-conf}
                      [ui/TableRow
                       [ui/TableCell {:floated :left
                                      :width   2}
                        [:span (:name subs-conf)]]
                       [ui/TableCell {:width 2} (criteria-popup subs-conf)]
                       [ui/TableCell {:floated :left
                                      :width   2}
                        [:span
                         [ui/Checkbox {:key             "enable-new"
                                       :disabled        (empty? @notif-methods)
                                       :default-checked (:enabled subs-conf)
                                       :style           {:margin "1em"}
                                       :on-change       (ui-callback/checked
                                                          #(do
                                                             (dispatch-sync [::events/set-notification-subscription-config subs-conf])
                                                             (on-change :collection (:resource-kind subs-conf))
                                                             (on-change :enabled %)
                                                             (when (= 1 (count @notif-methods))
                                                               (on-change :method-id (-> @notif-methods
                                                                                         first
                                                                                         :id)))
                                                             (dispatch [::events/toggle-enabled (:id subs-conf) %])))}]]]
                       [ui/TableCell {:floated :left
                                      :width   4}
                        [SubsNotifMethodDropdown
                         (:method-ids subs-conf) notif-methods true (:resource-kind subs-conf) (:id subs-conf)]]
                       [ui/TableCell {:floated :right
                                      :width   1
                                      :align   :right}
                        (when (general-utils/can-delete? subs-conf)
                          [DeleteButtonSubscriptionConfig subs-conf])
                        (when (general-utils/can-edit? subs-conf)
                          [icons/GearIcon {:color    :blue
                                           :style    {:cursor :pointer}
                                           :on-click #(dispatch [::events/open-edit-subscription-config-modal subs-conf])}])]])]]
                  :title-size :h4
                  :default-open true
                  :count (count resource-subs-confs)
                  :label (beautify-name resource-kind)]))))]))))


(defn TabMethods
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        all-methods (subscribe [::subs/notification-methods])]
    (fn []
      [ui/TabPane
       [MenuBarNotificationMethod]
       (if (empty? @all-methods)
         [ui/Message
          (str/capitalize (@tr [:no-notification-method-defined]))]
         [:div [ui/Table {:style {:margin-top 10}}
                [ui/TableHeader
                 [ui/TableRow
                  [ui/TableHeaderCell {:content (str/capitalize (@tr [:name]))}]
                  [ui/TableHeaderCell {:content (str/capitalize (@tr [:description]))}]
                  [ui/TableHeaderCell {:content (str/capitalize (@tr [:method]))}]
                  [ui/TableHeaderCell {:content (str/capitalize (@tr [:destination]))}]
                  [ui/TableHeaderCell {:content (str/capitalize (@tr [:action]))}]]]
                [ui/TableBody
                 (for [notif-method @all-methods]
                   ^{:key (:id notif-method)}
                   [single-notification-method notif-method])]]])])))


(defn tabs
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [{:menuItem {:content (str/capitalize (@tr [:subscriptions]))
                 :key     :subscriptions
                 :icon    "list alternate outline"}
      :render   #(r/as-element [TabSubscriptions])}
     {:menuItem {:content (str/capitalize (@tr [:methods]))
                 :key     :methods
                 :icon    "at"}
      :render   #(r/as-element [TabMethods])}]))


(defn TabsAll
  []
  (dispatch [::events/get-notification-methods])
  (fn []
    [components/LoadingPage {}
     [ui/Tab
      {:menu  {:secondary true
               :pointing  true
               :style     {:display        "flex"
                           :flex-direction "row"
                           :flex-wrap      "wrap"}}
       :panes (tabs)}]]))


(defn notifications-view
  [_path]
  (timbre/set-level! :info)
  [:<>
   [TabsAll]
   [EditSubscriptionModal]
   [AddNotificationMethodModal]
   [AddSubscriptionConfigModal]
   [EditSubscriptionConfigModal]
   [ManageSubscriptionsModal]])
