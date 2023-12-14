(ns sixsq.nuvla.ui.apps-applications-sets.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.apps-applications-sets.spec :as spec]
    [sixsq.nuvla.ui.apps.subs :as apps-subs]
    [sixsq.nuvla.ui.plugins.module :as module-plugin]))

(reg-sub ::db identity)

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

(reg-sub
  ::has-outdated-apps?
  :<- [::db]
  :<- [::apps-subs/module]
  :<- [::apps-selected 0]
  (fn [[db apps-set _apps] [_]]
    (let [apps-in-apps-set (get-in apps-set [:content :applications-sets 0 :applications])
          db-path [::spec/apps-sets 0]]
      (some (fn [{:keys [id version]}]
              (when-let [latest-published-version-no (module-plugin/latest-published-version-id db db-path id)]
                (< version latest-published-version-no)))
            apps-in-apps-set))))
