(ns sixsq.nuvla.ui.session.sign-in-views
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [form-validator.core :as fv]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.authn.events :as authn-events]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.history.views :as history-views]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.session.components :as comp]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.spec :as us]))

;; VALIDATION SPEC
(s/def ::username us/nonblank-string)
(s/def ::password us/nonblank-string)

(s/def ::session-template-password
  (s/keys :req-un [::username
                   ::password]))


(defn Form
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
                                      :label      (@tr [:username])
                                      :auto-focus true
                                      :on-change  (partial fv/event->names->value! form)
                                      :on-blur    (partial fv/event->show-message form)
                                      :error      (fv/?show-message form :username spec->msg)}]
                       [ui/FormInput {:name      :password
                                      :label     (str/capitalize (@tr [:password]))
                                      :type      "password"
                                      :on-change (partial fv/event->names->value! form)
                                      :on-blur   (partial fv/event->show-message form)
                                      :error     (fv/?show-message form :password spec->msg)}]
                       [ui/FormField
                        [history-views/link "reset-password" (@tr [:forgot-password])]]]
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


(defn Presentation
  []
  [comp/LeftPanel
   {:title "Nuvla"
    :subtitle "Start immediately deploying apps containers in one button click."
    :p1 "Start jouney with us"
    :p2 (str "Provide a secured edge to cloud (and back) management platform "
             "that enabled near-data AI for connected world use cases.")
    :button-text "Sign up"
    :button-callback #(dispatch [::history-events/navigate "sign-up"])}])