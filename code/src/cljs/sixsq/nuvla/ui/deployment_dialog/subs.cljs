(ns sixsq.nuvla.ui.deployment-dialog.subs
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.deployment-dialog.spec :as spec]
    [taoensso.timbre :as log]))


(reg-sub
  ::deploy-modal-visible?
  ::spec/deploy-modal-visible?)


(reg-sub
  ::loading-deployment?
  ::spec/loading-deployment?)


(reg-sub
  ::ready?
  :<- [::loading-deployment?]
  :<- [::deployment]
  (fn [[loading-deployment? deployment]]
    (and (not loading-deployment?) deployment)))


(reg-sub
  ::deployment
  ::spec/deployment)


(reg-sub
  ::module
  :<- [::deployment]
  (fn [deployment]
    (:module deployment)))


(reg-sub
  ::module-content
  :<- [::module]
  (fn [module]
    (:content module)))


(reg-sub
  ::module-name
  :<- [::module]
  (fn [module]
    (:name module)))


(reg-sub
  ::is-application?
  :<- [::module]
  (fn [module]
    (= (:subtype module) "application")))


(reg-sub
  ::files
  :<- [::module-content]
  (fn [module-content]
    (:files module-content)))


(reg-sub
  ::credentials-loading?                                    ;;FIXME not used
  ::spec/credentials-loading?)


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
  :<- [::deployment]
  (fn [[selected-credential deployment]]
    (boolean (and selected-credential (:parent deployment)))))


(reg-sub
  ::env-variables
  :<- [::module-content]
  (fn [module-content]
    (->> module-content :environmental-variables)))


(reg-sub
  ::env-variables-completed?
  :<- [::env-variables]
  (fn [env-variables]
    (->> env-variables
         (filter #(:required %))
         (every? #(not (str/blank? (:value %)))))))


(reg-sub
  ::data-completed?
  :<- [::selected-cloud]
  (fn [selected-cloud]
    (boolean selected-cloud)))


(reg-sub
  ::launch-disabled?
  :<- [::deployment]
  :<- [::data-completed?]
  :<- [::data-step-active?]
  :<- [::credentials-completed?]
  :<- [::env-variables-completed?]
  (fn [[deployment data-completed? data-step-active?
        credentials-completed? env-variables-completed?]]
    (or (not deployment)
        (and (not data-completed?) data-step-active?)
        (not credentials-completed?)
        (not env-variables-completed?))))