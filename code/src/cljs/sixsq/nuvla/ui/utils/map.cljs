(ns sixsq.nuvla.ui.utils.map
  (:require
    ["react-leaflet" :as leaflet]
    [clojure.string :as str]
    [reagent.core :as reagent]
    [sixsq.nuvla.ui.config :as config]))

(def Map (reagent/adapt-react-class leaflet/Map))

(def Marker (reagent/adapt-react-class leaflet/Marker))

(def TileLayer (reagent/adapt-react-class leaflet/TileLayer))

(def Popup (reagent/adapt-react-class leaflet/Popup))

(def LayersControl (reagent/adapt-react-class leaflet/LayersControl))

(def BaseLayer (reagent/adapt-react-class leaflet/LayersControl.BaseLayer))

(def CircleMarker (reagent/adapt-react-class leaflet/CircleMarker))

(def Circle (reagent/adapt-react-class leaflet/Circle))

(def Tooltip (reagent/adapt-react-class leaflet/Tooltip))

(def attributions
  (str/join " "
            ["© <a href=\"https://www.mapbox.com/about/maps/\">Mapbox</a>"
             "© <a href=\"http://www.openstreetmap.org/copyright\">OpenStreetMap</a>"
             "<strong><a href=\"https://www.mapbox.com/map-feedback/\" target=\"_blank\">Improve this map</a></strong>"]))

(defn DefaultLayers
  []
  (let [access-token "pk.eyJ1IjoiMHhiYXNlMTIiLCJhIjoiY2s2Yzhsa2hzMGo3MzNsb3ZxaWo3ZGE3NiJ9.s0_WxYEqHBpsoc3LmdaPWg"]
    [LayersControl {:position "topright"}
     [BaseLayer {:name    "Light"
                 :checked (not config/debug?)}
      [TileLayer {:url         (str "https://api.mapbox.com/styles/v1/mapbox/light-v9/tiles/{z}/{x}/{y}?access_token=" access-token)
                  :attribution attributions}]]
     [BaseLayer {:name "Streets"}
      [TileLayer {:url         (str "https://api.mapbox.com/styles/v1/mapbox/streets-v9/tiles/{z}/{x}/{y}?access_token=" access-token)
                  :attribution attributions}]]
     [BaseLayer {:name "Satellite"}
      [TileLayer {:url         (str "https://api.mapbox.com/styles/v1/mapbox/satellite-streets-v9/tiles/{z}/{x}/{y}?access_token=" access-token)
                  :attribution attributions}]]
     (when config/debug?
       [BaseLayer {:name    "Light cartodb dev"
                   :checked config/debug?}
        [TileLayer {:url "https://cartodb-basemaps-{s}.global.ssl.fastly.net/light_all/{z}/{x}/{y}.png"
                    :attribution "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a>, &copy; <a href=\"https://carto.com/attributions\">Carto</a>"
                    }]])
     ]))


(defn normalize-lng
  [lng]
  (-> lng (mod 360) (+ 540) (mod 360) (- 180)))


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


(defn MapBox
  [opts content]
  [:div {:className "mapbox-map"}
   [:a {:className "mapbox-wordmark"
        :href      "http://mapbox.com/about/maps"
        :target    "_blank"} "Mapbox"]
   [Map opts
    [DefaultLayers]
    content]])
