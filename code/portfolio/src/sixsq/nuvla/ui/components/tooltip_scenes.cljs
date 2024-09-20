(ns sixsq.nuvla.ui.components.tooltip-scenes
  (:require [portfolio.reagent :refer-macros [defscene]]
            [sixsq.nuvla.ui.utils.tooltip :refer [with-tooltip]]))

(defscene simple-tooltip
  [with-tooltip
   [:label "Component with tooltip"]
   "Tooltip content"])

(defscene longtext-tooltip
  [with-tooltip
   [:label "Component with tooltip"]
   "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer vitae varius velit. Morbi iaculis elementum augue id viverra. Mauris eget orci in ligula aliquet ultricies non non dui. Fusce blandit purus dolor, in ultrices dui lacinia eget. Cras suscipit pharetra augue, vitae aliquet enim tincidunt ac. Maecenas vulputate vestibulum metus a aliquet. Donec sagittis leo nec enim mattis, eget rhoncus odio maximus. Fusce consectetur tincidunt aliquam. Vivamus ac porttitor nisl. Donec vestibulum posuere euismod. Donec quis laoreet urna, id fermentum tellus. Nunc urna ante, imperdiet quis rhoncus laoreet, viverra nec arcu."])
