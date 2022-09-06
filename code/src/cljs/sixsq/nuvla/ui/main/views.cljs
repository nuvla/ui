(ns sixsq.nuvla.ui.main.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.about.views]
    [sixsq.nuvla.ui.apps-application.views]
    [sixsq.nuvla.ui.apps-component.views]
    [sixsq.nuvla.ui.apps-project.views]
    [sixsq.nuvla.ui.apps-store.views]
    [sixsq.nuvla.ui.apps.events :as apps-events]
    [sixsq.nuvla.ui.apps.views]
    [sixsq.nuvla.ui.cimi.subs :as api-subs]
    [sixsq.nuvla.ui.cimi.views]
    [sixsq.nuvla.ui.clouds-detail.views]
    [sixsq.nuvla.ui.clouds.views]
    [sixsq.nuvla.ui.credentials.views]
    [sixsq.nuvla.ui.dashboard.views]
    [sixsq.nuvla.ui.data.views]
    [sixsq.nuvla.ui.deployment-sets-detail.views]
    [sixsq.nuvla.ui.deployment-sets.views]
    [sixsq.nuvla.ui.deployments.views]
    [sixsq.nuvla.ui.docs.views]
    [sixsq.nuvla.ui.edges.views]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.history.utils :as history-utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.i18n.views :as i18n-views]
    [sixsq.nuvla.ui.intercom.views :as intercom]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.main.events :as events]
    [sixsq.nuvla.ui.main.subs :as subs]
    [sixsq.nuvla.ui.main.views-sidebar :as sidebar]
    [sixsq.nuvla.ui.messages.views :as messages]
    [sixsq.nuvla.ui.notifications.views]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.profile.subs :as profile-subs]
    [sixsq.nuvla.ui.profile.views]
    [sixsq.nuvla.ui.session.subs :as session-subs]
    [sixsq.nuvla.ui.session.views :as session-views]
    [sixsq.nuvla.ui.utils.general :as utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.welcome.views]))


