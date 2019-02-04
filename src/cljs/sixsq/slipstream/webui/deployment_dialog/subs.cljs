(ns sixsq.slipstream.webui.deployment-dialog.subs
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.slipstream.webui.deployment-dialog.spec :as spec]
    [sixsq.slipstream.webui.deployment-dialog.utils :as utils]))


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
    (::spec/data-clouds db)))


(reg-sub
  ::selected-cloud
  (fn [db]
    (::spec/selected-cloud db)))


(reg-sub
  ::connectors
  (fn [db]
    (::spec/connectors db)))


;;
;; dynamic subscriptions to manage flow of derived data
;;

(reg-sub
  ::size
  :<- [::deployment]
  (fn [deployment _]
    (let [{:keys [cpu ram disk]} (-> deployment :module :content)]
      (cond-> {}
              cpu (assoc :cpu cpu)
              ram (assoc :ram ram)
              disk (assoc :disk disk)))))


(reg-sub
  ::size-completed?
  :<- [::size]
  (fn [{:keys [cpu ram disk] :as size} _]
    (boolean (and cpu ram disk
                  (every? pos? (vals size))))))


(reg-sub
  ::credentials-completed?
  :<- [::selected-credential]
  (fn [selected-credential _]
    (boolean selected-credential)))


(reg-sub
  ::filtered-input-parameters
  (fn [_ _]
    [(subscribe [::deployment])
     (subscribe [::selected-credential])])
  (fn [[deployment selected-credential] _]
    (let [all-params (-> deployment :module :content :inputParameters)]
      (utils/remove-input-params all-params (utils/params-to-remove-fn selected-credential)))))


(reg-sub
  ::parameters-completed?
  :<- [::filtered-input-parameters]
  (fn [filtered-params _]
    (every? #(not (str/blank? (:value %))) filtered-params)))


(reg-sub
  ::data-completed?
  :<- [::selected-cloud]
  (fn [selected-cloud _]
    (boolean selected-cloud)))


