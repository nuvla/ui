(ns sixsq.nuvla.ui.apps-store.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.apps-project.views :as apps-project-views]
    [sixsq.nuvla.ui.apps-store.events :as events]
    [sixsq.nuvla.ui.apps-store.spec :as spec]
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
    [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
    [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
    [sixsq.nuvla.ui.plugins.tab :as tab-plugin]
    [sixsq.nuvla.ui.utils.general :as utils-general]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as utils-style]
    [sixsq.nuvla.ui.utils.values :as utils-values]))

(defn ModuleCard
  [{:keys [id name description path subtype logo-url price published versions tags]} show-published-tick?]
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
      :corner-button (when (and published show-published-tick?)
                       [ui/Label {:corner true} [uix/Icon {:name apps-utils/publish-icon}]])
      :href          detail-href
      :on-click      #(dispatch [::history-events/navigate detail-href])
      :button        [ui/Button button-ops]}]))

(defn ModulesCardsGroup
  []
  (let [modules              (subscribe [::subs/modules])
        active-tab           (subscribe [::tab-plugin/active-tab [::spec/tab]])
        show-published-tick? (boolean (#{:allapps :myapps} @active-tab))]
    [:div utils-style/center-items
     [ui/CardGroup {:centered    true
                    :itemsPerRow 4
                    :stackable   true}
      (for [{:keys [id] :as module} (get @modules :resources [])]
        ^{:key id}
        [ModuleCard module show-published-tick?])]]))

(defn RefreshButton
  []
  [components/RefreshMenu
   {:on-refresh #(dispatch [::events/get-modules])}])

(defn Pagination
  []
  (let [modules @(subscribe [::subs/modules])]
    [pagination-plugin/Pagination
     {:db-path      [::spec/pagination]
      :total-items  (:count modules)
      :change-event [::events/get-modules]}]))

(defn ControlBar
  []
  [ui/Menu {:secondary true}
   [ui/MenuMenu {:position "left"}
    [full-text-search-plugin/FullTextSearch
     {:db-path      [::spec/modules-search]
      :change-event [::pagination-plugin/change-page [::spec/pagination] 1]}]]
   [RefreshButton]])

(defn ControlBarProjects []
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Menu {:borderless true}
     [uix/MenuItem
      {:name     (@tr [:add])
       :icon     "add"
       :on-click #(dispatch [::apps-events/open-add-modal])}]
     [RefreshButton]]))

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

(defn TabDefault
  []
  [components/LoadingPage {}
   [ui/TabPane
    [ControlBar]
    [ModulesCardsGroup]
    [Pagination]]])

(defn tabs
  []
  (let [tr     @(subscribe [::i18n-subs/tr])
        render #(r/as-element [TabDefault])]
    [{:menuItem {:content (utils-general/capitalize-words (tr [:appstore]))
                 :key     :appstore
                 :icon    (r/as-element [ui/Icon {:className "fas fa-store"}])}
      :render   render}
     {:menuItem {:content (utils-general/capitalize-words (tr [:all-apps]))
                 :key     :allapps
                 :icon    "grid layout"}
      :render   render}
     {:menuItem {:content (utils-general/capitalize-words (tr [:my-apps]))
                 :key     :myapps
                 :icon    "user"}
      :render   render}
     {:menuItem {:content (utils-general/capitalize-words (tr [:navigate-apps]))
                 :key     :navigate
                 :icon    "folder open"}
      :render   #(r/as-element [TabNavigator])}]))

(defn RootView
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    (dispatch [::events/init])
    (fn []
      [ui/Container {:fluid true}
       [uix/PageHeader "fas fa-store"
        (utils-general/capitalize-first-letter (@tr [:apps]))]
       [tab-plugin/Tab
        {:db-path      [::spec/tab]
         :change-event [::pagination-plugin/change-page [::spec/pagination] 1]
         :menu         {:secondary true
                        :pointing  true
                        :style     {:display        "flex"
                                    :flex-direction "row"
                                    :flex-wrap      "wrap"}}
         :panes        (tabs)}]])))
