(ns sixsq.nuvla.ui.common-components.job.subs
  (:require [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.common-components.job.spec :as spec]))

(reg-sub
  ::jobs
  :-> ::spec/jobs)
