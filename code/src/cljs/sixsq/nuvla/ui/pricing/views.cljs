(ns sixsq.nuvla.ui.pricing.views
  (:require
    ["@stripe/react-stripe-js" :as react-stripe]
    [ajax.core :as ajax]
    [clojure.string :as str]
    [day8.re-frame.http-fx]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.pricing.events :as events]
    [sixsq.nuvla.ui.pricing.subs :as subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(def Elements (r/adapt-react-class react-stripe/Elements))
(def CardElement (r/adapt-react-class react-stripe/CardElement))
(def ElementsConsumer (r/adapt-react-class react-stripe/ElementsConsumer))
(def IbanElement (r/adapt-react-class react-stripe/IbanElement))


(def email (r/atom nil))

(def card-info-completed? (r/atom false))

(def card-validation-error-message (r/atom nil))

(def payment-form (r/atom "credit-card"))

(defn handle-submit-credit-card
  [elements event]
  (.preventDefault event)
  (when elements
    (dispatch [::events/create-payment-method
               #js{:type            "card"
                   :card            (elements.getElement react-stripe/CardElement)}])))


(defn handle-submit-sepa-debit
  [elements event]
  (.preventDefault event)
  (when elements
    (dispatch [::events/create-payment-method
               #js{:type            "sepa_debit"
                   :sepa_debit      (elements.getElement react-stripe/IbanElement)
                   :billing_details #js{:name  "test"}}])))


;; While not yet hooks support we have to use react components
;; https://github.com/reagent-project/reagent/blob/master/doc/ReactFeatures.md#hooks
(defn InternalCheckoutForm
  [elements]
  (let [className            (r/atom "stripe-input")
        stripe               (subscribe [::subs/stripe])
        payment-method-error (subscribe [::subs/error])
        processing?          (subscribe [::subs/processing?])
        plan-id              (subscribe [::subs/plan-id])]
    (fn [elements]
      [ui/Form {:on-submit (partial (if (= @payment-form "credit-card")
                                      handle-submit-credit-card
                                      handle-submit-sepa-debit) elements)
                :error     (boolean @payment-method-error)}
       [ui/Message {:error   true
                    :header  "Something went wrong"
                    :content @payment-method-error}]
       [ui/FormSelect {:label         "Choose your payment method"
                       :default-value "credit-card"
                       :on-change     (ui-callback/value #(reset! payment-form %))
                       :options       [{:text "credit card", :value "credit-card", :icon "credit card"}
                                       {:text "bank account", :value "sepa-debit", :icon "exchange"}]}]
       (if (= @payment-form "credit-card")
         [ui/FormField
          [:label "Card Number"]
          [CardElement {:className @className
                        :on-change (fn [event]
                                     (js/console.log event)
                                     (reset! card-validation-error-message
                                             (some-> event .-error .-message))
                                     (reset! card-info-completed? (.-complete event)))}]
          (when @card-validation-error-message
            [ui/Label {:basic true, :color "red", :pointing true} @card-validation-error-message])]
         [ui/FormField
          [:label "IBAN"]
          [IbanElement
           {:className @className
            :on-change (fn [event]
                         (reset! card-validation-error-message
                                 (some-> event .-error .-message))
                         (reset! card-info-completed? (.-complete event)))
            :options   (clj->js {:supportedCountries ["SEPA"]
                                 :placeholderCountry "CH"})}]
          (when @card-validation-error-message
            [ui/Label {:basic true, :color "red", :pointing true} @card-validation-error-message])]
         )
       [ui/Button {:type     "submit"
                   :animated "vertical"
                   :primary  true
                   :loading  @processing?
                   :floated  "right"
                   :disabled (or
                               (not @stripe)
                               (not @card-info-completed?)
                               #_(nil? @plan-id)
                               @processing?)}
        [ui/ButtonContent {:hidden true} [ui/Icon {:name "shop"}]]
        [ui/ButtonContent {:visible true} "Subscribe"]]])))


(defn ReactCheckoutForm []
  (let [elements (react-stripe/useElements)]
    (r/as-element
      [InternalCheckoutForm elements])))


(def SubscribeModal (r/adapt-react-class ReactCheckoutForm))

(defn SubscribeButton
  []
  (let [locale (subscribe [::i18n-subs/locale])
        stripe (subscribe [::subs/stripe])]
    [ui/Modal
     {:open       true
      :size "tiny"
      :trigger    (r/as-element [ui/Button {:style    {:width "80%"}
                                            :positive true
                                            :circular true
                                            :size     "large"} "Subscribe now"])
      :close-icon true}
     [ui/ModalHeader "Subscribe"]
     [ui/ModalContent
      ^{:key @locale}
      [Elements {:stripe  @stripe
                 :options {:locale @locale}}
       [SubscribeModal]]]]))


(defn PlanComp
  [{:keys [id title subtitle color logo first nb-number nb-price dep-number dep-price] :as ops}]
  (let [is-mobile? (subscribe [::main-subs/is-device? :mobile])]
    (fn [{:keys [id title subtitle color logo first nb-number nb-price dep-number dep-price] :as ops}]
      (let [selected-plan-id (subscribe [::subs/plan-id])
            extend           (or first @is-mobile?)]
        [ui/Card
         [ui/Segment {:text-align "center"}
          [ui/Header {:as :h2 :icon true :text-align "center"}
           [ui/Icon {:name logo, :color color}]
           title
           [ui/HeaderSubheader subtitle]]
          [:h4 {:style {:text-align "center"
                        :color      "white"
                        :background "grey"}}
           "Monthly"]
          [:p {:style {:height     50
                       :text-align "center"}}
           (if first "Pay as you go" "Discount automatically applies")]
          [ui/Grid {:divided true, :style {;:height 75
                                           :background-color "lightyellow"}}
           (when extend
             [ui/GridColumn {:width 10}
              [:div {:style {:text-align "left"}}
               [ui/Icon {:name "box"}]
               "NuvlaBox"
               [:p {:style {:color "grey"}} "Active devices only"]]])
           [ui/GridColumn (when extend {:width 6})
            nb-number
            [:br]
            [ui/Label {:color color} nb-price]]]
          [ui/Grid {:divided true,
                    :style   {;:height 75
                              :background-color "lightcyan"}}
           (when extend
             [ui/GridColumn {:width 10}
              [:div {:style {:text-align "left"}} [ui/Icon {:name "play"}]
               "App Deployments"
               [:p {:style {:color "grey"}} "Active devices only"]]])
           [ui/GridColumn (when extend {:width 6})
            dep-number
            [:br]
            [ui/Label {:color color} dep-price]]]
          ]]))))


(defn Pricing
  []
  (let [locale       (subscribe [::i18n-subs/locale])
        stripe       (subscribe [::subs/stripe])
        subscription (subscribe [::subs/subscription])
        plan-id      (subscribe [::subs/plan-id])]
    (dispatch [::events/init])
    (reset! card-validation-error-message nil)
    (reset! card-info-completed? false)
    (fn []
      [ui/Segment (assoc style/basic, :loading (not @stripe))
       [ui/CardGroup {:centered true}
        [PlanComp {:id         "plan_Gx4S6VYf9cbfRK"
                   :title      "Paper plane"
                   :subtitle   "Pay as you go"
                   :color      "olive"
                   :logo       "paper plane"
                   :first      true
                   :nb-number  "Up to 99"
                   :nb-price   "€ 50.00"
                   :dep-number "Up to 999"
                   :dep-price  "€ 6.00"
                   }]
        [PlanComp {:id         "plan_Gx4S6VYf9cbfRK2"
                   :title      "Air plane"
                   :subtitle   "20% Discount"
                   :color      "yellow"
                   :logo       "plane"
                   :nb-number  "From 100"
                   :nb-price   "€ 40.00"
                   :dep-number "From 1'000"
                   :dep-price  "€ 4.80"}]
        [PlanComp {:id         "plan_Gx4S6VYf9cbfRK3"
                   :title      "Rocket"
                   :subtitle   "35% Discount"
                   :color      "orange"
                   :logo       "rocket"
                   :nb-number  "From 500"
                   :nb-price   "€ 32.50"
                   :dep-number "From 5'000"
                   :dep-price  "€ 3.90"}]
        [PlanComp {:id         "plan_Gx4S6VYf9cbfRK4"
                   :title      "Starship"
                   :subtitle   "43% Discount"
                   :color      "red"
                   :logo       "space shuttle"
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
          [SubscribeButton]]]]


       [:br]
       [ui/GridRow {:columns 1}
        [ui/GridColumn
         [ui/ButtonGroup
          [ui/Button {:attached "left"
                      :active   (= @payment-form "credit-card")
                      :on-click #(reset! payment-form "credit-card")}
           "Credit Card"]
          [ui/Button {:attached "right"
                      :active   (= @payment-form "sepa-debit")
                      :on-click #(reset! payment-form "sepa-debit")}
           "SEPA Direct Debit payments"]]]]
       (when (and true #_@plan-id @stripe)
         [ui/GridColumn
          [ui/Segment {:compact true}
           ^{:key @locale}
           [Elements {:stripe  @stripe
                      :options {:locale @locale}}
            [SubscribeModal]]]])
       (when @subscription
         [:<>
          [ui/GridRow {:columns 1}
           [ui/GridColumn
            [:h2 "Your subscription status is: " (:status @subscription) " "
             (when (= (:status @subscription) "active")
               [ui/Icon {:name "handshake"}]
               )]]
           ]
          [ui/GridRow {:columns 1}
           [ui/GridColumn
            [ui/Segment
             [ui/CodeMirror {:value   (or (.stringify js/JSON (clj->js @subscription) nil 2) "")
                             :options {:mode      "application/json"
                                       :read-only true}}]
             ]]]]
         )
       ])))


(defmethod panel/render :pricing
  [path]
  [ui/Segment style/basic
   [uix/PageHeader "code" (str/upper-case "Pricing")]
   [Pricing]])