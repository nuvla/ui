(ns sixsq.nuvla.ui.intercom.spec
  (:require
    [clojure.spec.alpha :as s]))

(s/def ::events any?)

(def defaults {::events {}})
