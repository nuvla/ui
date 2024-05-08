(ns sixsq.nuvla.ui.pages.deployments-detail.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.common-components.plugins.events :as events-plugin]
            [sixsq.nuvla.ui.utils.time :as time]))

(s/def ::deployment any?)
(s/def ::nuvlabox any?)
(s/def ::loading? boolean?)
(s/def ::module-versions any?)
(s/def ::deployment-parameters any?)
(s/def ::events any?)
(s/def ::node-parameters any?)
(s/def ::upcoming-invoice any?)
(s/def ::not-found? boolean?)
(s/def ::deployment-data any?)
(s/def ::timespan any?)

(def defaults
  {::not-found?                false
   ::deployment                nil
   ::nuvlabox                  nil
   ::deployment-parameters     nil
   ::loading?                  true
   ::module-versions           nil
   ::events                    (events-plugin/build-spec
                                 :default-items-per-page 15)
   ::node-parameters           nil
   ::deployment-log-controller nil
   ::deployment-data           nil
   ::timespan                         {:timespan-option "last 15 minutes"
                                       :from            (time/subtract-minutes (time/now) 15)
                                       :to              (time/now)}})
