(ns sixsq.nuvla.ui.common-components.plugins.bulk-progress
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer
             [dispatch reg-event-db reg-event-fx reg-sub subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.config :as config]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.utils :refer [pathify]]
            [sixsq.nuvla.ui.session.utils :as session-utils]
            [sixsq.nuvla.ui.session.spec :as session-spec]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))

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
  {"bulk_stop_deployment"         :bulk-stop-in-progress
   "bulk_update_deployment"       :bulk-update-in-progress
   "bulk_force_delete_deployment" :bulk-force-delete-in-progress
   "bulk_deployment_set_start"    :depl-group-start-in-progress
   "bulk_deployment_set_update"   :depl-group-update-in-progress
   "bulk_deployment_set_stop"     :depl-group-stop-in-progress
   "bulk_update_nuvlabox"         :bulk-update-in-progress})

(defn- MonitoredJob
  [{:keys [db-path]} {:keys [id state status-message progress action]
                      :as   _job}]
  (let [tr            @(subscribe [::i18n-subs/tr])
        on-dismiss    #(dispatch [::dismiss db-path id])
        {:keys [FAILED SUCCESS]
         :as   status-message} (when (not= state "FAILED")
                                 (general-utils/json->edn status-message))
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
        ProgressBar   (fn [label]
                        [ui/Progress
                         (cond->
                           {:active   (not completed?)
                            :percent  progress
                            :progress true
                            :color    color
                            :size     "small"}
                           label (assoc :label label
                                        :style {:cursor "pointer"}))])]
    [ui/Message (when completed? {:on-dismiss on-dismiss})
     [ui/MessageHeader Header]
     [ui/MessageContent
      [:br]
      [ui/Modal {:trigger    (r/as-element
                               [:div [ProgressBar (tr [:click-for-details])]])
                 :close-icon true}
       [ui/ModalHeader Header]
       [ui/ModalContent
        [:h3 (str (str/capitalize (tr [:progress])) ":")]
        [ProgressBar]
        (when state-failed?
          [:p status-message])
        (when (seq FAILED)
          [:<>
           [:h3 (str (str/capitalize (tr [:failed])) ":")]
           [ui/ListSA
            (for [failed-id FAILED]
              ^{:key failed-id}
              [ui/ListItem
               [ui/ListContent
                [ui/ListHeader
                 {:as       :a
                  :href     (pathify [config/base-path failed-id])
                  :target   "_blank"
                  :on-click (fn [event]
                              (dispatch [::routing-events/navigate failed-id])
                              (.preventDefault event))} failed-id]
                [ui/ListDescription
                 (get-in status-message
                         [:bootstrap-exceptions (keyword failed-id)])]]])]])
        (when (seq SUCCESS)
          [:<>
           [:h3 (str (str/capitalize (tr [:success])) ":")]
           [ui/ListSA
            (for [success-id SUCCESS]
              ^{:key success-id}
              [ui/ListItem
               [ui/ListContent
                [ui/ListHeader [uix/Link success-id success-id]]]])]])
        ]]]]))

(defn MonitoredJobs
  [{:keys [db-path] :as _opts}]
  (dispatch [::start-polling db-path])
  (let [jobs (subscribe [::sorted-jobs db-path])]
    (fn [opts]
      [:<>
       (doall
         (for [{:keys [id] :as job} @jobs]
           ^{:key id}
           [MonitoredJob opts job]))])))

(s/fdef MonitoredJobs
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path])))
