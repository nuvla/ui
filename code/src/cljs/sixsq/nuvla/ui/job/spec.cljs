(ns sixsq.nuvla.ui.job.spec
  (:require
    [clojure.spec.alpha :as s]))

(s/def ::target-resource string?)

(s/def ::jobs any?)

(s/def ::jobs-per-page pos-int?)

(s/def ::job-page nat-int?)

(def defaults {::target-resource nil
               ::jobs            nil
               ::jobs-per-page   10
               ::job-page        1})
