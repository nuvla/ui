(ns sixsq.nuvla.ui.common-components.plugins.dg-sub-type-selector
  (:require [reagent.core :as r]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(defn DockerIcon
  [{:keys [selected? size]}]
  [icons/DockerIcon (cond-> {:color (if selected? :blue :grey)}
                            size (assoc :size size))])

(def size->width {:small "40px"
                  :large "75px"
                  :big   "100px"
                  :huge  "300px"})

(defn DockerSwarmIcon
  [{:keys [selected? size]}]
  [ui/Image {:src   (if selected? "/ui/images/docker-swarm.png" "/ui/images/docker-swarm-grey.png")
             :style {:width (size->width size)}}])

(defn KubernetesIcon
  [{:keys [selected? size]}]
  [ui/Image {:src   (if selected? "/ui/images/kubernetes.svg" "/ui/images/kubernetes-grey.svg")
             :style {:width (size->width size)}}])

(defn DGSubTypeSelector
  [control]
  (let [{:keys [::!value ::set-value-fn ::!size ::!disabled?]} control]
    (let [on-click #(set-value-fn %)]
      [ui/CardGroup {:centered true}
       [ui/Card
        (cond-> {:style  {:width (size->width @!size)}
                 :class  (cond-> ["docker-compose"]
                                 @!disabled? (conj "disabled"))
                 :raised (not @!disabled?)}
                (not @!disabled?)
                (assoc :on-click (partial on-click :docker-compose)))
        [ui/CardContent {:style {:display         :flex
                                 :justify-content :center
                                 :align-items     :center}}
         [ui/IconGroup {:size @!size}
          [DockerIcon {:selected? (= :docker-compose @!value)
                       :size      @!size}]]]]
       [ui/Card
        (cond-> {:style {:width (size->width @!size)}
                 :class "docker-swarm"}
                (not @!disabled?)
                (assoc :on-click (partial on-click :docker-swarm)))
        [ui/CardContent {:style {:display         :flex
                                 :justify-content :center
                                 :align-items     :center}}
         [ui/IconGroup {:size @!size}
          [DockerSwarmIcon {:selected? (= :docker-swarm @!value)
                            :size      @!size}]]]]
       [ui/Card
        (cond-> {:style {:width (size->width @!size)}
                 :class "kubernetes"}
                (not @!disabled?)
                (assoc :on-click (partial on-click :kubernetes)))
        [ui/CardContent {:style {:display         :flex
                                 :justify-content :center
                                 :align-items     :center}}
         [ui/IconGroup {:size @!size}
          [KubernetesIcon {:selected? (= :kubernetes @!value)
                           :size      @!size}]]]]])))

(defn DGSubTypeSelectorController
  [{:keys [!value

           ;; Optional
           set-value-fn

           ;; Optional
           ;; one of: [:small :large :big :huge]
           !size

           ;; Optional
           ;; whether the selector should be disabled
           !disabled?
           ]}]
  (r/with-let [set-value-fn (or set-value-fn #(reset! !value %))
               !size        (or !size (r/atom :big))
               !disabled?   (or !disabled? (r/atom false))]
    [DGSubTypeSelector {::!value       !value
                        ::set-value-fn set-value-fn
                        ::!size        !size
                        ::!disabled?   !disabled?}]))
