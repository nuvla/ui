(ns sixsq.nuvla.ui.deployment-dialog.views-price
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.deployment-dialog.events :as events]
    [sixsq.nuvla.ui.deployment-dialog.subs :as subs]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.deployment-dialog.utils :as utils]))


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
        [ui/Icon {:name "eur", :size "large"}]
        [ui/Icon {:name "warning sign", :size "large", :color "red"}])]
     [ui/TableCell {:collapsing true} (@tr [:price])]
     [ui/TableCell [:div (str
                           (general-utils/format "%.2f" (* (:cent-amount-daily @price) 0.3))
                           "€/" (str/capitalize (@tr [:month])))]]]))


(defmethod utils/step-content :pricing
  []
  (let [tr               (subscribe [::i18n-subs/tr])
        price            (subscribe [::subs/price])
        price-completed? (subscribe [::subs/price-completed?])
        coupon           (subscribe [::subs/coupon])
        deployment       (subscribe [::subs/deployment])]
    [:<>
     [ui/Segment
      [:p (str (@tr [:one-day-trial-deployment])) " " (@tr [:deployment-will-cost])
       [:b (if (>= (:cent-amount-daily @price) 100)
             (str (float (/ (:cent-amount-daily @price) 100)) "€/" (@tr [:day]))
             (str (:cent-amount-daily @price) "ct€/" (@tr [:day])))]]
      [ui/Checkbox {:label     (@tr [:accept-costs])
                    :checked   @price-completed?
                    :on-change (ui-callback/checked
                                 #(dispatch [::events/set-price-accepted? %]))}]]
     [ui/Input
      {:label         "Coupon"
       :placeholder   "code"
       :default-value (or @coupon "")
       :on-change     (ui-callback/input-callback
                        #(dispatch [::events/set-deployment
                                    (assoc @deployment :coupon %)]))}]]))