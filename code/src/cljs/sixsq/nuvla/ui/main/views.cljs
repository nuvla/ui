(ns sixsq.nuvla.ui.main.views
  (:require [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.i18n.views :as i18n-views]
            [sixsq.nuvla.ui.common-components.messages.views :as messages]
            [sixsq.nuvla.ui.main.components :as main-components]
            [sixsq.nuvla.ui.main.events :as events]
            [sixsq.nuvla.ui.main.subs :as subs]
            [sixsq.nuvla.ui.main.views-sidebar :as sidebar]
            [sixsq.nuvla.ui.pages.apps.apps-applications-sets.views]
            [sixsq.nuvla.ui.pages.apps.events :as apps-events]
            [sixsq.nuvla.ui.pages.profile.subs :as profile-subs]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.subs :as route-subs]
            [sixsq.nuvla.ui.routing.utils :refer [name->href trim-path]]
            [sixsq.nuvla.ui.session.views :as session-views]
            [sixsq.nuvla.ui.utils.general :as utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(defn crumb
  [index {:keys [text icon-class] :as segment}]
  (let [nav-path   (subscribe [::route-subs/nav-path])
        click-fn   #(dispatch [::routing-events/navigate (trim-path @nav-path index)])
        page-title (utils/truncate (str (or text segment)))]
    ^{:key (str index "_" segment)}
    [ui/BreadcrumbSection
     [:a {:on-click click-fn
          :style    {:cursor "pointer"}
          :class    (when (zero? index) :parent)}
      (when icon-class
        [icons/Icon {:name  icon-class
                     :style {:padding-right "10px"
                             :font-weight   (when-not
                                              (= icon-class "fal fa-rocket-launch")
                                              400)}}])
      page-title]]))

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
     :icon-class (or (:icon @page-info)
                     "fa-light fa-house")}))

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

(defn Breadcrumbs []
  (let [is-mobile? (subscribe [::subs/is-mobile-device?])]
    (if @is-mobile?
      [breadcrumbs-dropdown]
      [breadcrumbs-links])))

(defn Footer []
  (when-not @(subscribe [::subs/iframe?])
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
          [main-components/SpanVersion]]]
        [ui/GridColumn grid-style
         [i18n-views/LocaleDropdown]]]])))

(defn IgnoreChangesModal []
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

(defn SubscriptionRequiredModal []
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
         [:p [icons/InfoIconFull] (@tr [:subscription-required-content-group])])]]]))

(defn Contents [View]
  (let [content-key   @(subscribe [::subs/content-key])
        small-device? @(subscribe [::subs/is-small-device?])
        on-click      (when small-device? #(dispatch [::events/close-sidebar]))]
    [ui/Container
     {:as       "main"
      :key      content-key
      :id       "nuvla-ui-content"
      :fluid    true
      :on-click on-click}
     View]))

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

(defn Header []
  [:header
   [ui/Menu {:class      "nuvla-ui-header"
             :borderless true}
    [ui/MenuItem {:aria-label "toggle sidebar"
                  :link       true
                  :on-click   #(dispatch [::events/toggle-sidebar])}
     [icons/BarsIcon]]
    [ui/MenuItem [Breadcrumbs]]
    [ui/MenuMenu {:position "right"}
     [UpdateUIVersion]
     [messages/bell-menu]
     [session-views/AuthnMenu]]]
   [messages/alert-slider]
   [messages/alert-modal]])

(defn MessageSubscriptionCanceled []
  (let [subs-canceled? @(subscribe [::profile-subs/subscription-canceled?])]
    (when subs-canceled?
      [uix/MsgError {:header  [uix/TR :subscription-is-canceled]
                     :content [:span
                               [uix/TR :to-reactivate-your-subscription]
                               [:a {:href (name->href routes/profile)} [uix/TR :go-to-profile]]
                               [uix/TR :make-sure-you-have-pm]]}])))

(defn MainDiv [children]
  (let [show?            @(subscribe [::subs/sidebar-open?])
        is-small-device? @(subscribe [::subs/is-small-device?])
        nav-path-first   @(subscribe [::route-subs/nav-path-first])
        class            (str "nuvla-" (case nav-path-first
                                         ("deployment-set" "deployment-groups")
                                         "deployments"

                                         nav-path-first))
        style            {:transition  "0.5s"
                          :margin-left (if (and (not is-small-device?) show?)
                                         sidebar/sidebar-width "0")}]
    [:div {:class class :style style}
     children]))
