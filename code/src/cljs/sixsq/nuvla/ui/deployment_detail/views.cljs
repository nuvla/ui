(ns sixsq.nuvla.ui.deployment-detail.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.acl.views :as acl]
    [sixsq.nuvla.ui.apps-store.events :as apps-store-events]
    [sixsq.nuvla.ui.apps.views-versions :as views-versions]
    [sixsq.nuvla.ui.credentials.components :as creds-comp]
    [sixsq.nuvla.ui.credentials.subs :as creds-subs]
    [sixsq.nuvla.ui.credentials.utils :as creds-utils]
    [sixsq.nuvla.ui.deployment-detail.events :as events]
    [sixsq.nuvla.ui.deployment-detail.spec :as spec]
    [sixsq.nuvla.ui.deployment-detail.subs :as subs]
    [sixsq.nuvla.ui.deployment-dialog.events :as deployment-dialog-events]
    [sixsq.nuvla.ui.deployment-dialog.views :as deployment-dialog-views]
    [sixsq.nuvla.ui.deployment.subs :as deployment-subs]
    [sixsq.nuvla.ui.deployment.utils :as utils]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.history.views :as history-views]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.job.subs :as job-subs]
    [sixsq.nuvla.ui.job.views :as job-views]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.spec :as spec-utils]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as values]))


(def refresh-action-id :deployment-get-deployment)


(defn refresh
  [resource-id]
  (dispatch [::events/reset-db])
  (dispatch [::main-events/action-interval-start
             {:id        refresh-action-id
              :frequency 10000
              :event     [::events/get-deployment resource-id]}]))


(defn url-to-row
  [url-name url-pattern]
  (let [tr  (subscribe [::i18n-subs/tr])
        url (subscribe [::subs/url url-pattern])]
    [ui/TableRow
     [ui/TableCell url-name]
     [ui/TableCell {:class ["show-on-hover-value"]}
      (if @url
        (values/copy-value-to-clipboard
          [:a {:href @url, :target "_blank"} @url false]
          @url
          (@tr [:copy-to-clipboard]))
        url-pattern)]]))


(defn url-to-button
  ([url-name url-pattern] (url-to-button url-name url-pattern false))
  ([url-name url-pattern primary?]
   (let [url (subscribe [::subs/url url-pattern])]
     (when @url
       [ui/Button {:color    (if primary? "green" nil)
                   :icon     "external"
                   :content  url-name
                   :href     @url
                   :on-click (fn [event]
                               (js/window.open @url)
                               (.stopPropagation event)
                               (.preventDefault event))
                   :target   "_blank"
                   :rel      "noreferrer"
                   :style {:margin 2}}]))))


(defn urls-section
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        module-content (subscribe [::subs/deployment-module-content])
        urls           (get @module-content :urls)
        url-count      (count urls)]
    {:menuItem {:content (r/as-element [:span (@tr [:url])
                                        (when (> url-count 0)
                                          [ui/Label {:circular true
                                                     :size     "mini"
                                                     :attached "top right"}
                                           url-count])])
                :key     "urls"
                :icon    "linkify"}
     :render   (fn []
                 (r/as-element
                   (if (empty? urls)
                     [uix/WarningMsgNoElements (@tr [:no-urls])]
                     [ui/TabPane
                      [ui/Table {:basic   "very"
                                 :columns 2}
                       [ui/TableHeader
                        [ui/TableRow
                         [ui/TableHeaderCell [:span (@tr [:name])]]
                         [ui/TableHeaderCell [:span (@tr [:url])]]]]
                       [ui/TableBody
                        (for [[url-name url-pattern] urls]
                          ^{:key url-name}
                          [url-to-row url-name url-pattern])]]])))}))


(defn module-version-section
  []
  (let [tr                (subscribe [::i18n-subs/tr])
        module-versions   (subscribe [::subs/module-versions])
        module-content-id (subscribe [::subs/current-module-content-id])]
    {:menuItem {:content (r/as-element [:span (@tr [:module-version])])
                :key     "versions"
                :icon    "linkify"}
     :render   (fn [] (r/as-element [ui/TabPane
                                     [views-versions/versions-table @module-versions @module-content-id]]))}))


