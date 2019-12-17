(ns sixsq.nuvla.ui.session.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.authn.events :as authn-events]
    [sixsq.nuvla.ui.authn.subs :as authn-subs]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.session.reset-password-views :as reset-password-views]
    [sixsq.nuvla.ui.session.sign-in-views :as sign-in-views]
    [sixsq.nuvla.ui.session.sign-up-views :as sign-up-views]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]))


(defn LeftPanel
  []
  (let [first-path (subscribe [::main-subs/nav-path-first])]
    (case @first-path
      "sign-in" [sign-in-views/Presentation]
      "sign-up" [sign-up-views/Presentation]
      "reset-password" [reset-password-views/Presentation]
      [sign-in-views/Presentation])))


(defn RightPanel
  []
  (let [first-path (subscribe [::main-subs/nav-path-first])]
    (case @first-path
      "sign-in" [sign-in-views/Form]
      "sign-up" [sign-up-views/Form]
      "reset-password" [reset-password-views/Form]
      [sign-in-views/Form])))

(defn SessionPage
  []
  (let [session      (subscribe [::authn-subs/session])
        query-params (subscribe [::main-subs/nav-query-params])
        tr           (subscribe [::i18n-subs/tr])
        error        (some-> @query-params :error keyword)
        message      (some-> @query-params :message keyword)]
    (when @session
      (dispatch [::history-events/navigate "welcome"]))
    (when error
      (dispatch [::authn-events/set-error-message (@tr [(keyword error)])]))
    (when message
      (dispatch [::authn-events/set-success-message (@tr [(keyword message)])]))
    [ui/Grid {:stackable true
              :columns   2
              :reversed  "mobile"
              :style     {:margin           0
                          :background-color "white"}}

     [ui/GridColumn {:style {:background-image    "url(/ui/images/volumlight.png)"
                             :background-size     "cover"
                             :background-position "left"
                             :background-repeat   "no-repeat"
                             :color               "white"
                             :min-height          "100vh"}}
      [LeftPanel]]
     [ui/GridColumn
      [RightPanel]]]))
