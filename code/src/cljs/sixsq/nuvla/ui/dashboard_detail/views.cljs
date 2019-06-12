(ns sixsq.nuvla.ui.dashboard-detail.views
  (:require
    [clojure.pprint :refer [cl-format]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.cimi.subs :as api-subs]
    [sixsq.nuvla.ui.dashboard-detail.events :as events]
    [sixsq.nuvla.ui.dashboard-detail.subs :as subs]
    [sixsq.nuvla.ui.dashboard-detail.views-operations :as views-operations]
    [sixsq.nuvla.ui.dashboard.subs :as dashboard-subs]
    [sixsq.nuvla.ui.dashboard.utils :as utils]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.history.views :as history-views]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.utils.collapsible-card :as cc]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [taoensso.timbre :as log]))


(defn automatic-refresh
  [resource-id]
  (dispatch [::main-events/action-interval
             {:action    :start
              :id        :dashboard-detail-get-deployment
              :frequency 30000
              :event     [::events/get-deployment resource-id]}])
  (dispatch [::main-events/action-interval
             {:action    :start
              :id        :dashboard-detail-deployment-parameters
              :frequency 20000
              :event     [::events/get-deployment-parameters resource-id]}])
  (dispatch [::main-events/action-interval
             {:action    :start
              :id        :dashboard-detail-events
              :frequency 30000
              :event     [::events/get-events resource-id]}])
  (dispatch [::main-events/action-interval
             {:action    :start
              :id        :dashboard-detail-jobs
              :frequency 30000
              :event     [::events/get-jobs resource-id]}]))


(defn format-module-link
  [module]
  [history-views/link (str "apps/" module) module])


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
        deployment-parameters (subscribe [::subs/deployment-parameters])
        params                (vals @deployment-parameters)]
    (fn []
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
       :label (@tr [:module-output-parameters])])))


(def event-fields #{:id :content :timestamp :category})


(defn events-table-info
  [events]
  (when-let [start (-> events last :timestamp)]
    (let [dt-fn (partial utils/assoc-delta-time start)]
      (->> events
           (map #(select-keys % event-fields))
           (map dt-fn)))))


(defn link-short-uuid
  [id]
  (let [tag (general-utils/id->short-uuid id)]
    [history-views/link (utils/detail-href id) tag]))


(defn format-delta-time
  [delta-time]
  (cl-format nil "~,2F" delta-time))


(defn event-map-to-row
  [{:keys [id content timestamp category delta-time] :as evt}]
  [ui/TableRow
   [ui/TableCell (link-short-uuid id)]
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
    [uix/Accordion [events-table events-info], :label (@tr [:events]), :count (count events-info)]))


(defn job-map-to-row
  [{:keys [id action time-of-status-change state progress return-code status-message] :as job}]
  [ui/TableRow
   [ui/TableCell (link-short-uuid id)]
   [ui/TableCell action]
   [ui/TableCell time-of-status-change]
   [ui/TableCell state]
   [ui/TableCell progress]
   [ui/TableCell return-code]
   [ui/TableCell {:style {:white-space "pre"}} status-message]])


(defn jobs-table
  [jobs]
  (let [tr (subscribe [::i18n-subs/tr])
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
    [uix/Accordion [jobs-table @jobs], :label (@tr [:job]), :count (count resources)]))


(defn refresh-button
  []
  (let [tr         (subscribe [::i18n-subs/tr])
        loading?   (subscribe [::subs/loading?])
        deployment (subscribe [::subs/deployment])]
    (fn []
      [ui/MenuMenu {:position "right"}
       [uix/MenuItemWithIcon
        {:name      (@tr [:refresh])
         :icon-name "refresh"
         :loading?  @loading?
         :on-click  #(dispatch [::events/get-deployment (:id @deployment)])}]])))


(defn node-url
  [url-name url-pattern]
  (let [url (subscribe [::subs/url url-pattern])]
    (when @url
      [:div {:key url-name}
       [ui/Icon {:name "external"}]
       [:a {:href @url, :target "_blank"} (str url-name ": " @url)]])))


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
                 :content (@tr [:yes]), :positive true
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
  (let [tr             (subscribe [::i18n-subs/tr])
        creds-name     (subscribe [::dashboard-subs/creds-name-map])
        credential-id  (:credential-id deployment)
        {module-logo-url :logo-url
         module-name     :name
         module-path     :path
         module-content  :content} module
        cred-info      (get @creds-name credential-id credential-id)
        urls           (get module-content :urls [])
        secondary-urls (rest urls)
        [primary-url-name
         primary-url-pattern] (first urls)
        primary-url    (subscribe [::subs/url primary-url-pattern])
        started?       (utils/is-started? state)]

    ^{:key id}
    [ui/Card
     [ui/Image {:src      (or module-logo-url "")
                :bordered true
                :style    {:width      "auto"
                           :height     "100px"
                           :padding    "20px"
                           :object-fit "contain"}}]

     (cond
       (utils/stop-action? deployment) [ui/Label {:corner true, :size "small"}
                                        [stop-button deployment]]
       (utils/delete-action? deployment) [ui/Label {:corner true, :size "small"}
                                          [delete-button deployment]])

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

     (when-not clickable?
       (when (and started? (seq secondary-urls))
         [ui/CardContent {:extra true}
          (for [[url-name url-pattern] secondary-urls]
            ^{:key url-name}
            [node-url url-name url-pattern])]))

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
  (let [deployment (subscribe [::subs/deployment])
        cep        (subscribe [::api-subs/cloud-entry-point])]
    [ui/Menu {:borderless true}
     [views-operations/format-operations @deployment (:base-uri @cep) nil]
     [refresh-button]]))


(defn event-get-timestamp
  [event]
  (-> event :timestamp time/parse-iso8601))


(defn deployment-detail
  [uuid]
  (let [deployment  (subscribe [::subs/deployment])
        resource-id (str "deployment/" uuid)]

    (automatic-refresh resource-id)
    (fn [uuid]
      ^{:key uuid}
      [ui/Segment (merge style/basic
                         {:loading (not= uuid (-> @deployment
                                                  :id
                                                  (str/split #"/")
                                                  second))})
       [ui/Container {:fluid true}
        [menu]
        [summary @deployment]
        [parameters-section]
        [events-section]
        [jobs-section]]])))
