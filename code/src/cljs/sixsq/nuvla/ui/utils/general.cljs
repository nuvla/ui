(ns sixsq.nuvla.ui.utils.general
  (:require
    ["js-yaml" :as js-yaml]
    [clojure.set :as set]
    [clojure.string :as str]
    [goog.string :as gstring]
    [goog.string.format]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]))


(defn str->int
  "Converts a string into an integer. Returns input unchanged for any invalid input."
  [s]
  (if (and (string? s)
           (not (str/blank? s)))
    (let [res (js/Number s)]
      (if (js/isNaN res) s res))
    s))


(defn prepare-params [params]
  (->> params
       (remove (fn [[_ v]] (or (nil? v) (str/blank? (str v)))))
       (into {})))


(defn keys-in [m]
  (if (map? m)
    (vec
      (mapcat (fn [[k v]]
                (let [sub    (keys-in v)
                      nested (map #(into [(name k)] %) (filter (comp not empty?) sub))]
                  (if (seq nested)
                    nested
                    [[(name k)]])))
              m))
    []))


(defn all-keys [m]
  (->> m
       keys-in
       (map #(str/join "/" %))
       set))


(defn merge-keys [coll]
  (->> coll
       (map #(dissoc % :acl :operations))
       (map all-keys)
       (reduce set/union)
       vec
       distinct
       sort
       vec))


(defn parse-resource-path
  "Utility to split a resource path into a vector of terms. Returns an empty
   vector for a nil argument. Removes blank or nil terms from the result.
   The input path will be coerced to a string."
  [path]
  (vec (remove str/blank? (str/split path #"/"))))


;;
;; manual truncation of strings
;;

(def default-truncate-length 20)


(def ellipsis "\u2026")


(defn truncate
  "Truncates a string to the given size and adds the optional suffix if the
   string was actually truncated."
  ([s]
   (truncate s default-truncate-length ellipsis))
  ([s max-size]
   (truncate s max-size ellipsis))
  ([s max-size suffix]
   (if (> (count s) max-size)
     (str (subs s 0 max-size) suffix)
     s)))


(defn capitalize-words
  "Capitalize every word in a string"
  [s]
  (->> (str/split (str s) #"\b")
       (map str/capitalize)
       str/join))


;;
;; json/edn conversions
;;


(defn edn->json [edn & {:keys [spaces] :or {spaces 2}}]
  (.stringify js/JSON (clj->js edn) nil spaces))


(defn json->edn [json & {:keys [keywordize-keys] :or {keywordize-keys true}}]
  (js->clj (.parse js/JSON json) :keywordize-keys keywordize-keys))


(defn yaml->obj
  [yaml]
  (js-yaml/loadAll yaml))


(defn check-yaml
  [yaml]
  (try
    [true (yaml->obj yaml)]
    (catch :default e
      [false (str (js->clj e))])))


;;
;; utilities for random element identifiers
;;

(def default-random-id-size 6)


(def rand-alphanum #(rand-nth (vec "abcdefghijklmnopqrstuvwxyz0123456789")))


(defn random-element-id
  "Random character string that can be used to generate unique element
   identifiers. By default, will produce a string with 6 characters."
  ([]
   (random-element-id default-random-id-size))

  ([n]
   (str/join "" (repeatedly n rand-alphanum))))


;;
;; paging utils
;;


(defn total-pages
  [total-elements elements-per-page]
  (cond-> total-elements
          true (quot elements-per-page)
          (pos? (mod total-elements elements-per-page)) inc))


;;
;; cimi
;;


(def ^:const common-attrs #{:id, :resource-type, :created, :updated, :name, :description, :tags,
                            :parent, :subtype, :properties, :resource-metadata, :operations, :acl})

(defn select-common-attrs
  [resource]
  (select-keys resource common-attrs))


(defn remove-common-attrs
  [resource]
  (->> common-attrs
       (set/difference (set (keys resource)))
       (select-keys resource)))


(defn split-form-data
  [form-data]
  (let [common-attrs #{:name :description :properties}
        common-map   (select-keys form-data common-attrs)
        template-map (into {} (remove #(common-attrs (first %)) form-data))]
    [common-map template-map]))


(defn create-template
  [resource-type form-data]
  (let [[common-map template-map] (split-form-data form-data)]
    (assoc common-map :template template-map)))


(defn id->uuid
  [id]
  (let [[_ uuid] (str/split id #"/")]
    uuid))


(defn id->short-uuid
  [id]
  (let [uuid (id->uuid id)
        [short-uuid] (str/split uuid #"-")]
    short-uuid))


(defn operation-name [op-uri]
  (second (re-matches #"^(?:.*/)?(.+)$" op-uri)))


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

;;
;; ACL utils
;;

(defn can-operation? [operation data]
  (->> data :operations (map :rel) (some #{(name operation)}) nil? not))


(defn can-add? [data]
  (can-operation? :add data))


(defn can-edit? [data]
  (can-operation? :edit data))


(defn can-delete? [data]
  (can-operation? :delete data))


(defn can-bulk-delete? [data]
  (can-operation? :bulk-delete data))


(defn editable?
  [data is-new?]
  (or is-new? (can-edit? data)))

(defn mandatory-name
  [name]
  [:span name [:sup " " [ui/Icon {:name  :asterisk
                                  :size  :tiny
                                  :color :red}]]])


(defn fulltext-query-string
  [fulltext]
  (when-not (str/blank? fulltext)
    (str "fulltext=='"
         (->> (str/split fulltext #" ")
              (remove str/blank?)
              (map #(str % "*"))
              (str/join "+"))
         "'")))


;; Math

(defn round-up
  [value & {:keys [n-decimal] :or {n-decimal 2}}]
  (let [multiplier    (js/Math.pow 10 n-decimal)
        rounded-value (js/Math.round (* value multiplier))]
    (/ rounded-value multiplier)))


(defn percentage
  [used capacity]
  (-> (/ used capacity)
      (* 100)))


(defn mbytes->gbytes
  [mb]
  (/ mb 1024))


(defn format
  [fmt v]
  (gstring/format fmt v))