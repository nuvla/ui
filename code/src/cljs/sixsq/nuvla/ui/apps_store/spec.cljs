(ns sixsq.nuvla.ui.apps-store.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::modules any?)


(s/def ::full-text-search (s/nilable string?))


(s/def ::page int?)


(s/def ::elements-per-page int?)


(s/def ::active-tab-index number?)


;FIXME: needed for apps?
(s/def ::state-selector #{"all"
                          "started"
                          "starting"
                          "created"
                          "stopped"
                          "error"
                          "queued"})


(s/def ::db (s/keys :req [::modules
                          ::full-text-search
                          ::page
                          ::elements-per-page
                          ::active-tab-index
                          ::state-selector]))


(def defaults {::modules           nil
               ::full-text-search  nil
               ::page              1
               ::elements-per-page 8
               ::active-tab-index  0
               ::state-selector    "all"})
