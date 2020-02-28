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
  ::credentials-loading?
  ::spec/credentials-loading?)


(reg-sub
  ::credentials
  (fn [db]
    (sort-by (juxt :name :id) (::spec/credentials db))))


(reg-sub
  ::credentials-by-ids
  :<- [::credentials]
  (fn [credentials]
    (->> credentials
         (map (juxt :id identity))
         (into {}))))


(reg-sub
  ::credential
  :<- [::credentials-by-ids]
  (fn [credentials-by-ids [_ id]]
    (get credentials-by-ids id)))


(reg-sub
  ::selected-credential-id
  (fn [db]
    (::spec/selected-credential-id db)))


(reg-sub
  ::selected-credential
  :<- [::credentials-by-ids]
  :<- [::selected-credential-id]
  (fn [[credentials-by-ids selected-credential]]
    (get credentials-by-ids selected-credential)))


(reg-sub
  ::infra-services-loading?
  (fn [db]
    (::spec/infra-services-loading? db)))


(reg-sub
  ::selected-infra-service
  (fn [db]
    (::spec/selected-infra-service db)))


(reg-sub
  ::infra-services
  (fn [db]
    (::spec/infra-services db)))


(reg-sub
  ::infra-registries
  (fn [db]
    (::spec/infra-registries db)))


(reg-sub
  ::infra-registries-by-ids
  :<- [::infra-registries]
  (fn [infra-registries]
    (->> infra-registries
         (map (juxt :id identity))
         (into {}))))


(reg-sub
  ::infra-registry
  :<- [::infra-registries-by-ids]
  (fn [infra-registries-by-ids [_ id]]
    (get infra-registries-by-ids id)))


(reg-sub
  ::infra-registries-loading?
  (fn [db]
    (::spec/infra-registries-loading? db)))


(reg-sub
  ::infra-registries-creds
  (fn [db]
    (::spec/infra-registries-creds db)))


(reg-sub
  ::infra-registries-creds-by-parent-options
  :<- [::infra-registries-creds]
  (fn [infra-registries-creds [_ parent-id]]
    (->> (get infra-registries-creds parent-id [])
         (map (fn [{:keys [id name]}]
                {:key id, :text (or name id), :value id})))))


(reg-sub
  ::infra-registries-creds-loading?
  (fn [db]
    (::spec/infra-registries-creds-loading? db)))


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
  ::cloud-infra-services
  (fn [db]
    (::spec/cloud-infra-services db)))


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
  ::infra-services-completed?
  :<- [::selected-infra-service]
  (fn [selected-infra-service]
    (boolean selected-infra-service)))


(reg-sub
  ::registries-completed?
  :<- [::private-registries]
  :<- [::registries-creds]
  (fn [[private-registries registries-creds]]
    (or (nil? private-registries)
        (and
          (not (empty? private-registries))
          (= (count private-registries) (->> registries-creds vals (remove nil?) count))))))


(reg-sub
  ::registries-creds
  (fn [db]
    (::spec/registries-creds db)))


(reg-sub
  ::env-variables
  :<- [::module-content]
  (fn [module-content]
    (-> module-content :environmental-variables)))


(reg-sub
  ::private-registries
  :<- [::module-content]
  (fn [module-content]
    (-> module-content :private-registries distinct seq)))


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
  :<- [::registries-completed?]
  (fn [[deployment data-completed? data-step-active?
        credentials-completed? env-variables-completed?
        registries-completed?]]
    (or (not deployment)
        (and (not data-completed?) data-step-active?)
        (not credentials-completed?)
        (not env-variables-completed?)
        (not registries-completed?))))
