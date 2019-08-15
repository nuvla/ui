(ns sixsq.nuvla.ui.dashboard-detail.views
  (:require
    [clojure.pprint :refer [cl-format]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.acl.views :as acl]
    [sixsq.nuvla.ui.dashboard-detail.events :as events]
    [sixsq.nuvla.ui.dashboard-detail.subs :as subs]
    [sixsq.nuvla.ui.dashboard.subs :as dashboard-subs]
    [sixsq.nuvla.ui.dashboard.utils :as utils]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.history.views :as history-views]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.resource-details :as resource-details]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as values]
    [taoensso.timbre :as log]))


(def refresh-action-id :dashboard-detail-get-deployment)


(defn refresh
  [resource-id]
  (dispatch [::main-events/action-interval-start
             {:id        refresh-action-id
              :frequency 10000
              :event     [::events/get-deployment resource-id]}]))


(defn format-module-link
  [module]
  [history-views/link (str "apps/" module) module])


(defn url-to-row
  [url-name url-pattern]
  (let [url (subscribe [::subs/url url-pattern])]
    [ui/TableRow
     [ui/TableCell url-name]
     [ui/TableCell
      (if @url
        [:<>
         [ui/Icon {:name "external"}]
         [:a {:href @url, :target "_blank"} @url]]
        url-pattern)]]))


(defn urls-section
  [{:keys [module] :as deployment}]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [{:keys [module] :as deployment}]
      (let [urls (get-in module [:content :urls] [])]
        [uix/Accordion
         [ui/Segment style/autoscroll-x
          [ui/Table style/single-line
           [ui/TableHeader
            [ui/TableRow
             [ui/TableHeaderCell [:span (@tr [:name])]]
             [ui/TableHeaderCell [:span (@tr [:value])]]]]
           (when-not (empty? urls)
             [ui/TableBody
              (for [[url-name url-pattern] urls]
                ^{:key url-name}
                [url-to-row url-name url-pattern])])]]
         :count (count urls)
         :label "URLs"]))))


(defn parameter-to-row
  [{:keys [name description value] :as param}]
  (let [table-row [ui/TableRow
                   [ui/TableCell name]
                   [ui/TableCell value]]]
    (if description
      [ui/Popup
       {:content (r/as-element [:p description])
        :trigger (r/as-element table-row)}]
      table-row)))


(defn parameters-section
  []
  (let [tr                    (subscribe [::i18n-subs/tr])
        deployment-parameters (subscribe [::subs/deployment-parameters])]
    (fn []
      (let [params (vals @deployment-parameters)]
        [uix/Accordion
         [ui/Segment style/autoscroll-x
          [ui/Table style/single-line
           [ui/TableHeader
            [ui/TableRow
             [ui/TableHeaderCell [:span (@tr [:name])]]
             [ui/TableHeaderCell [:span (@tr [:value])]]]]
           (when-not (empty? params)
             [ui/TableBody
              (for [{param-name :name :as param} params]
                ^{:key param-name}
                [parameter-to-row param])])]]
         :count (count params)
         :label (@tr [:module-output-parameters])]))))


(defn env-var-to-row
  [{env-name :name env-value :value env-description :description}]
  (let [table-row [ui/TableRow
                   [ui/TableCell env-name]
                   [ui/TableCell env-value]
                   ]]
    (if env-description
      [ui/Popup
       (cond-> {:content (r/as-element [:p env-description])
                :trigger (r/as-element table-row)})]
      table-row)))


(defn env-vars-section
  [{:keys [module] :as deployment}]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [{:keys [module] :as deployment}]
      (let [env-vars (get-in module [:content :environmental-variables] [])]
        [uix/Accordion
         [ui/Segment style/autoscroll-x
          [ui/Table style/single-line
           [ui/TableHeader
            [ui/TableRow
             [ui/TableHeaderCell [:span (@tr [:name])]]
             [ui/TableHeaderCell [:span (@tr [:value])]]]]
           (when-not (empty? env-vars)
             [ui/TableBody
              (for [{:keys [name] :as env-var} env-vars]
                ^{:key (str "env-var-" name)}
                [env-var-to-row env-var])])]]
         :count (count env-vars)
         :label (str/capitalize (@tr [:environmental-variables]))]))))


(def event-fields #{:id :content :timestamp :category})


(defn events-table-info
  [events]
  (when-let [start (-> events last :timestamp)]
    (let [dt-fn (partial utils/assoc-delta-time start)]
      (->> events
           (map #(select-keys % event-fields))
           (map dt-fn)))))


(defn format-delta-time
  [delta-time]
  (cl-format nil "~,2F" delta-time))


(defn event-map-to-row
  [{:keys [id content timestamp category delta-time] :as evt}]
  [ui/TableRow
   [ui/TableCell [values/as-link id :label (general-utils/id->short-uuid id)]]
   [ui/TableCell timestamp]
   [ui/TableCell (format-delta-time delta-time)]
   [ui/TableCell category]
   [ui/TableCell (:state content)]])


(defn events-table
  [events]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [events]
      [ui/Segment style/autoscroll-x
       [ui/Table style/single-line
        [ui/TableHeader
         [ui/TableRow
          [ui/TableHeaderCell [:span (@tr [:event])]]
          [ui/TableHeaderCell [:span (@tr [:timestamp])]]
          [ui/TableHeaderCell [:span (@tr [:delta-min])]]
          [ui/TableHeaderCell [:span (@tr [:category])]]
          [ui/TableHeaderCell [:span (@tr [:state])]]]]
        [ui/TableBody
         (for [{:keys [id] :as event} events]
           ^{:key id}
           [event-map-to-row event])]]])))


