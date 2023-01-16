(ns sixsq.nuvla.ui.notifications.events
  (:require [cljs.spec.alpha :as s]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.cimi-detail.events :as cimi-detail-events]
            [sixsq.nuvla.ui.main.spec :as main-spec]
            [sixsq.nuvla.ui.messages.events :as messages-events]
            [sixsq.nuvla.ui.notifications.spec :as spec]
            [sixsq.nuvla.ui.notifications.utils :as utils]
            [sixsq.nuvla.ui.utils.response :as response]))

;;
;; notification-methods
;;

(reg-event-db
  ::method
  (fn [db [_ method]]
    (assoc-in db [::spec/notification-method :method] method)))


(reg-event-db
  ::set-notification-method
  (fn [db [_ notification-method]]
    (assoc db ::spec/notification-method notification-method)))


(reg-event-db
  ::set-notification-methods
  (fn [db [_ notification-methods]]
    (assoc db ::spec/notification-methods notification-methods
              ::main-spec/loading? false)))


(reg-event-fx
  ::get-notification-methods
  (fn [_ _]
    {::cimi-api-fx/search [:notification-method
                           {:orderby "name:asc, id:asc"}
                           #(dispatch [::set-notification-methods (:resources %)])]}))


(reg-event-db
  ::open-add-update-notification-method-modal
  (fn [db [_ notif-method is-new?]]
    (-> db
        (assoc ::spec/notification-method notif-method)
        (assoc ::spec/notification-method-modal-visible? true)
        (assoc ::spec/is-new? is-new?))))


(reg-event-db
  ::close-add-update-notification-method-modal
  (fn [db _]
    (assoc db ::spec/notification-method-modal-visible? false)))


(reg-event-fx
  ::delete-notification-method
  (fn [_ [_ id]]
    {::cimi-api-fx/delete [id #(dispatch [::get-notification-methods])]}))


(reg-event-db
  ::update-notification-method
  (fn [db [_ key value]]
    (assoc-in db [::spec/notification-method key] value)))


(reg-event-db
  ::validate-notification-method-form
  (fn [db]
    (let [form-spec      ::spec/notification-method
          notif-method   (get db ::spec/notification-method)
          validate-form? (get db ::spec/validate-form?)
          valid?         (if validate-form?
                           (if (nil? form-spec)
                             true
                             (s/valid? form-spec notif-method))
                           true)]
      (s/explain form-spec notif-method)
      (assoc db ::spec/form-valid? valid?))))


(reg-event-fx
  ::edit-notification-method
  (fn [{{:keys [::spec/notification-method] :as db} :db} [_]]
    (let [id               (:id notification-method)
          new-notif-method (utils/db->new-notification-method db)]
      (if (nil? id)
        {:db               db
         ::cimi-api-fx/add [:notification-method new-notif-method
                            #(do (dispatch [::cimi-detail-events/get (:resource-id %)])
                                 (dispatch [::close-add-update-notification-method-modal])
                                 (dispatch [::get-notification-methods])
                                 (let [{:keys [status message resource-id]} (response/parse %)]
                                   (dispatch [::messages-events/add
                                              {:header  (cond-> (str "added " resource-id)
                                                                status (str " (" status ")"))
                                               :content message
                                               :type    :success}])))]}
        {:db                db
         ::cimi-api-fx/edit [id notification-method
                             #(if (instance? js/Error %)
                                (let [{:keys [status message]} (response/parse-ex-info %)]
                                  (dispatch [::messages-events/add
                                             {:header  (cond-> (str "error editing " id)
                                                               status (str " (" status ")"))
                                              :content message
                                              :type    :error}]))
                                (do (dispatch [::cimi-detail-events/get (:id %)])
                                    (dispatch [::close-add-update-notification-method-modal])
                                    (dispatch [::get-notification-methods])))]}))))


(reg-event-db
  ::deactivate-notification-method-create-button
  (fn [db [_]]
    (assoc db ::spec/notification-method-create-button-visible? false)))


;;
;; subscription-config
;;

(reg-event-db
  ::method-subs-conf
  (fn [db [_ method]]
    (assoc-in db [::spec/notification-subscription-config :method] method)))


(reg-event-db
  ::set-notification-subscription-config-id
  (fn [db [_ subs-conf-id]]
    (assoc db ::spec/notification-subscription-config-id subs-conf-id)))


(reg-event-db
  ::set-notification-subscription-config
  (fn [db [_ subs-conf]]
    (assoc db ::spec/notification-subscription-config subs-conf)))


(reg-event-fx
  ::update-notification-subscription-config
  (fn [{db :db} [_ key value]]
    {:db (if (map? value)
           (update-in db [::spec/notification-subscription-config key] merge value)
           (assoc-in db [::spec/notification-subscription-config key] value))
     :fx [[:dispatch [::validate-notification-subscription-config-form]]]}))

(defn- update-subscription-criteria-fx [new-values]
  {:fx [[:dispatch [::update-notification-subscription-config
                    :criteria
                    new-values]]]})

(reg-event-fx
  ::choose-monthly-reset
  (fn [{{:keys [::spec/notification-subscription-config]} :db}]
    (let [criteria             (:criteria notification-subscription-config)
          new-reset-start-date (or (:reset-start-date criteria) 1)]
      (update-subscription-criteria-fx {:reset-interval   "month"
                                        :reset-start-date new-reset-start-date}))))

(reg-event-fx
  ::choose-custom-reset
  (fn [{{:keys [::spec/notification-subscription-config]} :db}]
    (let [criteria      (:criteria notification-subscription-config)
          reset-in-days (or (:reset-in-days criteria) 1)]
      (update-subscription-criteria-fx {:reset-interval (str reset-in-days "d")
                                        :reset-in-days  reset-in-days}))))

(reg-event-fx
  ::update-custom-days
  (fn [_ [_ value]]
    (let [new-custom-days      (js/Number value)
          custom-interval-days (str new-custom-days "d")]
      (update-subscription-criteria-fx {:reset-interval custom-interval-days
                                        :reset-in-days  new-custom-days}))))

(reg-event-fx
  ::update-custom-device-name
  (fn [_ [_ value]]
    (update-subscription-criteria-fx {:dev-name value})))


(reg-event-db
  ::remove-custom-name
  (fn [db]
    (update-in db [::spec/notification-subscription-config :criteria] dissoc :dev-name)))


(reg-event-db
  ::validate-notification-subscription-config-form
  (fn [db [_]]
    (let [subs-config    (get db ::spec/notification-subscription-config)
          form-spec      ::spec/notification-subscription-config
          validate-form? (get db ::spec/validate-form?)
          valid?         (if validate-form?
                           (if (nil? form-spec)
                             true
                             (s/valid? form-spec subs-config))
                           true)]
      (s/explain form-spec subs-config)
      (assoc db ::spec/form-valid? valid?))))


(reg-event-db
  ::set-notification-subscription-configs
  (fn [db [_ subs-confs]]
    (assoc db ::spec/notification-subscription-configs subs-confs)))


(reg-event-fx
  ::get-notification-subscription-configs
  (fn [_ [_]]
    {::cimi-api-fx/search [:subscription-config
                           {:orderby "name:asc, id:asc"}
                           #(dispatch [::set-notification-subscription-configs (map utils/model->view (:resources %))])]}))

(reg-event-fx
  ::edit-subscription-config
  (fn [{{:keys [::spec/notification-subscription-config] :as db} :db} [_]]
    (let [id              (:id notification-subscription-config)
          new-subs-config (utils/view->model (utils/db->new-subscription-config db))]
      (if (nil? id)
        {:db               db
         ::cimi-api-fx/add [:subscription-config new-subs-config
                            #(do (dispatch [::cimi-detail-events/get (:resource-id %)])
                                 (dispatch [::get-notification-subscription-configs])
                                 (let [{:keys [status message resource-id]} (response/parse %)]
                                   (dispatch [::messages-events/add
                                              {:header  (cond-> (str "added " resource-id)
                                                                status (str " (" status ")"))
                                               :content message
                                               :type    :success}])))]}
        {:db                db
         ::cimi-api-fx/edit [id new-subs-config
                             #(if (instance? js/Error %)
                                (let [{:keys [status message]} (response/parse-ex-info %)]
                                  (dispatch [::messages-events/add
                                             {:header  (cond-> (str "error editing " id)
                                                               status (str " (" status ")"))
                                              :content message
                                              :type    :error}]))
                                (do (dispatch [::cimi-detail-events/get (:id %)])
                                    (dispatch [::get-notification-subscription-configs])))]}))))


(reg-event-fx
  ::set-tags-available
  (fn [{:keys [db]} [_ tags-response]]
    (let [buckets (get-in tags-response
                          [:aggregations (keyword "terms:tags") :buckets])]
      {:db (assoc db ::spec/resource-tags-available buckets)})))


(reg-event-db
  ::reset-tags-available
  (fn [db [_]]
    (assoc db ::spec/resource-tags-available [])))


(reg-event-fx
  ::fetch-tags-available
  (fn [_ [_ resource-kind]]
    (when (not-empty resource-kind)
      {::cimi-api-fx/search [(keyword resource-kind) {:last        0
                                                      :aggregation "terms:tags"}
                             #(dispatch [::set-tags-available %])]})))


(reg-event-fx
  ::fetch-components-number
  (fn [_ [_ component]]
    (when component
      {::cimi-api-fx/search [(keyword component) {:last 0}
                             #(dispatch [::set-components-number (:count %)])]})))


(reg-event-db
  ::reset-subscription-config-all
  (fn [db [_]]
    (assoc db ::spec/notification-subscription-config {})
    (assoc db ::spec/components-number 0)
    (assoc db ::spec/resource-tags-available [])
    (assoc db ::spec/collection nil)))


(reg-event-fx
  ::toggle-enabled
  (fn [_ [_ rid enabled]]
    (if enabled
      {::cimi-api-fx/operation [rid "enable" #()]}
      {::cimi-api-fx/operation [rid "disable" #()]})))


(reg-event-fx
  ::set-notif-method-ids
  (fn [_ [_ subs-conf-id method-ids]]
    {::cimi-api-fx/operation [subs-conf-id "set-notif-method-ids" #() :data {:method-ids method-ids}]}))


;;
;; subscriptions
;;


(reg-event-db
  ::set-subscriptions
  (fn [db [_ subs]]
    (assoc db ::spec/subscriptions subs)))


(reg-event-db
  ::set-subscriptions-for-parent
  (fn [db [_ subs]]
    (assoc db ::spec/subscriptions-for-parent subs)))


(reg-event-db
  ::set-subscriptions-by-parent
  (fn [db [_ subs]]
    (assoc db ::spec/subscriptions-by-parent subs)))


(reg-event-db
  ::set-subscriptions-by-parent-counts
  (fn [db [_ subs-counts]]
    (assoc db ::spec/subscriptions-by-parent-counts subs-counts)))


(reg-event-db
  ::set-collection
  (fn [db [_ collection]]
    (assoc db ::spec/collection collection)))


(reg-event-db
  ::set-resource-tag
  (fn [db [_ resource-tag]]
    (assoc db ::spec/resource-tag resource-tag)))


(reg-event-db
  ::set-components-number
  (fn [db [_ num]]
    (assoc db ::spec/components-number num)))


(reg-event-db
  ::set-components-tagged-number
  (fn [db [_ tag]]
    (let [tags-all (::spec/resource-tags-available db)]
      (if (not-empty tag)
        (assoc db ::spec/components-number (:doc_count (first (filter (comp #{tag} :key) tags-all))))
        (dispatch [::fetch-components-number (get-in db [::spec/notification-subscription-config :collection])])))))


(reg-event-db
  ::set-criteria-condition
  (fn [db [_ condition]]
    (assoc db ::spec/criteria-condition condition)))


(reg-event-db
  ::set-criteria-value
  (fn [db [_ value]]
    (assoc db ::spec/criteria-value value)))


(reg-event-fx
  ::get-notification-subscriptions
  (fn [_ [_ parent]]
    (let [flt      (if parent
                     (str "category='notification' and parent='" parent "'")
                     "category='notification'")
          callback (fn
                     [{:keys [resources]}]
                     (if parent
                       (dispatch [::set-subscriptions-for-parent resources])
                       (let [subs-by-parent (reduce (fn [m [k v]]
                                                      (assoc m k (conj (get m k []) v)))
                                                    {}
                                                    (apply concat (map (fn [v] {(get v :parent) v}) resources)))
                             counts         (reduce into {} (map (fn [[k v]] {k (count v)}) subs-by-parent))]
                         (dispatch [::set-subscriptions-by-parent subs-by-parent])
                         (dispatch [::set-subscriptions-by-parent-counts counts])
                         (dispatch [::set-subscriptions resources]))))]
      {::cimi-api-fx/search [:subscription
                             {:filter flt :orderby "name:asc, id:asc"}
                             callback]})))


(reg-event-fx
  ::delete-subscription
  (fn [_ [_ id]]
    {::cimi-api-fx/delete [id #(dispatch [::get-notification-subscriptions])]}))


(reg-event-fx
  ::delete-subscription-config
  (fn [_ [_ id]]
    {::cimi-api-fx/delete [id #(dispatch [::get-notification-subscription-configs])]}))


(reg-event-db
  ::open-add-subscription-config-modal
  (fn [db [_ subs-conf]]
    (-> db
        (assoc ::spec/notification-subscription-config subs-conf)
        (assoc ::spec/add-subscription-config-modal-visible? true))))


(reg-event-db
  ::close-add-subscription-config-modal
  (fn [db _]
    (assoc db ::spec/add-subscription-config-modal-visible? false)))


(reg-event-db
  ::open-edit-subscription-config-modal
  (fn [db [_ subs-conf]]
    (let [sc (assoc subs-conf :collection (:resource-kind subs-conf))]
      (-> db
          (assoc ::spec/notification-subscription-config sc)
          (assoc ::spec/edit-subscription-config-modal-visible? true)))))


(reg-event-db
  ::close-edit-subscription-config-modal
  (fn [db _]
    (assoc db ::spec/edit-subscription-config-modal-visible? false)))


(reg-event-db
  ::open-add-subscription-modal
  (fn [db [_ comp-type]]
    (-> db
        (assoc ::spec/component-type comp-type)
        (assoc ::spec/add-subscription-modal-visible? true))))


(reg-event-db
  ::close-add-subscription-modal
  (fn [db _]
    (assoc db ::spec/add-subscription-modal-visible? false)))


(reg-event-db
  ::open-notification-subscription-modal
  (fn [db [_ notif-subs]]
    (-> db
        (assoc ::spec/subscriptions-for-parent notif-subs)
        (assoc ::spec/notification-subscriptions-modal-visible? true))))


(reg-event-db
  ::close-notification-subscription-modal
  (fn [db _]
    (assoc db ::spec/notification-subscriptions-modal-visible? false)))


(reg-event-db
  ::open-edit-subscription-modal
  (fn [db [_ subscription]]
    (-> db
        (assoc ::spec/subscription subscription)
        (assoc ::spec/edit-subscription-modal-visible? true))))


(reg-event-db
  ::close-edit-subscription-modal
  (fn [db _]
    (assoc db ::spec/edit-subscription-modal-visible? false)))


(reg-event-db
  ::update-subscription
  (fn [db [_ key value]]
    (assoc-in db [::spec/subscription key] value)))


(reg-event-db
  ::validate-subscription-form
  (fn [db]
    (let [form-spec      ::spec/subscription
          subscription   (get db ::spec/subscription)
          validate-form? (get db ::spec/validate-form?)
          valid?         (if validate-form? (if (nil? form-spec) true (s/valid? form-spec subscription)) true)]
      (s/explain form-spec subscription)
      (assoc db ::spec/form-valid? valid?))))


(reg-event-fx
  ::edit-subscription
  (fn [{{:keys [::spec/subscription] :as db} :db} [_]]
    (let [id       (:id subscription)
          new-subs (utils/db->new-subscription db)]
      (if (nil? id)
        {:db               db
         ::cimi-api-fx/add [:subscription new-subs
                            #(do (dispatch [::cimi-detail-events/get (:resource-id %)])
                                 (dispatch [::get-notification-subscriptions])
                                 (let [{:keys [status message resource-id]} (response/parse %)]
                                   (dispatch [::messages-events/add
                                              {:header  (cond-> (str "added " resource-id)
                                                                status (str " (" status ")"))
                                               :content message
                                               :type    :success}])))]}
        {:db                db
         ::cimi-api-fx/edit [id subscription
                             #(if (instance? js/Error %)
                                (let [{:keys [status message]} (response/parse-ex-info %)]
                                  (dispatch [::messages-events/add
                                             {:header  (cond-> (str "error editing " id)
                                                               status (str " (" status ")"))
                                              :content message
                                              :type    :error}]))
                                (do (dispatch [::cimi-detail-events/get (:id %)])
                                    (dispatch [::get-notification-subscriptions])))]}))))


;;
;; generic
;;

(reg-event-db
  ::set-validate-form?
  (fn [db [_ validate-form?]]
    (assoc db ::spec/validate-form? validate-form?)))
