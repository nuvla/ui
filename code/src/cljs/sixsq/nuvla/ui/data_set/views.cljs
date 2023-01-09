(ns sixsq.nuvla.ui.data-set.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.apps.utils :as application-utils]
            [sixsq.nuvla.ui.cimi.subs :as cimi-subs]
            [sixsq.nuvla.ui.data-set.events :as events]
            [sixsq.nuvla.ui.data-set.spec :as spec]
            [sixsq.nuvla.ui.data-set.subs :as subs]
            [sixsq.nuvla.ui.data-set.utils :as utils]
            [sixsq.nuvla.ui.data.events :as data-events]
            [sixsq.nuvla.ui.data.spec :as data-spec]
            [sixsq.nuvla.ui.data.subs :as data-subs]
            [sixsq.nuvla.ui.filter-comp.views :as filter-comp]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.plugins.tab :as tab-plugin]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.general :as utils-general]
            [sixsq.nuvla.ui.utils.map :as map]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
            [sixsq.nuvla.ui.utils.values :as values]))

(defn refresh
  []
  (dispatch [::events/get-data-set]))

(defn refresh-data-records
  []
  (dispatch [::events/get-data-records]))

(defn ApplicationListItem
  [{:keys [id name description subtype created] :as _application} selectable?]
  (let [selected-application-id (subscribe [::data-subs/selected-application-id])
        on-click-fn             #(dispatch [::data-events/set-selected-application-id id])]
    [ui/ListItem (cond-> {:active (and @selected-application-id (= id @selected-application-id))}
                         selectable? (assoc :on-click on-click-fn))
     [ui/ListIcon {:name (application-utils/subtype-icon subtype), :size "large"}]
     [ui/ListContent
      [ui/ListHeader (str (or name id) " (" (time/ago (time/parse-iso8601 created)) ")")]
      (or description "")]]))

(defn ApplicationList
  [_opts]
  (let [tr           (subscribe [::i18n-subs/tr])
        applications (subscribe [::data-subs/applications])
        loading?     (subscribe [::data-subs/loading-applications?])]
    (fn [{:keys [selectable?] :or {selectable? true} :as _opts}]
      [ui/Segment {:loading @loading?
                   :basic   true}
       (if (seq @applications)
         [ui/ListSA {:divided   true
                     :relaxed   true
                     :selection selectable?}
          (for [application @applications]
            ^{:key (:id application)}
            [ApplicationListItem application selectable?])]
         [ui/Message {:error true} (@tr [:no-apps])])])))

(defn DataRecordFilter
  []
  (let [tr                        (subscribe [::i18n-subs/tr])
        data-record-filter        (subscribe [::subs/data-record-filter])
        suggest-edit-filter?      (subscribe [::subs/suggest-update-data-record-filter?])
        data-set                  (subscribe [::subs/data-set])
        filter-open?              (r/atom false)
        set-data-record-filter-fn #(dispatch [::events/set-data-record-filter %])]
    (fn []
      [components/SearchInput
       {:on-change     (ui-callback/input-callback set-data-record-filter-fn)
        :default-value (or @data-record-filter "")
        :placeholder   "Data records filter"
        :action        (r/as-element
                         [:<>
                          [ui/Button {:icon     "search"
                                      :on-click refresh-data-records}]
                          ^{:key (random-uuid)}
                          [filter-comp/ButtonFilter
                           {:resource-name  "data-record"
                            :default-filter @data-record-filter
                            :open?          filter-open?
                            :on-done        set-data-record-filter-fn}]
                          (when @suggest-edit-filter?
                            [ui/Button
                             {:icon     "save"
                              :primary  true
                              :content  (@tr [:save])
                              :on-click #(dispatch
                                           [::events/edit
                                            (:id @data-set)
                                            {:data-record-filter
                                             @data-record-filter}
                                            (@tr [:updated-message])])}])])}])))

(defn DataRecordMarker
  [{:keys [id name location]}]
  (when location
    [map/Marker {:position (map/longlat->latlong location)}
     [map/Tooltip (or name id)]]))

(defn DataRecordGeoJson
  [{:keys [id name geometry]}]
  (when geometry
    [map/GeoJSON {:style {:color "lime"}
                  :data  geometry}
     [map/Tooltip (or name id)]]))

(defn GeoOperationButton
  [geo-operation]
  (let [tr      (subscribe [::i18n-subs/tr])
        active? (subscribe [::subs/geo-operation-active? geo-operation])]
    (fn [geo-operation]
      (let [button [ui/Button
                    {:active   @active?
                     :on-click #(dispatch
                                  [::events/set-geo-operation geo-operation])}
                    geo-operation]]
        [ui/Popup
         {:header            (str/capitalize geo-operation)
          :content           (@tr [(->> geo-operation
                                        (str "geo-op-helper-") keyword)])
          :mouse-enter-delay 500
          :trigger           (r/as-element button)}]))))

