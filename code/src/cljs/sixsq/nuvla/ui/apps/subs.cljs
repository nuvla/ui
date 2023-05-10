(ns sixsq.nuvla.ui.apps.subs
  (:require [clojure.set :as set]
            [re-frame.core :refer [reg-sub subscribe]]
            [sixsq.nuvla.ui.apps.spec :as spec]
            [sixsq.nuvla.ui.apps.utils :as utils]
            [sixsq.nuvla.ui.apps.utils-detail :as utils-detail]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.plugins.nav-tab :as nav-tab]
            [sixsq.nuvla.ui.utils.general :as general-utils]))


(reg-sub
  ::module-common
  (fn [db]
    (::spec/module-common db)))


(reg-sub
  ::description
  :<- [::module-common]
  (fn [module-common]
    (::spec/description module-common)))

(reg-sub
  ::is-description-template?
  :<- [::module-subtype]
  :<- [::description]
  (fn [[module-subtype description]]
    (not (utils/descr-not-template? module-subtype description))))

(reg-sub
  ::module-subtype
  :<- [::module-common]
  (fn [module-common]
    (::spec/subtype module-common)))

(reg-sub
  ::is-project?
  :<- [::module-subtype]
  (fn [subtype]
    (utils/project? subtype)))

(reg-sub
  ::is-applications-sets?
  :<- [::module-subtype]
  (fn [subtype]
    (utils/applications-sets? subtype)))

(reg-sub
  ::is-app?
  :<- [::is-project?]
  :-> not)

(reg-sub
  ::can-edit?
  :<- [::module]
  :-> general-utils/can-edit?)

(reg-sub
  ::can-copy?
  :<- [::is-app?]
  :<- [::price]
  :<- [::can-edit?]
  (fn [[is-app? price can-edit?]]
    (or (and is-app? (nil? price))
        (and is-app? price can-edit?))))

(reg-sub
  ::paste-disabled?
  :<- [::copy-module]
  :-> nil?)

(reg-sub
  ::deploy-disabled?
  :<- [::is-new?]
  :<- [::main-subs/changes-protection?]
  (fn [[is-new? page-changed?]]
    (or is-new? page-changed?)))

(reg-sub
  ::can-operation
  :<- [::module]
  (fn [module [_ op]]
    (general-utils/can-operation? op module)))
(reg-sub
  ::is-published?
  :<- [::module]
  :<- [::module-id-version]
  (fn [[module module-id]]
    (let [versions (:versions module)
          version  (js/parseInt (utils/extract-version module-id))]
      (-> versions (nth version) :published true?))))

(reg-sub
  ::can-publish?
  :<- [::is-app?]
  :<- [::is-published?]
  :<- [::can-operation :publish]
  (fn [[is-app? is-published? publish-op]]
    (and is-app? publish-op (not is-published?))))

(reg-sub
  ::can-unpublish?
  :<- [::is-app?]
  :<- [::is-published?]
  :<- [::can-operation :unpublish]
  (fn [[is-app? is-published? unpublish-op]]
    (and is-app? unpublish-op is-published?)))

(reg-sub
  ::module-license
  :<- [::module-common]
  (fn [module-common]
    (::spec/license module-common)))


;; Validation

(reg-sub
  ::details-validation-error?
  (fn [db]
    #_:clj-kondo/ignore
    (not (empty? (::spec/details-validation-errors db)))))

(reg-sub
  ::is-description-valid?
  :<- [::module-subtype]
  :<- [::description]
  (fn [[sub-type description]]
    (utils/description-valid? sub-type description)))

; Is the form valid?

(reg-sub
  ::form-valid?
  ::spec/form-valid?)


; Should the form be validated?

(reg-sub
  ::validate-form?
  ::spec/validate-form?)


(reg-sub
  ::is-new?
  ::spec/is-new?)


(reg-sub
  ::module
  ::spec/module)


(reg-sub
  ::editable?
  :<- [::module]
  :<- [::is-new?]
  (fn [[module is-new?]]
    (general-utils/editable? module is-new?)))


(reg-sub
  ::env-variables
  (fn [db]
    (get-in db [::spec/module-common ::spec/env-variables])))


(reg-sub
  ::urls
  (fn [db]
    (get-in db [::spec/module-common ::spec/urls])))


(reg-sub
  ::output-parameters
  (fn [db]
    (get-in db [::spec/module-common ::spec/output-parameters])))


