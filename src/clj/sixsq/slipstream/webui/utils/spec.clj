(ns sixsq.slipstream.webui.utils.spec
  "Utility from Alistair Roche on the clojure mailing list for creating a
   closed map definition."
  (:require
    [clojure.set :as set]
    [clojure.spec.alpha :as s]))


(defmacro only-keys
  "Map definition that includes only the defined keys and no others, that is a
   'closed' map."
  [& {:keys [req req-un opt opt-un] :as kw-args}]
  `(s/merge (s/keys ~@(apply concat (vec kw-args)))
            (s/map-of ~(set (concat req
                                    (set (map (comp keyword name) req-un))
                                    opt
                                    (set (map (comp keyword name) opt-un))))
                      any?)))
