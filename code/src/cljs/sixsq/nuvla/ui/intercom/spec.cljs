(ns sixsq.nuvla.ui.intercom.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::events any?)


(s/def ::db (s/keys :req [::events]))


(def defaults {::events {}})
