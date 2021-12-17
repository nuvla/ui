(ns sixsq.nuvla.ui.utils.map
  (:require
    ["react-leaflet" :as leaflet]
    ["react-leaflet-draw" :as react-leaflet-draw]
    ["wellknown" :as wellknown]
    [clojure.string :as str]
    [re-frame.core :refer [subscribe]]
    [reagent.core :as reagent]
    [sixsq.nuvla.ui.config :as config]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [reagent.core :as r]
    [sixsq.nuvla.ui.utils.general :as general-utils]))

(def Map (reagent/adapt-react-class leaflet/Map))

(def FeatureGroup (reagent/adapt-react-class leaflet/FeatureGroup))

(def EditControl (reagent/adapt-react-class react-leaflet-draw/EditControl))

(def Marker (reagent/adapt-react-class leaflet/Marker))

(def TileLayer (reagent/adapt-react-class leaflet/TileLayer))

(def Popup (reagent/adapt-react-class leaflet/Popup))

(def LayersControl (reagent/adapt-react-class leaflet/LayersControl))

(def BaseLayer (reagent/adapt-react-class leaflet/LayersControl.BaseLayer))

(def CircleMarker (reagent/adapt-react-class leaflet/CircleMarker))

(def Circle (reagent/adapt-react-class leaflet/Circle))

(def Rectangle (reagent/adapt-react-class leaflet/Rectangle))

(def Polygon (reagent/adapt-react-class leaflet/Polygon))

(def GeoJSON (reagent/adapt-react-class leaflet/GeoJSON))

(def Tooltip (reagent/adapt-react-class leaflet/Tooltip))

(def attributions
  (str/join " "
            ["© <a href=\"https://www.mapbox.com/about/maps/\">Mapbox</a>"
             "© <a href=\"http://www.openstreetmap.org/copyright\">OpenStreetMap</a>"
             "<strong><a href=\"https://www.mapbox.com/map-feedback/\" target=\"_blank\">Improve this map</a></strong>"]))


(defn DefaultLayers
  []
  (let [access-token (subscribe [::main-subs/config :mapbox-access-token])]
    [LayersControl {:position "topright"}
     (when @access-token
       [:<>
        [BaseLayer {:name    "Light"
                    :checked (not config/debug?)}
         [TileLayer {:url         (str "https://api.mapbox.com/styles/v1/mapbox/light-v9/tiles/{z}/{x}/{y}?access_token=" @access-token)
                     :attribution attributions}]]
        [BaseLayer {:name "Streets"}
         [TileLayer {:url         (str "https://api.mapbox.com/styles/v1/mapbox/streets-v9/tiles/{z}/{x}/{y}?access_token=" @access-token)
                     :attribution attributions}]]
        [BaseLayer {:name "Satellite"}
         [TileLayer {:url         (str "https://api.mapbox.com/styles/v1/mapbox/satellite-streets-v9/tiles/{z}/{x}/{y}?access_token=" @access-token)
                     :attribution attributions}]]])
     (when config/debug?
       [BaseLayer {:name    "Light cartodb dev"
                   :checked config/debug?}
        [TileLayer {:url         "https://cartodb-basemaps-{s}.global.ssl.fastly.net/light_all/{z}/{x}/{y}.png"
                    :attribution "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a>, &copy; <a href=\"https://carto.com/attributions\">Carto</a>"}]])]))


(defn normalize-lng
  [lng]
  (-> lng (mod 360) (+ 540) (mod 360) (- 180)))


(defn convert-latlong-map
  [^js latlong]
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


(def default-latlng-center [45, 0])

#_(defn normalize-lat-lng
    [^js latlng]
    (.map latlng #(.wrap %1)))

#_(defn normalize-lat-lngs
    [^js latlngs]
    (.map latlngs normalize-lat-lng))


(defn normalize-lng-coordinates
  [coordinates]
  (mapv #(update %1 0 normalize-lng) coordinates))


(defn normalize-lng-geojson
  [geojson]
  (update-in geojson [:geometry :coordinates] #(mapv normalize-lng-coordinates %1)))


(defn geojson->filter [attribute operation geojson]
  (some-> geojson
          normalize-lng-geojson
          clj->js
          wellknown/stringify
          (as-> wkt (str attribute " " operation " '" wkt "'"))))


(def map-default-ops
  {:min-zoom           2
   :style              {:height 500}
   :center             default-latlng-center
   :worldCopyJump      true
   :zoom               3})


(defn MapBox
  [opts content]
  [:div {:className "mapbox-map"}
   [:a {:className "mapbox-wordmark"
        :href      "http://mapbox.com/about/maps"
        :target    "_blank"} "Mapbox"]
   [Map (merge map-default-ops opts)
    [DefaultLayers]
    content]])


(defn MapBoxEdit
  [opts content]
  [MapBox (merge map-default-ops opts)
   [:<>
    [FeatureGroup
     [EditControl opts]]
    content]])
