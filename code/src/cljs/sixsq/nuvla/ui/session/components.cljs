(ns sixsq.nuvla.ui.session.components
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.authn.events :as authn-events]
    [sixsq.nuvla.ui.authn.subs :as authn-subs]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.i18n.views :as i18n-views]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(defn RightPanel
  [{:keys [title title-bold FormFields submit-text submit-fn ExtraContent]}]
  (let [error-message   (subscribe [::authn-subs/error-message])
        success-message (subscribe [::authn-subs/success-message])
        tr              (subscribe [::i18n-subs/tr])]
    [ui/GridColumn
     [:div {:style {:float "right"}} [i18n-views/LocaleDropdown]]
     [:div {:style {:margin-left "10%"
                    :margin-top  "30%"}}
      [:span {:style {:font-size "1.4em"}} title [:b title-bold]]


      [ui/Form {:style {:margin-top 30
                        :max-width  "70%"}}
       (when @error-message
         [ui/Message {:negative  true
                      :size      "tiny"
                      :onDismiss #(dispatch [::authn-events/set-error-message nil])}
          [ui/MessageHeader (@tr [:login-failed])]
          [:p @error-message]])

       (when @success-message
         [ui/Message {:negative  false
                      :size      "tiny"
                      :onDismiss #(dispatch [::authn-events/set-success-message nil])}
          [ui/MessageHeader (@tr [:success])]
          [:p @success-message]])

       FormFields

       [ui/Button {:primary  true
                   :floated  "right"
                   :on-click submit-fn}
        submit-text]]

      ExtraContent]]))


(defn LeftPanel
  [{:keys [title subtitle p1 p2 button-text button-callback]}]
  [ui/GridColumn {:style {:background-image    "url(/ui/images/volumlight.png)"
                          :background-size     "cover"
                          :background-position "left"
                          :background-repeat   "no-repeat"
                          :color               "white"
                          :min-height          "100vh"}}
   [:div {:style {:padding "75px"}}
    #_[:div {:style {:font-size "2em"}}
       "Welcome to"]
    [:div {:style {:font-size   "6em"
                   :line-height "normal"}}
     title]
    [:br]

    [:div {:style {:margin-top  40
                   :line-height "normal"
                   :font-size   "2em"}}
     subtitle]
    [:br]

    [:b {:style {:font-size "1.4em"}} p1]

    [:br] [:br]
    [ui/Button
     {:size     "large"
      :inverted true
      :on-click button-callback}
     button-text]
    [:div {:style {:margin-top  20
                   :line-height "normal"}}
     p2]

    [:div {:style {:position "absolute"
                   :bottom   40}}
     "Follow us on "
     [:span
      [ui/Icon {:name "facebook"}]
      [ui/Icon {:name "twitter"}]
      [ui/Icon {:name "youtube"}]]]]])


(defn SessionPage
  [LeftPanel Right-Panel]
  (let [session (subscribe [::authn-subs/session])]
    (when @session
      (dispatch [::history-events/navigate "welcome"]))
    [ui/Grid {:stackable true
              :columns   2
              :reversed  "mobile"
              :style     {:margin           0
                          :background-color "white"}}
     [LeftPanel]
     [Right-Panel]]))