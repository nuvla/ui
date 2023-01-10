(ns sixsq.nuvla.ui.cimi-detail.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::loading? boolean?)

(s/def ::resource-id (s/nilable string?))

(s/def ::resource any?)

(def defaults {::loading?    true
               ::resource-id nil
               ::resource    nil})
