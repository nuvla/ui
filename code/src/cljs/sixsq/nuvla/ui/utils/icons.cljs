(ns sixsq.nuvla.ui.utils.icons
  (:require [clojure.string :as str]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(defn Icon
  [{:keys [name] :as opts}]
  [ui/Icon
   (if (some #(str/starts-with? name %) ["fa-" "fal " "fad " "fas "])
     (-> opts (dissoc :name) (assoc :class name))
     opts)])

(def rocket "fal fa-rocket-launch")


(defn RocketIcon
  []
  [Icon {:name rocket}])