(ns sixsq.nuvla.ui.data.utils
  (:require
    [clojure.pprint :refer [cl-format]]
    [clojure.string :as str]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.time :as time]
    [taoensso.timbre :as log]))


(defn matches-parameter-name?
  [parameter-name parameter]
  (and parameter-name (= parameter-name (:parameter parameter))))


(defn update-parameter-in-list
  [name value parameters]
  (let [f (partial matches-parameter-name? name)
        current (first (filter f parameters))               ;; FIXME: Use group-by instead?
        others (remove f parameters)]
    (if current
      (->> (assoc current :value value)
           (conj others)
           (sort-by :parameter)
           vec))))


(defn update-parameter-in-deployment
  [name value deployment]
  (->> deployment
       :module
       :content
       :inputParameters
       (update-parameter-in-list name value)
       (assoc-in deployment [:module :content :inputParameters])))


(defn create-time-period-filter
  [[time-start time-end]]
  (str "(timestamp>='"
       (time/time->utc-str time-start)
       "' and timestamp<'"
       (time/time->utc-str time-end)
       "')"))


(defn create-infra-service-filter
  [credentials]
  (let [creds-services (->> credentials
                            (map :services)
                            (remove nil?)
                            (sequence cat))]
    (some->> creds-services
             (seq)
             (map #(str "infrastructure-service/href='" % "'"))
             (str/join " or "))))


(defn join-filters
  [op filters]
  (->> filters
       (remove nil?)
       (map #(str "(" % ")"))
       (str/join (str " " op " "))))


(defn join-or
  [& filters]
  (join-filters "or" filters))


(defn join-and
  [& filters]
  (join-filters "and" filters))


(defn format-bytes
  [bytes]
  (if (number? bytes)
    (let [scale 1024
          units ["B" "KiB" "MiB" "GiB" "TiB" "PiB" "EiB"]]
      (if (< bytes scale)
        (str bytes " B")
        (let [exp (int (/ (.log js/Math bytes) (.log js/Math scale)))
              prefix (get units exp)
              v (/ bytes (.pow js/Math scale exp))]
          (cl-format nil "~,1F ~a" v prefix))))
    "..."))