(defn MapFilter
  []
  (let [map-selection     (subscribe [::subs/map-selection])
        data-records      (subscribe [::subs/data-records])
        set-map-selection #(dispatch [::events/set-map-selection %1])
        get-first-layer   #(-> %1 .-layers .getLayers first)
        map-ref!          (atom nil)
        fg-props          {:onAdd #(let [bounds (-> %1 .-target .getBounds)]
                                     (when (.isValid bounds)
                                       (.fitBounds @map-ref! bounds)))}]
    (set-map-selection nil)
    (fn []
      (let [enable-selection? (nil? @map-selection)]
        [uix/Accordion
         [:div
          [ui/ButtonGroup {:attached "top" :basic true}
           (for [op ["intersects" "disjoint" "within" "contains"]]
             ^{:key (str "button-" op)}
             [GeoOperationButton op])]
          [ui/Segment {:attached true}
           [map/MapBox {:ref #(some->> %1 .-leafletElement (reset! map-ref!))}
            [:<>
             ^{:key (random-uuid)}
             [map/FeatureGroup
              (when-not @map-selection fg-props)
              (doall
                (for [data-record (:resources @data-records)]
                  ^{:key (:id data-record)}
                  [:<>
                   [DataRecordMarker data-record]
                   [DataRecordGeoJson data-record]]))]
             ^{:key (random-uuid)}
             [map/FeatureGroup
              (when @map-selection fg-props)
              [map/EditControl
               {:onCreated (fn [event]
                             (let [layer      (.-layer event)
                                   geojson    (-> layer .toGeoJSON
                                                  (js->clj :keywordize-keys
                                                           true))
                                   layer-type (.-layerType event)]
                               (set-map-selection
                                 {:geojson    geojson
                                  :layer-type layer-type
                                  :lat-lngs   (.getLatLngs layer)})
                               (-> event .-layer .remove)))
                :onEdited  (fn [event]
                             (let [layer   (get-first-layer event)
                                   geojson (-> layer .toGeoJSON
                                               (js->clj :keywordize-keys true))]
                               (set-map-selection
                                 {:geojson    geojson
                                  :layer-type (:layer-type @map-selection)
                                  :lat-lngs   (.getLatLngs layer)})))
                :onDeleted (fn [event]
                             (when (some? (get-first-layer event))
                               (set-map-selection nil)))
                :draw      {:rectangle    enable-selection?
                            :polygon      enable-selection?
                            :polyline     false
                            :marker       false
                            :circle       false
                            :circlemarker false}}]

              ;; key is random to keep selection on top
              (when (= (:layer-type @map-selection) "rectangle")
                ^{:key (random-uuid)}
                [map/Rectangle {:bounds (:lat-lngs @map-selection)}])

              (when (= (:layer-type @map-selection) "polygon")
                ^{:key (random-uuid)}
                [map/Polygon {:positions (:lat-lngs @map-selection)}])]
             ]]]]
         :label [uix/TR :select-on-map]
         :title-size :h5
         :default-open false]
        ))))


