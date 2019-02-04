(ns sixsq.slipstream.webui.usage.views
  (:require
    [cljs.pprint :as pprint]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as reagent]
    [sixsq.slipstream.webui.i18n.subs :as i18n-subs]
    [sixsq.slipstream.webui.panel :as panel]
    [sixsq.slipstream.webui.usage.events :as usage-events]
    [sixsq.slipstream.webui.usage.subs :as usage-subs]
    [sixsq.slipstream.webui.usage.utils :as u]
    [sixsq.slipstream.webui.utils.general :as general]
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]
    [sixsq.slipstream.webui.utils.semantic-ui-extensions :as uix]
    [sixsq.slipstream.webui.utils.style :as style]
    [sixsq.slipstream.webui.utils.time :as time]
    [sixsq.slipstream.webui.utils.ui-callback :as ui-callback]
    [sixsq.slipstream.webui.utils.values :as values]))


(defn format [fmt-str & v]
  (apply pprint/cl-format nil fmt-str v))


(defn value-in-table [v]
  (let [v-int-part (int v)
        v-float-part (- v v-int-part)]
    (format "~,,'',3:d~0,2f" v-int-part v-float-part)))


(defn value-in-statistic [v]
  (->> v Math/round (format "~,,'',3:d ")))


(defn truncate-credential
  [credential]
  (-> credential
      (str/split #"/")
      second
      (str/split #"-")
      first))


(defn credential-link
  [credential]
  (->> credential truncate-credential (values/as-link credential)))


(defn truncate-connector
  [connector]
  (-> connector (str/split #"/") second))


(defn connector-link
  [connector]
  (->> connector truncate-connector (values/as-link connector)))


(def key-fns
  {:credential first
   :cloud      (comp :cloud second)
   :vms        (comp :value :vms second)
   :cpus       (comp :value :cpus second)
   :ram        (comp :value :ram second)
   :disk       (comp :value :disk second)
   :price      (comp :value :price second)})


(def comparator-fns
  {:ascending  #(< %1 %2)
   :descending #(> %1 %2)})


(defn sort-rows
  [{:keys [column direction]} rows]
  (sort-by (get key-fns column first)
           (get comparator-fns direction (:descending comparator-fns))
           rows))


(defn column-click
  [column]
  (fn []
    (dispatch [::usage-events/set-sort column])))


(defn sorted-value
  [{:keys [column direction] :as sort} check-column]
  (when (= column check-column)
    (name direction)))


(defn results-table-row
  [[credential {:keys [vms cpus ram disk price cloud] :as result}]]
  (when (and credential result)
    ^{:key (name credential)}
    [ui/TableRow
     [ui/TableCell {:text-align "left", :collapsing true} (credential-link credential)]
     [ui/TableCell {:text-align "left" :collapsing true} (connector-link cloud)]
     [ui/TableCell (value-in-table (:value vms))]
     [ui/TableCell (value-in-table (:value cpus))]
     [ui/TableCell (value-in-table (:value ram))]
     [ui/TableCell (value-in-table (:value disk))]
     [ui/TableCell (value-in-table (:value price))]]))


(defn add-cloud-field
  [credentials-map [credential value]]
  (let [cloud (if (= credential "all-credentials")
                "SELECTED"
                (:href (get-in credentials-map [credential :connector])))]
    [credential (assoc value :cloud cloud)]))


(defn add-cloud-fields
  [credentials-map results]
  (map (partial add-cloud-field credentials-map) results))


(defn table-results-credentials []
  (let [tr (subscribe [::i18n-subs/tr])
        results (subscribe [::usage-subs/results])
        sort (subscribe [::usage-subs/sort])
        credentials-map (subscribe [::usage-subs/credentials-map])]
    (fn []
      (if (pos? (count @results))
        [ui/Table (merge style/selectable {:text-align "right"
                                           :celled     true
                                           :sortable   true})
         [ui/TableHeader
          [ui/TableRow
           [ui/TableHeaderCell {:sorted (sorted-value @sort :credential), :text-align "left", :collapsing true, :on-click (column-click :credential)} "credential"]
           [ui/TableHeaderCell {:sorted (sorted-value @sort :cloud), :text-align "left", :collapsing true,, :on-click (column-click :cloud)} "cloud"]
           [ui/TableHeaderCell {:sorted (sorted-value @sort :vms), :on-click (column-click :vms)} u/vms-unit]
           [ui/TableHeaderCell {:sorted (sorted-value @sort :cpus), :on-click (column-click :cpus)} u/cpus-unit]
           [ui/TableHeaderCell {:sorted (sorted-value @sort :ram), :on-click (column-click :ram)} u/ram-unit]
           [ui/TableHeaderCell {:sorted (sorted-value @sort :disk), :on-click (column-click :disk)} u/disk-unit]
           [ui/TableHeaderCell {:sorted (sorted-value @sort :price), :on-click (column-click :price)} u/price-unit]]]
         [ui/TableBody
          (some->> @results
                   (add-cloud-fields @credentials-map)
                   (sort-rows @sort)
                   (map results-table-row)
                   doall)]]
        [ui/Header {:as "h1", :text-align "center"} (@tr [:no-data])]))))


(defn totals []
  (let [totals (subscribe [::usage-subs/totals])
        credentials-map (subscribe [::usage-subs/credentials-map])
        selected-credentials (subscribe [::usage-subs/selected-credentials])]
    (fn []
      (let [{:keys [vms cpus ram disk price]} @totals
            all-creds-count (count @credentials-map)
            real-count-selected-creds (count @selected-credentials)
            count-selected-creds (if (zero? real-count-selected-creds)
                                   all-creds-count
                                   real-count-selected-creds)]
        (vec (concat [ui/StatisticGroup {:size "tiny", :widths "six", :style {:max-width "100%"}}
                      [ui/Statistic
                       [ui/StatisticValue (str count-selected-creds "/" all-creds-count) "\u0020"
                        [ui/Icon {:size "small" :name "key"}]]
                       [ui/StatisticLabel "credentials"]]]
                     (when-not (nil? @totals)
                       [[ui/Statistic
                         [ui/StatisticValue (value-in-statistic (:value vms)) "\u0020"
                          [ui/Icon {:size "small" :name "server"}]]
                         [ui/StatisticLabel u/vms-unit]]
                        [ui/Statistic
                         [ui/StatisticValue (value-in-statistic (:value cpus)) "\u0020"
                          [ui/Icon {:size "small" :rotated "clockwise" :name "microchip"}]]
                         [ui/StatisticLabel u/cpus-unit]]
                        [ui/Statistic
                         [ui/StatisticValue (value-in-statistic (:value ram)) "\u0020"
                          [ui/Icon {:size "small" :name "grid layout"}]]
                         [ui/StatisticLabel u/ram-unit]]
                        [ui/Statistic
                         [ui/StatisticValue (value-in-statistic (:value disk)) "\u0020"
                          [ui/Icon {:size "small" :name "database"}]]
                         [ui/StatisticLabel {} u/disk-unit]]
                        [ui/Statistic
                         [ui/StatisticValue (value-in-statistic (:value price)) "\u0020"
                          [ui/Icon {:size "small" :name "euro"}]]
                         [ui/StatisticLabel {} u/price-unit]]])))))))


(defn search-credentials-dropdown []
  (let [tr (subscribe [::i18n-subs/tr])
        credentials-map (subscribe [::usage-subs/credentials-map])
        selected-credentials (subscribe [::usage-subs/selected-credentials])
        loading-credentials-map? (subscribe [::usage-subs/loading-credentials-map?])]
    (fn []
      [ui/FormField
       [ui/Dropdown
        {:fluid       true
         :icon        "key"
         :className   "icon"
         :labeled     true
         :button      true
         :placeholder (@tr [:all-credentials])
         :loading     @loading-credentials-map?
         :multiple    true
         :search      true
         :selection   true
         :value       (clj->js (or @selected-credentials []))
         :onChange    (ui-callback/dropdown ::usage-events/set-selected-credentials)
         :options     (map
                        #(let [{:keys [id name description connector]} %]
                           {:key     id
                            :value   id
                            :text    (or name id)
                            :content (reagent/as-element [ui/Header {:as "h5"} (or name id)
                                                          [ui/HeaderSubheader (str "id: " id)]
                                                          [ui/HeaderSubheader (str "connector: " (:href connector))]
                                                          [ui/HeaderSubheader (str "name: " name)]
                                                          [ui/HeaderSubheader (str "description: " description)]])})
                        (vals @credentials-map))}]])))


(defn search-users-roles-dropdown []
  (let [tr (subscribe [::i18n-subs/tr])
        selected-users-roles (subscribe [::usage-subs/selected-users-roles])
        users-roles-list (subscribe [::usage-subs/users-roles-list])]
    (fn []
      [ui/FormField
       [ui/Dropdown {:fluid          true
                     :placeholder    (@tr [:usage-filter])
                     :search         true
                     :multiple       true
                     :icon           "users"
                     :labeled        true
                     :button         true
                     :value          @selected-users-roles
                     :className      "icon multiple"
                     :style          {:width nil}
                     :selection      true
                     :allowAdditions true
                     :onChange       (ui-callback/dropdown ::usage-events/set-users-roles)
                     :onAddItem      (ui-callback/value #(dispatch [::usage-events/push-users-roles-list %]))
                     :options        @users-roles-list}]])))


(defn search-header []
  (let [tr (subscribe [::i18n-subs/tr])
        date-range (subscribe [::usage-subs/date-range])
        locale (subscribe [::i18n-subs/locale])
        billable-only? (subscribe [::usage-subs/billable-only?])
        range-initial-val u/default-date-range
        range-dropdown (reagent/atom range-initial-val)]
    (fn []
      (let [[date-after date-before :as range] @date-range]
        [ui/Form
         [ui/FormGroup
          [ui/FormField
           [ui/Dropdown {:labeled   true
                         :button    true
                         :className "icon"
                         :icon      "time"
                         :selection true
                         :options   (map (fn [k] {:text  (@tr [(keyword k)])
                                                  :value k}) (keys u/date-range-entries))
                         :value     @range-dropdown
                         :onChange  #(do
                                       (reset! range-dropdown (-> %2
                                                                  (js->clj :keywordize-keys true)
                                                                  :value))
                                       (when-not (= "custom" @range-dropdown)
                                         (dispatch [::usage-events/set-date-range
                                                    (get u/date-range-entries @range-dropdown)])))}]]
          [ui/FormField
           [ui/DatePicker {:custom-input  (reagent/as-element [ui/Input {:label (@tr [:from])}])
                           :selected      date-after
                           :start-date    date-after
                           :end-date      date-before
                           :min-date      (time/days-before 90)
                           :max-date      (time/now)
                           :selects-start true
                           :locale        @locale
                           :fixed-height  true
                           :date-format   "ddd, D MMMM YYYY"
                           :on-change     (fn [date]
                                            (let [new-range [date date-before]
                                                  new-tag (u/get-date-range-tag new-range)]
                                              (reset! range-dropdown new-tag)
                                              (dispatch [::usage-events/set-date-range new-range])))}]]
          [ui/FormField
           [ui/DatePicker {:custom-input (reagent/as-element [ui/Input {:label (@tr [:to])}])
                           :selected     date-before
                           :start-date   date-after
                           :end-date     date-before
                           :locale       @locale
                           :fixed-height true
                           :date-format  "ddd, D MMMM YYYY"
                           :min-date     date-after
                           :max-date     (time/now)
                           :selects-end  true
                           :on-change    (fn [date]
                                           (let [new-range [date-after (.endOf date "day")]
                                                 new-tag (u/get-date-range-tag new-range)]
                                             (reset! range-dropdown new-tag)
                                             (dispatch [::usage-events/set-date-range new-range])))}]]]
         [ui/FormGroup
          [ui/Checkbox {:toggle    true
                        :checked   @billable-only?
                        :label     (@tr [:billable-only?])
                        :on-change #(dispatch [::usage-events/toggle-billable-only?])}]]
         [ui/FormGroup {:widths "equal"}
          [search-users-roles-dropdown]
          [search-credentials-dropdown]]]))))


(defn control-bar []
  (let [tr (subscribe [::i18n-subs/tr])
        totals (subscribe [::usage-subs/totals])
        results (subscribe [::usage-subs/results])]

    [:div
     [ui/Menu {:attached "top", :borderless true}
      [uix/MenuItemWithIcon
       {:name      (@tr [:search])
        :icon-name "search"
        :on-click  (fn []
                     (dispatch [::usage-events/fetch-data]))}]
      (when @results
        [uix/MenuItemWithIcon
         {:name      (@tr [:download])
          :icon-name "download"
          :as        :a
          :download  "data.json"
          :href      (->> (assoc @results :total @totals)
                          general/edn->json
                          (.encodeURIComponent js/window)
                          (str "data:application/json;charset=utf-8,"))}])]
     [ui/Segment {:attached "bottom"}
      [search-header]]]))


(defn data-segment []
  (let [loading-totals? (subscribe [::usage-subs/loading-totals?])
        loading-details? (subscribe [::usage-subs/loading-details?])]
    [ui/Segment (assoc style/autoscroll-x :loading (or @loading-totals? @loading-details?))
     [totals]
     [table-results-credentials]]))


(defn usage
  []
  (let [initialized? (subscribe [::usage-subs/initialized?])]

    ;; force the initialization of the credentials and data only the
    ;; first time that the usage page is mounted
    (when-not @initialized?
      (dispatch [::usage-events/initialize]))

    (fn []
      [ui/Container {:fluid true}
       [control-bar]
       [data-segment]])))


(defmethod panel/render :usage
  [_]
  [usage])
