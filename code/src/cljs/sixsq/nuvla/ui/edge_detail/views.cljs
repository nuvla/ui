(ns sixsq.nuvla.ui.edge-detail.views
  (:require
    [cljs.pprint :refer [cl-format]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.edge-detail.events :as events]
    [sixsq.nuvla.ui.edge-detail.subs :as subs]
    [sixsq.nuvla.ui.edge.utils :as u]
    [sixsq.nuvla.ui.edge.utils :as utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.plot.plot :as plot]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.time :as time]
    [taoensso.timbre :as log]))


(defn RefreshButton
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


(defn DecommissionButton
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        modal-open? (r/atom false)
        on-close    #(reset! modal-open? false)]
    (fn []
      [ui/Modal
       {:trigger    (r/as-element
                      [uix/MenuItemWithIcon
                       {:name      "Decommission"
                        :icon-name "eraser"
                        :on-click  #(reset! modal-open? true)}])
        :close-icon true
        :on-close   on-close
        :open       @modal-open?
        :header     "Decommission"
        :content    (@tr [:are-you-sure?])
        :actions    [{:key     "cancel"
                      :content (@tr [:cancel])}
                     {:key     "yes"
                      :content (@tr [:yes]), :color "red"
                      :onClick #(dispatch [::events/decommission])}]}])))


(defn MenuBar []
  (let [can-decommission? (subscribe [::subs/can-decommission?])]
    [ui/Menu {:borderless true}
     (when @can-decommission?
       [DecommissionButton])
     [RefreshButton]]))


(defn UsbDeviceRow
  [{:keys [bus-id device-id vendor-id product-id busy description] :as device}]
  ^{:key (utils/usb-hw-id device)}
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
     (for [usb (sort-by utils/usb-hw-id usb-list)]
       ^{:key (utils/usb-hw-id usb)}
       [UsbDeviceRow usb])]]
   :label "Peripherals"
   :icon "usb"])


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
        status                   (subscribe [::subs/status-nuvlabox])
        next-heartbeat-moment    (subscribe [::subs/next-heartbeat-moment])
        next-heartbeat-times-ago (time/ago @next-heartbeat-moment)

        last-heartbeat-msg       (str "Last heartbeat was " (time/ago updated-moment))
        next-heartbeat-msg       (if @status
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
        (let [{:keys [resources peripherals updated next-heartbeat]} @nuvlabox-status]
          [ui/Container {:fluid true}
           [Heartbeat updated next-heartbeat]
           (when resources
             [Load resources])
           [Peripherals peripherals]])
        [ui/Message
         {:warning true
          :content "NuvlaBox status not available."}]))))


(defn NuvlaboxCard
  [nuvlabox status]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [{:keys [id name description created state] :as nuvlabox} status]
      ^{:key id}
      [ui/Card
       [ui/CardContent

        [ui/CardHeader {:style {:word-wrap "break-word"}}
         [:div {:style {:float "right"}}
          [StatusIcon status :corner "top right"]]
         [ui/Icon {:name "box"}] (or name id)]

        [ui/CardMeta (str (@tr [:created]) " " (-> created time/parse-iso8601 time/ago))]

        [:p {:style {:float "right"}} state]

        (when-not (str/blank? description)
          [ui/CardDescription {:style {:overflow "hidden" :max-height "100px"}} description])]])))


(defn SummarySection
  []
  (let [nuvlabox (subscribe [::subs/nuvlabox])
        status   (subscribe [::subs/status-nuvlabox])]
    [ui/CardGroup {:centered true}
     [NuvlaboxCard @nuvlabox @status]]))


(defn EdgeDetails
  [uuid]
  (let []
    (dispatch [::main-events/action-interval
               {:action    :start
                :id        :nuvlabox-get-nuvlabox
                :frequency 10000
                :event     [::events/get-nuvlabox (str "nuvlabox/" uuid)]}])
    (fn [uuid]
      ^{:key uuid}
      [ui/Container {:fluid true}
       [MenuBar]
       [SummarySection]
       [StatusSection]])))
