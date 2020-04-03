(ns sixsq.nuvla.ui.main.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.about.views]
    [sixsq.nuvla.ui.apps-application.views]
    [sixsq.nuvla.ui.apps-component.views]
    [sixsq.nuvla.ui.apps-project.views]
    [sixsq.nuvla.ui.apps-store.views]
    [sixsq.nuvla.ui.apps.events :as apps-events]
    [sixsq.nuvla.ui.apps.views]
    [sixsq.nuvla.ui.cimi.subs :as api-subs]
    [sixsq.nuvla.ui.cimi.views]
    [sixsq.nuvla.ui.credentials.events :as credential-events]
    [sixsq.nuvla.ui.credentials.views]
    [sixsq.nuvla.ui.dashboard.views]
    [sixsq.nuvla.ui.data.views]
    [sixsq.nuvla.ui.docs.views]
    [sixsq.nuvla.ui.edge.views]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.history.utils :as history-utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.i18n.views :as i18n-views]
    [sixsq.nuvla.ui.infrastructures-detail.views]
    [sixsq.nuvla.ui.infrastructures.events :as infra-service-events]
    [sixsq.nuvla.ui.infrastructures.views]
    [sixsq.nuvla.ui.main.events :as events]
    [sixsq.nuvla.ui.main.subs :as subs]
    [sixsq.nuvla.ui.main.views-sidebar :as sidebar]
    [sixsq.nuvla.ui.messages.views :as messages]
    [sixsq.nuvla.ui.ocre.views]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.profile.views]
    [sixsq.nuvla.ui.session.subs :as session-subs]
    [sixsq.nuvla.ui.session.views :as session-views]
    [sixsq.nuvla.ui.utils.general :as utils]
    [sixsq.nuvla.ui.utils.responsive :as responsive]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.welcome.views]
    ["@stripe/stripe-js" :as stripe]
    ["@stripe/react-stripe-js" :as react-stripe]))