(defn SearchHeader
  [_refresh-fn _extra]
  (let [tr          (subscribe [::i18n-subs/tr])
        time-period (subscribe [::subs/time-period])
        locale      (subscribe [::i18n-subs/locale])]
    (fn [refresh-fn extra]
      (let [[time-start time-end] @time-period
            date-format "MMMM dd, yyyy HH:mm"
            time-format "HH:mm"]
        [ui/Form
         [ui/FormGroup {:widths (if extra 3 2)}
          [ui/FormField
           ;; FIXME: Find a better way to set the field width.
           [ui/DatePicker
            {:custom-input     (r/as-element
                                 [ui/Input {:label (str/capitalize
                                                     (@tr [:from]))
                                            :style {:min-width "20em"}}])
             :selected         time-start
             :start-date       time-start
             :end-date         time-end
             :max-date         time-end
             :selects-start    true
             :show-time-select true
             :time-format      time-format
             :time-intervals   1
             :locale           (or (time/locale-string->locale-object @locale) @locale)
             :fixed-height     true
             :date-format      date-format
             :on-change        #(do (dispatch [::events/set-time-period
                                               [% time-end]])
                                    (refresh-fn))}]]
          ;; FIXME: Find a better way to set the field width.
          [ui/FormField
           [ui/DatePicker {:custom-input     (r/as-element
                                               [ui/Input
                                                {:label (str/capitalize
                                                          (@tr [:to]))
                                                 :style {:min-width "20em"}}])
                           :selected         time-end
                           :start-date       time-start
                           :end-date         time-end
                           :min-date         time-start
                           :max-date         (time/days-before -1)
                           :selects-end      true
                           :show-time-select true
                           :time-format      time-format
                           :time-intervals   1
                           :locale           (or (time/locale-string->locale-object @locale) @locale)
                           :fixed-height     true
                           :date-format      date-format
                           :on-change        #(do (dispatch
                                                    [::events/set-time-period
                                                     [time-start %]])
                                                  (refresh-fn))}]]
          (when extra
            [ui/FormField extra])]]))))


(defn ProcessButton
  [_button-type]
  (let [tr                    (subscribe [::i18n-subs/tr])
        selected-data-sets    (subscribe [::data-subs/selected-data-set-ids])
        selected-data-records (subscribe [::subs/selected-data-record-ids])
        active-tab            (subscribe [::tab-plugin/active-tab [::data-spec/tab]])
        on-click              #(dispatch [::main-events/subscription-required-dispatch
                                          [::data-events/open-application-select-modal]])]
    (fn [button-type]
      (let [selected  (if (= @active-tab :data-sets)
                        @selected-data-sets
                        @selected-data-records)
            disabled? (not (seq selected))]
        (if (= button-type :menu-item)
          [uix/MenuItem
           {:name     (@tr [:process])
            :disabled disabled?
            :icon     "rocket"
            :on-click on-click}]
          [ui/ButtonGroup {:primary true
                           :style   {:padding-top 10}}
           [ui/Button
            {:content  (@tr [:process])
             :disabled disabled?
             :icon     "rocket"
             :on-click on-click}]])))))


