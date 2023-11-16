(ns sixsq.nuvla.ui.about.spec
  (:require [cljs.spec.alpha :as s]))

(s/def ::enabled-feature-flags set?)

(def defaults {::enabled-feature-flags #{}})
