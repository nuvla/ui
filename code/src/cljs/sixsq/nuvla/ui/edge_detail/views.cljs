(ns sixsq.nuvla.ui.edge-detail.views
  (:require
    [cljs.pprint :refer [cl-format]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.acl.views :as acl]
    [sixsq.nuvla.ui.edge-detail.events :as events]
    [sixsq.nuvla.ui.edge-detail.subs :as subs]
    [sixsq.nuvla.ui.edge.subs :as edge-subs]
    [sixsq.nuvla.ui.edge.utils :as u]
    [sixsq.nuvla.ui.edge.utils :as utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.utils.map :as map]
    [sixsq.nuvla.ui.utils.plot :as plot]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.style :as style]))


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


(defn Peripheral
  [{p-name        :name
    p-product     :product
    p-created     :created
    p-updated     :updated
    p-descr       :description
    p-interface   :interface
    p-device-path :device-path
    p-available   :available
    p-vendor      :vendor
    p-classes     :classes
    p-indentifier :identifier}]
  (let [locale (subscribe [::i18n-subs/locale])]
    [uix/Accordion
     [ui/Table {:basic "very"}
      [ui/TableBody
       (when p-product
         [ui/TableRow
          [ui/TableCell "Name"]
          [ui/TableCell (str p-name " " p-product)]])
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
          [ui/TableCell "Device path"]
          [ui/TableCell p-device-path]])
       [ui/TableRow
        [ui/TableCell "Identifier"]
        [ui/TableCell p-indentifier]]
       [ui/TableRow
        [ui/TableCell "Vendor"]
        [ui/TableCell p-vendor]]
       [ui/TableRow
        [ui/TableCell "Created"]
        [ui/TableCell (time/ago (time/parse-iso8601 p-created) @locale)]]
       [ui/TableRow
        [ui/TableCell "Updated"]
        [ui/TableCell (time/ago (time/parse-iso8601 p-updated) @locale)]]]
      ]
     :label (or p-name p-product)
     :title-size :h4
     :default-open false
     :icon (case p-interface
             "USB" "usb"
             nil)]))


(defn Peripherals
  []
  (let [nuvlabox-peripherals (subscribe [::subs/nuvlabox-peripherals])]
    [uix/Accordion
     [:div
      (doall
        (for [{p-indentifier :identifier
               p-created     :created
               :as           peripheral} @nuvlabox-peripherals]
          ^{:key (str p-indentifier p-created)}
          [Peripheral peripheral]))]
     :label "Peripherals"
     :icon "usb"
     :count (count @nuvlabox-peripherals)]))


(defn LocationAccordion
  [{:keys [id location] :as nuvlabox}]
  (let [tr           (subscribe [::i18n-subs/tr])
        zoom         (atom 3)
        new-location (r/atom nil)]
    (fn [{:keys [id location] :as nuvlabox}]
      (let [update-new-location #(reset! new-location %)
            position            (some-> (or @new-location location) map/longlat->latlong)]

        [uix/Accordion
         [ui/Segment style/basic
          (if position (@tr [:map-drag-to-update-nb-location])
                       (@tr [:map-click-to-set-nb-location]))
          [map/Map
           {:style             {:height        400
                                :width         "100%"
                                :margin-bottom 10
                                :cursor        (when-not location "pointer")}
            :center            (or position map/sixsq-latlng)
            :zoom              @zoom
            :onViewportChanged #(reset! zoom (.-zoom %))
            :on-click          (when-not position
                                 (map/click-location update-new-location))}
           [map/DefaultLayers]

           (when position
             [map/Marker {:position    position
                          :draggable   true
                          :on-drag-end (map/drag-end-location update-new-location)}])]
          [:div
           [ui/Button {:primary  true
                       :floated  "right"
                       :on-click #(dispatch
                                    [::events/edit id
                                     (assoc nuvlabox
                                       :location
                                       (update @new-location 0 map/normalize-lng))
                                     "NuvlaBox position updated successfully"])}
            (@tr [:save])]
           [ui/Button {:floated  "right"
                       :on-click #(reset! new-location nil)}
            (@tr [:cancel])]]]
         :default-open false
         :label "Location"
         :icon "map"]))))


(defn StatusIcon
  [status]
  [ui/Popup
   {:position "right center"
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
   (let [load-stats (u/load-statistics resources)]
     [plot/HorizontalBar {:height  50
                          :data    {:labels   (map :label load-stats)
                                    :datasets [{:data (map :percentage load-stats)}]}
                          :options {:scales {:xAxes [{:type  "linear"
                                                      :ticks {:beginAtZero true
                                                              :max         100}}]
                                             :yAxes [{:gridLines {:display false}}]}}}])
   :label "Load Percentages"
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


(defn NuvlaboxCard
  [nuvlabox status & {on-click-fn :on-click}]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [{:keys [id name description created state] :as nuvlabox} status]
      ^{:key id}
      [ui/Card (when on-click-fn {:on-click on-click-fn})
       [ui/CardContent

        [ui/CardHeader {:style {:word-wrap "break-word"}}
         [:div {:style {:float "right"}}
          [StatusIcon status :corner "top right"]]
         [ui/Icon {:name "box"}] (or name id)]

        [ui/CardMeta (str (@tr [:created]) " " (-> created time/parse-iso8601 time/ago))]

        [:p {:align "right"} state]

        (when-not (str/blank? description)
          [ui/CardDescription {:style {:overflow "hidden" :max-height "100px"}} description])]])))


(defn SummarySection
  []
  (let [{:keys [id] :as nuvlabox} @(subscribe [::subs/nuvlabox])
        status @(subscribe [::edge-subs/status-nuvlabox id])]
    [:<>
     [ui/CardGroup {:centered true}
      [NuvlaboxCard nuvlabox status]]
     [LocationAccordion nuvlabox]]))


(defn EdgeDetails
  [uuid]
  (let [nuvlabox  (subscribe [::subs/nuvlabox])
        can-edit? (subscribe [::subs/can-edit?])
        acl-open  (r/atom false)]
    (refresh uuid)
    (fn [uuid]
      ^{:key uuid}
      [ui/Container {:fluid true}
       [MenuBar uuid]
       (when (:acl @nuvlabox)
         ^{:key (:updated @nuvlabox)}
         [acl/AclButton
          {:default-value   (:acl @nuvlabox)
           :read-only       (not @can-edit?)
           :default-active? @acl-open
           :on-change       #(do
                               (reset! acl-open true)
                               (dispatch [::events/edit (:id @nuvlabox) (assoc @nuvlabox :acl %)
                                          "NuvlaBox ACL updated successfully"]))}])
       [SummarySection]
       [StatusSection]])))
