(ns sixsq.nuvla.ui.common-components.intercom.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::events any?)

(def defaults {::events {}})
