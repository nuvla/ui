(ns sixsq.nuvla.ui.docs.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::documents any?)

(def defaults {::documents nil})
