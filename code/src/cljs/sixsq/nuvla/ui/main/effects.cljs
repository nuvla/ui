(ns sixsq.nuvla.ui.main.effects
  (:require ["@stripe/stripe-js" :as stripejs]
            [re-frame.core :refer [dispatch reg-fx]]))


(reg-fx
  ::bulk-actions-interval
  (fn [[dispatched-event-key actions-interval]]
    (doseq [action-opts (vals actions-interval)]
      (dispatch [dispatched-event-key action-opts]))))


(reg-fx
  ::open-new-window
  (fn [[url]]
    (.open js/window url "_blank" "noreferrer")))


(reg-fx
  ::load-stripe
  (fn [[publishable-key callback]]
    (-> publishable-key
        (stripejs/loadStripe)
        (.then (fn [stripe]
                 (callback stripe))))))


(defn- before-unload-handler [event]
  (set! (.-returnValue event) "")
  (.preventDefault event))

(defn- clear-unload-protection []
  (.removeEventListener js/window "beforeunload" before-unload-handler))

(defn- set-unload-protection []
  (.addEventListener js/window "beforeunload" before-unload-handler))

(reg-fx
  ::on-unload-protection
  (fn [protected?]
    (if protected?
      (set-unload-protection)
      (clear-unload-protection))))
