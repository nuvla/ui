(ns sixsq.nuvla.ui.dashboard.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.dashboard.spec :as spec]
    [sixsq.nuvla.ui.dashboard.utils :as utils]))


(reg-sub
  ::loading?
  (fn [db]
    (::spec/loading? db)))
