(ns sixsq.nuvla.ui.data-set.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.data-set.events :as events]
    [sixsq.nuvla.ui.data.events :as data-events]
    [sixsq.nuvla.ui.data-set.subs :as subs]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.session.subs :as session-subs]
    [sixsq.nuvla.ui.utils.general :as utils-general]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as values]
    [sixsq.nuvla.ui.filter-comp.views :as filter-comp]
    [taoensso.timbre :as log]))


(defn refresh
  []
  (dispatch [::events/refresh]))


(defn DataRecordFilter
  []
  (let [tr                        (subscribe [::i18n-subs/tr])
        data-record-filter        (subscribe [::subs/data-record-filter])
        suggest-edit-filter?      (subscribe [::subs/suggest-update-data-record-filter?])
        suggest-new-data-set?     (subscribe [::subs/suggest-new-data-set?])
        data-set                  (subscribe [::subs/data-set])
        filter-open?              (r/atom false)
        set-data-record-filter-fn #(dispatch [::events/set-data-record-filter %])]
    (fn []
      [components/SearchInput
       {:on-change     (ui-callback/input-callback set-data-record-filter-fn)
        :default-value (or @data-record-filter "")
        :placeholder   "Data records filter"
        :action        (r/as-element [:<>
                                      [ui/Button {:icon     "search"
                                                  :on-click #(dispatch [::events/get-data-records])}]
                                      ^{:key (random-uuid)}
                                      [filter-comp/ButtonFilter
                                       {:resource-name  "data-record"
                                        :default-filter @data-record-filter
                                        :open?          filter-open?
                                        :on-done        set-data-record-filter-fn}]
                                      (when @suggest-new-data-set?
                                        [ui/Button {:icon     "plus"
                                                    :primary  true
                                                    :content  (@tr [:create])
                                                    :on-click #(do
                                                                 (dispatch [::data-events/set-modal-open? true])
                                                                 (dispatch [::data-events/set-add-data-set-form :data-record-filter @data-record-filter]))}])
                                      (when @suggest-edit-filter?
                                        [ui/Button {:icon     "save"
                                                    :primary  true
                                                    :content  (@tr [:save])
                                                    :on-click #(dispatch [::events/edit
                                                                          (:id @data-set)
                                                                          {:data-record-filter @data-record-filter}
                                                                          (@tr [:updated-message])])}])])}])))


(defn SearchHeader
  [_refresh-fn _extra]
  (let [tr          (subscribe [::i18n-subs/tr])
        time-period (subscribe [::subs/time-period])
        locale      (subscribe [::i18n-subs/locale])]
    (fn [refresh-fn extra]
      (let [[time-start time-end] @time-period
            date-format "MMMM DD, YYYY HH:mm"
            time-format "HH:mm"]
        [ui/Form
         [ui/FormGroup {:widths (if extra 3 2)}
          [ui/FormField
           ;; FIXME: Find a better way to set the field width.
           [ui/DatePicker {:custom-input     (r/as-element [ui/Input {:label (str/capitalize (@tr [:from]))
                                                                      :style {:min-width "20em"}}])
                           :selected         time-start
                           :start-date       time-start
                           :end-date         time-end
                           :max-date         time-end
                           :selects-start    true
                           :show-time-select true
                           :time-format      time-format
                           :time-intervals   15
                           :locale           @locale
                           :fixed-height     true
                           :date-format      date-format
                           :on-change        #(do (dispatch [::events/set-time-period [% time-end]])
                                                  (refresh-fn))}]]
          ;; FIXME: Find a better way to set the field width.
          [ui/FormField
           [ui/DatePicker {:custom-input     (r/as-element [ui/Input {:label (str/capitalize (@tr [:to]))
                                                                      :style {:min-width "20em"}}])
                           :selected         time-end
                           :start-date       time-start
                           :end-date         time-end
                           :min-date         time-start
                           :max-date         (time/days-before -1)
                           :selects-end      true
                           :show-time-select true
                           :time-format      time-format
                           :time-intervals   15
                           :locale           @locale
                           :fixed-height     true
                           :date-format      date-format
                           :on-change        #(do (dispatch [::events/set-time-period [time-start %]])
                                                  (refresh-fn))}]]
          (when extra
            [ui/FormField extra])]]))))


(defn ProcessButton
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        data-sets (subscribe [::subs/selected-data-set-ids])]
    (fn []
      [uix/MenuItem
       {:name     (@tr [:process])
        :disabled (not (seq @data-sets))
        :icon     "rocket"
        :on-click #(dispatch [::main-events/subscription-required-dispatch
                              [::events/open-application-select-modal]])}])))


