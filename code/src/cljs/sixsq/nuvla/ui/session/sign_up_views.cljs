(ns sixsq.nuvla.ui.session.sign-up-views
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [form-validator.core :as fv]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-fx]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.profile.events :as profile-events]
    [sixsq.nuvla.ui.profile.subs :as profile-subs]
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
        server-redirect-uri        (subscribe [::subs/server-redirect-uri])
        callback-msg-on-validation (js/encodeURI "signup-validation-success")
        github-template?           (subscribe [::subs/user-template-exist?
                                               "user-template/nuvla"])
        stripe                     (subscribe [::main-subs/stripe])
        pricing-catalogue          (subscribe [::profile-subs/pricing-catalogue])
        create-customer            (r/atom false)
        form-customer-conf         {:form-spec    ::profile-views/customer
                                    :names->value {:fullname       ""
                                                   :street-address ""
                                                   :city           ""
                                                   :country        ""
                                                   :postal-code    ""}}
        form-customer              (fv/init-form form-customer-conf)]
    (dispatch [::profile-events/get-pricing-catalogue])
    (fn []
      [comp/RightPanel
       {:title        (@tr [:create-an])
        :title-bold   (@tr [:account])
        :FormFields   [:<>
                       [ui/FormInput {:name          :email
                                      :label         "Email"
                                      :required      true
                                      :icon          "at"
                                      :icon-position "left"
                                      :auto-focus    true
                                      :auto-complete "off"
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

                       (when (and @stripe @create-customer)
                         [:<>
                          [ui/FormCheckbox {:label     "Start my trial now"
                                            :on-change (ui-callback/checked
                                                         #(reset! create-customer %))}]
                          [profile-views/CustomerFormFields form-customer]])]
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
                                 opts {:success-msg  (@tr [:validation-email-success-msg])
                                       :redirect-url (str @server-redirect-uri "?message="
                                                          callback-msg-on-validation)}]
                             (if @create-customer
                               (let [customer  (-> form-customer
                                                   profile-views/customer-form->customer
                                                   (assoc :subscription
                                                          (profile-events/catalogue->subscription
                                                            @pricing-catalogue)))
                                     data-cust (assoc data :customer customer)]
                                 (if (:payment-method customer)
                                   (dispatch [::events/create-payment-method
                                              utils/user-tmpl-email-password data-cust opts])
                                   (dispatch [::events/submit utils/user-tmpl-email-password
                                              data-cust opts])))
                               (dispatch [::events/submit
                                          utils/user-tmpl-email-password data opts])))))
        :ExtraContent (when @github-template?
                        [ui/Form {:action (str @cimi-fx/NUVLA_URL "/api/user")
                                  :method "post"
                                  :style  {:margin-top 70
                                           :color      "grey"}}
                         (@tr [:or-use-github-accout])
                         [:input {:hidden        true
                                  :name          "href"
                                  :default-value "user-template/nuvla"}]
                         [:input {:hidden        true
                                  :name          "redirect-url"
                                  :default-value @server-redirect-uri}]
                         [ui/Button {:style    {:margin-left 10}
                                     :circular true
                                     :basic    true
                                     :type     "submit"
                                     :class    "icon"}
                          [ui/Icon {:name "github"
                                    :size "large"}]]])}])))
