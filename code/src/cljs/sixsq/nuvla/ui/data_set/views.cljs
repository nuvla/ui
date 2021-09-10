(ns sixsq.nuvla.ui.data-set.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch dispatch-sync subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.data-set.events :as events]
    [sixsq.nuvla.ui.data-set.subs :as subs]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as components]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.session.subs :as session-subs]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as values]))


(defn refresh
  []
  (dispatch [::events/refresh]))


(defn SearchHeader
  [refresh-fn full-search-event full-text-search-subs]
  (let [tr          (subscribe [::i18n-subs/tr])
        time-period (subscribe [::subs/time-period])
        locale      (subscribe [::i18n-subs/locale])
        full-text   (subscribe [full-text-search-subs])]
    (fn []
      (let [[time-start time-end] @time-period
            date-format "MMMM DD, YYYY HH:mm"
            time-format "HH:mm"]
        [ui/Form
         [ui/FormGroup {:widths 3}
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
          [ui/FormField
           [components/SearchInput
            {:on-change     (ui-callback/input-callback
                              #(dispatch [full-search-event %]))
             :default-value @full-text}]]]]))))


(defn MenuBar
  []
  [:div
   [components/StickyBar
    [ui/Menu {:attached "top", :borderless true}
     [components/RefreshMenu
      {:on-refresh #(refresh)}]]]])


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
    ()
    ^{:key id}
    [uix/Card
     {:header      [:span [:p {:style {:overflow      "hidden",
                                       :text-overflow "ellipsis",
                                       :max-width     "20ch"}} (or name timestamp)]]
      :meta        (str (@tr [:created]) " " (-> timestamp time/parse-iso8601 time/ago))
      :description (general-utils/truncate description 60)
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


(defn Summary
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        dataset        (subscribe [::subs/data-set])
        device         (subscribe [::main-subs/device])
        {:keys [id name description created created-by updated data-record-filter module-filter tags]} @dataset
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
            [ui/TableCell name]]
           [ui/TableRow
            [ui/TableCell (str/capitalize (@tr [:description]))]
            [ui/TableCell description]]
           (when tags
             [ui/TableRow
              [ui/TableCell (str/capitalize (@tr [:tags]))]
              [ui/TableCell
               [uix/Tags {:tags tags}]]])
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
  []
  (let [data-records (subscribe [::subs/data-records])
        device       (subscribe [::main-subs/device])]
    [ui/Grid {:columns   (columns-per-device @device)
              :stackable true
              :padded    true
              :centered  true}
     [ui/GridRow {:centered true}
      [ui/GridColumn
       [SearchHeader refresh ::events/set-full-text-search ::subs/full-text-search]]]
     [ui/GridRow {:centered true}
      [ui/GridColumn
       [ui/Segment style/basic
        [ui/CardGroup {:centered    true
                       :itemsPerRow (cards-per-device @device)
                       :stackable   true}
         (for [data-record (:resources @data-records)]
           ^{:key (:id data-record)}
           [DataRecordCard data-record])]]]]]))


(defn Pagination
  []
  (let [data-records      (subscribe [::subs/data-records])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])
        total-elements    (:count @data-records)
        total-pages       (general-utils/total-pages total-elements @elements-per-page)]
    [uix/Pagination {:totalitems   total-elements
                     :totalPages   total-pages
                     :activePage   @page
                     :onPageChange (ui-callback/callback
                                     :activePage #(dispatch [::events/set-page %]))}]))


(defn DataSet
  [dataset-id]
  (dispatch-sync [::events/set-loading? true])
  (dispatch [::events/set-data-set-id dataset-id])
  (refresh)
  (let [tr       (subscribe [::i18n-subs/tr])
        data-set (subscribe [::subs/data-set])
        loading? (subscribe [::subs/loading?])
        name     (:name @data-set)]
    (fn [dataset-id]
      [components/LoadingContent @loading?
       [components/DimmableContent dataset-id
        [:<>
         [components/NotFoundPortal
          ::subs/not-found?
          :no-data-record-message-header
          :no-data-record-message-content]
         [ui/Segment style/basic
          [uix/PageHeader "database" (str name " " (@tr [:data-set]))]
          [MenuBar dataset-id]
          [Summary]
          [DataRecordCards]
          [Pagination]]]]])))

