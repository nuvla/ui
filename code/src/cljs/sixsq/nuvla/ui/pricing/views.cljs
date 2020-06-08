(ns sixsq.nuvla.ui.pricing.views
  (:require
    ["@stripe/react-stripe-js" :as react-stripe]
    [ajax.core :as ajax]
    [sixsq.nuvla.ui.profile.views :as profile-views]
    [clojure.string :as str]
    [day8.re-frame.http-fx]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.pricing.events :as events]
    [sixsq.nuvla.ui.profile.events :as profile-event]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.pricing.subs :as subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.general :as general]))


(defn PlanComp
  [{:keys [id title subtitle color logo first-item
           nb-number nb-price dep-number dep-price] :as ops}]
  (let [tr         (subscribe [::i18n-subs/tr])
        is-mobile? (subscribe [::main-subs/is-device? :mobile])]
    (fn [{:keys [id title subtitle color logo first-item
                 nb-number nb-price dep-number dep-price] :as ops}]
      (let [extend (or first-item @is-mobile?)]
        [ui/Card
         [ui/Segment {:text-align "center"}
          [ui/Header {:as :h2 :icon true :text-align "center"}
           [ui/Icon {:className logo
                     :color     color}]
           title
           [ui/HeaderSubheader subtitle]]
          [:h4 {:style {:text-align "center"
                        :color      "white"
                        :background "grey"}}
           (@tr [:monthly])]
          [:p {:style {:height     50
                       :text-align "center"}}
           (@tr [(if first-item
                   :you-only-pay-for-resources-you-use
                   :discount-automatically-applies)])]
          [ui/Grid {:divided true, :style {:background-color "lightyellow"}}
           (when extend
             [ui/GridColumn {:width 11}
              [:div {:style {:text-align "left"}}
               [ui/Icon {:name "box"}]
               "NuvlaBox"
               [:p {:style {:color "grey"}} (@tr [:active-devices-only])]]])
           [ui/GridColumn (when extend {:width 5})
            nb-number
            [:br]
            [ui/Label {:color color} nb-price]]]
          [ui/Grid {:divided true,
                    :style   {:background-color "lightcyan"}}
           (when extend
             [ui/GridColumn {:width 11}
              [:div {:style {:text-align "left"}} [ui/Icon {:name "play"}]
               "App Deployments"
               [:p {:style {:color "grey"}} (@tr [:concurrent-deployment])]]])
           [ui/GridColumn (when extend {:width 5})
            dep-number
            [:br]
            [ui/Label {:color color} dep-price]]]
          ]]))))


(defn Pricing
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    (let [up-to-str (@tr [:up-to])
          from-str  (@tr [:from])]
      [ui/Segment style/basic
       [ui/CardGroup {:centered true}
        [PlanComp {:id         "plan_Gx4S6VYf9cbfRK"
                   :title      "Paper plane"
                   :subtitle   "Pay as you go"
                   :color      "olive"
                   :logo       "fad fa-paper-plane"
                   :first-item true
                   :nb-number  (str up-to-str " 99")
                   :nb-price   "€ 50.00"
                   :dep-number (str up-to-str " 999")
                   :dep-price  "€ 6.00"}]
        [PlanComp {:id         "plan_Gx4S6VYf9cbfRK2"
                   :title      "Airplane"
                   :subtitle   "20% Discount"
                   :color      "yellow"
                   :logo       "fad fa-plane"
                   :nb-number  (str from-str " 100")
                   :nb-price   "€ 40.00"
                   :dep-number (str from-str " 1'000")
                   :dep-price  "€ 4.80"}]
        [PlanComp {:id         "plan_Gx4S6VYf9cbfRK3"
                   :title      "Rocket"
                   :subtitle   "35% Discount"
                   :color      "orange"
                   :logo       "fad fa-rocket"
                   :nb-number  (str from-str " 500")
                   :nb-price   "€ 32.50"
                   :dep-number (str from-str " 5'000")
                   :dep-price  "€ 3.90"}]
        [PlanComp {:id         "plan_Gx4S6VYf9cbfRK4"
                   :title      "Starship"
                   :subtitle   "43% Discount"
                   :color      "red"
                   :logo       "fad fa-starship"
                   :nb-number  (str from-str " 1'000")
                   :nb-price   "€ 28.50"
                   :dep-number (str from-str " 10'000")
                   :dep-price  "€ 3.42"}]]

       [ui/Grid {:centered true, :stackable true}
        [ui/GridRow {:vertical-align "middle"}
         [ui/GridColumn {:width 10}
          [ui/Table {:attached "top", :striped true, :text-align "center"}
           [ui/TableBody
            [ui/TableRow
             [ui/TableCell {:row-span 3, :width 2} [:h4 "Support"]]
             [ui/TableCell {:width 2} [:h5 "Bronze"]]
             [ui/TableCell {:width 12} (@tr [:included])]]
            [ui/TableRow
             [ui/TableCell [:h5 "Silver"]]
             [ui/TableCell {:style {:font-style "italic"}} (@tr [:contact-us])]]
            [ui/TableRow
             [ui/TableCell [:h5 "Gold"]]
             [ui/TableCell {:style {:font-style "italic"}} (@tr [:contact-us])]]]]
          [ui/Table {:attached "bottom", :striped true, :text-align "center"}
           [ui/TableBody
            [ui/TableRow
             [ui/TableCell {:row-span 2, :width 2} [:h4 "VPN"]]
             [ui/TableCell {:width 2} [:h5 "1st"]]
             [ui/TableCell {:width 12, :style {:font-style "italic"}} (@tr [:included])]]
            [ui/TableRow
             [ui/TableCell [:h5 (@tr [:additional])]]
             [ui/TableCell (str "€ 5.00 " (@tr [:per-month-each]))]]]]]
         [ui/GridColumn {:width 5, :text-align "center"}
          ^{:key (random-uuid)}
          [profile-views/SubscribeButton]
          ]]]])))


(defmethod panel/render :pricing
  [path]
  (let [stripe (subscribe [::main-subs/stripe])]
    [ui/Segment style/basic
     [uix/PageHeader
      "fas fa-piggy-bank" (str/upper-case "Pricing")]
     (when @stripe
       [Pricing])]))