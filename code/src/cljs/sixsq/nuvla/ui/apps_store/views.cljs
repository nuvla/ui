(ns sixsq.nuvla.ui.apps-store.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch dispatch-sync subscribe]]
    [reagent.core :as r]
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
    [clojure.string :as str]
    [sixsq.nuvla.ui.utils.general :as utils-general]
    [sixsq.nuvla.ui.apps-store.utils :as utils]))


(defn RefreshMenu
  []
  [main-components/RefreshMenu
   {:on-refresh #(do (dispatch [::events/get-modules]))}])


(defn RefreshMyAppsMenu
  []
  (let [owner (subscribe [::session-subs/user-id])]
    [main-components/RefreshMenu
     {:on-refresh #(dispatch [::events/get-my-modules owner])}]))


(defn ModuleCard
  [{:keys [id name description path subtype compatibility logo-url price] :as module}]
  (let [tr          (subscribe [::i18n-subs/tr])
        detail-href (str "apps/" path)
        button-ops  {:fluid    true
                     :primary  true
                     :icon     :rocket
                     :content  (@tr [:launch])
                     :on-click (fn [event]
                                 (dispatch [::main-events/subscription-required-dispatch
                                            [::deployment-dialog-events/create-deployment
                                             (:id module) :infra-services]])
                                 (.preventDefault event)
                                 (.stopPropagation event))}]
    [uix/Card
     {:image       logo-url
      :header      [:<>
                    [ui/Icon {:name (apps-utils/subtype-icon subtype)}]
                    (or name id)]
      :description description
      :href        detail-href
      :on-click    #(dispatch [::history-events/navigate detail-href])
      :content     (when compatibility
                     [ui/Label {:color "grey", :corner "right"}
                      [ui/Popup
                       {:position "top center"
                        :content  (str "COMPATIBILITY: " compatibility)
                        :size     "small"
                        :trigger  (r/as-element [ui/Icon {:name "info"}])}]])
      :button      [ui/Button
                    (cond-> button-ops
                            price (assoc :icon :cart
                                         :content (str (@tr [:launch-for])
                                                       (/ (:cent-amount-daily price) 100) "€/"
                                                       (@tr [:day]))))]}]))


(defn ModulesCardsGroup
  [modules-list]
  ;  style={{overflow: 'auto', maxHeight: 200 }}
  [:div utils-style/center-items
   [ui/CardGroup {:centered    true
                  :itemsPerRow 4
                  :stackable   true}
    (for [{:keys [id] :as module} modules-list]
      ^{:key id}
      [ModuleCard module])]])


(defn AppStoreControlBar
  []
  (let [full-text (subscribe [::subs/full-text-search])]
    [ui/Menu {:secondary true}
     [ui/MenuMenu {:position "left"}
      [main-components/SearchInput
       {:on-change     (ui-callback/input-callback #(dispatch [::events/set-full-text-search %]))
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


(defn RootProjects []
  (let [tr     (subscribe [::i18n-subs/tr])
        module (subscribe [::apps-subs/module])]
    (dispatch [::apps-events/get-module])
    (fn []
      (let []
        [:<>
         [apps-views-detail/add-modal]
         [apps-views-detail/format-error @module]
         [:<>
          [ControlBarProjects]
          (when (and @module (not (instance? js/Error @module)))
            (let [{:keys [children]} @module]
              [apps-project-views/format-module-children children]))]]))))


(defn AppStore
  []
  (let [tr                (subscribe [::i18n-subs/tr])
        modules           (subscribe [::subs/modules])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])]
    (dispatch [::events/get-modules])
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


(defn TabMyApps
  []
  (let [tr                (subscribe [::i18n-subs/tr])
        modules           (subscribe [::subs/my-modules])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])]
    (dispatch [::events/get-my-modules])
    (fn []
      (let [total-modules (get @modules :count 0)
            total-pages   (general-utils/total-pages total-modules @elements-per-page)]
        [ui/Segment
         [MyAppsControlBar]
         [ModulesCardsGroup (get @modules :resources [])]
         [uix/Pagination
          {:totalitems   total-modules
           :totalPages   total-pages
           :activePage   @page
           :onPageChange (ui-callback/callback
                           :activePage #(dispatch [::events/set-page %]))}]]))))


(defn TabGettingStarted
  []
  (let [tr                (subscribe [::i18n-subs/tr])
        modules           (subscribe [::subs/modules])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])]
    ;    (dispatch [::events/get-getting-started-modules])
    (fn []
      (let [total-modules (get @modules :count 0)
            total-pages   (general-utils/total-pages total-modules @elements-per-page)]
        [ui/Segment
         [:div "Here's a good place to start."]
         [ModulesCardsGroup (get @modules :resources [])]
         [uix/Pagination
          {:totalitems   total-modules
           :totalPages   total-pages
           :activePage   @page
           :onPageChange (ui-callback/callback
                           :activePage #(dispatch [::events/set-page %]))}]]))))


(defn TabAppStore
  []
  (fn []
    [ui/TabPane
     [AppStore]]))


(defn TabNavigator
  []
  (fn []
    [ui/TabPane
     [RootProjects]]))


(defn TabDeployments
  []
  (fn []
    [ui/TabPane
     [deployment-views/deployments-main-content]]))


(defn TabDiscoverSection
  [icon-name section-key modules dispatch-list]
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
       [ui/GridColumn {:only    "computer tablet"
                       :floated "left"}
        [ui/Button {:floated  "right"
                    :primary  true
                    :on-click #(dispatch dispatch-list)} "See more"]]]
      [ui/GridRow
       [ModulesCardsGroup (take 4 (get modules :resources []))]]
      [ui/GridRow {:only "mobile"}
       [ui/GridColumn {:textAlign "center"}
        [ui/Button {:primary  true
                    :on-click #(dispatch dispatch-list)} "See more"]]]]]))


(defn TabDiscover
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        modules        (subscribe [::subs/modules])
        all-my-modules (subscribe [::subs/my-modules])]
    (dispatch [::events/get-modules ""])
    (dispatch [::events/get-my-modules ""])
    (fn []
      (let []
        [:<>
         [TabDiscoverSection "fas fa-store" :appstore @modules [::events/set-active-tab-index utils/tab-app-store]]
         [TabDiscoverSection "fas fa-th" :all-apps @modules [::events/set-active-tab-index utils/tab-all-apps]]
         [TabDiscoverSection "user" :my-apps @all-my-modules [::events/set-active-tab-index utils/tab-my-apps]]]))))


(defn Tabs
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [
     {:menuItem {:content "Discover"
                 :key     "discover"
                 :icon    "play"}
      :render   (fn [] (r/as-element [TabDiscover]))}
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
      :render   (fn [] (r/as-element [TabAppStore]))}
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
    [ui/Container {:fluid true}
     [:<>
      [uix/PageHeader "fas fa-store" (general-utils/capitalize-first-letter (@tr [:apps]))]
      [TabsApps]
      [deployment-dialog-views/deploy-modal]]]))
