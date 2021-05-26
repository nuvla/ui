(ns sixsq.nuvla.ui.data-record.utils
  (:require
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.time :as time]))


(defn create-time-period-filter
  [[time-start time-end]]
  (str "(timestamp>='"
       (time/time->utc-str time-start)
       "' and timestamp<'"
       (time/time->utc-str time-end)
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


(defn get-query-params
  [data-record-filter full-text-search time-period-filter page elements-per-page]
  {:first   (inc (* (dec page) elements-per-page))
   :last    (* page elements-per-page)
   :orderby "timestamp:desc"
   :filter  (general-utils/join-and
              time-period-filter
              data-record-filter
              (general-utils/fulltext-query-string full-text-search))})
