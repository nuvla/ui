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
    [sixsq.nuvla.ui.authn.views :as authn-views]
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
    [sixsq.nuvla.ui.utils.general :as utils]
    [sixsq.nuvla.ui.utils.responsive :as responsive]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.welcome.views]
    [taoensso.timbre :as log]))


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
      [ui/GridColumn grid-style "© 2019, SixSq Sàrl"]
      [ui/GridColumn (assoc grid-style :text-align "center")
       [:a {:on-click #(dispatch [::history-events/navigate "about"])
            :style    {:cursor "pointer"}}
        [:span#release-version (str "v")]]]
      [ui/GridColumn (assoc grid-style :text-align "right")
       [i18n-views/locale-dropdown]]]]))


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


(defmulti BootstrapMessage identity)

(defmethod BootstrapMessage :no-swarm
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


(defmethod BootstrapMessage :no-credential
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
  (let [resource-path     (subscribe [::subs/nav-path])
        bootstrap-message (subscribe [::subs/bootstrap-message])
        content-key       (subscribe [::subs/content-key])
        is-small-device?  (subscribe [::subs/is-small-device?])]
    (fn []
      [ui/Container
       (cond-> {:as    "main"
                :key   @content-key
                :id    "nuvla-ui-content"
                :fluid true}
               @is-small-device? (assoc :on-click #(dispatch [::events/close-sidebar])))

       [Message]

       (when @bootstrap-message
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
      [authn-views/authn-menu]]]]

   [messages/alert-slider]
   [messages/alert-modal]])


(defn app []
  (fn []
    (let [show?            (subscribe [::subs/sidebar-open?])
          cep              (subscribe [::api-subs/cloud-entry-point])
          iframe?          (subscribe [::subs/iframe?])
          is-small-device? (subscribe [::subs/is-small-device?])]
      (if @cep
        [ui/Responsive {:as            "div"
                        :id            "nuvla-ui-main"
                        :fire-on-mount true
                        :on-update     (responsive/callback #(dispatch [::events/set-device %]))}
         [ui/Grid {:stackable true
                   :columns   2
                   :reversed  "mobile"
                   :style     {:margin           0
                               :background-color "white"}}
          [ui/GridColumn {:style {:background-image    "url(/ui/images/volumlight.png)"
                                  :background-size     "cover"
                                  :background-position "left"
                                  :background-repeat   "no-repeat"
                                  :color               "white"
                                  :min-height          "100vh"}}
           [:div {:style {:padding "75px"}}
            [:div {:style {:font-size "2em"}}
             "Welcome to"]
            [:div {:style {:font-size   "6em"
                           :line-height "normal"}}
             "Nuvla"]
            [:br]

            [:div {:style {:margin-top  40
                           :line-height "normal"
                           :font-size   "2em"}}
             "Start immediately deploying apps containers in one button click."]
            [:br]

            [:b {:style {:font-size "1.4em"}} "Start jouney with us"]

            [:br] [:br]
            [ui/Button {:style {:border-radius 0}
                        :size  "large" :inverted true} "Sign up"]
            [:div {:style {:margin-top  20
                           :line-height "normal"}}
             (str "Provide a secured edge to cloud (and back) management platform "
                  "that enabled near-data AI for connected world use cases.")]

            [:div {:style {:position "absolute"
                           :bottom   40}}
             "Follow us on "
             [:span
              [ui/Icon {:name "facebook"}]
              [ui/Icon {:name "twitter"}]
              [ui/Icon {:name "youtube"}]]]

            ]
           ]
          [ui/GridColumn

           [:div {:style {:margin-left "10%"
                          :margin-top  "30%"}}
            [:span {:style {:font-size "1.4em"}} "Login to " [:b "Account"]]
            [ui/Form {:style {:margin-top 30
                              :max-width  "60%"}}
             [ui/FormInput {:label "Username"}]
             [ui/FormInput {:label "Password"}]
             [ui/FormField
              [:a {:href ""} "Forgot your password?"]]
             [ui/Button {:primary true
                         :floated "right"
                         :style {:border-radius 0}} "Sign in"]]

            [:div {:style {:margin-top 70
                           :color      "grey"}} "or use your github account "
             [ui/Button {:style    {:margin-left 10}
                         :circular true
                         :basic    true
                         :class    "icon"}
              [ui/Icon {:name "github"
                        :size "large"}]

              ]
             ]

            ]
           ]
          ]
         #_[:<>
            [sidebar/menu]
            [:div {:style {:transition  "0.5s"
                           :margin-left (if (and (not @is-small-device?) @show?)
                                          sidebar/sidebar-width "0")}}
             [ui/Dimmer {:active   (and @is-small-device? @show?)
                         :inverted true
                         :style    {:z-index 999}
                         :on-click #(dispatch [::events/close-sidebar])}]
             [header]
             [contents]
             [ignore-changes-modal]
             (when-not @iframe? [footer])]]]
        [ui/Container
         [ui/Loader {:active true :size "massive"}]]))))
