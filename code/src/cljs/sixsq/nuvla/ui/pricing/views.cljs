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
  [{:keys [id title subtitle color logo first nb-number nb-price dep-number dep-price] :as ops}]
  (let [is-mobile? (subscribe [::main-subs/is-device? :mobile])]
    (fn [{:keys [id title subtitle color logo first nb-number nb-price dep-number dep-price] :as ops}]
      (let [extend (or first @is-mobile?)]
        [ui/Card
         [ui/Segment {:text-align "center"}
          [ui/Header {:as :h2 :icon true :text-align "center"}
           [ui/Icon {:className logo
                     :color     color}]
           title
           [ui/HeaderSubheader subtitle]]
          [:h4 {:style {:text-align "centerall.css"
                        :color      "white"
                        :background "grey"}}
           "Monthly"]
          [:p {:style {:height     50
                       :text-align "center"}}
           (if first "Pay as you go" "Discount automatically applies")]
          [ui/Grid {:divided true, :style {;:height 75
                                           :background-color "lightyellow"}}
           (when extend
             [ui/GridColumn {:width 11}
              [:div {:style {:text-align "left"}}
               [ui/Icon {:name "box"}]
               "NuvlaBox"
               [:p {:style {:color "grey"}} "Active devices only"]]])
           [ui/GridColumn (when extend {:width 5})
            nb-number
            [:br]
            [ui/Label {:color color} nb-price]]]
          [ui/Grid {:divided true,
                    :style   {;:height 75
                              :background-color "lightcyan"}}
           (when extend
             [ui/GridColumn {:width 11}
              [:div {:style {:text-align "left"}} [ui/Icon {:name "play"}]
               "App Deployments"
               [:p {:style {:color "grey"}} "Concurrent deployments"]]])
           [ui/GridColumn (when extend {:width 5})
            dep-number
            [:br]
            [ui/Label {:color color} dep-price]]]
          ]]))))


(defn Pricing
  []
  [ui/Segment style/basic
   [ui/CardGroup {:centered true}
    [PlanComp {:id         "plan_Gx4S6VYf9cbfRK"
               :title      "Paper plane"
               :subtitle   "Pay as you go"
               :color      "olive"
               :logo       "fad fa-paper-plane"
               :first      true
               :nb-number  "Up to 99"
               :nb-price   "€ 50.00"
               :dep-number "Up to 999"
               :dep-price  "€ 6.00"}]
    [PlanComp {:id         "plan_Gx4S6VYf9cbfRK2"
               :title      "Airplane"
               :subtitle   "20% Discount"
               :color      "yellow"
               :logo       "fad fa-plane"
               :nb-number  "From 100"
               :nb-price   "€ 40.00"
               :dep-number "From 1'000"
               :dep-price  "€ 4.80"}]
    [PlanComp {:id         "plan_Gx4S6VYf9cbfRK3"
               :title      "Rocket"
               :subtitle   "35% Discount"
               :color      "orange"
               :logo       "fad fa-rocket"
               :nb-number  "From 500"
               :nb-price   "€ 32.50"
               :dep-number "From 5'000"
               :dep-price  "€ 3.90"}]
    [PlanComp {:id         "plan_Gx4S6VYf9cbfRK4"
               :title      "Starship"
               :subtitle   "43% Discount"
               :color      "red"
               :logo       "fad fa-starship"
               :nb-number  "From 1'000"
               :nb-price   "€ 28.50"
               :dep-number "From 10'000"
               :dep-price  "€ 3.42"}]]

   [ui/Grid {:centered true, :stackable true}
    [ui/GridRow {:vertical-align "middle"}
     [ui/GridColumn {:width 10}
      [ui/Table {:attached "top", :striped true, :text-align "center"}
       [ui/TableBody
        [ui/TableRow
         [ui/TableCell {:row-span 3, :width 2} [:h4 "Support"]]
         [ui/TableCell {:width 2} [:h5 "Bronze"]]
         [ui/TableCell {:width 12} "included"]]
        [ui/TableRow
         [ui/TableCell [:h5 "Silver"]]
         [ui/TableCell {:style {:font-style "italic"}} "contact us"]]
        [ui/TableRow
         [ui/TableCell [:h5 "Gold"]]
         [ui/TableCell {:style {:font-style "italic"}} "contact us"]]]]
      [ui/Table {:attached "bottom", :striped true, :text-align "center"}
       [ui/TableBody
        [ui/TableRow
         [ui/TableCell {:row-span 2, :width 2} [:h4 "VPN"]]
         [ui/TableCell {:width 2} [:h5 "1st"]]
         [ui/TableCell {:width 12, :style {:font-style "italic"}} "included"]]
        [ui/TableRow
         [ui/TableCell [:h5 "Additional"]]
         [ui/TableCell "€ 5.00 per month, each"]]]]]
     [ui/GridColumn {:width 5, :text-align "center"}
      ^{:key (random-uuid)}
      [profile-views/SubscribeButton]
      ]]]])


(defmethod panel/render :pricing
  [path]
  (let [stripe (subscribe [::main-subs/stripe])]
    [ui/Segment style/basic
    [uix/PageHeader
     "fas fa-piggy-bank" (str/upper-case "Pricing")]
     (when @stripe
       [Pricing])]))