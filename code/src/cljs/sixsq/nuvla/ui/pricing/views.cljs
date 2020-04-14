(ns sixsq.nuvla.ui.pricing.views
  (:require
    ["@stripe/react-stripe-js" :as react-stripe]
    [ajax.core :as ajax]
    [clojure.string :as str]
    [day8.re-frame.http-fx]
    [re-frame.core :refer [dispatch subscribe]]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.pricing.events :as events]
    [sixsq.nuvla.ui.pricing.subs :as subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(def Elements (r/adapt-react-class react-stripe/Elements))
(def CardElement (r/adapt-react-class react-stripe/CardElement))
(def ElementsConsumer (r/adapt-react-class react-stripe/ElementsConsumer))
(def IbanElement (r/adapt-react-class react-stripe/IbanElement))


(def email (r/atom nil))

(def card-info-completed? (r/atom false))

(def card-validation-error-message (r/atom nil))

(def payment-form (r/atom :sepa-debit #_:credit-card))

(defn handle-submit-credit-card
  [elements event]
  (.preventDefault event)
  (when elements
    (dispatch [::events/create-payment-method
               #js{:type            "card"
                   :card            (elements.getElement react-stripe/CardElement)
                   :billing_details #js{:email @email}}])))


(defn handle-submit-sepa-debit
  [elements event]
  (.preventDefault event)
  (when elements
    (dispatch [::events/create-payment-method
               #js{:type            "sepa_debit"
                   :sepa_debit      (elements.getElement react-stripe/IbanElement)
                   :billing_details #js{:email @email
                                        :name "test"}}])))


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
      [ui/Form {:on-submit (partial (if (= payment-form :credit-card)
                                      handle-submit-credit-card
                                      handle-submit-sepa-debit) elements)
                :style     {:width 400}
                :error     (boolean @payment-method-error)}
       [ui/Message {:error   true
                    :header  "Something went wrong"
                    :content @payment-method-error}]
       [ui/FormInput {:label     "Email"
                      :on-change (ui-callback/value #(reset! email %))}]
       (if (= @payment-form :credit-card)
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
          [IbanElement {:className @className
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
                               (nil? @email)
                               (not @stripe)
                               (not @card-info-completed?)
                               (nil? @plan-id)
                               @processing?)}
        [ui/ButtonContent {:hidden true} [ui/Icon {:name "shop"}]]
        [ui/ButtonContent {:visible true} "Subscribe"]]])))


(defn ReactCheckoutForm []
  (let [elements (react-stripe/useElements)]
    (r/as-element
      [InternalCheckoutForm elements])))


(def CheckoutForm (r/adapt-react-class ReactCheckoutForm))


(defn PlanComp
  [id label price color logo]
  (let [selected-plan-id (subscribe [::subs/plan-id])]
    [ui/GridColumn
     [ui/Segment {:on-click   #(dispatch [::events/set-plan-id id])
                  :text-align "center"
                  :style      (cond-> {:cursor "pointer"}
                                      (= @selected-plan-id id) (assoc :border "2px solid #85b7d9"))}
      [ui/Header {:as :h2 :icon true :text-align "center"}
       [ui/Icon {:name logo, :color color}]
       label
       [ui/HeaderSubheader price]]
      [ui/Image {:src "https://react.semantic-ui.com/images/wireframe/media-paragraph.png"}]
      [ui/Image {:src "https://react.semantic-ui.com/images/wireframe/media-paragraph.png"}]
      [ui/Image {:src "https://react.semantic-ui.com/images/wireframe/media-paragraph.png"}]
      ]]))


(defn Pricing
  []
  (let [locale       (subscribe [::i18n-subs/locale])
        stripe       (subscribe [::subs/stripe])
        subscription (subscribe [::subs/subscription])
        plan-id      (subscribe [::subs/plan-id])]
    (js/console.log "INIT")
    (dispatch [::events/init])
    (reset! card-validation-error-message nil)
    (reset! card-info-completed? false)
    (fn []
      [ui/Segment (assoc style/basic, :loading (not @stripe))
       [ui/Grid {:stackable true, :columns 4}
        [ui/GridRow {:columns 4}
         [PlanComp "plan_Gx4S6VYf9cbfRK" "Free" "free" "black" "paper plane"]
         [PlanComp "plan_Gx23NQ4bczKU4e" "bronze" "500$ / month" "brown" "plane"]
         [PlanComp "plan_Gx43FhmevUCOau" "Silver" "4'000$ / month" "grey" "fighter jet"]
         [PlanComp "plan_Gx44NITOaAhpUU" "Gold" "15'000$ / month" "yellow" "space shuttle"]]
        [ui/GridRow {:columns 1}
         [ui/GridColumn
          [ui/ButtonGroup
           [ui/Button {:attached "left"
                       :active   (= @payment-form :credit-card)
                       :on-click #(reset! payment-form :credit-card)}
            "Credit Card"]
           [ui/Button {:attached "right"
                       :active   (= @payment-form :sepa-debit)
                       :on-click #(reset! payment-form :sepa-debit)}
            "SEPA Direct Debit payments"]]]]
        (when (and true #_@plan-id @stripe)
          [ui/GridColumn
           [ui/Segment {:compact true}
            ^{:key @locale}
            [Elements {:stripe  @stripe
                       :options {:locale @locale}}
             [CheckoutForm]]]])
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
        ]])))


(defmethod panel/render :pricing
  [path]
  [ui/Segment style/basic
   [uix/PageHeader "code" (str/upper-case "Pricing")]
   [Pricing]])