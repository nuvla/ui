(ns sixsq.nuvla.ui.dashboard-detail.views
  (:require
    [clojure.pprint :refer [cl-format]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as reagent]
    [sixsq.nuvla.ui.apps.utils :as apps-utils]
    [sixsq.nuvla.ui.cimi.subs :as api-subs]
    [sixsq.nuvla.ui.dashboard-detail.events :as events]
    [sixsq.nuvla.ui.dashboard-detail.subs :as subs]
    [sixsq.nuvla.ui.dashboard-detail.utils :as dashboard-detail-utils]
    [sixsq.nuvla.ui.dashboard-detail.views-operations :as views-operations]
    [sixsq.nuvla.ui.history.views :as history]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.utils.collapsible-card :as cc]
    [sixsq.nuvla.ui.utils.general :as general]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]))


(defn nodes-list                                            ;FIXME
  []
  ["machine"])


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


(def deployment-summary-keys #{:created
                               :updated
                               :resource-url
                               :state
                               :id
                               :name
                               :description})


(defn module-name
  [deployment]
  (-> deployment :module :name))


(defn format-parameter-key
  [k]
  (let [key-as-string (name k)]
    (if-let [abbrev-name (second (re-matches #"^.*:(.*)$" key-as-string))]
      abbrev-name
      key-as-string)))


(defn format-module-link
  [module]
  [history/link (str "application/" module) module])


(defn format-parameter-value
  [k v]
  (let [value (str v)]
    (cond
      (re-matches #"^.*:url.*$" (name k)) [:a {:href value} value]
      (= :module k) (format-module-link value)
      :else value)))


(defn tuple-to-row
  [[key value]]
  [ui/TableRow
   [ui/TableCell {:collapsing true} (format-parameter-key key)]
   [ui/TableCell {:style {:max-width     "80ex"             ;; FIXME: need to get this from parent container
                          :text-overflow "ellipsis"
                          :overflow      "hidden"}} (format-parameter-value key value)]])


(defn visible-state
  [state ss-state]
  (let [lower-state (str/lower-case state)]
    (case state
      "STARTED" (or ss-state lower-state)
      "STOPPED" (or ss-state lower-state)
      lower-state)))


(defn metadata-section
  []
  (let [deployment (subscribe [::subs/deployment])]
    (fn []
      (let [summary-info (-> (select-keys @deployment deployment-summary-keys)
                             (merge (select-keys (:module @deployment) #{:name :path :subtype})
                                    {:owners (->> @deployment :acl :owners (str/join ", "))}))
            icon         (-> @deployment :module :subtype apps-utils/subtype-icon)
            rows         (map tuple-to-row summary-info)
            state        (:state @deployment)]
        [cc/metadata
         {:title       (module-name @deployment)
          :description (:startTime summary-info)
          :icon        icon
          :subtitle    state}
         rows]))))


(defn node-parameter-table
  [params]
  [ui/Table style/definition
   (vec (concat [ui/TableBody] (map tuple-to-row params)))])


(defn parameter-to-row
  [{:keys [name description value] :as param}]
  (let [table-row [ui/TableRow
                   [ui/TableCell name]
                   [ui/TableCell value]]]
    (if description
      [ui/Popup
       {:content (reagent/as-element [:p description])
        :trigger (reagent/as-element table-row)}]
      table-row)))

(defn parameters-section
  []
  (let [tr                    (subscribe [::i18n-subs/tr])
        deployment-parameters (subscribe [::subs/deployment-parameters])]
    (fn []
      (let [params (vals @deployment-parameters)]
        [cc/collapsible-segment (@tr [:parameters])
         [ui/Segment style/autoscroll-x
          [ui/Table style/single-line
           [ui/TableHeader
            [ui/TableRow
             [ui/TableHeaderCell [:span (@tr [:name])]]
             [ui/TableHeaderCell [:span (@tr [:value])]]]]
           (when-not (empty? params)
             (vec (concat [ui/TableBody]
                          (map parameter-to-row params))))]]]))))


(def event-fields #{:id :content :timestamp :category})


(defn events-table-info
  [events]
  (when-let [start (-> events last :timestamp)]
    (let [dt-fn (partial dashboard-detail-utils/assoc-delta-time start)]
      (->> events
           (map #(select-keys % event-fields))
           (map dt-fn)))))


(defn format-id
  [id]
  (let [tag (second (re-matches #"^.*/([^-]+).*$" id))]
    [history/link (str "api/" id) tag]))


(defn format-delta-time
  [delta-time]
  (cl-format nil "~,2F" delta-time))


(defn event-map-to-row
  [{:keys [id content timestamp category delta-time] :as evt}]
  [ui/TableRow
   [ui/TableCell (format-id id)]
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
        (vec (concat [ui/TableBody]
                     (map event-map-to-row events)))]])))


(defn events-section
  []
  (let [tr     (subscribe [::i18n-subs/tr])
        events (subscribe [::subs/events])]
    (fn []
      (let [events (events-table-info @events)]
        [cc/collapsible-segment
         (@tr [:events])
         [events-table events]]))))


(defn job-map-to-row
  [{:keys [id action time-of-status-change state progress return-code status-message] :as job}]
  [ui/TableRow
   [ui/TableCell (format-id id)]
   [ui/TableCell action]
   [ui/TableCell time-of-status-change]
   [ui/TableCell state]
   [ui/TableCell progress]
   [ui/TableCell return-code]
   [ui/TableCell {:style {:white-space "pre"}} status-message]])


(defn jobs-table
  [jobs]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [jobs]
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
        (vec (concat [ui/TableBody]
                     (map job-map-to-row jobs)))]])))


(defn jobs-section
  []
  (let [tr   (subscribe [::i18n-subs/tr])
        jobs (subscribe [::subs/jobs])]
    (fn []
      (let [jobs @jobs]
        [cc/collapsible-segment
         (@tr [:job])
         [jobs-table jobs]]))))


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


(defn service-link-button
  []
  (let [deployment (subscribe [::subs/deployment])]
    (fn []
      (let [link @(subscribe [::subs/url (-> @deployment :module :content :urls first second)])]
        (when link
          [uix/MenuItemWithIcon
           {:name      (general/truncate link 35)
            :icon-name "external"
            :position  "right"
            :on-click  #(dispatch [::main-events/open-link link])}])))))


(defn node-card
  [node-name]
  (let [deployment (subscribe [::subs/deployment])
        [url-name url-pattern] (first (get-in @deployment [:module :content :urls]))
        url        @(subscribe [::subs/url url-pattern])]
    ^{:key node-name}
    [ui/Card
     [ui/CardContent
      [ui/CardHeader [ui/Header node-name]]
      [ui/CardDescription
       (when url
         [:div
          [ui/Icon {:name "external"}]
          [:a {:href url, :target "_blank"} (str url-name ": " url)]])]]]))


(defn summary-section
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [cc/collapsible-segment
     (@tr [:summary])
     (vec (concat [ui/CardGroup {:centered true}]
                  (map node-card (nodes-list))))]))


(defn menu
  []
  (let [deployment (subscribe [::subs/deployment])
        cep        (subscribe [::api-subs/cloud-entry-point])]
    (vec (concat [ui/Menu {:borderless true}]

                 (views-operations/format-operations nil @deployment (:base-uri @cep) nil)

                 [[service-link-button]
                  [refresh-button]]))))


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
        [metadata-section]
        [summary-section]
        [parameters-section]
        [events-section]
        [jobs-section]]])))
