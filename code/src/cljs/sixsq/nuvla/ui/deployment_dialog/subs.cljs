(ns sixsq.nuvla.ui.deployment-dialog.subs
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-sub subscribe]]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.deployment-dialog.spec :as spec]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.icons :as icons]))

(reg-sub
  ::deploy-modal-visible?
  :-> ::spec/deploy-modal-visible?)

(reg-sub
  ::deployment-state
  :<- [::deployment]
  :-> :state)

(reg-sub
  ::execution-mode
  :<- [::deployment]
  :-> :execution-mode)

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
                  (= module-subtype apps-utils/subtype-application)
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
    (if start? icons/i-rocket "redo")))

(reg-sub
  ::modal-operation
  :<- [::deployment-start?]
  (fn [start?]
    (if start? "start" "update")))

(reg-sub
  ::modal-action-button-text
  :<- [::i18n-subs/tr]
  :<- [::deployment-start?]
  :<- [::is-deploy-status? :ok]
  :<- [::execution-mode]
  :<- [::selected-infra-service]
  (fn [[tr start? deploy-status-ok? execution-mode {:keys [capabilities]}]]
    (let [pull? (or (= execution-mode "pull")
                    (contains? (set capabilities) "NUVLA_JOB_PULL"))]
      (tr [(cond
             (and start? pull?) :schedule-deploy
             (and (not start?) pull?) :schedule-update
             (and (not start?) deploy-status-ok?) :update
             :else :deploy)]))))

(reg-sub
  ::modal-action-button-color
  :<- [::is-deploy-status? :warning]
  (fn [launch-deploy-warning?]
    (if launch-deploy-warning? "yellow" "blue")))

(reg-sub
  ::step-completed?
  (fn []
    [(subscribe [::data-completed?])
     (subscribe [::env-variables-completed?])
     (subscribe [::price-completed?])
     (subscribe [::license-completed?])
     (subscribe [::registries-completed?])
     (subscribe [::credentials-completed?])])
  (fn [[data-completed? env-variables-completed? price-completed? license-completed?
        registries-completed? credentials-completed?]
       [_ step-id]]
    (case step-id
      :data data-completed?
      :infra-services credentials-completed?
      :env-variables env-variables-completed?
      :registries registries-completed?
      :license license-completed?
      :pricing price-completed?
      false)))

(reg-sub
  ::step-active?
  :<- [::active-step]
  (fn [active-step [_ step-id]]
    (= active-step step-id)))

(reg-sub
  ::deployment
  :-> ::spec/deployment)

(reg-sub
  ::loading-deployment?
  :<- [::deployment]
  :-> nil?)

(reg-sub
  ::module
  :<- [::deployment]
  :-> :module)

(reg-sub
  ::can-edit-module-data?
  :<- [::module]
  :<- [::session-subs/active-claim]
  (fn [[{{:keys [edit-acl edit-data owners]} :acl} active-claim]]
    (-> (concat edit-acl edit-data owners)
        set
        (contains? active-claim))))

(reg-sub
  ::module-content
  :<- [::module]
  :-> :content)

(reg-sub
  ::module-compatibility
  :<- [::module]
  :-> :compatibility)

(reg-sub
  ::module-name
  :<- [::module]
  :-> :name)

(reg-sub
  ::module-subtype
  :<- [::module]
  :-> :subtype)

(reg-sub
  ::module-id
  :<- [::module]
  :-> :id)

(reg-sub
  ::is-application?
  :<- [::module]
  (fn [{:keys [subtype]}]
    (= subtype apps-utils/subtype-application)))

(reg-sub
  ::price
  :<- [::module]
  :-> :price)

(reg-sub
  ::license
  :<- [::module]
  :-> :license)

(reg-sub
  ::files
  :<- [::module-content]
  :-> :files)

(reg-sub
  ::current-module-content-id
  :<- [::module-content]
  :-> :id)

(reg-sub
  ::module-info
  :-> ::spec/module-info)

(reg-sub
  ::module-versions
  :<- [::module-info]
  (fn [module-info]
    (-> module-info :versions apps-utils/map-versions-index)))

(reg-sub
  ::module-versions-options
  :<- [::module-versions]
  :<- [::i18n-subs/tr]
  (fn [[versions-indexed tr]]
    (apps-utils/versions-options versions-indexed tr)))

(reg-sub
  ::latest-version
  :<- [::module-versions]
  (fn [module-versions]
    (some-> module-versions first second :href)))

(reg-sub
  ::latest-published-version
  :<- [::module-versions]
  (fn [module-versions]
    (-> module-versions apps-utils/latest-published-version)))

