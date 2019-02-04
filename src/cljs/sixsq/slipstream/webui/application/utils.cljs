(ns sixsq.slipstream.webui.application.utils
  (:require
    [clojure.string :as str]))


(defn nav-path->module-path
  [nav-path]
  (some->> nav-path rest seq (str/join "/")))


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
