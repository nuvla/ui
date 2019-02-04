(ns sixsq.slipstream.webui.metrics.subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.slipstream.webui.metrics.spec :as metrics-spec]
    [sixsq.slipstream.webui.metrics.utils :as metrics-utils]))


(reg-sub
  ::loading?
  ::metrics-spec/loading?)


(reg-sub
  ::raw-metrics
  ::metrics-spec/raw-metrics)


(reg-sub
  ::jvm-threads
  (fn [query-v _]
    (subscribe [::raw-metrics]))
  (fn [raw-metrics query-v _]
    (metrics-utils/extract-thread-metrics raw-metrics)))


(reg-sub
  ::jvm-memory
  (fn [query-v _]
    (subscribe [::raw-metrics]))
  (fn [raw-metrics query-v _]
    (metrics-utils/extract-memory-metrics raw-metrics)))


(reg-sub
  ::ring-request-rates
  (fn [query-v _]
    (subscribe [::raw-metrics]))
  (fn [raw-metrics query-v _]
    (metrics-utils/extract-ring-requests-rates raw-metrics)))


(reg-sub
  ::ring-response-rates
  (fn [query-v _]
    (subscribe [::raw-metrics]))
  (fn [raw-metrics query-v _]
    (metrics-utils/extract-ring-responses-rates raw-metrics)))


(reg-sub
  ::loading-job-info?
  ::metrics-spec/loading-job-info?)


(reg-sub
  ::job-info
  ::metrics-spec/job-info)



