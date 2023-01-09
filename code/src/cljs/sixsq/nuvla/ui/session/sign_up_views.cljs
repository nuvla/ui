(ns sixsq.nuvla.ui.session.sign-up-views
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [form-validator.core :as fv]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-fx]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.profile.views :as profile-views]
            [sixsq.nuvla.ui.session.components :as comp]
            [sixsq.nuvla.ui.session.events :as events]
            [sixsq.nuvla.ui.session.subs :as subs]
            [sixsq.nuvla.ui.session.utils :as utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.spec :as us]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

;; VALIDATION SPEC
(s/def ::email (s/and string? us/email?))
(s/def ::password us/acceptable-password?)
(s/def ::password-repeat us/nonblank-string)

(s/def ::user-template-email-password
  (s/keys :req-un [::email
                   ::password
                   ::password-repeat]))


(defn password-repeat-check [form name]
  (let [password        (get-in @form [:names->value :password])
        password-repeat (get-in @form [:names->value name])]
    (when-not (= password password-repeat)
      [:password-repeat :password-not-equal])))


(defn Form
  []
  (let [form-conf                  {:form-spec         ::user-template-email-password
                                    :names->validators {:password-repeat [password-repeat-check]}
                                    :names->value      {:email           ""
                                                        :password        ""
                                                        :password-repeat ""}}
        form                       (fv/init-form form-conf)
        tr                         (subscribe [::i18n-subs/tr])
        spec->msg                  {::email          (@tr [:email-invalid-format])
                                    ::password       (@tr [:password-constraint])
                                    :password-repeat (@tr [:passwords-doesnt-match])}
        callback-msg-on-validation (js/encodeURI "signup-validation-success")
        server-redirect-uri        (subscribe [::subs/server-redirect-uri])
        resource-url               (str @cimi-fx/NUVLA_URL "/api/user")
        github-user-tmpl           "user-template/nuvla"
        geant-user-tmpl            "user-template/geant"
        icrc-user-tmpl             "user-template/icrc"
        github-template?           (subscribe [::subs/user-template-exist? github-user-tmpl])
        geant-template?            (subscribe [::subs/user-template-exist? geant-user-tmpl])
        icrc-template?             (subscribe [::subs/user-template-exist? icrc-user-tmpl])
        stripe                     (subscribe [::main-subs/stripe])
        pricing-url                (subscribe [::main-subs/config :pricing-url])
        create-customer            (r/atom false)
        form-customer-conf         {:form-spec    ::profile-views/customer
                                    :names->value {:fullname       ""
                                                   :street-address ""
                                                   :city           ""
                                                   :country        ""
                                                   :postal-code    ""}}
        form-customer              (fv/init-form form-customer-conf)]
    (fn []
      [comp/RightPanel
       {:title        (@tr [:create-an])
        :title-bold   (@tr [:account])
        :FormFields   [:<>
                       [ui/FormInput {:name          :email
                                      :label         "Email"
                                      :required      true
                                      :icon          "envelope"
                                      :icon-position "left"
                                      :auto-focus    true
                                      :auto-complete "username"
                                      :on-change     (partial fv/event->names->value! form)
                                      :on-blur       (partial fv/event->show-message form)
                                      :error         (fv/?show-message form :email spec->msg)}]
                       [ui/FormGroup {:widths 2}
                        [ui/FormInput {:name          :password
                                       :icon          "key"
                                       :icon-position "left"
                                       :required      true
                                       :auto-complete "new-password"
                                       :label         (str/capitalize (@tr [:password]))
                                       :type          "password"
                                       :on-change     (partial fv/event->names->value! form)
                                       :on-blur       (partial fv/event->show-message form)
                                       :error         (fv/?show-message form :password spec->msg)}]
                        [ui/FormInput {:name      :password-repeat
                                       :required  true
                                       :label     (str/capitalize (@tr [:password-repeat]))
                                       :type      "password"
                                       :on-change (partial fv/event->names->value! form)
                                       :on-blur   (partial fv/event->show-message form)
                                       :error     (fv/?show-message
                                                    form :password-repeat spec->msg)}]]

                       (when @stripe
                         [ui/FormGroup {:inline true}
                          [ui/FormCheckbox {:label     (@tr [:start-trial-now])
                                            :on-change (ui-callback/checked
                                                         #(reset! create-customer %))}]
                          (when @pricing-url
                            [:span "(" (@tr [:see]) " " [:a {:href @pricing-url, :target "_blank"}
                                                         (@tr [:pricing])] ")"])])
                       (when @create-customer
                         [profile-views/CustomerFormFields form-customer])]
        :submit-text  (@tr [:sign-up])
        :submit-fn    #(let [form-signup-valid?   (fv/validate-form-and-show? form)
                             form-customer-valid? (if @create-customer
                                                    (fv/validate-form-and-show? form-customer)
                                                    true)
                             form-valid?          (and form-signup-valid? form-customer-valid?)]
                         (when form-valid?
                           (let [data (-> @form
                                          :names->value
                                          (dissoc :password-repeat))
                                 opts {:success-msg  :validation-email-success-msg
                                       :navigate-to  "sign-in"
                                       :redirect-url (str @server-redirect-uri "?message="
                                                          callback-msg-on-validation)}]
                             (if @create-customer
                               (let [customer  (-> form-customer
                                                   profile-views/customer-form->customer
                                                   (assoc :subscription? true))
                                     data-cust (assoc data :customer customer)]
                                 (dispatch [::events/submit utils/user-tmpl-email-password
                                            data-cust opts]))
                               (dispatch [::events/submit
                                          utils/user-tmpl-email-password data opts])))))
        :ExtraContent [:div {:style {:margin-top 100}}
                       (when (or @github-template?
                                 @geant-template?
                                 @icrc-template?)
                         (@tr [:sign-up-with]))
                       [:span
                        (when @github-template?
                          [comp/SignExternal
                           {:resource-url        resource-url
                            :href                github-user-tmpl
                            :icon                :github
                            :server-redirect-uri @server-redirect-uri}])
                        (when @geant-template?
                          [comp/SignExternal
                           {:resource-url resource-url
                            :href         geant-user-tmpl
                            :icon         :geant}])
                        (when @icrc-template?
                          [comp/SignExternal
                           {:resource-url resource-url
                            :href         icrc-user-tmpl
                            :icon         :icrc}])]]
        }])))
