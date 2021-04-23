(ns sixsq.nuvla.ui.dashboard.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::loading? boolean?)

(s/def ::db (s/keys :req [::loading?]))

(def defaults {::loading?               false})
