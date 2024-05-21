(ns sixsq.nuvla.ui.common-components.job.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.job.events :as events]
            [sixsq.nuvla.ui.common-components.job.spec :as spec]
            [sixsq.nuvla.ui.common-components.job.subs :as subs]
            [sixsq.nuvla.ui.common-components.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.common-components.plugins.table :refer [Table]]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.values :as values]))

(defn JobsMessageCell [{{:keys [state]} :row-data
                        :keys           [cell-data]}]
  [:span {:style (cond-> {:white-space "pre"
                          :max-width   :unset
                          :overflow    :auto
                          :display     :block}
                         (= state "QUEUED")
                         (assoc :display "none"))}
   [uix/TruncateContent {:content cell-data :length 300}]])

(defn JobsTable
  [_jobs]
  (fn [{:keys [resources] :as jobs-data}]
    (let [{jobs-count :count} jobs-data]
      (if (empty? resources)
        [uix/WarningMsgNoElements]
        [ui/TabPane
         [ui/Table {:style   {:table-layout :fixed}
                    :striped true}
          [ui/TableHeader
           [ui/TableRow
            [ui/TableHeaderCell {:width 5}]
            [ui/TableHeaderCell {:width 11} "Message"]]]
          [ui/TableBody
           (for [resource resources]
             ^{:key (:id resource)}
             [ui/TableRow
              [ui/TableCell {:verticalAlign "top"}
               [:dl {:style {:overflow              :auto
                             :display               :grid
                             :grid-template-columns "repeat(2,auto)"
                             :width                 "max-content"
                             :max-width             "100%"}}
                [:dt [:b "Job: "]]
                [:dd [values/AsLink (:id resource) :label (general-utils/id->short-uuid (:id resource))]]
                [:dt [:b "Action: "]]
                [:dd (:action resource)]
                [:dt [:b "State: "]]
                [:dd (:state resource)]
                [:dt [:b "Timestamp: "]]
                [:dd (:time-of-status-change resource)]
                [:dt [:b "Progress: "]]
                [:dd (:progress resource)]
                [:dt [:b "Return code: "]]
                [:dd (:return-code resource)]]]
              [ui/TableCell {:verticalAlign "top"}
               [:div {:style (cond-> {:white-space :pre
                                      :overflow    :auto}
                                     (= (:state resource) "QUEUED") (assoc :display "none"))}
                [uix/TruncateContent {:content (str/replace (:status-message resource) #"\\n" "\n") :length 300}]]]])]]
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
                  {:field-key :message
                   :accessor  :status-message
                   :cell      (fn [cell-data] [JobsMessageCell cell-data])}]
                 :rows resources}]
         [pagination-plugin/Pagination
          {:db-path      [::spec/pagination]
           :change-event [::events/get-jobs]
           :total-items  jobs-count}]]))))

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
