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
    (js/console.error "2: ::on-unload-protection")
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
  (js/console.error "4a: start-browser-back before setting onpopstate nil")
  (set! js/window.onpopstate nil)
  (js/console.error "4b: start-browser-back before browser back")
  (.back js/window.history)  (js/console.error "4c: start-browser-back after browser back"))

(comment
  (stop-browser-back +)
  (start-browser-back)
  )

(reg-fx
  ::disable-browser-back
  (fn [f]
    (stop-browser-back f)))


(reg-fx
  ::enable-browser-back
  (fn [fn]
    (js/console.error "3: ::fx/enable-browser-back")
    (start-browser-back)
    (when (fn? fn) (fn))))

(reg-fx
  ::add-pop-state-listener-close-modal-event
  (fn [f]
    (.addEventListener js/window "popstate" f)))


(reg-fx
  ::clear-popstate-event-listener
  (fn
    [f]
    (js/console.error "1: clear-popstate-event-listener")
    (.removeEventListener js/window "popstate" f)))