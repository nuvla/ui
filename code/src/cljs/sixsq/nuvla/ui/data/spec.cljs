(ns sixsq.nuvla.ui.data.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.utils.time :as time]))


(s/def ::data-records any?)

(s/def ::credentials (s/nilable (s/coll-of any? :kind vector?)))

(s/def ::content-type-filter (s/nilable string?))

(s/def ::application-select-visible? boolean?)

(s/def ::loading-applications? boolean?)

(s/def ::applications (s/nilable (s/coll-of any? :kind vector?)))

(s/def ::selected-application-id (s/nilable string?))

(s/def ::full-text-search (s/nilable string?))

(s/def ::page int?)
(s/def ::elements-per-page int?)

(s/def ::total any?)

(s/def ::counts any?)

(s/def ::sizes any?)

(s/def ::data-sets (s/map-of string? map?))

(s/def ::selected-data-set-ids (s/coll-of string? :kind set?))

(s/def ::modal-open? boolean?)

(s/def ::active-tab-index number?)

(s/def ::add-data-set-form any?)

(s/def ::db (s/keys :req [::active-tab-index
                          ::modal-open?
                          ::data-records
                          ::credentials
                          ::application-select-visible?
                          ::loading-applications?
                          ::applications
                          ::content-type-filter
                          ::full-text-search
                          ::total
                          ::counts
                          ::sizes
                          ::data-sets
                          ::selected-data-set-ids
                          ::page
                          ::elements-per-page
                          ::add-data-set-form]))

(def default-time-period [(time/days-before 30)
                          (time/now)])

(def defaults {::active-tab-index            0
               ::modal-open?                 false
               ::data-records                nil
               ::credentials                 nil
               ::application-select-visible? false
               ::loading-applications?       false
               ::applications                nil
               ::content-type-filter         nil
               ::full-text-search            nil
               ::total                       0
               ::counts                      nil
               ::sizes                       nil
               ::data-sets                   {}
               ::selected-data-set-ids       #{}
               ::page                        1
               ::elements-per-page           8
               ::add-data-set-form           {}})
