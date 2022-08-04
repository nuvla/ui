(ns sixsq.nuvla.ui.apps-store.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.apps-project.views :as apps-project-views]
    [sixsq.nuvla.ui.apps-store.events :as events]
    [sixsq.nuvla.ui.apps-store.subs :as subs]
    [sixsq.nuvla.ui.apps.events :as apps-events]
    [sixsq.nuvla.ui.apps.subs :as apps-subs]
    [sixsq.nuvla.ui.apps.utils :as apps-utils]
    [sixsq.nuvla.ui.apps.views-detail :as apps-views-detail]
    [sixsq.nuvla.ui.deployment-dialog.events :as deployment-dialog-events]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.utils.general :as utils-general]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as utils-style]
    [sixsq.nuvla.ui.utils.tab :as tab]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as utils-values]))


(defn RefreshMenu
  []
  [components/RefreshMenu
   {:on-refresh #(do (dispatch [::events/get-modules]))}])


(defn RefreshMyAppsMenu
  []
  [components/RefreshMenu
   {:on-refresh #(dispatch [::events/get-my-modules])}])


(defn ModuleCard
  [{:keys [id name description path subtype logo-url price published versions tags]} show-published?]
  (let [tr             (subscribe [::i18n-subs/tr])
        map-versions   (apps-utils/map-versions-index versions)
        module-id      (if (true? published) (apps-utils/latest-published-module-with-index id map-versions) id)
        module-index   (apps-utils/latest-published-index map-versions)
        detail-href    (str "apps/" path (when (true? published) (str "?version=" module-index)))
        follow-trial?  (get price :follow-customer-trial false)
        button-icon    (if (and price (not follow-trial?)) :cart :rocket)
        button-color   (if follow-trial? "green" "blue")
        launch-price   (str (@tr [(if follow-trial?
                                    :free-trial-and-then
                                    :launch-for)])
                            (/ (:cent-amount-daily price) 100) "€/"
                            (@tr [:day]))
        button-content (if price launch-price (@tr [:launch]))
        on-click       (fn [event]
                         (dispatch [::main-events/subscription-required-dispatch
                                    [::deployment-dialog-events/create-deployment
                                     module-id :infra-services]])
                         (.preventDefault event)
                         (.stopPropagation event))
        button-ops     {:fluid    true
                        :color    button-color
                        :icon     button-icon
                        :content  button-content
                        :on-click on-click}
        desc-summary   (utils-values/markdown->summary description)]
    [uix/Card
     {:image         logo-url
      :header        [:<>
                      [ui/Icon {:name (apps-utils/subtype-icon subtype)}]
                      (or name id)]
      :description   (utils-general/truncate desc-summary 180)
      :content       [uix/Tags tags]
      :corner-button (when (and published show-published?)
                       [ui/Label {:corner true} [uix/Icon {:name apps-utils/publish-icon}]])
      :href          detail-href
      :on-click      #(dispatch [::history-events/navigate detail-href])
      :button        [ui/Button button-ops]}]))


(defn ModulesCardsGroup
  [modules-list show-published?]
  [:div utils-style/center-items
   [ui/CardGroup {:centered    true
                  :itemsPerRow 4
                  :stackable   true}
    (for [{:keys [id] :as module} modules-list]
      ^{:key id}
      [ModuleCard module show-published?])]])


(defn AppStoreControlBar
  []
  (let [full-text (subscribe [::subs/full-text-search-published])]
    [ui/Menu {:secondary true}
     [ui/MenuMenu {:position "left"}
      [components/SearchInput
       {:on-change     (ui-callback/input-callback #(dispatch [::events/set-full-text-search-published %]))
        :default-value @full-text}]]
     [RefreshMenu]]))


(defn AllAppsControlBar
  []
  (let [full-text (subscribe [::subs/full-text-search-all-apps])]
    [ui/Menu {:secondary true}
     [ui/MenuMenu {:position "left"}
      [components/SearchInput
       {:on-change     (ui-callback/input-callback #(dispatch [::events/set-full-text-search-all-apps %]))
        :default-value @full-text}]]
     [RefreshMenu]]))


(defn MyAppsControlBar
  []
  (let [full-text (subscribe [::subs/full-text-search-my])]
    [ui/Menu {:secondary true}
     [ui/MenuMenu {:position "left"}
      [components/SearchInput
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
  (let [module (subscribe [::apps-subs/module])]
    (dispatch [::apps-events/get-module])
    (fn []
      [components/LoadingPage {}
       [:<>
        [apps-views-detail/AddModal]
        [apps-views-detail/format-error @module]
        [ui/TabPane
         [ControlBarProjects]
         (let [{:keys [children]} @module]
           [apps-project-views/FormatModuleChildren children])]]])))


(defn TabAppStore
  []
  (let [modules           (subscribe [::subs/published-modules])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])]
    (dispatch [::events/get-published-modules])
    (fn []
      (let [total-modules (get @modules :count 0)
            total-pages   (utils-general/total-pages total-modules @elements-per-page)]
        [components/LoadingPage {}
         [ui/TabPane
          [AppStoreControlBar]
          [ModulesCardsGroup (get @modules :resources []) false]
          [uix/Pagination
           {:totalitems              total-modules
            :totalPages              total-pages
            :activePage              @page
            :elementsperpage         @elements-per-page
            :onElementsPerPageChange (ui-callback/value #(do (dispatch [::events/set-elements-per-page %])
                                                             (dispatch [::events/set-page-published-modules 1])
                                                             (dispatch [::events/get-published-modules])))
            :onPageChange            (ui-callback/callback
                                       :activePage #(dispatch [::events/set-page-published-modules %]))}]]]))))


(defn TabAllApps
  []
  (let [modules           (subscribe [::subs/modules])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])]
    (dispatch [::events/get-modules])
    (fn []
      (let [total-modules (get @modules :count 0)
            total-pages   (utils-general/total-pages total-modules @elements-per-page)]
        [components/LoadingPage {}
         [ui/TabPane
          [AllAppsControlBar]
          [ModulesCardsGroup (get @modules :resources []) true]
          [uix/Pagination
           {:totalitems              total-modules
            :totalPages              total-pages
            :activePage              @page
            :elementsperpage         @elements-per-page
            :onElementsPerPageChange (ui-callback/value #(do (dispatch [::events/set-elements-per-page %])
                                                             (dispatch [::events/set-page-all-modules 1])
                                                             (dispatch [::events/get-modules])))
            :onPageChange            (ui-callback/callback
                                       :activePage #(dispatch [::events/set-page-all-modules %]))}]]]))))


(defn TabMyApps
  []
  (let [modules           (subscribe [::subs/my-modules])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])]
    (dispatch [::events/get-my-modules])
    (fn []
      (let [total-modules (get @modules :count 0)
            total-pages   (utils-general/total-pages total-modules @elements-per-page)]
        [components/LoadingPage {}
         [ui/TabPane
          [MyAppsControlBar]
          [ModulesCardsGroup (get @modules :resources []) true]
          [uix/Pagination
           {:totalitems              total-modules
            :totalPages              total-pages
            :activePage              @page
            :elementsperpage         @elements-per-page
            :onElementsPerPageChange (ui-callback/value #(do (dispatch [::events/set-elements-per-page %])
                                                             (dispatch [::events/set-page-my-modules 1])
                                                             (dispatch [::events/get-my-modules])))
            :onPageChange            (ui-callback/callback
                                       :activePage #(dispatch [::events/set-page-my-modules %]))}]]]))))


(defn tabs
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [{:menuItem {:content (utils-general/capitalize-words (@tr [:appstore]))
                 :key     :appstore
                 :icon    (r/as-element [ui/Icon {:className "fas fa-store"}])}
      :render   #(r/as-element [TabAppStore])}
     {:menuItem {:content (utils-general/capitalize-words (@tr [:all-apps]))
                 :key     :allapps
                 :icon    "grid layout"}
      :render   #(r/as-element [TabAllApps])}
     {:menuItem {:content (utils-general/capitalize-words (@tr [:my-apps]))
                 :key     :myapps
                 :icon    "user"}
      :render   #(r/as-element [TabMyApps])}
     {:menuItem {:content "Navigate Apps"
                 :key     :navigate
                 :icon    "folder open"}
      :render   #(r/as-element [TabNavigator])}]))


(defn TabsApps
  []
  (let [active-tab (subscribe [::subs/active-tab])
        panes      (tabs)]
    [ui/Tab
     {:menu        {:secondary true
                    :pointing  true
                    :style     {:display        "flex"
                                :flex-direction "row"
                                :flex-wrap      "wrap"}}
      :panes       panes
      :activeIndex (tab/key->index panes @active-tab)
      :onTabChange (tab/on-tab-change
                     panes
                     #(dispatch [::events/set-active-tab %]))}]))


(defn RootView
  []
  (let [tr         (subscribe [::i18n-subs/tr])
        active-tab (subscribe [::subs/active-tab])]
    (dispatch [::apps-events/reset-version])
    (dispatch [::apps-events/module-not-found false])
    (fn []
      @active-tab
      (dispatch [::events/reset-page])
      [ui/Container {:fluid true}
       [:<>
        [uix/PageHeader "fas fa-store" (utils-general/capitalize-first-letter (@tr [:apps]))]
        [TabsApps]]])))