(defn item-to-row
  [{name :name value :value description :description}]
  (let [tr        (subscribe [::i18n-subs/tr])
        table-row [ui/TableRow
                   [ui/TableCell
                    (if (some? description)
                      [ui/Popup
                       (cond-> {:content (r/as-element [:p description])
                                :trigger (r/as-element [:p name " " [ui/Icon {:name "info circle"}]])})]
                      name)]
                   [ui/TableCell
                    {:class ["show-on-hover-value"]}
                    (when (not-empty value)
                      (if (> (count value) 1)
                        (values/copy-value-to-clipboard value value (@tr [:copy-to-clipboard]) false)
                        value))]]]
    table-row))


(defn list-section
  [items section-key section-name]
  (let [tr          (subscribe [::i18n-subs/tr])
        items-count (count items)]
    {:menuItem {:content (r/as-element
                           [:span (@tr [section-name])
                            (when (> items-count 0)
                              [ui/Label {:circular true
                                         :size     "mini"
                                         :attached "top right"}
                               items-count])])
                :key     section-key
                :icon    "list ol"}
     :render   (fn []
                 (r/as-element
                   [:<>
                    (if (empty? items)
                      [uix/WarningMsgNoElements]
                      [ui/TabPane
                       [ui/Table {:basic   "very"
                                  :columns 2}
                        [ui/TableHeader
                         [ui/TableRow
                          [ui/TableHeaderCell [:span (@tr [:name])]]
                          [ui/TableHeaderCell [:span (@tr [:value])]]]]
                        (when-not (empty? items)
                          [ui/TableBody
                           (for [{name :name :as item} items]
                             ^{:key name}
                             [item-to-row item])])]])]))}))


(defn parameters-section
  []
  (let [deployment-parameters (subscribe [::subs/deployment-parameters])
        params                (vals @deployment-parameters)]
    (list-section params "parameters-section" :module-output-parameters)))


(defn env-vars-section
  []
  (let [module-content (subscribe [::subs/deployment-module-content])
        env-vars       (get @module-content :environmental-variables [])]
    (list-section env-vars "env-vars" :env-variables)))


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
      ;style/autoscroll-x
      [ui/TabPane
       [ui/Table {:basic "very"}
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


(defn events-section                                        ;FIXME: add paging
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        events      (subscribe [::subs/events])
        events-info (events-table-info @events)
        event-count (count events-info)]
    {:menuItem {:content (r/as-element [:span (str/capitalize (@tr [:events]))
                                        (when (> event-count 0) [ui/Label {:circular true
                                                                           :size     "mini"
                                                                           :attached "top right"}
                                                                 event-count])])
                :key     "events"
                :icon    "bolt"}
     :render   (fn [] (r/as-element [events-table events-info]))}))


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


