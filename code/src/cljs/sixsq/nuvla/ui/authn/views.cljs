(ns sixsq.nuvla.ui.authn.views
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.authn.events :as events]
    [sixsq.nuvla.ui.authn.spec :as spec]
    [sixsq.nuvla.ui.authn.subs :as subs]
    [sixsq.nuvla.ui.authn.utils :as utils]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-fx]
    [sixsq.nuvla.ui.cimi.subs :as api-subs]
    [sixsq.nuvla.ui.cimi.utils :as api-utils]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.forms :as forms-utils]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.config :as config]))


(defn dropdown-method-option
  [{:keys [id name] :as method}]
  {:key id, :text name, :value id})


(defn generic-submit
  [submit-opts]
  (let [internal-auth? (subscribe [::subs/internal-auth?])
        form-id        (subscribe [::subs/form-id])]
    (if @internal-auth?
      #(dispatch [::events/submit submit-opts])
      #(some->> @form-id
                (.getElementById js/document)
                (.submit)))))

(defn login-password-fields
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        form-data (subscribe [::subs/form-data])]
    (fn []
      (let [{:keys [username password]} @form-data]
        [:<>
         [ui/FormInput {:name          "username"
                        :placeholder   (@tr [:username])
                        :icon          "user"
                        :icon-position "left"
                        :auto-focus    true
                        :on-change     (ui-callback/input-callback
                                         #(dispatch [::events/update-form-data :username %]))}]
         [ui/FormInput {:name          "password"
                        :placeholder   (str/capitalize (@tr [:password]))
                        :type          "password"
                        :icon          "key"
                        :icon-position "left"
                        :on-change     (ui-callback/input-callback
                                         #(dispatch [::events/update-form-data :password %]))}]
         [:div {:style {:text-align "right"}}
          [:a {:style    {:cursor "pointer"}
               :on-click (fn []
                           (dispatch [::events/close-modal])
                           (dispatch [::events/set-selected-method-group nil])
                           (dispatch [::events/set-form-id utils/session-tmpl-password-reset])
                           (dispatch [::events/open-modal :reset-password]))}
           (@tr [:forgot-password])]]]))))


(defn login-api-key-fields
  []
  (let [form-data (subscribe [::subs/form-data])]
    (fn []
      (let [{:keys [key secret]} @form-data]
        [:<>
         [ui/FormInput {:name          "key"
                        :placeholder   "Key"
                        :icon          "user"
                        :icon-position "left"
                        :error         (and
                                         (some? key)
                                         (not (s/valid? ::spec/key key)))
                        :auto-focus    true
                        :on-change     (ui-callback/input-callback
                                         #(dispatch [::events/update-form-data :key %]))}]
         [ui/FormInput {:name          "secret"
                        :placeholder   "Secret"
                        :icon          "key"
                        :icon-position "left"
                        :error         (and
                                         (some? secret)
                                         (not (s/valid? ::spec/secret secret)))
                        :on-change     (ui-callback/input-callback
                                         #(dispatch [::events/update-form-data :secret %]))}]]))))


(defn signup-email-password-fields
  []
  (let [tr                         (subscribe [::i18n-subs/tr])
        form-id                    (subscribe [::subs/form-id])
        form-data                  (subscribe [::subs/form-data])
        email-invalid?             (subscribe [::subs/form-signup-email-invalid?])
        passwords-doesnt-match?    (subscribe [::subs/form-signup-passwords-doesnt-match?])
        password-constraint-error? (subscribe [::subs/form-signup-password-constraint-error?])
        update-email-callback      (ui-callback/input-callback
                                     #(when-not (str/blank? %)
                                        (dispatch [::events/update-form-data :email %])))
        update-password-callback   (ui-callback/input-callback
                                     #(when-not (str/blank? %)
                                        (dispatch [::events/update-form-data :password %])))]
    (fn []
      (let [password-field-error? (or @passwords-doesnt-match?
                                      @password-constraint-error?)
            errors-list           (cond-> []
                                          @email-invalid? (conj (@tr [:email-invalid-format]))
                                          @passwords-doesnt-match? (conj (@tr [:passwords-doesnt-match]))
                                          @password-constraint-error? (conj (@tr [:password-constraint])))]

        ^{:key @form-id}
        [:<>

         [ui/Message {:hidden (empty? errors-list)
                      :size   "tiny"
                      :error  true
                      :header (@tr [:validation-error])
                      :list   errors-list}]

         [ui/FormInput {:name          "signup-email"
                        :placeholder   "email"
                        :icon          "at"
                        :icon-position "left"
                        :auto-focus    true
                        :auto-complete "off"
                        :error         @email-invalid?
                        :on-blur       update-email-callback
                        :on-change     (when @email-invalid?
                                         update-email-callback)}]

         [ui/FormGroup {:widths 2}
          [ui/FormInput {:name          "singup-password"
                         :type          "password"
                         :placeholder   (str/capitalize (@tr [:password]))
                         :icon          "key"
                         :icon-position "left"
                         :required      true
                         :error         password-field-error?
                         :auto-complete "new-password"
                         :on-blur       update-password-callback
                         :on-change     (when password-field-error? update-password-callback)}]

          [ui/FormInput {:name          "singup-password-repeat"
                         :type          "password"
                         :placeholder   (str/capitalize (@tr [:password-repeat]))
                         :required      true
                         :error         (or @passwords-doesnt-match?
                                            @password-constraint-error?)
                         :auto-complete "off"
                         :on-blur       (ui-callback/input-callback
                                          #(dispatch [::events/update-form-data :repeat-password %]))
                         :on-change     (ui-callback/input-callback
                                          #(when (= (:password @form-data) %)
                                             (dispatch [::events/update-form-data :repeat-password %])))}]]]))))


(defn generate-fields
  [method form-id form-data]
  (let [server-redirect-uri (subscribe [::subs/server-redirect-uri])]
    [:<>
     [ui/FormField
      [:input {:name      "href"
               :value     (or form-id "")
               :read-only true
               :hidden    true}]]
     [ui/FormField
      [:input {:name      "redirect-url"
               :hidden    true
               :read-only true
               :value     (str @server-redirect-uri)}]]]))


(defn login-method-form
  [[_ methods]]
  (let [form-id     (subscribe [::subs/form-id])
        form-data   (subscribe [::subs/form-data])
        form-valid? (subscribe [::subs/form-valid?])]
    (fn [[_ methods]]
      (let [dropdown?        (> (count methods) 1)
            method           (utils/select-method-by-id @form-id methods)
            dropdown-options (map dropdown-method-option methods)
            post-uri         (str @cimi-fx/NUVLA_URL "/api/session")]

        ^{:key @form-id}
        [ui/Form {:id           @form-id
                  :action       post-uri
                  :method       "post"
                  :on-key-press (when @form-valid?
                                  (partial forms-utils/on-return-key
                                           (generic-submit {})))}
         [ui/Segment {:style {:height     "35ex"
                              :overflow-y "auto"}}
          (when dropdown?
            [ui/FormDropdown
             {:options       dropdown-options
              :value         @form-id
              :fluid         true
              :selection     true
              :close-on-blur true
              :on-change     (ui-callback/dropdown ::events/set-form-id)}])

          (case @form-id
            utils/session-tmpl-password [login-password-fields]
            utils/session-tmpl-api-key [login-api-key-fields]
            [generate-fields method @form-id @form-data])]]))))


(defn submit-signup-opts
  []
  (let [tr                             (subscribe [::i18n-subs/tr])
        server-redirect-uri            (subscribe [::subs/server-redirect-uri])
        callback-message-on-validation (js/encodeURI "signup-validation-success")]
    {:close-modal  false
     :success-msg  (@tr [:validation-email-success-msg])
     :redirect-url (str @server-redirect-uri "?message=" callback-message-on-validation)}))


(defn signup-method-form
  [[_ methods]]
  (let [tr          (subscribe [::i18n-subs/tr])
        form-id     (subscribe [::subs/form-id])
        form-data   (subscribe [::subs/form-data])
        form-valid? (subscribe [::subs/form-valid?])]
    (fn [[_ methods]]
      ^{:key @form-id}
      (let [dropdown?        (> (count methods) 1)
            method           (utils/select-method-by-id @form-id methods)

            dropdown-options (map dropdown-method-option methods)
            post-uri         (str @cimi-fx/NUVLA_URL "/api/user")]

        [ui/Form
         {:id           @form-id
          :action       post-uri
          :method       "post"
          :error        true                                ;; Needed to show validation Message
          :on-key-press (partial forms-utils/on-return-key
                                 (when @form-valid?
                                   (generic-submit (submit-signup-opts))))}

         [ui/Segment {:style {:height     "35ex"
                              :overflow-y "auto"}}
          (when dropdown?
            [ui/FormDropdown
             {:options       dropdown-options
              :value         @form-id
              :fluid         true
              :selection     true
              :close-on-blur true
              :on-change     (ui-callback/dropdown ::events/set-form-id)}])
          (case @form-id
            utils/user-tmpl-email-password [signup-email-password-fields]
            [generate-fields method @form-id @form-data])]]))))


(defn authn-method-group-option
  [[group methods]]
  (let [{:keys [icon]} (first methods)
        option-label (r/as-element [:span [ui/Icon {:name icon}] group])]
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
                                     (let [form-id (-> (utils/select-group-methods-by-id group-id method-groups)
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
      (let [method-groups               (utils/grouped-authn-methods @templates)
            selected-authn-method-group (some->> method-groups
                                                 (filter #(-> % first (= @selected-method-group)))
                                                 first)]

        [ui/Segment {:basic true}
         (when @error-message
           [ui/Message {:negative  true
                        :size      "tiny"
                        :onDismiss #(dispatch [::events/set-error-message nil])}
            [ui/MessageHeader (@tr [failed-kw])]
            [:p @error-message]])

         (when @success-message
           [ui/Message {:negative  false
                        :size      "tiny"
                        :onDismiss #(dispatch [::events/set-success-message nil])}
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
  (let [tr               (subscribe [::i18n-subs/tr])
        signup-template? (subscribe [::subs/user-template-exist? utils/user-tmpl-email-password])]
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
          :login (when @signup-template?
                   [:span (@tr [:no-account?]) " "
                    [:a {:on-click on-click :style {:cursor "pointer"}} (str (@tr [:signup-link]))]])
          :reset-password [:span (@tr [:already-registered?]) " "
                           [:a {:on-click on-click :style {:cursor "pointer"}} (str (@tr [:login-link]))]]
          :signup [:span (@tr [:already-registered?]) " "
                   [:a {:on-click on-click :style {:cursor "pointer"}} (str (@tr [:login-link]))]])))))


(defn generic-modal
  [id modal-kw form-fn submit-opts]
  (let [tr          (subscribe [::i18n-subs/tr])
        open-modal  (subscribe [::subs/open-modal])
        loading?    (subscribe [::subs/loading?])
        form-valid? (subscribe [::subs/form-valid?])]
    (fn [id modal-kw form-fn submit-opts]
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
          :loading  @loading?
          :disabled (not @form-valid?)
          :on-click (generic-submit submit-opts)}]]])))


(defn modal-login
  []
  [generic-modal "modal-login-id" :login login-form-container {}])


(defn modal-reset-password []
  (let [tr                         (subscribe [::i18n-subs/tr])
        open-modal                 (subscribe [::subs/open-modal])
        error-message              (subscribe [::subs/error-message])
        success-message            (subscribe [::subs/success-message])
        loading?                   (subscribe [::subs/loading?])
        submit-fn                  #(dispatch [::events/submit {:close-modal false
                                                                :success-msg (@tr [:validation-email-success-msg])}])
        form-id                    (subscribe [::subs/form-id])
        form-data                  (subscribe [::subs/form-data])
        username-invalid?          (subscribe [::subs/form-password-reset-username-invalid?])
        passwords-doesnt-match?    (subscribe [::subs/form-password-reset-passwords-doesnt-match?])
        password-constraint-error? (subscribe [::subs/form-password-reset-password-constraint-error?])
        form-valid?                (subscribe [::subs/form-password-reset-valid?])
        update-username-callback   (ui-callback/input-callback
                                     #(dispatch [::events/update-form-data :username %]))
        update-password-callback   (ui-callback/input-callback
                                     #(dispatch [::events/update-form-data :new-password %]))]
    (fn []
      (let [{:keys [username new-password repeat-new-password]} @form-data
            password-field-error? (or @password-constraint-error?
                                      @passwords-doesnt-match?)
            errors-list           (cond-> []
                                          @passwords-doesnt-match? (conj (@tr [:passwords-doesnt-match]))
                                          @password-constraint-error? (conj (@tr [:password-constraint])))]

        ^{:key @form-id}
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
                         :onDismiss #(dispatch [::events/set-error-message nil])}
             [ui/MessageHeader (@tr [:reset-password-error])]
             [:p @error-message]])

          (when @success-message
            [ui/Message {:negative  false
                         :size      "tiny"
                         :onDismiss #(dispatch [::events/clear-success-message])}
             [ui/MessageHeader (@tr [:success])]
             [:p @success-message]])

          [ui/Form {:error        true                      ;; Needed to show validation Message
                    :on-key-press (partial forms-utils/on-return-key
                                           #(when @form-valid?
                                              (submit-fn)))}

           [ui/Message {:hidden (empty? errors-list)
                        :size   "tiny"
                        :error  true
                        :header (@tr [:validation-error])
                        :list   errors-list}]

           [ui/FormInput {:name          "reset-username"
                          :placeholder   (@tr [:username])
                          :icon          "user"
                          :fluid         false
                          :icon-position "left"
                          :error         @username-invalid?
                          :default-value (or username "")
                          :required      true
                          :auto-focus    true
                          :auto-complete "on"
                          :on-blur       update-username-callback
                          :on-change     (when @username-invalid? update-username-callback)}]

           [ui/FormGroup {:widths 2}
            [ui/FormInput {:name          "reset-new-password"
                           :type          "password"
                           :placeholder   (str/capitalize (@tr [:new-password]))
                           :icon          "key"
                           :icon-position "left"
                           :required      true
                           :error         password-field-error?
                           :auto-complete "new-password"
                           :on-blur       update-password-callback
                           :on-change     (when password-field-error? update-password-callback)}]

            [ui/FormInput {:name          "reset-new-password-repeat"
                           :type          "password"
                           :placeholder   (str/capitalize (@tr [:new-password-repeat]))
                           :required      true
                           :error         (or @password-constraint-error?
                                              @passwords-doesnt-match?)
                           :auto-complete "off"
                           :on-change     (ui-callback/input-callback
                                            #(when (= new-password %)
                                               (dispatch [::events/update-form-data :repeat-new-password %])))
                           :on-blur       (ui-callback/input-callback
                                            #(dispatch [::events/update-form-data :repeat-new-password %]))}]]]

          [:div {:style {:padding "10px 0"}} (@tr [:reset-password-inst])]]

         [ui/ModalActions
          [switch-panel-link :reset-password]
          [uix/Button
           {:text     (@tr [:reset-password])
            :positive true
            :loading  @loading?
            :disabled (not @form-valid?)
            :on-click submit-fn}]]]))))


(defn modal-create-user []
  (let [tr              (subscribe [::i18n-subs/tr])
        open-modal      (subscribe [::subs/open-modal])
        error-message   (subscribe [::subs/error-message])
        success-message (subscribe [::subs/success-message])
        loading?        (subscribe [::subs/loading?])
        form-data       (subscribe [::subs/form-data])
        form-spec       (subscribe [::subs/form-spec])]
    (fn []
      (let [email-encoded-uri (-> @form-data :email js/encodeURI)
            submit-fn         #(dispatch [::events/submit {:close-modal  false
                                                           :error-msg    (@tr [:error-occured])
                                                           :success-msg  (@tr [:invitation-email-success-msg])
                                                           :redirect-url (str @config/path-prefix
                                                                              "/reset-password"
                                                                              "?invited-user="
                                                                              email-encoded-uri)}])
            form-valid?       (s/valid? @form-spec @form-data)
            {:keys [email]} @form-data]

        [ui/Modal
         {:id        "modal-create-user"
          :size      :tiny
          :open      (= @open-modal :invite-user)
          :closeIcon true
          :on-close  #(dispatch [::events/close-modal])}

         [ui/ModalHeader (@tr [:invite-user])]

         [ui/ModalContent

          (when @error-message
            [ui/Message {:negative  true
                         :size      "tiny"
                         :onDismiss #(dispatch [::events/set-error-message nil])}
             [ui/MessageHeader (@tr [:error-occured])]
             [:p @error-message]])

          (when @success-message
            [ui/Message {:negative  false
                         :size      "tiny"
                         :onDismiss #(dispatch [::events/set-success-message nil])}
             [ui/MessageHeader (@tr [:success])]
             [:p @success-message]])

          [ui/Form {:on-key-press (partial forms-utils/on-return-key
                                           #(when form-valid?
                                              (submit-fn)))}
           [ui/FormInput {:name          "email"
                          :placeholder   "user@example.com"
                          :icon          "mail"
                          :fluid         false
                          :icon-position "left"
                          :error         (and (some? email)
                                              (not (s/valid? ::spec/email email)))
                          :required      true
                          :auto-focus    true
                          :auto-complete "on"
                          :on-blur       (ui-callback/input-callback
                                           #(dispatch [::events/update-form-data :email %]))}]]

          [:div {:style {:padding "10px 0"}} (@tr [:invite-user-inst])]]

         [ui/ModalActions
          [uix/Button
           {:text     (@tr [:invite-user])
            :positive true
            :loading  @loading?
            :disabled (not form-valid?)
            :on-click submit-fn}]]]))))


(defn modal-signup []
  [generic-modal "modal-signup-id" :signup signup-form-container (submit-signup-opts)])


(defn authn-dropdown-menu
  []
  (let [tr                   (subscribe [::i18n-subs/tr])
        user                 (subscribe [::subs/user])
        sign-out-fn          #(dispatch [::events/logout])
        create-user-fn       #(do
                                (dispatch [::events/set-form-id utils/user-tmpl-email-invitation])
                                (dispatch [::events/open-modal :invite-user]))
        logged-in?           (boolean @user)

        invitation-template? (subscribe [::subs/user-template-exist? utils/user-tmpl-email-invitation])]

    [ui/DropdownMenu

     (when logged-in?
       [:<>
        [ui/DropdownItem
         {:key      "sign-out"
          :text     (@tr [:logout])
          :icon     "sign out"
          :on-click sign-out-fn}]
        [ui/DropdownDivider]

        (when @invitation-template?
          [:<>
           [ui/DropdownItem
            {:key      "invite"
             :text     (@tr [:invite-user])
             :icon     "user add"
             :on-click create-user-fn}]
           [ui/DropdownDivider]])])

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
  (let [tr               (subscribe [::i18n-subs/tr])
        user             (subscribe [::subs/user])
        profile-fn       #(dispatch [::history-events/navigate "profile"])
        logged-in?       (boolean @user)
        signup-template? (subscribe [::subs/user-template-exist? utils/user-tmpl-email-password])
        dropdown-menu    [ui/Dropdown {:inline    true
                                       :button    true
                                       :pointing  "top right"
                                       :className "icon"}
                          (authn-dropdown-menu)]]
    [:<>
     (if logged-in?
       [ui/ButtonGroup {:primary true}
        [ui/Button {:on-click profile-fn}
         [ui/Icon {:name "user"}]
         (general-utils/truncate @user)]
        dropdown-menu]
       [:div
        (when @signup-template?
          [:span {:style    {:padding-right "10px"
                             :cursor        "pointer"}
                  :on-click #(dispatch [::history-events/navigate "sign-up"])}
           [ui/Icon {:name "signup"}]
           (@tr [:sign-up])])
        [ui/ButtonGroup {:primary true}
         [ui/Button {:on-click #(dispatch [::history-events/navigate "sign-in"])}
          [ui/Icon {:name "sign in"}]
          (@tr [:login])]
         dropdown-menu]])
     [modal-login]
     [modal-reset-password]
     [modal-signup]
     [modal-create-user]]))
