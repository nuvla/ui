(ns sixsq.nuvla.ui.session.components
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.i18n.views :as i18n-views]
    [sixsq.nuvla.ui.session.events :as events]
    [sixsq.nuvla.ui.session.subs :as subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))


(defn RightPanel
  [{:keys [title title-bold FormFields submit-text submit-fn ExtraContent]}]
  (let [error-message (subscribe [::subs/error-message])
        success-message (subscribe [::subs/success-message])
        loading? (subscribe [::subs/loading?])]
    [:<>
     [:div {:style {:float "right"}} [i18n-views/LocaleDropdown]]
     [:div {:style {:margin-left "10%"
                    :margin-top  "25%"}}
      [:span {:style {:font-size "1.4em"}} title [:b title-bold]]


      [ui/Form {:style {:margin-top 30
                        :max-width  "80%"}}
       (when @error-message
         [ui/Message {:negative  true
                      :size      "tiny"
                      :onDismiss #(dispatch [::events/set-error-message nil])}
          [ui/MessageHeader [uix/TR :error str/capitalize]]
          [:p [uix/TR @error-message]]])

       (when @success-message
         [ui/Message {:negative  false
                      :size      "tiny"
                      :onDismiss #(dispatch [::events/set-success-message nil])}
          [ui/MessageHeader [uix/TR :success str/capitalize]]
          [:p [uix/TR @success-message]]])

       FormFields

       (when submit-fn
         [ui/Button {:primary  true
                     :floated  "right"
                     :loading  @loading?
                     :on-click submit-fn}
          submit-text])]

      (when ExtraContent
        ExtraContent)]]))

(defn SignExternal
  [{:keys [resource-url href icon server-redirect-uri]}]
  [:form {:action resource-url
          :method "post"
          :style  {:display "inline"}}
   [:input {:hidden        true
            :name          "href"
            :default-value href}]
   (when server-redirect-uri
     [:input {:hidden        true
              :name          "redirect-url"
              :default-value server-redirect-uri}])
   [ui/Button {:style    {:margin-left 10}
               :circular true
               :basic    true
               :type     "submit"
               :class    "icon"}
    (case icon
      :github [ui/Icon {:name "github", :size "large"}]
      :geant [ui/Icon {:name "student", :size "large"}]
      :icrc [ui/Image {:src "/ui/images/icrc.png", :style {:width 21}}]
      nil)]])
