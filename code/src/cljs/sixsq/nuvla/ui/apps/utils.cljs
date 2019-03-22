(ns sixsq.nuvla.ui.apps.utils
  (:require
    [clojure.string :as str]))


(defn nav-path->module-path
  [nav-path]
  (some->> nav-path rest seq (str/join "/")))


(defn nav-path->parent-path
  [nav-path]
  (some->> nav-path rest drop-last seq (str/join "/")))


(defn nav-path->module-name
  [nav-path]
  (some->> nav-path rest last))


(defn category-icon
  [category]
  (case category
    "PROJECT" "folder"
    "APPLICATION" "sitemap"
    "IMAGE" "file"
    "COMPONENT" "microchip"
    "question circle"))


(defn meta-category-icon
  [category]
  (if (= "PROJECT" category)
    "folder open"
    (category-icon category)))


;; Sanitize before serialization to server

(defn sanitize-module-component
  [module]
   module)

(defn sanitize-module-project
  [module]
  (dissoc module :children))

(defn sanitize-module
  [module]
  (let [type (:type module)]
    (cond
      (= "COMPONENT" type) (sanitize-module-component module)
      (= "PROJECT" type) (sanitize-module-project module)
      :else module)))

