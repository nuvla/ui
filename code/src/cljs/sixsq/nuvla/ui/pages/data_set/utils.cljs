(ns sixsq.nuvla.ui.pages.data-set.utils
  (:require [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.map :as map]
            [sixsq.nuvla.ui.utils.time :as time]))

(defn create-time-period-filter
  [[time-start time-end]]
  (str "(timestamp>='"
       (time/time->utc-str time-start)
       "' and timestamp<'"
       (time/time->utc-str time-end)
       "')"))


(defn data-record-geometry-filter
  [geo-operation data-record-map-geojson]
  (when data-record-map-geojson
    (if (= geo-operation "intersects")
      (general-utils/join-or
        (map/geojson->filter "geometry" geo-operation data-record-map-geojson)
        (map/geojson->filter "location" geo-operation data-record-map-geojson))
      (map/geojson->filter "geometry" geo-operation data-record-map-geojson))))
