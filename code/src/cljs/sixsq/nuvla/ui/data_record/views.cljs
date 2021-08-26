(ns sixsq.nuvla.ui.data-record.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.data-record.events :as events]
    [sixsq.nuvla.ui.data-record.subs :as subs]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.general :as general-utils]))


(defn SearchHeader []
  (let [tr          (subscribe [::i18n-subs/tr])
        time-period (subscribe [::subs/time-period])
        locale      (subscribe [::i18n-subs/locale])
        full-text   (subscribe [::subs/full-text-search])]
    (fn []
      (let [[time-start time-end] @time-period]
        [ui/Form
         [ui/FormGroup {:widths 3}
          [ui/FormField
           ;; FIXME: Find a better way to set the field width.
           [ui/DatePicker {:custom-input     (r/as-element [ui/Input {:label (@tr [:from])
                                                                      :style {:min-width "25em"}}])
                           :selected         time-start
                           :start-date       time-start
                           :end-date         time-end
                           :max-date         time-end
                           :selects-start    true
                           :show-time-select true
                           :time-format      "HH:mm"
                           :time-intervals   15
                           :locale           @locale
                           :fixed-height     true
                           :date-format      "LLL"
                           :on-change        #(dispatch [::events/set-time-period [% time-end]])}]]
          ;; FIXME: Find a better way to set the field width.
          [ui/FormField
           [ui/DatePicker {:custom-input     (r/as-element [ui/Input {:label (@tr [:to])
                                                                      :style {:min-width "25em"}}])
                           :selected         time-end
                           :start-date       time-start
                           :end-date         time-end
                           :min-date         time-start
                           :max-date         (time/now)
                           :selects-end      true
                           :show-time-select true
                           :time-format      "HH:mm"
                           :time-intervals   15
                           :locale           @locale
                           :fixed-height     true
                           :date-format      "LLL"
                           :on-change        #(dispatch
                                                [::events/set-time-period [time-start %]])}]]
          [ui/FormField
           [main-components/SearchInput
            {:on-change     (ui-callback/input-callback
                              #(dispatch [::events/set-full-text-search %]))
             :default-value @full-text}]]]]))))


(defn MenuBar
  [dataset-id]
  [:div
   [main-components/StickyBar
    [ui/Menu {:attached "top", :borderless true}
     [main-components/RefreshMenu
      {:on-refresh #(dispatch [::events/get-data-set dataset-id])}]]]
   [ui/Segment
    [SearchHeader]]])


(defn DataRecordCard
  [{:keys [id name description timestamp _bucket tags _data-object infrastructure-service object] :as data-record}]
  (let [tr             (subscribe [::i18n-subs/tr])
        data-objects   (subscribe [::subs/data-objects])
        data-object-id (:resource:object data-record)
        data-object    (get @data-objects data-object-id)
        filename       object]
    ^{:key id}
    [uix/Card
     {:header      [:span [:p {:style {:overflow      "hidden",
                                       :text-overflow "ellipsis",
                                       :max-width     "20ch"}} (or name timestamp)]]
      :meta        (str (@tr [:created]) " " (-> timestamp time/parse-iso8601 time/ago))
      :description infrastructure-service
      :content     description
      :tags        tags
      :button      (when data-object
                     [ui/Button {:color   "green"
                                 :fluid   true
                                 :target  "_blank"
                                 :rel     "noreferrer"}
                      [:a {:href     (str data-object-id "/download")
                           :target   "_blank"
                           :style {:color "white"}
                           :download (when filename filename)
                           :fluid    true} [ui/Icon {:name "cloud download"}] " " (@tr [:download])]])}]))


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


(defn DataRecords
  [dataset-id]
  (dispatch [::events/get-data-set dataset-id])
  (let [data-set (subscribe [::subs/data-set])
        name     (:name @data-set)]
    [ui/Segment style/basic
     [uix/PageHeader "database" (str name " data records")]
     [MenuBar dataset-id]
     [DataRecordCards]
     [Pagination]]))
