(ns sixsq.slipstream.webui.messages.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.slipstream.webui.messages.spec :as messages-spec]
    [sixsq.slipstream.webui.utils.time :as time]))


(reg-event-db
  ::hide
  (fn [db _]
    (assoc db ::messages-spec/alert-message nil
              ::messages-spec/alert-display :none
              ::messages-spec/popup-open? false)))


(reg-event-db
  ::close-slider
  (fn [{:keys [::messages-spec/alert-display] :as db} _]
    (if (= :slider alert-display)
      (assoc db ::messages-spec/alert-display :none)
      db)))


(reg-event-db
  ::open-modal
  (fn [db _]
    (assoc db ::messages-spec/alert-display :modal
              ::messages-spec/popup-open? false)))


(reg-event-db
  ::close-modal
  (fn [db _]
    (assoc db ::messages-spec/alert-display :none
              ::messages-spec/popup-open? false)))


(reg-event-fx
  ::add
  (fn [{{:keys [::messages-spec/messages] :as db} :db :as cofx} [_ message]]
    (let [timestamped-message (assoc message :timestamp (time/now)
                                             :uuid (random-uuid))]
      (let [updated-messages (vec (cons timestamped-message messages))]
        {:db (assoc db ::messages-spec/messages updated-messages
                       ::messages-spec/alert-message timestamped-message
                       ::messages-spec/alert-display :slider)
         :dispatch-later [{:ms 3000 :dispatch [::close-slider]}]}))))


(reg-event-db
  ::remove
  (fn [{:keys [::messages-spec/messages] :as db} [_ {:keys [uuid] :as timestamped-message}]]
    (let [trimmed-messages (vec (remove (fn [m] (= uuid (:uuid m))) messages))]
      (assoc db ::messages-spec/messages trimmed-messages
                ::messages-spec/alert-message nil
                ::messages-spec/alert-display :none))))


(reg-event-db
  ::show
  (fn [{:keys [::messages-spec/messages] :as db} [_ timestamped-message]]
    (if timestamped-message
      (assoc db ::messages-spec/alert-message timestamped-message
                ::messages-spec/alert-display :modal
                ::messages-spec/popup-open? false)
      db)))


(reg-event-db
  ::clear-all
  (fn [db _]
    (assoc db ::messages-spec/messages []
              ::messages-spec/alert-message nil
              ::messages-spec/alert-display :none
              ::messages-spec/popup-open? false)))


(reg-event-db
  ::open-popup
  (fn [{:keys [::messages-spec/messages] :as db} _]
    (cond-> db
            (seq messages) (assoc ::messages-spec/popup-open? true))))


(reg-event-db
  ::close-popup
  (fn [{:keys [::messages-spec/messages] :as db} _]
    (cond-> db
            (seq messages) (assoc ::messages-spec/popup-open? false))))
