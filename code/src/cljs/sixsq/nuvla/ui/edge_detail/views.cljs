(ns sixsq.nuvla.ui.edge-detail.views
  (:require
    [cljs.pprint :refer [cl-format]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.edge-detail.events :as events]
    [sixsq.nuvla.ui.edge-detail.subs :as subs]
    [sixsq.nuvla.ui.edge.utils :as u]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.plot.plot :as plot]
    [sixsq.nuvla.ui.utils.collapsible-card :as cc]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.resource-details :as details]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.utils.style :as style]
    [taoensso.timbre :as log]))


(defn refresh-button
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        loading? (subscribe [::subs/loading?])
        nuvlabox (subscribe [::subs/nuvlabox])]
    (fn []
      [uix/MenuItemWithIcon
       {:name      (@tr [:refresh])
        :icon-name "refresh"
        :loading?  @loading?
        :position  "right"
        :on-click  #(dispatch [::events/get-nuvlabox (:id @nuvlabox)])}])))


(defn menu-bar []
  [ui/Segment style/basic
   [ui/Menu {:attached   "top"
             :borderless true}
    [refresh-button]]])


(defn hw-id
  [{:keys [bus-id device-id] :as device}]
  (str bus-id "." device-id))



(defn Load
  [resources]
  [uix/Accordion
   (let [load-stats (u/load-statistics resources)]
     [ui/Container
      [plot/HorizontalBar {:width 600
                           :height 200
                           :data    {:labels   (map :label load-stats)
                                     :datasets [{:data (map :percentage load-stats)}]}
                           :options {:scales {:xAxes [{:type  "linear"
                                                       :ticks {:beginAtZero true
                                                               :max         100}}]
                                              :yAxes [{:gridLines {:display false}}]}}}]])
   :label "Load Percentages"
   :icon "thermometer half"])

(defn UsbDeviceRow
  [{:keys [bus-id device-id vendor-id product-id busy description] :as device}]
  ^{:key (hw-id device)}
  [ui/TableRow
   [ui/TableCell {:collapsing true} (if busy "busy" "free")]
   [ui/TableCell {:collapsing true} bus-id]
   [ui/TableCell {:collapsing true} device-id]
   [ui/TableCell {:collapsing true} vendor-id]
   [ui/TableCell {:collapsing true} product-id]
   [ui/TableCell description]])


(defn Peripherals
  [{usb-list :usb}]
  [uix/Accordion
   [ui/Table
    [ui/TableHeader
     [ui/TableRow
      [ui/TableHeaderCell "busy"]
      [ui/TableHeaderCell "bus"]
      [ui/TableHeaderCell "device"]
      [ui/TableHeaderCell "vendor"]
      [ui/TableHeaderCell "product"]
      [ui/TableHeaderCell "description"]]]
    [ui/TableBody
     (for [usb (sort-by hw-id usb-list)]
       ^{:key (hw-id usb)}
       [UsbDeviceRow usb])]]
   :label "Peripherals"
   :icon "usb"])


(defn Heartbeat
  [updated next-heartbeat]
  (let [updated-moment    (time/parse-iso8601 updated)
        next-check-moment (time/parse-iso8601 next-heartbeat)

        check-ok?         (time/after-now? next-heartbeat)
        icon              (if check-ok? "heartbeat" "warning sign")

        msg-last          (str "Last heartbeat was " (time/ago updated-moment) " (" updated ").")
        msg-next          (if check-ok?
                            (str "Next heartbeat is expected " (time/ago next-check-moment) " (" next-heartbeat ").")
                            (str "Next heartbeat was expected " (time/ago next-check-moment) " (" next-heartbeat ")."))]

    [uix/Accordion
     [ui/Container
      [:p msg-last]
      [:p msg-next]]
     :label "Heartbeat"
     :icon "heartbeat"]))


(defn select-metadata
  [data]
  (let [metadata-keys #{:id :resource-url :name :description :created :updated}]
    (select-keys data metadata-keys)))


(defn metadata-row
  [[k v]]
  ^{:key k}
  [ui/TableRow [ui/TableCell {:collapsing true} k] [ui/TableCell v]])



(defn NuvlaboxCard
  [nuvlabox]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [{:keys [id name description created] :as nuvlabox}]
      ^{:key id}
      [ui/Card
       [ui/CardContent
        [ui/Segment (merge style/basic {:floated "right"})
         [:p "stateeee"]]

        [ui/CardHeader [:span [:p {:style {:overflow      "hidden",
                                           :text-overflow "ellipsis",
                                           :max-width     "20ch"}} (or name id)]]]

        [ui/CardMeta (str (@tr [:created]) " " (-> created time/parse-iso8601 time/ago))]

        [ui/CardDescription (when-not (str/blank? description)
                              [:div [ui/Icon {:name "key"}] description])]]])))


(defn summary
  []
  (let [nuvlabox (subscribe [::subs/nuvlabox])]
    (fn []
      (let [{:keys [id name description updated acl] :as data} @nuvlabox
            rows (->> data
                      select-metadata
                      (map metadata-row)
                      vec)]
        [ui/CardGroup {:centered true}
         [NuvlaboxCard @nuvlabox]]))))


(defn StatusSection
  []
  (let [nuvlabox-status (subscribe [::subs/nuvlabox-status])]
    (fn []
      (when @nuvlabox-status
        (let [{:keys [resources peripherals updated next-heartbeat]} @nuvlabox-status]
          [ui/Container {:fluid true}
           [Heartbeat updated next-heartbeat]
           (when resources
             [Load resources])
           [Peripherals peripherals]])))))


(defn record-info
  []
  (let [record (subscribe [::subs/record])]
    (when @record
      (details/group-table-sui (general-utils/remove-common-attrs @record) nil))))



(defn nuvlabox-detail
  [uuid]
  (let []
    (dispatch [::main-events/action-interval
               {:action    :start
                :id        :nuvlabox-get-nuvlabox
                :frequency 30000
                :event     [::events/get-nuvlabox (str "nuvlabox/" uuid)]}])
    (fn [uuid]
      ^{:key uuid}
      [ui/Container {:fluid true}
       [menu-bar]
       [summary]
       [StatusSection]])))
