(ns sixsq.slipstream.webui.dnd.utils)


(defn prevent-default
  "Stops the propagation of a UI event and prevents the default action from
   being performed."
  [evt]
  (when evt
    (.stopPropagation evt)
    (.preventDefault evt)))


(defn disable-browser-dnd
  "Disables the default browser 'drag and drop' behavior that opens a
   dragged/dropped file by default."
  []
  (.addEventListener js/window "dragover" prevent-default false)
  (.addEventListener js/window "drop" prevent-default false))
