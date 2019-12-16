(ns sixsq.nuvla.ui.session.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.authn.events :as authn-events]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.session.components :as comp]
    [sixsq.nuvla.ui.session.reset-password-views :as reset-password-views]
    [sixsq.nuvla.ui.session.sign-in-views :as sign-in-views]
    [sixsq.nuvla.ui.session.sign-up-views :as sign-up-views]
    ))


(defn Sign-in
  []
  (let [path         (subscribe [::main-subs/nav-path-first])
        query-params (subscribe [::main-subs/nav-query-params])
        {:keys [message, error]} @query-params
        tr           (subscribe [::i18n-subs/tr])]
    (when @query-params
      (when error
        (dispatch [::authn-events/set-error-message (@tr [(keyword error)])]))
      (when message
        (dispatch [::authn-events/set-success-message (@tr [(keyword message)])]))

      (dispatch [::history-events/navigate (str @path "/")])))
  [comp/SessionPage
   sign-in-views/Presentation
   sign-in-views/Form])


(defn Sign-up
  []
  [comp/SessionPage
   sign-up-views/Presentation
   sign-up-views/Form])


(defn Reset-password
  []
  [comp/SessionPage
   reset-password-views/Presentation
   reset-password-views/Form])


; redirect signup doit se faire sur la page sign-in
; reset password form

; si disponible
; github signup call
; github signin call
; sign-up

; en mobile formulaire au plus haut de la page
; ::automatic-logout-at-session-expiry
; nav quand not session redirect to sign-in
