(ns sixsq.slipstream.webui.deployment-detail.views
  (:require
    [clojure.pprint :refer [cl-format]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as reagent]
    [sixsq.slipstream.webui.cimi.subs :as api-subs]
    [sixsq.slipstream.webui.deployment-detail.events :as events]
    [sixsq.slipstream.webui.deployment-detail.subs :as subs]
    [sixsq.slipstream.webui.deployment-detail.utils :as deployment-detail-utils]
    [sixsq.slipstream.webui.deployment-detail.views-operations :as operations]
    [sixsq.slipstream.webui.history.views :as history]
    [sixsq.slipstream.webui.i18n.subs :as i18n-subs]
    [sixsq.slipstream.webui.main.events :as main-events]
    [sixsq.slipstream.webui.utils.collapsible-card :as cc]
    [sixsq.slipstream.webui.utils.general :as general]
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]
    [sixsq.slipstream.webui.utils.semantic-ui-extensions :as uix]
    [sixsq.slipstream.webui.utils.style :as style]
    [sixsq.slipstream.webui.utils.time :as time]))


(defn ^:export set-runUUID
  [uuid]                                                    ;Used by old UI
  (dispatch [::events/set-runUUID uuid]))


(defn nodes-list                                            ;FIXME
  []
  ["machine"])


(defn automatic-refresh
  [resource-id]
  (dispatch [::main-events/action-interval
             {:action    :start
              :id        :deployment-detail-get-deployment
              :frequency 30000
              :event     [::events/get-deployment resource-id]}])
  (dispatch [::main-events/action-interval
             {:action    :start
              :id        :deployment-detail-get-summary-nodes-parameters
              :frequency 30000
              :event     [::events/get-summary-nodes-parameters resource-id (nodes-list)]}])
  (dispatch [::main-events/action-interval
             {:action    :start
              :id        :deployment-detail-reports
              :frequency 30000
              :event     [::events/get-reports resource-id]}])
  (dispatch [::main-events/action-interval
             {:action    :start
              :id        :deployment-detail-deployment-parameters
              :frequency 20000
              :event     [::events/get-global-deployment-parameters resource-id]}])
  (dispatch [::main-events/action-interval
             {:action    :start
              :id        :deployment-detail-events
              :frequency 30000
              :event     [::events/get-events resource-id]}])
  (dispatch [::main-events/action-interval
             {:action    :start
              :id        :deployment-detail-jobs
              :frequency 30000
              :event     [::events/get-jobs resource-id]}]))


