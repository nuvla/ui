(ns sixsq.nuvla.ui.main.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.about.views]
    [sixsq.nuvla.ui.apps-component.views]
    [sixsq.nuvla.ui.apps-project.views]
    [sixsq.nuvla.ui.apps-store.views]
    [sixsq.nuvla.ui.apps.views]
    [sixsq.nuvla.ui.authn.views :as authn-views]
    [sixsq.nuvla.ui.cimi.subs :as api-subs]
    [sixsq.nuvla.ui.cimi.views]
    [sixsq.nuvla.ui.data.views]
    [sixsq.nuvla.ui.deployment.views]
    [sixsq.nuvla.ui.docs.views]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.history.utils :as history-utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.i18n.views :as i18n-views]
    [sixsq.nuvla.ui.infra-service.views]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.main.views-sidebar :as sidebar]
    [sixsq.nuvla.ui.messages.views :as messages]
    [sixsq.nuvla.ui.nuvlabox.views]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.profile.views]
    [sixsq.nuvla.ui.utils.general :as utils]
    [sixsq.nuvla.ui.utils.responsive :as responsive]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.welcome.views]))


(defn crumb
  [index segment]
  (let [nav-path (subscribe [::main-subs/nav-path])
        callback-fn #(dispatch [::history-events/navigate (history-utils/trim-path @nav-path index)])]
    ^{:key (str index "_" segment)}
    [ui/BreadcrumbSection
     [:a {:on-click callback-fn
          :style    {:cursor "pointer"}}
      (utils/truncate (str segment))]]))


(defn breadcrumbs-links []
  (let [nav-path (subscribe [::main-subs/nav-path])]
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
  (let [path (subscribe [::main-subs/nav-path])
        options (map breadcrumb-option (range) @path)
        selected (-> options last :value)
        callback-fn #(dispatch [::history-events/navigate (history-utils/trim-path @path %)])]
    [ui/Dropdown
     {:inline    true
      :value     selected
      :on-change (ui-callback/value callback-fn)
      :options   options}]))


(defn breadcrumbs []
  (let [device (subscribe [::main-subs/device])]
    (if (#{:mobile} @device)
      [breadcrumbs-dropdown]
      [breadcrumbs-links])))


(defn footer
  []
  [:footer.nuvla-ui-footer
   [:div.nuvla-ui-footer-left
    [:span "© 2019, SixSq Sàrl"]]
   [:div.nuvla-ui-footer-centre
    [:a {:on-click #(dispatch
                      [::history-events/navigate "about"])
         :style    {:cursor "pointer"}}
     [:span#release-version (str "v")]]]
   [:div.nuvla-ui-footer-right
    [i18n-views/locale-dropdown]]])


(defn ignore-changes-modal
  []
  (let [tr (subscribe [::i18n-subs/tr])
        navigation-info (subscribe [::main-subs/ignore-changes-modal])
        ignore-changes-fn #(dispatch [::main-events/ignore-changes false])]

    [ui/Modal {:open       (some? @navigation-info)
               :close-icon true
               :on-close   ignore-changes-fn}

     [ui/ModalHeader (str/capitalize (str (@tr [:ignore-changes?])))]

     [ui/ModalContent {:content (@tr [:ignore-changes-content])}]

     [ui/ModalActions
      [uix/Button {:text     (@tr [:ignore-changes]),
                   :positive true
                   :active   true
                   :on-click #(dispatch [::main-events/ignore-changes true])}]]]))


(defn contents
  []
  (let [resource-path (subscribe [::main-subs/nav-path])]
    (fn []
      [ui/Container {:as         "main"
                     :class-name "nuvla-ui-content"
                     :fluid      true}
       (panel/render @resource-path)])))


(defn header
  []
  [:header
   [ui/Menu {:className  "nuvla-ui-header"
             :borderless true}

    [ui/MenuItem {:aria-label "toggle sidebar"
                  :link       true
                  :on-click   #(dispatch [::main-events/toggle-sidebar])}
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
    (let [show? (subscribe [::main-subs/sidebar-open?])
          cep (subscribe [::api-subs/cloud-entry-point])
          iframe? (subscribe [::main-subs/iframe?])]

      (if @cep
        [ui/Responsive {:as            "div"
                        :fire-on-mount true
                        :on-update     (responsive/callback #(dispatch [::main-events/set-device %]))}
         [ui/SidebarPushable {:as    ui/SegmentRaw
                              :basic true}
          [sidebar/menu]
          [ui/SidebarPusher
           [ui/Container (cond-> {:id "nuvla-ui-main" :fluid true}
                                 @show? (assoc :className "sidebar-visible"))
            [header]
            [contents]
            [ignore-changes-modal]
            (when-not @iframe? [footer])]]]]
        [ui/Container [ui/Loader {:active true :size "massive"}]]))))