(defn crumb
  [index segment]
  (let [nav-path (subscribe [::subs/nav-path])
        click-fn #(dispatch [::history-events/navigate (history-utils/trim-path @nav-path index)])]
    ^{:key (str index "_" segment)}
    [ui/BreadcrumbSection
     [:a {:on-click click-fn
          :style    {:cursor "pointer"}}
      (utils/truncate (str segment))]]))


(defn breadcrumbs-links []
  (let [nav-path (subscribe [::subs/nav-path])]
    (vec (concat [ui/Breadcrumb {:size :large}]
                 (->> @nav-path
                      (map crumb (range))
                      (interpose [ui/BreadcrumbDivider {:icon "chevron right"}]))))))


(defn breadcrumb-option
  [index segment]
  {:key   segment
   :value index
   :text  (utils/truncate segment 8)})


(defn breadcrumbs-dropdown []
  (let [path        (subscribe [::subs/nav-path])
        options     (map breadcrumb-option (range) @path)
        selected    (-> options last :value)
        callback-fn #(dispatch [::history-events/navigate (history-utils/trim-path @path %)])]
    [ui/Dropdown
     {:inline    true
      :value     selected
      :on-change (ui-callback/value callback-fn)
      :options   options}]))


(defn breadcrumbs []
  (let [device (subscribe [::subs/device])]
    (if (#{:mobile} @device)
      [breadcrumbs-dropdown]
      [breadcrumbs-links])))


(defn footer
  []
  (let [grid-style {:style {:padding-top    5
                            :padding-bottom 5}}]
    [ui/Segment {:style {:border-radius 0}}
     [ui/Grid {:columns 3}
      [ui/GridColumn grid-style "© 2020, SixSq Sàrl"]
      [ui/GridColumn (assoc grid-style :text-align "center")
       [:a {:on-click #(dispatch [::history-events/navigate "about"])
            :style    {:cursor "pointer"}}
        [:span#release-version (str "v")]]]
      [ui/GridColumn (assoc grid-style :text-align "right")
       [i18n-views/LocaleDropdown]]]]))


(defn ignore-changes-modal
  []
  (let [tr                (subscribe [::i18n-subs/tr])
        navigation-info   (subscribe [::subs/ignore-changes-modal])
        ignore-changes-fn #(dispatch [::events/ignore-changes false])]

    [ui/Modal {:open       (some? @navigation-info)
               :close-icon true
               :on-close   ignore-changes-fn}

     [ui/ModalHeader (str/capitalize (str (@tr [:ignore-changes?])))]

     [ui/ModalContent {:content (@tr [:ignore-changes-content])}]

     [ui/ModalActions
      [uix/Button {:text     (@tr [:ignore-changes]),
                   :positive true
                   :active   true
                   :on-click #(do (dispatch [::events/ignore-changes true])
                                  (dispatch [::apps-events/form-valid]))}]]]))


#_(defmulti BootstrapMessage identity)

#_(defmethod BootstrapMessage :no-swarm
    [_]
    (let [tr (subscribe [::i18n-subs/tr])]
      [ui/Message
       {:icon       "inbox"
        :info       true
        :on-dismiss #(dispatch [::events/set-bootsrap-message])
        :header     (@tr [:message-no-swarm])
        :content    (r/as-element
                      [:p (@tr [:message-to-create-one])
                       [:a
                        {:style    {:cursor "pointer"}
                         :on-click #(do (dispatch [::history-events/navigate "infrastructures"])
                                        (dispatch [::infra-service-events/open-add-service-modal]))}
                        (str " " (@tr [:click-here]))]])}]))


#_(defmethod BootstrapMessage :no-credential
    [_]
    (let [tr (subscribe [::i18n-subs/tr])]
      [ui/Message
       {:icon       "inbox"
        :info       true
        :on-dismiss #(dispatch [::events/set-bootsrap-message])
        :header     (@tr [:message-no-credential])
        :content    (r/as-element
                      [:p (@tr [:message-to-create-one])
                       [:a
                        {:style    {:cursor "pointer"}
                         :on-click #(do (dispatch [::history-events/navigate "credentials"])
                                        (dispatch [::credential-events/open-add-credential-modal]))}
                        (str " " (@tr [:click-here]))]])}]))


(defn Message
  []
  (let [tr      (subscribe [::i18n-subs/tr])
        message (subscribe [::subs/message])]
    (fn []
      (let [[type content] @message]
        (when content
          [ui/Container {:text-align :center}
           [ui/Message
            (if (= type :success)
              {:success true
               :content (@tr [(keyword content)])}
              {:error   true
               :content content})]
           [:br]])))))


(defn contents
  []
  (let [resource-path    (subscribe [::subs/nav-path])
        ;bootstrap-message (subscribe [::subs/bootstrap-message])
        content-key      (subscribe [::subs/content-key])
        is-small-device? (subscribe [::subs/is-small-device?])]
    (fn []
      [ui/Container
       (cond-> {:as    "main"
                :key   @content-key
                :id    "nuvla-ui-content"
                :fluid true}
               @is-small-device? (assoc :on-click #(dispatch [::events/close-sidebar])))

       [Message]

       #_(when @bootstrap-message
           [BootstrapMessage @bootstrap-message])
       (panel/render @resource-path)])))


(defn header
  []
  [:header
   [ui/Menu {:className  "nuvla-ui-header"
             :borderless true}

    [ui/MenuItem {:aria-label "toggle sidebar"
                  :link       true
                  :on-click   #(dispatch [::events/toggle-sidebar])}
     [ui/Icon {:name "bars"}]]

    [ui/MenuItem [breadcrumbs]]

    [ui/MenuMenu {:position "right"}
     [messages/bell-menu]
     [ui/MenuItem {:fitted true}
      [session-views/authn-menu]]]]

   [messages/alert-slider]
   [messages/alert-modal]])


(def stripe-promise (stripe/loadStripe "pk_test_Wo4so0qa2wqn66052FlvyMpl00MhPPQdAG"))
(js/console.log stripe-promise)

(def Elements (r/adapt-react-class react-stripe/Elements))
(def CardElement (r/adapt-react-class react-stripe/CardElement))
(def ElementsConsumer (r/adapt-react-class react-stripe/ElementsConsumer))


(def email (r/atom nil))

(def card-info-completed? (r/atom false))

(def card-validation-error-message (r/atom nil))

(def create-payment-method-result (r/atom nil))

(def create-payment-method-error (r/atom nil))

(def plan (r/atom nil))

(defn handle-submit
  [stripe elements event]
  (.preventDefault event)
  (when (and stripe elements)
    (let [promise (stripe.createPaymentMethod
                    #js{:type            "card"
                        :card            (elements.getElement react-stripe/CardElement)

                        :billing_details #js{:email "jenny.rosen@example.com"}
                        })]
      (-> promise
          (.then (fn [res]
                   (reset! create-payment-method-result res)
                   (js/console.log "res:" res))
                 (fn [err]
                   (js/console.log err)
                   (reset! create-payment-method-error err)))
          (.catch (fn [err] (js/console.log "catch err:" err)))))))


;; While not yet hooks support we have to use react components
;; https://github.com/reagent-project/reagent/blob/master/doc/ReactFeatures.md#hooks
(defn InternalCheckoutForm
  [stripe elements]
  (let [className (r/atom "stripe-input")]
    (reset! card-validation-error-message nil)
    (reset! card-info-completed? false)
    (fn [stripe elements]
      [ui/Form {:on-submit (partial handle-submit stripe elements)
                :style     {:width 400}
                :error true #_(boolean? @create-payment-method-error)}
       [ui/Message {:error true
                    :header "Something went wrong"
                    :content "aaa" #_@create-payment-method-error}]
       [ui/FormInput {:label     "Email"
                      :on-change (ui-callback/value #(reset! email %))}]
       [ui/FormField
        [:label "Card Number"]
        [CardElement {:className @className
                      :on-change (fn [event]
                                   (reset! card-validation-error-message (some-> event .-error .-message))
                                   (reset! card-info-completed? (.-complete event)))}]
        (when @card-validation-error-message
          [ui/Label {:basic true, :color "red", :pointing true} @card-validation-error-message])]
       [ui/Button {:type     "submit"
                   :animated "vertical"
                   :primary  true
                   :floated  "right"
                   :disabled (or (nil? @email)
                                 (not stripe-promise)
                                 (not @card-info-completed?))}
        [ui/ButtonContent {:hidden true} [ui/Icon {:name "shop"}]]
        [ui/ButtonContent {:visible true} "Subscribe"]]])))


(defn ReactCheckoutForm []
  (let [stripe   (react-stripe/useStripe)
        elements (react-stripe/useElements)]
    (r/as-element
      [InternalCheckoutForm stripe elements])))


(def CheckoutForm (r/adapt-react-class ReactCheckoutForm))


(defn PlanComp
  [key label price color logo]
  [ui/GridColumn
   [ui/Segment {:on-click   #(reset! plan key)
                :text-align "center"
                :style      (cond-> {:cursor "pointer"}
                                    (= @plan key) (assoc :border "2px solid #85b7d9"))}
    [ui/Header {:as :h2 :icon true :text-align "center"}
     [ui/Icon {:name logo, :color color}]
     label
     [ui/HeaderSubheader price]]
    [ui/Image {:src "https://react.semantic-ui.com/images/wireframe/media-paragraph.png"}]
    ]]
  )

(defn StripeDemo
  []
  (let [locale (subscribe [::i18n-subs/locale])]
    (fn []
      [:<>
       [:div {:style {:margin 20}}
        [:h1 "Stripe demo"]
        [ui/Grid {:stackable true}
         [ui/GridRow {:columns 4}
          [PlanComp :free "Free" "free" "black" "paper plane"]
          [PlanComp :bronze "bronze" "500$ / month" "brown" "plane"]
          [PlanComp :silver "Silver" "4'000$ / month" "grey" "fighter jet"]
          [PlanComp :gold "Gold" "15'000$ / month" "yellow" "space shuttle"]]
         (when true #_@plan
           [ui/GridRow {:columns 1}
            [ui/GridColumn
             [ui/Segment {:compact true}
              ^{:key @locale}
              [Elements {:stripe  stripe-promise
                         :options {:locale @locale}}
               [CheckoutForm]]]]
            ])
         ]]])))

(defn app []
  (fn []
    (let [show?            (subscribe [::subs/sidebar-open?])
          cep              (subscribe [::api-subs/cloud-entry-point])
          iframe?          (subscribe [::subs/iframe?])
          is-small-device? (subscribe [::subs/is-small-device?])
          resource-path    (subscribe [::subs/nav-path])
          session-loading? (subscribe [::session-subs/session-loading?])]
      (if (and @cep (not @session-loading?))
        [ui/Responsive {:as            "div"
                        :id            "nuvla-ui-main"
                        :fire-on-mount true
                        :on-update     (responsive/callback #(dispatch [::events/set-device %]))}
         (case (first @resource-path)
           "sign-in" [session-views/SessionPage]
           "sign-up" [session-views/SessionPage]
           "reset-password" [session-views/SessionPage]
           nil [session-views/SessionPage]
           [:<>
            [sidebar/menu]
            [:div {:style {:transition  "0.5s"
                           :margin-left (if (and (not @is-small-device?) @show?)
                                          sidebar/sidebar-width "0")}}
             [ui/Dimmer {:active   (and @is-small-device? @show?)
                         :inverted true
                         :style    {:z-index 999}
                         :on-click #(dispatch [::events/close-sidebar])}]
             [header]
             [StripeDemo]
             [contents]
             [ignore-changes-modal]
             (when-not @iframe? [footer])]]
           )]
        [ui/Container
         [ui/Loader {:active true :size "massive"}]]))))
