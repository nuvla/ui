(ns sixsq.nuvla.ui.profile.views
  (:require
    [cljs.spec.alpha :as s]
    [reagent.core :as r]
    [clojure.string :as str]
    [form-validator.core :as fv]
    ["@stripe/react-stripe-js" :as react-stripe]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.profile.events :as events]
    [sixsq.nuvla.ui.profile.subs :as subs]
    [sixsq.nuvla.ui.session.subs :as session-subs]
    [sixsq.nuvla.ui.utils.collapsible-card :as cc]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.spec :as us]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.values :as values]
    [taoensso.timbre :as log]))


;;; VALIDATION SPEC
(s/def ::current-password us/nonblank-string)
(s/def ::new-password us/acceptable-password?)
(s/def ::new-password-repeat us/nonblank-string)

(s/def ::credential-change-password
  (s/keys :req-un [::current-password
                   ::new-password
                   ::new-password-repeat]))


(defn password-repeat-check [form name]
  (let [password        (get-in @form [:names->value :new-password])
        password-repeat (get-in @form [:names->value name])]
    (when-not (= password password-repeat)
      [:new-password-repeat :password-not-equal])))


(defn modal-change-password []
  (let [open?     (subscribe [::subs/modal-open? :change-password])
        eroor     (subscribe [::subs/error-message])
        tr        (subscribe [::i18n-subs/tr])
        form-conf {:names->value      {:current-password    ""
                                       :new-password        ""
                                       :new-password-repeat ""}
                   :form-spec         ::credential-change-password
                   :names->validators {:new-password-repeat [password-repeat-check]}}
        form      (fv/init-form form-conf)
        spec->msg {::current-password   (@tr [:should-not-be-empty])
                   ::new-password       (@tr [:password-constraint])
                   :new-password-repeat (@tr [:passwords-doesnt-match])}]
    (fn []
      [ui/Modal
       {:size      :tiny
        :open      @open?
        :closeIcon true
        :on-close  #(do
                      (dispatch [::events/close-modal])
                      (reset! form (fv/init-form form-conf)))}

       [ui/ModalHeader (@tr [:change-password])]

       [ui/ModalContent

        (when @eroor
          [ui/Message {:negative  true
                       :size      "tiny"
                       :onDismiss #(dispatch [::events/clear-error-message])}
           [ui/MessageHeader (str/capitalize (@tr [:error]))]
           [:p @eroor]])

        [ui/Form
         [ui/FormInput
          {:name          :current-password
           :id            "current-password"
           :label         (str/capitalize (@tr [:current-password]))
           :required      true
           :icon          "key"
           :icon-position "left"
           :auto-focus    "on"
           :auto-complete "off"
           :type          "password"
           :on-change     (partial fv/event->names->value! form)
           :on-blur       (partial fv/event->show-message form)
           :error         (fv/?show-message form :current-password spec->msg)}]
         [ui/FormGroup {:widths 2}
          [ui/FormInput {:name          :new-password
                         :icon          "key"
                         :icon-position "left"
                         :required      true
                         :auto-complete "new-password"
                         :label         (str/capitalize (@tr [:new-password]))
                         :type          "password"
                         :on-change     (partial fv/event->names->value! form)
                         :on-blur       (partial fv/event->show-message form)
                         :error         (fv/?show-message form :new-password spec->msg)}]
          [ui/FormInput {:name      :new-password-repeat
                         :required  true
                         :label     (str/capitalize (@tr [:new-password-repeat]))
                         :type      "password"
                         :on-change (partial fv/event->names->value! form)
                         :on-blur   (partial fv/event->show-message form)
                         :error     (fv/?show-message form :new-password-repeat spec->msg)}]]]]

       [ui/ModalActions
        [uix/Button
         {:text     (str/capitalize (@tr [:change-password]))
          :positive true
          :on-click #(when (fv/validate-form-and-show? form)
                       (dispatch [::events/change-password
                                  (-> @form
                                      :names->value
                                      (dissoc :new-password-repeat))]))}]]])))


