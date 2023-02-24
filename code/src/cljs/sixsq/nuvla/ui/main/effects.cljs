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


(defn- stop-browser-back
  "It's not possible to disable navigate back in modern browsers.

   Workaround:
   1. Push current route on history stack, so that navigate back leaves you on same page.
   2. Navigating back then goes to same page, triggers 'popstate' event, which pushes current route again.
       see: https://stackoverflow.com/a/64572567"
  [f]
  (.pushState js/window.history nil "" (.-href js/window.location))
  (set!
    js/window.onpopstate
    (fn []
      (.pushState js/window.history nil "" (.-href js/window.location))
      (when (fn? f) (f)))))

(defn- start-browser-back
  [f nav-back?]
  (set! js/window.onpopstate nil)
  ;; if modal was opened by navigating back, go back two steps in history stack
  (cond
    nav-back? (.go js/window.history -2)
    (fn? f)   (f)
    :else     (.back js/window.history)))


(reg-fx
  ::disable-browser-back
  (fn [f]
    (stop-browser-back f)))


(reg-fx
  ::enable-browser-back
  (fn [{:keys [cb-fn nav-back?]}]
    (start-browser-back cb-fn nav-back?)))

(reg-fx
  ::add-pop-state-listener-close-modal-event
  (fn [f]
    (.addEventListener js/window "popstate" f)))


(reg-fx
  ::clear-popstate-event-listener
  (fn
    [f]
    (.removeEventListener js/window "popstate" f)))