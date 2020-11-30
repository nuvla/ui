(ns sixsq.nuvla.ui.notifications.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch dispatch-sync subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.history.views :as history]
    [sixsq.nuvla.ui.notifications.events :as events]
    [sixsq.nuvla.ui.notifications.spec :as spec]
    [sixsq.nuvla.ui.notifications.subs :as subs]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
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
  [form-validation-spec]
  (dispatch-sync [::events/set-validate-form? true])
  (dispatch-sync [::events/validate-notification-subscription-config-form form-validation-spec])
  (let [form-valid? (get @re-frame.db/app-db ::spec/form-valid?)]
    (when form-valid?
      (do
        (dispatch [::events/set-validate-form? false])
        (dispatch [::events/edit-subscription-config])))))


(defn save-callback-notification-method
  [form-validation-spec]
  (dispatch-sync [::events/set-validate-form? true])
  (dispatch-sync [::events/validate-notification-method-form form-validation-spec])
  (let [form-valid? (get @re-frame.db/app-db ::spec/form-valid?)]
    (when form-valid?
      (do
        (dispatch [::events/set-validate-form? false])
        (dispatch [::events/edit-notification-method])))))


(defn save-callback-subscription
  [form-validation-spec]
  (dispatch-sync [::events/set-validate-form? true])
  (dispatch-sync [::events/validate-subscription-form form-validation-spec])
  (let [form-valid? (get @re-frame.db/app-db ::spec/form-valid?)]
    (when form-valid?
      (do
        (dispatch [::events/set-validate-form? false])
        (dispatch [::events/edit-subscription])
        (dispatch [::events/close-edit-subscription-modal])))))


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
  [{:keys [status resource method] :as notif-subs} notif-methods]
  (let [method-name (-> (filter #(= method (:id %)) @notif-methods)
                        first
                        :name)]
    [ui/TableRow
     [ui/TableCell {:floated :left
                    :width   2}
      [:span status]]
     [ui/TableCell {:floated :left
                    :width   9}
      [:span resource]]
     [ui/TableCell {:floated :left
                    :width   4}
      [:span method-name]]
     [ui/TableCell {:floated :right
                    :width   1
                    :align   :right
                    :style   {}}

      (when (general-utils/can-delete? notif-subs)
        [DeleteButtonSubscriptions notif-subs])

      (when (general-utils/can-edit? notif-subs)
        [ui/Icon {:name     :cog
                  :color    :blue
                  :style    {:cursor :pointer}
                  :on-click #(dispatch [::events/open-edit-subscription-modal notif-subs false])}])]]))


(defn manage-subscriptions-modal
  []
  (let [tr (subscribe [::i18n-subs/tr])
        notif-methods (subscribe [::subs/notification-methods])
        subscriptions (subscribe [::subs/subscriptions])
        visible? (subscribe [::subs/notification-subscriptions-modal-visible?])]
    (dispatch [::events/get-notification-methods])
    (dispatch [::events/get-notification-subscriptions])
    (fn []
      (let [header (@tr [:subscriptions-manage])]
        [:div]
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-notification-subscription-modal])}

         [ui/ModalHeader header]

         [ui/ModalContent {:scrolling false}
          [ui/Table {:style {:margin-top 10}}
           [ui/TableHeader
            [ui/TableRow
             [ui/TableHeaderCell {:content (str/capitalize (@tr [:status]))}]
             [ui/TableHeaderCell {:content (str/capitalize (@tr [:resource]))}]
             [ui/TableHeaderCell {:content (str/capitalize (@tr [:method]))}]
             [ui/TableHeaderCell {:content (str/capitalize (@tr [:action]))}]]]
           [ui/TableBody
            (for [sub @subscriptions]
              ^{:key (:id sub)}
              [single-notification-subscription sub notif-methods])]]
          [utils-validation/validation-error-message ::subs/form-valid?]]]))))


(defn subs-method-dropdown
  [current-value notif-methods]
  ^{:key current-value}
  [ui/Dropdown
   {:selection     true
    :fluid         false
    :default-value (if (not (nil? current-value))
                     current-value
                     (-> @notif-methods first :id))
    :on-change     (ui-callback/value
                     #(do (dispatch-sync [::events/update-subscription :method %])))
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
                     #(do (dispatch-sync [::events/update-subscription :status %])))
    :options       (map (fn [v] {:key v, :value v, :text v}) ["enabled" "disabled"])}])


(defn edit-subscription-modal
  []
  (let [tr (subscribe [::i18n-subs/tr])
        notif-methods (subscribe [::subs/notification-methods])
        visible? (subscribe [::subs/edit-subscription-modal-visible?])
        validate-form? (subscribe [::subs/validate-form?])
        form-valid? (subscribe [::subs/form-valid?])
        subscription (subscribe [::subs/subscription])
        on-change      (fn [name-kw value]
                         (dispatch [::events/update-subscription name-kw value])
                         (dispatch [::events/validate-subscription-form]))]
    (dispatch [::events/get-notification-methods])
    (fn []
      (let [editable? true
            header (str/capitalize (str (@tr [:edit]) " " (@tr [:subscription])))
            {:keys [name description method status type kind category resource]} @subscription]
        [:div]
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-edit-subscription-modal])}

         [ui/ModalHeader header]

         [ui/ModalContent {:scrolling false}
          [utils-validation/validation-error-message ::subs/form-valid?]
          [ui/Table style/definition
           [ui/TableBody
            [uix/TableRowField "name", :editable? editable?, :default-value name,
             :required? false, :spec ::spec/name, :on-change (partial on-change :name),
             :validate-form? @validate-form?]
            [uix/TableRowField "description", :editable? editable?, :required? false,
             :default-value description, :spec ::spec/description,
             :on-change (partial on-change :description), :validate-form? @validate-form?]]
           [ui/TableRow
            [ui/TableCell {:collapsing true
                           :style      {:padding-bottom 8}} "method"]
            [ui/TableCell
             [subs-method-dropdown method notif-methods]]]
           [ui/TableRow
            [ui/TableCell {:collapsing true
                           :style      {:padding-bottom 8}} "status"]
            [ui/TableCell
             [subs-status-dropdown status]]]
           [uix/TableRowField "type", :editable? false, :default-value type, :required? false]
           [uix/TableRowField "kind", :editable? false, :default-value kind, :required? false]
           [uix/TableRowField "category", :editable? false, :default-value category, :required? false]
           [uix/TableRowField "resource"
            :editable? false
            :default-value [history/link (str "api/" resource) resource]
            :required? false]]]

         [ui/ModalActions
          [uix/Button {:text     (@tr [:save])
                       :positive true
                       :disabled (when-not @form-valid? true)
                       :active   true
                       :on-click #(save-callback-subscription ::spec/subscription)}]]]))))


(defn add-notification-method-modal
  []
  (let [tr (subscribe [::i18n-subs/tr])
        visible? (subscribe [::subs/notification-method-modal-visible?])
        validate-form? (subscribe [::subs/validate-form?])
        form-valid? (subscribe [::subs/form-valid?])
        notif-method (subscribe [::subs/notification-method])
        on-change      (fn [name-kw value]
                         (dispatch [::events/update-notification-method name-kw value])
                         (dispatch [::events/validate-notification-method-form]))
        is-new? (subscribe [::subs/is-new?])]
    (fn []
      (let [editable? (general-utils/editable? @notif-method @is-new?)
            header (str/capitalize (str (if (true? @is-new?) (@tr [:new]) (@tr [:update])) " " (@tr [:notification-method])))
            {:keys [name description method destination]} @notif-method]
        [:div]
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-add-update-notification-method-modal])}

         [ui/ModalHeader header]

         [ui/ModalContent {:scrolling false}
          [utils-validation/validation-error-message ::subs/form-valid?]
          [ui/Table style/definition
           [ui/TableBody
            [uix/TableRowField (@tr [:name]), :editable? editable?, :default-value name,
             :required? true, :spec ::spec/name, :on-change (partial on-change :name),
             :validate-form? @validate-form?]
            [uix/TableRowField (@tr [:description]), :editable? editable?, :required? true,
             :default-value description, :spec ::spec/description,
             :on-change (partial on-change :description), :validate-form? @validate-form?]]
           [ui/TableRow
            [ui/TableCell {:collapsing true
                           :style      {:padding-bottom 8}} "method"]
            [ui/TableCell
             [ui/Dropdown {:selection true
                           :fluid     true
                           :value     method
                           :on-change (ui-callback/value
                                        #(dispatch [::events/method (str/lower-case %)]))
                           :options   [{:key "slack", :text "Slack", :value "slack"}
                                       {:key   "email", :text "Email", :value "email"}]}]]]
           [uix/TableRowField (@tr [:destination]), :editable? editable?, :required? true,
            :default-value destination, :spec ::spec/destination,
            :on-change (partial on-change :destination), :validate-form? @validate-form?]]]

         [ui/ModalActions
          [uix/Button {:text     (if (true? @is-new?) (@tr [:create]) (@tr [:save]))
                       :positive true
                       :disabled (when-not @form-valid? true)
                       :active   true
                       :on-click #(save-callback-notification-method ::spec/notification-method)}]]]))))


(defn MenuBarSubscriptions
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [main-components/StickyBar
     [ui/Menu {:borderless true}
      [uix/MenuItemWithIcon
       {:name      (@tr [:add])
        :icon-name "add"
        ;:on-click  #(dispatch [::events/open-add-update-notification-method-modal {} true])
        }]]]))


(defn MenuBarNotificationMethod
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [main-components/StickyBar
     [ui/Menu {:borderless true}
      [uix/MenuItemWithIcon
       {:name      (@tr [:add])
        :icon-name "add"
        :on-click  #(dispatch [::events/open-add-update-notification-method-modal {} true])}]
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
                  :align   :right
                  :style   {}}
    (when (general-utils/can-delete? notif-method)
      [DeleteButtonNotificationMethod notif-method])
    (when (general-utils/can-edit? notif-method)
      [ui/Icon {:name     :cog
                :color    :blue
                :style    {:cursor :pointer}
                :on-click #(dispatch [::events/open-add-update-notification-method-modal notif-method false])}])]])


(defn subs-notif-method-create-button
  []
  [uix/Button {:text    "create"
               :positive true
               :disabled false
               :active   true
               :on-click #(dispatch [::events/open-add-update-notification-method-modal {} true])}])


(defn subs-notif-method-dropdown
  [current-value notif-methods]
  ^{:key current-value}
  [ui/Dropdown
   {:selection     true
    :fluid         false
    :default-value (if (not (nil? current-value))
                     current-value
                     (-> @notif-methods first :id))
    :on-change     (ui-callback/value
                     #(do (dispatch-sync [::events/update-notification-subscription-config :method %])
                          (save-callback-notification-subscription-config ::spec/notification-subscription-config)))
    :options       (map (fn [{id :id, method-name :name}]
                          {:key id, :value id, :text method-name})
                        @notif-methods)}])


(defn subs-notif-method-select-or-add
  [current-method notif-methods]
  (let [create-method? (empty? @notif-methods)]
    (fn []
      (if create-method?
        [subs-notif-method-create-button]
        [subs-notif-method-dropdown current-method notif-methods]))))


(defn TabSubscriptions
  []
  (let [tr (subscribe [::i18n-subs/tr])
        subscriptions (subscribe [::subs/subscriptions])
        subscription-configs (subscribe [::subs/notification-subscription-configs])
        on-change (fn [name-kw value]
                    (dispatch-sync [::events/update-notification-subscription-config name-kw value])
                    (dispatch [::events/validate-notification-subscription-config-form]))
        notif-methods (subscribe [::subs/notification-methods])
        type "notification"
        collection "infrastructure-service"
        category "state"
        name (str type " " collection " " category)]
    (dispatch [::events/get-notification-subscription-configs])
    (dispatch [::events/get-notification-subscriptions])
    (dispatch [::events/get-notification-methods])
    (fn []
      (let [infra-service-subs-confs (filter #(= collection (:collection %)) @subscription-configs)
            infra-service-state-subs-conf (first (filter #(= category (:category %)) infra-service-subs-confs))]
        (if infra-service-state-subs-conf
          (dispatch [::events/set-notification-subscription-config infra-service-state-subs-conf])
          (dispatch [::events/set-notification-subscription-config {:type        type
                                                                    :collection  collection
                                                                    :category    category
                                                                    :enabled     false
                                                                    :name        name
                                                                    :description name}]))
        [ui/TabPane
         [MenuBarSubscriptions]
         [uix/Accordion
          [:<>
           [:div
            [ui/Table {:basic   "very"
                       :compact true
                       :style   {:margin-top 10}}
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
                [:span ff/nbsp (ff/help-popup (@tr [:subscriptions-manage-help]))]]]]
             [ui/TableBody
              [ui/TableRow
               [ui/TableCell {:floated :left
                              :width   2}
                [:span (str/capitalize (@tr [:state-change]))]
                [:span ff/nbsp (ff/help-popup (@tr [:subscription-infra-svc-state-change-help]))]]
               [ui/TableCell {:floated :left
                              :width   2}
                [:span
                 [ui/Checkbox {:key             "enable-new"
                               :disabled        (empty? @notif-methods)
                               :default-checked (:enabled infra-service-state-subs-conf)
                               :style           {:margin "1em"}
                               :on-change       (ui-callback/checked
                                                  #(do
                                                     (on-change :enabled %)
                                                     (if (= 1 (count @notif-methods))
                                                       (on-change :method (-> @notif-methods
                                                                              first
                                                                              :id)))
                                                     (save-callback-notification-subscription-config
                                                       ::spec/notification-subscription-config)))}]]]
               [ui/TableCell {:floated :left
                              :width   4}
                [subs-notif-method-select-or-add (:method infra-service-state-subs-conf) notif-methods]]
               [ui/TableCell {:floated :left
                              :width   1
                              :align   :right
                              :style   {}}
                [uix/Button {:text     (@tr [:manage])
                             :positive true
                             :disabled (empty? @subscriptions)
                             :active   true
                             :on-click #(dispatch [::events/open-notification-subscription-modal @subscriptions])}]]]]]]
           ]
          :label (str (@tr [:infra-services]) " " (@tr [:subscriptions]))
          :count (count @subscriptions)]]))))


(defn TabMethods
  []
  (let [tr (subscribe [::i18n-subs/tr])
        all-methods (subscribe [::subs/notification-methods])]
    (dispatch [::events/get-notification-methods])
    (fn []
      (let []
        [ui/TabPane
         [MenuBarNotificationMethod]
         [:div methods]
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
                     [single-notification-method notif-method])]]])]))))


(defn tabs
  [tr]
  [{:menuItem {:content (str/capitalize (@tr [:subscriptions]))
               :key     "subscriptions"
               :icon    "list alternate outline"}
    :render (fn [] (r/as-element [TabSubscriptions]))}
   {:menuItem {:content (str/capitalize (@tr [:methods]))
               :key     "methods"
               :icon    "at"}
    :render (fn [] (r/as-element [TabMethods]))}])


(defn TabsAll
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Tab
     {:menu   {:secondary true
               :pointing  true
               :style {:display "flex"
                       :flex-direction "row"
                       :flex-wrap "wrap"}}
      :panes  (tabs tr)}]))


(defmethod panel/render :notifications
  [path]
  (timbre/set-level! :info)
  [:<>
   [TabsAll]
   [edit-subscription-modal]
   [add-notification-method-modal]
   [manage-subscriptions-modal]])
