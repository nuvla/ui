(ns sixsq.nuvla.ui.apps-store.views
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.apps-project.views :as apps-project-views]
            [sixsq.nuvla.ui.apps-store.events :as events]
            [sixsq.nuvla.ui.apps-store.spec :as spec]
            [sixsq.nuvla.ui.apps-store.subs :as subs]
            [sixsq.nuvla.ui.apps.events :as apps-events]
            [sixsq.nuvla.ui.apps.subs :as apps-subs]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.apps.views-detail :as apps-views-detail]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.plugins.nav-tab :as tab-plugin]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.utils :refer [name->href pathify]]
            [sixsq.nuvla.ui.utils.general :as utils-general :refer [format-money]]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as utils-style]
            [sixsq.nuvla.ui.utils.values :as utils-values]))

(defn ModuleCardView
  [{:keys [logo-url subtype name id desc-summary tags published target
           show-published-tick? detail-href on-click button-ops]}]
  [uix/Card
   {:image         logo-url
    :header        [:<>
                    [icons/Icon {:name (apps-utils/subtype-icon subtype)}]
                    (or name id)]
    :description   desc-summary
    :content       [uix/Tags tags]
    :corner-button (when (and published show-published-tick?)
                     [ui/Label {:corner true} [icons/Icon {:name apps-utils/publish-icon}]])
    :href          detail-href
    :on-click      on-click
    :button        [uix/Button button-ops]
    :target        target}])


(defn ModuleCard
  [{:keys [id name description path subtype logo-url price published versions tags]} show-published-tick?]
  (let [tr             (subscribe [::i18n-subs/tr])
        map-versions   (apps-utils/map-versions-index versions)
        module-id      (if (true? published) (apps-utils/latest-published-module-with-index id map-versions) id)
        module-index   (apps-utils/latest-published-index map-versions)
        detail-href    (pathify [(name->href routes/apps) path (when (true? published) (str "?version=" module-index))])
        follow-trial?  (get price :follow-customer-trial false)
        button-icon    (if (and price (not follow-trial?)) :cart icons/i-rocket)
        button-color   (if follow-trial? "green" "blue")
        deploy-price   (str (@tr [(if follow-trial?
                                    :free-trial-and-then
                                    :deploy-for)])
                            (format-money (/ (:cent-amount-daily price) 100)) "/"
                            (@tr [:day]))
        button-content (if price deploy-price (@tr [:deploy]))
        on-click       (fn [event]
                         (apps-views-detail/deploy-click module-id (apps-utils/applications-sets? subtype))
                         (.preventDefault event)
                         (.stopPropagation event))
        button-ops     {:fluid    true
                        :color    button-color
                        :icon     button-icon
                        :content  button-content
                        :on-click on-click}
        desc-summary   (utils-values/markdown->summary description)]
    [ModuleCardView
     {:logo-url logo-url
      :subtype subtype
      :name name
      :id id
      :desc-summary desc-summary
      :tags tags
      :published published
      :show-published-tick? show-published-tick?
      :detail-href detail-href
      :button-ops button-ops}]))

(defn ModulesCardsGroupView
  [& children]
  [:div utils-style/center-items
     [ui/CardGroup {:centered    true
                    :itemsPerRow 4
                    :stackable   true}
      children]])

(defn ModulesCardsGroup
  [active-tab]
  (let [modules              (subscribe [::subs/modules])
        show-published-tick? (boolean (#{:allapps :myapps} active-tab))]
    [ModulesCardsGroupView
     (for [{:keys [id] :as module} (get @modules :resources [])]
       ^{:key id}
       [ModuleCard module show-published-tick?])]))

(defn RefreshButton
  [active-tab]
  [components/RefreshMenu
   {:on-refresh #(dispatch [::events/get-modules active-tab])}])

(defn Pagination
  [active-tab]
  (let [modules @(subscribe [::subs/modules])]
    [pagination-plugin/Pagination
     {:db-path      [(spec/page-keys->pagination-db-path active-tab)]
      :total-items  (:count modules)
      :change-event [::events/get-modules active-tab]}]))

(defn ControlBar
  [active-tab pagination-db-path]
  [ui/Menu {:secondary true}
   [ui/MenuMenu {:position "left"}
    [full-text-search-plugin/FullTextSearch
     {:db-path      [::spec/modules-search]
      :change-event [::pagination-plugin/change-page [(or pagination-db-path (spec/page-keys->pagination-db-path active-tab))] 1]}]]
   [RefreshButton active-tab]])

(defn ControlBarProjects [active-tab]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Menu {:borderless true}
     [uix/MenuItem
      {:name     (@tr [:add])
       :icon     icons/i-plus-large
       :on-click #(dispatch [::apps-events/open-add-modal])}]
     [RefreshButton active-tab]]))

(defn TabNavigator [_active-tab]
  (let [module (subscribe [::apps-subs/module])]
    (dispatch [::apps-events/get-module])
    (fn [active-tab]
      [components/LoadingPage {}
       [:<>
        [apps-views-detail/AddModal]
        [apps-views-detail/format-error @module]
        [ui/TabPane
         {:class :uix-apps-navigator}
         [ControlBarProjects active-tab]
         (let [{:keys [children]} @module]
           [apps-project-views/FormatModuleChildren children])]]])))

(defn TabDefault
  [active-tab]
  (dispatch [::events/get-modules active-tab])
  (dispatch [::events/set-default-tab active-tab])
  ^{:key active-tab}
  [components/LoadingPage {}
   [ui/TabPane
    [ControlBar active-tab]
    [ModulesCardsGroup active-tab]
    [Pagination active-tab]]])


(defn tabs
  []
  (let [tr     @(subscribe [::i18n-subs/tr]) ]
    [{:menuItem {:content (utils-general/capitalize-words (tr [:appstore]))
                 :key     spec/appstore-key
                 :icon    (r/as-element [icons/StoreIcon])}
      :render   #(r/as-element [TabDefault spec/appstore-key])}
     {:menuItem {:content (utils-general/capitalize-words (tr [:all-apps]))
                 :key     spec/allapps-key
                 :icon    (r/as-element [icons/LayerGroupIcon])}
      :render   #(r/as-element [TabDefault spec/allapps-key])}
     {:menuItem {:content (utils-general/capitalize-words (tr [:my-apps]))
                 :key     spec/myapps-key
                 :icon    (r/as-element [icons/StarIcon])}
      :render   #(r/as-element [TabDefault spec/myapps-key])}
     {:menuItem {:content (utils-general/capitalize-words (tr [:navigate-projects]))
                 :key     spec/navigate-key
                 :icon    (r/as-element [icons/FolderIcon])}
      :render   #(r/as-element [TabNavigator spec/navigate-key])}]))

(defn RootView
  []
  (dispatch [::events/init])
  (fn []
    [ui/Container {:fluid true}
     [tab-plugin/Tab
      {:db-path      [::spec/tab]
       :change-event [::pagination-plugin/change-page [::spec/pagination] 1]
       :menu         {:secondary true
                      :pointing  true
                      :style     {:display       "flex"
                                  :flex-direction "row"
                                  :flex-wrap      "wrap"}}
       :panes        (tabs)}]]))
