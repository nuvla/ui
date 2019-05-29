(ns sixsq.nuvla.ui.dashboard.utils
  (:require [clojure.string :as str]))

(defn resolve-url-pattern
  "When url-pattern is passed and all used params in pattern has values in
  deployment-parameters it return a resolved url otherwise nil"
  [url-pattern deployment-parameters]
  (when url-pattern
    (let [pattern-in-params (re-seq #"\$\{([^}]+)\}+" url-pattern)
          pattern-value     (map (fn [[param-pattern param-name]]
                                   (some->> (get deployment-parameters param-name)
                                            :value
                                            (conj [param-pattern])))
                                 pattern-in-params)]
      (when (every? some? pattern-value)
        (reduce
          (fn [url [param-pattern param-value]]
            (str/replace url param-pattern param-value))
          url-pattern pattern-value)))))