(defn crumb
  [index segment]
  (let [nav-path (subscribe [::subs/nav-path])
        click-fn #(dispatch [::history-events/navigate (history-utils/trim-path @nav-path index)])]
    ^{:key (str index "_" segment)}
    [ui/BreadcrumbSection
     [:a {:on-click click-fn
          :style    {:cursor "pointer"}}
      (utils/truncate (str segment))]]))


(defn format-first-crumb
  [nav-path]
  (let [tr (subscribe [::i18n-subs/tr])]
    (utils/capitalize-first-letter (@tr [(keyword (first nav-path))]))))


(defn decorate-breadcrumbs
  [nav-path]
  (conj (rest nav-path) (format-first-crumb nav-path)))


(defn breadcrumbs-links []
  (let [nav-path           (subscribe [::subs/nav-path])
        decorated-nav-path (decorate-breadcrumbs @nav-path)]
    (vec (concat [ui/Breadcrumb {:size :large}]
                 (->> decorated-nav-path
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
  (let [is-mobile? (subscribe [::subs/is-mobile-device?])]
    (if @is-mobile?
      [breadcrumbs-dropdown]
      [breadcrumbs-links])))


(defn footer
  []
  (let [grid-style   {:style {:padding-top    5
                              :padding-bottom 5
                              :text-align     "center"}}
        current-year (.year (time/now))]
    [ui/Segment {:style {:border-radius 0}}
     [ui/Grid {:columns 3}
      [ui/GridColumn grid-style (str "© " current-year ", SixSq SA")]
      [ui/GridColumn grid-style
       [:a {:on-click #(dispatch [::history-events/navigate "about"])
            :style    {:cursor "pointer"}}
        [:span#release-version (str "v")]]]
      [ui/GridColumn grid-style
       [i18n-views/LocaleDropdown]]]]))


(defn ignore-changes-modal
  []
  (let [tr                (subscribe [::i18n-subs/tr])
        navigation-info   (subscribe [::subs/ignore-changes-modal])
        ignore-changes-fn #(dispatch [::events/ignore-changes false])]

    [ui/Modal {:open       (some? @navigation-info)
               :close-icon true
               :on-close   ignore-changes-fn}

     [uix/ModalHeader {:header (@tr [:ignore-changes?])}]

     [ui/ModalContent {:content (@tr [:ignore-changes-content])}]

     [ui/ModalActions
      [uix/Button {:text     (@tr [:ignore-changes]),
                   :positive true
                   :active   true
                   :on-click #(do (dispatch [::events/ignore-changes true])
                                  (dispatch [::apps-events/form-valid])
                                  (dispatch [::apps-events/set-active-tab :overview]))}]]]))

(defn subscription-required-modal
  []
  (let [tr                  (subscribe [::i18n-subs/tr])
        open-subs-required? (subscribe [::subs/modal-open? :subscription-required])
        open-subs-unpaid?   (subscribe [::subs/modal-open? :subscription-unpaid])]

    [ui/Modal {:open       (or @open-subs-required?
                               @open-subs-unpaid?)
               :close-icon true
               :size       "small"
               :on-close   #(dispatch [::events/close-modal])}

     [uix/ModalHeader {:header (@tr [:subscription-required])}]

     [ui/ModalContent
      [:div
       [:p (if @open-subs-unpaid?
             (@tr [:subscription-unpaid-content])
             (@tr [:subscription-required-content]))]
       [ui/Button {:primary  true
                   :on-click #(do
                                (dispatch [::history-events/navigate "profile"])
                                (dispatch [::events/close-modal]))}
        (if @open-subs-unpaid?
          (@tr [:profile-page])
          (@tr [:subscribe]))]
       [:p]
       (when @open-subs-required?
         [:p [ui/Icon {:name "info circle"}] (@tr [:subscription-required-content-group])])]]]))


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

       (panel/render @resource-path)])))


(defn UpdateUIVersion
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        open?       (subscribe [::subs/ui-version-modal-open?])
        new-version (subscribe [::subs/ui-version-new-version])
        open-modal  #(dispatch [::events/new-version-open-modal? true])
        close-modal #(dispatch [::events/new-version-open-modal? false])
        reload      #(.reload js/location true)]
    [:<>
     [ui/Modal {:open @open?
                :size :small
                :basic true}
      [uix/ModalHeader {:icon "refresh"
                        :header (@tr [:new-ui-version])}]
      [ui/ModalContent (@tr [:new-ui-version-content])]
      [ui/ModalActions
       [ui/Button {:negative true
                   :on-click close-modal}
        (@tr [:will-do-it-later])]
       [ui/Button {:positive true
                   :on-click reload}
        (@tr [:update-and-reload])]]]
     (when @new-version
       [ui/MenuItem {:on-click open-modal}
        [ui/Label {:color    "red"
                   :icon     "refresh"
                   :circular true
                   :content  (@tr [:new-ui-version])}]])]))


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
     [UpdateUIVersion]
     [messages/bell-menu]
     [session-views/AuthnMenu]]]

   [messages/alert-slider]
   [messages/alert-modal]])


(defn app []
  (let [show?            (subscribe [::subs/sidebar-open?])
        cep              (subscribe [::api-subs/cloud-entry-point])
        iframe?          (subscribe [::subs/iframe?])
        is-small-device? (subscribe [::subs/is-small-device?])
        resource-path    (subscribe [::subs/nav-path])
        session-loading? (subscribe [::session-subs/session-loading?])
        subs-canceled?   (subscribe [::profile-subs/subscription-canceled?])]
    (if (and @cep (not @session-loading?))
      [:div {:id "nuvla-ui-main"}
       (case (first @resource-path)
         "sign-in" [session-views/SessionPage true]
         "sign-up" [session-views/SessionPage true]
         "reset-password" [session-views/SessionPage true]
         "set-password" [session-views/SessionPage false]
         "sign-in-token" [session-views/SessionPage true]
         nil [session-views/SessionPage true]
         [:<>
          [intercom/widget]
          [sidebar/menu]
          [:div {:style {:transition  "0.5s"
                         :margin-left (if (and (not @is-small-device?) @show?)
                                        sidebar/sidebar-width "0")}}
           [ui/Dimmer {:active   (and @is-small-device? @show?)
                       :inverted true
                       :style    {:z-index 999}
                       :on-click #(dispatch [::events/close-sidebar])}]
           [header]
           [:div {:ref main-components/ref}
            (when @subs-canceled?
              [uix/Message {:icon    "warning"
                            :header  [uix/TR :subscription-is-canceled]
                            :content [:span
                                      [uix/TR :to-reactivate-your-subscription]
                                      [:a {:href "profile"} [uix/TR :go-to-profile]]
                                      [uix/TR :make-sure-you-have-pm]]
                            :type    :error}])
            [contents]
            [ignore-changes-modal]
            [subscription-required-modal]
            (when-not @iframe? [footer])]]])]
      [ui/Container
       [ui/Loader {:active true :size "massive"}]])))
