(ns sixsq.nuvla.ui.job.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.job.events :as events]
    [sixsq.nuvla.ui.job.spec :as spec]
    [sixsq.nuvla.ui.job.subs :as subs]
    [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.values :as values]))

(defn job-map-to-row
  [{:keys [id action time-of-status-change state progress return-code
           status-message] :as _job}]
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
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [{:keys [resources count]}]
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
         [pagination-plugin/Pagination
          {:db-path      [::spec/pagination]
           :change-event [::events/get-jobs]
           :total-items  count}]]))))

(defn jobs-section
  []
  (let [tr   (subscribe [::i18n-subs/tr])
        jobs (subscribe [::subs/jobs])
        {:keys [count]} @jobs]
    {:menuItem
     {:content (r/as-element
                 [:span (str/capitalize (@tr [:jobs]))
                  (when (pos? count) [ui/Label {:circular true
                                                :size     "mini"
                                                :attached "top right"}
                                      count])])
      :key     :jobs
      :icon    "clipboard list"}
     :render #(r/as-element [JobsTable @jobs])}))

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