(defn Session
  []
  (let [tr      (subscribe [::i18n-subs/tr])
        session (subscribe [::session-subs/session])]
    [ui/Segment {:padded true, :color "teal", :style {:height "100%"}}
     [ui/Header {:as :h2 :dividing true} "Session"]
     (if @session
       [ui/Table {:basic "very"}
        [ui/TableBody
         [ui/TableRow
          [ui/TableCell {:width 5} [:b "Identifier"]]
          [ui/TableCell {:width 11} (:identifier @session)]]
         [ui/TableRow
          [ui/TableCell [:b (str/capitalize (@tr [:session-expires]))]]
          [ui/TableCell (-> @session :expiry time/parse-iso8601 time/ago)]
          ]
         [ui/TableRow
          [ui/TableCell [:b "User id"]]
          [ui/TableCell (values/as-href {:href (:user @session)})]
          ]
         [ui/TableRow
          [ui/TableCell [:b "Roles"]]
          [ui/TableCell (values/format-collection (sort (str/split (:roles @session) #"\s+")))]]]]
       [ui/Grid {:text-align     "center"
                 :vertical-align "middle"
                 :style          {:height "100%"}}
        [ui/GridColumn
         [ui/Header {:as :h3, :icon true, :disabled true, :text-align "center"}
          [ui/Icon {:className "fad fa-sign-in-alt"}]
          "No session"]]
        ])]))


(def Elements (r/adapt-react-class react-stripe/Elements))
(def CardElement (r/adapt-react-class react-stripe/CardElement))
(def ElementsConsumer (r/adapt-react-class react-stripe/ElementsConsumer))
(def IbanElement (r/adapt-react-class react-stripe/IbanElement))


(def card-info-completed? (r/atom false))

(def card-validation-error-message (r/atom nil))

(def payment-form (r/atom "credit-card"))

(def elements-atom (r/atom nil))


(defn handle-setup-intent-credit-card
  [elements event]
  (.preventDefault event)
  (when elements
    (dispatch [::events/confirm-card-setup
               #js{:payment_method
                   #js{"card" (elements.getElement react-stripe/CardElement)}}])))


(defn handle-setup-intent-sepa-debit
  [elements event]
  (.preventDefault event)
  (when elements
    (dispatch [::events/confirm-iban-setup
               #js{:payment_method
                   #js{:sepa_debit      (elements.getElement react-stripe/IbanElement)
                       :billing_details #js{:name "test"}}}])))



(defn PaymentMethodInputInternal
  [{:keys [type onChange options] :as props}]
  (case type
    "sepa_debit" [IbanElement
                  {:className "stripe-input"
                   :on-change onChange
                   :options   (clj->js options)}]
    "card" [CardElement {:className "stripe-input"
                         :on-change onChange}]
    [:div]))

;; While not yet hooks support we have to use react components
;; https://github.com/reagent-project/reagent/blob/master/doc/ReactFeatures.md#hooks

(defn PaymentMethodInputWrapper
  [props]
  (let [elements     (react-stripe/useElements)
        props        (js->clj props :keywordize-keys true)
        set-elements (:setElements props)]
    (when set-elements (set-elements elements))
    (r/as-element
      [PaymentMethodInputInternal props])))


(def PaymentMethodInputReactClass (r/adapt-react-class PaymentMethodInputWrapper))

(defn PaymentMethodInput
  [props]
  (let [locale (subscribe [::i18n-subs/locale])
        stripe (subscribe [::subs/stripe])]
    (fn [props]
      ^{:key (str @locale @stripe)}
      [Elements {:stripe  @stripe
                 :options {:locale @locale}}
       [PaymentMethodInputReactClass props]])))


;; VALIDATION SPEC
(s/def ::fullname us/nonblank-string)
(s/def ::street-address us/nonblank-string)
(s/def ::city us/nonblank-string)
(s/def ::country us/nonblank-string)
(s/def ::postal-code us/nonblank-string)
(s/def ::payment-method (s/nilable map?))

(s/def ::customer
  (s/keys :req-un [::fullname
                   ::street-address
                   ::city
                   ::country
                   ::postal-code]
          :opt-un [::payment-method]))

(defn CustomerFormFields
  [form]
  (let [tr                        (subscribe [::i18n-subs/tr])
        payment-form              (r/atom "card")
        elements                  (r/atom nil)
        card-validation-error-msg (r/atom nil)]
    (dispatch [::events/load-stripe])
    (fn [form]
      (let [should-not-be-empty-msg (@tr [:should-not-be-empty])
            spec->msg               {::fullname       should-not-be-empty-msg
                                     ::street-address should-not-be-empty-msg
                                     ::city           should-not-be-empty-msg
                                     ::country        should-not-be-empty-msg
                                     ::postal-code    should-not-be-empty-msg}]
        [:<>
         [ui/FormInput {:name      :fullname
                        :label     "Full Name"
                        :required  true
                        :on-change (partial fv/event->names->value! form)
                        :on-blur   (partial fv/event->show-message form)
                        :error     (fv/?show-message form :fullname spec->msg)}]
         [ui/FormGroup
          [ui/FormInput {:name      :street-address
                         :label     "Street Address"
                         :required  true
                         :on-change (partial fv/event->names->value! form)
                         :on-blur   (partial fv/event->show-message form)
                         :error     (fv/?show-message form :street-address spec->msg)
                         :width     10}]
          [ui/FormInput {:name      :postal-code
                         :label     "Zip/Postal Code"
                         :required  true
                         :on-change (partial fv/event->names->value! form)
                         :on-blur   (partial fv/event->show-message form)
                         :error     (fv/?show-message form :postal-code spec->msg)
                         :width     6}]]
         [ui/FormGroup {:widths 2}
          [ui/FormInput {:name      :city
                         :label     "City"
                         :required  true
                         :on-change (partial fv/event->names->value! form)
                         :on-blur   (partial fv/event->show-message form)
                         :error     (fv/?show-message form :city spec->msg)}]
          [ui/FormDropdown {:name        :country
                            :label       "Country"
                            :search      true
                            :selection   true
                            :required    true
                            :on-change   (fn [_ data]
                                           (let [name  (keyword (.-name data))
                                                 value (.-value data)]
                                             (swap! form #(assoc-in % [:names->value name] value))
                                             (fv/validate-form form)))
                            :error       (fv/?show-message form :country spec->msg)
                            :options     [{:key "ch", :value "CH", :flag "ch", :text "Switzerland"}
                                          {:key "at", :value "AT", :flag "at", :text "Austria"}]
                            :placeholder "Select Country"}]]


         [ui/FormGroup {:inline true}
          [:label "Payment Method"]
          [ui/FormRadio {:label     "Credit Card"
                         :checked   (= @payment-form "card")
                         :on-change (ui-callback/value #(reset! payment-form "card"))}]
          [ui/FormRadio {:label     "Bank Account"
                         :checked   (= @payment-form "sepa_debit")
                         :on-change (ui-callback/value #(reset! payment-form "sepa_debit"))}]]
         [ui/FormField {:style {:max-width 380}}
          [:label "Card Number"]
          [PaymentMethodInput
           (cond-> {:type         @payment-form
                    :set-elements #(reset! elements %)
                    :on-change    (fn [event]
                                    (let [error       (some-> event .-error .-message)
                                          empty-field (.-empty event)
                                          swap-form!  (fn [v]
                                                        (swap! form
                                                               #(assoc-in % [:names->value
                                                                             :payment-method] v)))]
                                      (reset! card-validation-error-msg error)
                                      (cond
                                        error (swap-form! error)
                                        (.-complete event) (swap-form! {:type     @payment-form
                                                                        :elements @elements})
                                        empty-field (swap-form! nil)
                                        (not empty-field) (swap-form! "in-progress")))
                                    (fv/validate-form form))}
                   (= @payment-form "sepa_debit") (assoc :options {:supportedCountries ["SEPA"]
                                                                   :placeholderCountry "CH"}))]
          (when @card-validation-error-msg
            [ui/Label {:basic true, :color "red", :pointing true} @card-validation-error-msg])]]
        ))))

(defn customer-form->customer
  [form]
  (let [{:keys [fullname payment-method] :as customer} (:names->value @form)]
    (cond-> {:fullname     fullname
             :address      (select-keys customer [:street-address
                                                  :city
                                                  :country
                                                  :postal-code])
             :subscription {:plan-id       "plan_HGQ9iUgnz2ho8e"
                            :plan-item-ids ["plan_HGQIIWmhYmi45G"
                                            "plan_HIrgmGboUlLqG9"
                                            "plan_HGQAXewpgs9NeW"
                                            "plan_HGQqB0p8h86Ija"]}}
            payment-method (assoc :payment-method payment-method))))

(defn SubscribeButton
  []
  (let [stripe                   (subscribe [::subs/stripe])
        loading-create-payment?  (subscribe [::subs/loading? :create-payment])
        loading-customer?        (subscribe [::subs/loading? :customer])
        open?                    (subscribe [::subs/modal-open? :subscribe])
        disabled?                (subscribe [::subs/subscribe-button-disabled?])
        session                  (subscribe [::session-subs/session])
        error                    (subscribe [::subs/error-message])
        loading-payment?         (subscribe [::subs/loading? :create-payment])
        loading-create-customer? (subscribe [::subs/loading? :create-customer])
        form-conf                {:form-spec    ::customer
                                  :names->value {:fullname       ""
                                                 :street-address ""
                                                 :city           ""
                                                 :country        ""
                                                 :postal-code    ""}}
        form                     (fv/init-form form-conf)]
    (fn []
      [:<>
       [ui/Modal
        {:open       @open?
         :size       "small"
         :on-close   #(dispatch [::events/close-modal])
         :close-icon true}
        [ui/ModalHeader "Subscribe"]
        [ui/ModalContent
         [ui/Form {:error   (boolean @error)
                   :loading (or @loading-payment? @loading-create-customer?)}
          [ui/Message {:error   true
                       :header  "Something went wrong"
                       :content @error}]
          [CustomerFormFields form]]]
        [ui/ModalActions
         [ui/Button {:animated "vertical"
                     :primary  true
                     :on-click #(when (fv/validate-form-and-show? form)
                                  (dispatch [::events/create-payment-method
                                             (customer-form->customer form)]))
                     :disabled (or
                                 (not @stripe)
                                 @loading-create-payment?
                                 @loading-create-customer?
                                 (not (fv/form-valid? form)))}
          [ui/ButtonContent {:hidden true} [ui/Icon {:name "shop"}]]
          [ui/ButtonContent {:visible true} "Subscribe"]]]]
       [ui/Button {:primary  true
                   :circular true
                   :basic    true
                   :loading  @loading-customer?
                   :disabled @disabled?
                   :on-click (if @session
                               #(dispatch [::events/open-modal :subscribe])
                               #(dispatch [::history-events/navigate "sign-up"]))}
        "Try Nuvla for free for 14 days"]
       ])))


(defn Subscription
  []
  (let [tr           (subscribe [::i18n-subs/tr])
        locale       (subscribe [::i18n-subs/locale])
        subscription (subscribe [::subs/subscription])
        loading?     (subscribe [::subs/loading? :subscription])]
    (fn []
      (let [{:keys [status start-date trial-start trial-end
                    current-period-start current-period-end]} @subscription]
        [ui/Segment {:padded  true
                     :color   "red"
                     :loading @loading?
                     :style   {:height "100%"}}
         [ui/Header {:as :h2 :dividing true} "Subscription"]
         (if @subscription
           [ui/Table {:basic "very"}
            [ui/TableBody
             [ui/TableRow
              [ui/TableCell {:width 5} [:b "Status"]]
              [ui/TableCell {:width 11} (str/capitalize status)]]
             [ui/TableRow
              [ui/TableCell [:b "Start date"]]
              [ui/TableCell (some-> start-date (time/time->format "LLL" @locale))]]
             (when (= status "trialing")
               [:<>
                [ui/TableRow
                 [ui/TableCell [:b "Trial start date"]]
                 [ui/TableCell (some-> trial-start
                                       (time/time->format "LLL" @locale))]]
                [ui/TableRow
                 [ui/TableCell [:b "Trial end date"]]
                 [ui/TableCell (some-> trial-end (time/time->format "LLL" @locale))]]])
             [ui/TableRow
              [ui/TableCell [:b "Current period start"]]
              [ui/TableCell (some-> current-period-start (time/time->format "LLL" @locale))]]
             [ui/TableRow
              [ui/TableCell [:b "Current period end"]]
              [ui/TableCell (some-> current-period-end (time/time->format "LLL" @locale))]
              ]]]
           [ui/Grid {:text-align     "center"
                     :vertical-align "middle"
                     :style          {:height "100%"}}
            [ui/GridColumn
             [ui/Header {:as :h3, :icon true, :disabled true}
              [ui/Icon {:className "fad fa-money-check-edit"}]
              "Not subscribed yet"]
             [:br]
             [SubscribeButton]]
            ])]))))


(defn AddPaymentMethodButton
  []
  (let [stripe                 (subscribe [::subs/stripe])
        loading-setup-intent?  (subscribe [::subs/loading? :create-setup-intent])
        loading-confirm-setup? (subscribe [::subs/loading? :confirm-setup-intent])
        disabled?              (subscribe [::subs/cannot-create-setup-intent?])
        open?                  (subscribe [::subs/modal-open? :add-payment-method])
        error                  (subscribe [::subs/error-message])]
    (reset! elements-atom nil)
    (fn []
      [:<>
       [ui/Modal
        {:open       @open?
         :size       "small"
         :on-close   #(dispatch [::events/close-modal])
         :close-icon true}
        [ui/ModalHeader "Add payment method"]
        [ui/ModalContent
         [ui/Form {:error   (boolean @error)
                   :loading @loading-confirm-setup?}
          [ui/Message {:error   true
                       :header  "Something went wrong"
                       :content @error}]
          [ui/FormGroup {:inline true}
           [:label "Billing Method"]
           [ui/FormRadio {:label     "Credit Card"
                          :checked   (= @payment-form "card")
                          :on-change (ui-callback/value #(reset! payment-form "card"))}]
           [ui/FormRadio {:label     "Bank Account"
                          :checked   (= @payment-form "sepa_debit")
                          :on-change (ui-callback/value #(reset! payment-form "sepa_debit"))}]]
          [ui/FormField {:width 9}
           [PaymentMethodInput
            (cond-> {:type         @payment-form
                     :on-change    (fn [event]
                                     (reset! card-validation-error-message
                                             (some-> event .-error .-message))
                                     (reset! card-info-completed? (.-complete event)))
                     :set-elements #(reset! elements-atom %)}
                    (= @payment-form "sepa_debit") (assoc :options {:supportedCountries ["SEPA"]
                                                                    :placeholderCountry "CH"}))]]]]
        [ui/ModalActions
         [ui/Button {:primary  true
                     :on-click (partial (if (= @payment-form "card")
                                          handle-setup-intent-credit-card
                                          handle-setup-intent-sepa-debit) @elements-atom)
                     :disabled (or
                                 (not @elements-atom)
                                 (not @stripe)
                                 (not @card-info-completed?)
                                 @loading-setup-intent?)}
          "Add"]]]
       [ui/Button {:primary  true
                   :circular true
                   :basic    true
                   :size     "small"
                   :disabled @disabled?
                   :on-click #(do
                                (dispatch [::events/create-setup-intent])
                                (dispatch [::events/open-modal :add-payment-method]))}
        [ui/Icon {:name "plus square outline"}]
        "Add"]])))


(defn PaymentMethods
  []
  (let [tr                  (subscribe [::i18n-subs/tr])
        loading?            (subscribe [::subs/loading? :payment-methods])
        customer            @(subscribe [::subs/customer])
        cards-bank-accounts (subscribe [::subs/cards-bank-accounts])
        default             @(subscribe [::subs/default-payment-method])]
    [ui/Segment {:padded  true
                 :color   "purple"
                 :loading @loading?
                 :style   {:height "100%"}}
     [ui/Header {:as :h2 :dividing true} "Payment Methods"]
     (if @cards-bank-accounts
       [:<>
        [ui/Table {:basic "very"}
         [ui/TableBody
          (for [{:keys [last4 brand payment-method exp-month exp-year]} @cards-bank-accounts]
            (let [is-default? (= default payment-method)]
              ^{:key (str payment-method)}
              [ui/TableRow
               [ui/TableCell
                [ui/Icon {:name (case brand
                                  "visa" "cc visa"
                                  "mastercard" "cc mastercard"
                                  "amex" "cc amex"
                                  "iban" "building"
                                  "payment")
                          :size "large"}]
                (str/upper-case brand)]
               [ui/TableCell "•••• " last4 " "
                (when is-default?
                  [ui/Label {:size :tiny :circular true :color "blue"} "default"])]
               [ui/TableCell {:style {:color "grey"}}
                (when (and exp-month exp-year)
                  (str (general-utils/format "%02d" exp-month) "/" exp-year))]
               [ui/TableCell
                [ui/ButtonGroup {:basic true :size "small" :icon true :floated "right"}
                 (when-not is-default?
                   [ui/Popup
                    {:position "top center"
                     :content  "Set as default"
                     :trigger  (r/as-element
                                 [ui/Button
                                  {:on-click #(dispatch [::events/set-default-payment-method
                                                         payment-method])}
                                  [ui/Icon {:name "pin"}]])}])
                 [ui/Popup
                  {:position "top center"
                   :content  "Delete"
                   :trigger  (r/as-element [ui/Button
                                            {:on-click #(dispatch [::events/detach-payment-method
                                                                   payment-method])}
                                            [ui/Icon {:name "trash", :color "red"}]])}]]
                ]]))
          [ui/TableRow
           [ui/TableCell {:col-span 4}
            [AddPaymentMethodButton]
            ]]]]]
       [ui/Grid {:text-align     "center"
                 :vertical-align "middle"
                 :style          {:height "100%"}}
        [ui/GridColumn
         [ui/Header {:as :h3, :icon true, :disabled true}
          [ui/Icon {:className "fad fa-credit-card"}]
          "Payment method"]
         (when customer
           [:<>
            [:br]
            [AddPaymentMethodButton]])]])]))


(defn format-currency
  [currency amount]
  (str (if (= currency "eur") "€" currency)
       " " (general-utils/format "%.2f" amount)))


(defn UpcomingInvoice
  []
  (let [tr               (subscribe [::i18n-subs/tr])
        locale           @(subscribe [::i18n-subs/locale])
        loading?         (subscribe [::subs/loading? :upcoming-invoice])
        upcoming-invoice @(subscribe [::subs/upcoming-invoice])
        upcoming-lines   @(subscribe [::subs/upcoming-invoice-lines])]
    [ui/Segment {:padded  true
                 :color   "brown"
                 :loading @loading?
                 :style   {:height "100%"}}
     [ui/Header {:as :h2 :dividing true} "Upcoming Invoice"]
     (if upcoming-invoice
       [ui/Table
        [ui/TableHeader
         [ui/TableRow
          [ui/TableHeaderCell "Description"]
          [ui/TableHeaderCell "Amount"]]]
        [ui/TableBody
         (for [[period lines] upcoming-lines]
           ^{:key (str period)}
           [:<>
            [ui/TableRow
             [ui/TableCell {:col-span 2, :style {:color "grey"}}
              (str (some-> period :start (time/time->format "LL" locale))
                   " - "
                   (some-> period :end (time/time->format "LL" locale)))]]
            (for [{:keys [description amount currency] :as line} lines]
              ^{:key (str period description)}
              [ui/TableRow
               [ui/TableCell description]
               [ui/TableCell
                (format-currency currency amount)]])])]
        [ui/TableFooter
         [ui/TableRow
          [ui/TableCell [:b "Total"]]
          [ui/TableCell
           [:b (format-currency (:currency upcoming-invoice) (:total upcoming-invoice))]]]]]
       [ui/Grid {:text-align     "center"
                 :vertical-align "middle"
                 :style          {:height "100%"}}
        [ui/GridColumn
         [ui/Header {:as :h3, :icon true, :disabled true}
          [ui/Icon {:className "fad fa-file-invoice"}]
          "Not any"]]])]))


(defn Invoices
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        locale   @(subscribe [::i18n-subs/locale])
        loading? @(subscribe [::subs/loading? :invoices])
        invoices @(subscribe [::subs/invoices])]
    [ui/Segment {:padded  true
                 :color   "yellow"
                 :loading loading?
                 :style   {:height "100%"}}
     [ui/Header {:as :h2 :dividing true} "Invoices"]
     (if invoices
       [ui/Table
        [ui/TableHeader
         [ui/TableRow
          [ui/TableHeaderCell "Number"]
          [ui/TableHeaderCell "Created"]
          [ui/TableHeaderCell "Status"]
          [ui/TableHeaderCell "Due date"]
          [ui/TableHeaderCell "Total"]
          [ui/TableHeaderCell "Download"]]]
        [ui/TableBody
         (for [{:keys [number created status due-date invoice-pdf currency total]} invoices]
           ^{:key (str number)}
           [ui/TableRow
            [ui/TableCell number]
            [ui/TableCell (some-> created (time/time->format "LL" locale))]
            [ui/TableCell (str/capitalize status)]
            [ui/TableCell (if due-date (some-> due-date (time/time->format "LL" locale)) "-")]
            [ui/TableCell (format-currency currency total)]
            [ui/TableCell
             (when invoice-pdf
               [ui/Button {:basic true
                           :icon  "download"
                           :href  invoice-pdf}])]])]]
       [ui/Grid {:text-align     "center"
                 :vertical-align "middle"
                 :style          {:height "100%"}}
        [ui/GridColumn
         [ui/Header {:as :h3, :icon true, :disabled true}
          [ui/Icon {:className "fad fa-file-invoice-dollar"}]
          "Not any"]]])]))


(defn Content
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        cred-pass (subscribe [::subs/credential-password])
        session   (subscribe [::session-subs/session])
        is-admin? (subscribe [::session-subs/is-admin?])]
    (dispatch [::events/init])
    (reset! card-validation-error-message nil)
    (reset! card-info-completed? false)
    (fn []
      (let [show-sections (and @session (not @is-admin?))]
        [:<>
         [uix/PageHeader "user" "Profile"]
         [ui/Menu {:borderless true}
          [ui/MenuItem {:disabled (nil? @cred-pass)
                        :content  (str/capitalize (@tr [:change-password]))
                        :on-click #(dispatch [::events/open-modal :change-password])}]]
         [ui/Grid {:stackable true}

          [ui/GridRow {:columns 2}
           [ui/GridColumn
            [Session]]
           [ui/GridColumn
            (when show-sections
              [Subscription])]]
          (when show-sections
            [:<>
             [ui/GridRow {:columns 2}
              [ui/GridColumn
               [UpcomingInvoice]]
              [ui/GridColumn
               [Invoices]]]
             [ui/GridRow {:columns 2}
              [ui/GridColumn
               [PaymentMethods]]
              [ui/GridColumn
               [ui/Segment {:padded true
                            :color  "blue"}
                [ui/Header {:as :h3 :dividing true} "Usage"]]]]]
            )]]))))


(defmethod panel/render :profile
  [path]
  [:div
   [Content]
   [modal-change-password]])
