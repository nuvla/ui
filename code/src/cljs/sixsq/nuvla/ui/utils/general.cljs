(ns sixsq.nuvla.ui.utils.general
  (:require ["js-yaml" :as js-yaml]
            [clojure.set :as set]
            [clojure.string :as str]
            [goog.string :as gstring]
            [goog.string.format]
            [sixsq.nuvla.ui.session.utils :as session-utils]
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


(defn capitalize-first-letter
  [s]
  (some-> (first s)
          str/upper-case
          (str (str/join "" (rest s)))))


(defn name->display-name
  [name]
  (some-> name
          (str/replace #"-" " ")
          (capitalize-words)))


;;
;; json/edn conversions
;;


(defn edn->json [edn & {:keys [spaces] :or {spaces 2}}]
  (.stringify js/JSON (clj->js edn) nil spaces))


(defn json->edn [json & {:keys [keywordize-keys] :or {keywordize-keys true}}]
  (try
    (js->clj (.parse js/JSON json) :keywordize-keys keywordize-keys)
    (catch js/Error e
      (js/console.error "Parsing json failed: " e json)
      false)))


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
  [_resource-type form-data]
  (let [[common-map template-map] (split-form-data form-data)]
    (assoc common-map :template template-map)))

(defn id-split
  [id f]
  (cond-> (str/split id #"/")
          f f))

(defn id->uuid
  [id]
  (id-split id second))

(defn id->resource-name
  [id]
  (id-split id first))

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
       (remove str/blank?)
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


(defn can-terminate? [data]
  (can-operation? :terminate data))


(defn can-stop? [data]
  (can-operation? :stop data))


(defn can-start? [data]
  (can-operation? :start data))


(defn can-bulk-delete? [data]
  (can-operation? :bulk-delete data))


(defn editable?
  [data is-new?]
  (or is-new? (can-edit? data)))


(defn mandatory-icon
  []
  [:sup " "
   [ui/Icon {:name  :asterisk
             :size  :tiny
             :color :red}]])


(defn mandatory-name
  [name]
  [:span name [mandatory-icon]])


(defn fulltext-query-string
  ([fulltext]
   (fulltext-query-string "fulltext" fulltext))
  ([field fulltext]
   (when-not (str/blank? fulltext)
     (str field
          "=='"
          (-> fulltext
              (str/escape {\' "\\'", \\ "\\\\"})
              (str/split #" ")
              (->> (remove str/blank?)
                   (map #(str % "*"))
                   (str/join " ")))
          "'"))))


(defn published-query-string
  []
  "published=true")


(defn owner-like-query-string
  [owner]
  (when-not (str/blank? owner)
    (str "acl/owners=='" owner "'")))


(defn by-tag-query-string
  [tag]
  (when-not (str/blank? tag)
    (str "tags=='" tag "'")))

(defn create-filter-for-read-only-resources
  [session selected-filter]
  (join-and
    (apply
      join-and
      (mapcat
        (fn [role]
          [(str "acl/owners!='" role "'")
           (str "acl/edit-meta!='" role "'")])
        (session-utils/get-roles session)))
    selected-filter))

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


(defn aggregate-to-map
  "convert the aggregate structure returned by Nuvla into a terms/value map"
  [aggregate]
  (into {} (for [a aggregate] {(keyword (str (:key a))) (:doc_count a)})))

(defn sanitize-name [name]
  (when name
    (str/lower-case
      (str/replace
        (str/trim
          (str/join "" (re-seq #"[a-zA-Z0-9\ ]" name)))
        " " "-"))))

(defn regex-escape
  "Escapes regex special chars in the string s."
  [s]
  (str/escape
    s
    {\- "\\-", \[ "\\[", \] "\\]", \{ "\\{", \} "\\}",
     \( "\\(", \) "\\)", \* "\\*", \+ "\\+", \? "\\?",
     \. "\\.", \\ "\\\\", \^ "\\^", \$ "\\$", \| "\\|"}))

(defn envsubst-str
  [envsubst s]
  (str/replace s (->> envsubst keys (map regex-escape) (str/join "|") re-pattern) envsubst))

(defn- read-fav-language!
  "Reads js/window.navigator.languages and returns first element or 'en-US'."
  []
  (-> js/window
      .-navigator
      .-languages
      first
      (or "en-US")))

(def browser-fav-language (read-fav-language!))

(defn format-number
  ([amount]
   (format-number amount {:locale browser-fav-language}))
  ([amount {:keys [locale style currency]
            :or {locale   browser-fav-language
                 style    "decimal"
                 currency "EUR"}}]
   (.format (js/Intl.NumberFormat. locale
                                   (clj->js {:style style
                                             :currency currency}))
            amount)))

(defn format-money
  ([amount]
   (format-number amount {:style  "currency"}))
  ([amount opts]
   (format-number amount (merge opts {:style "currency"}))))