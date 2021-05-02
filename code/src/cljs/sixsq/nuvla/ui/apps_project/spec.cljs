(ns sixsq.nuvla.ui.apps-project.spec
  (:require [clojure.spec.alpha :as s]))

; create an initial entry for new components
(def defaults {})

(s/def ::module-project nil?)