(defn CreateDataSet
  []
  (let [tr                       (subscribe [::i18n-subs/tr])
        active-tab               (subscribe [::tab-plugin/active-tab
                                             [::data-spec/tab]])
        selected-data-record-ids (subscribe [::subs/selected-data-record-ids])
        data-record-filter       (subscribe [::subs/data-record-filter])
        map-selection            (subscribe [::subs/map-selection])
        geo-operation            (subscribe [::subs/geo-operation])]
    (fn []
      (let [selected-data-records? (-> @selected-data-record-ids seq boolean)
            some-filter-str?       (not (str/blank? @data-record-filter))
            some-map-selection?    (some? @map-selection)]
        (when (and (= @active-tab :data-records)
                   (or selected-data-records?
                       some-filter-str?
                       some-map-selection?))
          [:span
           " "
           [ui/Button
            {:content  (@tr [:create-data-set])
             :icon     "plus"
             :primary  true
             :on-click (fn []
                         (dispatch [::data-events/set-modal-open? true])
                         (dispatch [::data-events/set-add-data-set-form
                                    :data-record-filter
                                    (utils-general/join-and
                                      (when selected-data-records?
                                        (->> @selected-data-record-ids
                                             (map #(str "id='" %1 "'"))
                                             (str/join " or ")))
                                      (when some-filter-str?
                                        @data-record-filter)
                                      (when some-map-selection?
                                        (utils/data-record-geometry-filter
                                          @geo-operation
                                          (:geojson @map-selection))))
                                    ]))}]]
          )))))

(defn DeleteButton
  [{:keys [id name description] :as _data-set}]
  (let [tr      (subscribe [::i18n-subs/tr])
        content (str (or name id) (when description " - ") description)]
    [uix/ModalDanger
     {:button-text (@tr [:delete])
      :on-confirm  #(dispatch [::events/delete])
      :trigger     (r/as-element [ui/MenuItem
                                  [ui/Icon {:name "trash"}]
                                  (@tr [:delete])])
      :header      (@tr [:delete-data-set])
      :content     content}]))

(defn MenuBar
  []
  (let [data-set (subscribe [::subs/data-set])]
    [:div
     [components/StickyBar
      [ui/Menu {:attached "top", :borderless true}
       (when (utils-general/can-delete? @data-set)
         [DeleteButton @data-set])
       [components/RefreshMenu
        {:on-refresh #(refresh)}]]]]))

(defn Pagination
  []
  (let [data-records (subscribe [::subs/data-records])]
    [pagination-plugin/Pagination
     {:db-path      [::spec/pagination]
      :change-event [::events/get-data-records]
      :total-items  (:count @data-records)}]))

(defn DataRecordRow
  [{:keys [id name description tags created timestamp bucket content-type
           infrastructure-service resource:deployment] :as _data-record}]
  (let [locale           (subscribe [::i18n-subs/locale])
        data-records-set (subscribe [::subs/selected-data-record-ids])]
    (fn [_data-record]
      ^{:key id}
      (let [uuid            (utils-general/id->uuid id)
            deployment-uuid (utils-general/id->uuid resource:deployment)
            is-uuid         (utils-general/id->uuid infrastructure-service)
            selected?       (boolean (@data-records-set id))]
        [ui/TableRow
         [ui/TableCell
          [ui/Checkbox
           {:checked  selected?
            :on-click (fn [event]
                        (dispatch [::events/toggle-data-record-id id])
                        (.stopPropagation event))}]]
         [ui/TableCell name]
         [ui/TableCell description]
         [ui/TableCell (time/parse-ago created @locale)]
         [ui/TableCell timestamp]
         [ui/TableCell bucket]
         [ui/TableCell content-type]
         [ui/TableCell [uix/Tags tags]]
         [ui/TableCell
          [values/as-link is-uuid :page "clouds"
           :label (utils-general/id->short-uuid infrastructure-service)]]
         [ui/TableCell (when resource:deployment
                         (values/as-link resource:deployment
                                         :label deployment-uuid))]
         [ui/TableCell (values/as-link id :label uuid)]]))))

(defn NoDataRecordsMessage
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Message {:info true} (@tr [:no-data-records])]))

(defn DataRecordTable
  [Pagination]
  (let [tr           (subscribe [::i18n-subs/tr])
        data-records (subscribe [::subs/data-records])
        resources    (:resources @data-records)]
    [:<>
     [SearchHeader refresh-data-records [DataRecordFilter]]
     [MapFilter]
     (if (-> resources count pos?)
       [ui/Table {:compact "very", :selectable true}
        [ui/TableHeader
         [ui/TableRow
          [ui/TableHeaderCell]
          [ui/TableHeaderCell (@tr [:name])]
          [ui/TableHeaderCell (@tr [:description])]
          [ui/TableHeaderCell (@tr [:created])]
          [ui/TableHeaderCell (@tr [:timestamp])]
          [ui/TableHeaderCell "bucket"]
          [ui/TableHeaderCell "content-type"]
          [ui/TableHeaderCell (@tr [:tags])]
          [ui/TableHeaderCell (str/lower-case (@tr [:storage-service]))]
          [ui/TableHeaderCell (@tr [:deployment])]
          [ui/TableHeaderCell (@tr [:id])]]]
        [ui/TableBody
         (for [{:keys [id] :as dr} resources]
           ^{:key id}
           [DataRecordRow dr])]]
       [NoDataRecordsMessage])
     Pagination]))

(defn DataRecordCard
  [{:keys [id name description timestamp _bucket tags
           _data-object infrastructure-service object] :as data-record}]
  (let [tr                        (subscribe [::i18n-subs/tr])
        data-objects              (subscribe [::subs/data-objects])
        base-uri                  (subscribe [::cimi-subs/base-uri])
        data-records-set          (subscribe [::subs/selected-data-record-ids])
        data-object-id            (:resource:object data-record)
        data-object               (get @data-objects data-object-id)
        filename                  object
        resource-deployment-id    (some-> data-record
                                          :resource:deployment
                                          values/resource->id)
        infrastructure-service-id (some-> infrastructure-service
                                          values/resource->id)
        selected?                 (boolean (@data-records-set id))]
    ^{:key id}
    [uix/Card
     {:header      [:span [:p {:style {:overflow      "hidden",
                                       :text-overflow "ellipsis",
                                       :max-width     "20ch"}}
                           (or name timestamp)]]
      :meta        (str (@tr [:created]) " " (-> timestamp
                                                 time/parse-iso8601 time/ago))
      :description (utils-general/truncate description 60)
      :content     [:<>
                    (when resource-deployment-id
                      [:div {:style {:padding "10px 0 0 0"}}
                       [ui/Icon {:name "rocket"}]
                       (values/as-link resource-deployment-id :page "deployment"
                                       :label (@tr [:deployment]))])
                    (when infrastructure-service-id
                      [:div {:style {:padding "10px 0 0 0"}}
                       [ui/Icon {:name "cloud"}]
                       [values/as-link infrastructure-service-id
                        :page "clouds" :label (@tr [:storage-service])]])]
      :tags        tags
      :on-select   #(dispatch [::events/toggle-data-record-id id])
      :selected?   selected?
      :button      (when data-object
                     [ui/Button {:color  "green"
                                 :target "_blank"
                                 :rel    "noreferrer"}
                      [:a {:href     (str @base-uri data-object-id "/download")
                           :target   "_blank"
                           :style    {:color "white"}
                           :download (when filename filename)}
                       [ui/Icon {:name "cloud download"}] " "
                       (@tr [:download])]])}]))

(defn cards-per-device
  [device]
  (case device
    :wide-screen 4
    :large-screen 4
    :computer 2
    :tablet 2
    :mobile 1
    1))

(defn columns-per-device
  [device]
  (case device
    :wide-screen 2
    :large-screen 1
    :computer 1
    :tablet 1
    :mobile 1
    1))

(defn TableCell
  [attribute {:keys [id] :as element}]
  (let [tr           (subscribe [::i18n-subs/tr])
        editable?    (subscribe [::subs/editable?])
        on-change-fn #(dispatch [::events/edit
                                 id {attribute %}
                                 (@tr [:updated-message])])]
    (if @editable?
      [components/EditableInput attribute element on-change-fn]
      [ui/TableCell (get element attribute)])))

(defn ModalAppPreview
  [_module-filter]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [module-filter]
      [uix/ModalFromButton
       {:trigger (r/as-element
                   [ui/Label {:style    {:float  "right"
                                         :cursor "pointer"}
                              :circular true
                              :color    "blue"
                              :on-click #(dispatch
                                           [::data-events/search-application
                                            module-filter])}
                    [ui/Icon {:name "eye"}]
                    (@tr [:preview])])
        :header  (str/capitalize (@tr [:application]))
        :content [ApplicationList {:selectable? false}]}])))

(defn Summary
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        dataset        (subscribe [::subs/data-set])
        device         (subscribe [::main-subs/device])
        editable?      (subscribe [::subs/editable?])
        {:keys [id created created-by updated data-record-filter module-filter
                tags]} @dataset
        resolved-owner (subscribe [::session-subs/resolve-user created-by])]
    [ui/Grid {:columns   (if (contains? #{:wide-screen} @device) 2 1)
              :stackable true
              :padded    true
              :centered  true}
     [ui/GridRow {:centered true}
      [ui/GridColumn
       [ui/SegmentGroup {:style  {:display         "flex"
                                  :justify-content "space-between"
                                  :background      "#f3f4f5"}
                         :raised true}
        [ui/Segment {:secondary true
                     :color     "green"
                     :raised    true}

         [:h4 {:style {:margin-top 0}} (str/capitalize (@tr [:summary]))]

         [ui/Table {:basic "very"}
          [ui/TableBody
           [ui/TableRow
            [ui/TableCell (str/capitalize (@tr [:name]))]
            [TableCell :name @dataset]]
           [ui/TableRow
            [ui/TableCell (str/capitalize (@tr [:description]))]
            [TableCell :description @dataset]]
           (when (or tags @editable?)
             [ui/TableRow
              [ui/TableCell (str/capitalize (@tr [:tags]))]
              [ui/TableCell
               [components/EditableTags @dataset
                #(dispatch [::events/edit id {:tags %}
                            (@tr [:updated-successfully])])]]])
           [ui/TableRow
            [ui/TableCell (str/capitalize (str (@tr [:created])))]
            [ui/TableCell (-> created time/parse-iso8601 time/ago)]]
           [ui/TableRow
            [ui/TableCell (str/capitalize (str (@tr [:owner])))]
            [ui/TableCell @resolved-owner]]
           [ui/TableRow
            [ui/TableCell (str/capitalize (str (@tr [:updated])))]
            [ui/TableCell (-> updated time/parse-iso8601 time/ago)]]
           [ui/TableRow
            [ui/TableCell (str/capitalize (@tr [:data-record-filter]))]
            [ui/TableCell data-record-filter]]
           [ui/TableRow
            [ui/TableCell (str/capitalize (@tr [:application]))]
            [ui/TableCell
             module-filter
             (when-not (str/blank? module-filter)
               [ModalAppPreview module-filter])]]
           [ui/TableRow
            [ui/TableCell "Id"]
            [ui/TableCell (when (some? id)
                            [values/as-link id
                             :label (utils-general/id->uuid id)])]]]]]]]]]))

