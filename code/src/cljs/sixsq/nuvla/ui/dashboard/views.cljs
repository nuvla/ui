(ns sixsq.nuvla.ui.dashboard.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.apps-store.subs :as apps-store-subs]
    [sixsq.nuvla.ui.credentials.subs :as credentials-subs]
    [sixsq.nuvla.ui.dashboard.events :as events]
    [sixsq.nuvla.ui.dashboard.utils :as utils]
    [sixsq.nuvla.ui.deployments.subs :as deployments-subs]
    [sixsq.nuvla.ui.deployments.views :as deployments-views]
    [sixsq.nuvla.ui.edges.subs :as edges-subs]
    [sixsq.nuvla.ui.edges.views :as edges-views]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as components]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
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


(defn TabOverviewNuvlaBox
  []
  (let [icon "box"
        {:keys [resource tab-index tab-index-event]} utils/target-nbs]
    [ui/Segment {:secondary true
                 :color     "green"
                 :raised    true
                 :style     {:display         "flex"
                             :flex-direction  "column"
                             :justify-content "space-between"}}

     [:h4 [ui/Icon {:name icon}] (str/upper-case "NuvlaEdges")]

     [edges-views/StatisticStatesEdge false]

     [ui/Button {:icon     icon
                 :color    :green
                 :style    {:align-self "start"}
                 :content  "Show me"
                 :on-click #(do (when (and tab-index tab-index-event)
                                  (dispatch [tab-index-event tab-index]))
                                (dispatch [::history-events/navigate resource]))}]]))


; TODO: reduce duplication with deployment-views/DeploymentsOverviewSegment
(defn TabOverviewDeployments
  []
  (let [tr    (subscribe [::i18n-subs/tr])
        icon  "rocket"
        color "blue"
        {:keys [resource tab-key tab-event]} utils/target-deployments]
    [ui/Segment {:secondary true
                 :color     color
                 :raised    true
                 :style     {:display         "flex"
                             :flex-direction  "column"
                             :justify-content "space-between"}}

     [:h4 [ui/Icon {:name icon}] (str/upper-case (@tr [:deployments]))]

     [deployments-views/StatisticStates false ::deployments-subs/deployments-summary-all]

     [ui/Button {:color    color
                 :icon     icon
                 :style    {:align-self "start"}
                 :content  "Show me"
                 :on-click #(do (when (and tab-event tab-key)
                                  (dispatch [tab-event tab-key]))
                                (dispatch [::history-events/navigate resource]))}]]))


(defn Statistic
  [value icon label target]
  (let [color (if (pos? value) "black" "grey")
        {:keys [resource tab-event]} target]
    [ui/Statistic {:style    {:cursor "pointer"}
                   :color    color
                   :class    "slight-up"
                   :on-click #(do
                                (when tab-event
                                  (dispatch tab-event))
                                (dispatch [::history-events/navigate resource]))}
     [ui/StatisticValue (or value "-")
      "\u2002"
      [ui/Icon {:className icon}]]
     [ui/StatisticLabel label]]))


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
                                        :display    "block"
                                        :text-align "center"
                                        :width      "100%"}})
     [Statistic no-of-apps (utils/type->icon utils/type-apps) utils/type-apps utils/target-apps]
     [Statistic no-of-deployments (utils/type->icon utils/type-deployments) utils/type-deployments utils/target-deployments]
     [Statistic no-of-nb (utils/type->icon utils/type-nbs) utils/type-nbs utils/target-nbs]
     [Statistic no-of-creds (utils/type->icon utils/type-creds) utils/type-creds utils/target-creds]]))


(defn DashboardMain
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    (refresh)
    (fn []
      [components/LoadingPage {}
       [:<>
        [:div {:style {:display         :flex
                       :justify-content :space-between}}
         [uix/PageHeader "dashboard" (str/capitalize (@tr [:dashboard]))]
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


(defmethod panel/render :dashboard
  [path]
  (let [n    (count path)
        [_ uuid] path
        root [DashboardMain]]
    (case n
      2 ^{:key uuid} (dispatch [::history-events/navigate (str "deployment/" uuid)])
      [ui/Segment utils-style/basic root])))
