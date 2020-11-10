(ns sixsq.nuvla.ui.filter-comp.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]))

(s/def ::db (s/keys :req [::resource-metadata]))

(def defaults {::resource-metadata           {}})
