(ns sixsq.nuvla.ui.deployment-dialog.views-price
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.deployment-dialog.events :as events]
            [sixsq.nuvla.ui.deployment-dialog.subs :as subs]
            [sixsq.nuvla.ui.deployment-dialog.utils :as utils]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn summary-row
  []
  (let [tr               (subscribe [::i18n-subs/tr])
        price            (subscribe [::subs/price])
        price-completed? (subscribe [::subs/price-completed?])
        on-click-fn      #(dispatch [::events/set-active-step :pricing])]
    [ui/TableRow {:active   false
                  :on-click on-click-fn}
     [ui/TableCell {:collapsing true}
      (if @price-completed?
        [icons/EuroIcon {:size "large"}]
        [icons/WarningIcon {:size "large", :color "red"}])]
     [ui/TableCell {:collapsing true} (@tr [:price])]
     [ui/TableCell [:div (str
                           (general-utils/format "%.2f" (* (:cent-amount-daily @price) 0.3))
                           "€/" (str/capitalize (@tr [:month])))]]]))


(defmethod utils/step-content :pricing
  []
  (let [tr               (subscribe [::i18n-subs/tr])
        price-completed? (subscribe [::subs/price-completed?])
        start?           (subscribe [::subs/deployment-start?])
        price            (subscribe [::subs/price])
        new-price        (subscribe [::subs/new-price])
        can-edit?        (subscribe [::subs/can-edit-module-data?])
        format-price     #(if (>= (:cent-amount-daily %) 100)
                            (str (float (/ (:cent-amount-daily %) 100)) "€/" (@tr [:day]))
                            (str (:cent-amount-daily %) "ct€/" (@tr [:day])))]
    [:<>
     (when @can-edit?
       [ui/Message {:info    true
                    :content (@tr [:free-deployment-for-vendor])}])
     [ui/Segment
      [:p
       (str (when @start?
              (if (:follow-customer-trial @price)
                (@tr [:trial-deployment-follow])
                (@tr [:trial-deployment])))
            (cond-> (@tr [:deployment-will-cost])
                    @new-price (str/capitalize)))
       (when @new-price
         (if (> (:cent-amount-daily @new-price)
                (:cent-amount-daily @price))
           [icons/CaretUpIcon {:color "red"}]
           [icons/CaretDownIcon {:color "green"}]))

       [:b (format-price (or @new-price @price))]]
      (when @new-price
        [:p [:i (str (@tr [:price-changed])
                     (format-price @price) " " (@tr [:to]) " "
                     (format-price @new-price))]])

      [ui/Checkbox {:label     (@tr [:accept-costs])
                    :checked   @price-completed?
                    :on-change (ui-callback/checked
                                 #(dispatch [::events/set-price-accepted? %]))}]]]))
