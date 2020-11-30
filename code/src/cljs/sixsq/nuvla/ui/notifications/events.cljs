(ns sixsq.nuvla.ui.notifications.events
  (:require
    [cljs.spec.alpha :as s]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.cimi-detail.events :as cimi-detail-events]
    [sixsq.nuvla.ui.notifications.spec :as spec]
    [sixsq.nuvla.ui.notifications.utils :as utils]
    [sixsq.nuvla.ui.messages.events :as messages-events]
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
    (assoc db ::spec/notification-methods notification-methods)))


(reg-event-fx
  ::get-notification-methods
  (fn [{:keys [db]} [_]]
    {:db                  (assoc db ::spec/completed? false)
     ::cimi-api-fx/search [:notification-method
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
  (fn [{:keys [db]} [_ id]]
    {:db                  db
     ::cimi-api-fx/delete [id #(dispatch [::get-notification-methods])]}))


(reg-event-db
  ::update-notification-method
  (fn [db [_ key value]]
    (assoc-in db [::spec/notification-method key] value)))


(reg-event-db
  ::validate-notification-method-form
  (fn [db]
    (let [form-spec ::spec/notification-method
          notif-method   (get db ::spec/notification-method)
          validate-form? (get db ::spec/validate-form?)
          valid?         (if validate-form? (if (nil? form-spec) true (s/valid? form-spec notif-method)) true)]
      (s/explain form-spec notif-method)
      (assoc db ::spec/form-valid? valid?))))


(reg-event-fx
  ::edit-notification-method
  (fn [{{:keys [::spec/notification-method] :as db} :db} [_]]
    (let [id             (:id notification-method)
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


;;
;; subscription-config
;;

(reg-event-db
  ::method-subs-conf
  (fn [db [_ method]]
    (assoc-in db [::spec/notification-subscription-config :method] method)))


(reg-event-db
  ::set-notification-subscription-config
  (fn [db [_ subs-confs]]
    (assoc db ::spec/notification-subscription-config subs-confs)))


(reg-event-db
  ::update-notification-subscription-config
  (fn [db [_ key value]]
    (assoc-in db [::spec/notification-subscription-config key] value)))


(reg-event-db
  ::validate-notification-subscription-config-form
  (fn [db [_ form-spec]]
    (let [notif-method   (get db ::spec/notification-subscription-config)
          validate-form? (get db ::spec/validate-form?)
          valid?         (if validate-form? (if (nil? form-spec) true (s/valid? form-spec notif-method)) true)]
      (s/explain form-spec notif-method)
      (assoc db ::spec/form-valid? valid?))))


(reg-event-db
  ::set-notification-subscription-configs
  (fn [db [_ subs-confs]]
    (assoc db ::spec/notification-subscription-configs subs-confs)))


(reg-event-fx
  ::get-notification-subscription-configs
  (fn [{:keys [db]} [_]]
    {:db                  (assoc db ::spec/completed? false)
     ::cimi-api-fx/search [:subscription-config
                           {:filter "type='notification'"
                            :orderby "name:asc, id:asc"}
                           #(dispatch [::set-notification-subscription-configs (:resources %)])]}))

(reg-event-fx
  ::edit-subscription-config
  (fn [{{:keys [::spec/notification-subscription-config] :as db} :db} [_]]
    (let [id             (:id notification-subscription-config)
          new-subs-config (utils/db->new-subscription-config db)]
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
         ::cimi-api-fx/edit [id notification-subscription-config
                             #(if (instance? js/Error %)
                                (let [{:keys [status message]} (response/parse-ex-info %)]
                                  (dispatch [::messages-events/add
                                             {:header  (cond-> (str "error editing " id)
                                                               status (str " (" status ")"))
                                              :content message
                                              :type    :error}]))
                                (do (dispatch [::cimi-detail-events/get (:id %)])
                                    (dispatch [::get-notification-subscription-configs])))]}))))


;;
;; subscriptions
;;


(reg-event-db
  ::set-subscriptions
  (fn [db [_ subs]]
    (assoc db ::spec/subscriptions subs)))


(reg-event-fx
  ::get-notification-subscriptions
  (fn [{:keys [db]} [_]]
    {:db                  (assoc db ::spec/completed? false)
     ::cimi-api-fx/search [:subscription
                           {:filter "type='notification'"
                            :orderby "name:asc, id:asc"}
                           #(dispatch [::set-subscriptions (:resources %)])]}))


(reg-event-fx
  ::delete-subscription
  (fn [{:keys [db]} [_ id]]
    {:db                  db
     ::cimi-api-fx/delete [id #(dispatch [::get-notification-subscriptions])]}))


(reg-event-db
  ::open-notification-subscription-modal
  (fn [db [_ notif-subs]]
    (-> db
        (assoc ::spec/notification-subscriptions notif-subs)
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
    (let [form-spec ::spec/subscription
          subscription   (get db ::spec/subscription)
          validate-form? (get db ::spec/validate-form?)
          valid?         (if validate-form? (if (nil? form-spec) true (s/valid? form-spec subscription)) true)]
      (s/explain form-spec subscription)
      (assoc db ::spec/form-valid? valid?))))


(reg-event-fx
  ::edit-subscription
  (fn [{{:keys [::spec/subscription] :as db} :db} [_]]
    (let [id             (:id subscription)
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
