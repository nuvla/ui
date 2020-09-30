(ns sixsq.nuvla.ui.edge-detail.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.acl.views :as acl]
    [sixsq.nuvla.ui.config :as config]
    [sixsq.nuvla.ui.edge-detail.events :as events]
    [sixsq.nuvla.ui.edge-detail.subs :as subs]
    [sixsq.nuvla.ui.edge.subs :as edge-subs]
    [sixsq.nuvla.ui.edge.utils :as u]
    [sixsq.nuvla.ui.edge.utils :as utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.utils.forms :as forms]
    [sixsq.nuvla.ui.utils.map :as map]
    [sixsq.nuvla.ui.utils.plot :as plot]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(def refresh-action-id :nuvlabox-get-nuvlabox)


(defn refresh
  [uuid]
  (dispatch [::main-events/action-interval-start
             {:id        refresh-action-id
              :frequency 10000
              :event     [::events/get-nuvlabox (str "nuvlabox/" uuid)]}]))



(defn DecommissionButton
  [nuvlabox]
  (let [tr      (subscribe [::i18n-subs/tr])
        {:keys [id name description]} nuvlabox
        content (str (or name id) (when description " - ") description)]
    [uix/ModalDanger
     {:button-text (@tr [:decommission])
      :on-confirm  #(dispatch [::events/decommission])
      :danger-msg  (@tr [:nuvlabox-decommission-warning])
      :trigger     (r/as-element [ui/MenuItem
                                  [ui/Icon {:name "eraser"}]
                                  (@tr [:decommission])])
      :header      (@tr [:decommission-nuvlabox])
      :content     [:h3 content]}]))


(defn DeleteButton
  [nuvlabox]
  (let [tr      (subscribe [::i18n-subs/tr])
        {:keys [id name description]} nuvlabox
        content (str (or name id) (when description " - ") description)]
    [uix/ModalDanger
     {:button-text (@tr [:delete])
      :on-confirm  #(dispatch [::events/delete])
      :trigger     (r/as-element [ui/MenuItem
                                  [ui/Icon {:name "trash"}]
                                  (@tr [:delete])])
      :header      (@tr [:delete-nuvlabox])
      :content     [:h3 content]}]))


