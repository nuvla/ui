(ns sixsq.nuvla.ui.job.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.job.spec :as spec]))

(reg-sub
  ::jobs
  :-> ::spec/jobs)
