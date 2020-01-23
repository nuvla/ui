(ns sixsq.nuvla.ui.utils.accordion
  (:require [re-frame.core :refer [dispatch dispatch-sync]]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.utils.form-fields :as form-fields]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(defn toggle [v]
  (swap! v not))


(defn show-count
  [coll]
  [:span form-fields/nbsp [ui/Label {:circular true} (count coll)]])


(defn plus
  [add-event validate-event]
  [ui/Icon {:name     "plus circle"
            :link     true
            :on-click #(do (dispatch [::main-events/changes-protection? true])
                           (dispatch [add-event (random-uuid) {}])
                           (dispatch [validate-event]))}])
