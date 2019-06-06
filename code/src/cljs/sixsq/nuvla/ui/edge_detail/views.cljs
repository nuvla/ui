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
    [sixsq.nuvla.ui.utils.style :as style]))


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


(defn device-row-header
  []
  [ui/TableHeader
   [ui/TableRow
    [ui/TableHeaderCell "busy"]
    [ui/TableHeaderCell "bus"]
    [ui/TableHeaderCell "device"]
    [ui/TableHeaderCell "vendor"]
    [ui/TableHeaderCell "product"]
    [ui/TableHeaderCell "description"]]])


(defn device-row
  [{:keys [bus-id device-id vendor-id product-id busy description] :as device}]
  ^{:key (hw-id device)}
  [ui/TableRow
   [ui/TableCell {:collapsing true} (if busy "busy" "free")]
   [ui/TableCell {:collapsing true} bus-id]
   [ui/TableCell {:collapsing true} device-id]
   [ui/TableCell {:collapsing true} vendor-id]
   [ui/TableCell {:collapsing true} product-id]
   [ui/TableCell description]])


(defn load-chartjs
  [_ load]
  (let [load-stats (u/load-statistics load)]
    [ui/CardGroup {:doubling true, :items-per-row 2}
     [ui/Card
      [ui/CardContent
       [ui/CardHeader "Load Percentages"]
       [plot/HorizontalBar {:data    {:labels   (mapv :label load-stats)
                                      :datasets [{:data (mapv :percentage load-stats)}]}
                            :options {:scales {:xAxes [{:type  "linear"
                                                        :ticks {:beginAtZero true
                                                                :max         100}}]
                                               :yAxes [{:gridLines {:display false}}]}}}]]]]))


(defn load
  [cpu ram disks]
  [cc/collapsible-segment

   [:span [ui/Icon {:name "thermometer half"}] " load"]
   [load-chartjs {} {:cpu cpu :ram ram :disks disks}]])


(defn usb-devices
  [usb]
  [cc/collapsible-segment
   [:span [ui/Icon {:name "usb"}] " usb devices"]
   [ui/Table
    [device-row-header]
    (vec (concat [ui/TableBody] (mapv device-row (sort-by hw-id usb))))]])


(defn heartbeat
  [updated next-check]
  (let [updated-moment    (time/parse-iso8601 updated)
        next-check-moment (time/parse-iso8601 next-check)

        check-ok?         (time/after-now? next-check)
        icon              (if check-ok? "heartbeat" "warning sign")

        msg-last          (str "Last heartbeat was " (time/ago updated-moment) " (" updated ").")
        msg-next          (if check-ok?
                            (str "Next heartbeat is expected " (time/ago next-check-moment) " (" next-check ").")
                            (str "Next heartbeat was expected " (time/ago next-check-moment) " (" next-check ")."))]

    [cc/collapsible-segment
     [:span [ui/Icon {:name icon}] " heartbeat"]
     [:div
      [:div msg-last]
      [:div msg-next]]]))


(defn select-metadata
  [data]
  (let [metadata-keys #{:id :resource-url :name :description :created :updated}]
    (select-keys data metadata-keys)))


(defn metadata-row
  [[k v]]
  ^{:key k}
  [ui/TableRow [ui/TableCell {:collapsing true} k] [ui/TableCell v]])


(defn summary
  []
  (let [nuvlabox (subscribe [::subs/nuvlabox])]
    (fn []
      (let [{:keys [id name description updated acl] :as data} @nuvlabox
            rows (->> data
                      select-metadata
                      (map metadata-row)
                      vec)]
        [cc/metadata {:title       (or name id)
                      :subtitle    (-> (or id "unknown/unknown")
                                       (str/split #"/")
                                       second)
                      :description description
                      :icon        "box"
                      :updated     updated
                      :acl         acl}
         rows]))))


(defn status-table
  []
  (let [nuvlabox-status (subscribe [::subs/nuvlabox-status])]
    (fn []
      (when @nuvlabox-status
        (let [{:keys [cpu ram disks usb updated next-check]} @nuvlabox-status]
          [ui/Container {:fluid true}
           [heartbeat updated next-check]
           [load cpu ram disks]
           [usb-devices usb]])))))


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
      #_(when (or (nil? @detail) (nil? @record))
          (dispatch [::events/fetch-detail]))
      ^{:key uuid}
      [ui/Container {:fluid true}
       [menu-bar]
       [summary]
       [status-table]
       #_[state-table]
       #_[record-info]])))
