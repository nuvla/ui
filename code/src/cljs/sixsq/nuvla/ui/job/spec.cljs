(ns sixsq.nuvla.ui.job.spec
  (:require
    [clojure.spec.alpha :as s]))

(s/def ::target-resource string?)

(s/def ::jobs any?)

(s/def ::jobs-per-page pos-int?)

(s/def ::job-page nat-int?)


(s/def ::db (s/keys :req [::target-resource
                          ::jobs
                          ::jobs-per-page
                          ::job-page]))


(def defaults {::target-resource nil
               ::jobs            nil
               ::jobs-per-page   10
               ::job-page        1})
