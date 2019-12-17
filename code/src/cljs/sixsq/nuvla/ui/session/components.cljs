(ns sixsq.nuvla.ui.session.components
  (:require
    [clojure.string :as str]
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
        tr              (subscribe [::i18n-subs/tr])
        loading?        (subscribe [::authn-subs/loading?])]
    [:<>
     [:div {:style {:float "right"}} [i18n-views/LocaleDropdown]]
     [:div {:style {:margin-left "10%"
                    :margin-top  "30%"}}
      [:span {:style {:font-size "1.4em"}} title [:b title-bold]]


      [ui/Form {:style {:margin-top 30
                        :max-width  "80%"}}
       (when @error-message
         [ui/Message {:negative  true
                      :size      "tiny"
                      :onDismiss #(dispatch [::authn-events/set-error-message nil])}
          [ui/MessageHeader (str/capitalize (@tr [:error]))]
          [:p @error-message]])

       (when @success-message
         [ui/Message {:negative  false
                      :size      "tiny"
                      :onDismiss #(dispatch [::authn-events/set-success-message nil])}
          [ui/MessageHeader (str/capitalize (@tr [:success]))]
          [:p @success-message]])

       FormFields

       [ui/Button {:primary  true
                   :floated  "right"
                   :loading  @loading?
                   :on-click submit-fn}
        submit-text]]

      (when ExtraContent
        ExtraContent)]]))


(defn LeftPanel
  [{:keys [title subtitle p1 p2 button-text button-callback]}]
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
   (when button-text
     [ui/Button
      {:size     "large"
       :inverted true
       :on-click button-callback}
      button-text])
   [:div {:style {:margin-top  20
                  :line-height "normal"}}
    p2]

   [:div {:style {:position "absolute"
                  :bottom   40}}
    "Follow us on "
    [:span
     [ui/Icon {:name "facebook"}]
     [ui/Icon {:name "twitter"}]
     [ui/Icon {:name "youtube"}]]]])
