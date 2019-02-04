(ns sixsq.nuvla.webui.main.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.webui.about.views]
    [sixsq.nuvla.webui.application.views]
    [sixsq.nuvla.webui.appstore.views]
    [sixsq.nuvla.webui.authn.views :as authn-views]
    [sixsq.nuvla.webui.cimi.subs :as api-subs]
    [sixsq.nuvla.webui.cimi.views]
    [sixsq.nuvla.webui.dashboard.views]
    [sixsq.nuvla.webui.data.views]
    [sixsq.nuvla.webui.deployment.views]
    [sixsq.nuvla.webui.docs.views]
    [sixsq.nuvla.webui.history.events :as history-events]
    [sixsq.nuvla.webui.i18n.views :as i18n-views]
    [sixsq.nuvla.webui.legacy-application.views]
    [sixsq.nuvla.webui.main.events :as main-events]
    [sixsq.nuvla.webui.main.subs :as main-subs]
    [sixsq.nuvla.webui.main.views-sidebar :as sidebar]
    [sixsq.nuvla.webui.messages.views :as messages]
    [sixsq.nuvla.webui.metrics.views]
    [sixsq.nuvla.webui.nuvlabox.views]
    [sixsq.nuvla.webui.panel :as panel]
    [sixsq.nuvla.webui.profile.views]
    [sixsq.nuvla.webui.quota.views]
    [sixsq.nuvla.webui.usage.views]
    [sixsq.nuvla.webui.utils.general :as utils]
    [sixsq.nuvla.webui.utils.responsive :as responsive]
    [sixsq.nuvla.webui.utils.semantic-ui :as ui]
    [sixsq.nuvla.webui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.webui.welcome.views]))


(defn crumb
  [index segment]
  (let [nav-fn (fn [& _] (dispatch [::main-events/trim-breadcrumb index]))]
    ^{:key (str index "_" segment)} [ui/BreadcrumbSection [:a {:on-click nav-fn :style {:cursor "pointer"}}
                                           (utils/truncate (str segment))]]))


(defn breadcrumbs-links []
  (let [path (subscribe [::main-subs/nav-path])]
    (vec (concat [ui/Breadcrumb {:size :large}]
                 (->> @path
                      (map crumb (range))
                      (interpose [ui/BreadcrumbDivider {:icon "chevron right"}]))))))


(defn breadcrumb-option
  [index segment]
  {:key   segment
   :value index
   :text  (utils/truncate segment 8)})


(defn breadcrumbs-dropdown []
  (let [path (subscribe [::main-subs/nav-path])]
    (let [options (map breadcrumb-option (range) @path)
          selected (-> options last :value)]
      [ui/Dropdown
       {:inline    true
        :value     selected
        :on-change (ui-callback/value #(dispatch [::main-events/trim-breadcrumb %]))
        :options   options}])))


(defn breadcrumbs []
  (let [device (subscribe [::main-subs/device])]
    (if (#{:mobile} @device)
      [breadcrumbs-dropdown]
      [breadcrumbs-links])))


(defn footer
  []
  [:footer.webui-footer
   [:div.webui-footer-left
    [:span "© 2019, SixSq Sàrl"]]
   [:div.webui-footer-centre
    [:a {:on-click #(dispatch
                      [::history-events/navigate "about"])
         :style    {:cursor "pointer"}}
     [:span#release-version (str "v")]]]
   [:div.webui-footer-right
    [i18n-views/locale-dropdown]]])


(defn contents
  []
  (let [resource-path (subscribe [::main-subs/nav-path])]
    (fn []
      [ui/Container {:as         "main"
                     :class-name "webui-content"
                     :fluid      true}
       (panel/render @resource-path)])))


(defn header
  []
  [:header
   [ui/Menu {:className  "webui-header"
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
             [ui/Container (cond-> {:id "webui-main" :fluid true}
                                   @show? (assoc :className "sidebar-visible"))
              [header]
              [contents]
              (when-not @iframe? [footer])]]]]
          [ui/Container [ui/Loader {:active true :size "massive"}]]))))
