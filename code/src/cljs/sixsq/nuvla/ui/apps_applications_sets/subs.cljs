(ns sixsq.nuvla.ui.apps-applications-sets.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.apps-applications-sets.spec :as spec]))


(reg-sub
  ::configuration-error?
  (fn [db]
    #_:clj-kondo/ignore
    (seq (::spec/configuration-validation-errors db))))

(reg-sub
  ::apps-error?
  (fn [db]
    #_:clj-kondo/ignore
    (seq (::spec/apps-validation-errors db))))

(reg-sub
  ::apps-sets
  :-> ::spec/apps-sets)

