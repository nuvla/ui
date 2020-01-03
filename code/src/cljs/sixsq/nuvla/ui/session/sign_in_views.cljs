(ns sixsq.nuvla.ui.session.sign-in-views
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [form-validator.core :as fv]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.authn.events :as authn-events]
    [sixsq.nuvla.ui.authn.subs :as authn-subs]
    [sixsq.nuvla.ui.authn.utils :as utils]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-fx]
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
  (let [form-conf           {:form-spec ::session-template-password}
        form                (fv/init-form form-conf)
        tr                  (subscribe [::i18n-subs/tr])
        spec->msg           {::username (@tr [:should-not-be-empty])
                             ::password (@tr [:should-not-be-empty])}
        server-redirect-uri (subscribe [::authn-subs/server-redirect-uri])
        github-template?    (subscribe [::authn-subs/session-template-exist?
                                        "session-template/github-nuvla"])]
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
                         (dispatch [::authn-events/submit "session-template/password"
                                    (:names->value @form)]))
        :ExtraContent (when @github-template?
                        [ui/Form {:action (str @cimi-fx/NUVLA_URL "/api/session")
                                  :method "post"
                                  :style  {:margin-top 70
                                           :color      "grey"}}
                         "or use your github account "
                         [:input {:hidden        true
                                  :name          "href"
                                  :default-value "session-template/github-nuvla"}]
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
  (let [signup-template? (subscribe [::authn-subs/user-template-exist?
                                     utils/user-tmpl-email-password])]
    [comp/LeftPanel
     (cond->
       {:title    "Nuvla"
        :subtitle "Start immediately deploying apps containers in one button click."
        :p1       "Start jouney with us"
        :p2       (str "Provide a secured edge to cloud (and back) management platform "
                       "that enabled near-data AI for connected world use cases.")}
       @signup-template? (assoc :button-text "Sign up"
                                :button-callback #(dispatch [::history-events/navigate "sign-up"]))
       )]))