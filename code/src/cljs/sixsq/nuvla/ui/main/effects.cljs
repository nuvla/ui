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
(js/console.error "::on-unload-protection")
    (if protected?
      (set-unload-protection)
      (clear-unload-protection))))

(comment
  (set-unload-protection)
  (clear-unload-protection)
  )

(defn- stop-browser-back
  [f]
  (.pushState js/window.history nil "" (.-href js/window.location))
  (set!
    js/window.onpopstate
    (fn []
      (.pushState js/window.history nil "" (.-href js/window.location))
      (when (fn? f) (f)))))

(defn- start-browser-back
  []
  (set! js/window.onpopstate nil)
  (.back js/window.history))

(comment
  (stop-browser-back +)
  (start-browser-back)
  )

(reg-fx
  ::disable-browser-back
  (fn [f]
    (js/console.error "disable-browser-back")
    (stop-browser-back f)))

(reg-fx
  ::enable-browser-back
  (fn [fn]
    (js/console.error "enable-browser-back effect" fn)
    (start-browser-back)
    (when (fn? fn) (fn))))