(defn billing-section
  []
  (let [tr               (subscribe [::i18n-subs/tr])
        upcoming-invoice (subscribe [::subs/upcoming-invoice])
        deployment       (subscribe [::subs/deployment])
        locale           (subscribe [::i18n-subs/locale])
        {total    :total
         currency :currency} @upcoming-invoice              ;{:total 100.45 :currency "eur"}
        {:keys [description period]} (some-> @upcoming-invoice :lines first)
        coupon           (get-in @upcoming-invoice [:discount :coupon])]
    (when (some? (:subscription-id @deployment))
      {:menuItem {:content (r/as-element [:span (str/capitalize (@tr [:billing]))])
                  :key     "billing"
                  :icon    "eur"}
       :render   (fn []
                   (r/as-element
                     [ui/Segment
                      [ui/Table {:collapsing true
                                 :basic      "very"
                                 :padded     false}
                       [ui/TableBody
                        [ui/TableRow
                         [ui/TableCell
                          [:b (str/capitalize (@tr [:details])) ": "]]
                         [ui/TableCell
                          description]]
                        [ui/TableRow
                         [ui/TableCell
                          [:b (str/capitalize (@tr [:period])) ": "]]
                         [ui/TableCell
                          (some-> period :start (time/time->format "LL" @locale))
                          " - "
                          (some-> period :end (time/time->format "LL" @locale))]]
                        [ui/TableRow
                         [ui/TableCell
                          [:b (str/capitalize (@tr [:coupon])) ": "]]
                         [ui/TableCell
                          (or (:name coupon) "-")]]
                        [ui/TableRow
                         [ui/TableCell
                          [:b (str/capitalize (@tr [:total])) ": "]]
                         [ui/TableCell
                          (or total "-") " " currency]]]]]))})))


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
  (let [tr (subscribe [::i18n-subs/tr])]
    {:menuItem {:content (r/as-element [:span (str/capitalize (@tr [:logs]))])
                :key     "logs"
                :icon    "file code"}
     :render   (fn [] (r/as-element [logs-viewer-wrapper]))}))



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
  (let [tr        (subscribe [::i18n-subs/tr])
        open?     (r/atom false)
        checked?  (r/atom false)
        icon-name "stop"]
    (fn [deployment & {:keys [label?, menu-item?], :or {label? false, menu-item? false}}]
      (let [{:keys [id name description module parent]} deployment
            cred-loading?     (subscribe [::creds-subs/credential-check-loading? parent])
            cred-invalid?     (subscribe [::creds-subs/credential-check-status-invalid? parent])
            cred-check-status (creds-utils/credential-check-status @cred-loading? @cred-invalid?)
            text1             (str (or name id) (when description " - ") description)
            text2             (str (@tr [:created-from-module]) (or (:name module) (:id module)))
            button            (action-button
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
         {:on-close           (fn [event]
                                (reset! open? false)
                                (.stopPropagation event)
                                (.preventDefault event))
          :on-confirm         #(do
                                 (dispatch [::events/stop-deployment id])
                                 (reset! open? false))
          :open               @open?
          :control-confirmed? checked?
          :trigger            (r/as-element button)
          :content            [:<> [:h3 text1] [:p text2]]
          :header             (@tr [:shutdown-deployment])
          :danger-msg         (@tr [:deployment-shutdown-msg])
          :button-text        (@tr [(cond
                                      (= "pull" (:execution-mode deployment)) :schedule-shutdown
                                      (= :ok cred-check-status) :shutdown
                                      :else :shutdown-force)])
          :modal-action       [creds-comp/CredentialCheckPopup parent]}]))))


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


(defn StartUpdateButton
  [{:keys [data state] :as deployment}]
  (let [tr         (subscribe [::i18n-subs/tr])
        start      (#{"CREATED" "STOPPED"} state)
        first-step (if data :data :infra-services)
        button     (action-button
                     {:button-text (if start
                                     (@tr [:start])
                                     (@tr [:update]))
                      :popup-text  (@tr [(if start :deployment-start-msg
                                                   :deployment-update-msg)])
                      :icon-name   (if start "play" "redo")
                      :menu-item?  true
                      :disabled?   (if start
                                     (not (general-utils/can-operation? "start" deployment))
                                     (not (general-utils/can-operation? "update" deployment)))
                      :on-click    #(dispatch [::deployment-dialog-events/open-deployment-modal
                                               first-step deployment])})]
    [:<>
     [deployment-dialog-views/deploy-modal]
     button]))


