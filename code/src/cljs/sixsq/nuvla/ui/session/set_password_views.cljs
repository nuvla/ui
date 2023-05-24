(ns sixsq.nuvla.ui.session.set-password-views
  (:require [cljs.spec.alpha :as s]
            [form-validator.core :as fv]
            [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.routing.subs :as route-subs]
            [sixsq.nuvla.ui.session.components :as comp]
            [sixsq.nuvla.ui.session.events :as events]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.spec :as us]
            [sixsq.nuvla.ui.utils.icons :as icons]))

;; VALIDATION SPEC
(s/def ::new-password us/acceptable-password?)
(s/def ::password-repeat us/nonblank-string)

(s/def ::password-set
  (s/keys :req-un [::new-password
                   ::password-repeat]))

;; VALIDATION SPEC
(s/def ::password us/acceptable-password?)
(s/def ::password-repeat us/nonblank-string)


(defn password-repeat-check [form name]
  (let [password        (get-in @form [:names->value :new-password])
        password-repeat (get-in @form [:names->value name])]
    (when-not (= password password-repeat)
      [:password-repeat :password-not-equal])))


(defn Form
  []
  (let [tr           (subscribe [::i18n-subs/tr])
        query-params (subscribe [::route-subs/nav-query-params])
        {:keys [username callback type]} @query-params
        invitation?  (= type "invitation")
        form-conf    {:form-spec         ::password-set
                      :names->value      {:new-password    ""
                                          :password-repeat ""}
                      :names->validators {:password-repeat [password-repeat-check]}}
        form         (fv/init-form form-conf)
        spec->msg    {::new-password   (@tr [:password-constraint])
                      :password-repeat (@tr [:passwords-doesnt-match])}]
    (fn []
      [comp/RightPanel
       {:title       (if invitation? (@tr [:accept]) (@tr [:reset]))
        :title-bold  (if invitation? (@tr [:invitation]) (@tr [:password]))
        :FormFields  [:<>
                      [ui/FormInput {:name          :username
                                     :label         (@tr [:username])
                                     :default-value username
                                     :read-only     true}]
                      [ui/FormGroup {:widths 2}
                       [ui/FormInput {:name          :new-password
                                      :icon          icons/i-key
                                      :icon-position "left"
                                      :required      true
                                      :auto-complete "new-password"
                                      :auto-focus    true
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
        :submit-text (@tr [:set-password])
        :submit-fn   #(when (fv/validate-form-and-show? form)
                        (dispatch [::events/set-password callback (:names->value @form)]))}])))