(defn MenuBar
  []
  [:div
   [components/StickyBar
    [ui/Menu {:attached "top", :borderless true}
     [components/RefreshMenu
      {:on-refresh #(refresh)}]]]])


(defn Pagination
  []
  (let [data-records      (subscribe [::subs/data-records])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])
        total-elements    (:count @data-records)
        total-pages       (utils-general/total-pages total-elements @elements-per-page)]
    [uix/Pagination {:totalitems              total-elements
                     :totalPages              total-pages
                     :activePage              @page
                     :elementsperpage         @elements-per-page
                     :onElementsPerPageChange (ui-callback/value
                                                #(do (dispatch [::events/set-elements-per-page %])
                                                     (dispatch [::events/set-page 1])
                                                     (dispatch [::events/get-data-set])))
                     :onPageChange            (ui-callback/callback
                                                :activePage #(do (dispatch [::events/set-page %])
                                                                 (dispatch [::events/get-data-set])))}]))


(defn DataRecordRow
  [{:keys [id name description tags created timestamp bucket content-type infrastructure-service
           resource:deployment] :as _data-record}]
  (fn [_data-record]
    ^{:key id}
    (let [uuid            (utils-general/id->uuid id)
          deployment-uuid (utils-general/id->uuid resource:deployment)
          is-uuid         (utils-general/id->uuid infrastructure-service)]
      [ui/TableRow
       [ui/TableCell name]
       [ui/TableCell description]
       [ui/TableCell (values/format-created created)]
       [ui/TableCell timestamp]
       [ui/TableCell bucket]
       [ui/TableCell content-type]
       [ui/TableCell [uix/Tags tags]]
       [ui/TableCell (values/as-link infrastructure-service :label is-uuid :page "infrastructure-service")]
       [ui/TableCell (values/as-link resource:deployment :label deployment-uuid)]
       [ui/TableCell (values/as-link id :label uuid)]])))


(defn DataRecordTable
  [Pagination]
  (let [tr           (subscribe [::i18n-subs/tr])
        data-records (subscribe [::subs/data-records])]
    [:<>
     [SearchHeader refresh [DataRecordFilter]]
     [ui/Table {:compact "very", :selectable true}
      [ui/TableHeader
       [ui/TableRow
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
       (for [{:keys [id] :as dr} (:resources @data-records)]
         ^{:key id}
         [DataRecordRow dr])]]
     Pagination]))


(defn DataRecordCard
  [{:keys [id name description timestamp _bucket tags _data-object infrastructure-service object] :as data-record}]
  (let [tr                        (subscribe [::i18n-subs/tr])
        data-objects              (subscribe [::subs/data-objects])
        nuvla-api                 (subscribe [::main-subs/nuvla-api])
        data-object-id            (:resource:object data-record)
        data-object               (get @data-objects data-object-id)
        filename                  object
        resource-deployment-id    (values/resource->id (:resource:deployment data-record))
        infrastructure-service-id (values/resource->id infrastructure-service)]
    ^{:key id}
    [uix/Card
     {:header      [:span [:p {:style {:overflow      "hidden",
                                       :text-overflow "ellipsis",
                                       :max-width     "20ch"}} (or name timestamp)]]
      :meta        (str (@tr [:created]) " " (-> timestamp time/parse-iso8601 time/ago))
      :description (utils-general/truncate description 60)
      :content     [:<>
                    (when resource-deployment-id
                      [:div {:style {:padding "10px 0 0 0"}}
                       [ui/Icon {:name "rocket"}]
                       (values/as-link resource-deployment-id :page "deployment" :label (@tr [:deployment]))])
                    (when infrastructure-service-id
                      [:div {:style {:padding "10px 0 0 0"}}
                       [ui/Icon {:name "cloud"}]
                       [values/as-link infrastructure-service-id :page "infrastructures" :label (@tr [:storage-service])]])]
      :tags        tags
      :button      (when data-object
                     [ui/Button {:color  "green"
                                 :target "_blank"
                                 :rel    "noreferrer"}
                      [:a {:href     (str @nuvla-api "/" data-object-id "/download")
                           :target   "_blank"
                           :style    {:color "white"}
                           :download (when filename filename)} [ui/Icon {:name "cloud download"}] " " (@tr [:download])]])}]))


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


(defn Summary
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        dataset        (subscribe [::subs/data-set])
        device         (subscribe [::main-subs/device])
        editable?      (subscribe [::subs/editable?])
        {:keys [id created created-by updated data-record-filter module-filter tags]} @dataset
        resolved-owner (subscribe [::session-subs/resolve-user created-by])]
    [ui/Grid {:columns   (if (contains? #{:wide-screen} @device) 2 1)
              :stackable true
              :padded    true
              :centered  true}
     [ui/GridRow {:centered true}
      [ui/GridColumn

       [ui/SegmentGroup {:style  {:display    "flex", :justify-content "space-between",
                                  :background "#f3f4f5"}
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
               [components/EditableTags @dataset #(dispatch [::events/edit id {:tags %}
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
            [ui/TableCell (str/capitalize (@tr [:module-filter]))]
            [ui/TableCell module-filter]]
           [ui/TableRow
            [ui/TableCell "Id"]
            [ui/TableCell (when (some? id) [values/as-link id :label (subs id 11)])]]]]]]]]]))


(defn DataRecordCards
  [Pagination]
  (let [data-records (subscribe [::subs/data-records])
        device       (subscribe [::main-subs/device])]
    [:<>
     [ui/Grid {:columns   (columns-per-device @device)
               :stackable true
               :padded    true
               :centered  true}
      [ui/GridRow {:centered true}
       [ui/GridColumn
        [SearchHeader refresh [DataRecordFilter]]]]
      [ui/GridRow {:centered true}
       [ui/GridColumn
        [ui/Segment style/basic
         [ui/CardGroup {:centered    true
                        :itemsPerRow (cards-per-device @device)
                        :stackable   true}
          (for [data-record (:resources @data-records)]
            ^{:key (:id data-record)}
            [DataRecordCard data-record])]]]]]
     Pagination]))


(defn DataSet
  [dataset-id]
  (dispatch [::events/set-data-set-id dataset-id])
  (refresh)
  (let [tr       (subscribe [::i18n-subs/tr])
        data-set (subscribe [::subs/data-set])
        name     (:name @data-set)]
    (fn [dataset-id]
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
         [DataRecordCards [Pagination]]]]])))
