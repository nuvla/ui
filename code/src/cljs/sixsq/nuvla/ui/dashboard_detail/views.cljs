(ns sixsq.nuvla.ui.dashboard-detail.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.acl.views :as acl]
    [sixsq.nuvla.ui.apps.views-versions :as views-versions]
    [sixsq.nuvla.ui.credentials.components :as creds-comp]
    [sixsq.nuvla.ui.credentials.events :as creds-events]
    [sixsq.nuvla.ui.credentials.subs :as creds-subs]
    [sixsq.nuvla.ui.credentials.utils :as creds-utils]
    [sixsq.nuvla.ui.dashboard-detail.events :as events]
    [sixsq.nuvla.ui.dashboard-detail.spec :as spec]
    [sixsq.nuvla.ui.dashboard-detail.subs :as subs]
    [sixsq.nuvla.ui.dashboard.subs :as dashboard-subs]
    [sixsq.nuvla.ui.dashboard.utils :as utils]
    [sixsq.nuvla.ui.deployment-dialog.events :as deployment-dialog-events]
    [sixsq.nuvla.ui.deployment-dialog.views :as deployment-dialog-views]
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
  (dispatch [::events/reset-db])
  (dispatch [::main-events/action-interval-start
             {:id        refresh-action-id
              :frequency 10000
              :event     [::events/get-deployment resource-id]}]))


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


(defn module-version-section
  []
  (let [module-versions   (subscribe [::subs/module-versions])
        module-content-id (subscribe [::subs/current-module-content-id])
        current-version   (subscribe [::subs/current-module-version])
        module            (subscribe [::subs/deployment-module])]
    (fn []
      [uix/Accordion
       [ui/Segment style/autoscroll-x
        [views-versions/versions-table @module-versions @module-content-id
         :on-click #(dispatch [::history-events/navigate
                               (str "apps/" (:path @module) "?version=" @current-version)])]]
       :default-open false
       :count (str "v" (or @current-version "-"))
       :label "Module versions"])))


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


(defn billing-section
  []
  (let [tr               (subscribe [::i18n-subs/tr])
        upcoming-invoice (subscribe [::subs/upcoming-invoice])]
    (fn []
      (let [locale @(subscribe [::i18n-subs/locale])
            {total    :total
             currency :currency} @upcoming-invoice
            {:keys [description period]} (some-> @upcoming-invoice :lines first)
            coupon (get-in @upcoming-invoice [:discount :coupon])]
        [uix/Accordion
         [:div
          [:b (str/capitalize (@tr [:details])) ": "]
          description

          [:br]

          [:b (str/capitalize (@tr [:period])) ": "]
          (str (some-> period :start (time/time->format "LL" locale))
               " - "
               (some-> period :end (time/time->format "LL" locale)))
          [:br]
          [:b (str/capitalize (@tr [:coupon])) ": "]
          (or (:name coupon) "-")]
         :label (str/capitalize (@tr [:billing]))
         :default-open false
         :count (when total (str (if (= currency "eur") "€" currency)
                                 " " (general-utils/format "%.2f" total)))]))))


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



(defn action-button
  [{:keys [label? menu-item? icon-name button-text on-click disabled? popup-text]
    :or   {disabled? false on-click identity}}]
  (let [button (cond
                 label? [ui/Label {:corner   true
                                   :size     "small"
                                   :on-click on-click}
                         [ui/Icon {:name  icon-name
                                   :style {:cursor "pointer"}
                                   :color "red"}]]
                 menu-item? [ui/MenuItem
                             {:on-click on-click
                              :disabled disabled?}
                             [ui/Icon {:name icon-name}]
                             button-text]
                 :else [ui/Icon {:name     icon-name
                                 :style    {:cursor "pointer"}
                                 :color    "red"
                                 :on-click on-click}])]
    (if popup-text
      [ui/Popup
       (cond-> {:header            (str/capitalize button-text)
                :content           popup-text
                :mouse-enter-delay 500
                :trigger           (r/as-element button)}
               (and (not menu-item?) (not label?)) (assoc :position "bottom center"))]
      button)))


(defn ShutdownButton
  [deployment & {:keys [label?, menu-item?], :or {label? false, menu-item? false}}]
  (let [tr         (subscribe [::i18n-subs/tr])
        credential (subscribe [::creds-subs/credential])
        open?      (r/atom false)
        icon-name  "stop"]
    (fn [deployment & {:keys [label?, menu-item?], :or {label? false, menu-item? false}}]
      (let [{:keys [id name description module parent]} deployment
            credential-loading?     (subscribe [::creds-subs/credential-check-loading? parent])
            credential-invalid?     (subscribe [::creds-subs/credential-check-status-invalid? parent])
            credential-check-status (creds-utils/credential-check-status @credential-loading? @credential-invalid?)
            text1  (str (or name id) (when description " - ") description)
            text2  (str (@tr [:created-from-module]) (or (:name module) (:id module)))
            button (action-button
                     {:label?      label?
                      :menu-item?  menu-item?
                      :on-click    (fn [event]
                                     (reset! open? true)
                                     (dispatch [::events/check-credential parent])
                                     (.stopPropagation event)
                                     (.preventDefault event))
                      :disabled?   (not (general-utils/can-operation? "stop" deployment))
                      :icon-name   icon-name
                      :button-text (@tr [:shutdown])
                      :popup-text  (@tr [:deployment-shutdown-msg])})]
        ^{:key (random-uuid)}
        [uix/ModalDanger
         {:on-close    (fn [event]
                         (reset! open? false)
                         (.stopPropagation event)
                         (.preventDefault event))
          :on-confirm  #(do
                          (dispatch [::events/stop-deployment id])
                          (reset! open? false))
          :open        @open?
          :trigger     (r/as-element button)
          :content     [:<> [:h3 text1] [:p text2]]
          :header      [:span [creds-comp/CredentialCheckPopup parent] " " (@tr [:shutdown-deployment])]
          :danger-msg  (@tr [:deployment-shutdown-msg])
          :button-text (@tr [(if (= :ok credential-check-status) :shutdown :shutdown-force)])}]))))


