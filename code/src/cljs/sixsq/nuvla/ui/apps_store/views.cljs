(ns sixsq.nuvla.ui.apps-store.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch dispatch-sync subscribe]]
    [reagent.core :as r]
    [markdown-to-hiccup.core :as md]
    [sixsq.nuvla.ui.apps-project.views :as apps-project-views]
    [sixsq.nuvla.ui.apps-store.events :as events]
    [sixsq.nuvla.ui.apps-store.subs :as subs]
    [sixsq.nuvla.ui.apps.events :as apps-events]
    [sixsq.nuvla.ui.apps.subs :as apps-subs]
    [sixsq.nuvla.ui.apps.utils :as apps-utils]
    [sixsq.nuvla.ui.apps.views-detail :as apps-views-detail]
    [sixsq.nuvla.ui.deployment-dialog.events :as deployment-dialog-events]
    [sixsq.nuvla.ui.deployment-dialog.views :as deployment-dialog-views]
    [sixsq.nuvla.ui.deployment.views :as deployment-views]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.session.subs :as session-subs]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as utils-style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.general :as utils-general]
    [sixsq.nuvla.ui.utils.values :as utils-values]
    [sixsq.nuvla.ui.apps-store.utils :as utils]
    [sixsq.nuvla.ui.dashboard.utils :as dashboard-utils]
    [taoensso.timbre :as log]))


