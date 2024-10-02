(ns sixsq.nuvla.ui.portfolio-utils
  (:require [portfolio.reagent]
            [reagent.core :as r]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui])
  (:require-macros [sixsq.nuvla.ui.portfolio-utils]))

(defmulti scene-root identity)

(defn SceneRoot
  [scene-id]
  (scene-root scene-id))

(defn Scene
  [scene-id]
  (r/with-let [reset-atom (r/atom 0)]
    [:div
     [:div {:style {:margin-bottom "20px"}}
      [ui/Button {:on-click #(swap! reset-atom inc)} "Reset"]]
     ^{:key @reset-atom}
     [SceneRoot scene-id]]))
