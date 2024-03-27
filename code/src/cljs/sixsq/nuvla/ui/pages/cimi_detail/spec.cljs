(ns sixsq.nuvla.ui.pages.cimi-detail.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::loading? boolean?)

(s/def ::resource any?)

(def defaults {::loading?    true
               ::resource    nil})
