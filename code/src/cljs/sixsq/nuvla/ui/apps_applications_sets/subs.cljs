(ns sixsq.nuvla.ui.apps-applications-sets.subs
  (:require
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.apps-applications-sets.spec :as spec]
    [sixsq.nuvla.ui.apps.utils :as apps-utils]
    [sixsq.nuvla.ui.plugins.module-selector :as module-selector]))


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

(def docker-subtypes #{apps-utils/subtype-component apps-utils/subtype-application})
(def k8s-subtypes #{apps-utils/subtype-application-k8s})
(defn get-subtypes
  [subtype]
  (condp contains? subtype
    k8s-subtypes k8s-subtypes
    docker-subtypes docker-subtypes
    nil))

(reg-sub
  ::apps-selected-subtypes
  (fn [[_ id]]
    (subscribe [::apps-selected id]))
  (fn [apps-selected]
    (some #(get-subtypes (:subtype %)) apps-selected)))


(reg-sub
  ::subtypes
  (fn [[_ db-path]]
    (subscribe [::module-selector/selected db-path]))
  (fn [selected]
    (some (fn [{:keys [subtype]}]
            (cond
              (docker-subtypes subtype) docker-subtypes
              (k8s-subtypes subtype) k8s-subtypes
              :else nil)) selected)))
