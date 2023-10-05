(ns sixsq.nuvla.ui.about.utils
  (:require [clojure.set :as set]))

(def feature-deployment-set-key "deployment-set")
(def feature-applications-sets-key "applications-sets")
(def feature-edge-on-k8s "edge-on-k8s")
(def feature-internal "internal")

(def feature-flags [{:k     feature-deployment-set-key
                     :label "Page deployment set visible in left menu."}
                    {:k     feature-applications-sets-key
                     :label "Applications sets support"}
                    {:k     feature-edge-on-k8s
                     :label "k8s installation method available in Add Edge modal."}])

(def existing-feature-flags-keys (->> (conj feature-flags {:k feature-internal}) (map :k) (conj ) set))

(defn set-feature-flag
  [enabled-feature-flags k enable?]
  ((if enable? conj disj) enabled-feature-flags k))

(defn feature-flag-enabled?
  [enabled-feature-flags k]
  (contains? enabled-feature-flags k))

(defn keep-exsiting-feature-flags
  [enabled-feature-flags]
  (set/intersection existing-feature-flags-keys enabled-feature-flags))
