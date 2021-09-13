(ns sixsq.nuvla.ui.apps.subs
  (:require
    [clojure.set :as set]
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.apps.spec :as spec]
    [sixsq.nuvla.ui.apps.utils :as utils]
    [sixsq.nuvla.ui.apps.utils-detail :as utils-detail]
    [sixsq.nuvla.ui.utils.general :as general-utils]))


(reg-sub
  ::loading?
  (fn [db]
    (::spec/loading? db)))


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
  ::module-subtype
  :<- [::module-common]
  (fn [module-common]
    (::spec/subtype module-common)))


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

; Is the form valid?

(reg-sub
  ::form-valid?
  ::spec/form-valid?)


; Should the form be validated?

(reg-sub
  ::validate-form?
  ::spec/validate-form?)


(reg-sub
  ::active-input
  ::spec/active-input)


(reg-sub
  ::completed?
  ::spec/completed?)


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
  ::add-data
  ::spec/add-data)


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
  :<- [::is-latest-version?]
  :<- [::module-content-id]
  (fn [[module versions is-latest? current]]
    (let [id (:id module)]
      (if is-latest?
        id
        (str id "_" (some (fn [[i {:keys [href]}]] (when (= current href) i)) versions))))))


(reg-sub
  ::version
  :<- [::module]
  :<- [::versions]
  :<- [::is-latest-version?]
  :<- [::module-content-id]
  (fn [[module versions is-latest? current]]
    (let [id (:id module)]
      (if is-latest?
        id
        (str id "_" (some (fn [[i {:keys [href]}]] (when (= current href) i)) versions))))))


(reg-sub
  ::copy-module
  ::spec/copy-module)


(reg-sub
  ::paste-modal-visible?
  ::spec/paste-modal-visible?)

(reg-sub
  ::active-tab-index
  ::spec/active-tab-index)


(reg-sub
  ::module-not-found?
  ::spec/module-not-found?)