(defn events-section
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        events      (subscribe [::subs/events])
        events-info (events-table-info @events)]
    [uix/Accordion [events-table events-info]
     :label (str/capitalize (@tr [:events]))
     :count (count events-info)]))


(defn job-map-to-row
  [{:keys [id action time-of-status-change state progress return-code status-message] :as job}]
  [ui/TableRow
   [ui/TableCell [values/as-link id :label (general-utils/id->short-uuid id)]]
   [ui/TableCell action]
   [ui/TableCell time-of-status-change]
   [ui/TableCell state]
   [ui/TableCell progress]
   [ui/TableCell return-code]
   [ui/TableCell {:style {:white-space "pre"}} status-message]])


(defn jobs-table
  [jobs]
  (let [tr                (subscribe [::i18n-subs/tr])
        elements-per-page (subscribe [::subs/jobs-per-page])
        page              (subscribe [::subs/job-page])]
    (fn [{:keys [resources] :as jobs}]
      (let [total-elements (get jobs :count 0)
            total-pages    (general-utils/total-pages total-elements @elements-per-page)]
        [ui/Segment style/autoscroll-x
         [ui/Table
          [ui/TableHeader
           [ui/TableRow
            [ui/TableHeaderCell [:span (@tr [:job])]]
            [ui/TableHeaderCell [:span (@tr [:action])]]
            [ui/TableHeaderCell [:span (@tr [:timestamp])]]
            [ui/TableHeaderCell [:span (@tr [:state])]]
            [ui/TableHeaderCell [:span (@tr [:progress])]]
            [ui/TableHeaderCell [:span (@tr [:return-code])]]
            [ui/TableHeaderCell [:span (@tr [:message])]]]]
          [ui/TableBody
           (for [{:keys [id] :as job} resources]
             ^{:key id}
             [job-map-to-row job])]]

         (when (> total-pages 1)
           [uix/Pagination {:totalPages   total-pages
                            :activePage   @page
                            :onPageChange (ui-callback/callback
                                            :activePage #(dispatch [::events/set-job-page %]))}])]))))


(defn jobs-section
  []
  (let [tr   (subscribe [::i18n-subs/tr])
        jobs (subscribe [::subs/jobs])
        {:keys [resources]} @jobs]
    [uix/Accordion [jobs-table @jobs]
     :label (str/capitalize (@tr [:job]))
     :count (count resources)]))


(defn log-controller
  []
  (let [{:keys [id]} @(subscribe [::subs/deployment])]
    [ui/Menu

     [ui/MenuItem {:on-click #(dispatch [::events/create-log id "fake-service-name"])} "create log"]

     [ui/MenuItem
      {:on-click #(do
                    (dispatch [::main-events/action-interval-start
                               {:id        :dashboard-detail-fetch-deployment-log
                                :frequency 3000
                                :event     [::events/fetch-deployment-log]}])
                    (dispatch [::main-events/action-interval-start
                               {:id        :dashboard-detail-get-deployment-log
                                :frequency 5000
                                :event     [::events/get-deployment-log]}]))}
      "start fetch action interval"]

     [ui/MenuItem {:on-click #(do
                              (dispatch [::main-events/action-interval-delete
                                         :dashboard-detail-fetch-deployment-log])
                              (dispatch [::main-events/action-interval-delete
                                         :dashboard-detail-get-deployment-log]))}
      "stop fetch action interval"]

     ])
  )

(defn logs-viewer
  []
  (let [deployment-log (subscribe [::subs/deployment-log])
        log            (:log @deployment-log)]
    [:div
     [ui/Segment {:id "log-segment"
                  :style {:max-height 300
                          :overflow-y "auto"}}
      (for [[i line] (map-indexed vector log)]
        ^{:key (str "log_" i)}
        [:pre {:style {:margin-top    3
                       :margin-bottom 3}} line])]

     [ui/Label (str "line count:")
      [ui/LabelDetail (count log)]]
     ]))


(defn logs-section
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [uix/Accordion [:div
                    [log-controller]
                    [logs-viewer]]
     :label (str/capitalize (@tr [:logs]))]))


(defn action-button
  [popup-text icon-name event-kw deployment-id]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Modal
     {:trigger (r/as-element
                 [:div
                  [ui/Popup {:content  (@tr [popup-text])
                             :size     "tiny"
                             :position "top center"
                             :trigger  (r/as-element
                                         [ui/Icon {:name  icon-name
                                                   :style {:cursor "pointer"}
                                                   :color "red"}])}]])
      :header  (@tr [popup-text])
      :content (@tr [:are-you-sure?])
      :actions [{:key     "cancel"
                 :content (@tr [:cancel])}
                {:key     "yes"
                 :content (@tr [:yes]), :primary true
                 :onClick #(dispatch [event-kw deployment-id])}]}]))


(defn stop-button
  [{:keys [id] :as deployment}]
  [action-button :stop "stop" ::events/stop-deployment id])


(defn delete-button
  [{:keys [id] :as deployment}]
  [action-button :delete "trash" ::events/delete id])


(defn DeploymentCard
  [{:keys [id state module] :as deployment} & {:keys [clickable?]
                                               :or   {clickable? true}}]
  (let [tr            (subscribe [::i18n-subs/tr])
        creds-name    (subscribe [::dashboard-subs/creds-name-map])
        credential-id (:parent deployment)
        {module-logo-url :logo-url
         module-name     :name
         module-path     :path
         module-content  :content} module
        cred-info     (get @creds-name credential-id credential-id)
        [primary-url-name
         primary-url-pattern] (-> module-content (get :urls []) first)
        primary-url   (if clickable?
                        (subscribe [::dashboard-subs/deployment-url id primary-url-pattern])
                        (subscribe [::subs/url primary-url-pattern]))
        started?      (utils/is-started? state)]

    ^{:key id}
    [ui/Card
     [ui/Image {:src      (or module-logo-url "")
                :bordered true
                :style    {:width      "auto"
                           :height     "100px"
                           :padding    "20px"
                           :object-fit "contain"}}]

     (when clickable?
       (cond
         (general-utils/can-operation? "stop" deployment) [ui/Label {:corner true, :size "small"}
                                                           [stop-button deployment]]
         (general-utils/can-delete? deployment) [ui/Label {:corner true, :size "small"}
                                                 [delete-button deployment]]))

     [ui/CardContent (when clickable?
                       {:href     (utils/detail-href id)
                        :on-click (fn [event]
                                    (dispatch [::history-events/navigate (utils/detail-href id)])
                                    (.preventDefault event))})

      [ui/Segment (merge style/basic {:floated "right"})
       [:p {:style {:color "initial"}} state]
       [ui/Loader {:active        (utils/deployment-active? state)
                   :indeterminate true}]]

      [ui/CardHeader (if clickable?
                       [:span [:p {:style {:overflow      "hidden",
                                           :text-overflow "ellipsis",
                                           :max-width     "20ch"}} module-name]]
                       [history-views/link (str "apps/" module-path) module-name])]

      [ui/CardMeta (str (@tr [:created]) " " (-> deployment :created time/parse-iso8601 time/ago))]

      [ui/CardDescription

       (when-not (str/blank? cred-info)
         [:div [ui/Icon {:name "key"}] cred-info])]]

     (when (and started? @primary-url)
       [ui/Button {:color   "green"
                   :icon    "external"
                   :content primary-url-name
                   :fluid   true
                   :href    @primary-url
                   :target  "_blank"
                   :rel     "noreferrer"}])]))


(defn summary
  [deployment]
  [ui/CardGroup {:centered true}
   [DeploymentCard deployment :clickable? false]])


(defn menu
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        loading? (subscribe [::subs/loading?])
        {:keys [id] :as deployment} @(subscribe [::subs/deployment])]
    [ui/Menu {:borderless true}
     (when (general-utils/can-delete? deployment)
       [resource-details/delete-button deployment #(dispatch [::events/delete id])])
     (when (general-utils/can-operation? "stop" deployment)
       [resource-details/action-button-icon (@tr [:stop]) (@tr [:yes]) "stop" (@tr [:stop]) (@tr [:are-you-sure?])
        #(dispatch [::events/stop-deployment id]) (constantly nil)])
     [main-components/RefreshMenu
      {:action-id  refresh-action-id
       :loading?   @loading?
       :on-refresh #(refresh id)}]]))


(defn event-get-timestamp
  [event]
  (-> event :timestamp time/parse-iso8601))


(defn deployment-detail
  [uuid]
  (let [tr          (subscribe [::i18n-subs/tr])
        deployment  (subscribe [::subs/deployment])
        resource-id (str "deployment/" uuid)]

    (refresh resource-id)
    (fn [uuid]
      (let [{:keys [id acl] :as dep} @deployment]
        ^{:key uuid}
        [ui/Segment (merge style/basic
                           {:loading (not= uuid (general-utils/id->uuid id))})
         [ui/Container {:fluid true}
          [uix/PageHeader "dashboard" (str/capitalize (@tr [:dashboard])) :inline true]
          [acl/AclButton {:default-value acl
                          :read-only     (not (general-utils/can-edit? dep))
                          :on-change     #(dispatch [::events/edit id (assoc dep :acl %)])}]
          [menu]
          [summary dep]
          [urls-section dep]
          [parameters-section]
          [env-vars-section dep]
          [events-section]
          [logs-section]
          [jobs-section]]]))))
