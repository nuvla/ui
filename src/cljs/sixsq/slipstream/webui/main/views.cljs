(ns sixsq.slipstream.webui.main.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.slipstream.webui.about.views]
    [sixsq.slipstream.webui.application.views]
    [sixsq.slipstream.webui.appstore.views]
    [sixsq.slipstream.webui.authn.views :as authn-views]
    [sixsq.slipstream.webui.cimi.subs :as api-subs]
    [sixsq.slipstream.webui.cimi.views]
    [sixsq.slipstream.webui.dashboard.views]
    [sixsq.slipstream.webui.data.views]
    [sixsq.slipstream.webui.deployment.views]
    [sixsq.slipstream.webui.docs.views]
    [sixsq.slipstream.webui.history.events :as history-events]
    [sixsq.slipstream.webui.i18n.views :as i18n-views]
    [sixsq.slipstream.webui.legacy-application.views]
    [sixsq.slipstream.webui.main.events :as main-events]
    [sixsq.slipstream.webui.main.subs :as main-subs]
    [sixsq.slipstream.webui.main.views-sidebar :as sidebar]
    [sixsq.slipstream.webui.messages.views :as messages]
    [sixsq.slipstream.webui.metrics.views]
    [sixsq.slipstream.webui.nuvlabox.views]
    [sixsq.slipstream.webui.panel :as panel]
    [sixsq.slipstream.webui.profile.views]
    [sixsq.slipstream.webui.quota.views]
    [sixsq.slipstream.webui.usage.views]
    [sixsq.slipstream.webui.utils.general :as utils]
    [sixsq.slipstream.webui.utils.responsive :as responsive]
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]
    [sixsq.slipstream.webui.utils.ui-callback :as ui-callback]
    [sixsq.slipstream.webui.welcome.views]))


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
