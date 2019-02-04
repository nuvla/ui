(ns sixsq.slipstream.webui.authn.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.slipstream.webui.authn.events :as authn-events]
    [sixsq.slipstream.webui.authn.subs :as authn-subs]
    [sixsq.slipstream.webui.authn.utils :as u]
    [sixsq.slipstream.webui.cimi.subs :as api-subs]
    [sixsq.slipstream.webui.cimi.utils :as api-utils]
    [sixsq.slipstream.webui.docs.subs :as docs-subs]
    [sixsq.slipstream.webui.history.events :as history-events]
    [sixsq.slipstream.webui.history.utils :as history-utils]
    [sixsq.slipstream.webui.i18n.subs :as i18n-subs]
    [sixsq.slipstream.webui.utils.general :as utils]
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]
    [sixsq.slipstream.webui.utils.semantic-ui-extensions :as uix]
    [sixsq.slipstream.webui.utils.ui-callback :as ui-callback]
    [sixsq.slipstream.webui.utils.form-fields :as forms]
    [taoensso.timbre :as log]))


(defn form-component
  "Provides a single element of a form. This should provide a reasonable
   control for each defined type, but this initial implementation just provides
   either a text or password field."
  [[param-name {:keys [data type displayName mandatory autocomplete] :as param}]]
  (case type
    "hidden" [ui/FormField [:input {:name param-name :type "hidden" :value (or data "")}]]
    "password" [ui/FormInput {:name         param-name
                              :type         type
                              :placeholder  displayName
                              :icon         "lock"
                              :iconPosition "left"
                              :required     mandatory
                              :autoComplete (or autocomplete "off")}]
    [ui/FormInput {:name         param-name
                   :type         type
                   :placeholder  displayName
                   :icon         "user"
                   :iconPosition "left"
                   :required     mandatory
                   :autoComplete (or autocomplete "off")}]))


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


(defn authn-method-form
  "Renders the form for a particular authentication (login or sign up) method.
   The fields are taken from the method description."
  [methods collections-kw]
  (let [cep (subscribe [::api-subs/cloud-entry-point])
        server-redirect-uri (subscribe [::authn-subs/server-redirect-uri])
        form-id (subscribe [::authn-subs/form-id])
        tr (subscribe [::i18n-subs/tr])]
    (fn [methods collections-kw]
      (let [dropdown? (> (count methods) 1)
            method (u/select-method-by-id @form-id methods)
            resourceMetadata (subscribe [::docs-subs/document method])
            {:keys [baseURI collection-href]} @cep
            post-uri (str baseURI (collections-kw collection-href)) ;; FIXME: Should be part of CIMI API.
            inputs-method (conj
                            (->> (:attributes @resourceMetadata)
                                 (filter (fn [{:keys [consumerWritable consumerMandatory group] :as attribute}]
                                           (and (not (#{"metadata" "acl"} group))
                                                consumerWritable)))
                                 (sort-by :order))
                            {:name "href" :vscope {:value @form-id} :hidden true}
                            {:name "redirectURI" :vscope {:value @server-redirect-uri} :hidden true})
            dropdown-options (map dropdown-method-option methods)]

        (log/infof "creating authentication form: %s %s" (name collections-kw) @form-id)
        (vec
          (concat
            [ui/Form {:id (or @form-id "authn-form-placeholder-id"), :action post-uri, :method "post"}]

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

                          (mapv (partial forms/form-field #() @form-id) inputs-method)
                          (when (= "internal" (:method method))
                            (reset-password tr))))]))))))


(defn login-method-form
  [[_ methods]]
  [authn-method-form methods :sessions])


(defn signup-method-form
  [[_ methods]]
  [authn-method-form methods :users])


(defn authn-method-group-option
  [[group methods]]
  (let [{:keys [icon]} (first methods)
        option-label (r/as-element [:span [ui/Icon {:name icon}] group])]
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


(def username (r/atom nil))

(defn reset-password-modal
  []
  (let [open-modal (subscribe [::authn-subs/open-modal])
        cep (subscribe [::api-subs/cloud-entry-point])
        error-message (subscribe [::authn-subs/error-message])
        success-message (subscribe [::authn-subs/success-message])
        loading? (subscribe [::authn-subs/loading?])
        tr (subscribe [::i18n-subs/tr])]

    [ui/Modal
     {:id        "modal-reset-password-id"
      :size      :tiny
      :open      (= @open-modal :reset-password)
      :closeIcon true
      :on-close  (fn []
                   (dispatch [::authn-events/close-modal])
                   (dispatch [::authn-events/clear-success-message])
                   (dispatch [::authn-events/clear-error-message])
                   (reset! username nil))}

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

      [ui/FormInput {:name         "username"
                     :type         "text"
                     :placeholder  "Username"
                     :icon         "user"
                     :iconPosition "left"
                     :required     true
                     :autoComplete "off"
                     :on-change    (ui-callback/value #(reset! username %))}]

      [:div {:style {:padding "10px 0"}} (@tr [:reset-password-inst])]]

     [ui/ModalActions
      [switch-panel-link :reset-password]
      [uix/Button
       {:text     (@tr [:reset-password])
        :positive true
        :loading  @loading?
        :disabled (str/blank? @username)
        :on-click #(do
                     (.preventDefault %)
                     (dispatch [::authn-events/reset-password username]))}]]]))


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
          :on-click #(some->> @form-id
                              (.getElementById js/document)
                              (.submit))}]]])))


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


(defn ^:export open-authn-modal []
  (log/debug "dispatch open-modal for login modal")
  (dispatch [::authn-events/open-modal :login]))


(defn ^:export open-sign-up-modal []
  (log/debug "dispatch open-modal for sign up modal")
  (dispatch [::authn-events/open-modal :signup]))
