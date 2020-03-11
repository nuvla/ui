(ns sixsq.nuvla.ui.dashboard-detail.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.acl.views :as acl]
    [sixsq.nuvla.ui.dashboard-detail.events :as events]
    [sixsq.nuvla.ui.dashboard-detail.spec :as spec]
    [sixsq.nuvla.ui.dashboard-detail.subs :as subs]
    [sixsq.nuvla.ui.dashboard.subs :as dashboard-subs]
    [sixsq.nuvla.ui.dashboard.utils :as utils]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.history.views :as history-views]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as values]))


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
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        module-content (subscribe [::subs/deployment-module-content])]
    (fn []
      (let [urls (get @module-content :urls [])]
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
         :default-open false
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
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        module-content (subscribe [::subs/deployment-module-content])]
    (fn []
      (let [env-vars (get @module-content :environmental-variables [])]
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
         :default-open false
         :label (str/capitalize (@tr [:env-variables]))]))))


(def event-fields #{:id :content :timestamp :category})


(defn events-table-info
  [events]
  (when-let [start (-> events last :timestamp)]
    (let [dt-fn (partial utils/assoc-delta-time start)]
      (->> events
           (map #(select-keys % event-fields))
           (map dt-fn)))))


(defn event-map-to-row
  [{:keys [id content timestamp category delta-time] :as evt}]
  [ui/TableRow
   [ui/TableCell [values/as-link id :label (general-utils/id->short-uuid id)]]
   [ui/TableCell timestamp]
   [ui/TableCell (general-utils/round-up delta-time)]
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
     :default-open false
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

         [uix/Pagination {:totalPages   total-pages
                          :activePage   @page
                          :onPageChange (ui-callback/callback
                                          :activePage #(dispatch [::events/set-job-page %]))}]]))))


(defn jobs-section
  []
  (let [tr   (subscribe [::i18n-subs/tr])
        jobs (subscribe [::subs/jobs])
        {:keys [resources]} @jobs]
    [uix/Accordion [jobs-table @jobs]
     :label (str/capitalize (@tr [:job]))
     :default-open false
     :count (count resources)]))


(defn log-controller
  [go-live?]
  (let [locale        (subscribe [::i18n-subs/locale])
        services-list (subscribe [::subs/deployment-services-list])
        since         (subscribe [::subs/deployment-log-since])
        service       (subscribe [::subs/deployment-log-service])
        play?         (subscribe [::subs/deployment-log-play?])]
    (when (= (count @services-list) 1)
      (dispatch [::events/set-deployment-log-service (first @services-list)]))
    (fn [go-live?]
      [ui/Menu {:size "small", :attached "top"}

       [ui/MenuItem
        {:disabled (not @service)
         :on-click #(dispatch [::events/set-deployment-log-play? (not @play?)])}
        [ui/Icon {:name (if @play? "pause" "play")}]]

       (when (> (count @services-list) 1)
         [ui/Dropdown
          {:value     @service
           :text      (if @service @service "Select a service")
           :item      true
           :on-change (ui-callback/value #(dispatch [::events/set-deployment-log-service %]))
           :options   (map (fn [service]
                             {:key service, :text service, :value service}) @services-list)}])
       [ui/MenuItem
        [:span
         "Since:  "
         [ui/DatePicker
          {:custom-input     (r/as-element
                               [ui/Input {:transparent true
                                          :style       {:width "17em"}}])
           :locale           @locale
           :date-format      "LLL"
           :show-time-select true
           :timeIntervals    1
           :selected         @since
           :on-change        #(dispatch [::events/set-deployment-log-since %])}]]]

       [ui/MenuMenu {:position "right"}

        [ui/MenuItem
         {:active   @go-live?
          :color    (if @go-live? "green" "black")
          :on-click #(swap! go-live? not)}
         [ui/IconGroup {:size "large"}
          [ui/Icon {:name "bars"}]
          [ui/Icon {:name "chevron circle down", :corner true}]]
         "Go Live"]

        [ui/MenuItem {:on-click #(dispatch [::events/clear-deployment-log])}
         [ui/IconGroup {:size "large"}
          [ui/Icon {:name "bars"}]
          [ui/Icon {:name "trash", :corner true}]]
         "Clear"]]])))


(defn logs-viewer
  []
  (let [deployment-log (subscribe [::subs/deployment-log])
        id             (subscribe [::subs/deployment-log-id])
        play?          (subscribe [::subs/deployment-log-play?])
        go-live?       (r/atom true)
        scroll-info    (r/atom nil)]
    (fn []
      (let [log (:log @deployment-log)]
        [:div
         [log-controller go-live?]
         [:<>
          ^{:key (str "logger" @go-live?)}
          [ui/Segment {:attached    "bottom"
                       :loading     (and (nil? @deployment-log)
                                         @play?)
                       :placeholder true
                       :style       {:padding 0
                                     :z-index 0
                                     :height  300}}

           (if @id
             [ui/CodeMirror (cond-> {:value    (str/join "\n" log)
                                     :scroll   {:x (:left @scroll-info)
                                                :y (if @go-live?
                                                     (.-MAX_VALUE js/Number)
                                                     (:top @scroll-info))}
                                     :onScroll #(reset! scroll-info
                                                        (js->clj %2 :keywordize-keys true))
                                     :options  {:mode     ""
                                                :readOnly true
                                                :theme    "logger"}})]
             [ui/Header {:icon true}
              [ui/Icon {:name "search"}]
              "Get service logs"]
             )]
          [ui/Label (str "line count:")
           [ui/LabelDetail (count log)]]]]))))


(defn logs-viewer-wrapper
  []
  (r/create-class
    {:component-will-unmount #(do
                                (dispatch [::events/delete-deployment-log])
                                (dispatch [::events/set-deployment-log-since (spec/default-since)])
                                (dispatch [::events/set-deployment-log-service nil]))
     :reagent-render         logs-viewer}))


(defn logs-section
  []
  (let [tr            (subscribe [::i18n-subs/tr])
        services-list (subscribe [::subs/deployment-services-list])
        id            (subscribe [::subs/deployment-log-id])]
    [uix/Accordion [logs-viewer-wrapper]
     :label (str/capitalize (@tr [:logs]))
     :default-open false
     :on-open #(when (and (= (count @services-list) 1) (not @id))
                 (dispatch [::events/set-deployment-log-since (spec/default-since)])
                 (dispatch [::events/set-deployment-log-service (first @services-list)]))]))



(defn button-icon
  [label? icon-name on-click]
  (r/as-element
    (if label?
      [ui/Label {:corner   true
                 :size     "small"
                 :on-click on-click}
       [ui/Icon {:name  icon-name
                 :style {:cursor "pointer"}
                 :color "red"}]]
      [ui/Icon {:name     icon-name
                :style    {:cursor "pointer"}
                :color    "red"
                :on-click on-click}])))


(defn StopButton
  [deployment & {:keys [label?, menu-item?], :or {label? false, menu-item? false}}]
  (let [tr        (subscribe [::i18n-subs/tr])
        open?     (r/atom false)
        icon-name "stop"]
    (fn [deployment & {:keys [label?, menu-item?], :or {label? false, menu-item? false}}]
      (let [{:keys [id name description module]} deployment
            content        (str (or name id) (when description " - ") description)
            module-content (str (@tr [:created-from-module]) (or (:name module) (:id module)))
            button-text    (@tr [:stop])]
        [uix/ModalDanger
         {:on-close    (fn [event]
                         (reset! open? false)
                         (.stopPropagation event)
                         (.preventDefault event))
          :on-confirm  #(dispatch [::events/stop-deployment id])
          :open        @open?
          :trigger     (r/as-element
                         (if menu-item?
                           [ui/MenuItem {:on-click #(reset! open? true)}
                            [ui/Icon {:name icon-name}]
                            (@tr [:stop])]
                           [ui/Popup {:content  button-text
                                      :size     "tiny"
                                      :position "top center"
                                      :trigger  (button-icon label? icon-name
                                                             (fn [event]
                                                               (reset! open? true)
                                                               (.stopPropagation event)
                                                               (.preventDefault event)))}]))
          :content     [:<> [:h3 content] [:p module-content]]
          :header      (@tr [:stop-deployment])
          :danger-msg  (@tr [:deployment-stop-warning])
          :button-text button-text}]))))


(defn DeleteButton
  [deployment & {:keys [label?, menu-item?], :or {label? false, menu-item? false}}]
  (let [tr        (subscribe [::i18n-subs/tr])
        open?     (r/atom false)
        icon-name "trash"]
    (fn [deployment & {:keys [label?, menu-item?], :or {label? false, menu-item? false}}]
      (let [{:keys [id name description module]} deployment
            content        (str (or name id) (when description " - ") description)
            module-content (str (@tr [:created-from-module]) (or (:name module) (:id module)))
            button-text    (@tr [:delete])]
        [uix/ModalDanger
         {:on-close    (fn [event]
                         (reset! open? false)
                         (.stopPropagation event)
                         (.preventDefault event))
          :on-confirm  #(dispatch [::events/delete id])
          :open        @open?
          :trigger     (r/as-element
                         (if menu-item?
                           [ui/MenuItem {:on-click #(reset! open? true)}
                            [ui/Icon {:name icon-name}]
                            (@tr [:delete])]
                           [ui/Popup {:content  button-text
                                      :size     "tiny"
                                      :position "top center"
                                      :trigger  (button-icon label? icon-name
                                                             (fn [event]
                                                               (reset! open? true)
                                                               (.stopPropagation event)
                                                               (.preventDefault event)))}]))
          :content     [:<> [:h3 content] [:p module-content]]
          :header      (@tr [:delete-deployment])
          :button-text button-text}]))))


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
    [ui/Card (when clickable?
               {:as       :div
                :link     true
                :on-click (fn [event]
                            (dispatch [::history-events/navigate (utils/detail-href id)])
                            (.preventDefault event))
                })
     [ui/Image {:src      (or module-logo-url "")
                :bordered true
                :style    {:width      "auto"
                           :height     "100px"
                           :padding    "20px"
                           :object-fit "contain"}}]

     (when clickable?
       (cond
         (general-utils/can-operation? "stop" deployment) [StopButton deployment :label? true]
         (general-utils/can-delete? deployment) [DeleteButton deployment :label? true]))

     [ui/CardContent

      [ui/Segment (merge style/basic {:floated "right"})
       [:p {:style {:color "initial"}} state]
       [ui/Loader {:active        (utils/deployment-in-transition? state)
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
       [ui/Button {:color    "green"
                   :icon     "external"
                   :content  primary-url-name
                   :fluid    true
                   :href     @primary-url
                   :on-click (fn [event]
                               (.stopPropagation event))
                   :target   "_blank"
                   :rel      "noreferrer"}])]))


(defn summary
  [deployment]
  [ui/CardGroup {:centered true}
   [DeploymentCard deployment :clickable? false]])


(defn MenuBar
  []
  (let [loading? (subscribe [::subs/loading?])
        {:keys [id] :as deployment} @(subscribe [::subs/deployment])]
    [ui/Menu {:borderless true}
     (when (general-utils/can-delete? deployment)
       [DeleteButton deployment :menu-item? true])
     (when (general-utils/can-operation? "stop" deployment)
       [StopButton deployment :menu-item? true])
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
        read-only?  (subscribe [::subs/is-read-only?])
        acl         (subscribe [::subs/deployment-acl])
        resource-id (str "deployment/" uuid)
        loading?    (subscribe [::subs/loading? resource-id])]

    (refresh resource-id)
    (fn [uuid]
      ^{:key uuid}
      [ui/Segment (merge style/basic {:loading @loading?})
       [ui/Container {:fluid true}
        [uix/PageHeader "dashboard" (str/capitalize (@tr [:dashboard])) :inline true]
        (when @acl
          [acl/AclButton
           {:default-value @acl
            :read-only     @read-only?
            :on-change     #(dispatch [::events/edit resource-id (assoc @deployment :acl %)])}])
        [MenuBar]
        [summary @deployment]
        [urls-section]
        [logs-section]
        [events-section]
        [parameters-section]
        [env-vars-section]
        [jobs-section]]])))
