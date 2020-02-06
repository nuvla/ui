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

(def CircleMarker (reagent/adapt-react-class leaflet/CircleMarker))

(def Circle (reagent/adapt-react-class leaflet/Circle))

(def Tooltip (reagent/adapt-react-class leaflet/Tooltip))


(defn DefaultLayers
  []
  [LayersControl {:position "topright"}
   [BaseLayer {:name    "Light"
               :checked true}
    [TileLayer {:url "https://cartodb-basemaps-{s}.global.ssl.fastly.net/light_all/{z}/{x}/{y}.png"
                :attribution "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a>, &copy; <a href=\"https://carto.com/attributions\">Carto</a>"}]]
   [BaseLayer {:name "Classic"}
    [TileLayer {:url         "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                :attribution "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a>"}]]])


(defn convert-latlong-map
  [latlong]
  [(.-lng latlong) (.-lat latlong)])


(defn longlat->latlong
  [[lng lat]]
  [lat lng])


(defn click-location
  [callback]
  (fn [event]
    (let [latlong (.-latlng event)]
      (callback (convert-latlong-map latlong)))))


(defn drag-end-location
  [callback]
  (fn [event]
    (let [latlong (-> event .-target .getLatLng)]
      (callback (convert-latlong-map latlong)))))


(def sixsq-latlng [46.2273, 6.07661])
