(ns sixsq.nuvla.ui.deployment-dialog.subs
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.deployment-dialog.spec :as spec]))


(reg-sub
  ::deploy-modal-visible?
  ::spec/deploy-modal-visible?)


(reg-sub
  ::loading-deployment?
  ::spec/loading-deployment?)


(reg-sub
  ::deployment
  ::spec/deployment)


(reg-sub
  ::loading-credentials?
  ::spec/loading-credentials?)


(reg-sub
  ::selected-credential
  (fn [db]
    (::spec/selected-credential db)))


(reg-sub
  ::credentials
  (fn [db]
    (::spec/credentials db)))


(reg-sub
  ::active-step
  (fn [db]
    (::spec/active-step db)))


(reg-sub
  ::data-step-active?
  (fn [db]
    (::spec/data-step-active? db)))


(reg-sub
  ::step-states
  (fn [db]
    (::spec/step-states db)))


(reg-sub
  ::data-clouds
  (fn [db]
    (::spec/data-infra-services db)))


(reg-sub
  ::selected-cloud
  (fn [db]
    (::spec/selected-infra-service db)))


(reg-sub
  ::connectors
  (fn [db]
    (::spec/infra-services db)))


;;
;; dynamic subscriptions to manage flow of derived data
;;


(reg-sub
  ::credentials-completed?
  :<- [::selected-credential]
  (fn [selected-credential _]
    (boolean selected-credential)))


(reg-sub
  ::env-variables
  :<- [::deployment]
  (fn [deployment _]
    (->> deployment :module :content :environmental-variables (sort-by :name))))


(reg-sub
  ::env-variables-completed?
  :<- [::env-variables]
  (fn [env-variables _]
    (->> env-variables
         (filter #(:required %))
         (every? #(not (str/blank? (:value %)))))))


(reg-sub
  ::data-completed?
  :<- [::selected-cloud]
  (fn [selected-cloud _]
    (boolean selected-cloud)))


