(ns sixsq.nuvla.ui.utils.dnd
  "Mapping of names of Semantic UI components to the Soda Ash wrappers. This
   namespace has no real functionality; it just keeps Cursive from complaining
   about undefined symbols."
  (:require ["@dnd-kit/core" :as dnd-core]
            ["@dnd-kit/modifiers" :as dnd-modifiers]
            ["@dnd-kit/sortable" :as dnd-sortable]
            ["@dnd-kit/utilities" :as dnd-utilities]
            [reagent.core :as r]))

(def DndContext (r/adapt-react-class dnd-core/DndContext))
(def SortableContext (r/adapt-react-class dnd-sortable/SortableContext))

(def useSortable dnd-sortable/useSortable)
(def useSensors dnd-core/useSensors)
(def useSensor dnd-core/useSensor)
(def closestCenter dnd-core/closestCenter)
(def restrictToHorizontalAxis dnd-modifiers/restrictToHorizontalAxis)
(def horizontalListSortingStrategy dnd-sortable/horizontalListSortingStrategy)

(defn pointerSensor
  []
  (useSensors (useSensor dnd-core/PointerSensor #js {"activationConstraint" #js {"distance" 5}})))

(defn translate-css
  [sortable]
  (.toString (.-Translate dnd-utilities/CSS) (.-transform sortable)))