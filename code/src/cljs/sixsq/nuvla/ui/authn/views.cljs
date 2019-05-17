(ns sixsq.nuvla.ui.authn.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as reagent]
    [sixsq.nuvla.ui.authn.events :as events]
    [sixsq.nuvla.ui.authn.subs :as subs]
    [sixsq.nuvla.ui.authn.utils :as u]
    [sixsq.nuvla.ui.cimi.subs :as api-subs]
    [sixsq.nuvla.ui.cimi.utils :as api-utils]
    [sixsq.nuvla.ui.docs.subs :as docs-subs]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.history.utils :as history-utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.form-fields :as forms]
    [sixsq.nuvla.ui.utils.forms :as forms-utils]
    [sixsq.nuvla.ui.utils.general :as utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [taoensso.timbre :as log]))

(defn dropdown-method-option
  [{:keys [id name] :as method}]
  {:key id, :text name, :value id})


(defn login-password-fields
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [[ui/FormInput {:name          "username"
                    :placeholder   (str/capitalize (@tr [:username]))
                    :icon          "user"
                    :icon-position "left"
                    :auto-focus    true
                    :on-change     (ui-callback/value
                                     #(dispatch [::events/update-form-data :username %]))}]
     [ui/FormInput {:name          "password"
                    :placeholder   (str/capitalize (@tr [:password]))
                    :type          "password"
                    :icon          "key"
                    :icon-position "left"
                    :on-change     (ui-callback/value
                                     #(dispatch [::events/update-form-data :password %]))}]
     [:div {:style {:text-align "right"}}
      [:a {:style    {:cursor "pointer"}
           :on-click (fn []
                       (dispatch [::events/close-modal])
                       (dispatch [::events/set-selected-method-group nil])
                       (dispatch [::events/set-form-id "session-template/password-reset"])
                       (dispatch [::events/open-modal :reset-password]))}
       (@tr [:forgot-password])]]]))


(defn login-api-key-fields
  []
  [[ui/FormInput {:name          "key"
                  :placeholder   "Key"
                  :icon          "user"
                  :icon-position "left"
                  :auto-focus    true
                  :on-change     (ui-callback/value
                                   #(dispatch [::events/update-form-data :key %]))}]
   [ui/FormInput {:name          "secret"
                  :placeholder   "Secret"
                  :icon          "key"
                  :icon-position "left"
                  :on-change     (ui-callback/value
                                   #(dispatch [::events/update-form-data :secret %]))}]])


(defn signup-email-password-fields
  []
  (let [tr              (subscribe [::i18n-subs/tr])
        fields-in-error (subscribe [::subs/fields-in-errors])]
    [[ui/FormInput {:name          "email"
                    :placeholder   "email"
                    :icon          "at"
                    :icon-position "left"
                    :auto-focus    true
                    :auto-complete "on"
                    :on-change     (ui-callback/value
                                     #(dispatch [::events/update-form-data :email %]))}]

     [ui/FormGroup {:widths 2}
      [ui/FormInput {:name          "password"
                     :type          "password"
                     :placeholder   (str/capitalize (@tr [:password]))
                     :icon          "key"
                     :icon-position "left"
                     :required      true
                     :error         (contains? @fields-in-error "password")
                     :auto-complete "off"
                     :on-change     (ui-callback/value
                                      #(dispatch [::events/update-form-data :password %]))}]

      [ui/FormInput {:name          "password"
                     :type          "password"
                     :placeholder   (str/capitalize (@tr [:password-repeat]))
                     :required      true
                     :error         (contains? @fields-in-error "password")
                     :auto-complete "off"
                     :on-change     (ui-callback/value
                                      #(dispatch [::events/update-form-data :repeat-password %]))}]]]))


;;TODO fix field generation when resource metadata finalized
(defn generate-fields
  [method form-id form-data]
  (let [resource-metadata (subscribe [::docs-subs/document method])
        inputs-method     (->> (:attributes @resource-metadata)
                               (filter (fn [{:keys [required group] :as attribute}]
                                         (and (not (#{"metadata" "acl"} group))
                                              required)))
                               (sort-by :order))]
    (mapv (fn [{value-scope :value-scope param-name :name :as input-method}]
            (let [value                (get form-data param-name)
                  input-method-updated (cond-> input-method
                                               value (assoc-in [:value-scope :value] value))]
              (dispatch [::events/update-form-data
                         param-name (or value
                                        (:value value-scope)
                                        (:default value-scope))])
              (forms/form-field (fn [_ name value]
                                  (dispatch [::events/update-form-data name value])
                                  ) form-id input-method-updated))) inputs-method)))


(defn login-method-form
  [[_ methods]]
  (let [form-id     (subscribe [::subs/form-id])
        form-data   (subscribe [::subs/form-data])
        form-error? (subscribe [::subs/form-error?])]
    (fn [[_ methods]]
      (let [dropdown?        (> (count methods) 1)
            method           (u/select-method-by-id @form-id methods)
            form-fields      (case @form-id
                               "session-template/password" (login-password-fields)
                               "session-template/api-key" (login-api-key-fields)
                               (generate-fields method @form-id @form-data))
            dropdown-options (map dropdown-method-option methods)]

        ^{:key @form-id}
        [ui/Form {:id           (or @form-id "authn-form-placeholder-id")
                  :on-key-press (partial forms-utils/on-return-key
                                         #(when-not @form-error?
                                            (dispatch [::events/submit])))}
         (vec (concat [ui/Segment {:style {:height     "35ex"
                                           :overflow-y "auto"}}
                       (when dropdown?
                         [ui/FormDropdown
                          {:options       dropdown-options
                           :value         @form-id
                           :fluid         true
                           :selection     true
                           :close-on-blur true
                           :on-change     (ui-callback/dropdown ::events/set-form-id)}])]

                      form-fields))]))))


(defn signup-method-form
  [[_ methods]]
  (let [tr          (subscribe [::i18n-subs/tr])
        form-id     (subscribe [::subs/form-id])
        form-data   (subscribe [::subs/form-data])
        form-error? (subscribe [::subs/form-error?])]
    (fn [[_ methods]]
      ^{:key @form-id}
      (let [dropdown?        (> (count methods) 1)
            method           (u/select-method-by-id @form-id methods)

            form-fields      (case @form-id
                               "user-template/email-password" (signup-email-password-fields)
                               (generate-fields method @form-id @form-data))

            dropdown-options (map dropdown-method-option methods)]

        [ui/Form
         {:id           (or @form-id "authn-form-placeholder-id")
          :on-key-press (partial forms-utils/on-return-key
                                 #(when-not @form-error?
                                    (dispatch [::events/submit {:close-modal false
                                                                :success-msg (@tr [:validation-email-success-msg])}])))}

         (vec (concat [ui/Segment {:style {:height     "35ex"
                                           :overflow-y "auto"}}
                       (when dropdown?
                         [ui/FormDropdown
                          {:options       dropdown-options
                           :value         @form-id
                           :fluid         true
                           :selection     true
                           :close-on-blur true
                           :on-change     (ui-callback/dropdown ::events/set-form-id)}])]

                      form-fields))]))))


(defn authn-method-group-option
  [[group methods]]
  (let [{:keys [icon]} (first methods)
        option-label (reagent/as-element [:span [ui/Icon {:name icon}] group])]
    {:text    option-label
     :value   group
     :content option-label}))


(defn generic-method-dropdown
  [method-groups]
  (let [selected-method-group (subscribe [::subs/selected-method-group])]
    (fn [method-groups]
      (let [default         (ffirst method-groups)
            default-form-id (-> method-groups first second first :id)
            options         (mapv authn-method-group-option method-groups)]

        (when (nil? @selected-method-group)
          (dispatch [::events/set-selected-method-group default])
          (dispatch [::events/set-form-id default-form-id]))

        [ui/Dropdown {:fluid     true
                      :selection true
                      :loading   (nil? @selected-method-group)
                      :value     @selected-method-group
                      :options   options
                      :on-change (ui-callback/value
                                   (fn [group-id]
                                     (dispatch [::events/set-selected-method-group group-id])
                                     (let [form-id (-> (u/select-group-methods-by-id group-id method-groups)
                                                       first
                                                       :id)]
                                       (dispatch [::events/set-form-id form-id]))))}]))))


(defn generic-form-container
  "Container that holds all of the authentication (login or sign up) forms."
  [collection-kw failed-kw method-form-fn]
  (let [template-href         (api-utils/collection-template-href collection-kw)
        templates             (subscribe [::api-subs/collection-templates template-href])
        tr                    (subscribe [::i18n-subs/tr])
        error-message         (subscribe [::subs/error-message])
        success-message       (subscribe [::subs/success-message])
        selected-method-group (subscribe [::subs/selected-method-group])]
    (fn [collection-kw failed-kw method-form-fn]
      (let [method-groups               (u/grouped-authn-methods @templates)
            selected-authn-method-group (some->> method-groups
                                                 (filter #(-> % first (= @selected-method-group)))
                                                 first)]

        [ui/Segment {:basic true}
         (when @error-message
           [ui/Message {:negative  true
                        :size      "tiny"
                        :onDismiss #(dispatch [::events/clear-error-message])}
            [ui/MessageHeader (@tr [failed-kw])]
            [:p @error-message]])

         (when @success-message
           [ui/Message {:negative  false
                        :size      "tiny"
                        :onDismiss #(dispatch [::events/clear-success-message])}
            [ui/MessageHeader (@tr [:success])]
            [:p @success-message]])

         [generic-method-dropdown method-groups]
         [ui/Divider]
         [method-form-fn selected-authn-method-group]]))))


(defn login-form-container
  []
  [generic-form-container :session :login-failed login-method-form])


(defn signup-form-container
  []
  [generic-form-container :user :signup-failed signup-method-form])


(defn switch-panel-link
  [modal-kw]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [modal-kw]
      (let [other-modal (case modal-kw
                          :login :signup
                          :reset-password :login
                          :signup :login)
            on-click    (fn []
                          (dispatch [::events/close-modal])
                          (dispatch [::events/set-selected-method-group nil])
                          (dispatch [::events/set-form-id nil])
                          (dispatch [::events/open-modal other-modal]))]
        (case modal-kw
          :login [:span (@tr [:no-account?]) " "
                  [:a {:on-click on-click :style {:cursor "pointer"}} (str (@tr [:signup-link]))]]
          :reset-password [:span (@tr [:already-registered?]) " "
                           [:a {:on-click on-click :style {:cursor "pointer"}} (str (@tr [:login-link]))]]
          :signup [:span (@tr [:already-registered?]) " "
                   [:a {:on-click on-click :style {:cursor "pointer"}} (str (@tr [:login-link]))]])))))


(defn generic-modal
  [id modal-kw form-fn submit-opts]
  (let [tr          (subscribe [::i18n-subs/tr])
        open-modal  (subscribe [::subs/open-modal])
        form-error? (subscribe [::subs/form-error?])]
    (fn [id modal-kw form-fn]
      [ui/Modal
       {:id        id
        :size      :tiny
        :open      (= @open-modal modal-kw)
        :closeIcon true
        :on-close  #(dispatch [::events/close-modal])}

       [ui/ModalHeader (@tr [modal-kw])]

       [ui/ModalContent
        [form-fn]]

       [ui/ModalActions
        [switch-panel-link modal-kw]
        [uix/Button
         {:text     (@tr [modal-kw])
          :positive true
          :disabled @form-error?
          :on-click #(dispatch [::events/submit submit-opts])}]]])))


(defn modal-login []
  [generic-modal "modal-login-id" :login login-form-container {}])


(defn modal-reset-password []
  (let [open-modal       (subscribe [::subs/open-modal])
        error-message    (subscribe [::subs/error-message])
        success-message  (subscribe [::subs/success-message])
        loading?         (subscribe [::subs/loading?])
        tr               (subscribe [::i18n-subs/tr])
        fields-in-errors (subscribe [::subs/fields-in-errors])
        form-error?      (subscribe [::subs/form-error?])
        submit-fn        #(dispatch [::events/submit {:close-modal false
                                                      :success-msg (@tr [:validation-email-success-msg])}])]
    (fn []
      [ui/Modal
       {:id        "modal-reset-password-id"
        :size      :tiny
        :open      (= @open-modal :reset-password)
        :closeIcon true
        :on-close  #(dispatch [::events/close-modal])}

       [ui/ModalHeader (@tr [:reset-password])]

       [ui/ModalContent

        (when @error-message
          [ui/Message {:negative  true
                       :size      "tiny"
                       :onDismiss #(dispatch [::events/clear-error-message])}
           [ui/MessageHeader (@tr [:reset-password-error])]
           [:p @error-message]])

        (when @success-message
          [ui/Message {:negative  false
                       :size      "tiny"
                       :onDismiss #(dispatch [::events/clear-success-message])}
           [ui/MessageHeader (@tr [:success])]
           [:p @success-message]])

        [ui/Form {:on-key-press (partial forms-utils/on-return-key
                                         #(when-not @form-error?
                                            (submit-fn)))}
         [ui/FormInput {:name          "username"
                        :placeholder   (str/capitalize (@tr [:username]))
                        :icon          "user"
                        :fluid         false
                        :icon-position "left"
                        :required      true
                        :auto-focus    true
                        :auto-complete "on"
                        :on-change     (ui-callback/value
                                         #(dispatch [::events/update-form-data :username %]))}]

         [ui/FormGroup {:widths 2}
          [ui/FormInput {:name          "password"
                         :type          "password"
                         :placeholder   (str/capitalize (@tr [:new-password]))
                         :icon          "key"
                         :icon-position "left"
                         :required      true
                         :error         (contains? @fields-in-errors "password")
                         :auto-complete "off"
                         :on-change     (ui-callback/value
                                          #(dispatch [::events/update-form-data :new-password %]))}]

          [ui/FormInput {:name          "password"
                         :type          "password"
                         :placeholder   (str/capitalize (@tr [:new-password-repeat]))
                         :required      true
                         :error         (contains? @fields-in-errors "password")
                         :auto-complete "off"
                         :on-change     (ui-callback/value
                                          #(dispatch [::events/update-form-data :repeat-new-password %]))}]]]

        [:div {:style {:padding "10px 0"}} (@tr [:reset-password-inst])]]

       [ui/ModalActions
        [switch-panel-link :reset-password]
        [uix/Button
         {:text     (@tr [:reset-password])
          :positive true
          :loading  @loading?
          :disabled @form-error?
          :on-click submit-fn}]]])))


(defn modal-create-user []
  (let [open-modal          (subscribe [::subs/open-modal])
        error-message       (subscribe [::subs/error-message])
        success-message     (subscribe [::subs/success-message])
        loading?            (subscribe [::subs/loading?])
        tr                  (subscribe [::i18n-subs/tr])
        fields-in-errors    (subscribe [::subs/fields-in-errors])
        form-error?         (subscribe [::subs/form-error?])
        server-redirect-uri (subscribe [::subs/server-redirect-uri])
        submit-fn           #(dispatch [::events/submit {:close-modal false
                                                         :error-msg   (@tr [:error-occured])
                                                         :success-msg (@tr [:invitation-email-success-msg])
                                                         :redirect-url (str @server-redirect-uri "?reset-password")}])]
    (fn []
      [ui/Modal
       {:id        "modal-create-user"
        :size      :tiny
        :open      (= @open-modal :create-user)
        :closeIcon true
        :on-close  #(dispatch [::events/close-modal])}

       [ui/ModalHeader (@tr [:create-user])]

       [ui/ModalContent

        (when @error-message
          [ui/Message {:negative  true
                       :size      "tiny"
                       :onDismiss #(dispatch [::events/clear-error-message])}
           [ui/MessageHeader (@tr [:error-occured])]
           [:p @error-message]])

        (when @success-message
          [ui/Message {:negative  false
                       :size      "tiny"
                       :onDismiss #(dispatch [::events/clear-success-message])}
           [ui/MessageHeader (@tr [:success])]
           [:p @success-message]])

        [ui/Form {:on-key-press (partial forms-utils/on-return-key
                                         #(when-not @form-error?
                                            (submit-fn)))}
         [ui/FormInput {:name          "email"
                        :placeholder   "user@example.com"
                        :icon          "mail"
                        :fluid         false
                        :icon-position "left"
                        :error         (contains? @fields-in-errors "email")
                        :required      true
                        :auto-focus    true
                        :auto-complete "on"
                        :on-change     (ui-callback/value
                                         #(dispatch [::events/update-form-data :email %]))}]]

        [:div {:style {:padding "10px 0"}} (@tr [:create-user-inst])]]

       [ui/ModalActions
        [uix/Button
         {:text     (@tr [:create-user])
          :positive true
          :loading  @loading?
          :disabled @form-error?
          :on-click submit-fn}]]])))



(defn modal-signup []
  (let [tr (subscribe [::i18n-subs/tr])]
    [generic-modal "modal-signup-id" :signup signup-form-container
     {:close-modal false
      :success-msg (@tr [:validation-email-success-msg])}]))


(defn authn-dropdown-menu
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        user           (subscribe [::subs/user])
        sign-out-fn    (fn []
                         (dispatch [::events/logout])
                         (dispatch [::history-events/navigate "welcome"]))
        create-user-fn #(do
                          (dispatch [::events/set-form-id "user-template/email-invitation"])
                          (dispatch [::events/open-modal :create-user]))
        logged-in?     (boolean @user)]

    [ui/DropdownMenu

     (when logged-in?
       [:<>
        [ui/DropdownItem
         {:key      "sign-out"
          :text     (@tr [:logout])
          :icon     "sign out"
          :on-click sign-out-fn}]
        [ui/DropdownDivider]
        [ui/DropdownItem
         {:key      "invite"
          :text     (@tr [:create-user])
          :icon     "user add"
          :on-click create-user-fn}]
        [ui/DropdownDivider]])

     [:<>
      [ui/DropdownItem {:aria-label (@tr [:documentation])
                        :icon       "book"
                        :text       (@tr [:documentation])
                        :href       "https://docs.nuvla.io/"
                        :target     "_blank"
                        :rel        "noreferrer"}]
      [ui/DropdownItem {:aria-label (@tr [:knowledge-base])
                        :icon       "info circle"
                        :text       (@tr [:knowledge-base])
                        :href       "https://support.sixsq.com/solution/categories"
                        :target     "_blank"
                        :rel        "noreferrer"}]
      [ui/DropdownItem {:aria-label (@tr [:support])
                        :icon       "mail"
                        :text       (@tr [:support])
                        :href       (str "mailto:support%40sixsq%2Ecom?subject=%5BSlipStream%5D%20Support%20"
                                         "question%20%2D%20Not%20logged%20in")}]]]))


(defn authn-menu
  []
  (let [tr         (subscribe [::i18n-subs/tr])
        user       (subscribe [::subs/user])
        profile-fn #(history-utils/navigate "profile")
        login-fn   #(dispatch [::events/open-modal :login])
        signup-fn  #(dispatch [::events/open-modal :signup])
        logged-in? (boolean @user)]
    [:<>
     [modal-login]
     [modal-reset-password]
     [modal-signup]
     [modal-create-user]
     (if logged-in?
       [ui/ButtonGroup {:primary true}
        [ui/Button {:on-click profile-fn}
         [ui/Icon {:name "user"}]
         (utils/truncate @user)]
        [ui/Dropdown {:inline    true
                      :button    true
                      :pointing  "top right"
                      :className "icon"}
         (authn-dropdown-menu)]]
       [:div
        [:span {:style    {:padding-right "10px"
                           :cursor        "pointer"}
                :on-click signup-fn}
         [ui/Icon {:name "signup"}]
         (@tr [:signup])]
        [ui/ButtonGroup {:primary true}
         [ui/Button {:on-click login-fn}
          [ui/Icon {:name "sign in"}]
          (@tr [:login])]
         [ui/Dropdown {:inline    true
                       :button    true
                       :pointing  "top right"
                       :className "icon"}
          (authn-dropdown-menu)]]])]))
