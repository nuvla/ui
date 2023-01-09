(ns sixsq.nuvla.ui.i18n.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::locale string?)
(s/def ::tr fn?)

(def defaults {::locale "en"})