(defn vpn-info
  []
  (let [{:keys [state module]} @(subscribe [::subs/deployment])
        {module-content :content} module
        [_ url] (-> module-content (get :urls []) first)
        tr          (subscribe [::i18n-subs/tr])
        primary-url (subscribe [::subs/url url])
        parameters  (subscribe [::subs/deployment-parameters])
        started?    (utils/is-started? state)
        hostname    (or (get-in @parameters ["hostname" :value]) "")]
    (when (and started? @primary-url (spec-utils/private-ipv4? hostname))
      [ui/Message {:info true}
       [ui/MessageHeader (@tr [:vpn-information])]
       [ui/MessageContent
        (@tr [:deployment-run-private-ip]) ". "
        [:br]
        (@tr [:deployment-access-url]) " "
        [:a {:on-click #(dispatch [::history-events/navigate "credentials"]) :href "#"}
         (@tr [:create-vpn-credential])] " " (@tr [:and]) " "
        [:a {:href "https://docs.nuvla.io/nuvla/vpn" :target "_blank"} (@tr [:connect-vpn])] "."]])))


(defn up-to-date?
  [v versions]
  (when v
    (let [tr           (subscribe [::i18n-subs/tr])
          last-version (ffirst versions)]
      (if (= v last-version)
        [:span [ui/Icon {:name "check", :color "green"}] " (" (@tr [:up-to-date]) ")"]
        [:span [ui/Icon {:name "warning", :color "orange"}]
         (str (@tr [:behind-version-1]) " " (- last-version v) " " (@tr [:behind-version-2]))]))))


(defn TabOverviewModule
  []
  (let [tr         (subscribe [::i18n-subs/tr])
        deployment (subscribe [::subs/deployment])
        locale     (subscribe [::i18n-subs/locale])
        module     (:module @deployment)
        id         (:id module "")
        {:keys [created updated name acl description parent-path path logo-url]} module]
    [ui/Segment {:secondary true
                 :color     "blue"
                 :raised    true}
     [:h4 "Module"]
     [ui/Table {:basic  "very"
                :padded false}
      [ui/TableBody
       (when name
         [ui/TableRow
          [ui/TableCell (str/capitalize (@tr [:name]))]
          [ui/TableCell [values/as-link path :label name :page "apps"]]])
       (when description
         [ui/TableRow
          [ui/TableCell (str/capitalize (@tr [:description]))]
          [ui/TableCell description]])
       (when parent-path
         [ui/TableRow
          [ui/TableCell (str/capitalize (@tr [:project]))]
          [ui/TableCell [values/as-link parent-path :label parent-path :page "apps"]]])
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:created]))]
        [ui/TableCell (time/ago (time/parse-iso8601 created) @locale)]]
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:updated]))]
        [ui/TableCell (time/ago (time/parse-iso8601 updated) @locale)]]
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:id]))]
        [ui/TableCell [values/as-link id :label (subs id 11)]]]]]]))


