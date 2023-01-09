(ns sixsq.nuvla.ui.session.reset-password-views
  (:require [cljs.spec.alpha :as s]
            [form-validator.core :as fv]
            [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.session.components :as comp]
            [sixsq.nuvla.ui.session.events :as events]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.spec :as us]))

;; VALIDATION SPEC
(s/def ::username us/nonblank-string)

(s/def ::hook-reset-password
  (s/keys :req-un [::username]))

;; VALIDATION SPEC
(s/def ::email (s/and string? us/email?))


(defn Form
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        form-conf {:form-spec    ::hook-reset-password
                   :names->value {:username ""}}
        form      (fv/init-form form-conf)
        spec->msg {::username (@tr [:should-not-be-empty])}]
    (fn []
      [comp/RightPanel
       {:title       (@tr [:reset])
        :title-bold  (@tr [:password])
        :FormFields  [:<>
                      [ui/FormInput {:name          :username
                                     :label         (@tr [:username])
                                     :required      true
                                     :auto-focus    true
                                     :auto-complete "off"
                                     :on-change     (partial fv/event->names->value! form)
                                     :on-blur       (partial fv/event->show-message form)
                                     :error         (fv/?show-message form :username spec->msg)}]]
        :submit-text (@tr [:reset-password])
        :submit-fn   #(when (fv/validate-form-and-show? form)
                        (dispatch [::events/reset-password (:names->value @form)]))}])))
