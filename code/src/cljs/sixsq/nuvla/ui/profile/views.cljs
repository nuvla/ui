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
    [sixsq.nuvla.ui.utils.values :as values]))


(defn tuple-to-row [[v1 v2]]
  [ui/TableRow
   [ui/TableCell {:collapsing true} (str v1)]
   [ui/TableCell v2]])


(def data-to-tuple
  (juxt (comp name first) (comp values/format-value second)))


(defn format-roles
  [{:keys [roles] :as m}]
  (assoc m :roles (values/format-collection (sort (str/split roles #"\s+")))))


(defn user-as-link
  [{:keys [user] :as m}]
  (assoc m :user (values/as-href {:href user})))


(def session-keys #{:user :roles :clientIP})


(def session-keys-order {:user 1, :clientIP 2, :expiry 3, :roles 4})


(defn add-index
  [[k _ :as entry]]
  (-> k
      (session-keys-order 5)
      (cons entry)))


(defn process-session-data
  [{:keys [expiry] :as data}]
  (let [locale (subscribe [::i18n-subs/locale])]
    (->> (select-keys data session-keys)
         (cons [:expiry (time/remaining expiry @locale)])
         (map add-index)
         (sort-by first)
         (map rest))))


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


(defn session-info
  []
  (let [tr                  (subscribe [::i18n-subs/tr])
        session             (subscribe [::session-subs/session])
        credential-password (subscribe [::subs/credential-password])]
    (fn []
      [ui/Segment style/basic
       (when @session
         (when-not @credential-password
           (dispatch [::events/get-user]))
         [cc/metadata
          {:title       (:identifier @session)
           :icon        "user"
           :description (str (@tr [:session-expires]) " "
                             (-> @session :expiry time/parse-iso8601 time/ago))}
          (->> @session
               general-utils/remove-common-attrs
               user-as-link
               format-roles
               process-session-data
               (map data-to-tuple)
               (map tuple-to-row))])
       (when @credential-password
         [ui/Button {:primary  true
                     :on-click #(dispatch [::events/open-modal :change-password])}
          (str/capitalize (@tr [:change-password]))])
       (when-not @session
         [:p (@tr [:no-session])])])))


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
          [ui/Icon {:name "search"}]
          "No session"]]
        ])]))




(def Elements (r/adapt-react-class react-stripe/Elements))
(def CardElement (r/adapt-react-class react-stripe/CardElement))
(def ElementsConsumer (r/adapt-react-class react-stripe/ElementsConsumer))
(def IbanElement (r/adapt-react-class react-stripe/IbanElement))


(def email (r/atom nil))

(def card-info-completed? (r/atom false))

(def card-validation-error-message (r/atom nil))

(def payment-form (r/atom "credit-card"))

(def elements-atom (r/atom nil))

(defn handle-submit-credit-card
  [elements event]
  (.preventDefault event)
  (when elements
    (dispatch [::events/create-payment-method
               #js{:type "card"
                   :card (elements.getElement react-stripe/CardElement)}])))


(defn handle-submit-sepa-debit
  [elements event]
  (.preventDefault event)
  (when elements
    (dispatch [::events/create-payment-method
               #js{:type            "sepa_debit"
                   :sepa_debit      (elements.getElement react-stripe/IbanElement)
                   :billing_details #js{:name  "test"
                                        :email "test@example.com"}}])))