(defn DataRecordCards
  [Pagination]
  (let [data-records (subscribe [::subs/data-records])
        device       (subscribe [::main-subs/device])
        resources    (:resources @data-records)]
    [:<>
     [SearchHeader refresh-data-records [DataRecordFilter]]
     [MapFilter]
     [ui/Segment style/basic
      (if (-> resources count pos?)
        [ui/CardGroup {:centered    true
                       :itemsPerRow (cards-per-device @device)
                       :stackable   true}
         (for [data-record resources]
           ^{:key (:id data-record)}
           [DataRecordCard data-record])]
        [NoDataRecordsMessage])]
     Pagination]))

(defn DataSet
  [dataset-id]
  (let [dataset-id (if (string? dataset-id) dataset-id (get-in dataset-id [:path-params :uuid]))
        tr         (subscribe [::i18n-subs/tr])
        data-set   (subscribe [::subs/data-set])
        device     (subscribe [::main-subs/device])]
    (dispatch [::events/set-data-set-id dataset-id])
    (refresh)
    (fn [dataset-id]
      (let [name (:name @data-set)]
        [components/LoadingPage {:dimmable? true}
         [:<>
          [components/NotFoundPortal
           ::subs/not-found?
           :no-data-set-message-header
           :no-data-set-message-content]
          [ui/Segment style/basic
           [uix/PageHeader "database" (str name " " (@tr [:data-set]))]
           [MenuBar dataset-id]
           [Summary]
           [ui/Grid {:columns   (columns-per-device @device)
                     :stackable true
                     :padded    true
                     :centered  true}
            [ui/GridRow {:centered true}
             [ui/GridColumn
              [DataRecordCards [Pagination]]]]]]]]))))
