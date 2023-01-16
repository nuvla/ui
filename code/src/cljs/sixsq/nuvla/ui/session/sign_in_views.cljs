(ns sixsq.nuvla.ui.session.sign-in-views
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [form-validator.core :as fv]
            [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-fx]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.session.components :as comp]
            [sixsq.nuvla.ui.session.events :as events]
            [sixsq.nuvla.ui.session.subs :as subs]
            [sixsq.nuvla.ui.session.utils :as utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.spec :as us]))

;; VALIDATION SPEC
(s/def ::username us/nonblank-string)
(s/def ::password us/nonblank-string)

(s/def ::session-template-password
  (s/keys :req-un [::username
                   ::password]))


(defn FormTokenValidation
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        on-submit #(dispatch [::events/validate-2fa-activation %1])]
    (fn []
      [comp/RightPanel
       {:title      (str (@tr [:sign-in]) " ")
        :title-bold (@tr [:code-verification])
        :FormFields [ui/FormInput
                     {:label    (str/capitalize (@tr [:code]))
                      :required true}
                     [uix/TokenSubmiter
                      {:on-submit on-submit}]]}])))


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
        resource-url        (str @cimi-fx/NUVLA_URL "/api/session")
        github-session-tmpl "session-template/github-nuvla"
        geant-session-tmpl  "session-template/oidc-geant"
        icrc-session-tmpl   "session-template/oidc-icrc"
        github-template?    (subscribe [::subs/session-template-exist? github-session-tmpl])
        geant-template?     (subscribe [::subs/session-template-exist? geant-session-tmpl])
        icrc-template?      (subscribe [::subs/session-template-exist? icrc-session-tmpl])]
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
                       [ui/FormField
                        [uix/Link "reset-password" (@tr [:forgot-password])]]]
        :submit-text  (@tr [:sign-in])
        :submit-fn    #(when (fv/validate-form-and-show? form)
                         (dispatch [::events/submit utils/session-tmpl-password
                                    (:names->value @form)]))
        :ExtraContent [:div {:style {:margin-top 100}}
                       (when (or @github-template?
                                 @geant-template?
                                 @icrc-template?)
                         (@tr [:sign-in-with]))
                       (when @github-template?
                         [comp/SignExternal
                          {:resource-url        resource-url
                           :href                github-session-tmpl
                           :icon                :github
                           :server-redirect-uri @server-redirect-uri}])
                       (when @geant-template?
                         [comp/SignExternal
                          {:resource-url resource-url
                           :href         geant-session-tmpl
                           :icon         :geant}])
                       (when @icrc-template?
                         [comp/SignExternal
                          {:resource-url resource-url
                           :href         icrc-session-tmpl
                           :icon         :icrc}])]
        }])))
