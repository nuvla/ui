(ns sixsq.nuvla.ui.authn.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as reagent]
    [sixsq.nuvla.ui.authn.events :as authn-events]
    [sixsq.nuvla.ui.authn.subs :as authn-subs]
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
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn form-component
  "Provides a single element of a form. This should provide a reasonable
   control for each defined type, but this initial implementation just provides
   either a text or password field."
  [[param-name {:keys [data type display-name mandatory autocomplete] :as param}]]
  (case type
    "hidden" [ui/FormField [:input {:name param-name :type "hidden" :value (or data "")}]]
    "password" [ui/FormInput {:name         param-name
                              :type         type
                              :placeholder  display-name
                              :icon         "lock"
                              :iconPosition "left"
                              :required     mandatory
                              :auto-complete (or autocomplete "off")}]
    [ui/FormInput {:name         param-name
                   :type         type
                   :placeholder  display-name
                   :icon         "user"
                   :iconPosition "left"
                   :required     mandatory
                   :auto-complete (or autocomplete "off")}]))


(defn dropdown-method-option
  [{:keys [id name] :as method}]
  {:key id, :text name, :value id})


(defn reset-password [tr]
  [[:div {:style {:text-align "right"}}
    [:a {:style    {:cursor "pointer"}
         :on-click (fn []
                     (dispatch [::authn-events/close-modal])
                     (dispatch [::authn-events/set-selected-method-group nil])
                     (dispatch [::authn-events/set-form-id nil])
                     (dispatch [::authn-events/open-modal :reset-password]))}
     (@tr [:forgot-password])]]])


(defn form-id->resource-type
  [form-id]
  (cond
    (str/starts-with? form-id "user-template/") :user
    (str/starts-with? form-id "session-template/") :session))


