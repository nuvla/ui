(ns sixsq.nuvla.ui.pages.docs.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::documents any?)

(def defaults {::documents nil})
