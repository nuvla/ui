(ns sixsq.nuvla.ui.session.sign-up-views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.authn.subs :as authn-subs]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.spec :as us]
    [sixsq.nuvla.ui.authn.events :as authn-events]
    [cljs.spec.alpha :as s]
    [sixsq.nuvla.ui.session.components :as comp]
    [form-validator.core :as fv]
    [clojure.string :as str]))

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


(defn Sign-up-form
  []
  (let [form-conf                      {:form-spec         ::user-template-email-password
                                        :names->validators {:password-repeat [password-repeat-check]}}
        form                           (fv/init-form form-conf)
        tr                             (subscribe [::i18n-subs/tr])
        spec->msg                      {::email          (@tr [:email-invalid-format])
                                        ::password       (@tr [:password-constraint])
                                        :password-repeat (@tr [:passwords-doesnt-match])}
        server-redirect-uri            (subscribe [::authn-subs/server-redirect-uri])
        callback-message-on-validation (js/encodeURI "signup-validation-success")]
    (fn []
      [comp/RightPanel
       {:title        "Create an  "
        :title-bold   "Account"
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
                                       :error     (fv/?show-message form :password-repeat spec->msg)}]]]
        :submit-text  (@tr [:sign-up])
        :submit-fn    #(when (fv/validate-form-and-show? form)
                         (dispatch [::authn-events/submit2 "user-template/email-password"
                                    (-> @form
                                        :names->value
                                        (dissoc :password-repeat))
                                    {:success-msg  (@tr [:validation-email-success-msg])
                                     :redirect-url (str @server-redirect-uri "?message="
                                                        callback-message-on-validation)}]))
        :ExtraContent [:div {:style {:margin-top 70
                                     :color      "grey"}} "or use your github account "
                       [ui/Button {:style    {:margin-left 10}
                                   :circular true
                                   :basic    true
                                   :class    "icon"}
                        [ui/Icon {:name "github"
                                  :size "large"}]]]}])))
