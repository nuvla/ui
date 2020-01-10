(ns sixsq.nuvla.ui.session.components
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.i18n.views :as i18n-views]
    [sixsq.nuvla.ui.session.events :as events]
    [sixsq.nuvla.ui.session.subs :as subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(defn RightPanel
  [{:keys [title title-bold FormFields submit-text submit-fn ExtraContent]}]
  (let [error-message   (subscribe [::subs/error-message])
        success-message (subscribe [::subs/success-message])
        tr              (subscribe [::i18n-subs/tr])
        loading?        (subscribe [::subs/loading?])]
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
                      :onDismiss #(dispatch [::events/set-error-message nil])}
          [ui/MessageHeader (str/capitalize (@tr [:error]))]
          [:p @error-message]])

       (when @success-message
         [ui/Message {:negative  false
                      :size      "tiny"
                      :onDismiss #(dispatch [::events/set-success-message nil])}
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
