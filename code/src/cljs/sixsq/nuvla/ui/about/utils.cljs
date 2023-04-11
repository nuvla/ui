(ns sixsq.nuvla.ui.about.utils
  (:require [clojure.set :as set]))

(def feature-flags [{:k     "deployment-set"
                     :label "Page deployment set visible in left menu."}
                    {:k     "applications-sets"
                     :label "Applications sets support"}])

(def existing-feature-flags-keys (->> feature-flags (map :k) set))

(defn set-feature-flag
  [enabled-feature-flags k enable?]
  ((if enable? conj disj) enabled-feature-flags k))

(defn feature-flag-enabled?
  [enabled-feature-flags k]
  (contains? enabled-feature-flags k))

(defn keep-exsiting-feature-flags
  [enabled-feature-flags]
  (set/intersection existing-feature-flags-keys enabled-feature-flags))
