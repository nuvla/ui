(ns sixsq.nuvla.ui.utils.accordion
  (:require [re-frame.core :refer [dispatch]]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.utils.form-fields :as form-fields]))

(defn toggle [v]
  (swap! v not))


(defn show-count
  [coll]
  [:span form-fields/nbsp [ui/Label {:circular true} (count coll)]])


(defn trash
  [id remove-event validate-event]
  [ui/Icon {:name     "trash"
            :style    {:cursor :pointer}
            :on-click #(do (when-not (nil? validate-event) (dispatch [::main-events/changes-protection? true]))
                           (dispatch [remove-event id])
                           (when-not (nil? validate-event) (dispatch [validate-event])))
            :color    :red}])


(defn plus
  [add-event validate-event]
  [ui/Icon {:name     "plus circle"
            :on-click #(do (dispatch [::main-events/changes-protection? true])
                           (dispatch [add-event (random-uuid) {}])
                           (dispatch [validate-event]))}])
