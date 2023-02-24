(ns sixsq.nuvla.ui.main.views
  (:require [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.apps.events :as apps-events]
            [sixsq.nuvla.ui.cimi.subs :as api-subs]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.i18n.views :as i18n-views]
            [sixsq.nuvla.ui.intercom.views :as intercom]
            [sixsq.nuvla.ui.main.components :as main-components]
            [sixsq.nuvla.ui.main.events :as events]
            [sixsq.nuvla.ui.main.subs :as subs]
            [sixsq.nuvla.ui.main.views-sidebar :as sidebar]
            [sixsq.nuvla.ui.messages.views :as messages]
            [sixsq.nuvla.ui.profile.subs :as profile-subs]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.router :refer [router-component]]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.subs :as route-subs]
            [sixsq.nuvla.ui.routing.utils :refer [name->href trim-path]]
            [sixsq.nuvla.ui.session.views :as session-views]
            [sixsq.nuvla.ui.utils.general :as utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn crumb
  [index segment]
  (let [nav-path  (subscribe [::route-subs/nav-path])
        click-fn  #(dispatch [::routing-events/navigate (trim-path @nav-path index)])
        page-icon (:icon-class segment)]
    ^{:key (str index "_" segment)}
    [ui/BreadcrumbSection
     [:a {:on-click click-fn
          :style    {:cursor "pointer"}
          :class    (when (zero? index) :parent)}
      (when page-icon [uix/Icon {:name page-icon :style {:padding-right "10px"
                                                         :font-weight   400}}])
      (utils/truncate (str (or (:text segment) segment)))]]))

(defn- format-path-segment [tr first-segment]
  (utils/capitalize-first-letter (@tr [(keyword first-segment)])))


(defn format-first-crumb
  [nav-path]
  (let [tr            (subscribe [::i18n-subs/tr])
        first-segment (first nav-path)
        page-info     (subscribe [::subs/page-info first-segment])]
    {:text       (if (seq first-segment)
                   (format-path-segment tr first-segment)
                   (format-path-segment tr "welcome"))
     :icon-class (:icon @page-info)}))


(defn decorate-breadcrumbs
  [nav-path]
  (conj (rest nav-path) (format-first-crumb nav-path)))


(defn breadcrumbs-links []
  (let [nav-path           (subscribe [::route-subs/nav-path])
        decorated-nav-path (decorate-breadcrumbs @nav-path)]
    (vec (concat [ui/Breadcrumb {:id   "nuvla-ui-header-breadcrumb"
                                 :size :large}]
                 (->> decorated-nav-path
                      (map crumb (range))
                      (interpose [ui/BreadcrumbDivider {:icon "chevron right"}]))))))


(defn breadcrumb-option
  [index segment]
  {:key   segment
   :value index
   :text  (utils/truncate segment 8)})


(defn breadcrumbs-dropdown []
  (let [path        (subscribe [::route-subs/nav-path])
        options     (map breadcrumb-option (range) @path)
        selected    (-> options last :value)
        callback-fn #(dispatch [::routing-events/navigate (trim-path @path %)])]
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
        current-year (.getFullYear (time/now))]
    [ui/Segment {:class "footer" :style {:border-radius 0
                                         :z-index       10}}
     [ui/Grid {:columns 3}
      [ui/GridColumn grid-style (str "© " current-year ", SixSq SA")]
      [ui/GridColumn grid-style
       [:a {:on-click #(dispatch [::routing-events/navigate routes/about])
            :style    {:cursor "pointer"}}
        [:span#release-version (str "v")]]]
      [ui/GridColumn grid-style
       [i18n-views/LocaleDropdown]]]]))


(defn ignore-changes-modal
  []
  (let [tr                       (subscribe [::i18n-subs/tr])
        navigation-info          (subscribe [::subs/ignore-changes-modal])
        do-not-ignore-changes-fn #(dispatch [::events/do-not-ignore-changes])]
    (when @navigation-info
      (dispatch [::events/opening-protection-modal]))

    [ui/Modal {:open        (some? @navigation-info)
               :close-icon  true
               :on-close    do-not-ignore-changes-fn
                ;; data-testid is used for e2e test
               :data-testid "protection-modal"}

     [uix/ModalHeader {:header (@tr [:ignore-changes?])}]

     [ui/ModalContent {:content (@tr [:ignore-changes-content])}]

     [ui/ModalActions
      [uix/Button {:text     (@tr [:ignore-changes]),
                   :positive true
                   :active   true
                   :on-click #(do (dispatch [::events/ignore-changes])
                                  (dispatch [::apps-events/form-valid]))}]]]))

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
                                (dispatch [::routing-events/navigate routes/profile])
                                (dispatch [::events/close-modal]))}
        (if @open-subs-unpaid?
          (@tr [:profile-page])
          (@tr [:subscribe]))]
       [:p]
       (when @open-subs-required?
         [:p [ui/Icon {:name "info circle"}] (@tr [:subscription-required-content-group])])]]]))


