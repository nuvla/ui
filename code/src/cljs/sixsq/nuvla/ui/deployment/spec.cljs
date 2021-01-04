(ns sixsq.nuvla.ui.deployment.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.utils.time :as time]))

(s/def ::loading? boolean?)

(s/def ::deployment any?)

(s/def ::module-versions any?)

(s/def ::deployment-parameters any?)

(s/def ::events any?)

(s/def ::jobs any?)

(s/def ::jobs-per-page pos-int?)

(s/def ::job-page nat-int?)

(s/def ::node-parameters any?)

(s/def ::deployment-log-id (s/nilable string?))

(s/def ::deployment-log-service (s/nilable string?))

(s/def ::deployment-log-since (s/nilable string?))

(s/def ::deployment-log-play? boolean?)

(s/def ::deployment-log any?)

(s/def ::upcoming-invoice any?)

(s/def ::active-tab-index number?)


(s/def ::db (s/keys :req [::loading?
                          ::deployment
                          ::deployment-parameters
                          ::events
                          ::jobs
                          ::node-parameters
                          ::jobs-per-page
                          ::job-page
                          ::deployment-log-id
                          ::deployment-log-service
                          ::deployment-log-since
                          ::deployment-log-play?
                          ::deployment-log
                          ::upcoming-invoice
                          ::active-tab-index]))


(defn default-since []
  (-> (time/now) (.seconds 0)))

(def defaults {::loading?                  true
               ::deployment                nil
               ::deployment-parameters     nil
               ::module-versions           nil
               ::events                    nil
               ::jobs                      nil
               ::jobs-per-page             10
               ::job-page                  1
               ::node-parameters           nil
               ::deployment-log-controller nil
               ::deployment-log-id         nil
               ::deployment-log-service    nil
               ::deployment-log-since      (default-since)
               ::deployment-log-play?      false
               ::deployment-log            nil
               ::upcoming-invoice          nil
               ::active-tab-index          0})
