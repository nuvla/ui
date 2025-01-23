(ns sixsq.nuvla.ui.components.duration-picker-scenes
  (:require [reagent.core :as r]
            [sixsq.nuvla.ui.portfolio-utils :refer [defscene BoolParam]]
            [sixsq.nuvla.ui.common-components.plugins.duration-picker :refer [DurationPickerController]]))

(defscene basic-picker
  (r/with-let [!seconds       (r/atom 0)
               !show-days?    (r/atom true)
               !show-hours?   (r/atom true)
               !show-minutes? (r/atom true)
               !show-seconds? (r/atom true)]
    [:div {:style {:width 400}}
     [BoolParam !show-days? "checkbox-show-days" "Show days ?"]
     [BoolParam !show-hours? "checkbox-show-hours" "Show hours ?"]
     [BoolParam !show-minutes? "checkbox-show-minutes" "Show minutes ?"]
     [BoolParam !show-seconds? "checkbox-show-seconds" "Show seconds ?"]
     [:div {:style {:margin 10}}
      [DurationPickerController {:!value         !seconds
                                 :!show-days?    !show-days?
                                 :!show-hours?   !show-hours?
                                 :!show-minutes? !show-minutes?
                                 :!show-seconds? !show-seconds?}]]
     [:div {:data-testid "total-seconds"} "Duration in seconds: " @!seconds]]))

(defscene custom-options
  (r/with-let [!seconds         (r/atom 0)
               !days-options    (r/atom [0 5 10])
               !hours-options   (r/atom [0 5 10 15 20])
               !minutes-options (r/atom [0 10 20 30 40 50])
               !seconds-options (r/atom [0 30])]
    [:div {:style {:width 400}}
     [:div {:style {:margin 10}}
      [DurationPickerController {:!value           !seconds
                                 :!days-options    !days-options
                                 :!hours-options   !hours-options
                                 :!minutes-options !minutes-options
                                 :!seconds-options !seconds-options}]]
     [:div {:data-testid "total-seconds"} "Duration in seconds: " @!seconds]]))