(defn authn-method-form
  "Renders the form for a particular authentication (login or sign up) method.
   The fields are taken from the method description."
  [methods]
  (let [form-id (subscribe [::authn-subs/form-id])
        form-data (subscribe [::authn-subs/form-data @form-id])
        tr (subscribe [::i18n-subs/tr])]
    (fn [methods]
      ^{:key @form-id}
      (let [dropdown? (> (count methods) 1)
            method (u/select-method-by-id @form-id methods)
            resource-metadata (subscribe [::docs-subs/document method])
            inputs-method (->> (:attributes @resource-metadata)
                               (filter (fn [{:keys [consumer-mandatory group] :as attribute}]
                                         (and (not (#{"metadata" "acl"} group))
                                              consumer-mandatory)))
                               (sort-by :order))
            dropdown-options (map dropdown-method-option methods)
            password-method? (= "password" (:method method))]

        (vec
          (concat
            [ui/Form {:id           (or @form-id "authn-form-placeholder-id")
                      :on-key-press (partial forms-utils/on-return-key
                                             #(when @form-id
                                                (dispatch [::authn-events/submit
                                                           (form-id->resource-type @form-id) @form-id])))}]
            [(vec (concat [ui/Segment {:style {:height     "35ex"
                                               :overflow-y "auto"}}
                           (when dropdown?
                             [ui/FormDropdown
                              {:options       dropdown-options
                               :value         @form-id
                               :fluid         true
                               :selection     true
                               :close-on-blur true
                               :on-change     (ui-callback/dropdown ::authn-events/set-form-id)}])]

                          (mapv (fn [{vscope :vscope param-name :name :as input-method}]
                                  (let [value (get @form-data param-name)
                                        input-method-updated (cond-> input-method
                                                                     value (assoc-in [:vscope :value] value))]
                                    (dispatch [::authn-events/update-form-data @form-id
                                               param-name (or value
                                                              (:value vscope)
                                                              (:default vscope))])
                                    (forms/form-field (fn [form-id name value]
                                                        (dispatch [::authn-events/update-form-data form-id name value])
                                                        ) @form-id input-method-updated))) inputs-method)
                          (when password-method?
                            (reset-password tr))))]))))))


(defn login-method-form
  [[_ methods]]
  [authn-method-form methods])


(defn signup-method-form
  [[_ methods]]
  [authn-method-form methods])


(defn authn-method-group-option
  [[group methods]]
  (let [{:keys [icon]} (first methods)
        option-label (reagent/as-element [:span [ui/Icon {:name icon}] group])]
    {:text    option-label
     :value   group
     :content option-label}))


(defn authn-method-dropdown
  [method-groups]
  (let [selected-method-group (subscribe [::authn-subs/selected-method-group])]
    (fn [method-groups]
      (let [default (ffirst method-groups)
            default-form-id (-> method-groups first second first :id)
            options (mapv authn-method-group-option method-groups)]

        (when (nil? @selected-method-group)
          (dispatch [::authn-events/set-selected-method-group default])
          (dispatch [::authn-events/set-form-id default-form-id]))

        [ui/Dropdown {:fluid     true
                      :selection true
                      :loading   (nil? @selected-method-group)
                      :value     @selected-method-group
                      :options   options
                      :on-change (ui-callback/value
                                   (fn [group-id]
                                     (dispatch [::authn-events/set-selected-method-group group-id])
                                     (let [form-id (-> (u/select-group-methods-by-id group-id method-groups)
                                                       first
                                                       :id)]
                                       (dispatch [::authn-events/set-form-id form-id]))))}]))))


(defn authn-form-container
  "Container that holds all of the authentication (login or sign up) forms."
  [collection-kw failed-kw method-form-fn]
  (let [template-href (api-utils/collection-template-href collection-kw)
        templates (subscribe [::api-subs/collection-templates template-href])
        tr (subscribe [::i18n-subs/tr])
        error-message (subscribe [::authn-subs/error-message])
        selected-method-group (subscribe [::authn-subs/selected-method-group])]
    (fn [collection-kw failed-kw group-form-fn]
      (let [authn-method-groups (u/grouped-authn-methods @templates)
            selected-authn-method-group (some->> authn-method-groups
                                                 (filter #(-> % first (= @selected-method-group)))
                                                 first)]

        [ui/Segment {:basic true}
         (when @error-message
           [ui/Message {:negative  true
                        :size      "tiny"
                        :onDismiss #(dispatch [::authn-events/clear-error-message])}
            [ui/MessageHeader (@tr [failed-kw])]
            [:p @error-message]])

         [authn-method-dropdown authn-method-groups]
         [ui/Divider]
         [group-form-fn selected-authn-method-group]]))))


(defn login-form-container
  []
  [authn-form-container :session :login-failed login-method-form])


(defn signup-form-container
  []
  [authn-form-container :user :signup-failed signup-method-form])


(defn switch-panel-link
  [modal-kw]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [modal-kw]
      (let [other-modal (case modal-kw
                          :login :signup
                          :reset-password :login
                          :signup :login)
            f (fn []
                (dispatch [::authn-events/close-modal])
                (dispatch [::authn-events/set-selected-method-group nil])
                (dispatch [::authn-events/set-form-id nil])
                (dispatch [::authn-events/open-modal other-modal]))]
        (case modal-kw
          :login [:span (@tr [:no-account?]) " "
                  [:a {:on-click f :style {:cursor "pointer"}} (str (@tr [:signup-link]))]]
          :reset-password [:span (@tr [:already-registered?]) " "
                           [:a {:on-click f :style {:cursor "pointer"}} (str (@tr [:login-link]))]]
          :signup [:span (@tr [:already-registered?]) " "
                   [:a {:on-click f :style {:cursor "pointer"}} (str (@tr [:login-link]))]])))))


(defn reset-password-modal
  []
  (let [open-modal (subscribe [::authn-subs/open-modal])
        error-message (subscribe [::authn-subs/error-message])
        success-message (subscribe [::authn-subs/success-message])
        loading? (subscribe [::authn-subs/loading?])
        tr (subscribe [::i18n-subs/tr])
        empty-reset-state {:username            nil
                           :new-password        nil
                           :repeat-new-password nil}
        reset-form (reagent/atom empty-reset-state)]
    (fn []
      (let [{:keys [username
                    new-password
                    repeat-new-password]} @reset-form
            error-password? (not= new-password repeat-new-password)]
        [ui/Modal
         {:id        "modal-reset-password-id"
          :size      :tiny
          :open      (= @open-modal :reset-password)
          :closeIcon true
          :on-close  (fn []
                       (dispatch [::authn-events/close-modal])
                       (reset! reset-form empty-reset-state))}

         [ui/ModalHeader (@tr [:reset-password])]

         [ui/ModalContent

          (when @error-message
            [ui/Message {:negative  true
                         :size      "tiny"
                         :onDismiss #(dispatch [::authn-events/clear-error-message])}
             [ui/MessageHeader (@tr [:reset-password-error])]
             [:p @error-message]])

          (when @success-message
            [ui/Message {:negative  false
                         :size      "tiny"
                         :onDismiss #(dispatch [::authn-events/clear-success-message])}
             [ui/MessageHeader (@tr [:reset-password-success])]
             [:p @success-message]])

          [ui/Form {:on-key-press (partial forms-utils/on-return-key
                                           #(dispatch [::authn-events/reset-password username new-password]))}
           [ui/FormInput {:name          "username"
                          :type          "text"
                          :placeholder   "Username"
                          :icon          "user"
                          :fluid         false
                          :iconPosition  "left"
                          :required      true
                          :auto-focus    true
                          :auto-complete "on"
                          :on-change     (ui-callback/value #(swap! reset-form assoc :username %))}]

           [ui/FormGroup {:widths 2}
            [ui/FormInput {:name          "password"
                           :type          "password"
                           :placeholder   "New password"
                           :icon          "key"
                           :iconPosition  "left"
                           :required      true
                           :error         error-password?
                           :auto-complete "off"
                           :on-change     (ui-callback/value #(swap! reset-form assoc :new-password %))}]

            [ui/FormInput {:name          "password"
                           :type          "password"
                           :placeholder   "Repeat new password"
                           :required      true
                           :error         error-password?
                           :auto-complete "off"
                           :on-change     (ui-callback/value #(swap! reset-form assoc :repeat-new-password %))}]]]

          [:div {:style {:padding "10px 0"}} (@tr [:reset-password-inst])]]

         [ui/ModalActions
          [switch-panel-link :reset-password]
          [uix/Button
           {:text     (@tr [:reset-password])
            :positive true
            :loading  @loading?
            :disabled (or (str/blank? username) (str/blank? new-password) error-password?)
            :on-click #(dispatch [::authn-events/reset-password username new-password])}]]]))))


(defn authn-modal
  "Modal that holds the authentication (login or sign up) forms."
  [id modal-kw form-fn]
  (let [tr (subscribe [::i18n-subs/tr])
        open-modal (subscribe [::authn-subs/open-modal])
        form-id (subscribe [::authn-subs/form-id])]
    (fn [id modal-kw form-fn]
      [ui/Modal
       {:id        id
        :size      :tiny
        :open      (= @open-modal modal-kw)
        :closeIcon true
        :on-close  #(dispatch [::authn-events/close-modal])}

       [ui/ModalHeader (@tr [modal-kw])]

       [ui/ModalContent
        [form-fn]]

       [ui/ModalActions
        [switch-panel-link modal-kw]
        [uix/Button
         {:text     (@tr [modal-kw])
          :positive true
          :disabled (nil? @form-id)
          :on-click #(dispatch [::authn-events/submit (form-id->resource-type @form-id) @form-id])}]]])))


(defn modal-login []
  [authn-modal "modal-login-id" :login login-form-container])


(defn modal-reset-password []
  [reset-password-modal])


(defn modal-signup []
  [authn-modal "modal-signup-id" :signup signup-form-container])


(defn authn-menu
  "Provides either a login or user dropdown depending on whether the user has
   an active session. The login button will bring up a modal dialog."
  []
  (let [tr (subscribe [::i18n-subs/tr])
        user (subscribe [::authn-subs/user])
        template-href (api-utils/collection-template-href :user)
        user-templates (subscribe [::api-subs/collection-templates (keyword template-href)])]
    (let [profile-fn #(history-utils/navigate "profile")
          sign-out-fn (fn []
                        (dispatch [::authn-events/logout])
                        (dispatch [::history-events/navigate "welcome"]))
          login-fn #(dispatch [::authn-events/open-modal :login])
          logged-in? (boolean @user)
          sign-up-ok? (get-in @user-templates [:templates (keyword (str template-href "/self-registration"))])]

      [ui/ButtonGroup {:primary true}
       [ui/Button {:aria-label (if logged-in? "profile" "login")
                   :on-click   (if logged-in? profile-fn login-fn)}
        [ui/Icon {:name (if logged-in? "user" "sign in")}]
        (if logged-in? (utils/truncate @user) (@tr [:login]))]
       [ui/Dropdown {:inline    true
                     :button    true
                     :pointing  "top right"
                     :className "icon"}
        (vec
          (concat
            [ui/DropdownMenu]

            (when logged-in?
              [[ui/DropdownItem
                {:key      "sign-out"
                 :text     (@tr [:logout])
                 :icon     "sign out"
                 :on-click sign-out-fn}]])

            (when (and sign-up-ok? (not logged-in?))
              [[ui/DropdownItem {:icon     "signup"
                                 :text     (@tr [:signup])
                                 :on-click #(dispatch [::authn-events/open-modal :signup])}]])

            (when (or logged-in? sign-up-ok?)
              [[ui/DropdownDivider]])

            [[ui/DropdownItem {:aria-label (@tr [:documentation])
                               :icon       "book"
                               :text       (@tr [:documentation])
                               :href       "https://ssdocs.sixsq.com/"
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
                                                "question%20%2D%20Not%20logged%20in")}]]))]
       [modal-login]
       [modal-reset-password]
       [modal-signup]])))

