(ns sixsq.nuvla.ui.plugins.bulk-progress
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.history.events :as history-events]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.utils.general :as utils-general]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.history.views :as history-views]
            [sixsq.nuvla.ui.utils.general :as general-utils]))

(s/def ::monitored-ids set?)
(s/def ::resources map?)

(defn build-spec
  []
  {::monitored-ids #{}
   ::resources     {}})

(reg-event-db
  ::dissmiss
  (fn [db [_ db-path job-id]]
    (-> db
        (update-in (conj db-path ::monitored-ids) disj job-id)
        (update-in (conj db-path ::resources) dissoc job-id))))

(reg-event-db
  ::monitor
  (fn [db [_ db-path job-id]]
    (-> db
        (update-in (conj db-path ::monitored-ids) conj job-id)
        (assoc-in (conj db-path ::resources) job-id))))

(reg-event-db
  ::set-jobs
  (fn [db [_ db-path resources]]
    (let [monitored-ids-path (conj db-path ::monitored-ids)
          finished-jobs-ids  (->> resources
                                  (remove #(-> % :progress (< 100)))
                                  (map :id))
          monitored-ids      (get-in db monitored-ids-path)
          ids-to-monitor     (apply disj monitored-ids finished-jobs-ids)]
      (-> db
          (assoc-in (conj db-path ::resources) (->> resources
                                                    (map (juxt :id identity))
                                                    (into {})))
          (assoc-in monitored-ids-path ids-to-monitor)))))

(reg-event-fx
  ::search-jobs
  (fn [{db :db} [_ db-path]]

    (let [monitored-ids (get-in db (conj db-path ::monitored-ids))]
      (when (seq monitored-ids)
        {::cimi-api-fx/search
         [:job {:last   10000
                :filter (->> (get-in db (conj db-path ::monitored-ids))
                             (map #(str "id='" % "'"))
                             (apply general-utils/join-or))}
          #(dispatch [::set-jobs db-path (:resources %)])]}))))

(reg-event-fx
  ::start-polling
  (fn [_ [_ db-path]]
    {:fx [[:dispatch [::main-events/action-interval-start
                      {:id        (str db-path)
                       :frequency 10000
                       :event     [::search-jobs db-path]}]]]}))

(def job-action->header
  {"bulk_stop_deployment"         :bulk-stop-in-progress
   "bulk_update_deployment"       :bulk-update-in-progress
   "bulk_force_delete_deployment" :bulk-force-delete-in-progress})

(defn- MonitoredJob
  [{:keys [db-path]} job-id]
  (let [{:keys [state status-message progress action]
         :as   _job} @(subscribe [::helpers/retrieve
                                  (conj db-path ::resources) job-id])
        tr            @(subscribe [::i18n-subs/tr])
        on-dissmiss   #(dispatch [::dissmiss db-path job-id])
        {:keys [FAILED SUCCESS]
         :as   status-message} (when (not= state "FAILED")
                                 (utils-general/json->edn status-message))
        some-fail?    (pos? (count FAILED))
        some-success? (pos? (count SUCCESS))
        completed?    (= progress 100)
        state-failed? (= state "FAILED")
        color         (cond
                        (and some-fail? some-success?) "yellow"
                        (or state-failed? some-fail?) "red"
                        :else "green")
        Header        [uix/TR (or (job-action->header action)
                                  (some-> action
                                          (str/replace-all #"_" " ")
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
    [ui/Message (when completed? {:on-dismiss on-dissmiss})
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
                  :href     failed-id
                  :target   "_blank"
                  :on-click (fn [event]
                              (dispatch [::history-events/navigate failed-id])
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
                [ui/ListHeader [history-views/link success-id success-id]]]])]])
        ]]]]))

(defn MonitoredJobs
  [{:keys [db-path] :as _opts}]
  (dispatch [::start-polling db-path])
  (let [ids (subscribe [::helpers/retrieve db-path ::monitored-ids])]
    (fn [opts]
      [:<>
       (doall
         (for [job-id @ids]
           ^{:key job-id}
           [MonitoredJob opts job-id]))])))

(s/fdef MonitoredJobs
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path])))
