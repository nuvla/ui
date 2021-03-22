(ns sixsq.nuvla.ui.deployment.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.deployment-detail.views :as deployment-detail-views]
    [sixsq.nuvla.ui.deployment.events :as events]
    [sixsq.nuvla.ui.deployment.subs :as subs]
    [sixsq.nuvla.ui.deployment.utils :as utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.utils.general :as utils-general]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.style :as utils-style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as values]
    [taoensso.timbre :as log]))


(defn refresh
  [& opts]
  (dispatch [::events/refresh opts]))


(defn control-bar []
  (let [tr           (subscribe [::i18n-subs/tr])
        active-only? (subscribe [::subs/active-only?])
        full-text    (subscribe [::subs/full-text-search])]
    [:span                                                  ;{:style {:display "inline"}}

     [main-components/SearchInput
      {:on-change     (ui-callback/input-callback #(dispatch [::events/set-full-text-search %]))
       :default-value @full-text}]
     ]))


(defn MenuBar
  []
  (let [view     (subscribe [::subs/view])
        loading? (subscribe [::subs/loading?])]
    (fn []
      [:<>
       [main-components/StickyBar
        [ui/Menu {:borderless true, :stackable true}
         [ui/MenuItem {:icon     "grid layout"
                       :active   (= @view "cards")
                       :on-click #(dispatch [::events/set-view "cards"])}]
         [ui/MenuItem {:icon     "table"
                       :active   (= @view "table")
                       :on-click #(dispatch [::events/set-view "table"])}]

         [main-components/RefreshMenu
          {:action-id  events/refresh-action-deployments-id
           :loading?   @loading?
           :on-refresh refresh}]]]])))


(defn row-fn
  [{:keys [id state module] :as deployment}]
  (let [credential-id (:parent deployment)
        creds-name    (subscribe [::subs/creds-name-map])
        [primary-url-name
         primary-url-pattern] (-> module :content (get :urls []) first)
        url           @(subscribe [::subs/deployment-url id primary-url-pattern])]
    [ui/TableRow
     [ui/TableCell [values/as-link (utils-general/id->uuid id)
                    :page "dashboard" :label (utils-general/id->short-uuid id)]]
     [ui/TableCell {:style {:overflow      "hidden",
                            :text-overflow "ellipsis",
                            :max-width     "20ch"}} (:name module)]
     [ui/TableCell state]
     [ui/TableCell (when url
                     [:a {:href url, :target "_blank", :rel "noreferrer"}
                      [ui/Icon {:name "external"}]
                      primary-url-name])]
     [ui/TableCell (-> deployment :created time/parse-iso8601 time/ago)]
     [ui/TableCell {:style {:overflow      "hidden",
                            :text-overflow "ellipsis",
                            :max-width     "20ch"}} (get @creds-name credential-id credential-id)]
     [ui/TableCell
      (cond
        (utils-general/can-operation? "stop" deployment)
        [deployment-detail-views/ShutdownButton deployment]

        (utils-general/can-delete? deployment)
        [deployment-detail-views/DeleteButton deployment])]]))


(defn vertical-data-table
  [deployments-list]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [deployments-list]
      (if (empty? deployments-list)
        [uix/WarningMsgNoElements]
        [ui/Table
         (merge style/single-line {:stackable true})
         [ui/TableHeader
          [ui/TableRow
           [ui/TableHeaderCell (@tr [:id])]
           [ui/TableHeaderCell (@tr [:module])]
           [ui/TableHeaderCell (@tr [:status])]
           [ui/TableHeaderCell (@tr [:url])]
           [ui/TableHeaderCell (@tr [:created])]
           [ui/TableHeaderCell (@tr [:infrastructure])]
           [ui/TableHeaderCell (@tr [:actions])]]]
         [ui/TableBody
          (for [{:keys [id] :as deployment} deployments-list]
            ^{:key id}
            [row-fn deployment])]]))))


(defn cards-data-table
  [deployments-list]
  [:div utils-style/center-items
   [ui/CardGroup {:centered    true
                  :itemsPerRow 4
                  :stackable   true}
    (for [{:keys [id] :as deployment} deployments-list]
      ^{:key id}
      [deployment-detail-views/DeploymentCard deployment])]])


(defn deployments-display
  [deployments-list]
  (let [loading? (subscribe [::subs/loading?])
        view     (subscribe [::subs/view])]
    (fn [deployments-list]
      [ui/Segment (merge style/basic
                         {:loading @loading?})
       (if (= @view "cards")
         [cards-data-table deployments-list]
         [vertical-data-table deployments-list])])))


(defn StatisticStates
  ([] [StatisticStates true])
  ([clickable?]
   (let [tr          (subscribe [::i18n-subs/tr])
         summary     (subscribe [::subs/deployments-summary])
         summary-all (subscribe [::subs/deployments-summary-all])
         open-popup  (r/atom true)]
     (fn [clickable?]
       (let [summary       (if clickable? summary summary-all)
             terms         (utils-general/aggregate-to-map (get-in @summary [:aggregations :terms:state :buckets]))
             started       (:STARTED terms 0)
             starting      (:STARTIN terms 0)
             created       (:CREATED terms 0)
             stopped       (:STOPPED terms 0)
             error         (:ERROR terms 0)
             queued        (:QUEUED terms 0)
             starting-plus (+ starting created queued)
             total         (:count @summary)]
         [:div {:style {:margin     "10px auto 10px auto"
                        :text-align "center"
                        :width      "100%"}}
          [ui/StatisticGroup (merge {:widths (if clickable? nil 5) :size "tiny"}
                                    {:style {:margin-right "0px"
                                             :display      "block"}})
           [main-components/StatisticState total ["fas fa-rocket"] "TOTAL" clickable?
            ::events/set-state-selector ::subs/state-selector]
           [main-components/StatisticState started [(utils/status->icon utils/status-started)] utils/status-started
            clickable? "green"
            ::events/set-state-selector ::subs/state-selector]
           [main-components/StatisticState starting-plus [(utils/status->icon utils/status-starting)]
            utils/status-starting clickable? "yellow"
            ::events/set-state-selector ::subs/state-selector]
           [main-components/StatisticState stopped [(utils/status->icon utils/status-stopped)] utils/status-stopped
            clickable? "yellow"
            ::events/set-state-selector ::subs/state-selector]
           [main-components/StatisticState error [(utils/status->icon utils/status-error)] utils/status-error
            clickable? "red" ::events/set-state-selector ::subs/state-selector]
           (if clickable?
             [main-components/ClickMeStaticPopup])]])))))


(defn deployments-main-content
  []
  (let
    [elements-per-page (subscribe [::subs/elements-per-page])
     page              (subscribe [::subs/page])
     deployments       (subscribe [::subs/deployments])
     total-deployments (:count @deployments)
     total-pages       (utils-general/total-pages
                         (get @deployments :count 0) @elements-per-page)
     deployments-list  (get @deployments :resources [])]
    (refresh :init? true)
    [:<>
     [MenuBar]
     [ui/Segment style/basic
      [:div {:style {:display "flex"}}
       [control-bar]
       [StatisticStates true]
       [ui/Input {:style {:visibility "hidden"}
                  :icon  "search"}]]
      [deployments-display deployments-list]]
     [uix/Pagination
      {:totalitems   total-deployments
       :totalPages   total-pages
       :activePage   @page
       :onPageChange (ui-callback/callback :activePage #(dispatch [::events/set-page %]))}]]))