(reg-sub
  ::is-latest-version?
  :<- [::latest-version]
  :<- [::current-module-content-id]
  (fn [[latest-version current-version]]
    (if (and latest-version (= latest-version current-version))
      true
      false)))

(reg-sub
  ::is-latest-published-version?
  :<- [::latest-published-version]
  :<- [::current-module-content-id]
  (fn [[latest-published-version current-version]]
    (if (and latest-published-version (= latest-published-version current-version))
      true
      false)))

(reg-sub
  ::is-module-published?
  :<- [::module]
  (fn [module]
    (-> module :published true?)))

(reg-sub
  ::selected-version
  :-> ::spec/selected-version)

(reg-sub
  ::original-module
  :-> ::spec/original-module)


(reg-sub
  ::credentials-loading?
  :-> ::spec/credentials-loading?)

(reg-sub
  ::app-infra-compatibility-msg
  :<- [::module-subtype]
  :<- [::module-compatibility]
  :<- [::i18n-subs/tr]
  (fn [[module-subtype module-compatibility tr]
       [_ {:keys [swarm-enabled swarm-manager] infra-subtype :subtype
           :as   _infra-service}]]
    (cond
      (= [module-subtype module-compatibility infra-subtype swarm-enabled]
         ["application" "swarm" "swarm" false])
      (tr [:swarm-app-cant-be-deployed-compose-node])

      (= [module-subtype module-compatibility infra-subtype swarm-enabled swarm-manager]
         ["application" "swarm" "swarm" true false])
      (tr [:swarm-app-cant-be-deployed-worker-node])

      (= [module-subtype module-compatibility infra-subtype swarm-enabled]
         ["application" "docker-compose" "swarm" true])
      (tr [:compose-app-deployed-swarm-node]))))

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
  :-> ::spec/infra-registries-loading?)

(reg-sub
  ::infra-registries-creds
  :-> ::spec/infra-registries-creds)

(reg-sub
  ::infra-registries-creds-by-parent-options
  :<- [::infra-registries-creds]
  (fn [infra-registries-creds [_ parent-id]]
    (->> (get infra-registries-creds parent-id [])
         (map (fn [{:keys [id name]}]
                {:key id, :text (or name id), :value id})))))

(reg-sub
  ::active-step
  :-> ::spec/active-step)

(reg-sub
  ::data-step-active?
  :-> ::spec/data-step-active?)

(reg-sub
  ::step-states
  :-> ::spec/step-states)

(reg-sub
  ::data-clouds
  :-> ::spec/data-clouds)

(reg-sub
  ::selected-cloud
  :-> ::spec/selected-cloud)


(reg-sub
  ::cloud-infra-services
  :-> ::spec/cloud-infra-services)

(reg-sub
  ::credentials-completed?
  :<- [::selected-credential]
  :<- [::deployment]
  (fn [[selected-credential deployment]]
    (boolean (and selected-credential (:parent deployment)))))

(reg-sub
  ::infra-services-completed?
  :<- [::selected-infra-service]
  :-> boolean)

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
  ::deploy-status
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
  ::is-deploy-status?
  :<- [::deploy-status]
  (fn [deploy-status [_ v]]
    (= deploy-status v)))

(reg-sub
  ::license-completed?
  :-> ::spec/license-accepted?)

(reg-sub
  ::price-completed?
  :-> ::spec/price-accepted?)

(reg-sub
  ::registries-creds
  :-> ::spec/registries-creds)

(reg-sub
  ::env-variables
  :<- [::module-content]
  :-> :environmental-variables)

(reg-sub
  ::error-message
  :-> ::spec/error-message)

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
  :-> boolean)

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
  (fn [[deployment data-completed? data-step-active? credentials-completed? env-variables-completed?
        registries-completed? license license-completed? price price-completed? version-completed?]]
    (or (not deployment)
        (and (not data-completed?) data-step-active?)
        (not credentials-completed?)
        (not env-variables-completed?)
        (not registries-completed?)
        (and price (not price-completed?))
        (and license (not license-completed?))
        (not registries-completed?)
        (not version-completed?))))

(reg-sub
  ::check-dct
  :-> ::spec/check-dct)

(reg-sub
  ::new-price
  :<- [::deployment-start?]
  :<- [::module-info]
  :<- [::original-module]
  (fn [[deployment-start? {new-price :price} {current-price :price}]]
    (when (and (not deployment-start?)
               (not= (:cent-amount-daily current-price) (:cent-amount-daily new-price)))
      new-price)))

(reg-sub
  ::submit-loading?
  :-> ::spec/submit-loading?)
