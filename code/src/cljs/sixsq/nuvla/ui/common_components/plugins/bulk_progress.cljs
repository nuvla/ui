(ns sixsq.nuvla.ui.common-components.plugins.bulk-progress
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer
             [dispatch reg-event-db reg-event-fx reg-sub subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.session.utils :as session-utils]
            [sixsq.nuvla.ui.session.spec :as session-spec]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.values :as values]))

(s/def ::monitored-ids set?)
(s/def ::jobs map?)

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
    {::cimi-api-fx/operation [job-id "cancel" #(dispatch [::dismiss db-path job-id])]}))

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
  (fn [{{:keys [::session-spec/session] :as db} :db} [_ db-path]]
    (let [monitored-ids   (seq (get-in db (conj db-path ::monitored-ids)))
          target-resource (get-in db (conj db-path ::target-resource))]
      (when (or monitored-ids target-resource)
        {::cimi-api-fx/search
         [:job {:last    10000
                :orderby "created:desc"
                :filter  (general-utils/join-and
                           "action^='bulk'"
                           (str "created-by='" (session-utils/get-user-id session) "'")
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
  {"bulk_stop_deployment"       :bulk-stop-in-progress
   "bulk_update_deployment"     :bulk-update-in-progress
   "bulk_delete_deployment"     :bulk-delete-in-progress
   "bulk_deployment_set_start"  :depl-group-start-in-progress
   "bulk_deployment_set_update" :depl-group-update-in-progress
   "bulk_deployment_set_stop"   :depl-group-stop-in-progress
   "bulk_update_nuvlabox"       :bulk-update-in-progress})

(defn- ProgressLabel
  [{:keys [ACTIONS_CALLED ACTIONS_COUNT JOBS_DONE JOBS_COUNT SUCCESS FAILED]
    :as   _status_message}]
  [:span
   [:span {:style {:color "gray"}}
    (when (and ACTIONS_CALLED ACTIONS_COUNT)
      (str "Actions called " ACTIONS_CALLED "/" ACTIONS_COUNT ". "))]
   [:span (when (and JOBS_DONE (pos? JOBS_COUNT))
            (str "Jobs done " JOBS_DONE "/" JOBS_COUNT ". "))]])

(defn DotPopup
  [{:keys [id color msg on-mount]}]
  [ui/Popup {:on        "click"
             :hoverable true
             :on-mount  on-mount
             :flowing   true
             :trigger   (r/as-element
                          [:span [ui/Icon {:name  "circle"
                                           :link  true
                                           :color color}]])}
   [ui/PopupHeader
    [uix/TR :details str/capitalize]
    general-utils/nbsp
    [values/AsPageLink id
     :label [icons/ArrowRightFromBracketIcon]
     :new-tab true]]
   [ui/PopupContent
    (when msg
      [:div {:style {:white-space :pre
                     :overflow    :auto
                     :min-height  "10em"}}
       [:p {:style {:overflow   "auto"
                    :max-width  "60vw"
                    :max-height "20vw"}}
        (some-> msg (str/replace #"\n" "\n"))]])]])

(defn DotPopupController
  [opts]
  (let [job-child (r/atom nil)]
    (fn [{:keys [id job-id color status-message]}]
      (let [bootstrap-exception (get-in status-message [:BOOTSTRAP_EXCEPTIONS (keyword id)])]
        [DotPopup {:id       id
                   :color    color
                   :on-mount (fn []
                               (when-not bootstrap-exception
                                 (dispatch [::get-popup-content job-id id #(reset! job-child %)])))
                   :msg      (or bootstrap-exception
                                 (some-> @job-child :status-message))}]))))

(defn- GridColumnLinks
  [label color ids job-id status-message]
  (r/with-let [tr (subscribe [::i18n-subs/tr])]
    [ui/GridColumn
     [:h3 {:style {:color color}} (str (str/capitalize label) ": " (count ids))
      (when (seq ids)
        [uix/HelpPopup (@tr [:bulk-progress-help])])]
     (for [id ids]
       ^{:key (str "GridColumnLinks-" id)}
       [DotPopupController {:id             id
                            :job-id         job-id
                            :color          color
                            :status-message status-message}])]))

(defn- SuccessFailedLinks
  [job-id {:keys [QUEUED RUNNING SUCCESS FAILED]
           :as   status_message}]
  (r/with-let [tr (subscribe [::i18n-subs/tr])]
    (when (or (seq QUEUED)
              (seq RUNNING)
              (seq SUCCESS)
              (seq FAILED))
      [:<>
       [GridColumnLinks (@tr [:queued]) "black" QUEUED job-id status_message]
       [GridColumnLinks (@tr [:running]) "teal" RUNNING job-id status_message]
       [GridColumnLinks (@tr [:successes]) "green" SUCCESS job-id status_message]
       [GridColumnLinks (@tr [:failures]) "red" FAILED job-id status_message]])))

(defn- MonitoredJob
  [{:keys [db-path]} {:keys [id state status-message progress action]
                      :as   job}]
  (let [tr            @(subscribe [::i18n-subs/tr])
        on-dismiss    #(dispatch [::dismiss db-path id])
        {:keys [FAILED SUCCESS]
         :as   status-message} (when (not= state "FAILED")
                                 (try
                                   (general-utils/json->edn status-message)
                                   (catch :default _ nil)))
        some-fail?    (pos? (count FAILED))
        some-success? (pos? (count SUCCESS))
        completed?    (= progress 100)
        state-failed? (= state "FAILED")
        color         (cond
                        (and some-fail? some-success?) "yellow"
                        (or state-failed? some-fail?) "red"
                        :else "green")
        Header        [uix/TR
                       (or (job-action->header action)
                           (some-> action
                                   (str/replace #"_" " ")
                                   (general-utils/capitalize-first-letter)))]
        ProgressBar   (fn []
                        [ui/Progress
                         {:active   (not completed?)
                          :percent  progress
                          :progress true
                          :color    color
                          :size     "small"}
                         [ProgressLabel status-message]])]
    [ui/Modal {:trigger    (r/as-element
                             [ui/Message (cond-> {:style {:cursor "pointer"}}
                                                 completed? (assoc :on-dismiss on-dismiss))
                              [ui/MessageHeader Header]
                              [ui/MessageContent
                               [ProgressBar]]])
               :close-icon true}
     [ui/ModalHeader Header]
     [ui/ModalContent
      [ui/Grid {:stackable true}
       [ui/GridRow
        [ui/GridColumn
         [:h3 (str/capitalize (tr [:progress])) ": "]
         [ProgressBar]
         (when state-failed?
           [:p {:style {:color "red"}} status-message])]]
       [ui/GridRow {:columns 4}

        [SuccessFailedLinks id status-message]]]]
     (when (general-utils/can-operation? "cancel" job)
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
