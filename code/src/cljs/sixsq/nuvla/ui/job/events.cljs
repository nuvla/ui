(ns sixsq.nuvla.ui.job.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.job.spec :as spec]
    [sixsq.nuvla.ui.utils.general :as general-utils]))


(reg-event-fx
  ::set-job-page
  (fn [{{:keys [::spec/target-resource] :as db} :db} [_ page]]
    {:dispatch [::get-jobs target-resource]
     :db       (assoc db ::spec/job-page page)}))


(reg-event-db
  ::set-jobs
  (fn [db [_ jobs]]
    (assoc db ::spec/jobs jobs)))


(reg-event-fx
  ::get-jobs
  (fn [{{:keys [::spec/target-resource
                ::spec/job-page
                ::spec/jobs-per-page] :as db} :db} [_ href]]
    (let [filter-str   (str "target-resource/href='" href "'")
          select-str   (str "id, action, time-of-status-change, updated, state, "
                            "target-resource, return-code, progress, status-message")
          query-params {:filter  filter-str
                        :select  select-str
                        :first   (inc (* (dec job-page) jobs-per-page))
                        :last    (* job-page jobs-per-page)
                        :orderby "created:desc"}]
      {:db                  (-> db
                                (cond-> (not= href target-resource) (merge spec/defaults))
                                (assoc ::spec/target-resource href))
       ::cimi-api-fx/search [:job
                             (general-utils/prepare-params query-params)
                             #(dispatch [::set-jobs %])]})))








