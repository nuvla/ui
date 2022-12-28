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
        icon  "fa-light fa-rocket-launch"
        {:keys [resource tab-key tab-event]} utils/target-deployments]
    [ui/Segment {:secondary true
                 :raised    true
                 :class     "nuvla-deployments"
                 :style     {:display         "flex"
                             :flex-direction  "column"
                             :justify-content "space-between"
                             :border-radius   "8px"}}

     [:h4 {:class "ui-header"} [ui/Icon {:name icon}] (str/upper-case (@tr [:deployments]))]

     [deployments-views/StatisticStates false ::deployments-subs/deployments-summary-all]

     [ui/Button {:icon     icon
                 :style    {:align-self "start"}
                 :content  "Show me"
                 :on-click #(do (when (and tab-event tab-key)
                                  (dispatch [tab-event tab-key]))
                                (dispatch [::history-events/navigate resource]))}]]))


(defn Statistic
  [{:keys [value icon class label target]}]
  (let [color (if (pos? value) "black" "grey")
        {:keys [resource tab-event]} target]
    [ui/Statistic {:style    {:cursor "pointer"}
                   :color    color
                   :class    (conj ["slight-up"] class)
                   :on-click #(do
                                (when tab-event
                                  (dispatch tab-event))
                                (dispatch [::history-events/navigate resource]))}
     [ui/Icon {:className icon}]
     [ui/StatisticValue (or value "-")]
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
                                        :display    "flex"
                                        :text-align "center"
                                        :width      "100%"
                                        :max-width  1200
                                        :padding    "2rem"}})
     [Statistic {:value no-of-apps :icon  (utils/type->icon utils/type-apps) :class  "nuvla-apps" :label  utils/type-apps :target  utils/target-apps}]
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


(defmethod panel/render :dashboard
  [path]
  (let [n    (count path)
        [_ uuid] path
        root [DashboardMain]]
    (case n
      2 ^{:key uuid} (dispatch [::history-events/navigate (str "deployment/" uuid)])
      [ui/Segment utils-style/basic root])))
