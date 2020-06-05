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
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.apps.views-detail :as apps-views-detail]
    [sixsq.nuvla.ui.deployment-dialog.events :as deployment-dialog-events]
    [sixsq.nuvla.ui.deployment-dialog.views :as deployment-dialog-views]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn refresh-menu
  []
  [main-components/RefreshMenu
   {:on-refresh #(do (dispatch [::events/get-modules])
                     (dispatch [::apps-events/get-module]))}])

(defn module-card
  [{:keys [id name description path parent-path subtype compatibility logo-url] :as module}]
  (let [tr          (subscribe [::i18n-subs/tr])
        detail-href (str "apps/" path)]
    [ui/Card
     (when logo-url
       [ui/Image {:src   logo-url
                  :style {:width      "auto"
                          :height     "100px"
                          :object-fit "contain"}}])
     [ui/CardContent {:href     detail-href
                      :on-click (fn [event]
                                  (dispatch [::history-events/navigate detail-href])
                                  (.preventDefault event))}
      [ui/CardHeader {:style {:word-wrap "break-word"}}
       [ui/Icon {:name (apps-utils/subtype-icon subtype)}]
       (or name id)]
      [ui/CardMeta {:style {:word-wrap "break-word"}} parent-path]
      [ui/CardDescription {:style {:overflow "hidden" :max-height "100px"}} description]
      (when compatibility
        [ui/Label {:color "grey", :corner "right"}
         [ui/Popup
          {:position "top center"
           :content  (str "COMPATIBILITY: " compatibility)
           :size     "small"
           :trigger  (r/as-element [ui/Icon {:name "info"}])}]])]
     [ui/Button {:fluid    true
                 :primary  true
                 :icon     :rocket
                 :content  (@tr [:launch])
                 :on-click #(dispatch [::main-events/subscription-required-dispatch
                                       [::deployment-dialog-events/create-deployment
                                        (:id module) :infra-services]])}]]))


(defn modules-cards-group
  [modules-list]
  [ui/Segment style/basic
   [ui/CardGroup {:centered true}
    (for [{:keys [id] :as module} modules-list]
      ^{:key id}
      [module-card module])]])


(defn control-bar
  []
  (let [full-text (subscribe [::subs/full-text-search])]
    [ui/Menu {:secondary true}
     [ui/MenuMenu {:position "left"}
      [main-components/SearchInput
       {:on-change     (ui-callback/input-callback #(dispatch [::events/set-full-text-search %]))
        :default-value @full-text}]]
     [refresh-menu]]))


(defn control-bar-projects []
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Menu {:borderless true}
     [uix/MenuItemWithIcon
      {:name      (@tr [:add])
       :icon-name "add"
       :on-click  #(dispatch [::apps-events/open-add-modal])}]
     [refresh-menu]]))


(defn root-projects []
  (let [tr     (subscribe [::i18n-subs/tr])
        module (subscribe [::apps-subs/module])]
    (dispatch [::apps-events/get-module])
    (fn []
      (let []
        [ui/Container {:fluid true}
         [apps-views-detail/add-modal]
         [apps-views-detail/format-error @module]
         [uix/Accordion
          [:<>
           [control-bar-projects]
           (when (and @module (not (instance? js/Error @module)))
             (let [{:keys [children]} @module]
               [apps-project-views/format-module-children children]))]
          :label (str/capitalize (@tr [:all-projects]))
          :title-size :h2]]))))


(defn appstore
  []
  (let [tr                (subscribe [::i18n-subs/tr])
        modules           (subscribe [::subs/modules])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])]
    (dispatch [::events/set-full-text-search nil])
    (fn []
      (let [total-modules (get @modules :count 0)
            total-pages   (general-utils/total-pages total-modules @elements-per-page)]
        [ui/Container {:fluid true}
         [uix/Accordion
          [:<>
           [control-bar]
           [modules-cards-group (get @modules :resources [])]
           [uix/Pagination
            {:totalitems   total-modules
             :totalPages   total-pages
             :activePage   @page
             :onPageChange (ui-callback/callback
                             :activePage #(dispatch [::events/set-page %]))}]]
          :label (str/capitalize (@tr [:appstore]))
          :title-size :h2]]))))


(defn root-view
  []
  [ui/Container {:fluid true}
   [appstore]
   [root-projects]
   [deployment-dialog-views/deploy-modal]])
