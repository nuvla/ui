(ns sixsq.nuvla.ui.components.tooltip-scenes
  (:require [portfolio.reagent :refer-macros [defscene]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.tooltip :refer [with-tooltip]]))

(defscene simple-tooltip
  #_(let [child (r/atom nil)]
      (js/setTimeout #(reset! child
                              [ui/Popup
                               {:content   (r/as-element [:div "Tooltip content"])
                                :trigger   (r/as-element [:div "Component with tooltip"])
                                :pinned    true
                                :hoverable true}])
                     5000)
      (fn []
        [:div.ui.container "Hello" @child
         #_[with-tooltip
            [:label "Component with tooltip"]
            "Tooltip content"]]))
  (let [context-ref (r/atom nil)]
    (fn []
      [:div
       (when @context-ref
         [ui/Popup
          {:content "Tooltip content"
           :trigger (r/as-element [:div "Component with tooltip"])
           ;:pinned    true
           ;:hoverable true
           :context @context-ref}])
       [:div {:ref (fn [el] (reset! context-ref el))}
        "here"]])))

(defscene longtext-tooltip
  [with-tooltip
   [:label "Component with tooltip"]
   "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer vitae varius velit. Morbi iaculis elementum augue id viverra. Mauris eget orci in ligula aliquet ultricies non non dui. Fusce blandit purus dolor, in ultrices dui lacinia eget. Cras suscipit pharetra augue, vitae aliquet enim tincidunt ac. Maecenas vulputate vestibulum metus a aliquet. Donec sagittis leo nec enim mattis, eget rhoncus odio maximus. Fusce consectetur tincidunt aliquam. Vivamus ac porttitor nisl. Donec vestibulum posuere euismod. Donec quis laoreet urna, id fermentum tellus. Nunc urna ante, imperdiet quis rhoncus laoreet, viverra nec arcu."])
