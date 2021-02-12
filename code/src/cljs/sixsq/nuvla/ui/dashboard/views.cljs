(ns sixsq.nuvla.ui.dashboard.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.apps-store.subs :as apps-store-subs]
    [sixsq.nuvla.ui.apps-store.events :as apps-store-events]
    [sixsq.nuvla.ui.credentials.events :as credentials-events]
    [sixsq.nuvla.ui.credentials.subs :as credentials-subs]
    [sixsq.nuvla.ui.dashboard.events :as events]
    [sixsq.nuvla.ui.dashboard.subs :as subs]
    [sixsq.nuvla.ui.deployment.subs :as deployment-subs]
    [sixsq.nuvla.ui.edge.events :as edge-events]
    [sixsq.nuvla.ui.edge.subs :as edge-subs]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [taoensso.timbre :as log]
    [sixsq.nuvla.ui.dashboard.utils :as utils]))


(defn refresh
  [& opts]
  (dispatch [::events/refresh opts])
  (dispatch [::edge-events/refresh])
  (dispatch [::apps-store-events/get-modules])
  (dispatch [::credentials-events/get-credentials]))


(defn MenuRefresh
  []
  (let [loading? (subscribe [::subs/loading?])]
    [ui/Menu {:borderless true, :stackable true}
     [main-components/RefreshMenu
      {:action-id  events/refresh-action-id
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
     [:h4 [ui/Icon {:name icon}] "Apps "
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
  (let [nuvlaboxes (subscribe [::edge-subs/nuvlaboxes])
        grouped    (group-by :state (map #(select-keys % [:state]) (:resources @nuvlaboxes)))
        no-of-nb   (count (:resources @nuvlaboxes))
        icon       "box"
        {:keys [resource tab-index tab-index-event]} utils/target-nbs]
    [ui/Segment {:secondary true
                 :color     "green"
                 :raised    true}
     [:h4 [ui/Icon {:name icon}] "NuvlaBoxes "
      (when @nuvlaboxes
        [ui/Label {:circular true
                   :color    "green"
                   :size     "tiny"}
         no-of-nb])]
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
                 ;:primary  true
                 :icon     icon
                 :color    :green
                 :content  "Show me"
                 :on-click #((when (and tab-index tab-index-event)
                               (dispatch [tab-index-event tab-index]))
                              (dispatch [::history-events/navigate resource]))}]]))


(defn TabOverviewDeployments
  []
  (let [deployments       (subscribe [::deployment-subs/deployments])
        grouped           (group-by :state (map #(select-keys % [:state]) (:resources @deployments)))
        no-of-deployments (count (:resources @deployments))
        icon              "rocket"
        {:keys [resource tab-index tab-index-event]} utils/target-deployments]
    [ui/Segment {:secondary true
                 :color     "blue"
                 :raised    true}
     [:h4 [ui/Icon {:name icon}] "Running Apps (deployments) "
      (when @deployments
        [ui/Label {:circular true
                   :color    "blue"
                   :size     "tiny"}
         no-of-deployments])]
     [ui/Table {:basic  "very"
                :padded false}
      [ui/TableBody
       [ui/TableRow
        [ui/TableCell "Started"]
        [ui/TableCell (count (get grouped "STARTED"))]]
       [ui/TableRow
        [ui/TableCell "Starting"]
        [ui/TableCell (count (get grouped "STARTING"))]]
       [ui/TableRow
        [ui/TableCell "Created"]
        [ui/TableCell (count (get grouped "CREATED"))]]
       [ui/TableRow
        [ui/TableCell "Stopped"]
        [ui/TableCell (count (get grouped "STOPPED"))]]
       [ui/TableRow
        [ui/TableCell "Error"]
        [ui/TableCell (count (get grouped "ERROR"))]]
       [ui/TableRow
        [ui/TableCell "Queued"]
        [ui/TableCell (count (get grouped "QUEUED"))]]
       ]]
     [ui/Button {:fluid    true
                 :primary  true
                 :icon     :rocket
                 :content  "Show me"
                 :on-click #((when (and tab-index tab-index-event)
                               (dispatch [tab-index-event tab-index]))
                              (dispatch [::history-events/navigate resource]))}]]))


(defn Statistic
  [value icon label target]
  (let [color (if (pos? value) "black" "grey")
        {:keys [resource tab-index tab-index-event]} target]
    [ui/Statistic {:style    {:cursor "pointer"}
                   :color    color
                   :on-click #((when (and tab-index tab-index-event)
                                 (dispatch [tab-index-event tab-index]))
                                (dispatch [::history-events/navigate resource]))}
     [ui/StatisticValue (or value "-")
      "\u2002"
      [ui/Icon {:name icon}]]
     [ui/StatisticLabel label]]))


(defn Statistics
  []
  (let [apps              (subscribe [::apps-store-subs/modules])
        no-of-apps        (:count @apps)
        nuvlaboxes        (subscribe [::edge-subs/nuvlaboxes])
        no-of-nb          (:count @nuvlaboxes)
        deployments       (subscribe [::deployment-subs/deployments])
        no-of-deployments (:count @deployments)
        credentials       (subscribe [::credentials-subs/credentials])
        no-of-creds       (count @credentials)]
    [ui/StatisticGroup (merge {:size "tiny"} style/center-block)
     [Statistic no-of-apps (utils/type->icon utils/type-apps) utils/type-apps utils/target-apps]
     [Statistic no-of-deployments (utils/type->icon utils/type-deployments) utils/type-deployments utils/target-deployments]
     [Statistic no-of-nb (utils/type->icon utils/type-nbs) utils/type-nbs utils/target-nbs]
     [Statistic no-of-creds (utils/type->icon utils/type-creds) utils/type-creds utils/target-creds]]))


(defn dashboard-main
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        loading? (subscribe [::subs/loading?])]
    (refresh :init? true)
    (fn []
      (let []
        [:<>
         [uix/PageHeader "dashboard" (str/capitalize (@tr [:dashboard]))]
         [MenuRefresh]
         [Statistics]
         [ui/Grid {:columns   2,
                   :stackable true
                   :padded    true}
          [ui/GridRow
           [ui/GridColumn {:stretched true}
            [TabOverviewDeployments]]

           [ui/GridColumn {:stretched true}
            [TabOverviewNuvlaBox]]]

          [ui/GridRow
           [ui/GridColumn
            [TabOverviewApps]]
           [ui/GridColumn
            [TabOverviewCredentials]]]
          ]]))))


(defmethod panel/render :dashboard
  [path]
  (let [n    (count path)
        [_ uuid] path
        root [dashboard-main]]
    (case n
      2 ^{:key uuid} (dispatch [::history-events/navigate (str "deployment/" uuid)])
      [ui/Segment style/basic root])))
