(ns sixsq.nuvla.ui.session.sign-up-views
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [form-validator.core :as fv]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-fx]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.session.components :as comp]
    [sixsq.nuvla.ui.session.events :as session-events]
    [sixsq.nuvla.ui.session.subs :as session-subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.spec :as us]))

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
        server-redirect-uri        (subscribe [::session-subs/server-redirect-uri])
        callback-msg-on-validation (js/encodeURI "signup-validation-success")
        github-template?           (subscribe [::session-subs/user-template-exist?
                                               "user-template/nuvla"])]
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
                         (dispatch [::session-events/submit "user-template/email-password"
                                    (-> @form
                                        :names->value
                                        (dissoc :password-repeat))
                                    {:success-msg  (@tr [:validation-email-success-msg])
                                     :redirect-url (str @server-redirect-uri "?message="
                                                        callback-msg-on-validation)}]))
        :ExtraContent (when @github-template?
                        [ui/Form {:action (str @cimi-fx/NUVLA_URL "/api/user")
                                  :method "post"
                                  :style  {:margin-top 70
                                           :color      "grey"}}
                         "or use your github account "
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


(defn Presentation
  []
  [comp/LeftPanel
   {:title           "Nuvla"
    :subtitle        "Start immediately deploying apps containers in one button click."
    :p1              "Start jouney with us"
    :p2              (str "Provide a secured edge to cloud (and back) management platform "
                          "that enabled near-data AI for connected world use cases.")
    :button-text     "Sign in"
    :button-callback #(dispatch [::history-events/navigate "sign-in"])}])
