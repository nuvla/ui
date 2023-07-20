(ns sixsq.nuvla.ui.job.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.job.events :as events]
            [sixsq.nuvla.ui.job.spec :as spec]
            [sixsq.nuvla.ui.job.subs :as subs]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.plugins.table :refer [Table]]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.values :as values]))


(defn JobsTable
  [_jobs]
  (fn [{:keys [resources count]}]
    (if (empty? resources)
      [uix/WarningMsgNoElements]
      [ui/TabPane
       [Table {:columns
               [{:field-key :jobs
                 :accessor  :id
                 :cell      (fn [{id :cell-data}] [values/AsLink id :label (general-utils/id->short-uuid id)])}
                {:field-key :action}
                {:field-key :timestamp
                 :accessor  :time-of-status-change}
                {:field-key :state}
                {:field-key :progress}
                {:field-key :return-code}
                {:field-key  :message
                 :accessor   :status-message
                 :cell (fn [{{:keys [state]} :row-data
                             :keys [cell-data]}]
                         (if (= state "QUEUED")
                           [:span {:style {:display "none"}} cell-data]
                           cell-data))
                 :cell-props {:style {:white-space "pre"}}}]
               :rows resources}]
       [pagination-plugin/Pagination
        {:db-path      [::spec/pagination]
         :change-event [::events/get-jobs]
         :total-items  count}]])))

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
      :icon    icons/i-list}
     :render #(r/as-element [JobsTable @jobs])}))

(def filtered-actions #{"dct_check" "stop_deployment_set" "start_deployment_set"})

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
        error    (or (= "FAILED" state)
                     (and (= "ERROR" resource-state)
                          (not= state "QUEUED")))]
    (when (and last-job (< progress 100))
      [ui/Segment
       [ui/Progress {:active   true
                     :label    message
                     :percent  progress
                     :progress true
                     :size     "small"
                     :error    error
                     :class    ["green"]}]])))
