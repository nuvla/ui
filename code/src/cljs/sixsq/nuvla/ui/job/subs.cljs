(ns sixsq.nuvla.ui.job.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.job.spec :as spec]))


(reg-sub
  ::jobs
  (fn [db]
    (::spec/jobs db)))


(reg-sub
  ::jobs-per-page
  (fn [db]
    (::spec/jobs-per-page db)))


(reg-sub
  ::job-page
  (fn [db]
    (::spec/job-page db)))

