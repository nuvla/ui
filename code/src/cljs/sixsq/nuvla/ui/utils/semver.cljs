(ns sixsq.nuvla.ui.utils.semver
  (:require [clojure.string :as str]))

(defrecord Version
  [major minor patch pre-release build])

(def ^{:private true} pattern
  #"^(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?(?:\+([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?$")

(defn valid?
  [version]
  (boolean (re-matches pattern version)))

(defn try-parse-int
  [s]
  (try
    (js/parseInt s)
    (catch :default _)))

(defn parse
  [s]
  (when-let [[[_ major minor patch pre-release build]] (and
                                                         (string? s)
                                                         (valid? s)
                                                         (re-seq pattern s))]
    (Version.
      (try-parse-int major)
      (try-parse-int minor)
      (try-parse-int patch)
      pre-release
      build)))

(defn- cmp
  "Compares versions a and b, returning -1 if a is older than b, 0 if
  they're the same version, and 1 if a is newer than b"
  [a b]
  (let [key-for-ident #(when % (into [] (map try-parse-int (str/split % #"\."))))
        k             (juxt :major
                            :minor
                            :patch
                            ;; Because non-existent pre-release tags take
                            ;; precedence over existing ones
                            #(nil? (:pre-release %))
                            #(key-for-ident (:pre-release %))
                            #(key-for-ident (:build %)))]
    (compare (k a) (k b))))

(defn newer?
  [a b]
  (pos? (cmp a b)))

(defn older?
  [a b]
  (neg? (cmp a b)))

(defn equal?
  [a b]
  (zero? (cmp a b)))

(defn sort-versions
  [versions]
  (sort newer? versions))
