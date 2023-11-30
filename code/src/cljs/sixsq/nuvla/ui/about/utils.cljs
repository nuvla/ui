(ns sixsq.nuvla.ui.about.utils
  (:require [clojure.set :as set]))

(def feature-edge-on-k8s "edge-on-k8s")
(def feature-internal-ui-demo-page "internal")

(def feature-flags [{:k     feature-edge-on-k8s
                     :label "k8s installation method available in Add Edge modal."}
                    {:k     feature-internal-ui-demo-page
                     :label "show internal ui demo/docu page."}])

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
