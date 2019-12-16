(ns sixsq.nuvla.ui.session.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.session.components :as comp]
    [sixsq.nuvla.ui.session.sign-in-views :as sign-in-views]
    [sixsq.nuvla.ui.session.reset-password-views :as reset-password-views]
    [sixsq.nuvla.ui.session.sign-up-views :as sign-up-views]))


(defn Sign-in
  []
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