(def deployment-summary-keys #{:created
                               :updated
                               :resourceUri
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
  (let [deployment (subscribe [::subs/deployment])
        deployment-parameters (subscribe [::subs/global-deployment-parameters])]
    (fn []
      (let [summary-info (-> (select-keys @deployment deployment-summary-keys)
                             (merge (select-keys (:module @deployment) #{:name :path :type})
                                    {:owner (-> @deployment :acl :owner :principal)}))
            icon (-> @deployment :module :type deployment-detail-utils/category-icon)
            rows (map tuple-to-row summary-info)
            ss-state (-> @deployment-parameters (get "ss:state" {}) :value)
            state (:state @deployment)]
        [cc/metadata
         (cond-> {:title       (module-name @deployment)
                  :description (:startTime summary-info)
                  :icon        icon}
                 ss-state (assoc :subtitle (visible-state state ss-state)))
         rows]))))


(defn node-parameter-table
  [params]
  [ui/Table style/definition
   (vec (concat [ui/TableBody] (map tuple-to-row params)))])


(defn parameter-to-row
  [{:keys [nodeID name description value] :as param}]
  [ui/Popup
   {:content (reagent/as-element [:p description])
    :trigger (reagent/as-element
               [ui/TableRow
                [ui/TableCell name]
                [ui/TableCell value]])}])

(defn global-parameters-section
  []
  (let [tr (subscribe [::i18n-subs/tr])
        deployment-parameters (subscribe [::subs/global-deployment-parameters])]
    (fn []
      (let [global-params (vals @deployment-parameters)]
        [cc/collapsible-segment (@tr [:global-parameters])
         [ui/Segment style/autoscroll-x
          [ui/Table style/single-line
           [ui/TableHeader
            [ui/TableRow
             [ui/TableHeaderCell [:span (@tr [:name])]]
             [ui/TableHeaderCell [:span (@tr [:value])]]]]
           (when-not (empty? global-params)
             (vec (concat [ui/TableBody]
                          (map parameter-to-row global-params))))]]]))))

(defn node-parameters-section
  []
  (let [tr (subscribe [::i18n-subs/tr])
        node-parameters (subscribe [::subs/node-parameters])]
    (dispatch [::main-events/action-interval
               {:action    :start
                :id        :deployment-detail-get-node-parameters
                :frequency 5000
                :event     [::events/get-node-parameters]}])
    (fn []
      [ui/Table style/single-line
       [ui/TableHeader
        [ui/TableRow
         [ui/TableHeaderCell [:span (@tr [:name])]]
         [ui/TableHeaderCell [:span (@tr [:value])]]]]
       (vec (concat [ui/TableBody]
                    (map parameter-to-row @node-parameters)))])))


(defn report-item
  [{:keys [id component created state] :as report}]
  (let [cep (subscribe [::api-subs/cloud-entry-point])
        {:keys [baseURI]} @cep]
    (when baseURI
      ^{:key id} [:li
                  (let [label (str/join " " [component created])]
                    (if (= state "ready")
                      ;; FIXME: The download URLs should be taken from operations rather than constructed like this.
                      [:a {:style    {:cursor "pointer"}
                           :download true
                           :href     (str baseURI id "/download")} label]
                      label))])))


(def event-fields #{:id :content :timestamp :type})


(defn events-table-info
  [events]
  (when-let [start (-> events last :timestamp)]
    (let [dt-fn (partial deployment-detail-utils/assoc-delta-time start)]
      (->> events
           (map #(select-keys % event-fields))
           (map dt-fn)))))


(defn format-event-id
  [id]
  (let [tag (second (re-matches #"^.*/([^-]+).*$" id))]
    [history/link (str "str/" id) tag]))


(defn format-delta-time
  [delta-time]
  (cl-format nil "~,2F" delta-time))


(defn event-map-to-row
  [{:keys [id content timestamp type delta-time] :as evt}]
  [ui/TableRow
   [ui/TableCell (format-event-id id)]
   [ui/TableCell timestamp]
   [ui/TableCell (format-delta-time delta-time)]
   [ui/TableCell type]
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
          [ui/TableHeaderCell [:span (@tr [:type])]]
          [ui/TableHeaderCell [:span (@tr [:state])]]]]
        (vec (concat [ui/TableBody]
                     (map event-map-to-row events)))]])))


(defn events-section
  []
  (let [tr (subscribe [::i18n-subs/tr])
        events (subscribe [::subs/events])]
    (fn []
      (let [events (events-table-info @events)]
        [cc/collapsible-segment
         (@tr [:events])
         [events-table events]]))))


(defn job-map-to-row
  [{:keys [id timeOfStatusChange state progress returnCode statusMessage] :as job}]
  [ui/TableRow
   [ui/TableCell (format-event-id id)]
   [ui/TableCell timeOfStatusChange]
   [ui/TableCell state]
   [ui/TableCell progress]
   [ui/TableCell returnCode]
   [ui/TableCell statusMessage]])


(defn jobs-table
  [jobs]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [jobs]
      [ui/Segment style/autoscroll-x
       [ui/Table style/single-line
        [ui/TableHeader
         [ui/TableRow
          [ui/TableHeaderCell [:span (@tr [:job])]]
          [ui/TableHeaderCell [:span (@tr [:timestamp])]]
          [ui/TableHeaderCell [:span (@tr [:state])]]
          [ui/TableHeaderCell [:span (@tr [:progress])]]
          [ui/TableHeaderCell [:span (@tr [:return-code])]]
          [ui/TableHeaderCell [:span (@tr [:message])]]]]
        (vec (concat [ui/TableBody]
                     (map job-map-to-row jobs)))]])))


(defn jobs-section
  []
  (let [tr (subscribe [::i18n-subs/tr])
        jobs (subscribe [::subs/jobs])]
    (fn []
      (let [jobs @jobs]
        [cc/collapsible-segment
         (@tr [:job])
         [jobs-table jobs]]))))

(defn reports-list-view
  []
  (let [reports (subscribe [::subs/reports])]
    (if (seq @reports)
      (vec (concat [:ul] (mapv report-item (:externalObjects @reports))))
      [:p "Reports will be displayed as soon as available. No need to refresh."])))


(defn reports-list                                          ; Used by old UI
  []
  (let [runUUID (subscribe [::subs/runUUID])]
    (when-not (str/blank? @runUUID)
      (dispatch [::main-events/action-interval
                 {:action    :start
                  :id        :deployment-detail-reports
                  :frequency 30000
                  :event     [::events/get-reports @runUUID]}]))
    [reports-list-view]))


(defn reports-section
  [href]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn []
      [cc/collapsible-segment
       (@tr [:reports])
       [reports-list-view]])))