(defn RefreshMenu
  []
  [main-components/RefreshMenu
   {:on-refresh #(do (dispatch [::events/get-modules]))}])


(defn RefreshMyAppsMenu
  []
  (let [owner (subscribe [::session-subs/active-claim])]
    [main-components/RefreshMenu
     {:on-refresh #(dispatch [::events/get-my-modules owner])}]))


(defn ModuleCard
  [{:keys [id name description path subtype logo-url price published versions] :as module}]
  (let [tr           (subscribe [::i18n-subs/tr])
        map-versions (apps-utils/map-versions-index versions)
        module-id    (if (true? published) (apps-utils/latest-published-module-with-index id map-versions) id)
        module-index (apps-utils/latest-published-index map-versions)
        detail-href  (str "apps/" path (when (true? published) (str "?version=" module-index)))
        button-ops   {:fluid    true
                      :primary  true
                      :icon     :rocket
                      :content  (@tr [:launch])
                      :on-click (fn [event]
                                  (dispatch [::main-events/subscription-required-dispatch
                                             [::deployment-dialog-events/create-deployment
                                              module-id :infra-services]])
                                  (.preventDefault event)
                                  (.stopPropagation event))}
        desc-summary (utils-values/markdown->summary description)]
    [uix/Card
     {:image       logo-url
      :header      [:<>
                    [ui/Icon {:name (apps-utils/subtype-icon subtype)}]
                    (or name id)]
      :description (utils-general/truncate desc-summary 180)
      :href        detail-href
      :on-click    #(dispatch [::history-events/navigate detail-href])
      :button      [ui/Button
                    (cond-> button-ops
                            price (assoc :icon :cart
                                         :content (str (@tr [:launch-for])
                                                       (/ (:cent-amount-daily price) 100) "€/"
                                                       (@tr [:day]))))]}]))


(defn ModulesCardsGroup
  [modules-list]
  [:div utils-style/center-items
   [ui/CardGroup {:centered    true
                  :itemsPerRow 4
                  :stackable   true}
    (for [{:keys [id] :as module} modules-list]
      ^{:key id}
      [ModuleCard module])]])


(defn AppStoreControlBar
  []
  (let [full-text (subscribe [::subs/full-text-search-published])]
    [ui/Menu {:secondary true}
     [ui/MenuMenu {:position "left"}
      [main-components/SearchInput
       {:on-change     (ui-callback/input-callback #(dispatch [::events/set-full-text-search-published %]))
        :default-value @full-text}]]
     [RefreshMenu]]))


(defn AllAppsControlBar
  []
  (let [full-text (subscribe [::subs/full-text-search-all-apps])]
    [ui/Menu {:secondary true}
     [ui/MenuMenu {:position "left"}
      [main-components/SearchInput
       {:on-change     (ui-callback/input-callback #(dispatch [::events/set-full-text-search-all-apps %]))
        :default-value @full-text}]]
     [RefreshMenu]]))


(defn MyAppsControlBar
  []
  (let [full-text (subscribe [::subs/full-text-search-my])]
    [ui/Menu {:secondary true}
     [ui/MenuMenu {:position "left"}
      [main-components/SearchInput
       {:on-change     (ui-callback/input-callback #(dispatch [::events/set-full-text-search-my %]))
        :default-value @full-text}]]
     [RefreshMyAppsMenu]]))


(defn ControlBarProjects []
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Menu {:borderless true}
     [uix/MenuItem
      {:name     (@tr [:add])
       :icon     "add"
       :on-click #(dispatch [::apps-events/open-add-modal])}]
     [RefreshMenu]]))


(defn TabNavigator []
  (let [tr     (subscribe [::i18n-subs/tr])
        module (subscribe [::apps-subs/module])]
    (dispatch [::apps-events/get-module])
    (fn []
      (let []
        [:<>
         [apps-views-detail/add-modal]
         [apps-views-detail/format-error @module]
         [ui/TabPane
          [ControlBarProjects]
          (when (and @module (not (instance? js/Error @module)))
            (let [{:keys [children]} @module]
              [apps-project-views/format-module-children children]))]]))))


(defn TabAppStore
  []
  (let [modules           (subscribe [::subs/published-modules])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])]
    (dispatch [::events/get-published-modules])
    (fn []
      (let [total-modules (get @modules :count 0)
            total-pages   (general-utils/total-pages total-modules @elements-per-page)]
        [ui/Segment
         [AppStoreControlBar]
         [ModulesCardsGroup (get @modules :resources [])]
         [uix/Pagination
          {:totalitems   total-modules
           :totalPages   total-pages
           :activePage   @page
           :onPageChange (ui-callback/callback
                           :activePage #(dispatch [::events/set-page %]))}]]))))


(defn TabAllApps
  []
  (let [modules           (subscribe [::subs/modules])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])]
    (dispatch [::events/get-modules])
    (fn []
      (let [total-modules (get @modules :count 0)
            total-pages   (general-utils/total-pages total-modules @elements-per-page)]
        [ui/TabPane
         [AllAppsControlBar]
         [ModulesCardsGroup (get @modules :resources [])]
         [uix/Pagination
          {:totalitems   total-modules
           :totalPages   total-pages
           :activePage   @page
           :onPageChange (ui-callback/callback
                           :activePage #(dispatch [::events/set-page %]))}]]))))


(defn TabMyApps
  []
  (let [tr                (subscribe [::i18n-subs/tr])
        modules           (subscribe [::subs/my-modules])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])
        search            (subscribe [::subs/full-text-search-my])
        {:keys [resource tab-index tab-index-event]} dashboard-utils/target-navigator]
    (dispatch [::events/get-my-modules])
    (fn []
      (let [total-modules (get @modules :count 0)
            total-pages   (general-utils/total-pages total-modules @elements-per-page)]
        [ui/TabPane
         (if (or (pos? total-modules) (not (empty? @search)))
           [:<>
            [MyAppsControlBar]
            [ModulesCardsGroup (get @modules :resources [])]
            [uix/Pagination
             {:totalitems   total-modules
              :totalPages   total-pages
              :activePage   @page
              :onPageChange (ui-callback/callback
                              :activePage #(dispatch [::events/set-page %]))}]]
           [:<>
            [uix/WarningMsgNoElements (@tr [:no-apps-available])]
            [ui/Container {:textAlign "center"}
             [ui/Icon {:name     "plus"
                       :size     "huge"
                       :style    {:cursor "pointer"}
                       :on-click #(do
                                    (when (and tab-index tab-index-event)
                                      (dispatch [tab-index-event tab-index]))
                                    (dispatch [::history-events/navigate resource]))}]]])]))))


(defn TabDeployments
  []
  (fn []
    [ui/TabPane
     [deployment-views/deployments-main-content]]))


(defn TabDiscoverSection
  [icon-name section-key modules dispatch-list section-message]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Segment
     [ui/Grid {:columns   "equal"
               :stackable true}
      [ui/GridRow
       [ui/GridColumn {:floated "left"}
        [:div {:style {:padding-bottom 10}}
         [:h2
          [ui/Icon {:className icon-name}]
          (utils-general/capitalize-words (@tr [section-key]))
          [ui/Label {:circular true
                     :size     "mini"}
           (:count modules)]]]]
       [ui/GridColumn
        [ui/Message {:info true} (@tr [section-message])]]
       [ui/GridColumn {:only    "computer tablet"
                       :floated "left"}
        [ui/Button {:floated  "right"
                    :primary  true
                    :on-click #(dispatch dispatch-list)} "See more"]]]
      [ui/GridRow
       [ui/GridColumn
        [ModulesCardsGroup (take 4 (get modules :resources []))]]]
      [ui/GridRow {:only "mobile"}
       [ui/GridColumn {:textAlign "center"}
        [ui/Button {:primary  true
                    :on-click #(dispatch dispatch-list)} "See more"]]]]]))


(defn TabDiscover
  []
  (let [published-modules (subscribe [::subs/published-modules])
        modules           (subscribe [::subs/modules])
        all-my-modules    (subscribe [::subs/my-modules])]
    ;    (dispatch [::events/get-modules ""])
    (dispatch [::events/get-published-modules ""])
    (dispatch [::events/get-my-modules ""])
    (fn []
      (let []
        [:<>
         [TabDiscoverSection "fas fa-store", :appstore, @published-modules,
          [::events/set-active-tab-index utils/tab-app-store], :discover-published-apps-message]
         ;[TabDiscoverSection "fas fa-th", :all-apps, @modules,
         ; [::events/set-active-tab-index utils/tab-all-apps], :discover-all-apps-message]
         (when (pos? (:count @all-my-modules 0))
           [TabDiscoverSection "user" :my-apps, @all-my-modules,
            [::events/set-active-tab-index utils/tab-my-apps], :discover-my-apps-message])]))))


(defn Tabs
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [
     {:menuItem (r/as-element
                  [ui/MenuItem
                   {:key "appstore"}
                   [ui/Icon {:className "fas fa-store"}]
                   (utils-general/capitalize-words (@tr [:appstore]))])
      :render   (fn [] (r/as-element [TabAppStore]))}
     {:menuItem (r/as-element
                  [ui/MenuItem
                   {:key "allapps"}
                   [ui/Icon {:className "grid layout"}]
                   (utils-general/capitalize-words (@tr [:all-apps]))])
      :render   (fn [] (r/as-element [TabAllApps]))}
     {:menuItem (r/as-element
                  [ui/MenuItem
                   {:key "myapps"}
                   [ui/Icon {:className "user"}]
                   (utils-general/capitalize-words (@tr [:my-apps]))])
      :render   (fn [] (r/as-element [TabMyApps]))}
     {:menuItem {:content "Navigate Apps"
                 :key     "navigate"
                 :icon    "folder open"}
      :render   (fn [] (r/as-element [TabNavigator]))}
     {:menuItem {:content "Deployments"
                 :key     "deployments"
                 :icon    "rocket"}
      :render   (fn [] (r/as-element [TabDeployments]))}]))


(defn TabsApps
  []
  (fn []
    (let [active-index (subscribe [::subs/active-tab-index])]
      [ui/Tab
       {:menu        {:secondary true
                      :pointing  true
                      :style     {:display        "flex"
                                  :flex-direction "row"
                                  :flex-wrap      "wrap"}}
        :panes       (Tabs)
        :activeIndex @active-index
        :onTabChange (fn [_ data]
                       (let [active-index (. data -activeIndex)]
                         (dispatch [::events/set-active-tab-index active-index])))}])))


(defn RootView
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    (dispatch [::apps-events/reset-version])
    (fn []
      [ui/Container {:fluid true}
       [:<>
        [uix/PageHeader "fas fa-store" (general-utils/capitalize-first-letter (@tr [:apps]))]
        [TabsApps]]])))
