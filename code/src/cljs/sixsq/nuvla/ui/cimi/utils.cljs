(ns sixsq.nuvla.ui.cimi.utils
  (:require [clojure.string :as str]))


(def template-suffix "-template")


(defn collection-href-map
  "Creates a map from the CloudEntryPoint that maps the resource collection
   key (as a keyword) to the href for the collection (as a string)."
  [{:keys [collections] :as cep}]
  (when cep
    (->> collections
         (map (juxt first #(:href (second %))))
         (into {}))))


(defn collection-key-map
  "Creates a map from the CloudEntryPoint that maps the collections href (as a
   string) to the key for the collection (as a keyword)."
  [cep]
  (when cep
    (into {} (->> cep
                  collection-href-map
                  (map (juxt second first))))))


(defn collections-template-map
  "Creates a map with only templates collections href as key and nil as a value."
  [collection-href-map]
  (into {} (->> collection-href-map
                (filter #(-> % second (str/ends-with? template-suffix)))
                (map (juxt #(-> % second keyword) (constantly nil))))))


(defn collection-template-href
  "Returns the collection template href for the collection href."
  [collection-href]
  (-> collection-href name (str template-suffix) keyword))


(defn template-resource-key
  "Returns the collection keyword for the template resource associated with
   the given collection. If there is no template resource, then nil is
   returned."
  [cloud-entry-point collection-href]
  (when-let [href->key (:collection-key cloud-entry-point)]
    (href->key (str collection-href template-suffix))))
