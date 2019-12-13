(ns sixsq.nuvla.ui.session.sign-in-views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.spec :as us]
    [sixsq.nuvla.ui.authn.events :as authn-events]
    [cljs.spec.alpha :as s]
    [sixsq.nuvla.ui.session.components :as comp]
    [form-validator.core :as fv]
    [clojure.string :as str]))

;; VALIDATION SPEC
(s/def ::username us/nonblank-string)
(s/def ::password us/nonblank-string)

(s/def ::session-template-password
  (s/keys :req-un [::username
                   ::password]))


(defn Sign-in-form
  []
  (let [form-conf {:form-spec ::session-template-password}
        form      (fv/init-form form-conf)
        tr        (subscribe [::i18n-subs/tr])
        spec->msg {::username (@tr [:should-not-be-empty])
                   ::password (@tr [:should-not-be-empty])}]
    (fn []
      [comp/RightPanel
       {:title        "Login to "
        :title-bold   "Account"
        :FormFields   [:<>
                       [ui/FormInput {:name       :username
                                      :label      (str/capitalize (@tr [:username]))
                                      :auto-focus true
                                      :on-change  (partial fv/event->names->value! form)
                                      :on-blur    (partial fv/event->show-message form)
                                      :error      (fv/?show-message form :username spec->msg)}]
                       [ui/FormInput {:name      :password
                                      :label     (str/capitalize (@tr [:password]))
                                      :type      "password"
                                      :on-change (partial fv/event->names->value! form)
                                      :on-blur   (partial fv/event->show-message form)
                                      :error     (fv/?show-message form :password spec->msg)}]]
        :submit-text  (@tr [:sign-in])
        :submit-fn    #(when (fv/validate-form-and-show? form)
                         (dispatch [::authn-events/submit2 "session-template/password" (:names->value @form)]))
        :ExtraContent [:div {:style {:margin-top 70
                                     :color      "grey"}} "or use your github account "
                       [ui/Button {:style    {:margin-left 10}
                                   :circular true
                                   :basic    true
                                   :class    "icon"}
                        [ui/Icon {:name "github"
                                  :size "large"}]]]}])))

