(ns sixsq.nuvla.ui.utils.time
  (:require ["date-fns" :refer [addMilliseconds
                                differenceInMilliseconds
                                differenceInMinutes
                                format
                                formatDistance
                                intlFormatDistance
                                isAfter
                                isBefore
                                parseISO
                                startOfDay
                                subDays
                                subWeeks
                                subMonths
                                subYears
                                subMilliseconds
                                subMinutes
                                interval
                                eachDayOfInterval
                                eachHourOfInterval
                                eachMinuteOfInterval
                                getHours]]
            ["date-fns/locale/fr$default" :as fr]))

(def ^:const default-locale "en")
(def ^:const locale-string->locale-object {"fr" fr})

(defn- ^:const get-locale-object [locale]
  (some->> (locale-string->locale-object locale)
           (assoc {} :locale)))


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
  (addMilliseconds date milliseconds))


(defn subtract-milliseconds
  [date milliseconds]
  (subMilliseconds date milliseconds))

(defn subtract-minutes
  [date minutes]
  (subMinutes date minutes))

(defn subtract-days
  [date days]
  (subDays date days))

(defn subtract-months
  [date months]
  (subMonths date months))

(defn subtract-years
  [date years]
  (subYears date years))

(defn parse-iso8601
  ([iso8601]
   (parse-iso8601 iso8601 default-locale))
  ([iso8601 _]
   (if (string? iso8601)
     (parseISO iso8601)
     (js/Date. iso8601))))

(defn parse-unix
  ([unix-timestamp]
   (js/Date. (* 1000 unix-timestamp)))
  ([unix-timestamp _]
   (js/Date. (* 1000 unix-timestamp))))


(defn invalid
  ([] "Invalid date")
  ([_]
   "Invalid date"))


(defn delta-milliseconds
  "Returns the difference in the given date-time instances in milliseconds."
  ([start]
   (delta-milliseconds start (now)))
  ([start end]
   (differenceInMilliseconds (parse-iso8601 end) (parse-iso8601 start))))

(def units-in-ms [["year" (* 1000 60 60 24 365)]
                  ["month" (* 1000 60 60 24 31)]
                  ["day" (* 1000 60 60 24)]
                  ["hour" (* 1000 60 60)]
                  ["minute" (* 1000 60)]
                  ["second" 0]])

(defn ago
  "Returns a human-readable string on how much time is remaining before the
   given date (javascript Date). Uses English as the natural language
   unless another locale is given."
  ([date]
   (ago date default-locale))
  ([date locale]
   (let [n    (now)
         unit (some #(when (<= (second %) (Math/abs (delta-milliseconds date n))) (first %)) units-in-ms)]
     (or (some-> date (intlFormatDistance (now) (clj->js {:locale  locale
                                                          :numeric "always"
                                                          :unit    unit})))
         (invalid locale)))))


(defn before-now?
  [iso8601]
  (let [^js ts (parse-iso8601 iso8601)]
    (isBefore ts (js/Date.))))


(defn after-now?
  [iso8601]
  (let [^js ts (parse-iso8601 iso8601)]
    (isAfter ts (js/Date.))))


(defn format-distance
  "Return the distance between the given date and now in words.
   Uses English as the natural language unless another locale is given."
  ([date]
   (format-distance date default-locale))
  ([expiry-iso8601 locale]
   (or (some-> expiry-iso8601
               (parse-iso8601)
               (formatDistance (now) (-> (get-locale-object locale)
                                         (merge {:includeSeconds true})
                                         (clj->js))))
       (invalid locale))))


(defn delta-minutes
  "Returns the difference in the given date-time instances in minutes."
  ([start]
   (delta-minutes start (now)))
  ([start end]
   (differenceInMinutes (parse-iso8601 end) (parse-iso8601 start))))


(defn days-before
  ([n]
   (days-before n default-locale))
  ([n locale]
   (-> (now locale) (startOfDay) (subDays n))))


(defn months-before
  ([n]
   (months-before n default-locale))
  ([n locale]
   (-> (now locale) (startOfDay) (subMonths n))))


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
                                  "LL"  "PPP"}
         format-string           (or (moment->date-fns-format custom-format) custom-format)
         locale                  (clj->js (get-locale-object locale-string))]
     (-> (js/Date. iso8601) (format format-string locale)))))


(defn parse-ago
  ([time-str]
   (some-> time-str parse-iso8601 ago))
  ([time-str locale]
   (some-> time-str parse-iso8601 (ago locale))))

(defn hours-between [{:keys [start-date end-date]}]
  (eachHourOfInterval (clj->js {:start start-date
                                :end   end-date})))

(defn days-between [{:keys [start-date end-date]}]
  (eachDayOfInterval (clj->js {:start start-date
                                :end   end-date})))

(defn minutes-between [{:keys [start-date end-date]}]
  (eachMinuteOfInterval (clj->js {:start start-date
                                :end   end-date})))

(defn before? [date1 date2]
  (isBefore date1 date2))

(getHours #inst"2023-08-31T12:00:00.000-00:00")



