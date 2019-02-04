(ns sixsq.slipstream.webui.nuvlabox-detail.views
  (:require
    [cljs.pprint :refer [cl-format]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.slipstream.webui.cimi-api.utils :as cimi-api-utils]
    [sixsq.slipstream.webui.i18n.subs :as i18n-subs]
    [sixsq.slipstream.webui.nuvlabox-detail.events :as nuvlabox-events]
    [sixsq.slipstream.webui.nuvlabox-detail.subs :as nuvlabox-subs]
    [sixsq.slipstream.webui.nuvlabox.utils :as u]
    [sixsq.slipstream.webui.plot.plot :as plot]
    [sixsq.slipstream.webui.utils.collapsible-card :as cc]
    [sixsq.slipstream.webui.utils.resource-details :as details]
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]
    [sixsq.slipstream.webui.utils.semantic-ui-extensions :as uix]
    [sixsq.slipstream.webui.utils.time :as time]))


(defn controls-detail
  []
  (let [tr (subscribe [::i18n-subs/tr])
        loading? (subscribe [::nuvlabox-subs/loading?])]
    (fn []
      [ui/Menu
       [uix/MenuItemWithIcon
        {:name      (@tr [:refresh])
         :icon-name "refresh"
         :loading?  @loading?
         :on-click  #(dispatch [::nuvlabox-events/fetch-detail])}]])))


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
  (let [updated-moment (time/parse-iso8601 updated)
        next-check-moment (time/parse-iso8601 next-check)

        check-ok? (time/after-now? next-check)
        icon (if check-ok? "heartbeat" "warning sign")

        msg-last (str "Last heartbeat was " (time/ago updated-moment) " (" updated ").")
        msg-next (if check-ok?
                   (str "Next heartbeat is expected " (time/ago next-check-moment) " (" next-check ").")
                   (str "Next heartbeat was expected " (time/ago next-check-moment) " (" next-check ")."))]

    [cc/collapsible-segment
     [:span [ui/Icon {:name icon}] " heartbeat"]
     [:div
      [:div msg-last]
      [:div msg-next]]]))


(defn select-metadata
  [data]
  (let [metadata-keys #{:id :resourceURI :name :description :created :updated}]
    (select-keys data metadata-keys)))


(defn metadata-row
  [[k v]]
  ^{:key k}
  [ui/TableRow [ui/TableCell {:collapsing true} k] [ui/TableCell v]])


(defn nb-metadata
  []
  (let [record (subscribe [::nuvlabox-subs/record])]
    (fn []
      (let [{:keys [id name description updated acl] :as data} @record
            rows (->> data
                      select-metadata
                      (map metadata-row)
                      vec)]
        [cc/metadata {:title       (or name id)
                      :subtitle    (-> (or id "unknown/unknown")
                                       (str/split #"/")
                                       second)
                      :description description
                      :icon        "computer"
                      :updated     updated
                      :acl         acl}
         rows]))))


(defn state-table
  []
  (let [detail (subscribe [::nuvlabox-subs/state])]
    (fn []
      (when @detail
        (let [{:keys [cpu ram disks usb updated nextCheck]} @detail]
          [ui/Container {:fluid true}
           [heartbeat updated nextCheck]
           [load cpu ram disks]
           [usb-devices usb]])))))


(defn record-info
  []
  (let [record (subscribe [::nuvlabox-subs/record])]
    (when @record
      (details/group-table-sui (cimi-api-utils/remove-common-attrs @record) nil))))


(defn nb-detail
  []
  (let [detail (subscribe [::nuvlabox-subs/state])
        record (subscribe [::nuvlabox-subs/record])]
    (fn []
      (when (or (nil? @detail) (nil? @record))
        (dispatch [::nuvlabox-events/fetch-detail]))
      [ui/Container {:fluid true}
       [controls-detail]
       [nb-metadata]
       [state-table]
       [record-info]])))
