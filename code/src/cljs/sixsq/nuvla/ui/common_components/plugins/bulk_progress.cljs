(ns sixsq.nuvla.ui.common-components.plugins.bulk-progress
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer
             [dispatch reg-event-db reg-event-fx reg-sub subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.common-components.plugins.table-refactor :as table-refactor]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.session.spec :as session-spec]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.values :as values]))

(s/def ::monitored-ids set?)
(s/def ::jobs map?)

(def ^:const state-queued "QUEUED")
(def ^:const state-failed "FAILED")
(def ^:const state-canceled "CANCELED")
(def ^:const state-success "SUCCESS")
(def ^:const state-running "RUNNING")
(def ^:const action-cancel "cancel")

(defn job-completed?
  [{:keys [state] :as _job}]
  (boolean (#{state-success state-failed state-canceled} state)))

(defn job-running?
  [{:keys [state] :as _job}]
  (= state state-running))

(defn job-queued?
  [{:keys [state] :as _job}]
  (= state state-queued))

(defn job-failed?
  [{:keys [state] :as _job}]
  (= state state-failed))

(defn build-spec
  [& {:keys [target-resource]}]
  {::monitored-ids   #{}
   ::jobs            {}
   ::target-resource target-resource})

(reg-event-db
  ::dismiss
  (fn [db [_ db-path job-id]]
    (-> db
        (update-in (conj db-path ::monitored-ids) disj job-id)
        (update-in (conj db-path ::jobs) dissoc job-id))))

(reg-event-fx
  ::cancel
  (fn [_ [_ db-path job-id]]
    {::cimi-api-fx/operation [job-id action-cancel #(dispatch [::dismiss db-path job-id])]}))

(reg-event-db
  ::set-target-resource
  (fn [db [_ db-path target-resource]]
    (assoc-in db (conj db-path ::target-resource) target-resource)))

(reg-event-db
  ::set-jobs
  (fn [db [_ db-path resources]]
    (let [jobs-path          (conj db-path ::jobs)
          monitored-ids-path (conj db-path ::monitored-ids)
          jobs               (->> resources
                                  (map (juxt :id identity))
                                  (into {}))]
      (-> db
          (assoc-in jobs-path jobs)
          (assoc-in monitored-ids-path (set (keys jobs)))))))

(reg-event-fx
  ::search-jobs
  (fn [{db :db} [_ db-path]]
    (let [monitored-ids   (seq (get-in db (conj db-path ::monitored-ids)))
          target-resource (get-in db (conj db-path ::target-resource))]
      (when (or monitored-ids target-resource)
        {::cimi-api-fx/search
         [:job {:last    10000
                :orderby "created:desc"
                :filter  (general-utils/join-and
                           "action^='bulk'"
                           (general-utils/join-or
                             (when monitored-ids
                               (general-utils/filter-eq-ids monitored-ids))
                             (when target-resource
                               (general-utils/join-and
                                 (str "target-resource/href='" target-resource "'")
                                 "progress<100"))))}
          #(dispatch [::set-jobs db-path (:resources %)])]}))))


(reg-event-fx
  ::get-popup-content
  (fn [{{:keys [::session-spec/session] :as db} :db} [_ job-id id callback-fn]]
    {::cimi-api-fx/search
     [:job {:last   1
            :filter (general-utils/join-and
                      (str "parent-job='" job-id "'")
                      (str "target-resource/href='" id "'"))}
      #(callback-fn (-> % :resources first))]}))

(reg-event-fx
  ::start-polling
  (fn [_ [_ db-path]]
    {:fx [[:dispatch [::main-events/action-interval-start
                      {:id        (str db-path)
                       :frequency 10000
                       :event     [::search-jobs db-path]}]]]}))

(reg-sub
  ::sorted-jobs
  (fn [db [_ db-path]]
    (vals (get-in db (conj db-path ::jobs)))))

(def job-action->header
  {"bulk_stop_deployment"       :bulk-stop
   "bulk_update_deployment"     :bulk-update
   "bulk_delete_deployment"     :bulk-delete
   "bulk_deployment_set_start"  :depl-group-start
   "bulk_deployment_set_update" :depl-group-update
   "bulk_deployment_set_stop"   :depl-group-stop
   "bulk_update_nuvlabox"       :bulk-update})

(defn append-parsed-job-status-message
  [{:keys [status-message] :as job}]
  (when-not (job-failed? job)
    (try
      (assoc job :parsed-status-message (general-utils/json->edn status-message))
      (catch :default _ nil))))

(defn executed-count
  [{:keys [success_count failed_count skipped_count]
    :as   _parsed-status-message}]
  (+ success_count failed_count skipped_count))

(defn error-count
  [{:keys [failed_count skipped_count]
    :as   _parsed-status-message}]
  (+ failed_count skipped_count))

(defn JobProgress
  [{progress                                          :progress
    {:keys [total_actions] :as parsed-status-message} :parsed-status-message :as job-parsed}]
  [ui/Progress {:active   (job-running? job-parsed)
                :percent  progress
                :progress true
                :size     "small"
                :style    {:margin-top 10}}
   [:span (executed-count parsed-status-message)
    " Executed Actions " " / " total_actions " Total Actions"]])

(defn JobCounters
  [{{:keys [success_count failed_count skipped_count total_actions
            running_count queued_count]
     :as   parsed-status-message} :parsed-status-message :as _job-parsed}]
  [ui/Grid {:stackable true}
   [ui/GridRow {:columns 4}
    [ui/GridColumn
     [:b "Total Actions: " total_actions]]
    [ui/GridColumn
     [:b "Executed Actions: " (executed-count parsed-status-message)]
     [:div [icons/CircleIcon {:color "green"}] "Completed" ": " success_count]
     [:div [icons/CircleIcon {:color "orange"}] "Skipped" ": " skipped_count]
     [:div [icons/CircleIcon {:color "red"}] "Failed" ": " failed_count]]
    [ui/GridColumn
     [:b "Ongoing Actions: " (+ queued_count running_count)]
     [:div [icons/CircleIcon] "Queued" ": " queued_count]
     [:div [icons/CircleIcon {:color "yellow"}] "Running" ": " running_count]]
    (let [er-count (error-count parsed-status-message)]
      (when (and (pos? total_actions) (int? er-count))
        [ui/GridColumn
         [:b "Error Rate: " (or (general-utils/to-fixed (* (/ er-count total_actions) 100)) 0) "%"]
         [uix/HelpPopup
          (str "(" failed_count " " "Failed" " + " skipped_count " " "Skipped" ") / "
               total_actions " " "Total actions")]]))]])

(defn JobErrorBreakdownByReason
  [selected-reason {{:keys [total_actions error_reasons]} :parsed-status-message :as _parsed-job}]
  (r/with-let [!table-data (r/atom [])]
    (when @selected-reason
      (reset! !table-data
              (some #(when (= (:reason %) @selected-reason)
                       (mapv (fn [{:keys [count] :as entry}]
                               (assoc entry :PercentTotal (general-utils/to-fixed (* (/ count total_actions) 100)))
                               ) (:data %))
                       ) error_reasons))
      [:div {:style {:margin-top 20}}
       [:span
        [:b "By reason: "]
        (str @selected-reason " ")
        [icons/CloseIcon {:color :red
                          :style {:cursor :pointer} :on-click #(reset! selected-reason nil)} "Close"]]
       [table-refactor/TableController
        {:!enable-column-customization? (r/atom false)
         :!enable-sorting?              (r/atom false)
         :!enable-pagination?           (r/atom true)
         :!pagination                   (r/atom {:page-index 0
                                                 :page-size  10})
         :!columns                      (r/atom [{::table-refactor/field-key      :id
                                                  ::table-refactor/header-content "Resource"
                                                  ::table-refactor/field-cell     (fn [id row-data]
                                                                                    [values/AsPageLink id
                                                                                     :label (:name row-data)
                                                                                     :new-tab true])}
                                                 {::table-refactor/field-key      :count
                                                  ::table-refactor/header-content "Count"}
                                                 {::table-refactor/field-key      :PercentTotal
                                                  ::table-refactor/header-content "% of Total"}
                                                 {::table-refactor/field-key      :message
                                                  ::table-refactor/header-content "Message"}])
         :!default-columns              (r/atom [:id :name :count :PercentTotal :message])
         :row-id-fn                     :id
         :!data                         !table-data}]])))

(defn JobErrorBreakdown
  [{{:keys [total_actions error_reasons] :as parsed-status-message} :parsed-status-message :as parsed-job}]
  (r/with-let [selected-reason (r/atom nil)
               !table-data     (r/atom [])]
    (when (pos? (error-count parsed-status-message))
      (reset! !table-data
              (mapv (fn [{:keys [reason category count]}]
                      {:Reason       reason
                       :Count        count
                       :ErrorType    category
                       :PercentTotal (general-utils/to-fixed (* (/ count total_actions) 100))}) error_reasons))
      [:div
       [ui/Divider]
       [:b "Errors Breakdown:"
        [uix/HelpPopup "Click on row to check details by Reason"]]
       [table-refactor/TableController
        {:on-row-click                  #(reset! selected-reason (:Reason %))
         :!enable-pagination?           (r/atom true)
         :!pagination                   (r/atom {:page-index 0
                                                 :page-size  10})
         :!enable-column-customization? (r/atom false)
         :!enable-sorting?              (r/atom false)
         :!columns                      (r/atom [{::table-refactor/field-key      :Reason
                                                  ::table-refactor/header-content "Reason"}
                                                 {::table-refactor/field-key      :ErrorType
                                                  ::table-refactor/header-content "Type"}
                                                 {::table-refactor/field-key      :Count
                                                  ::table-refactor/header-content "Count"}
                                                 {::table-refactor/field-key      :PercentTotal
                                                  ::table-refactor/header-content "% of Total"}])
         :!default-columns              (r/atom [:Reason :Count :ErrorType :PercentTotal])
         :row-id-fn                     :Reason
         :!data                         !table-data}]
       [JobErrorBreakdownByReason selected-reason parsed-job]])))

(defn JobDetail
  [{:keys [status-message] :as job-parsed}]
  (if (job-failed? job-parsed)
    [:p {:style {:color "red"}} status-message]
    [:<>
     [JobCounters job-parsed]
     [JobProgress job-parsed]
     [JobErrorBreakdown job-parsed]]))

(defn- MonitoredJob
  [{:keys [db-path]} {:keys [id action] :as job}]
  (let [tr         @(subscribe [::i18n-subs/tr])
        on-dismiss #(dispatch [::dismiss db-path id])
        completed? (job-completed? job)
        Header     (str (or (tr [(job-action->header action)])
                            (some-> action
                                    (str/replace #"_" " ")
                                    (general-utils/capitalize-first-letter)))
                        " "
                        (tr [(cond
                               (job-running? job) :in-progress
                               (job-queued? job) :queued
                               completed? :completed)]))
        parsed-job (append-parsed-job-status-message job)]
    [ui/Modal {:trigger    (r/as-element
                             [ui/Message (cond-> {:style {:cursor "pointer"}}
                                                 completed? (assoc :on-dismiss on-dismiss))
                              [ui/MessageHeader Header]
                              [ui/MessageContent
                               [JobProgress parsed-job]]])
               :close-icon true}
     [ui/ModalHeader Header]
     [ui/ModalContent {:scrolling true}
      [JobDetail parsed-job]]
     (when (general-utils/can-operation? action-cancel job)
       [ui/ModalActions
        [uix/ButtonAskingForConfirmation
         {:icon              icons/i-ban-full
          :update-event      [::cancel db-path id]
          :text              (str/capitalize (tr [:cancel]))
          :action-aria-label (tr [:cancel])}]])]))

(defn MonitoredJobs
  [{:keys [db-path] :as _opts}]
  (dispatch [::start-polling db-path])
  (let [jobs (subscribe [::sorted-jobs db-path])]
    (fn [opts]
      [:div {:style {:margin-bottom "1em"}}
       (for [{:keys [id] :as job} @jobs]
         ^{:key id}
         [MonitoredJob opts job])])))

(s/fdef MonitoredJobs
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path])))
