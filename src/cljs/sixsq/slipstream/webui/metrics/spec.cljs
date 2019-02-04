(ns sixsq.slipstream.webui.metrics.spec
  (:require-macros [sixsq.slipstream.webui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::loading? boolean?)

(s/def ::raw-metrics (s/nilable map?))

(s/def ::jvm-threads (s/nilable vector?))

(s/def ::jvm-memory (s/nilable vector?))

(s/def ::ring-request-rates (s/nilable vector?))

(s/def ::ring-response-rates (s/nilable vector?))

(s/def ::loading-job-info? boolean?)


(s/def ::key string?)
(s/def ::doc_count nat-int?)
(s/def ::job-stat (s/keys :req-un [::key ::doc_count]))

(s/def ::states (s/coll-of ::job-stat :type :vector))

(s/def ::old nat-int?)

(s/def ::stale nat-int?)

(s/def ::blocked nat-int?)

(s/def ::job-info (s/nilable (s/keys :req-un [::old ::stale ::blocked ::states])))


(s/def ::db (s/keys :req [::loading?
                          ::raw-metrics
                          ::jvm-threads
                          ::jvm-memory
                          ::ring-request-rates
                          ::ring-response-rates
                          ::loading-job-info?
                          ::job-info]))


(def defaults {::loading?            false
               ::raw-metrics         nil
               ::jvm-threads         nil
               ::jvm-memory          nil
               ::ring-request-rates  nil
               ::ring-response-rates nil
               ::loading-job-info?   false
               ::job-info            nil})
