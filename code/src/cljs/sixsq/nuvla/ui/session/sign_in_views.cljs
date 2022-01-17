(ns sixsq.nuvla.ui.session.sign-in-views
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [form-validator.core :as fv]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-fx]
    [sixsq.nuvla.ui.history.views :as history-views]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.session.components :as comp]
    [sixsq.nuvla.ui.session.events :as events]
    [sixsq.nuvla.ui.session.subs :as subs]
    [sixsq.nuvla.ui.session.utils :as utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.spec :as us]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

;; VALIDATION SPEC
(s/def ::username us/nonblank-string)
(s/def ::password us/nonblank-string)

(s/def ::session-template-password
  (s/keys :req-un [::username
                   ::password]))


(defn FormTokenValidation
  []
  (let [tr    (subscribe [::i18n-subs/tr])
        token (r/atom nil)]
    (fn []
      [comp/RightPanel
       {:title       (str (@tr [:sign-in]) " ")
        :title-bold  (@tr [:code-verification])
        :FormFields  [:<>
                      [ui/Message {:info    true
                                   :header  (@tr [:code-verification])
                                   :content (@tr [:two-factor-authentication-message-send])
                                   :icon    "envelope"}]
                      [ui/FormInput
                       {:label         (str/capitalize (@tr [:code]))
                        :required      true
                        :icon          "key"
                        :icon-position "left"
                        :auto-focus    "on"
                        :auto-complete "off"
                        :value         @token
                        :on-change     (ui-callback/input-callback #(reset! token (or (re-find #"\d+" %1) "")))}]]
        :submit-text (str/capitalize (@tr [:validate]))
        :submit-fn   #(dispatch [::events/validate-2fa-activation @token])}])))

(defn Form
  []
  (let [form-conf           {:form-spec    ::session-template-password
                             :names->value {:username ""
                                            :password ""}}
        form                (fv/init-form form-conf)
        tr                  (subscribe [::i18n-subs/tr])
        spec->msg           {::username (@tr [:should-not-be-empty])
                             ::password (@tr [:should-not-be-empty])}
        server-redirect-uri (subscribe [::subs/server-redirect-uri])
        github-template?    (subscribe [::subs/session-template-exist?
                                        "session-template/github-nuvla"])
        geant-template?     (subscribe [::subs/session-template-exist?
                                        "session-template/oidc-geant"])]
    (fn []
      [comp/RightPanel
       {:title        (@tr [:login-to])
        :title-bold   (@tr [:account])
        :FormFields   [:<>
                       [ui/FormInput {:name          :username
                                      :label         (@tr [:username])
                                      :auto-focus    true
                                      :auto-complete "username"
                                      :on-change     (partial fv/event->names->value! form)
                                      :on-blur       (partial fv/event->show-message form)
                                      :error         (fv/?show-message form :username spec->msg)}]
                       [ui/FormInput {:name          :password
                                      :label         (str/capitalize (@tr [:password]))
                                      :type          "password"
                                      :auto-complete "current-password"
                                      :on-change     (partial fv/event->names->value! form)
                                      :on-blur       (partial fv/event->show-message form)
                                      :error         (fv/?show-message form :password spec->msg)}]
                       [ui/FormField {:style {:position "absolute"}}
                        [history-views/link "reset-password" (@tr [:forgot-password])]]]
        :submit-text  (@tr [:sign-in])
        :submit-fn    #(when (fv/validate-form-and-show? form)
                         (dispatch [::events/submit utils/session-tmpl-password
                                    (:names->value @form)]))
        :ExtraContent [:div {:style {:margin-top 100}}
                       (when (or @github-template?
                                 @geant-template?)
                         (@tr [:sign-in-with]))
                       (when @github-template?
                         [:form {:action (str @cimi-fx/NUVLA_URL "/api/session")
                                 :method "post"
                                 :style  {:display "inline"}}
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
                                     :size "large"}]]])
                       (when @geant-template?
                         [:form {:action (str @cimi-fx/NUVLA_URL "/api/session")
                                 :method "post"
                                 :style  {:display "inline"}}
                          [:input {:hidden        true
                                   :name          "href"
                                   :default-value "session-template/oidc-geant"}]
                          [ui/Button {:style    {:margin-left 10}
                                      :circular true
                                      :basic    true
                                      :type     "submit"
                                      :class    "icon"}
                           [ui/Icon {:name "student"
                                     :size "large"}]]])
                       ]}])))
