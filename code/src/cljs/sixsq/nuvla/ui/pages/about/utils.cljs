(ns sixsq.nuvla.ui.pages.about.utils
  (:require [clojure.set :as set]))

(def feature-edge-on-k8s "edge-on-k8s")
(def feature-etsi-mec "etsi-mec")

(def feature-flags [{:k     feature-edge-on-k8s
                     :label "k8s installation method available in Add Edge modal."}
                    {:k     feature-etsi-mec
                     :label "Enable ETSI MEC UI surfaces and subtype handling."}])

(def existing-feature-flags-keys (->> feature-flags (map :k) set))

(defn set-feature-flag
  [enabled-feature-flags k enable?]
  ((if enable? conj disj) enabled-feature-flags k))

(defn feature-flag-enabled?
  [enabled-feature-flags k]
  (contains? enabled-feature-flags k))

(defn keep-existing-feature-flags
  [enabled-feature-flags]
  (set/intersection existing-feature-flags-keys enabled-feature-flags))
