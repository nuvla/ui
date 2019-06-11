(ns sixsq.nuvla.ui.dashboard-detail.spec
  (:require
    [clojure.spec.alpha :as s]))

(s/def ::loading? boolean?)

(s/def ::deployment any?)

(s/def ::deployment-parameters any?)

(s/def ::events any?)

(s/def ::jobs any?)

(s/def ::jobs-per-page pos-int?)

(s/def ::job-page nat-int?)

(s/def ::node-parameters any?)


(s/def ::db (s/keys :req [::loading?
                          ::deployment
                          ::deployment-parameters
                          ::events
                          ::jobs
                          ::node-parameters
                          ::jobs-per-page
                          ::job-page]))


(def defaults {::loading?              false
               ::deployment            nil
               ::deployment-parameters nil
               ::events                nil
               ::jobs                  nil
               ::jobs-per-page         10
               ::job-page              1
               ::node-parameters       nil})
