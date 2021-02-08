(ns sixsq.nuvla.ui.deployment-dialog.subs
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [reg-sub subscribe]]
    [sixsq.nuvla.ui.credentials.subs :as creds-subs]
    [sixsq.nuvla.ui.deployment-dialog.spec :as spec]
    [sixsq.nuvla.ui.deployment-dialog.utils :as utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]))


(reg-sub
  ::deploy-modal-visible?
  (fn [db]
    (::spec/deploy-modal-visible? db)))


(reg-sub
  ::deployment-state
  :<- [::deployment]
  (fn [deployment]
    (:state deployment)))


(reg-sub
  ::deployment-start?
  :<- [::deployment-state]
  (fn [state]
    (boolean (#{"CREATED" "STOPPED"} state))))


(reg-sub
  ::visible-steps
  :<- [::data-step-active?]
  :<- [::registries-creds]
  :<- [::env-variables]
  :<- [::module-subtype]
  :<- [::files]
  :<- [::license]
  :<- [::price]
  :<- [::deployment-start?]
  (fn [[data-step-active? registries-creds env-variables module-subtype files license price start?]]
    (->> [(when data-step-active? :data)
          :infra-services
          :module-version
          (when (seq registries-creds) :registries)
          (when (seq env-variables) :env-variables)
          (when (and
                  start?
                  (= module-subtype "application")
                  (seq files)) :files)
          (when license :license)
          (when price :pricing)
          :summary]
         (remove nil?)
         set)))


(reg-sub
  ::is-step-visible?
  :<- [::visible-steps]
  (fn [visible-steps-set [_ step]]
    (boolean (visible-steps-set step))))


(reg-sub
  ::modal-header-text
  :<- [::loading-deployment?]
  :<- [::module-name]
  (fn [[loading? module-name]]
    (if loading?
      "\u2026"
      (str "\u00a0" module-name))))


(reg-sub
  ::modal-action-button-icon
  :<- [::deployment-start?]
  (fn [start?]
    (if start? "rocket" "redo")))


(reg-sub
  ::modal-operation
  :<- [::deployment-start?]
  (fn [start?]
    (if start? "start" "update")))


(reg-sub
  ::execution-mode
  :<- [::deployment]
  :<- [::selected-infra-service]
  :<- [::selected-credential-id]
  (fn [[deployment infra-service cred-id]]
    (or (:execution-mode deployment)
        (let [cred-unknown? @(subscribe [::creds-subs/credential-check-status-unknown? cred-id])
              cred-loading? @(subscribe [::creds-subs/credential-check-loading? cred-id])]
          (if (utils/infra-support-pull? infra-service)
            (if (and cred-unknown? (not cred-loading?))
              "pull"
              "mixed")
            "push")))))


(reg-sub
  ::modal-action-button-text
  :<- [::i18n-subs/tr]
  :<- [::deployment-start?]
  :<- [::is-launch-status? :ok]
  :<- [::execution-mode]
  (fn [[tr start? launch-status-ok? execution-mode]]
    (let [execution-mode-pull? (= execution-mode "pull")]
      (tr [(cond
             (and start? execution-mode-pull?) :schedule-launch
             (and (not start?) execution-mode-pull?) :schedule-update
             (and start? launch-status-ok?) :launch
             (and start? (not launch-status-ok?)) :launch-force
             (and (not start?) launch-status-ok?) :update
             :else :update-force)]))))


(reg-sub
  ::modal-action-button-color
  :<- [::is-launch-status? :warning]
  (fn [launch-status-warning?]
    (if launch-status-warning? "yellow" "blue")))


(reg-sub
  ::step-completed?
  (fn [_ _]
    [(subscribe [::selected-credential-id])
     (subscribe [::data-completed?])
     (subscribe [::env-variables-completed?])
     (subscribe [::price-completed?])
     (subscribe [::license-completed?])
     (subscribe [::registries-completed?])
     (subscribe [::credentials-completed?])
     (subscribe [::launch-status-registries :registries])])
  (fn [[cred-id data-completed? env-variables-completed? price-completed? license-completed?
        registries-completed? credentials-completed? registries-status]
       [_ step-id]]
    (let [cred-loading? @(subscribe [::creds-subs/credential-check-loading? cred-id])
          cred-valid?   @(subscribe [::creds-subs/credential-check-status-valid? cred-id])]
      (case step-id
        :data data-completed?
        :infra-services (and credentials-completed?
                             (not cred-loading?)
                             cred-valid?)
        :env-variables env-variables-completed?
        :registries (and registries-completed? (= :ok registries-status))
        :license license-completed?
        :pricing price-completed?
        false))))


(reg-sub
  ::step-active?
  :<- [::active-step]
  (fn [active-step [_ step-id]]
    (= active-step step-id)))


(reg-sub
  ::deployment
  (fn [db]
    (::spec/deployment db)))


(reg-sub
  ::loading-deployment?
  :<- [::deployment]
  (fn [deployment]
    (nil? deployment)))


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
  ::module-subtype
  :<- [::module]
  (fn [module]
    (:subtype module)))


(reg-sub
  ::module-id
  :<- [::module]
  (fn [module]
    (:id module)))


(reg-sub
  ::is-application?
  :<- [::module]
  (fn [module]
    (= (:subtype module) "application")))


(reg-sub
  ::price
  :<- [::module]
  (fn [module]
    (:price module)))


(reg-sub
  ::license
  :<- [::module]
  (fn [module]
    (:license module)))


(reg-sub
  ::coupon
  :<- [::deployment]
  (fn [deployment]
    (:coupon deployment)))


(reg-sub
  ::files
  :<- [::module-content]
  (fn [module-content]
    (:files module-content)))


(reg-sub
  ::current-module-content-id
  :<- [::module-content]
  (fn [module-content]
    (:id module-content)))


(reg-sub
  ::module-info
  (fn [db]
    (::spec/module-info db)))


(reg-sub
  ::module-versions
  :<- [::module-info]
  (fn [module-info]
    (->> module-info
         :versions
         (map-indexed vector)
         reverse)))


(reg-sub
  ::latest-version
  :<- [::module-versions]
  (fn [module-versions]
    (some-> module-versions first second :href)))


(reg-sub
  ::is-latest-version?
  :<- [::latest-version]
  :<- [::current-module-content-id]
  (fn [[latest-version current-version]]
    (and latest-version (= latest-version current-version))))


(reg-sub
  ::selected-version
  (fn [db]
    (::spec/selected-version db)))


(reg-sub
  ::original-module
  (fn [db]
    (::spec/original-module db)))


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
  ::visible-infra-services
  :<- [::infra-services]
  :<- [::deployment-start?]
  :<- [::selected-infra-service]
  (fn [[infra-services deployment-start? selected-infra-service]]
    (if deployment-start?
      infra-services
      (when selected-infra-service [selected-infra-service]))))


(reg-sub
  ::launch-status-registries
  (fn [db [_ step-id]]
    (get-in db [::spec/step-states step-id :status])))

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
  ::deployment-reg-creds-count
  :<- [::deployment]
  (fn [deployment]
    (->> deployment :registries-credentials (remove str/blank?) count)))

(reg-sub
  ::module-private-registries-count
  :<- [::module-content]
  (fn [module-content]
    (-> module-content :private-registries count)))


(reg-sub
  ::registries-completed?
  :<- [::module-private-registries-count]
  :<- [::deployment-reg-creds-count]
  (fn [[module-private-registries-count deployment-reg-creds-count]]
    (>= deployment-reg-creds-count module-private-registries-count)))


(reg-sub
  ::version-completed?
  :<- [::current-module-content-id]
  :<- [::selected-version]
  :<- [::is-latest-version?]
  (fn [[current-version selected-version is-latest?]]
    (or (and
          (some? current-version)
          (some? selected-version)
          (= current-version selected-version))
        (and is-latest? (nil? selected-version)))))

(reg-sub
  ::launch-status
  (fn [db]
    (let [steps-status (->> (::spec/step-states db)
                            vals
                            (map :status)
                            (remove nil?))]
      (cond
        (some #{:warning} steps-status) :warning
        (some #{:loading} steps-status) :loading
        :else :ok))))


(reg-sub
  ::is-launch-status?
  :<- [::launch-status]
  (fn [launch-status [_ v]]
    (= launch-status v)))

(reg-sub
  ::license-completed?
  (fn [db]
    (::spec/license-accepted? db)))


(reg-sub
  ::price-completed?
  (fn [db]
    (::spec/price-accepted? db)))


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
  ::error-message
  (fn [db]
    (::spec/error-message db)))


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
  ::modal-action-button-disabled?
  :<- [::deployment]
  :<- [::data-completed?]
  :<- [::data-step-active?]
  :<- [::credentials-completed?]
  :<- [::env-variables-completed?]
  :<- [::registries-completed?]
  :<- [::license]
  :<- [::license-completed?]
  :<- [::price]
  :<- [::price-completed?]
  :<- [::version-completed?]
  :<- [::selected-credential-id]
  (fn [[deployment data-completed? data-step-active? credentials-completed? env-variables-completed?
        registries-completed? license license-completed? price price-completed? version-completed?
        selected-credential-id]]
    (let [cred-invalid? @(subscribe [::creds-subs/credential-check-status-invalid? selected-credential-id])]
      (or (not deployment)
        (and (not data-completed?) data-step-active?)
        (not credentials-completed?)
        (not env-variables-completed?)
        (not registries-completed?)
        (and price (not price-completed?))
        (and license (not license-completed?))
        (not registries-completed?)
        (not version-completed?)
        cred-invalid?))))


(reg-sub
  ::check-dct
  (fn [db]
    (::spec/check-dct db)))


(reg-sub
  ::new-price
  :<- [::deployment-start?]
  :<- [::module-info]
  :<- [::original-module]
  (fn [[deployment-start? {new-price :price} {current-price :price}]]
    (when (and (not deployment-start?)
               (not= (:cent-amount-daily current-price) (:cent-amount-daily new-price)))
      new-price)))
