(ns sixsq.nuvla.ui.dashboard.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.apps-store.subs :as apps-store-subs]
            [sixsq.nuvla.ui.credentials.subs :as credentials-subs]
            [sixsq.nuvla.ui.dashboard.events :as events]
            [sixsq.nuvla.ui.dashboard.utils :as utils]
            [sixsq.nuvla.ui.deployments.subs :as deployments-subs]
            [sixsq.nuvla.ui.deployments.utils :as deployments-utils]
            [sixsq.nuvla.ui.edges.subs :as edges-subs]
            [sixsq.nuvla.ui.edges.utils :as edges-utils]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.style :as utils-style]))


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
  [{:keys [value icon class label target positive-color icon-color color]
    :or   {positive-color "black"}}]
  (let [color        (or color (if (pos? value) positive-color "grey"))
        {:keys [resource tab-event]} target
        interactive? (or tab-event resource)]
    [ui/Statistic {:style    {:cursor (when interactive? "pointer")}
                   :color    color
                   :class    (conj [(when interactive? "slight-up")] class)
                   :on-click #(do
                                (when tab-event
                                  (dispatch tab-event))
                                (when resource
                                  (dispatch [::routing-events/navigate resource])))}
     [ui/Icon (merge {:className (str "icons " icon)} (when icon-color {:color icon-color}))]
     [ui/StatisticValue (or value "-")]
     [ui/StatisticLabel label]]))


(defn StatisticStatesEdge
  []
  (let [summary         (subscribe [::edges-subs/nuvlaboxes-summary-all])
        total           (:count @summary)
        online-statuses (general-utils/aggregate-to-map
                          (get-in @summary [:aggregations :terms:online :buckets]))
        online          (:1 online-statuses)
        offline         (:0 online-statuses)
        unknown         (- total (+ online offline))]

    [ui/StatisticGroup {:size  "tiny"
                        :style {:padding "0.2rem"}}
     [Statistic {:value total
                 :icon  "fa-light fa-box"
                 :label "TOTAL"
                 :color "black"}]
     [Statistic {:value          online
                 :icon           "fa-light fa-power-off"
                 :label          edges-utils/status-online
                 :positive-color "green"
                 :color          "green"}]
     [Statistic {:value offline
                 :icon  "fa-light fa-power-off"
                 :label edges-utils/status-offline
                 :color "red"}]
     [Statistic {:value unknown
                 :icon  "fa-light fa-power-off"
                 :label edges-utils/status-unknown
                 :color "orange"}]]))

(defn TabOverviewNuvlaBox
  []
  (let [icon "fa-light fa-box"
        {:keys [resource tab-index tab-index-event]} utils/target-nbs]
    [ui/Segment {:secondary true
                 :raised    true
                 :class     "nuvla-edges"
                 :style     {:display         "flex"
                             :flex-direction  "column"
                             :justify-content "space-between"
                             :border-radius   "8px"
                             :overflow         :hidden}}

     [:h4 {:class "ui-header"} [ui/Icon {:name icon}] (str/upper-case "NuvlaEdges")]

     [StatisticStatesEdge]

     [ui/Button {:class    "center"
                 :content  "Show me"
                 :on-click #(do (when (and tab-index tab-index-event)
                                  (dispatch [tab-index-event tab-index]))
                                (dispatch [::routing-events/navigate resource]))}]]))


(defn StatisticStates
  [summary-subs]
  (let [summary       (subscribe [summary-subs])
        terms         (general-utils/aggregate-to-map
                        (get-in @summary [:aggregations :terms:state :buckets]))
        started       (:STARTED terms 0)
        starting      (:STARTING terms 0)
        created       (:CREATED terms 0)
        stopped       (:STOPPED terms 0)
        error         (:ERROR terms 0)
        pending       (:PENDING terms 0)
        starting-plus (+ starting created pending)
        total         (:count @summary)]
    [ui/GridColumn {:class "wide"
                    :style {:padding "0.2rem"}}
     [ui/StatisticGroup {:size  "tiny"
                         :style {:justify-content "center"}}
      [Statistic {:value total :icon "fa-light fa-rocket-launch" :label "TOTAL" :color "black"}]
      [Statistic {:value started :icon (deployments-utils/state->icon deployments-utils/STARTED) :label deployments-utils/STARTED :color "green"}]
      [Statistic {:value starting-plus :icon (deployments-utils/state->icon deployments-utils/STARTING) :label deployments-utils/STARTING :color "orange"}]
      [Statistic {:value stopped :icon (deployments-utils/state->icon deployments-utils/STOPPED) :label deployments-utils/STOPPED :color "orange"}]
      [Statistic {:value error :icon (deployments-utils/state->icon deployments-utils/ERROR) :label deployments-utils/ERROR :positive-color "red"}]]]))

; TODO: reduce duplication with deployment-views/DeploymentsOverviewSegment
(defn TabOverviewDeployments
  []
  (let [tr   (subscribe [::i18n-subs/tr])
        icon "fa-light fa-rocket-launch"
        {:keys [resource tab-key tab-event]} utils/target-deployments]
    [ui/Segment {:secondary true
                 :raised    true
                 :class     "nuvla-deployments"
                 :style     {:display         "flex"
                             :flex-direction  "column"
                             :justify-content "space-between"
                             :border-radius   "8px"
                             :overflow         :hidden}}

     [:h4 {:class "ui-header"} [ui/Icon {:name icon}] (str/upper-case (@tr [:deployments]))]

     [StatisticStates ::deployments-subs/deployments-summary-all]

     [ui/Button {:class    "center"
                 :content  "Show me"
                 :on-click #(do (when (and tab-event tab-key)
                                  (dispatch [tab-event tab-key]))
                                (dispatch [::routing-events/navigate resource]))}]]))


(defn Statistics
  []
  (let [apps              (subscribe [::apps-store-subs/modules])
        no-of-apps        (:count @apps)
        nuvlaboxes        (subscribe [::edges-subs/nuvlaboxes-summary-all])
        no-of-nb          (:count @nuvlaboxes)
        deployments       (subscribe [::deployments-subs/deployments-summary-all])
        no-of-deployments (:count @deployments)
        credentials       (subscribe [::credentials-subs/credentials-summary])
        no-of-creds       (:count @credentials)]
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
  (let [tr (subscribe [::i18n-subs/tr])]
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
           [ui/GridColumn {:stretched true}
            [TabOverviewNuvlaBox]]]]]]])))


(defn dashboard-view
  [{path :path}]
  (let [n    (count path)
        [_ uuid] path
        root [DashboardMain]]
    (case n
      2 ^{:key uuid} (dispatch [::routing-events/navigate routes/deployment-details {:uuid uuid}])
      [ui/Segment utils-style/basic root])))
