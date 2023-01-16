(ns sixsq.nuvla.ui.apps-applications-sets.subs
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.apps-applications-sets.spec :as spec]
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
    (->> (get-in apps-sets [id ::spec/apps-selected])
         vals
         (sort-by :name))))


(reg-sub
  ::subtypes
  (fn [[_ db-path]]
    (subscribe [::module-selector/selected db-path]))
  (fn [selected]
    (some (fn [{:keys [subtype]}]
            (let [docker-subtypes     #{"component" "application"}
                  kubernetes-subtypes #{"application_kubernetes"}]
              (cond
                (docker-subtypes subtype) docker-subtypes
                (kubernetes-subtypes subtype) kubernetes-subtypes
                :else nil))) selected)))

