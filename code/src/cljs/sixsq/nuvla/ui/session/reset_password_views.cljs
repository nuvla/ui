(ns sixsq.nuvla.ui.session.reset-password-views
  (:require
    [cljs.spec.alpha :as s]
    [form-validator.core :as fv]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.session.components :as comp]
    [sixsq.nuvla.ui.session.events :as events]
    [sixsq.nuvla.ui.session.utils :as utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.spec :as us]))

;; VALIDATION SPEC
(s/def ::username us/nonblank-string)
(s/def ::new-password us/acceptable-password?)
(s/def ::password-repeat us/nonblank-string)

(s/def ::session-template-password-reset
  (s/keys :req-un [::username
                   ::new-password
                   ::password-repeat]))

;; VALIDATION SPEC
(s/def ::email (s/and string? us/email?))
(s/def ::password us/acceptable-password?)
(s/def ::password-repeat us/nonblank-string)

(s/def ::user-template-email-password
  (s/keys :req-un [::username
                   ::password
                   ::password-repeat]))


(defn password-repeat-check [form name]
  (let [password        (get-in @form [:names->value :new-password])
        password-repeat (get-in @form [:names->value name])]
    (when-not (= password password-repeat)
      [:password-repeat :password-not-equal])))


(defn Form
  []
  (let [query-params (subscribe [::main-subs/nav-query-params])
        invited-user (:invited-user @query-params)
        form-conf    {:form-spec         ::session-template-password-reset
                      :names->value      {:username        (or invited-user "")
                                          :new-password    ""
                                          :password-repeat ""}
                      :names->validators {:password-repeat [password-repeat-check]}}
        form         (fv/init-form form-conf)
        tr           (subscribe [::i18n-subs/tr])
        spec->msg    {::username       (@tr [:should-not-be-empty])
                      ::new-password   (@tr [:password-constraint])
                      :password-repeat (@tr [:passwords-doesnt-match])}]
    (fn []
      [comp/RightPanel
       {:title       (if invited-user (@tr [:accept]) (@tr [:reset]))
        :title-bold  (if invited-user (@tr [:invitation]) (@tr [:password]))
        :FormFields  [:<>
                      [ui/FormInput {:name          :username
                                     :label         (@tr [:username])
                                     :required      true
                                     :auto-focus    true
                                     :default-value (or invited-user "")
                                     :auto-complete "off"
                                     :on-change     (partial fv/event->names->value! form)
                                     :on-blur       (partial fv/event->show-message form)
                                     :error         (fv/?show-message form :username spec->msg)}]
                      [ui/FormGroup {:widths 2}
                       [ui/FormInput {:name          :new-password
                                      :icon          "key"
                                      :icon-position "left"
                                      :required      true
                                      :auto-complete "new-password"
                                      :label         (@tr [:new-password])
                                      :type          "password"
                                      :on-change     (partial fv/event->names->value! form)
                                      :on-blur       (partial fv/event->show-message form)
                                      :error         (fv/?show-message form
                                                                       :new-password spec->msg)}]
                       [ui/FormInput {:name      :password-repeat
                                      :required  true
                                      :label     (@tr [:password-repeat])
                                      :type      "password"
                                      :on-change (partial fv/event->names->value! form)
                                      :on-blur   (partial fv/event->show-message form)
                                      :error     (fv/?show-message form
                                                                   :password-repeat spec->msg)}]]]
        :submit-text (@tr [(if invited-user :set-password :reset-password)])
        :submit-fn   #(when (fv/validate-form-and-show? form)
                        (dispatch [::events/submit utils/session-tmpl-password-reset
                                   (-> @form
                                       :names->value
                                       (dissoc :password-repeat))
                                   {:success-msg (@tr [:validation-email-success-msg])}]))}])))
