(ns sixsq.nuvla.ui.job.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.job.events :as events]
    [sixsq.nuvla.ui.job.subs :as subs]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as values]))


(defn job-map-to-row
  [{:keys [id action time-of-status-change state progress return-code status-message] :as _job}]
  [ui/TableRow
   [ui/TableCell [values/as-link id :label (general-utils/id->short-uuid id)]]
   [ui/TableCell action]
   [ui/TableCell time-of-status-change]
   [ui/TableCell state]
   [ui/TableCell progress]
   [ui/TableCell return-code]
   [ui/TableCell {:style {:white-space "pre"}} status-message]])


(defn JobsTable
  [_jobs]
  (let [tr                (subscribe [::i18n-subs/tr])
        elements-per-page (subscribe [::subs/jobs-per-page])
        page              (subscribe [::subs/job-page])]
    (fn [{:keys [resources] :as jobs}]
      (let [total-elements (get jobs :count 0)
            total-pages    (general-utils/total-pages total-elements @elements-per-page)]
        (if (empty? resources)
          [uix/WarningMsgNoElements]
          [ui/TabPane
           [ui/Table {:basic "very"}
            [ui/TableHeader
             [ui/TableRow
              [ui/TableHeaderCell [:span (@tr [:job])]]
              [ui/TableHeaderCell [:span (@tr [:action])]]
              [ui/TableHeaderCell [:span (@tr [:timestamp])]]
              [ui/TableHeaderCell [:span (@tr [:state])]]
              [ui/TableHeaderCell [:span (@tr [:progress])]]
              [ui/TableHeaderCell [:span (@tr [:return-code])]]
              [ui/TableHeaderCell [:span (@tr [:message])]]]]
            [ui/TableBody
             (for [{:keys [id] :as job} resources]
               ^{:key id}
               [job-map-to-row job])]]

           [uix/Pagination {:totalPages   total-pages
                            :activePage   @page
                            :onPageChange (ui-callback/callback
                                            :activePage #(dispatch [::events/set-job-page %]))}]])))))


(defn jobs-section
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        jobs      (subscribe [::subs/jobs])
        {:keys [resources]} @jobs
        job-count (count resources)]
    {:menuItem {:content (r/as-element [:span (str/capitalize (@tr [:jobs]))
                                        (when (> job-count 0) [ui/Label {:circular true
                                                                         :size     "mini"
                                                                         :attached "top right"}
                                                               job-count])])
                :key     :jobs
                :icon    "clipboard list"}
     :render   #(r/as-element [JobsTable @jobs])}))


(def filtered-actions #{"dct_check"})


(defn filter-actions
  "Filter out actions included in filtered-actions"
  [action]
  (not (boolean (some (set [action]) filtered-actions))))


(defn ProgressJobAction
  [resource-state]
  (let [jobs     (subscribe [::subs/jobs])
        filtered (filter #(filter-actions (:action %)) (:resources @jobs))
        last-job (first filtered)
        {:keys [action progress state]} last-job
        message  (str/replace (str/lower-case (str action ": " state)) #"_" " ")
        error    (or (= "FAILED" state) (= "ERROR" resource-state))]
    (when (and last-job (< progress 100))
      [ui/Segment
       [ui/Progress {:active   true
                     :label    message
                     :percent  progress
                     :progress true
                     :size     "small"
                     :error    error
                     :class    ["green"]}]])))
