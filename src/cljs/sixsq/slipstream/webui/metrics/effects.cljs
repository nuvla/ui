(ns sixsq.slipstream.webui.metrics.effects
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [re-frame.core :refer [dispatch reg-fx]]
    [sixsq.slipstream.webui.metrics.utils :as u]
    [taoensso.timbre :as log]))


(reg-fx
  ::fetch-job-info
  (fn [[client callback]]
    (go
      (let [old (:count (<! (u/job-search client u/old-jobs)))
            stale (:count (<! (u/job-search client u/stale-jobs)))
            blocked (:count (<! (u/job-search client u/blocked-jobs)))
            states (-> (<! (u/job-search client u/job-states))
                       :aggregations
                       :terms:state
                       :buckets
                       u/add-total)]
        (callback {:old old, :stale stale, :blocked blocked, :states states})))))
