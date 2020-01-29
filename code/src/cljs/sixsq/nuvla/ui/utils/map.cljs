(ns sixsq.nuvla.ui.utils.map
  (:require
    ["react-leaflet" :as leaflet]
    [reagent.core :as reagent]))

(def Map (reagent/adapt-react-class leaflet/Map))

(def Marker (reagent/adapt-react-class leaflet/Marker))

(def TileLayer (reagent/adapt-react-class leaflet/TileLayer))

(def Popup (reagent/adapt-react-class leaflet/Popup))

(def LayersControl (reagent/adapt-react-class leaflet/LayersControl))

(def BaseLayer (reagent/adapt-react-class leaflet/LayersControl.BaseLayer))