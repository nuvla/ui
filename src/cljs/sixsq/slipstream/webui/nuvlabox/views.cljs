(ns sixsq.slipstream.webui.nuvlabox.views
  (:require
    [cljs.pprint :refer [cl-format pprint]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.slipstream.webui.history.events :as history-events]
    [sixsq.slipstream.webui.i18n.subs :as i18n-subs]
    [sixsq.slipstream.webui.main.subs :as main-subs]
    [sixsq.slipstream.webui.nuvlabox-detail.events :as nuvlabox-detail-events]
    [sixsq.slipstream.webui.nuvlabox-detail.views :as nuvlabox-detail]
    [sixsq.slipstream.webui.nuvlabox.events :as nuvlabox-events]
    [sixsq.slipstream.webui.nuvlabox.subs :as nuvlabox-subs]
    [sixsq.slipstream.webui.panel :as panel]
    [sixsq.slipstream.webui.utils.forms :as forms]
    [sixsq.slipstream.webui.utils.general :as general-utils]
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]
    [sixsq.slipstream.webui.utils.semantic-ui-extensions :as uix]
    [sixsq.slipstream.webui.utils.style :as style]
    [sixsq.slipstream.webui.utils.ui-callback :as ui-callback]))


(defn stat
  [value icon color label]
  [ui/Statistic {:size "tiny"}
   [ui/StatisticValue value "\u2002" [ui/Icon {:name icon :color color}]]
   [ui/StatisticLabel label]])


(defn health-summary
  []
  (let [nuvlabox-records (subscribe [::nuvlabox-subs/nuvlabox-records])
        health-info (subscribe [::nuvlabox-subs/health-info])]
    (let [{:keys [stale-count active-count]} @health-info
          total (get @nuvlabox-records :count 0)
          unknown (max (- total stale-count active-count) 0)]
      [ui/Segment style/evenly
       [stat total "computer" "black" "NuvlaBox"]
       [stat stale-count "warning sign" "red" "stale"]
       [stat active-count "check circle" "green" "active"]
       [stat unknown "ellipsis horizontal" "yellow" "unknown"]])))


(defn search-header []
  (let [state-selector (subscribe [::nuvlabox-subs/state-selector])]
    (fn []
      ;; reset visible values of parameters
      [ui/Form {:on-key-press (partial forms/on-return-key #(dispatch [::nuvlabox-events/get-nuvlabox-records]))}

       [ui/FormGroup

        [ui/FormField
         ^{:key (str "state:" @state-selector)}
         [ui/Dropdown
          {:value     @state-selector
           :scrolling false
           :selection true
           :options   [{:value "all", :text "all states"}
                       {:value "new", :text "new state"}
                       {:value "activated", :text "activated state"}
                       {:value "quarantined", :text "quarantined state"}]
           :on-change (ui-callback/value #(dispatch [::nuvlabox-events/set-state-selector %]))}]]]])))


(defn search-button
  []
  (let [tr (subscribe [::i18n-subs/tr])
        loading? (subscribe [::nuvlabox-subs/loading?])]
    (fn []
      [uix/MenuItemWithIcon
       {:name      (@tr [:refresh])
        :icon-name "refresh"
        :loading?  @loading?
        :position "right"
        :disabled  false #_(nil? @selected-id)
        :on-click  (fn []
                     (dispatch [::nuvlabox-events/fetch-health-info])
                     (dispatch [::nuvlabox-events/get-nuvlabox-records]))}])))


(defn menu-bar []
  [ui/Segment style/basic
   [ui/Menu {:attached   "top"
             :borderless true}
    [search-button]]
   [ui/Segment {:attached "bottom"}
    [search-header]]])


(defn format-nb-header
  []
  [ui/TableHeader
   [ui/TableRow
    [ui/TableHeaderCell [ui/Icon {:name "heartbeat"}]]
    [ui/TableHeaderCell "mac"]
    [ui/TableHeaderCell "state"]
    [ui/TableHeaderCell "name"]]])


(defn health-icon
  [value]
  (case value
    true [ui/Icon {:name "check circle" :color "green"}]
    false [ui/Icon {:name "warning sign" :color "red"}]
    [ui/Icon {:name "ellipsis horizontal" :color "yellow"}]))


(defn format-nb-row
  [healthy? {:keys [id macAddress state name] :as row}]
  (let [uuid (second (str/split id #"/"))
        on-click (fn []
                   (dispatch [::nuvlabox-detail-events/clear-detail])
                   (dispatch [::history-events/navigate (str "nuvlabox/" uuid)]))]
    [ui/TableRow {:on-click on-click
                  :style    {:cursor "pointer"}}
     [ui/TableCell {:collapsing true} (health-icon (get healthy? id))]
     [ui/TableCell {:collapsing true} [:a {:on-click on-click} macAddress]]
     [ui/TableCell {:collapsing true} state]
     [ui/TableCell name]]))


(defn nb-table
  []
  (let [nuvlabox-records (subscribe [::nuvlabox-subs/nuvlabox-records])
        elements-per-page (subscribe [::nuvlabox-subs/elements-per-page])
        page (subscribe [::nuvlabox-subs/page])
        health-info (subscribe [::nuvlabox-subs/health-info])]
    (dispatch [::nuvlabox-events/get-nuvlabox-records])
    (fn []
      (let [{:keys [healthy?]} @health-info
            total-elements (get @nuvlabox-records :count 0)]
        [:div

         [ui/Table {:compact "very", :selectable true}
          (format-nb-header)
          (vec (concat [ui/TableBody]
                       (mapv (partial format-nb-row healthy?) (get @nuvlabox-records :nuvlaboxRecords []))))]

         [uix/Pagination {:totalPages   (general-utils/total-pages total-elements @elements-per-page)
                          :activePage   @page
                          :onPageChange (ui-callback/callback :activePage #(dispatch [::nuvlabox-events/set-page %]))}]
         ]))))


(defn nb-info
  []
  (let [path (subscribe [::main-subs/nav-path])]
    (fn []
      (let [[_ mac] @path
            n (count @path)
            children (case n
                       1 [[menu-bar]
                          [health-summary]
                          [nb-table]]
                       2 [[nuvlabox-detail/nb-detail]]
                       [[menu-bar]
                        [health-summary]
                        [nb-table]])]
        (dispatch [::nuvlabox-detail-events/set-mac mac])
        (vec (concat [ui/Segment style/basic] children))))))


(defmethod panel/render :nuvlabox
  [path]
  [nb-info])