(defn MenuBar [uuid]
  (let [can-decommission? (subscribe [::subs/can-decommission?])
        can-delete?       (subscribe [::subs/can-delete?])
        nuvlabox          (subscribe [::subs/nuvlabox])
        loading?          (subscribe [::subs/loading?])]
    [ui/Menu {:borderless true, :stackable true}
     (when @can-decommission?
       [DecommissionButton @nuvlabox])
     (when @can-delete?
       [DeleteButton @nuvlabox])

     [main-components/RefreshMenu
      {:action-id  refresh-action-id
       :loading?   @loading?
       :on-refresh #(refresh uuid)}]]))


(defn get-available-actions
  [operations]
  (filter some? (map #(nth (str/split % #"/") 2 nil) (map :href operations))))


(defn Peripheral
  [id]
  (let [locale       (subscribe [::i18n-subs/locale])
        last-updated (r/atom "1970-01-01T00:00:00Z")
        button-load? (r/atom false)
        peripheral   (subscribe [::subs/nuvlabox-peripheral id])]
    (fn [id]
      (let [{p-id                :id
             p-ops               :operations
             p-name              :name
             p-product           :product
             p-created           :created
             p-updated           :updated
             p-descr             :description
             p-interface         :interface
             p-device-path       :device-path
             p-available         :available
             p-vendor            :vendor
             p-classes           :classes
             p-identifier        :identifier
             p-serial-num        :serial-number
             p-video-dev         :video-device
             p-data-gw-url       :local-data-gateway-endpoint
             p-data-sample       :raw-data-sample
             p-additional-assets :additional-assets
             p-resources         :resources} @peripheral
            actions (get-available-actions p-ops)]

        (when (pos? (compare p-updated @last-updated))
          (reset! button-load? false)
          (reset! last-updated p-updated))
        [uix/Accordion
         [ui/Table {:basic  "very"
                    :padded false}
          [ui/TableBody
           (when p-product
             [ui/TableRow
              [ui/TableCell "Name"]
              [ui/TableCell (str p-name " " p-product)]])
           (when p-serial-num
             [ui/TableRow
              [ui/TableCell "Serial Number"]
              [ui/TableCell p-serial-num]])
           (when p-descr
             [ui/TableRow
              [ui/TableCell "Description"]
              [ui/TableCell p-descr]])
           [ui/TableRow
            [ui/TableCell "Classes"]
            [ui/TableCell (str/join ", " p-classes)]]
           [ui/TableRow
            [ui/TableCell "Available"]
            [ui/TableCell
             [ui/Icon {:name "circle", :color (if p-available "green" "red")}]
             (if p-available "Yes" "No")]]
           (when p-interface
             [ui/TableRow
              [ui/TableCell "Interface"]
              [ui/TableCell p-interface]])
           (when p-device-path
             [ui/TableRow
              [ui/TableCell "Device Path"]
              [ui/TableCell p-device-path]])
           (when p-video-dev
             [ui/TableRow
              [ui/TableCell "Video Device"]
              [ui/TableCell p-video-dev]])
           [ui/TableRow
            [ui/TableCell "Identifier"]
            [ui/TableCell p-identifier]]
           [ui/TableRow
            [ui/TableCell "Vendor"]
            [ui/TableCell p-vendor]]
           [ui/TableRow
            [ui/TableCell "Created"]
            [ui/TableCell (time/ago (time/parse-iso8601 p-created) @locale)]]
           [ui/TableRow
            [ui/TableCell "Updated"]
            [ui/TableCell (time/ago (time/parse-iso8601 p-updated) @locale)]]
           (when p-resources
             [ui/TableRow
              [ui/TableCell "Resources"]
              [ui/TableCell [ui/Grid {:columns   3,
                                      :stackable true
                                      :divided   "vertically"}
                             (for [resource p-resources]
                               [ui/GridRow
                                [ui/GridColumn
                                 [:div [:span {:style {:font-weight "bold"}}
                                        "Unit: "]
                                  (:unit resource)]]
                                [ui/GridColumn
                                 [:span [:span {:style {:font-weight "bold"}}
                                         "Capacity: "]
                                  (:capacity resource)]]
                                (when (:load resource)
                                  [ui/GridColumn
                                   [:span [:span {:style {:font-weight "bold"}}
                                           "Load: "]
                                    (:load resource) "%"]])])]]])
           (when p-additional-assets
             [ui/TableRow
              [ui/TableCell "Additional Assets"]
              [ui/TableCell (map (fn [[key value]]
                                   [ui/Segment {:vertical true}
                                    [:div {:style {:font-weight "bold" :font-variant "small-caps"}}
                                     (str (name key) ": ")]
                                    (map (fn [val]
                                           [:div val]) value)]) p-additional-assets)]])
           (when p-data-gw-url
             [ui/TableRow {:positive true}
              [ui/TableCell "Data Gateway Connection"]
              [ui/TableCell p-data-gw-url]])
           (when p-data-sample
             [ui/TableRow {:positive true}
              [ui/TableCell "Raw Data Sample"]
              [ui/TableCell p-data-sample]])]]
         :label [:span (or p-name p-product)
                 (when (pos? (count actions))
                   [ui/Popup
                    {:position "left center"
                     :content  "Click to start/stop routing this peripheral's data through the Data Gateway"
                     :header   "data-gateway"
                     :wide     "very"
                     :size     "small"
                     :trigger  (r/as-element
                                 [ui/Button
                                  {:on-click (fn [event]
                                               (reset! button-load? true)
                                               (dispatch
                                                 [::events/custom-action p-id (first actions)
                                                  (str "Triggered " (first actions) " for " p-id)])
                                               (.stopPropagation event))
                                   :style    {:margin "-.6em"}
                                   :color    "vk"
                                   :floated  "right"
                                   :circular true
                                   :disabled @button-load?
                                   :loading  @button-load?}
                                  (first actions)])}])]
         :title-size :h4
         :default-open false
         :icon (case p-interface
                 "USB" "usb"
                 nil)]))))


(defn Peripherals
  []
  (let [ids (subscribe [::subs/nuvlabox-peripherals-ids])]
    (fn []
      [uix/Accordion
       [:div
        (doall
          (for [id @ids]
            ^{:key id}
            [Peripheral id]))]
       :label "Peripherals"
       :icon "usb"
       :count (count @ids)])))


(defn LocationAccordion
  [{:keys [id location] :as nuvlabox}]
  (let [tr           (subscribe [::i18n-subs/tr])
        zoom         (atom 3)
        new-location (r/atom nil)]
    (fn [{:keys [id location] :as nuvlabox}]
      (let [update-new-location #(reset! new-location %)
            position            (some-> (or @new-location location) map/longlat->latlong)]

        [uix/Accordion
         [:div
          (if position (@tr [:map-drag-to-update-nb-location])
                       (@tr [:map-click-to-set-nb-location]))
          [map/MapBox
           {:style             {:height 400
                                :cursor (when-not location "pointer")}
            :center            (or position map/sixsq-latlng)
            :zoom              @zoom
            :onViewportChanged #(reset! zoom (.-zoom %))
            :on-click          (when-not position
                                 (map/click-location update-new-location))}
           (when position
             [map/Marker {:position    position
                          :draggable   true
                          :on-drag-end (map/drag-end-location update-new-location)}])]
          [:div {:align "right"}
           [ui/Button {:on-click #(reset! new-location nil)}
            (@tr [:cancel])]
           [ui/Button {:primary  true
                       :on-click #(dispatch
                                    [::events/edit id
                                     (assoc nuvlabox
                                       :location
                                       (update @new-location 0 map/normalize-lng))
                                     "NuvlaBox position updated successfully"])}
            (@tr [:save])]
           ]]
         :default-open false
         :label "Location"
         :icon "map"]))))


(defn StatusIcon
  [status]
  [ui/Popup
   {:position "bottom center"
    :content  status
    :trigger  (r/as-element
                [ui/Icon {:name  "power"
                          :color (utils/status->color status)}])}])


(defn Heartbeat
  [updated]
  (let [updated-moment           (time/parse-iso8601 updated)
        {:keys [id]} @(subscribe [::subs/nuvlabox])
        status                   (subscribe [::edge-subs/status-nuvlabox id])
        next-heartbeat-moment    (subscribe [::subs/next-heartbeat-moment])
        next-heartbeat-times-ago (time/ago @next-heartbeat-moment)

        last-heartbeat-msg       (str "Last heartbeat was " (time/ago updated-moment))
        next-heartbeat-msg       (if (= @status :online)
                                   (str "Next heartbeat is expected " next-heartbeat-times-ago)
                                   (str "Next heartbeat was expected " next-heartbeat-times-ago))]

    [uix/Accordion
     [:<>
      [:p last-heartbeat-msg]
      [:p next-heartbeat-msg]]
     :label "Heartbeat"
     :icon "heartbeat"]))


(defn Load
  [resources]
  [uix/Accordion
   (let [load-stats      (u/load-statistics resources)
         net-stats       (u/load-net-stats (:net-stats resources))
         number-of-stats (count load-stats)]
     ; TODO: if the number-of-stats grows if should split into a new row
     [ui/Grid {:columns   number-of-stats,
               :stackable true
               :divided   true
               :celled    "internally"}
      [ui/GridRow
       (for [stat load-stats]
         ^{:key (:title stat)}
         [ui/GridColumn
          [:div
           [plot/Doughnut {:height  250
                           :data    {:labels   (:label stat)
                                     :datasets [{:data            [(:percentage stat), (:value stat)]
                                                 :backgroundColor ["rgb(230, 99, 100)",
                                                                   "rgba(155, 99, 132, 0.1)",
                                                                   "rgb(230, 99, 100)"]
                                                 :borderColor     ["rgba(230, 99, 100,1)"]
                                                 :borderWidth     3}]}
                           :options {:legend              {:display true
                                                           :labels  {:fontColor "grey"}}
                                     :title               {:display  true
                                                           :text     (:title stat)
                                                           :position "bottom"}
                                     :maintainAspectRatio false
                                     :circumference       4.14
                                     :rotation            -3.64
                                     :cutoutPercentage    60}}]]

          (when (pos? (count (:data-gateway stat)))
            [ui/Container {:key        (:topic stat)
                           :text-align :center}
             [ui/LabelGroup {:key  (:topic stat)
                             :size "tiny"}
              [ui/Label {:color "blue"
                         :basic true
                         :image true}
               "Topic: "
               [ui/LabelDetail
                (first (:data-gateway stat))]]
              [ui/Label {:color "blue"
                         :basic true
                         :image true}
               "Raw sample: "
               [ui/LabelDetail
                (last (:data-gateway stat))]]]]
            )


          ; TODO: the data-gateway stats should be in a popup instead of raw text. But fails some unknown reason,
          ;[ui/Popup
          ; {:trigger        (r/as-element [ui/Icon {:name "info circle"}])
          ;  :content        "Let your NuvlaBox apps subscribe to the internal MQTT to access these values locally"
          ;  :header         "data-gateway"
          ;  :position       "right center"
          ;  :inverted       true
          ;  :wide           true
          ;
          ;  :on             "hover"
          ;  :hide-on-scroll true}]
          ])]
      (when (pos? (count (:label net-stats)))
        [ui/GridRow {:centered true
                     :columns  2}
         [ui/GridColumn
          [:div
           [plot/Bar {:height  200
                      :data    {:labels   (:label net-stats)
                                :datasets [{:label           "Received",
                                            :data            (:rx net-stats)
                                            :backgroundColor "rgb(182, 219, 238)"
                                            :borderColor     "white"
                                            :borderWidth     1}
                                           {:label           "Transmitted",
                                            :data            (:tx net-stats)
                                            :backgroundColor "rgb(230, 99, 100)"
                                            :borderColor     "white"
                                            :borderWidth     1}]}
                      :options {:legend {:display true
                                         :labels  {:fontColor "grey"}}
                                :title  {:display  true
                                         :text     (:title net-stats)
                                         :position "bottom"}
                                :scales {:yAxes [{:type       "logarithmic"
                                                  :scaleLabel {:labelString "bytes"
                                                               :display     true}}]}}}]]]])])

   :label [:span "Resource Consumption "
           [ui/Popup
            {:trigger        (r/as-element [ui/Icon {:name "info circle"}])
             :content        "Let your NuvlaBox apps subscribe to the internal MQTT topics
                                          to access these values locally"
             :header         "data-gateway"
             :position       "right center"
             :inverted       true
             :wide           true
             :on             "hover"
             :hide-on-scroll true}]]
   :icon "thermometer half"])


(defn StatusSection
  []
  (let [nuvlabox-status (subscribe [::subs/nuvlabox-status])]
    (fn []
      (if @nuvlabox-status
        (let [{:keys [resources updated next-heartbeat]} @nuvlabox-status]
          [ui/Container {:fluid true}
           [Heartbeat updated next-heartbeat]
           (when resources
             [Load resources])
           [Peripherals]])
        [ui/Message
         {:warning true
          :content "NuvlaBox status not available."}]))))


(defn ActionsMenu
  "This creates a floating (top right) label with a pinned popup menu
  with a list of available actions for the corresponding resource.

  The list of actions must be passed as an argument, as a list of elements, in the following form:
  [
    {:content \"link text\" :on-click #() :style {}}
  ]"
  [action-list]
  [ui/Label {:circular true
             :floating true
             :basic    true}
   [ui/Popup {:position "right center"
              :on       "click"
              :style    {:padding "5px"}
              :size     "small"
              :pinned   true
              :trigger  (r/as-element [ui/Button
                                       {:icon   true
                                        :style  {:margin  "0"
                                                 :padding "0"
                                                 :border  "0px"
                                                 :background  "none"}}
                                       [ui/Icon {:name   "ellipsis vertical"
                                                 :link  true}]])
              :content  (r/as-element [ui/ListSA {:vertical-align  "middle"
                                                  :link        true
                                                  :selection   true
                                                  :divided     true}
                                       (for [action action-list]
                                         [ui/ListItem {:as "a"
                                                       :key (str "action." (apply str
                                                                             (take 12
                                                                               (repeatedly #(char (+ (rand 26) 65))))))}
                                          [ui/ListContent
                                           [ui/ListDescription
                                            [:span {:on-click  (:on-click action)
                                                    :style     (:style action)}
                                             (:content action)]]]])])}]])


(defn EditAction
  [uuid body close-fn]
  (dispatch [::events/edit
             uuid body
             "NuvlaBox updated successfully"])
  (close-fn))


(defn NuvlaboxCard
  [nuvlabox status & {on-click-fn :on-click}]
  (let [tr        (subscribe [::i18n-subs/tr])
        edit      (r/atom false)
        nb-name   (r/atom (:name nuvlabox))
        close-fn  #(do
                     (reset! edit false)
                     (refresh (:id nuvlabox)))]
    (fn [{:keys [id name description created state tags] :as nuvlabox} status]
      ^{:key id}
      [ui/Card (when on-click-fn {:on-click on-click-fn})
       [ui/CardContent

        [ActionsMenu
         [{:content "Edit" :on-click #(reset! edit true)}]]

        [ui/CardHeader {:style {:word-wrap "break-word"}}
         [:div {:style {:float "right"}}
          [StatusIcon status :corner "top right"]]
         [ui/Icon {:name "box"}]
         ; we could use just the Input with {:disabled (not @edit) :tranparent (not @edit)}, and get rid of this "if"
         ; but for some reason, the :placeholder and :default-value properties of the input
         ; do not get updated after an edit
         (if @edit
           [ui/Input {:default-value  (or name id)
                      :on-key-press   (partial forms/on-return-key #(if (= @nb-name (or name id))
                                                                      (close-fn)
                                                                      (EditAction id {:name @nb-name} close-fn)))
                      :on-change      (ui-callback/input-callback #(reset! nb-name %))
                      :focus          true
                      :size           "mini"}]
           (or name id))]


        [ui/CardMeta (str (@tr [:created]) " " (-> created time/parse-iso8601 time/ago))]

        [:p {:align "right"} state]

        (when-not (str/blank? description)
          [ui/CardDescription {:style {:overflow "hidden" :max-height "100px"}} description])

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
            ])]]])))


(defn SummarySection
  []
  (let [nuvlabox  (subscribe [::subs/nuvlabox])
        nb-status (subscribe [::subs/nuvlabox-status])
        can-edit? (subscribe [::subs/can-edit?])
        ssh-creds (subscribe [::subs/nuvlabox-ssh-keys])
        acl-open  (r/atom false)
        tr        (subscribe [::i18n-subs/tr])]

    (fn []
      (let [status       @(subscribe [::edge-subs/status-nuvlabox (:id @nuvlabox)])
            {:keys [hostname ip docker-server-version
                    operating-system architecture last-boot]} @nb-status

            card-options {:color "black"
                          :style {:max-width 200}}]
        (when (not= (count (:ssh-keys @nuvlabox)) (count (:associated-ssh-keys @ssh-creds)))
          (dispatch [::events/get-nuvlabox-ssh-keys (:ssh-keys @nuvlabox)]))
        [:<>
         (when (:acl @nuvlabox)
           ^{:key (:updated @nuvlabox)}
           [acl/AclButton
            {:default-value   (:acl @nuvlabox)
             :read-only       (not @can-edit?)
             :default-active? @acl-open
             :on-change       #(do
                                 (reset! acl-open true)
                                 (dispatch [::events/edit
                                            (:id @nuvlabox) (assoc @nuvlabox :acl %)
                                            "NuvlaBox ACL updated successfully"]))}])
         [ui/Grid {:columns 3, :stackable true}
          [ui/GridRow
           [ui/GridColumn
            (when last-boot
              [ui/Label {:color    "grey"
                         :basic    true
                         :circular true}
               (str "Last boot: " (time/time->format last-boot))])

            (when (pos? (count (:associated-ssh-keys @ssh-creds)))
              [ui/Segment {:compact true}
               [ui/Popup
                {:hoverable true
                 :flowing   true
                 :position  "bottom center"
                 :content   (r/as-element [ui/ListSA {:divided true
                                                      :relaxed true}
                                           (for [sshkey (:associated-ssh-keys @ssh-creds)]
                                             [ui/ListItem {:key (:id sshkey)}
                                              [ui/ListContent
                                               [ui/ListHeader
                                                [:a {:href   (str @config/path-prefix
                                                                  "/api/" (:id sshkey))
                                                     :target "_blank"}
                                                 (or (:name sshkey) (:id sshkey))]]
                                               [ui/ListDescription
                                                (str (subs (:public-key sshkey) 0 55) " ...")]]])])
                 :trigger   (r/as-element [ui/Icon {:name   "key"
                                                    :fitted true}
                                           (@tr [:nuvlabox-detail-ssh-enabled])
                                           [ui/Icon {:name   "angle down"
                                                     :fitted true}]])}]
               ])]
           [ui/GridColumn
            [ui/CardGroup {:centered true}
             (when @nuvlabox
               [NuvlaboxCard @nuvlabox status])]]]]

         (when (->> [hostname ip docker-server-version
                     operating-system architecture last-boot]
                    (remove #(or (nil? %) (str/blank? %)))
                    seq)
           [ui/Segment
            [ui/CardGroup {:centered true}
             (when operating-system
               [ui/Card card-options
                [ui/CardContent
                 [ui/CardHeader "Operating System"]
                 [ui/CardMeta operating-system]]])
             (when hostname
               [ui/Card card-options
                [ui/CardContent
                 [ui/CardHeader "Hostname"]
                 [ui/CardMeta hostname]]])
             (when architecture
               [ui/Card card-options
                [ui/CardContent
                 [ui/CardHeader "Architecture"]
                 [ui/CardMeta architecture]]])
             (when ip
               [ui/Card card-options
                [ui/CardContent
                 [ui/CardHeader "IP"]
                 [ui/CardMeta ip]]])
             (when docker-server-version
               [ui/Card card-options
                [ui/CardContent
                 [ui/CardHeader "Docker Version"]
                 [ui/CardMeta docker-server-version]]])]])

         [LocationAccordion @nuvlabox]]))))


(defn EdgeDetails
  [uuid]
  (refresh uuid)
  (fn [uuid]
    ^{:key uuid}
    [ui/Container {:fluid true}
     [MenuBar uuid]
     [SummarySection]
     [StatusSection]]))
