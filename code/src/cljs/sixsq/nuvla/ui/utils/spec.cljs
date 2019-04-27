(ns sixsq.nuvla.ui.utils.spec
  (:require [clojure.string :as str]))

(defn nonblank-string [s]
  (let [str-s (str s)]
    (when-not (str/blank? str-s)
      str-s)))
