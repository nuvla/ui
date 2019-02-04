(ns sixsq.slipstream.webui.utils.responsive)


(defn device
  "Classifies the device being used based on the screen width. On any error
   (e.g. invalid input), the function will return :computer. The breakpoints
   are taken from the Semantic UI React code; they are not available in a more
   convenient form."
  [width]
  (try
    (cond
      (< width 767) :mobile
      (< width 991) :tablet
      (< width 1199) :computer
      (< width 1919) :large-screen
      :else :wide-screen)
    (catch :default _
      :computer)))


(defn callback
  "Creates a function intended for the on-update callback from the Responsive
   element. The function extracts the display width, classifies the device, and
   calls the provided callback with the device keyword."
  [callback]
  (fn [evt data]
    (when callback
      (-> (.-width data)
          device
          callback))))
