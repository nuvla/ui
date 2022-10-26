(ns sixsq.nuvla.ui.utils.time
  (:require
   ["date-fns" :as dfn]
   ["date-fns/locale" :refer [fr]]))

(def ^:const default-locale "en")
(def ^:const locale-string->locale-object {"fr" fr})

(defn- ^:const get-locale-object [locale]
  (some->> (locale-string->locale-object locale)
           (assoc {} :locale)
           (clj->js)))


(defn timestamp
  []
  (/ (.now js/Date) 1000))


(defn now
  ([]
   (js/Date.))
  ([_]
   (js/Date.)))


(defn add-milliseconds
  [date milliseconds]
  (dfn/addMilliseconds date milliseconds))


(defn subtract-milliseconds
  [date milliseconds]
  (dfn/subMilliseconds date milliseconds))


(defn parse-iso8601
  ([iso8601]
   (parse-iso8601 iso8601 default-locale))
  ([iso8601 _]
   (if (string? iso8601)
     (dfn/parseISO iso8601)
     (js/Date. iso8601))))

(defn parse-unix
  ([unix-timestamp]
   (dfn/fromUnixTime unix-timestamp))
  ([unix-timestamp _]
   (dfn/fromUnixTime unix-timestamp)))


(defn invalid
  ([] "Invalid date")
  ([_]
   "Invalid date"))


(defn delta-milliseconds
  "Returns the difference in the given date-time instances in milliseconds."
  ([start]
   (delta-milliseconds start (now)))
  ([start end]
   (dfn/differenceInMilliseconds (parse-iso8601 end) (parse-iso8601 start))))

(def units-in-ms [["year"     (* 1000 60 60 24 365)]
                  ["month"    (* 1000 60 60 24 31)]
                  ["day"      (* 1000 60 60 24)]
                  ["hour"     (* 1000 60 60)]
                  ["minute"   (* 1000 60)]
                  ["second"   0]])

(defn ago
  "Returns a human-readable string on how much time is remaining before the
   given expiry date (javascript Date). Uses English as the natural language
   unless another locale is given."
  ([date]
   (ago date default-locale))
  ([date locale]
  (let [n (now)
        unit (some #(when (<= (second %) (Math/abs (delta-milliseconds date n))) (first %)) units-in-ms)]
    (or (some-> date (dfn/intlFormatDistance (now) (clj->js {:locale locale
                                                             :numeric "always"
                                                             :unit unit})))
        (invalid locale)))))
(ago
 (subtract-milliseconds (js/Date.) 1000000))

(defn before-now?
  [iso8601]
  (let [^js ts (parse-iso8601 iso8601)]
    (dfn/isBefore ts (js/Date.))))


(defn after-now?
  [iso8601]
  (let [^js ts (parse-iso8601 iso8601)]
    (dfn/isAfter ts (js/Date.))))


(defn remaining
  "Returns a human-readable string on how much time is remaining before the
   given expiry date (in ISO8601 format). Uses English as the natural language
   unless another locale is given."
  ([expiry-iso8601]
   (remaining expiry-iso8601 default-locale))
  ([expiry-iso8601 locale]
   (or (some-> expiry-iso8601
               (parse-iso8601)
               (dfn/formatDistance (now) (-> (get-locale-object locale)
                                             (js->clj)
                                             (merge {:includeSeconds true})
                                             (clj->js))))
       (invalid locale))))


(defn delta-minutes
  "Returns the difference in the given date-time instances in minutes."
  ([start]
   (delta-minutes start (now)))
  ([start end]
   (dfn/differenceInMinutes end start)))


(defn days-before
  ([n]
   (days-before n default-locale))
  ([n locale]
   (-> (now locale) (dfn/startOfDay) (dfn/subDays n))))


(defn time-value
  [iso8601]
  (str (-> iso8601 parse-iso8601 ago) " (" iso8601 ")"))


(defn range-equals
  "Checks whether two date ranges (with the start and end times being javascript
   Date objects) are the same. If any of the arguments are nil, then false is
   returned."
  [[start1 end1] [start2 end2]]
  (and start1 end1 start2 end2
       (= start1 start2)
       (= end1 end2)))


(defn time->utc-str
  "Javascript Date to ISO string, milliseconds stripped away"
  [^js date]
  (-> date .toISOString (.split ".") (first) (str "Z")))


(defn time->format
  ([iso8601]
   (time->format iso8601 "yyyy/MM/dd, hh:mm:ss"))
  ([iso8601 custom-format]
   (time->format iso8601 custom-format default-locale))
  ([iso8601 custom-format locale-string]
   (let [moment->date-fns-format {"LLL" "PPP p"
                                  "LL" "PPP"}
         format (or (moment->date-fns-format custom-format) custom-format)
         locale (get-locale-object locale-string)]
     (-> (js/Date. iso8601) (dfn/format format locale)))))


(defn parse-ago
  [time-str locale]
  (some-> time-str parse-iso8601 (ago locale)))