(defn handle-setup-intent-credit-card
  [elements event]
  (.preventDefault event)
  (when elements
    (dispatch [::events/confirm-card-setup
               #js{:payment_method
                   #js{:card (elements.getElement react-stripe/CardElement)}}])))


(defn handle-setup-intent-sepa-debit
  [elements event]
  (.preventDefault event)
  (when elements
    (dispatch [::events/confirm-iban-setup
               #js{:payment_method
                   #js{:sepa_debit      (elements.getElement react-stripe/IbanElement)
                       :billing_details #js{:name "test"}}}])))


;; While not yet hooks support we have to use react components
;; https://github.com/reagent-project/reagent/blob/master/doc/ReactFeatures.md#hooks
(defn InternalCheckoutForm
  [elements]
  (let [className (r/atom "stripe-input")
        error     (subscribe [::subs/error-message])
        loading?  (subscribe [::subs/loading? :create-payment])]
    (fn []
      [ui/Form {:error   (boolean @error)
                :loading @loading?}
       [ui/Message {:error   true
                    :header  "Something went wrong"
                    :content @error}]
       [ui/FormGroup {:widths 3}
        [ui/FormInput {:label "Company"}]]
       [ui/FormGroup {:widths "equal"}
        [ui/FormInput {:label    "Last Name"
                       :required true}]
        [ui/FormInput {:label    "First Name"
                       :required true}]]
       [ui/FormGroup
        [ui/FormDropdown {:label       "Country"
                          :search      true
                          :selection   true
                          :required    true
                          :width       6
                          :options     [{:key "ch", :value "ch", :flag "ch", :text "Switzerland"}
                                        {:key "at", :value "at", :flag "at", :text "Austria"}]
                          :placeholder "Select Country"}]
        [ui/FormInput {:label    "Street Address"
                       :required true
                       :width    10}]]
       [ui/FormGroup {:widths "equal"}
        [ui/FormInput {:label    "City"
                       :required true}]
        [ui/FormInput {:label    "Zip/Postal Code"
                       :required true}]
        [ui/FormInput {:label "Phone Number"}]]
       [ui/FormGroup {:inline true}
        [:label "Billing Method"]
        #_{:label         "Choose your payment method"
           :default-value "credit-card"
           :on-change     (ui-callback/value #(reset! payment-form %))
           :options       [{:text "credit card", :value "credit-card", :icon "credit card"}
                           {:text "bank account", :value "sepa-debit", :icon "exchange"}]}
        [ui/FormRadio {:label     "Credit Card"
                       :checked   (= @payment-form "credit-card")
                       :on-change (ui-callback/value #(reset! payment-form "credit-card"))}]
        [ui/FormRadio {:label     "Bank Account"
                       :checked   (= @payment-form "sepa-debit")
                       :on-change (ui-callback/value #(reset! payment-form "sepa-debit"))}]]
       (if (= @payment-form "credit-card")
         [ui/FormGroup
          [ui/FormField {:width 8}
           [:label "Card Number"]
           [CardElement {:className @className
                         :on-change (fn [event]
                                      (reset! card-validation-error-message
                                              (some-> event .-error .-message))
                                      (reset! card-info-completed? (.-complete event)))}]
           (when @card-validation-error-message
             [ui/Label {:basic true, :color "red", :pointing true} @card-validation-error-message])]]
         [ui/FormField {:width 8}
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
       ])))


(defn ReactCheckoutForm []
  (let [elements (react-stripe/useElements)]
    (reset! elements-atom elements)
    (r/as-element
      [InternalCheckoutForm])))


(def CheckoutForm (r/adapt-react-class ReactCheckoutForm))

(defn SubscribeButton
  []
  (let [locale                  (subscribe [::i18n-subs/locale])
        stripe                  (subscribe [::subs/stripe])
        loading-create-payment? (subscribe [::subs/loading? :create-payment])
        loading-customer?       (subscribe [::subs/loading? :customer])
        open?                   (subscribe [::subs/modal-open? :subscribe])
        disabled?               (subscribe [::subs/subscribe-button-disabled?])
        session                 (subscribe [::session-subs/session])]
    (reset! elements-atom nil)
    (fn []
      [:<>
       [ui/Modal
        {:open       @open?
         :size       "small"
         :on-close   #(dispatch [::events/close-modal])
         :close-icon true}
        [ui/ModalHeader "Subscribe"]
        [ui/ModalContent
         ^{:key @locale}
         [Elements {:stripe  @stripe
                    :options {:locale @locale}}
          [CheckoutForm]]]
        [ui/ModalActions
         [ui/Button {:animated "vertical"
                     :primary  true
                     :on-click (partial (if (= @payment-form "credit-card")
                                          handle-submit-credit-card
                                          handle-submit-sepa-debit) @elements-atom)
                     :disabled (or
                                 (not @elements-atom)
                                 (not @stripe)
                                 (not @card-info-completed?)
                                 @loading-create-payment?)}
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
  (let [tr          (subscribe [::i18n-subs/tr])
        locale      (subscribe [::i18n-subs/locale])
        subscrption (subscribe [::subs/subscription])
        loading?    (subscribe [::subs/loading? :customer])]
    [ui/Segment {:padded  true
                 :color   "red"
                 :loading @loading?
                 :style   {:height "100%"}}
     [ui/Header {:as :h2 :dividing true} "Subscription"]
     (if @subscrption
       [ui/Table {:basic "very"}
        [ui/TableBody
         [ui/TableRow
          [ui/TableCell {:width 5} [:b "Status"]]
          [ui/TableCell {:width 11} (str/capitalize (:status @subscrption))]]
         [ui/TableRow
          [ui/TableCell [:b "Start date"]]
          [ui/TableCell (some-> @subscrption :start-date (time/time->format "LLL" @locale))]]
         [ui/TableRow
          [ui/TableCell [:b "Trial start date"]]
          [ui/TableCell (some-> @subscrption :trial-start (time/time->format "LLL" @locale))]]
         [ui/TableRow
          [ui/TableCell [:b "Trial end date"]]
          [ui/TableCell (some-> @subscrption :trial-end (time/time->format "LLL" @locale))]]
         [ui/TableRow
          [ui/TableCell [:b "Current period start"]]
          [ui/TableCell (some-> @subscrption :current-period-start
                                (time/time->format "LLL" @locale))]]
         [ui/TableRow
          [ui/TableCell [:b "Current period end"]]
          [ui/TableCell (some-> @subscrption :current-period-end
                                (time/time->format "LLL" @locale))]
          ]]]
       [ui/Grid {:text-align     "center"
                 :vertical-align "middle"
                 :style          {:height "100%"}}
        [ui/GridColumn
         [ui/Header {:as :h3, :icon true, :disabled true}
          [ui/Icon {:name "edit"}]
          "Not subscribed yet"]
         [:br]
         [SubscribeButton]]
        ])]))


(defn PaymentMethodInputInternal
  [{:keys [type onChange options] :as props}]
  (case type
    "sepa-debit" [IbanElement
                  {:className "stripe-input"
                   :on-change onChange
                   :options   (clj->js options)}]
    "credit-card" [CardElement {:className "stripe-input"
                                :on-change onChange}]
    [:div]))


(defn PaymentMethodInputWrapper
  [props]
  (let [elements (react-stripe/useElements)]
    (reset! elements-atom elements)
    (r/as-element
      [PaymentMethodInputInternal (js->clj props :keywordize-keys true)])))


(def PaymentMethodInputReactClass (r/adapt-react-class PaymentMethodInputWrapper))

(defn PaymentMethodInput
  [props]
  (let [locale (subscribe [::i18n-subs/locale])
        stripe (subscribe [::subs/stripe])]
    (fn [props]
      ^{:key @locale}
      [Elements {:stripe  @stripe
                 :options {:locale @locale}}
       [PaymentMethodInputReactClass props]])))


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
                          :checked   (= @payment-form "credit-card")
                          :on-change (ui-callback/value #(reset! payment-form "credit-card"))}]
           [ui/FormRadio {:label     "Bank Account"
                          :checked   (= @payment-form "sepa-debit")
                          :on-change (ui-callback/value #(reset! payment-form "sepa-debit"))}]]
          [ui/FormField
           [PaymentMethodInput
            (cond-> {:type      @payment-form
                     :on-change (fn [event]
                                  (reset! card-validation-error-message
                                          (some-> event .-error .-message))
                                  (reset! card-info-completed? (.-complete event)))}
                    (= @payment-form "sepa-debit") (assoc :options {:supportedCountries ["SEPA"]
                                                                    :placeholderCountry "CH"}))]]]]
        [ui/ModalActions
         [ui/Button {:primary  true
                     :on-click (partial (if (= @payment-form "credit-card")
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
        cards-bank-accounts (subscribe [::subs/cards-bank-accounts])
        default             @(subscribe [::subs/default-payment-method])]
    [ui/Segment {:padded  true
                 :color   "purple"
                 :loading @loading?
                 :style   {:height "100%"}}
     [ui/Header {:as :h2 :dividing true} "Payment Methods"]
     (js/console.log @cards-bank-accounts)
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
               (when (and exp-month exp-year)
                 [ui/TableCell {:style {:color "grey"}}
                  (str (if (= (count (str exp-month)) 1)
                         (str "0" exp-month)
                         exp-month) "/" exp-year)])
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
            ]]
          ]]]
       [ui/Grid {:text-align     "center"
                 :vertical-align "middle"
                 :style          {:height "100%"}}
        [ui/GridColumn
         [ui/Header {:as :h3, :icon true, :disabled true}
          [ui/Icon {:name "payment"}]
          "Add your first payment method"]
         [:br]
         [AddPaymentMethodButton]]])]))


(defn Content
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        cred-pass (subscribe [::subs/credential-password])
        session   (subscribe [::session-subs/session])]
    (dispatch [::events/init])
    (reset! card-validation-error-message nil)
    (reset! card-info-completed? false)
    (fn []
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
          [Subscription]]]
        (when @session
          [:<>
           [ui/GridRow {:columns 2}
            [ui/GridColumn
             [ui/Segment {:padded true
                          :color  "blue"}
              [ui/Header {:as :h3 :dividing true} "Usage"]]]
            [ui/GridColumn
             [ui/Segment {:padded true
                          :color  "brown"}
              [ui/Header {:as :h3 :dividing true} "Next Bill"]]]]
           [ui/GridRow {:columns 2}
            [ui/GridColumn
             [ui/Segment {:padded true
                          :color  "yellow"}
              [ui/Header {:as :h3 :dividing true} "Invoices"]]]
            [ui/GridColumn
             [PaymentMethods]]]]
          )]])))


(defmethod panel/render :profile
  [path]
  [:div
   #_[session-info]
   [Content]
   [modal-change-password]])