(defn refresh-button
  []
  (let [tr (subscribe [::i18n-subs/tr])
        loading? (subscribe [::subs/loading?])
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
  (let [deployment-parameters (subscribe [::subs/global-deployment-parameters])]
    (fn []
      (let [link (-> @deployment-parameters (get "ss:url.service") :value)]
        (when link
          [uix/MenuItemWithIcon
           {:name      (general/truncate link 35)
            :icon-name "external"
            :position  "right"
            :on-click  #(dispatch [::main-events/open-link link])}])))))


(defn node-card
  [node-name]
  (let [summary-nodes-parameters (subscribe [::subs/summary-nodes-parameters])
        node-params (get @summary-nodes-parameters node-name [])
        params-by-name (into {} (map (juxt :name identity) node-params))
        service-url (get-in params-by-name ["url.service" :value])
        custom-state (get-in params-by-name ["statecustom" :value])
        ssh-url (get-in params-by-name ["url.ssh" :value])
        ssh-password (get-in params-by-name ["password.ssh" :value])
        complete (get-in params-by-name ["complete" :value])]
    ^{:key node-name}
    [ui/Card
     [ui/CardContent
      [ui/CardHeader [ui/Header node-name]]
      [ui/CardDescription
       (when service-url
         [:div
          [ui/Icon {:name "external"}]
          [:a {:href service-url, :target "_blank"} service-url]])
       (when ssh-url
         [:div
          [ui/Icon {:name "terminal"}]
          [:a {:href ssh-url} ssh-url]])
       (when ssh-password
         [:div (str "password: " (when ssh-password "••••••"))
          [ui/Popup {:trigger  (reagent/as-element [ui/CopyToClipboard {:text ssh-password}
                                                    [:a [ui/Icon {:name "clipboard outline"}]]])
                     :position "top center"}
           "copy to clipboard"]])
       (when custom-state
         [:div custom-state])
       [:div [:a {:href     "#apache.1"
                  :on-click #(dispatch [::events/show-node-parameters-modal node-name])}
              "details"]]
       (when (= complete "Ready")
         [:div {:align "right"} [ui/Icon {:name "check"}]])]]]))


(defn summary-section
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [cc/collapsible-segment
     (@tr [:summary])
     (vec (concat [ui/CardGroup {:centered true}]
                  (map node-card (nodes-list))))]))


(defn node-parameters-modal
  []
  (let [tr (subscribe [::i18n-subs/tr])
        node-name (subscribe [::subs/node-parameters-modal])
        deployment (subscribe [::subs/deployment])]
    (fn []
      (let [hide-fn #(do (dispatch [::events/close-node-parameters-modal])
                         (automatic-refresh (:id @deployment)))]
        [ui/Modal {:open       (boolean @node-name)
                   :close-icon true
                   :on-close   hide-fn}

         [ui/ModalHeader [ui/Icon {:name "microchip"}] @node-name]

         [ui/ModalContent {:scrolling true}
          [node-parameters-section]]

         [ui/ModalActions
          [uix/Button {:text     (@tr [:close]),
                       :on-click hide-fn}]]]))))


(defn menu
  []
  (let [deployment (subscribe [::subs/deployment])
        cep (subscribe [::api-subs/cloud-entry-point])]
    (vec (concat [ui/Menu {:borderless true}]

                 (operations/format-operations nil @deployment (:baseURI @cep) nil)

                 [[service-link-button]
                  [refresh-button]]))))


(def deployment-states ["Provisioning" "Executing" "SendingReports" "Ready" "Done"])

(def states-map (into {} (map-indexed (fn [i state] [state i]) deployment-states)))

(def steps [["Provisioning" "Provisioning" "Starting systems"]
            ["Executing" "Executing" "Executing recipes"]
            ["Reporting" "SendingReports" "Gathering for posterity"]
            ["Ready" "Ready" "Ready, all systems go"]])


(defn event-get-timestamp
  [event]
  (-> event :timestamp time/parse-iso8601))


(defn step-items
  [locale active-state-index events-map [title state description]]
  (let [event-state (get events-map state)
        state-index (get states-map state)
        is-state-active? (= active-state-index state-index)
        is-state-completed? (> active-state-index state-index)]
    {:key         state
     :title       title
     :description (str description
                       (cond
                         is-state-active? (str ". Running for " (time/delta-humanize
                                                                  (event-get-timestamp event-state)
                                                                  locale))
                         is-state-completed? (str ". Took " (time/delta-humanize
                                                              (event-get-timestamp event-state)
                                                              (some->> (inc state-index)
                                                                       (nth deployment-states)
                                                                       (get events-map)
                                                                       event-get-timestamp)
                                                              locale))))
     :icon        (cond
                    is-state-completed? "check"
                    is-state-active? "rocket"
                    :else "ellipsis horizontal")
     :active      is-state-active?
     :disabled    (< active-state-index state-index)}))


(defn extract-steps
  [events locale]
  (let [state-events (filter #(-> % :type (= "state")) events)
        events-map (into {} (map (juxt (comp :state :content) identity) events))
        active-state-index (or (some->> state-events first :content :state (get states-map)) 0)]
    (map (partial step-items locale active-state-index events-map) steps)))


(defn progression-section
  []
  (let [events (subscribe [::subs/events])
        force-refresh (subscribe [::subs/force-refresh-events-steps])
        locale (subscribe [::i18n-subs/locale])]
    ^{:key @force-refresh}
    [ui/StepGroup {:key    @force-refresh
                   :fluid  true
                   :widths (count steps)
                   :items  (extract-steps @events @locale)}]))


(defn deployment-detail
  [resource-id]
  (let [deployment (subscribe [::subs/deployment])]
    (automatic-refresh resource-id)
    (fn [resource-id]
      ^{:key resource-id}
      [ui/Segment (merge style/basic
                         {:loading (not= resource-id (:id @deployment))})
       [ui/Container {:fluid true}
        [menu]
        [metadata-section]
        [progression-section]
        [summary-section]
        [global-parameters-section]
        [events-section]
        [jobs-section]
        [reports-section resource-id]
        [node-parameters-modal]
        ]])))
