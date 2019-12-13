(ns sixsq.nuvla.ui.session.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.authn.subs :as authn-subs]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.spec :as us]
    [sixsq.nuvla.ui.authn.events :as authn-events]
    [cljs.spec.alpha :as s]
    [reagent.core :as r]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.session.sign-in-views :as sign-in-views]
    [sixsq.nuvla.ui.session.components :as comp]
    [sixsq.nuvla.ui.session.sign-up-views :as sign-up-views]
    [form-validator.core :as fv]
    [clojure.string :as str]
    [sixsq.nuvla.ui.i18n.views :as i18n-views]))


(defn Sign-in
  []
  [comp/SessionPage sign-in-views/Sign-in-form])


(defn Sign-up
  []
  [comp/SessionPage sign-up-views/Sign-up-form])


; redirect signup doit se faire sur la page sign-in
; reset password form

; si disponible
  ; github signup call
  ; github signin call
  ; sign-up

; en mobile formulaire au plus haut de la page
; ::automatic-logout-at-session-expiry
; nav quand not session redirect to sign-in