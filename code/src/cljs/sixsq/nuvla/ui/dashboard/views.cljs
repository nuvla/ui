(ns sixsq.nuvla.ui.dashboard.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.apps-store.subs :as apps-store-subs]
    [sixsq.nuvla.ui.apps-store.events :as apps-store-events]
    [sixsq.nuvla.ui.credentials.events :as credentials-events]
    [sixsq.nuvla.ui.credentials.subs :as credentials-subs]
    [sixsq.nuvla.ui.credentials.views :as credentials-views]
    [sixsq.nuvla.ui.dashboard.events :as events]
    [sixsq.nuvla.ui.dashboard.subs :as subs]
    [sixsq.nuvla.ui.deployment.events :as deployment-events]
    [sixsq.nuvla.ui.deployment.subs :as deployment-subs]
    [sixsq.nuvla.ui.deployment.views :as deployment-views]
    [sixsq.nuvla.ui.edge.events :as edge-events]
    [sixsq.nuvla.ui.edge.subs :as edge-subs]
    [sixsq.nuvla.ui.edge.views :as edge-views]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as utils-style]
    [taoensso.timbre :as log]
    [sixsq.nuvla.ui.dashboard.utils :as utils]))


(defn refresh
  [& opts]
  (dispatch [::events/refresh opts]))


(defn MenuRefresh
  []
  (let [loading? (subscribe [::subs/loading?])]
    [:span {:style {:display "inline-flex"}}
     [main-components/RefreshCompact
      {:action-id  events/refresh-action-deployments-id
       :loading?   @loading?
       :on-refresh refresh}]]))


(defn TabOverviewCredentials
  []
  (let [creds      (subscribe [::credentials-subs/credentials])
        grouped    (group-by :state (map #(select-keys % [:state]) (:resources @creds)))
        no-of-apps (count (:resources @creds))
        color      "orange"
        icon       "key"
        {:keys [resource tab-index tab-index-event]} utils/target-creds]
    [ui/Segment {:secondary true
                 :color     color
                 :raised    true}
     [:h4 [ui/Icon {:name icon}] "Credentials "
      (when @creds
        [ui/Label {:circular true
                   :color    color
                   :size     "tiny"}
         no-of-apps])]
     [ui/Table {:basic  "very"
                :padded false}
      [ui/TableBody
       [ui/TableRow
        [ui/TableCell "Commissioned"]
        [ui/TableCell (count (get grouped "COMMISSIONED"))]]
       [ui/TableRow
        [ui/TableCell "New"]
        [ui/TableCell (count (get grouped "NEW"))]]
       [ui/TableRow
        [ui/TableCell "Activated"]
        [ui/TableCell (count (get grouped "Activated"))]]
       [ui/TableRow
        [ui/TableCell "Decommissioning"]
        [ui/TableCell (count (get grouped "DECOMMISSIONING"))]]
       [ui/TableRow
        [ui/TableCell "Decommissioned"]
        [ui/TableCell (count (get grouped "DECOMMISSIONED"))]]
       [ui/TableRow
        [ui/TableCell "Error"]
        [ui/TableCell (count (get grouped "ERROR"))]]
       ]]
     [ui/Button {:fluid    true
                 :icon     icon
                 :color    color
                 :content  "Show me"
                 :on-click #((when (and tab-index tab-index-event)
                               (dispatch [tab-index-event tab-index]))
                              (dispatch [::history-events/navigate resource]))}]]))


(defn TabOverviewApps
  []
  (let [apps       (subscribe [::apps-store-subs/modules])
        grouped    (group-by :state (map #(select-keys % [:state]) (:resources @apps)))
        no-of-apps (count (:resources @apps))
        color      "grey"
        icon       "fas fa-store"
        {:keys [resource tab-index tab-index-event]} utils/target-apps]
    [ui/Segment {:secondary true
                 :color     color
                 :raised    true}
     [:h4 [ui/Icon {:className icon}] "Apps "
      (when @apps
        [ui/Label {:circular true
                   :color    color
                   :size     "tiny"}
         no-of-apps])]
     [ui/Table {:basic  "very"
                :padded false}
      [ui/TableBody
       [ui/TableRow
        [ui/TableCell "Commissioned"]
        [ui/TableCell (count (get grouped "COMMISSIONED"))]]
       [ui/TableRow
        [ui/TableCell "New"]
        [ui/TableCell (count (get grouped "NEW"))]]
       [ui/TableRow
        [ui/TableCell "Activated"]
        [ui/TableCell (count (get grouped "Activated"))]]
       [ui/TableRow
        [ui/TableCell "Decommissioning"]
        [ui/TableCell (count (get grouped "DECOMMISSIONING"))]]
       [ui/TableRow
        [ui/TableCell "Decommissioned"]
        [ui/TableCell (count (get grouped "DECOMMISSIONED"))]]
       [ui/TableRow
        [ui/TableCell "Error"]
        [ui/TableCell (count (get grouped "ERROR"))]]
       ]]
     [ui/Button {:fluid    true
                 :icon     icon
                 :color    color
                 :content  "Show me"
                 :on-click #((when (and tab-index tab-index-event)
                               (dispatch [tab-index-event tab-index]))
                              (dispatch [::history-events/navigate resource]))}]]))


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

     [:h4 [ui/Icon {:name icon}] (str/upper-case "NuvlaBoxes")]

     [edge-views/StatisticStates false]

     [ui/Button {:icon     icon
                 :color    :green
                 :style    {:align-self "start"}
                 :content  "Show me"
                 :on-click #((when (and tab-index tab-index-event)
                               (dispatch [tab-index-event tab-index]))
                              (dispatch [::history-events/navigate resource]))}]]))


