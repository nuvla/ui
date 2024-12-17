(ns sixsq.nuvla.ui.common-components.job.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.job.events :as events]
            [sixsq.nuvla.ui.common-components.job.spec :as spec]
            [sixsq.nuvla.ui.common-components.job.subs :as subs]
            [sixsq.nuvla.ui.common-components.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.values :as values]))

(defn DefaultJobCell
  [{:keys [state status-message] :as _resource} last-line?]
  [:div {:style (cond-> {:white-space :pre
                         :overflow    :auto}
                        (= state "QUEUED") (assoc :display "none"))}
   [(uix/label-group-overflow-detector
      (fn []
        (if last-line?
          (some-> status-message str/split-lines last)
          status-message)))]])

(defmulti JobCell :action)

(defmethod JobCell :default
  [resource last-line?]
  [DefaultJobCell resource last-line?])

(defn JobRow
  [{:keys [id action state time-of-status-change updated progress return-code] :as resource}]
  [ui/TableRow
   [ui/TableCell {:verticalAlign "top"}
    [:dl {:style {:overflow-wrap         :anywhere
                  :display               :grid
                  :grid-template-columns "repeat(2,auto)"
                  :width                 "max-content"
                  :max-width             "100%"}}
     (for [[k v] [[:id [values/AsLink id
                        :label (general-utils/id->short-uuid id)]]
                  [:action (some-> action (str/replace #"_" " "))]
                  [:state state]
                  [:timestamp (or time-of-status-change
                                  updated)]
                  [:progress progress]
                  [:return-code return-code]]]
       ^{:key (str (:id resource) "_" k)}
       [:<>
        [:dt [:b [uix/TR k str/capitalize] ":"]]
        [:dd v]])]]
   [ui/TableCell {:verticalAlign "top"}
    [JobCell resource]]])

(defn JobsTable
  [_jobs]
  (dispatch [::pagination-plugin/change-page [::spec/pagination] 1])
  (fn [{:keys [resources] :as jobs-data}]
    (let [{jobs-count :count} jobs-data]
      (if (empty? resources)
        [uix/MsgNoItemsToShow]
        [ui/TabPane
         [ui/Table {:striped true
                    :fixed   true}
          [ui/TableHeader
           [ui/TableRow
            [ui/TableHeaderCell {:style {:width "26em"}} [uix/TR :job str/capitalize]]
            [ui/TableHeaderCell [uix/TR :details str/capitalize]]]]
          [ui/TableBody
           (for [resource resources]
             ^{:key (:id resource)}
             [JobRow resource])]]
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

(defn ErrorJobsMessage
  [_job-subs _set-active-tab-event _job-tab _on-click]
  (let [tr                        (subscribe [::i18n-subs/tr])
        most-recent-job-dismissed (r/atom nil)]
    (fn [job-subs set-active-tab-event job-tab on-click]
      (let [all-jobs                       (->> @(subscribe [job-subs])
                                                :resources)
            most-recent-failed-job         (->> all-jobs
                                                (filter #(= "FAILED" (:state %)))
                                                (sort-by :updated >)
                                                (first))
            action-of-interest?            (fn [a] (not (contains? #{"dct_check"} a)))
            successful-jobs-after-failure? (filter (fn [{:keys [updated state action] :as _job}]
                                                     (and (> updated
                                                             (:updated most-recent-failed-job))
                                                          (= state
                                                             "SUCCESS")
                                                          (action-of-interest? action)))
                                                   all-jobs)]
        [:<>
         (when (and most-recent-failed-job
                    (empty? successful-jobs-after-failure?)
                    (not= (:id most-recent-failed-job) @most-recent-job-dismissed)) ;; it is a newer failed job that we haven't dismissed yet
           (let [{:keys [id action]} most-recent-failed-job]
             ^{:key id}
             [ui/Message {:style      {:cursor "pointer"}
                          :error      true
                          :on-dismiss #(do (reset! most-recent-job-dismissed id)
                                           (.stopPropagation %))
                          :on-click   (or on-click
                                          #(dispatch [set-active-tab-event job-tab]))}
              [ui/MessageHeader
               (str (str/capitalize (@tr [:job])) " " action " " (@tr [:failed]))]
              [ui/MessageContent
               [:div {:style {:margin-top "0.5em"}}
                [JobCell most-recent-failed-job true]]]]))]))))
