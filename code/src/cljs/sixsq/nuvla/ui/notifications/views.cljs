(ns sixsq.nuvla.ui.notifications.views
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch dispatch-sync subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.history.views :as history]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.notifications.events :as events]
    [sixsq.nuvla.ui.notifications.spec :as spec]
    [sixsq.nuvla.ui.notifications.subs :as subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.form-fields :as ff]
    [sixsq.nuvla.ui.utils.general :as general-utils]
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


(defn subs-notif-method-create-button
  []
  [ui/FormField
   [uix/Button {:text     "create new"
                :positive true
                :disabled false
                :on-click #(dispatch [::events/open-add-update-notification-method-modal {} true])}]])


(defn subs-notif-method-dropdown
  [current-value notif-methods save? collection subs-conf-id]
  ^{:key current-value}
  [ui/FormDropdown
   {:selection     true
    :fluid         false
    :default-value (if (not (nil? current-value))
                     current-value
                     (-> @notif-methods first :id))
    :on-change     (ui-callback/value
                     #(do
                        (dispatch-sync [::events/update-notification-subscription-config :method-id %])
                        (if collection
                          (dispatch-sync [::events/update-notification-subscription-config :collection collection]))
                        (if save?
                          (dispatch [::events/set-notif-method-id subs-conf-id %]))))
    :options       (map (fn [{id :id, method-name :name}]
                          {:key id, :value id, :text method-name})
                        @notif-methods)}])


(defn subs-notif-method-select-or-add
  [current-method notif-methods save?]
  (fn [current-method notif-methods]
    [:<>
     [subs-notif-method-dropdown current-method notif-methods save? nil]
     [subs-notif-method-create-button]]))


(defn DeleteButtonSubscriptions
  [sub]
  (let [tr      (subscribe [::i18n-subs/tr])
        {:keys [id name description]} sub
        content (str (or name id) (when description " - ") description)]
    [uix/ModalDanger
     {:on-confirm  #(dispatch [::events/delete-subscription id])
      :trigger     (r/as-element [ui/Icon {:name  "trash"
                                           :style {:cursor "pointer"}
                                           :color "red"}])
      :content     [:h3 content]
      :header      (@tr [:delete-notification-subscription])
      :danger-msg  (@tr [:notification-subscription-delete-warning])
      :button-text (@tr [:delete])}]))


(defn single-notification-subscription
  [{:keys [enabled resource-id method-id] :as notif-subs} notif-methods]
  (let [method-name (-> (filter #(= method-id (:id %)) @notif-methods)
                        first
                        :name)]
    [ui/TableRow
     [ui/TableCell {:floated :left
                    :width   2}
      [:span (if enabled "enabled" "disabled")]]
     [ui/TableCell {:floated :left
                    :width   2}
      [:span method-name]]
     [ui/TableCell {:floated :left
                    :width   9}
      [:span [history/link (str "api/" resource-id) resource-id]]]
     [ui/TableCell {:floated :right
                    :width   1
                    :align   :right}

      (when (general-utils/can-edit? notif-subs)
        [ui/Icon {:name     :cog
                  :color    :blue
                  :style    {:cursor :pointer}
                  :on-click #(dispatch [::events/open-edit-subscription-modal notif-subs false])}])]]))


(defn manage-subscriptions-modal
  []
  (let [tr            (subscribe [::i18n-subs/tr])
        notif-methods (subscribe [::subs/notification-methods])
        subscriptions (subscribe [::subs/notification-subscriptions])
        visible?      (subscribe [::subs/notification-subscriptions-modal-visible?])]
    (dispatch [::events/get-notification-methods])
    (dispatch [::events/get-notification-subscriptions])
    (fn []
      (let [header (@tr [:subscriptions-manage])]
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-notification-subscription-modal])}

         [uix/ModalHeader {:header header}]

         [ui/ModalContent {:scrolling false}
          [ui/Table {:style {:margin-top 10}}
           [ui/TableHeader
            [ui/TableRow
             [ui/TableHeaderCell {:content (str/capitalize (@tr [:status]))}]
             [ui/TableHeaderCell {:content (str/capitalize (@tr [:method]))}]
             [ui/TableHeaderCell {:content (str/capitalize (@tr [:resource]))}]
             [ui/TableHeaderCell {:content (str/capitalize (@tr [:action]))}]]]
           [ui/TableBody
            (for [sub @subscriptions]
              ^{:key (:id sub)}
              [single-notification-subscription sub notif-methods])]]
          [utils-validation/validation-error-message ::subs/form-valid?]]]))))


(defn subs-method-dropdown
  [current-value notif-methods disabled?]
  ^{:key current-value}
  [ui/Dropdown
   {:selection     true
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


(defn edit-subscription-modal
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
            {:keys [name description method-id enabled category resource-id resource-kind]} @subscription]
        [:div]
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
                           :style      {:padding-bottom 8}} "method"]
            [ui/TableCell
             [subs-method-dropdown method-id notif-methods true]]]
           [uix/TableRowField "enabled", :editable? false, :default-value (str enabled), :required? false]
           [uix/TableRowField "resource-kind", :editable? false, :default-value resource-kind, :required? false]
           [uix/TableRowField "category", :editable? false, :default-value category, :required? false]
           [uix/TableRowField "resource"
            :editable? false
            :default-value [history/link (str "api/" resource-id) resource-id]
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
                     :options   [{:key "nuvlabox-status", :text "NuvlaBox Telemetry", :value "nuvlabox-status"}
                                 {:key "infrastructure-service", :text "Infrastructure Service", :value "infrastructure-service"}]}]
       ]]]))

(def criteria-metric-options
  {"nuvlabox"               [{:key "cpu load" :text "CPU load %" :value "load"}
                             {:key "ram usage" :text "RAM usage %" :value "ram"}
                             {:key "disk usage" :text "Disk usage %" :value "disk"}
                             {:key "state" :text "NB online" :value "state"}]
   "infrastructure-service" [{:key "status" :text "status" :value "status"}]})

(def criteria-conditions
  {:numeric (map (fn [x] {:key x :value x :text x}) [">" "<" "=" "!="])
   :boolean (map (fn [x] {:key x :value x :text x}) ["yes" "no"])
   :set     (map (fn [x] {:key x :value x :text x}) ["not equal" "equal" "in" "not in"])})

(def criteria-condition-type
  {"nuvlabox"               {:load  :numeric
                             :ram   :numeric
                             :disk  :numeric
                             :state :boolean}
   "infrastructure-service" {:status :set}})

(def criteria-condition-options
  {"nuvlabox"               {:load  ((get-in criteria-condition-type ["nuvlabox" :load]) criteria-conditions)
                             :ram   ((get-in criteria-condition-type ["nuvlabox" :ram]) criteria-conditions)
                             :disk  ((get-in criteria-condition-type ["nuvlabox" :disk]) criteria-conditions)
                             :state ((get-in criteria-condition-type ["nuvlabox" :state]) criteria-conditions)}
   "infrastructure-service" {:status ((get-in criteria-condition-type ["infrastructure-service" :status]) criteria-conditions)}})

(defn get-criteria-condition-options
  []
  (let [component (subscribe [::subs/collection])
        metric    (subscribe [::subs/criteria-metric])]
    ((keyword @metric) (get criteria-condition-options @component))))

(def component-options
  [{:key "nuvlabox", :text "NuvlaBox Telemetry", :value "nuvlabox"}
   {:key "infrastructure-service", :text "Infrastructure Service", :value "infrastructure-service"}])

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

(defn add-subscription-config-modal
  []
  (let [tr                (subscribe [::i18n-subs/tr])
        visible?          (subscribe [::subs/subscription-config-modal-visible?])
        validate-form?    (subscribe [::subs/validate-form?])
        form-valid?       (subscribe [::subs/form-valid?])
        on-change         (fn [name-kw value]
                            (dispatch [::events/update-notification-subscription-config name-kw value])
                            (dispatch [::events/validate-notification-subscription-config-form]))
        notif-methods     (subscribe [::subs/notification-methods])
        collection        (subscribe [::subs/collection])
        criteria-metric   (subscribe [::subs/criteria-metric])
        components-number (subscribe [::subs/components-number])]
    (dispatch [::events/get-notification-methods])
    (dispatch [::events/set-components-number 0])
    (dispatch [::events/reset-tags-available])
    (fn []
      (let [header (str/capitalize (str (@tr [:add]) " " (@tr [:subscription])))]
        (dispatch [::events/update-notification-subscription-config :enabled true])
        (dispatch [::events/update-notification-subscription-config :category "notification"])
        (dispatch [::events/update-notification-subscription-config :resource-filter ""])
        [:div]
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
            [uix/TableRowField (@tr [:description]), :editable? true, :default-value name,
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
                            :options   component-options}]]]
            [ui/TableRow
             [ui/TableCell {:collapsing true
                            :style      {:padding-bottom 8}} "Tag"]
             [ui/TableCell
              [ui/Dropdown {:selection true
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
              [ui/Dropdown {:selection true
                            :options   (get criteria-metric-options @collection)
                            :on-change (ui-callback/value
                                         #(on-change
                                            :criteria {:metric %
                                                       :kind   (criteria-metric-kind @collection %)}))}]]]
            [ui/TableRow
             [ui/TableCell {:collapsing true
                            :style      {:padding-bottom 8}} "Condition"]
             [ui/TableCell
              [ui/Dropdown {:selection true
                            :options   (if (nil? (keyword @criteria-metric))
                                         []
                                         ((keyword @criteria-metric) (get criteria-condition-options @collection)))
                            :on-change (ui-callback/value #(on-change :criteria {:condition %}))}]]]

            (if-not (= :boolean (get-in criteria-condition-type [@collection (keyword @criteria-metric)]))
              [ui/TableRow
               [ui/TableCell {:collapsing true
                              :style      {:padding-bottom 8}} "Value"]
               [ui/TableCell
                [ui/Input
                 {:type      "text"
                  :name      "Value"
                  :read-only false
                  :on-change (ui-callback/value #(on-change :criteria {:value %}))}]]]
              )]]
          [ui/Header {:as "h3"} "Notification"]
          [ui/Form
           [ui/FormGroup
            [subs-notif-method-dropdown "" notif-methods false]
            [subs-notif-method-create-button]]]]
         [ui/ModalActions
          [uix/Button {:text     (@tr [:create])
                       :positive true
                       :disabled (when-not @form-valid? true)
                       :active   true
                       :on-click #(save-callback-add-subscription-config ::spec/notification-subscription-config)}]]]))))


(defn edit-subscription-config-modal
  []
  (let [tr                  (subscribe [::i18n-subs/tr])
        visible?            (subscribe [::subs/edit-subscription-config-modal-visible?])
        validate-form?      (subscribe [::subs/validate-form?])
        form-valid?         (subscribe [::subs/form-valid?])
        on-change           (fn [name-kw value]
                              (dispatch [::events/update-notification-subscription-config name-kw value])
                              (dispatch [::events/validate-notification-subscription-config-form]))
        notif-methods       (subscribe [::subs/notification-methods])
        criteria-metric     (subscribe [::subs/criteria-metric])
        components-number   (subscribe [::subs/components-number])
        subscription-config (subscribe [::subs/notification-subscription-config])]
    (dispatch [::events/get-notification-methods])
    (dispatch [::events/reset-tags-available])
    (fn []
      (let [header     (str/capitalize (str (@tr [:edit]) " " (@tr [:subscription])))
            {:keys [name description method-id collection resource-filter criteria]} @subscription-config
            filter-tag (last (str/split (str/replace (or resource-filter "") #"'" "") #"="))]
        (dispatch [::events/fetch-tags-available collection])
        [:div]
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
              [ui/Dropdown {:selection true
                            :name      "tag"
                            :clearable true
                            :on-change (ui-callback/value
                                         #(do
                                            (dispatch [::events/set-components-tagged-number %])
                                            (on-change :resource-filter (if (empty? %)
                                                                          ""
                                                                          (str "tags='" % "'")))))
                            :value     filter-tag
                            :disabled  true
                            :options   (resource-tag-options)}]]]]]

          [ui/Header {:as "h3"} "Criteria"]
          [ui/Table style/definition
           [ui/TableBody
            [ui/TableRow
             [ui/TableCell {:collapsing true
                            :style      {:padding-bottom 8}} "Metric"]
             [ui/TableCell
              [ui/Dropdown {:selection true
                            :value     (:metric criteria)
                            :options   (get criteria-metric-options collection)
                            :on-change (ui-callback/value
                                         #(on-change
                                            :criteria {:metric %
                                                       :kind   (criteria-metric-kind collection %)}))}]]]
            [ui/TableRow
             [ui/TableCell {:collapsing true
                            :style      {:padding-bottom 8}} "Condition"]
             [ui/TableCell
              [ui/Dropdown {:selection true
                            :value     (:condition criteria)
                            :options   (if (nil? (keyword @criteria-metric))
                                         []
                                         ((keyword @criteria-metric) (get criteria-condition-options collection)))
                            :on-change (ui-callback/value #(on-change :criteria {:condition %}))}]]]

            (if-not (= :boolean (get-in criteria-condition-type [collection (keyword @criteria-metric)]))
              [ui/TableRow
               [ui/TableCell {:collapsing true
                              :style      {:padding-bottom 8}} "Value"]
               [ui/TableCell
                [ui/Input
                 {:type      "text"
                  :name      "Value"
                  :read-only false
                  :value     (:value criteria)
                  :on-change (ui-callback/value #(on-change :criteria {:value %}))}]]]
              )]]
          [ui/Header {:as "h3"} "Notification"]
          [subs-notif-method-dropdown method-id notif-methods false]
          [:span ff/nbsp ff/nbsp]
          [subs-notif-method-create-button]]
         [ui/ModalActions
          [uix/Button {:text     (@tr [:save])
                       :positive true
                       :disabled (when-not @form-valid? true)
                       :active   true
                       :on-click #(save-callback-edit-subscription-config ::spec/notification-subscription-config)}]]]
        ))))

(defn notif-method-select-dropdown
  [method on-change]
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

(defn add-notification-method-modal
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
    [main-components/StickyBar
     [ui/Menu {:borderless true}
      [uix/MenuItem
       {:name     (@tr [:add])
        :icon     "add"
        :on-click #(dispatch [::events/open-add-subscription-config-modal {}])}]]]))


(defn MenuBarNotificationMethod
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [main-components/StickyBar
     [ui/Menu {:borderless true}
      [uix/MenuItem
       {:name     (@tr [:add])
        :icon     "add"
        :on-click #(dispatch [::events/open-add-update-notification-method-modal {} true])}]
      [main-components/RefreshMenu
       {:on-refresh #(dispatch [::events/get-notification-methods])}]]]))


(defn DeleteButtonNotificationMethod
  [notif-method]
  (let [tr      (subscribe [::i18n-subs/tr])
        {:keys [id name description]} notif-method
        content (str (or name id) (when description " - ") description)]
    [uix/ModalDanger
     {:on-confirm  #(dispatch [::events/delete-notification-method id])
      :trigger     (r/as-element [ui/Icon {:name  "trash"
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
      [ui/Icon {:name     :cog
                :color    :blue
                :style    {:cursor :pointer}
                :on-click #(dispatch [::events/open-add-update-notification-method-modal notif-method false])}])]])


(defn DeleteButtonSubscriptionConfig
  [sub]
  (let [tr      (subscribe [::i18n-subs/tr])
        {:keys [id name description]} sub
        content (str (or name id) (when description " - ") description)]
    [uix/ModalDanger
     {:on-confirm  #(dispatch [::events/delete-subscription-config id])
      :trigger     (r/as-element [ui/Icon {:name  "trash"
                                           :style {:cursor "pointer"}
                                           :color "red"}])
      :content     [:h3 content]
      :header      (@tr [:delete-notification-subscription-conf])
      :danger-msg  (@tr [:notification-subscription-conf-delete-warning])
      :button-text (@tr [:delete])}]))


(defn subscription-configs-table-header
  [tr]
  [ui/TableHeader
   [ui/TableRow
    [ui/TableCell {:content ""}]
    [ui/TableCell
     [:span (str/capitalize (@tr [:enable]))]
     [:span ff/nbsp (ff/help-popup (@tr [:notifications-enable-disable-help]))]]
    [ui/TableCell
     [:span (str/capitalize (@tr [:notification-method]))]
     [:span ff/nbsp (ff/help-popup (@tr [:notifications-method-help]))]]
    [ui/TableCell
     [:span (str/capitalize (@tr [:subscriptions]))]
     [:span ff/nbsp (ff/help-popup (@tr [:subscriptions-manage-help]))]]
    [ui/TableCell
     [:span (str/capitalize "action")]]]])


(def resource-to-collection-names
  {"infrastructure-service" "Infrastructure Service"
   "nuvlabox"               "NuvlaBox"})

(defn criteria-popup
  [subs-conf]
  (let [collection (:collection subs-conf)
        {:keys [metric condition kind value]} (:criteria subs-conf)]
    (r/as-element
      [:span "criteria: " metric " "
       [:span {:style {:font-weight "bold"}} condition]
       (when-not (= "boolean" kind) (str " " value))])
    ))

(defn TabSubscriptions
  []
  (let [tr                   (subscribe [::i18n-subs/tr])
        subscription-configs (subscribe [::subs/notification-subscription-configs])
        notif-methods        (subscribe [::subs/notification-methods])
        subscriptions        (subscribe [::subs/subscriptions])
        on-change            (fn [name-kw value]
                               (dispatch-sync [::events/update-notification-subscription-config name-kw value])
                               (dispatch [::events/validate-notification-subscription-config-form]))]
    (dispatch [::events/get-notification-subscription-configs])
    (dispatch [::events/get-notification-subscriptions])
    (dispatch [::events/get-notification-methods])
    (fn []
      (let [infra-service-subs-confs (filter #(= "infrastructure-service" (:resource-kind %)) @subscription-configs)
            nuvlabox-subs-confs      (filter #(= "nuvlabox" (:resource-kind %)) @subscription-configs)
            subs-confs-all           {"infrastructure-service" infra-service-subs-confs
                                      "nuvlabox"               nuvlabox-subs-confs}]
        [ui/TabPane
         [MenuBarSubscription]
         (if (empty? @subscription-configs)
           [ui/Message (str/capitalize (@tr [:no-subscription-configs-defined]))]
           (for [[resource-kind resource-subs-confs] subs-confs-all]
             (if-not (empty? resource-subs-confs)
               ^{:key resource-kind}
               [uix/Accordion
                [ui/Table {:basic   "very"
                           :compact true
                           :style   {:margin-top 10}}

                 [subscription-configs-table-header tr]

                 [ui/TableBody
                  (for [subs-conf resource-subs-confs]
                    ^{:key subs-conf}
                    [ui/TableRow
                     [ui/TableCell {:floated :left
                                    :width   2}
                      [:span (:name subs-conf)]
                      [:span ff/nbsp (ff/help-popup (criteria-popup subs-conf))]]
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
                                                           (if (= 1 (count @notif-methods))
                                                             (on-change :method-id (-> @notif-methods
                                                                                       first
                                                                                       :id)))
                                                           (dispatch [::events/toggle-enabled (:id subs-conf) %])))}]]]
                     [ui/TableCell {:floated :left
                                    :width   4}
                      [subs-notif-method-dropdown
                       (:method-id subs-conf) notif-methods true (:resource-kind subs-conf) (:id subs-conf)]]
                     [ui/TableCell {:floated :left
                                    :width   1
                                    :align   :right}
                      (let [subscrs (filter #(= (:id subs-conf) (:parent %)) @subscriptions)]
                        [uix/Button {:text     (@tr [:manage])
                                     :positive true
                                     :disabled (empty? subscrs)
                                     :active   true
                                     :on-click #(dispatch [::events/open-notification-subscription-modal subscrs])}])
                      ]
                     [ui/TableCell {:floated :right
                                    :width   1
                                    :align   :right}
                      (when (general-utils/can-delete? subs-conf)
                        [DeleteButtonSubscriptionConfig subs-conf])
                      (when (general-utils/can-edit? subs-conf)
                        [ui/Icon {:name     :cog
                                  :color    :blue
                                  :style    {:cursor :pointer}
                                  :on-click #(dispatch [::events/open-edit-subscription-config-modal subs-conf])}])]])
                  ]]
                #_[:<> [:divNB RAM usage]]
                :title-size :h4
                :default-open false
                :count (count resource-subs-confs)
                :label (get resource-to-collection-names resource-kind)]
               )))
         ]))))


(defn TabMethods
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        all-methods (subscribe [::subs/notification-methods])]
    (dispatch [::events/get-notification-methods])
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
  [tr]
  [{:menuItem {:content (str/capitalize (@tr [:subscriptions]))
               :key     "subscriptions"
               :icon    "list alternate outline"}
    :render   (fn [] (r/as-element [TabSubscriptions]))}
   {:menuItem {:content (str/capitalize (@tr [:methods]))
               :key     "methods"
               :icon    "at"}
    :render   (fn [] (r/as-element [TabMethods]))}])


(defn TabsAll
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Tab
     {:menu  {:secondary true
              :pointing  true
              :style     {:display        "flex"
                          :flex-direction "row"
                          :flex-wrap      "wrap"}}
      :panes (tabs tr)}]))


(defmethod panel/render :notifications
  [path]
  (timbre/set-level! :info)
  [:<>
   [TabsAll]
   [edit-subscription-modal]
   [add-notification-method-modal]
   [add-subscription-config-modal]
   [edit-subscription-config-modal]
   [manage-subscriptions-modal]])
