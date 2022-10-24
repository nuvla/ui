(ns sixsq.nuvla.ui.data-set.utils
  (:require
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.map :as map]
    [sixsq.nuvla.ui.utils.time :as time]))

(defn create-time-period-filter
  [[time-start time-end]]
  (str "(timestamp>='"
       (time/js-date->utc-str time-start)
       "' and timestamp<'"
       (time/js-date->utc-str time-end)
       "')"))

(defn format-bytes
  [bytes]
  (if (number? bytes)
    (let [scale 1024
          units ["B" "KiB" "MiB" "GiB" "TiB" "PiB" "EiB"]]
      (if (< bytes scale)
        (str bytes " B")
        (let [exp    (int (/ (js/Math.log bytes) (js/Math.log scale)))
              prefix (get units exp)
              v      (/ bytes (js/Math.pow scale exp))]
          (str (general-utils/round-up v :n-decimal 1) prefix))))
    "..."))

(defn data-record-geometry-filter
  [geo-operation data-record-map-geojson]
  (when data-record-map-geojson
    (if (= geo-operation "intersects")
      (general-utils/join-or
        (map/geojson->filter "geometry" geo-operation data-record-map-geojson)
        (map/geojson->filter "location" geo-operation data-record-map-geojson))
      (map/geojson->filter "geometry" geo-operation data-record-map-geojson))))
