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

(reg-sub
  ::apps-selected
  :<- [::apps-sets]
  (fn [apps-sets [_ id]]
    (vals (get-in apps-sets [id ::spec/apps-selected]))))

(reg-sub
  ::apps-set-subtype
  :<- [::apps-sets]
  (fn [apps-sets [_ id]]
    (get-in apps-sets [id ::spec/apps-set-subtype])))
