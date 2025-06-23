(ns sixsq.nuvla.ui.pages.dashboard.views
  (:require [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.plugins.audit-log :as audit-log-plugin]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.pages.apps.apps-store.subs :as apps-store-subs]
            [sixsq.nuvla.ui.pages.credentials.subs :as credentials-subs]
            [sixsq.nuvla.ui.pages.dashboard.events :as events]
            [sixsq.nuvla.ui.pages.dashboard.spec :as spec]
            [sixsq.nuvla.ui.pages.dashboard.utils :as utils]
            [sixsq.nuvla.ui.pages.deployments.subs :as deployments-subs]
            [sixsq.nuvla.ui.pages.deployments.views :as deployments-views]
            [sixsq.nuvla.ui.pages.edges.subs :as edges-subs]
            [sixsq.nuvla.ui.pages.edges.utils :as edges-utils]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as utils-style]))


(defn init
  [& opts]
  (dispatch [::events/init opts]))


(defn refresh
  [& opts]
  (dispatch [::events/refresh opts]))


(defn MenuRefresh
  []
  [:span {:style {:display "inline-flex"}}
   [components/RefreshCompact
    {:action-id  events/refresh-action-deployments-id
     :on-refresh refresh}]])

(defn Statistic
  [{:keys [value icon class label target on-click positive-color color]
    :or   {positive-color "black"}}]
  (let [color        (if (pos? value) (or color positive-color) "grey")
        {:keys [resource tab-event]} target
        interactive? (or on-click tab-event resource)]
    [ui/Statistic {:style    {:cursor (when interactive? "pointer")}
                   :color    color
                   :class    (conj [(when interactive? "slight-up")] class)
                   :on-click #(if on-click
                                (on-click)
                                (do
                                  (when tab-event
                                    (dispatch tab-event))
                                  (when resource
                                    (dispatch [::routing-events/navigate resource]))))}
     [icons/Icon {:name  icon
                  :color color}]
     [ui/StatisticValue (or value 0)]
     [ui/StatisticLabel label]]))

(defn StatisticStatesEdgeView [{:keys [total online offline unknown]}]
  [ui/StatisticGroup {:size  "tiny"
                      :style {:padding "0.2rem"}}
   [Statistic {:value total
               :icon  icons/i-box
               :label "TOTAL"
               :color "black"}]
   [Statistic {:value          online
               :icon           icons/i-power
               :label          edges-utils/status-online
               :positive-color "green"
               :color          "green"}]
   [Statistic {:value offline
               :icon  icons/i-power
               :label edges-utils/status-offline
               :color "red"}]
   [Statistic {:value unknown
               :icon  icons/i-power
               :label edges-utils/status-unknown
               :color "orange"}]])


(defn StatisticStatesEdge
  []
  (let [summary-stats (subscribe [::edges-subs/nuvlaboxes-summary-all-stats])]
    [StatisticStatesEdgeView @summary-stats]))

(defn TabOverviewNuvlaBox
  []
  (let [tr @(subscribe [::i18n-subs/tr])
        {:keys [resource tab-index tab-index-event]} utils/target-nbs]
    [ui/Segment {:secondary true
                 :class     "nuvla-edges"
                 :style     {:display         "flex"
                             :flex-direction  "column"
                             :justify-content "space-between"
                             :overflow        :hidden}}

     [:div
      [:h4 {:class "ui-header"}
       [icons/BoxIcon]
       "NuvlaEdges"]

      [StatisticStatesEdge]]

     [uix/Button {:class    "center"
                  :icon     (utils/type->icon utils/type-nbs)
                  :content  (tr [:show-me])
                  :on-click #(do (when (and tab-index tab-index-event)
                                   (dispatch [tab-index-event tab-index]))
                                 (dispatch [::routing-events/navigate resource]))}]]))
(defn TabOverviewDeployments
  []
  (let [{:keys [resource tab-key tab-event]} utils/target-deployments]
    [deployments-views/DeploymentsOverviewSegment
     {:sub-key  ::deployments-subs/deployments-summary-all
      :on-click #(do (when (and tab-event tab-key)
                       (dispatch [tab-event tab-key]))
                     (dispatch [::routing-events/navigate resource]))}]))


(defn TabOverviewAuditLog
  []
  (let [tr @(subscribe [::i18n-subs/tr])]
    [ui/Segment {:secondary true
                 :class     "audit-logs"
                 :style     {:display         "flex"
                             :flex-direction  "column"
                             :justify-content "space-between"}}

     [:div
      [:h4 {:class "ui-header"}
       [icons/BoltIcon]
       (tr [:audit-log])]

      [audit-log-plugin/EventsTableWithFilters {:db-path    [::spec/events]
                                                :max-height 300}]]]))


(defn Statistics
  []
  (let [apps              (subscribe [::apps-store-subs/modules])
        no-of-apps        (:count @apps)
        nuvlaboxes        (subscribe [::edges-subs/nuvlaboxes-summary-all])
        no-of-nb          (:count @nuvlaboxes)
        deployments       (subscribe [::deployments-subs/deployments-summary-all])
        no-of-deployments (:count @deployments)
        credentials       (subscribe [::credentials-subs/credentials-summary])
        no-of-creds       (get-in @credentials [:aggregations :value_count:id :value])]
    [ui/StatisticGroup (merge {:widths 10 :size "tiny"
                               :style  {:margin     "0px auto 10px auto"
                                        :display    "flex"
                                        :text-align "center"
                                        :width      "100%"
                                        :max-width  1200
                                        :padding    "2rem"}})
     [Statistic {:value no-of-apps :icon (utils/type->icon utils/type-apps) :class "nuvla-apps" :label utils/type-apps :target utils/target-apps}]
     [Statistic {:value no-of-deployments :icon (utils/type->icon utils/type-deployments) :class "nuvla-deployments" :label utils/type-deployments :target utils/target-deployments}]
     [Statistic {:value no-of-nb :icon (utils/type->icon utils/type-nbs) :class "nuvla-edges" :label utils/type-nbs :target utils/target-nbs}]
     [Statistic {:value no-of-creds :icon (utils/type->icon utils/type-creds) :class "nuvla-credentials" :label utils/type-creds :target utils/target-creds}]]))


(defn DashboardMain
  []
  (init)
  (refresh)
  (fn []
    [components/LoadingPage {}
     [:<>
      [:div {:style {:display         :flex
                     :justify-content :space-between}}
       [MenuRefresh]]
      [Statistics]
      [:div utils-style/center-items
       [ui/Grid {:columns   2,
                 :stackable true
                 :padded    true}
        [ui/GridRow
         [ui/GridColumn {:stretched true}
          [TabOverviewDeployments]]
         [ui/GridColumn {:stretched true
                         :style     {:max-height 320}}
          [TabOverviewNuvlaBox]]
         [ui/GridColumn {:stretched true
                         :class     "sixteen wide"
                         :style     {:max-height 500}}
          [TabOverviewAuditLog]]]]]]]))


(defn dashboard-view
  [{path :path}]
  (let [n    (count path)
        [_ uuid] path
        root [DashboardMain]]
    (case n
      2 ^{:key uuid} (dispatch [::routing-events/navigate routes/deployment-details {:uuid uuid}])
      [ui/Segment utils-style/basic root])))
