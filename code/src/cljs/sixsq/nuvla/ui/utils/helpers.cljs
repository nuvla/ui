(ns sixsq.nuvla.ui.utils.helpers)

(defn update-values
  [m f]
  (reduce-kv
    (fn [acu k v]
      (assoc acu k (f v)))
    {}
    m))
