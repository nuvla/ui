(ns sixsq.slipstream.webui.utils.time
  (:require
    ["moment" :as moment]
    [clojure.string :as str]))


(def ^:const default-locale "en")


(def ^:private iso8601-format (.-ISO_8601 moment))


(defn now
  ([]
   (now default-locale))
  ([locale]
   (.locale (moment) locale)))


(defn parse-iso8601
  ([iso8601]
   (parse-iso8601 iso8601 default-locale))
  ([iso8601 locale]
   (moment iso8601 iso8601-format locale true)))


(defn invalid
  ([]
   (invalid default-locale))
  ([locale]
   (-> moment .invalid .clone (.locale locale) .format)))


(defn ago
  "Returns a human-readable string on how much time is remaining before the
   given expiry date (as a moment object). Uses English as the natural language
   unless another locale is given."
  ([moment]
   (ago moment default-locale))
  ([moment locale]
   (or (some-> moment .clone (.locale locale) .fromNow)
       (invalid locale))))


(defn before-now?
  [iso8601]
  (let [ts (parse-iso8601 iso8601)]
    (boolean (.isBefore ts (now)))))


(defn after-now?
  [iso8601]
  (let [ts (parse-iso8601 iso8601)]
    (boolean (.isAfter ts (now)))))


(defn remaining
  "Returns a human-readable string on how much time is remaining before the
   given expiry date (in ISO8601 format). Uses English as the natural language
   unless another locale is given."
  ([expiry-iso8601]
   (remaining expiry-iso8601 default-locale))
  ([expiry-iso8601 locale]
   (or (some-> expiry-iso8601 (parse-iso8601) .clone (.locale locale) (.toNow true))
       (invalid locale))))


(defn delta-duration
  ([start]
   (delta-duration start (now)))
  ([start end]
   (let [start-moment (parse-iso8601 start)
         end-moment (parse-iso8601 end)]
     (.duration moment (.diff end-moment start-moment true)))))


(defn delta-minutes
  "Returns the difference in the given date-time instances in minutes."
  ([start]
   (delta-minutes start (now)))
  ([start end]
   (.asMinutes (delta-duration start end))))


(defn delta-humanize
  "Returns the difference in the given date-time instances in natural language
   unless another locale is given."
  ([start locale]
   (delta-humanize start (now) locale))
  ([start end locale]
   (-> (delta-duration start end) .clone (.locale locale) .humanize)))


(defn delta-milliseconds
  "Returns the difference in the given date-time instances in milliseconds."
  ([start]
   (delta-milliseconds start (now)))
  ([start end]
   (.asMilliseconds (delta-duration start end))))


(defn days-before
  ([n]
   (days-before n default-locale))
  ([n locale]
   (-> (now locale) (.startOf "date") (.add (- n) "days"))))


(defn time-value
  [iso8601]
  (str (-> iso8601 parse-iso8601 ago) " (" iso8601 ")"))


(defn range-equals
  "Checks whether two date ranges (with the start and end times being objects
   from Moment.js) are the same. If any of the arguments are nil, then false is
   returned."
  [[start1 end1] [start2 end2]]
  (and start1 end1 start2 end2
       (.isSame start1 start2)
       (.isSame end1 end2)))


(defn time->utc-str
  "Time to UTC string"
  [moment]
  (-> moment .clone .utc .format))
