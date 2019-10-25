(ns sixsq.nuvla.ui.infrastructures-detail.views
  (:require
    [cljs.pprint :refer [cl-format]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.edge-detail.events :as events]
    [sixsq.nuvla.ui.edge-detail.subs :as subs]
    [sixsq.nuvla.ui.edge.subs :as edge-subs]
    [sixsq.nuvla.ui.edge.utils :as u]
    [sixsq.nuvla.ui.edge.utils :as utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.plot.plot :as plot]
    [sixsq.nuvla.ui.utils.resource-details :as resource-details]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.time :as time]
    [taoensso.timbre :as log]))


(def refresh-action-id :nuvlabox-get-nuvlabox)


(defn refresh
  [uuid]
  (dispatch [::main-events/action-interval-start {:id        refresh-action-id
                                                  :frequency 10000
                                                  :event     [::events/get-nuvlabox (str "nuvlabox/" uuid)]}]))


(defn MenuBar [uuid]
  (let [tr                (subscribe [::i18n-subs/tr])
        can-decommission? (subscribe [::subs/can-decommission?])
        can-delete?       (subscribe [::subs/can-delete?])
        nuvlabox          (subscribe [::subs/nuvlabox])
        loading?          (subscribe [::subs/loading?])
        {:keys [id name] :as nuvlabox} @nuvlabox]
    [ui/Menu {:borderless true}
     (when @can-decommission?
       [resource-details/action-button-icon "Decommission" (@tr [:yes]) "eraser" (str "Decommission " (or id name))
        (@tr [:are-you-sure?]) #(dispatch [::events/decommission]) (constantly nil)])
     (when @can-delete?
       [resource-details/delete-button nuvlabox #(dispatch [::events/delete])])

     [main-components/RefreshMenu
      {:action-id  refresh-action-id
       :loading?   @loading?
       :on-refresh #(refresh uuid)}]]))
;
;
;(defn Peripheral
;  [{p-name        :name
;    p-product     :product
;    p-created     :created
;    p-updated     :updated
;    p-descr       :description
;    p-interface   :interface
;    p-device-path :device-path
;    p-available   :available
;    p-vendor      :vendor
;    p-classes     :classes
;    p-indentifier :identifier}]
;  (let [locale (subscribe [::i18n-subs/locale])]
;    [uix/Accordion
;     [ui/Table {:basic "very"}
;      [ui/TableBody
;       (when p-product
;         [ui/TableRow
;          [ui/TableCell "Name"]
;          [ui/TableCell (str p-name " " p-product)]])
;       (when p-descr
;         [ui/TableRow
;          [ui/TableCell "Description"]
;          [ui/TableCell p-descr]])
;       [ui/TableRow
;        [ui/TableCell "Classes"]
;        [ui/TableCell (str/join ", " p-classes)]]
;       [ui/TableRow
;        [ui/TableCell "Available"]
;        [ui/TableCell
;         [ui/Icon {:name "circle", :color (if p-available "green" "red")}]
;         (if p-available "Yes" "No")]]
;       (when p-interface
;         [ui/TableRow
;          [ui/TableCell "Interface"]
;          [ui/TableCell p-interface]])
;       (when p-device-path
;         [ui/TableRow
;          [ui/TableCell "Device path"]
;          [ui/TableCell p-device-path]])
;       [ui/TableRow
;        [ui/TableCell "Identifier"]
;        [ui/TableCell p-indentifier]]
;       [ui/TableRow
;        [ui/TableCell "Vendor"]
;        [ui/TableCell p-vendor]]
;       [ui/TableRow
;        [ui/TableCell "Created"]
;        [ui/TableCell (time/ago (time/parse-iso8601 p-created) @locale)]]
;       [ui/TableRow
;        [ui/TableCell "Updated"]
;        [ui/TableCell (time/ago (time/parse-iso8601 p-updated) @locale)]]]
;      ]
;     :label (or p-name p-product)
;     :title-size :h4
;     :default-open false
;     :icon (case p-interface
;             "USB" "usb"
;             nil)]))
;
;
;(defn Peripherals
;  []
;  (let [nuvlabox-peripherals (subscribe [::subs/nuvlabox-peripherals])]
;    [uix/Accordion
;     [:div
;      (doall
;        (for [{p-indentifier :identifier
;               p-created     :created
;               :as           peripheral} @nuvlabox-peripherals]
;          ^{:key (str p-indentifier p-created)}
;          [Peripheral peripheral]))]
;     :label "Peripherals"
;     :icon "usb"
;     :count (count @nuvlabox-peripherals)]))
;
;
;(defn StatusIcon
;  [status]
;  [ui/Popup
;   {:position "right center"
;    :content  status
;    :trigger  (r/as-element
;                [ui/Icon {:name  "power"
;                          :color (utils/status->color status)}])}])
;
;
;(defn Heartbeat
;  [updated]
;  (let [updated-moment           (time/parse-iso8601 updated)
;        {:keys [id]} @(subscribe [::subs/nuvlabox])
;        status                   (subscribe [::edge-subs/status-nuvlabox id])
;        next-heartbeat-moment    (subscribe [::subs/next-heartbeat-moment])
;        next-heartbeat-times-ago (time/ago @next-heartbeat-moment)
;
;        last-heartbeat-msg       (str "Last heartbeat was " (time/ago updated-moment))
;        next-heartbeat-msg       (if (= @status :online)
;                                   (str "Next heartbeat is expected " next-heartbeat-times-ago)
;                                   (str "Next heartbeat was expected " next-heartbeat-times-ago))]
;
;    [uix/Accordion
;     [:<>
;      [:p last-heartbeat-msg]
;      [:p next-heartbeat-msg]]
;     :label "Heartbeat"
;     :icon "heartbeat"]))
;
;
;(defn Load
;  [resources]
;  [uix/Accordion
;   (let [load-stats (u/load-statistics resources)]
;     [plot/HorizontalBar {:height  50
;                          :data    {:labels   (map :label load-stats)
;                                    :datasets [{:data (map :percentage load-stats)}]}
;                          :options {:scales {:xAxes [{:type  "linear"
;                                                      :ticks {:beginAtZero true
;                                                              :max         100}}]
;                                             :yAxes [{:gridLines {:display false}}]}}}])
;   :label "Load Percentages"
;   :icon "thermometer half"])
;
;
;(defn StatusSection
;  []
;  (let [nuvlabox-status (subscribe [::subs/nuvlabox-status])]
;    (fn []
;      (if @nuvlabox-status
;        (let [{:keys [resources updated next-heartbeat]} @nuvlabox-status]
;          [ui/Container {:fluid true}
;           [Heartbeat updated next-heartbeat]
;           (when resources
;             [Load resources])
;           [Peripherals]])
;        [ui/Message
;         {:warning true
;          :content "NuvlaBox status not available."}]))))
;
;
;(defn NuvlaboxCard
;  [nuvlabox status]
;  (let [tr (subscribe [::i18n-subs/tr])]
;    (fn [{:keys [id name description created state] :as nuvlabox} status]
;      ^{:key id}
;      [ui/Card
;       [ui/CardContent
;
;        [ui/CardHeader {:style {:word-wrap "break-word"}}
;         [:div {:style {:float "right"}}
;          [StatusIcon status :corner "top right"]]
;         [ui/Icon {:name "box"}] (or name id)]
;
;        [ui/CardMeta (str (@tr [:created]) " " (-> created time/parse-iso8601 time/ago))]
;
;        [:p {:style {:float "right"}} state]
;
;        (when-not (str/blank? description)
;          [ui/CardDescription {:style {:overflow "hidden" :max-height "100px"}} description])]])))
;
;
;(defn SummarySection
;  []
;  (let [{:keys [id] :as nuvlabox} @(subscribe [::subs/nuvlabox])
;        status @(subscribe [::edge-subs/status-nuvlabox id])]
;    [ui/CardGroup {:centered true}
;     [NuvlaboxCard nuvlabox status]]))


(defn InfrastructureDetails
  [uuid]
  (let []
    (refresh uuid)
    (fn [uuid]
      ^{:key uuid}
      [ui/Container {:fluid true}
       [MenuBar uuid]
       #_[SummarySection]
       #_[StatusSection]])))
