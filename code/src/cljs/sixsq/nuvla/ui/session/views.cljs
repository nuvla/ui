(ns sixsq.nuvla.ui.session.views
  (:require
    [clojure.spec.alpha :as s]
    [form-validator.core :as fv]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.config :as config]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.session.events :as events]
    [sixsq.nuvla.ui.session.reset-password-views :as reset-password-views]
    [sixsq.nuvla.ui.session.sign-in-views :as sign-in-views]
    [sixsq.nuvla.ui.session.sign-up-views :as sign-up-views]
    [sixsq.nuvla.ui.session.subs :as subs]
    [sixsq.nuvla.ui.session.utils :as utils]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.spec :as us]))


(defn LeftPanel
  []
  (let [first-path (subscribe [::main-subs/nav-path-first])]
    (case @first-path
      "sign-in" [sign-in-views/Presentation]
      "sign-up" [sign-up-views/Presentation]
      "reset-password" [reset-password-views/Presentation]
      [sign-in-views/Presentation])))


(defn RightPanel
  []
  (let [first-path (subscribe [::main-subs/nav-path-first])]
    (case @first-path
      "sign-in" [sign-in-views/Form]
      "sign-up" [sign-up-views/Form]
      "reset-password" [reset-password-views/Form]
      [sign-in-views/Form])))


(defn SessionPage
  []
  (let [session      (subscribe [::subs/session])
        query-params (subscribe [::main-subs/nav-query-params])
        tr           (subscribe [::i18n-subs/tr])
        error        (some-> @query-params :error keyword)
        message      (some-> @query-params :message keyword)]
    (when @session
      (dispatch [::history-events/navigate "welcome"]))
    (when error
      (dispatch [::events/set-error-message (@tr [(keyword error)])]))
    (when message
      (dispatch [::events/set-success-message (@tr [(keyword message)])]))
    [ui/Grid {:stackable true
              :columns   2
              :reversed  "mobile"
              :style     {:margin           0
                          :background-color "white"}}

     [ui/GridColumn {:style {:background-image    "url(/ui/images/volumlight.png)"
                             :background-size     "cover"
                             :background-position "left"
                             :background-repeat   "no-repeat"
                             :color               "white"
                             :min-height          "100vh"}}
      [LeftPanel]]
     [ui/GridColumn
      [RightPanel]]]))



;;; VALIDATION SPEC
(s/def ::email (s/and string? us/email?))

(s/def ::user-template-email-invitation
  (s/keys :req-un [::email]))


(defn modal-create-user []
  (let [tr              (subscribe [::i18n-subs/tr])
        open-modal      (subscribe [::subs/open-modal])
        error-message   (subscribe [::subs/error-message])
        success-message (subscribe [::subs/success-message])
        loading?        (subscribe [::subs/loading?])
        form-conf       {:form-spec ::user-template-email-invitation}
        form            (fv/init-form form-conf)
        spec->msg       {::email (@tr [:email-invalid-format])}]
    (fn []
      (let [submit-fn #(when (fv/validate-form-and-show? form)
                         (let [form-data     (:names->value @form)
                               email-encoded (-> form-data :email js/encodeURI)]
                           (dispatch [::events/submit "user-template/email-invitation"
                                      (:names->value @form)
                                      {:success-msg  (@tr [:invitation-email-success-msg])
                                       :redirect-url (str @config/path-prefix
                                                          "/reset-password"
                                                          "?invited-user="
                                                          email-encoded)}])))]
        [ui/Modal
         {:id        "modal-create-user"
          :size      :tiny
          :open      (= @open-modal :invite-user)
          :closeIcon true
          :on-close  #(do
                        (dispatch [::events/close-modal])
                        (reset! form (fv/init-form form-conf)))}

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

          [ui/Form
           [ui/FormInput {:name          :email
                          :label         "Email"
                          :required      true
                          :icon          "at"
                          :icon-position "left"
                          :auto-focus    true
                          :auto-complete "on"
                          :on-change     (partial fv/event->names->value! form)
                          :on-blur       (partial fv/event->show-message form)
                          :error         (fv/?show-message form :email spec->msg)}]]

          [:div {:style {:padding "10px 0"}} (@tr [:invite-user-inst])]]

         [ui/ModalActions
          [uix/Button
           {:text     (@tr [:invite-user])
            :positive true
            :loading  @loading?
            :on-click submit-fn}]]]))))


(defn authn-dropdown-menu
  []
  (let [tr                   (subscribe [::i18n-subs/tr])
        user                 (subscribe [::subs/user])
        sign-out-fn          #(dispatch [::events/logout])
        create-user-fn       #(dispatch [::events/open-modal :invite-user])
        logged-in?           (boolean @user)

        invitation-template? (subscribe [::subs/user-template-exist?
                                         utils/user-tmpl-email-invitation])]

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
                        :href       (str "mailto:support%40sixsq%2Ecom?subject=%5BSlipStream%5D%20"
                                         "Support%20question%20%2D%20Not%20logged%20in")}]]]))


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
     [modal-create-user]]))

