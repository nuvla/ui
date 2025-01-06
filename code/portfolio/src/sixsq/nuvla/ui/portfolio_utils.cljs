(ns sixsq.nuvla.ui.portfolio-utils
  (:require [portfolio.reagent]
            [reagent.core :as r]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui])
  (:require-macros [sixsq.nuvla.ui.portfolio-utils]))

(defmulti scene-root identity)

(defn SceneRoot
  [scene-id]
  (scene-root scene-id))

(defn err-boundary
  [& _children]
  (let [err-state (r/atom nil)]
    (r/create-class
      {:display-name                 "ErrBoundary"
       :component-did-catch          (fn [_err _info])
       :get-derived-state-from-error (fn [err]
                                       (reset! err-state [err err])
                                       #js {})
       :reagent-render               (fn [& children]
                                       (if (nil? @err-state)
                                         (into [:<>] children)
                                         (let [[_ info] @err-state]
                                           [:pre [:code (pr-str info)]])))})))

(defn Scene
  [scene-id]
  (r/with-let [reset-atom (r/atom 0)]
    [err-boundary
     [:div
      [:div {:style {:margin-bottom "20px"}}
       [ui/Button {:on-click #(swap! reset-atom inc)} "Reset"]]
      ^{:key @reset-atom}
      [SceneRoot scene-id]]]))
