(ns sixsq.slipstream.webui.utils.ui-callback
  (:require
    [re-frame.core :refer [dispatch]]))


(defn input-callback
  [f]
  (fn [evt]
    (let [v (-> evt .-target .-value)]
      (f v))))


(defn input
  [event-kw]
  (let [f #(dispatch [event-kw %])]
    (input-callback f)))


(defn callback
  "Create a UI callback function that has the standard signature f(evt data),
   converts the javascript data object to clojurescript, extracts the value of
   the given keyword, and then calls the provided function with the reformatted
   data."
  [kw f]
  (fn [evt data]
    (-> data
        (js->clj :keywordize-keys true)
        kw
        f)))


(def value (partial callback :value))


(def checked (partial callback :checked))


(defn dropdown
  "Creates a callback function for the on-change hook of a dropdown. The given
   event is dispatched with the new value of the dropdown."
  [event-kw]
  (value #(dispatch [event-kw %])))
