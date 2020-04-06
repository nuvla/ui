(ns sixsq.nuvla.ui.pricing.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.pricing.effects :as fx]
    [sixsq.nuvla.ui.pricing.spec :as spec]
    [ajax.core :as ajax]))


(reg-event-fx
  ::init
  (fn [{db :db} _]
    {:db              (merge db spec/defaults)
     ::fx/load-stripe ["pk_test_Wo4so0qa2wqn66052FlvyMpl00MhPPQdAG"
                       #(dispatch [::load-stripe-done %])]}))


(reg-event-db
  ::load-stripe-done
  (fn [db [_ stripe]]
    (assoc db ::spec/stripe stripe)))


(reg-event-fx
  ::create-payment-method
  (fn [{{:keys [::spec/stripe] :as db} :db} [_ data]]
    (when stripe
      {::fx/create-payment-method [stripe data #(dispatch [::set-payment-method-result %])]
       :db                        (assoc db ::spec/processing? true
                                            ::spec/error nil)})))


(reg-event-fx
  ::set-payment-method-result
  (fn [{db :db} [_ result]]
    (let [res            (-> result (js->clj :keywordize-keys true))
          error          (:error res)
          payment-method (:paymentMethod res)]
      (if error
        {:db (assoc db ::spec/error (:message error)
                       ::spec/processing? false)}
        {:dispatch [::create-customer
                    ["plan_Gx43FhmevUCOau"]
                    (:id payment-method)
                    (-> payment-method :billing_details :email)]}))))


(reg-event-db
  ::success-http-result
  (fn [db [_ result]]
    (js/console.log ::success-http-result ": " result)
    db))


(reg-event-db
  ::failure-http-result
  (fn [db [_ result]]
    (js/console.log ::failure-post-result ": " result)
    db))


(reg-event-fx
  ::http-post
  (fn [_ [_ uri data success-event error-event]]
    {:http-xhrio {:method          :post
                  :uri             uri
                  :params          data
                  :timeout         5000
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [(or success-event ::success-http-result)]
                  :on-failure      [(or error-event ::failure-http-result)]}}))


(reg-event-fx
  ::create-customer
  (fn [_ [_ plan-ids payment-id email]]
    {:dispatch [::http-post "http://localhost:4242/create-customer"
                {:plan_ids       plan-ids
                 :payment_method payment-id
                 :email          email}
                ::set-subscription]}))


(reg-event-fx
  ::set-subscription
  (fn [{{:keys [::spec/stripe] :as db} :db} [_ result]]
    (let [res   (-> result (js->clj :keywordize-keys true))
          error (:error res)]
      (if error
        {:db (assoc db ::spec/error (:message error)
                       ::spec/processing? false)}
        (let [subscription         result
              payment-intent       (-> subscription :latest_invoice :payment_intent)
              requires-action?     (= (:status payment-intent) "requires_action")
              subscription-active? (= (:status subscription) "active")]
          (cond-> {:db (assoc db ::spec/subscription subscription
                                 ::spec/processing? (not subscription-active?))}
                  requires-action? (assoc ::fx/confirm-card-payment
                                          [stripe (:client_secret payment-intent)
                                           #(dispatch [::get-subscription
                                                       (:id subscription) %])])))))))


(reg-event-fx
  ::get-subscription
  (fn [{db :db} [_ subscription-id result]]
    (let [res   (-> result (js->clj :keywordize-keys true))
          error (:error res)]
      (if error
        {:db (assoc db ::spec/error (:message error)
                       ::spec/processing? false)}
        {:dispatch [::http-post "http://localhost:4242/subscription"
                    {:subscriptionId subscription-id}
                    ::set-subscription]}))))
