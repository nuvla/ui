(ns sixsq.nuvla.ui.messages.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [sixsq.nuvla.ui.messages.spec :as spec]
            [sixsq.nuvla.ui.utils.time :as time]))


(reg-event-db
  ::hide
  (fn [db _]
    (assoc db ::spec/alert-message nil
              ::spec/alert-display :none
              ::spec/popup-open? false)))


(reg-event-db
  ::close-slider
  (fn [{:keys [::spec/alert-display] :as db} _]
    (if (= :slider alert-display)
      (assoc db ::spec/alert-display :none)
      db)))


(reg-event-db
  ::open-modal
  (fn [db _]
    (assoc db ::spec/alert-display :modal
              ::spec/popup-open? false)))


(reg-event-db
  ::close-modal
  (fn [db _]
    (assoc db ::spec/alert-display :none
              ::spec/popup-open? false)))


(reg-event-fx
  ::add
  (fn [{{:keys [::spec/messages] :as db} :db} [_ message uuid]]
    (let [timestamped-message (assoc message :timestamp (time/now)
                                             :uuid (or uuid (random-uuid)))
          updated-messages    (vec (cons timestamped-message messages))]
      {:db             (assoc db ::spec/messages updated-messages
                                 ::spec/alert-message timestamped-message
                                 ::spec/alert-display :slider)
       :dispatch-later [{:ms 5000 :dispatch [::close-slider]}]})))


(reg-event-db
  ::remove
  (fn [{:keys [::spec/messages] :as db} [_ uuid]]
    (let [trimmed-messages (vec (remove (fn [m] (= uuid (:uuid m))) messages))]
      (assoc db ::spec/messages trimmed-messages
                ::spec/alert-message nil
                ::spec/alert-display :none))))


(reg-event-db
  ::show
  (fn [db [_ timestamped-message]]
    (if timestamped-message
      (assoc db ::spec/alert-message timestamped-message
                ::spec/alert-display :modal
                ::spec/popup-open? false)
      db)))


(reg-event-db
  ::clear-all
  (fn [{:keys [::spec/messages] :as db} _]
    (assoc db ::spec/messages (vec (filter (fn [{:keys [type]}] (= type :notif)) messages))
              ::spec/alert-message nil
              ::spec/alert-display :none
              ::spec/popup-open? false)))


(reg-event-db
  ::open-popup
  (fn [{:keys [::spec/messages] :as db} _]
    (cond-> db
            (seq messages) (assoc ::spec/popup-open? true))))


(reg-event-db
  ::close-popup
  (fn [{:keys [::spec/messages] :as db} _]
    (cond-> db
            (seq messages) (assoc ::spec/popup-open? false))))