(defn DeploymentCard
  [{:keys [id state module tags] :as deployment} & {:keys [clickable?]
                                                    :or   {clickable? true}}]
  (let [tr            (subscribe [::i18n-subs/tr])
        creds-name    (subscribe [::deployment-subs/creds-name-map])
        credential-id (:parent deployment)
        {module-logo-url :logo-url
         module-name     :name
         module-path     :path
         module-content  :content} module
        cred-info     (get @creds-name credential-id credential-id)
        [primary-url-name
         primary-url-pattern] (-> module-content (get :urls []) first)
        primary-url   (if clickable?
                        (subscribe [::deployment-subs/deployment-url id primary-url-pattern])
                        (subscribe [::subs/url primary-url-pattern]))
        started?      (utils/is-started? state)]

    ^{:key id}
    [ui/Card (when clickable?
               {:as       :div
                :link     true
                :on-click (fn [event]
                            (dispatch [::history-events/navigate (utils/deployment-href id)])
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
          [ui/Icon {:name "tag"}] tag])]]

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

(defn TabOverviewSummary
  []
  (let [tr            (subscribe [::i18n-subs/tr])
        deployment    (subscribe [::subs/deployment])
        version       (subscribe [::subs/current-module-version])
        versions      (subscribe [::subs/module-versions])
        module        (:module @deployment)
        {:keys [logo-url]} module
        {:keys [id state module tags acl]} @deployment
        owners        (:owners acl)
        creds-name    (subscribe [::deployment-subs/creds-name-map])
        credential-id (:parent @deployment)
        {module-content :content} module
        cred-info     (get @creds-name credential-id credential-id)
        urls          (:urls module-content)]

    [ui/SegmentGroup {:style  {:display    "flex", :justify-content "space-between",
                               :background "#f3f4f5"}
                      :raised true}
     [ui/Segment {:secondary true
                  :color     "green"
                  :raised    true}
      [ui/Segment (merge style/basic {:floated "right"})
       [ui/Image {:src      (or logo-url "")
                  :bordered true
                  :style    {:width      "auto"
                             :height     "100px"
                             :padding    "20px"
                             :object-fit "contain"}}]]

      [:h4 {:style {:margin-top 0}} (@tr [:summary])]

      [ui/Table {:basic "very" :style {:display "inline", :floated "left"}}
       [ui/TableBody
        (when tags
          [ui/TableRow
           [ui/TableCell (str/capitalize (@tr [:tags]))]
           [ui/TableCell
            [ui/LabelGroup {:size  "tiny"
                            :color "teal"
                            :style {:margin-top 10, :max-height 150, :overflow "auto"}}
             (for [tag tags]
               ^{:key (str id "-" tag)}
               [ui/Label {:style {:max-width     "15ch"
                                  :overflow      "hidden"
                                  :text-overflow "ellipsis"
                                  :white-space   "nowrap"
                                  :margin        "20px"}}
                [ui/Icon {:name "tag"}] tag
                ])]]])
        [ui/TableRow
         [ui/TableCell (str/capitalize (str (@tr [:created])))]
         [ui/TableCell (-> @deployment :created time/parse-iso8601 time/ago)]]
        [ui/TableRow
         [ui/TableCell "Id"]
         [ui/TableCell (when (some? id) (subs id 11))]]
        (when (not-empty owners)
          [ui/TableRow
           [ui/TableCell (str/capitalize (@tr [:owner]))]
           [ui/TableCell (str/join ", " owners)]])
        [ui/TableRow
         [ui/TableCell (str/capitalize (@tr [:status]))]
         [ui/TableCell state
          " "
          (when (utils/deployment-in-transition? state)
            [ui/Icon {:loading true :name "circle notch" :color "grey"}])]]
        [ui/TableRow
         [ui/TableCell (str/capitalize (@tr [:credential]))]
         [ui/TableCell
          (when-not (str/blank? cred-info)
            [:div [ui/Icon {:name "key"}] cred-info])]]
        [ui/TableRow
         [ui/TableCell (str/capitalize (@tr [:version-number]))]
         [ui/TableCell @version " " (up-to-date? @version @versions)]]]]]
     [ui/Segment {:attached  false
                  :secondary true}
      (for [[i [url-name url-pattern]] (map-indexed list urls)]
        ^{:key url-name}
        [url-to-button url-name url-pattern (= i 0)])]]))


(defn sum-replicas
  [parameters ends-with]
  (->> (vals parameters)
       (filter #(str/ends-with? (:name %) ends-with))
       (map #(js/parseInt (:value %)))
       (reduce +)))


(defn sum-running-replicas
  [parameters]
  (sum-replicas parameters "replicas.running"))


(defn sum-desired-replicas
  [parameters]
  (sum-replicas parameters "replicas.desired"))


(defn ProgressJobDeployment
  []
  (let [parameters (subscribe [::subs/deployment-parameters])
        running    (sum-running-replicas @parameters)
        desired    (sum-desired-replicas @parameters)]
    (when (and running desired (not= running desired))
      [ui/Progress {:label      "deployment: started"
                    :total      desired
                    :value      running
                    :progress   "ratio"
                    :size       "small"
                    :class      ["green"]}])))


(defn overview-pane
  []
  (let []
    [:<>
     [job-views/ProgressJobAction]
     [ProgressJobDeployment]
     [ui/TabPane
      [ui/Grid {:columns   2,
                :stackable true
                :padded    true}
       [ui/GridRow
        [ui/GridColumn {:stretched true}
         [TabOverviewSummary]]
        [ui/GridColumn {:stretched true}
         [TabOverviewModule]]]]]]))


(defn overview
  []
  {:menuItem {:content (r/as-element [:span "Overview"])
              :key     "overview"
              :icon    "info"}
   :render   (fn [] (r/as-element [overview-pane]))})


(defn MenuBar
  [{:keys [id] :as deployment}]
  (let [loading? (subscribe [::subs/loading?])]
    [main-components/StickyBar
     [ui/Menu {:borderless true}
      [StartUpdateButton deployment]
      [ShutdownButton deployment :menu-item? true]
      [CloneButton deployment]
      [DeleteButton deployment :menu-item? true]
      [main-components/RefreshMenu
       {:action-id  refresh-action-id
        :loading?   @loading?
        :on-refresh #(refresh id)}]]]))


(defn deployment-detail-panes
  []
  (let [deployment (subscribe [::subs/deployment])
        read-only? (subscribe [::subs/is-read-only?])]
    [(overview)
     (urls-section)
     (module-version-section)
     (logs-section)
     (events-section)
     (parameters-section)
     (env-vars-section)
     (billing-section)
     (job-views/jobs-section)
     (acl/TabAcls deployment (not @read-only?) ::events/edit)]))


(defn depl-state->status
  [state]
  (case (if (some? state) (str/lower-case state) "")
    "started" :online
    :offline))


(defn StatusIcon
  [status & {:keys [corner] :or {corner "bottom center"} :as position}]
  [ui/Popup
   {:position corner
    :content  status
    :trigger  (r/as-element
                [ui/Icon {:name  "power"
                          :color (values/status->color status)}])}])


(defn PageHeader
  []
  (let [tr         (subscribe [::i18n-subs/tr])
        deployment (subscribe [::subs/deployment])]
    (fn []
      (let [module-name (get-in @deployment [:module :name] "")
            state       (:state @deployment)
            uuid        (:id @deployment "")]
        [:div
         [:h2 {:style {:margin "0 0 0 0"}}
          [StatusIcon (depl-state->status state)]
          module-name " (" (general-utils/truncate (subs uuid 11)) ")"]
         [:p {:style {:margin "0.5em 0 1em 0"}}
          [:span {:style {:font-weight "bold"}}
           "State "
           [ui/Popup
            {:trigger        (r/as-element [ui/Icon {:name "question circle"}])
             :content        (@tr [:deployment-state])
             :position       "bottom center"
             :on             "hover"
             :size           "tiny"
             :hide-on-scroll true}] ": "]
          state]]))))


(defn TabsDeployment
  [uuid]
  (let [deployment  (subscribe [::subs/deployment])
        resource-id (str "deployment/" uuid)]
    (refresh resource-id)
    (fn [uuid]
      (let [active-index (subscribe [::subs/active-tab-index])]
        [:<>
         [PageHeader]
         [MenuBar @deployment]
         [main-components/ErrorJobsMessage ::subs/deployment ::job-subs/jobs ::events/set-active-tab-index 8]
         [vpn-info]
         [ui/Tab
          {:menu        {:secondary true
                         :pointing  true
                         :style     {:display        "flex"
                                     :flex-direction "row"
                                     :flex-wrap      "wrap"}}
           :panes       (deployment-detail-panes)
           :activeIndex @active-index
           :onTabChange (fn [_ data]
                          (let [active-index (. data -activeIndex)]
                            (dispatch [::events/set-active-tab-index active-index])))}]]))))


(defmethod panel/render :deployment
  [path]
  (let [[_ uuid] path
        n (count path)]
    (case n
      2 [TabsDeployment uuid]
      (do
        (dispatch [::apps-store-events/set-active-tab-index 2])
        (dispatch [::history-events/navigate (str "apps")])))))