(defn DeleteButton
  [deployment & {:keys [label?, menu-item?], :or {label? false, menu-item? false}}]
  (let [tr        (subscribe [::i18n-subs/tr])
        open?     (r/atom false)
        icon-name "trash"]
    (fn [deployment & {:keys [label?, menu-item?], :or {label? false, menu-item? false}}]
      (let [{:keys [id name description module]} deployment
            text-1 (str (or name id) (when description " - ") description)
            text-2 (str (@tr [:created-from-module]) (or (:name module) (:id module)))
            button (action-button
                     {:on-click    (fn [event]
                                     (reset! open? true)
                                     (.stopPropagation event)
                                     (.preventDefault event))
                      :button-text (@tr [:delete])
                      :popup-text  (@tr [:deployment-delete-msg])
                      :icon-name   icon-name
                      :label?      label?
                      :menu-item?  menu-item?
                      :disabled?   (not (general-utils/can-delete? deployment))})]
        ^{:key (random-uuid)}
        [uix/ModalDanger
         {:on-close    (fn [event]
                         (reset! open? false)
                         (.stopPropagation event)
                         (.preventDefault event))
          :on-confirm  #(dispatch [::events/delete id])
          :open        @open?
          :trigger     (r/as-element button)
          :content     [:<> [:h3 text-1] [:p text-2]]
          :header      (@tr [:delete-deployment])
          :danger-msg  (@tr [:deployment-delete-msg])
          :button-text (@tr [:delete])}]))))


(defn CloneButton
  [{:keys [id data module] :as deployment}]
  (let [tr         (subscribe [::i18n-subs/tr])
        first-step (if data :data :infra-services)
        button     (action-button
                     {:menu-item?  true
                      :button-text (@tr [:clone])
                      :icon-name   "code branch"
                      :popup-text  (@tr [:deployment-clone-msg])
                      :on-click    #(dispatch [::deployment-dialog-events/create-deployment
                                               id first-step])
                      :disabled?   (nil? module)})]
    [:<>
     [deployment-dialog-views/deploy-modal]
     button]))


(defn StartButton
  [{:keys [data] :as deployment}]
  (let [tr         (subscribe [::i18n-subs/tr])
        first-step (if data :data :infra-services)
        button     (action-button
                     {:button-text (@tr [:restart])
                      :popup-text  (@tr [:deployment-restart-msg])
                      :icon-name   "play"
                      :menu-item?  true
                      :disabled?   (not (general-utils/can-operation? "start" deployment))
                      :on-click    #(dispatch [::deployment-dialog-events/open-deployment-modal first-step
                                               deployment])})]
    [:<>
     [deployment-dialog-views/deploy-modal]
     button]))


(defn FetchModuleButton
  [{:keys [id] :as deployment}]
  (let [tr         (subscribe [::i18n-subs/tr])
        is-latest? (subscribe [::subs/is-latest-module-versions?])]
    (action-button
      {:button-text (@tr [:fetch])
       :popup-text  (@tr [:deployment-fetch-msg])
       :disabled?   (or (not (general-utils/can-operation? "fetch-module" deployment))
                        @is-latest?)
       :icon-name   "history"
       :menu-item?  true
       :on-click    #(dispatch [::events/fetch-module id])})))


(defn DeploymentCard
  [{:keys [id state module tags] :as deployment} & {:keys [clickable?]
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
                            (.preventDefault event))})
     [ui/Image {:src      (or module-logo-url "")
                :bordered true
                :style    {:width      "auto"
                           :height     "100px"
                           :padding    "20px"
                           :object-fit "contain"}}]

     (when clickable?
       (cond
         (general-utils/can-operation? "stop" deployment) [ShutdownButton deployment :label? true]
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
         [:div [ui/Icon {:name "key"}] cred-info])]

      [ui/LabelGroup {:size  "tiny"
                      :color "teal"
                      :style {:margin-top 10, :max-height 150, :overflow "auto"}}
       (for [tag tags]
         ^{:key (str id "-" tag)}
         [ui/Label {:style {:max-width     "15ch"
                            :overflow      "hidden"
                            :text-overflow "ellipsis"
                            :white-space   "nowrap"}}
          [ui/Icon {:name "tag"}] tag
          ])]]

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
  [{:keys [id] :as deployment}]
  (let [loading? (subscribe [::subs/loading?])]
    [main-components/StickyBar
     [ui/Menu {:borderless true}
      [StartButton deployment]
      [ShutdownButton deployment :menu-item? true]
      [FetchModuleButton deployment]
      [CloneButton deployment]
      [DeleteButton deployment :menu-item? true]
      [main-components/RefreshMenu
       {:action-id  refresh-action-id
        :loading?   @loading?
        :on-refresh #(refresh id)}]]]))


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
        [MenuBar @deployment]
        (when @acl
          [acl/AclButton
           {:default-value @acl
            :read-only     @read-only?
            :on-change     #(dispatch [::events/edit resource-id (assoc @deployment :acl %)])}])
        [summary @deployment]
        [urls-section]
        [module-version-section]
        [logs-section]
        [events-section]
        [parameters-section]
        [env-vars-section]
        (when (:subscription-id @deployment)
          [billing-section])
        [jobs-section]]])))