(defn contents
  []
  (let [content-key      (subscribe [::subs/content-key])
        is-small-device? (subscribe [::subs/is-small-device?])]
    (fn []
      [ui/Container
       (cond-> {:as    "main"
                :key   @content-key
                :id    "nuvla-ui-content"
                :fluid true}
               @is-small-device? (assoc :on-click #(dispatch [::events/close-sidebar])))

       [router-component]])))


(defn UpdateUIVersion
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        open?       (subscribe [::subs/ui-version-modal-open?])
        new-version (subscribe [::subs/ui-version-new-version])
        open-modal  #(dispatch [::events/new-version-open-modal? true])
        close-modal #(dispatch [::events/new-version-open-modal? false])
        reload      #(.reload js/location true)]
    [:<>
     [ui/Modal {:open  @open?
                :size  :small
                :basic true}
      [uix/ModalHeader {:icon   "refresh"
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
   [ui/Menu {:class      "nuvla-ui-header"
             :borderless true}

    [ui/MenuItem {:aria-label "toggle sidebar"
                  :link       true
                  :on-click   #(dispatch [::events/toggle-sidebar])}
     [uix/Icon {:name "fa-light fa-bars"}]]

    [ui/MenuItem [breadcrumbs]]


    [ui/MenuMenu {:position "right"}
     [UpdateUIVersion]
     [messages/bell-menu]
     [session-views/AuthnMenu]]]

   [messages/alert-slider]
   [messages/alert-modal]])

(defn AppLoader
  []
  (let [tr     (subscribe [::i18n-subs/tr])
        error? (subscribe [::api-subs/cloud-entry-point-error?])]
    [ui/Container
     [ui/Loader {:active true :size "massive"}
      (when @error?
        [ui/Header {:text-align :center
                    :as         :h2
                    :content    (@tr [:service-unavailable])
                    :subheader  (@tr [:take-coffee-back-soon])}])]]))

(defn app []
  (let [show?            (subscribe [::subs/sidebar-open?])
        iframe?          (subscribe [::subs/iframe?])
        is-small-device? (subscribe [::subs/is-small-device?])
        resource-path    (subscribe [::route-subs/nav-path])
        app-loading?     (subscribe [::subs/app-loading?])
        subs-canceled?   (subscribe [::profile-subs/subscription-canceled?])]
    (if @app-loading?
      [AppLoader]
      [:div {:id "nuvla-ui-main"}
       (if (#{"sign-in"
              "sign-up"
              "reset-password"
              "set-password"
              "sign-in-token"
              nil} (first @resource-path))
         [router-component]
         [:<>
          [intercom/widget]
          [sidebar/menu]
          [:div {:class (str "nuvla-" (first @resource-path))
                 :style {:transition  "0.5s"
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
                                      [:a {:href (name->href routes/profile)} [uix/TR :go-to-profile]]
                                      [uix/TR :make-sure-you-have-pm]]
                            :type    :error}])
            [contents]
            [ignore-changes-modal]
            [subscription-required-modal]
            (when-not @iframe? [footer])]]])])))