(reg-sub
  ::private-registries-options
  (fn [db]
    (let [registries-infra        (::spec/registries-infra db)
          private-registries-set  (->> (get-in db [::spec/module-common ::spec/registries])
                                       vals
                                       (map ::spec/registry-id)
                                       (remove nil?)
                                       set)
          registries-infra-set    (set (map :id registries-infra))
          not-existing-registries (set/difference private-registries-set registries-infra-set)]
      (map (fn [{:keys [id name]}]
             {:key id, :value id, :text (or name id)})
           (concat registries-infra
                   (map (fn [id] {:id id}) not-existing-registries))))))


(reg-sub
  ::registries-credentials
  (fn [db]
    (group-by :parent (::spec/registries-credentials db))))


(reg-sub
  ::registries-credentials-options
  :<- [::registries-credentials]
  (fn [registries-credentials [_ registry-id]]
    (let [creds (get registries-credentials registry-id [])]
      (map (fn [{:keys [id name]}] {:key id, :value id, :text (or name id)}) creds))))


(reg-sub
  ::registries
  (fn [db]
    (get-in db [::spec/module-common ::spec/registries])))


(reg-sub
  ::price
  (fn [db]
    (get-in db [::spec/module-common ::spec/price])))


(reg-sub
  ::data-types
  (fn [db]
    (get-in db [::spec/module-common ::spec/data-types])))


(reg-sub
  ::add-modal-visible?
  ::spec/add-modal-visible?)


(reg-sub
  ::save-modal-visible?
  ::spec/save-modal-visible?)


(reg-sub
  ::default-logo-url
  ::spec/default-logo-url)


(reg-sub
  ::logo-url-modal-visible?
  ::spec/logo-url-modal-visible?)

(reg-sub
  ::commit-message
  ::spec/commit-message)


(reg-sub
  ::validate-docker-compose
  (fn [db]
    (or (::spec/validate-docker-compose db)
        (let [docker-compose-valid (get-in db [::spec/module-immutable :valid])]
          (when (boolean? docker-compose-valid)
            {:valid?    docker-compose-valid
             :loading?  false
             :error-msg (get-in db [::spec/module-immutable :validation-message] "")})))))


(reg-sub
  ::module-content-updated?
  (fn [{:keys [::spec/module
               ::spec/module-immutable] :as db}]
    (not= (-> module
              (utils-detail/db->module nil db)
              :content
              (dissoc :commit :author :children))
          (-> module-immutable :content (dissoc :commit :author :children)))))


(reg-sub
  ::versions
  :<- [::module]
  (fn [{:keys [versions]}]
    (utils/map-versions-index versions)))


(reg-sub
  ::module-content-id
  :<- [::module]
  (fn [{{:keys [id]} :content}]
    id))


(reg-sub
  ::compare-module-left
  (fn [db]
    (::spec/compare-module-left db)))


(reg-sub
  ::compare-module-right
  (fn [db]
    (::spec/compare-module-right db)))


(reg-sub
  ::is-latest-version?
  :<- [::versions]
  :<- [::module-content-id]
  (fn [[versions id]]
    (or (nil? id)
        (= (some-> versions first second :href)
           id))))


(reg-sub
  ::latest-published-version
  :<- [::versions]
  (fn [versions]
    (-> versions utils/latest-published-version)))


(reg-sub
  ::latest-published-index
  :<- [::versions]
  (fn [versions]
    (-> versions utils/latest-published-index)))


(reg-sub
  ::is-latest-published-version?
  :<- [::latest-published-version]
  :<- [::module-content-id]
  (fn [[latest-published-version module-content-id]]
    (if (and latest-published-version (= latest-published-version module-content-id))
      true
      false)))


(reg-sub
  ::is-module-published?
  :<- [::module]
  (fn [module]
    (-> module :published true?)))


(reg-sub
  ::module-id-version
  :<- [::module]
  :<- [::versions]
  :<- [::module-content-id]
  (fn [[module versions current]]
    (let [id (:id module)]
      (str id "_" (some (fn [[i {:keys [href]}]] (when (= current href) i)) versions)))))


(reg-sub
  ::copy-module
  :-> ::spec/copy-module)


(reg-sub
  ::paste-modal-visible?
  :-> ::spec/paste-modal-visible?)


(reg-sub
  ::active-tab
  (fn [[_ db-path]]
    (subscribe [::nav-tab/active-tab (or db-path [::spec/tab])]))
  (fn [active-tab]
    active-tab))


(reg-sub
  ::module-not-found?
  :-> ::spec/module-not-found?)