(defn TabOverviewDeployments
  []
  (let [tr    (subscribe [::i18n-subs/tr])
        icon  "rocket"
        color "blue"
        {:keys [resource tab-index tab-index-event]} utils/target-deployments]
    [ui/Segment {:secondary true
                 :color     color
                 :raised    true
                 :style     {:display         "flex"
                             :flex-direction  "column"
                             :justify-content "space-between"}}

     [:h4 [ui/Icon {:name icon}] (str/upper-case (@tr [:deployments]))]

     [deployment-views/StatisticStates false]

     [ui/Button {:color    color
                 :icon     icon
                 :style    {:align-self "start"}
                 :content  "Show me"
                 :on-click #((when (and tab-index tab-index-event)
                               (dispatch [tab-index-event tab-index]))
                              (dispatch [::history-events/navigate resource]))}]]))


(defn TabOverviewCredentials
  []
  (let [tr    (subscribe [::i18n-subs/tr])
        icon  "key"
        color "brown"
        {:keys [resource tab-index tab-index-event]} utils/target-creds]
    [ui/Segment {:secondary true
                 :color     color
                 :raised    true
                 :style     {:display         "flex"
                             :flex-direction  "column"
                             :justify-content "space-between"}}

     [:h4 [ui/Icon {:name icon}] (str/upper-case (@tr [:credentials]))]

     [credentials-views/StatisticStates false]

     [ui/Button {:color    color
                 :icon     icon
                 :style    {:align-self "start"}
                 :content  "Show me"
                 :on-click #(dispatch [::history-events/navigate resource])}]]))


(defn Statistic
  [value icon label target]
  (let [color (if (pos? value) "black" "grey")
        {:keys [resource tab-index tab-index-event]} target]
    [ui/Statistic {:style    {:cursor "pointer"}
                   :color    color
                   :class    "slight-up"
                   :on-click #((when (and tab-index tab-index-event)
                                 (dispatch [tab-index-event tab-index]))
                                (dispatch [::history-events/navigate resource]))}
     [ui/StatisticValue (or value "-")
      "\u2002"
      [ui/Icon {:className icon}]]
     [ui/StatisticLabel label]]))


(defn Statistics
  []
  (let [apps              (subscribe [::apps-store-subs/modules])
        no-of-apps        (:count @apps)
        nuvlaboxes        (subscribe [::edge-subs/nuvlaboxes])
        no-of-nb          (:count @nuvlaboxes)
        deployments       (subscribe [::deployment-subs/deployments-summary])
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


(defn dashboard-main
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        loading? (subscribe [::subs/loading?])]
    (refresh)
    (fn []
      (let []
        [:<>
         [:div {:style {:display "flex" :justify-content "space-between"}}
          [uix/PageHeader "dashboard" (str/capitalize (@tr [:dashboard]))]
          [MenuRefresh]
          ]
         [Statistics]
         [:div utils-style/center-items
          [ui/Grid {:columns   2,
                    :stackable true
                    :padded    true}
           [ui/GridRow
            [ui/GridColumn {:stretched true}
             [TabOverviewDeployments]]
            [ui/GridColumn {:stretched true}
             [TabOverviewNuvlaBox]]]
           ]]]))))


(defmethod panel/render :dashboard
  [path]
  (let [n    (count path)
        [_ uuid] path
        root [dashboard-main]]
    (case n
      2 ^{:key uuid} (dispatch [::history-events/navigate (str "deployment/" uuid)])
      [ui/Segment utils-style/basic root])))
