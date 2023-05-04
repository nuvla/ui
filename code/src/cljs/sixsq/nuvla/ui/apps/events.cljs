(ns sixsq.nuvla.ui.apps.events
  (:require [cljs.spec.alpha :as s]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [sixsq.nuvla.ui.apps-application.spec :as apps-application-spec]
            [sixsq.nuvla.ui.apps-application.utils :as apps-application-utils]
            [sixsq.nuvla.ui.apps-applications-sets.spec :as apps-applications-sets-spec]
            [sixsq.nuvla.ui.apps-applications-sets.utils :as apps-applications-sets-utils]
            [sixsq.nuvla.ui.apps-component.spec :as apps-component-spec]
            [sixsq.nuvla.ui.apps-component.utils :as apps-component-utils]
            [sixsq.nuvla.ui.apps-project.spec :as apps-project-spec]
            [sixsq.nuvla.ui.apps-project.utils :as apps-project-utils]
            [sixsq.nuvla.ui.apps-store.spec :as apps-store-spec]
            [sixsq.nuvla.ui.apps.effects :as apps-fx]
            [sixsq.nuvla.ui.apps.spec :as spec]
            [sixsq.nuvla.ui.apps.utils :as utils]
            [sixsq.nuvla.ui.apps.utils-detail :as utils-detail]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.deployments.events :as deployments-events]
            [sixsq.nuvla.ui.job.events :as job-events]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.main.spec :as main-spec]
            [sixsq.nuvla.ui.messages.events :as messages-events]
            [sixsq.nuvla.ui.plugins.nav-tab :as nav-tab]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.utils :refer [db-path->query-param-key name->href
                                                  str-pathify]]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.response :as response]))


(def refresh-action-get-module :apps-get-module)
(def refresh-action-get-deployment :apps-get-deployment)


(reg-event-fx
  ::refresh
  (fn [{{:keys [::spec/version
                ::spec/module] :as db} :db} [_ page-changed?]]
    (let [get-module-fn ::get-module
          dispatch-fn   (if page-changed?
                          [::main-events/ignore-changes-modal get-module-fn]
                          [get-module-fn version])]
      {:db db
       :fx [[:dispatch [::main-events/action-interval-start
                        {:id        refresh-action-get-module
                         :frequency 20000
                         :event     dispatch-fn}]]
            [:dispatch [::main-events/action-interval-start
                        {:id        refresh-action-get-deployment
                         :frequency 20000
                         :event     [::deployments-events/get-deployments
                                     {:filter-external-arg   (str "module/id='" (:id module) "'")
                                      :external-filter-only? true
                                      :pagination-db-path    ::apps-application-spec/deployment-pagination}]}]]]})))



;; Validation

(defn get-module
  [module-subtype db]
  (let [component   (get db ::apps-component-spec/module-component)
        project     (get db ::apps-project-spec/module-project)
        application (get db ::apps-application-spec/module-application)
        apps-sets   (get db ::apps-applications-sets-spec/apps-sets)]
    (condp = module-subtype
      utils/subtype-component component
      utils/subtype-project project
      utils/subtype-application application
      utils/subtype-application-k8s application
      utils/subtype-applications-sets apps-sets
      project)))


(reg-event-db
  ::set-validate-form?
  (fn [db [_ validate-form?]]
    (assoc db ::spec/validate-form? validate-form?)))


(reg-event-db
  ::set-module-spec
  (fn [db [_ module-spec]]
    (assoc db ::spec/module-spec module-spec)))


; Perform form validation if validate-form? is true.
(reg-event-db
  ::validate-form
  (fn [db]
    (let [form-spec      (get db ::spec/form-spec)
          module-subtype (-> db ::spec/module-common ::spec/subtype)
          module-common  (get db ::spec/module-common)
          module         (get-module module-subtype db)
          validate-form? (get db ::spec/validate-form?)
          valid?         (if validate-form?
                           (and
                             (s/valid? ::spec/module-common module-common)
                             (or (nil? form-spec) (s/valid? form-spec module)))
                           true)]
      ; Helpful to debug validation issues
      ;(log/error "module-common validate: "
      ;           (s/explain-str ::spec/module-common module-common))
      ;(log/error "optional form validate: "
      ;           (s/explain-str form-spec module))
      (assoc db ::spec/form-valid? valid?))))


; Set the spec to apply for form validation
(reg-event-db
  ::set-form-spec
  (fn [db [_ form-spec]]
    (assoc db ::spec/form-spec form-spec)))


(reg-event-db
  ::active-input
  (fn [db [_ input-name]]
    (assoc db ::spec/active-input input-name)))


(reg-event-db
  ::form-invalid
  (fn [db [_]]
    (assoc db ::spec/form-valid? false)))


(reg-event-db
  ::form-valid
  (fn [db [_]]
    (assoc db ::spec/form-valid? true)))


(reg-event-db
  ::module-not-found
  (fn [db [_ not-found?]]
    (assoc db ::spec/module-not-found? not-found?)))


(reg-event-fx
  ::set-module
  (fn [{{:keys [::spec/validate-docker-compose] :as db} :db} [_ {:keys [id] :as module}]]
    (let [db      (assoc db ::spec/module-path (:path module)
                            ::spec/module (if (nil? module) {} module)
                            ::spec/module-immutable module
                            ::spec/validate-docker-compose (if (= id (:module-id validate-docker-compose))
                                                             validate-docker-compose
                                                             nil)
                            ::spec/module-not-found? (nil? module)
                            ::main-spec/loading? false)
          subtype (:subtype module)]
      (cond-> {:db (condp = subtype
                     utils/subtype-component (apps-component-utils/module->db db module)
                     utils/subtype-project (apps-project-utils/module->db db module)
                     utils/subtype-application (apps-application-utils/module->db db module)
                     utils/subtype-application-k8s (apps-application-utils/module->db db module)
                     utils/subtype-applications-sets (apps-applications-sets-utils/module->db db module)
                     db)}
              (= subtype utils/subtype-applications-sets)
              (assoc :fx (apps-applications-sets-utils/module->fx module))))))


(reg-event-db
  ::open-add-modal
  (fn [db _]
    (assoc db ::spec/add-modal-visible? true)))


(reg-event-db
  ::close-add-modal
  (fn [db _]
    (assoc db ::spec/add-modal-visible? false
              ::spec/active-tab :details
              ::spec/add-data nil)))


(reg-event-db
  ::clear-module
  (fn [db [_ new-name new-parent new-subtype]]
    (let [new-parent       (or new-parent "")
          default-logo-url (::spec/default-logo-url db)]
      (-> db
          (assoc ::spec/module {})
          (assoc ::spec/module-immutable {})
          (assoc ::spec/module-common {})
          (assoc ::main-spec/loading? false)
          (assoc-in [::spec/module-common ::spec/name] new-name)
          (assoc-in [::spec/module-common ::spec/description] (or (utils/subtype->descr-template new-subtype)
                                                                  ""))
          (assoc-in [::spec/module-common ::spec/logo-url] default-logo-url)
          (assoc-in [::spec/module-common ::spec/parent-path] new-parent)
          (assoc-in [::spec/module-common ::spec/subtype] new-subtype)
          (assoc-in [::spec/module-common ::spec/env-variables] (sorted-map))
          (assoc-in [::spec/module-common ::spec/urls] (sorted-map))
          (assoc-in [::spec/module-common ::spec/output-parameters] (sorted-map))
          (assoc-in [::spec/module-common ::spec/data-types] (sorted-map))))))


(reg-event-fx
  ::get-module
  (fn [{{:keys [::main-spec/nav-path
                ::spec/version] :as db} :db} [_ requested-version]]
    (let [path (utils/nav-path->module-path nav-path)
          v    (if (nil? requested-version) version requested-version)]
      {:db                  (cond-> db
                                    requested-version (assoc ::spec/version requested-version))
       ::apps-fx/get-module [path v #(do (dispatch [::set-module %])
                                         (dispatch [::get-deployments-for-module %]))]})))


(reg-event-fx
  ::get-deployments-for-module
  (fn [{{:keys [::spec/module]} :db} [_ {id :id}]]
    (let [module-id (or id (:id module))]
      (when module-id
        {:fx [[:dispatch [::deployments-events/get-deployments
                          {:filter-external-arg   (str "module/id='" module-id "'")
                           :external-filter-only? true
                           :pagination-db-path    ::apps-application-spec/deployment-pagination}]]]}))))

(reg-event-fx
  ::init-view
  (fn [{{current-route :current-route :as db} :db} [_ {:keys [tab-key db-path]}]]
    (let [db-path    (or db-path [::spec/tab])
          query-key  (db-path->query-param-key db-path)
          query-view (get (:query-params current-route) query-key)]
      (when-not query-view
        {:fx [[:dispatch [::routing-events/change-query-param
                          {:partial-query-params {query-key (or
                                                              tab-key
                                                              (nav-tab/get-default-tab db db-path)
                                                              spec/default-tab)}}]]]}))))

(reg-event-fx
  ::set-active-tab
  (fn [_ [_ active-tab db-path]]
    {:fx [[:dispatch [::nav-tab/change-tab {:db-path (or db-path [::spec/tab])
                                            :tab-key active-tab}]]]}))

(reg-event-fx
  ::set-default-tab
  (fn [_ [_ active-tab]]
    {:fx [[:dispatch [::nav-tab/set-default-tab [::spec/tab] (keyword active-tab)]]]}))

(reg-event-db
  ::is-new?
  (fn [db [_ is-new?]]
    (assoc db ::spec/is-new? is-new?)))


(reg-event-db
  ::name
  (fn [db [_ name]]
    (assoc-in db [::spec/module-common ::spec/name] name)))


(reg-event-db
  ::description
  (fn [db [_ description]]
    (assoc-in db [::spec/module-common ::spec/description] description)))


(reg-event-db
  ::subtype
  (fn [db [_ subtype]]
    (assoc-in db [::spec/module-common ::spec/subtype] subtype)))


(reg-event-db
  ::acl
  (fn [db [_ acl]]
    (assoc-in db [::spec/module-common ::spec/acl] acl)))


(reg-event-db
  ::commit-message
  (fn [db [_ msg]]
    (assoc db ::spec/commit-message msg)))


(reg-event-db
  ::open-save-modal
  (fn [db _]
    (assoc db ::spec/save-modal-visible? true)))


(reg-event-db
  ::close-save-modal
  (fn [db _]
    (assoc db ::spec/save-modal-visible? false)))


(reg-event-db
  ::open-validation-error-modal
  (fn [db _]
    (assoc db ::spec/validation-error-modal-visible? true)))


(reg-event-db
  ::close-validation-error-modal
  (fn [db _]
    (assoc db ::spec/validation-error-modal-visible? false)))


(reg-event-fx
  ::save-logo-url
  (fn [{:keys [db]} [_ logo-url]]
    {:db       (-> db
                   (assoc-in [::spec/module-common ::spec/logo-url] logo-url)
                   (assoc-in [::spec/logo-url-modal-visible?] false))
     :dispatch [::main-events/changes-protection? true]}))


(reg-event-db
  ::open-logo-url-modal
  (fn [db _]
    (assoc db ::spec/logo-url-modal-visible? true)))


(reg-event-db
  ::close-logo-url-modal
  (fn [db _]
    (assoc db ::spec/logo-url-modal-visible? false)))


; Environmental variables

(reg-event-db
  ::add-env-variable
  (fn [db [_ env-variable]]
    (let [id (-> db
                 (get-in [::spec/module-common ::spec/env-variables])
                 utils/sorted-map-new-idx)]
      (assoc-in db [::spec/module-common ::spec/env-variables id] (assoc env-variable :id id)))))


(reg-event-db
  ::remove-env-variable
  (fn [db [_ id]]
    (update-in db [::spec/module-common ::spec/env-variables] dissoc id)))


(reg-event-db
  ::update-env-name
  (fn [db [_ id value]]
    (assoc-in db [::spec/module-common ::spec/env-variables id ::spec/env-name] value)))


(reg-event-db
  ::update-env-value
  (fn [db [_ id value]]
    (assoc-in db [::spec/module-common ::spec/env-variables id ::spec/env-value] value)))


(reg-event-db
  ::update-env-description
  (fn [db [_ id value]]
    (assoc-in db [::spec/module-common ::spec/env-variables id ::spec/env-description] value)))


(reg-event-db
  ::update-env-required
  (fn [db [_ id value]]
    (assoc-in db [::spec/module-common ::spec/env-variables id ::spec/env-required] value)))


; URLs

(reg-event-db
  ::add-url
  (fn [db [_ url]]
    (let [id (-> db
                 (get-in [::spec/module-common ::spec/urls])
                 utils/sorted-map-new-idx)]
      (assoc-in db [::spec/module-common ::spec/urls id] (assoc url :id id)))))


(reg-event-db
  ::remove-url
  (fn [db [_ id]]
    (update-in db [::spec/module-common ::spec/urls] dissoc id)))


(reg-event-db
  ::update-url-name
  (fn [db [_ id name]]
    (assoc-in db [::spec/module-common ::spec/urls id ::spec/url-name] name)))


(reg-event-db
  ::update-url-url
  (fn [db [_ id url]]
    (assoc-in db [::spec/module-common ::spec/urls id ::spec/url] url)))


; Output-parameters

(reg-event-db
  ::add-output-parameter
  (fn [db [_ param]]
    (let [id (-> db
                 (get-in [::spec/module-common ::spec/output-parameters])
                 utils/sorted-map-new-idx)]
      (assoc-in db [::spec/module-common ::spec/output-parameters id] (assoc param :id id)))))


(reg-event-db
  ::remove-output-parameter
  (fn [db [_ id]]
    (update-in db [::spec/module-common ::spec/output-parameters] dissoc id)))


(reg-event-db
  ::update-output-parameter-name
  (fn [db [_ id name]]
    (assoc-in db [::spec/module-common ::spec/output-parameters id
                  ::spec/output-parameter-name] name)))


(reg-event-db
  ::update-output-parameter-description
  (fn [db [_ id description]]
    (assoc-in db [::spec/module-common ::spec/output-parameters id
                  ::spec/output-parameter-description] description)))


; Data types

(reg-event-db
  ::add-data-type
  (fn [db [_ data-type-map]]
    (let [id                (-> db
                                (get-in [::spec/module-common ::spec/data-types])
                                utils/sorted-map-new-idx)
          default-data-type (:key (first @utils-detail/data-type-options))]
      (assoc-in db
                [::spec/module-common ::spec/data-types id]
                (assoc data-type-map :id id, ::spec/data-type default-data-type)))))


(reg-event-db
  ::remove-data-type
  (fn [db [_ id]]
    (update-in db [::spec/module-common ::spec/data-types] dissoc id)))


(reg-event-db
  ::update-data-type
  (fn [db [_ id dt]]
    (assoc-in db [::spec/module-common ::spec/data-types id] {:id id ::spec/data-type dt})))


;; Private registries

(reg-event-db
  ::add-registry
  (fn [db [_ registry]]
    (let [id (-> db
                 (get-in [::spec/module-common ::spec/registries])
                 utils/sorted-map-new-idx)]
      (assoc-in db [::spec/module-common ::spec/registries id]
                (assoc registry :id id
                                ::spec/registry-cred-id "")))))


(reg-event-db
  ::remove-registry
  (fn [db [_ id]]
    (update-in db [::spec/module-common ::spec/registries] dissoc id)))


(reg-event-db
  ::update-registry-id
  (fn [db [_ id registry-id]]
    (-> db
        (assoc-in [::spec/module-common ::spec/registries
                   id ::spec/registry-id] registry-id)
        (assoc-in [::spec/module-common ::spec/registries
                   id ::spec/registry-cred-id] ""))))


(reg-event-db
  ::update-registry-cred-id
  (fn [db [_ id registry-cred-id]]
    (assoc-in db [::spec/module-common ::spec/registries
                  id ::spec/registry-cred-id] registry-cred-id)))


(reg-event-db
  ::cent-amount-daily
  (fn [db [_ amount]]
    (assoc-in db [::spec/module-common ::spec/price] {:cent-amount-daily amount
                                                      :currency          "EUR"})))

(reg-event-db
  ::follow-customer-trial
  (fn [db [_ follow?]]
    (update-in db [::spec/module-common ::spec/price] assoc :follow-customer-trial follow?)))


(defn find-license
  [license-name licenses]
  (some #(when (= license-name (:license-name %)) %) licenses))


(reg-event-db
  ::set-license
  (fn [db [_ license-name licenses]]
    (let [license (find-license license-name licenses)
          {:keys [license-name license-description license-url]} license]
      (-> db
          (assoc-in [::spec/module-common ::spec/license :license-name] license-name)
          (assoc-in [::spec/module-common ::spec/license :license-description] license-description)
          (assoc-in [::spec/module-common ::spec/license :license-url] license-url)))))


(reg-event-db
  ::license-name
  (fn [db [_ name]]
    (assoc-in db [::spec/module-common ::spec/license :license-name] name)))


(reg-event-db
  ::license-description
  (fn [db [_ description]]
    (assoc-in db [::spec/module-common ::spec/license :license-description] description)))


(reg-event-db
  ::license-url
  (fn [db [_ url]]
    (assoc-in db [::spec/module-common ::spec/license :license-url] url)))


(reg-event-db
  ::set-registries-infra
  (fn [db [_ {resources :resources}]]
    (assoc db ::spec/registries-infra resources)))


(reg-event-fx
  ::get-registries-infra
  (fn [_ _]
    {::cimi-api-fx/search
     [:infrastructure-service
      {:filter  "subtype='registry'"
       :select  "id, name"
       :orderby "name:asc, id:asc"
       :last    10000} #(dispatch [::set-registries-infra %])]}))


(reg-event-db
  ::set-registries-credentials
  (fn [db [_ {resources :resources}]]
    (assoc db ::spec/registries-credentials resources)))


(reg-event-fx
  ::get-registries-credentials
  (fn [_ _]
    {::cimi-api-fx/search
     [:credential
      {:filter  "subtype='infrastructure-service-registry'"
       :select  "id, name, parent"
       :orderby "name:asc, id:asc"
       :last    10000} #(dispatch [::set-registries-credentials %])]}))


(reg-event-db
  ::set-module-to-compare
  (fn [db [_ which-one module]]
    (if (= which-one "left")
      (assoc db ::spec/compare-module-left module)
      (assoc db ::spec/compare-module-right module))))


(reg-event-fx
  ::get-module-to-compare
  (fn [_ [_ module-id-version which-one]]
    {::cimi-api-fx/get
     [module-id-version #(dispatch [::set-module-to-compare which-one %])]}))


(reg-event-db
  ::docker-compose-validation-complete
  (fn [{:keys [::spec/module] :as db}
       [_ {:keys [return-code target-resource status-message] :as _job}]]
    (cond-> db
            (= (:href target-resource)
               (:id module)) (assoc ::spec/validate-docker-compose
                                    {:valid?    (= return-code 0)
                                     :loading?  false
                                     :error-msg status-message}))))


(reg-event-fx
  ::validate-docker-compose
  (fn [{db :db} [_ module-or-id]]
    (let [validate-op "validate-docker-compose"
          id          (if (string? module-or-id) module-or-id (:id module-or-id))
          on-success  (fn [response]
                        (dispatch [::job-events/wait-job-to-complete
                                   {:job-id              (:location response)
                                    :on-complete         #(dispatch [::docker-compose-validation-complete %])
                                    :refresh-interval-ms 5000}]))]
      (when (or (string? module-or-id)
                (general-utils/can-operation? validate-op module-or-id))
        {:db                     (assoc db ::spec/validate-docker-compose {:loading?  true
                                                                           :module-id id})
         ::cimi-api-fx/operation [id validate-op on-success]}))))

(defn version-id->index
  [{:keys [versions] :as module}]
  (let [version-id   (-> module :content :id)
        map-versions (utils/map-versions-index versions)]
    (ffirst (filter #(-> % second :href (= version-id)) map-versions))))

(reg-event-fx
  ::edit-module
  (fn [{{:keys [::spec/module] :as db} :db} [_ commit-map]]
    (let [id (:id module)
          {:keys [subtype] :as sanitized-module} (utils-detail/db->module module commit-map db)
          is-app? (= subtype utils/subtype-application)]
      (if (nil? id)
        {::cimi-api-fx/add [:module sanitized-module
                            #(do
                               (dispatch [::set-module sanitized-module]) ;Needed?
                               (when is-app?
                                 (dispatch [::validate-docker-compose (:resource-id %)]))
                               (dispatch [::main-events/reset-changes-protection
                                          [::routing-events/navigate
                                           (str-pathify (name->href routes/apps) (:path sanitized-module))]]))
                            :on-error #(let [{:keys [status]} (response/parse-ex-info %)]
                                         (cimi-api-fx/default-add-on-error :module %)
                                         (when (= status 409)
                                           (dispatch [::name nil])
                                           (dispatch [::validate-form])))]}
        {::cimi-api-fx/edit [id (if commit-map
                                  sanitized-module
                                  (dissoc sanitized-module :content))
                             #(if (instance? js/Error %)
                                (let [{:keys [status message]} (response/parse-ex-info %)]
                                  (dispatch [::messages-events/add
                                             {:header  (cond-> (str "error editing " id)
                                                               status (str " (" status ")"))
                                              :content message
                                              :type    :error}]))
                                (do (dispatch [::get-module (version-id->index %)])
                                    (when is-app?
                                      (dispatch [::validate-docker-compose %]))
                                    (dispatch [::main-events/reset-changes-protection])))]}))))


(reg-event-fx
  ::delete-module
  (fn [{:keys [db]} [_ id]]
    ;; TODO: Add on-error restoring changes-protection
    (let [query-params {:apps-store-tab (-> db ::apps-store-spec/tab :default-tab)}]
      {:db                  (-> db
                                (dissoc ::spec/module)
                                (assoc  ::spec/form-valid? true))
       ::cimi-api-fx/delete [id #(dispatch [::main-events/reset-changes-protection
                                            [::routing-events/navigate routes/apps nil query-params]])]})))


(reg-event-db
  ::copy
  (fn [db [_]]
    (assoc db ::spec/copy-module (::spec/module db))))


(reg-event-db
  ::open-paste-modal
  (fn [db _]
    (assoc db ::spec/paste-modal-visible? true)))


(reg-event-db
  ::close-paste-modal
  (fn [db _]
    (assoc db ::spec/paste-modal-visible? false)))


(reg-event-fx
  ::paste-module
  (fn [{{:keys [::spec/copy-module ::spec/module]} :db} [_ new-module-name]]
    (let [paste-parent-path (:path module)
          paste-module      (-> copy-module
                                (assoc :name new-module-name)
                                (assoc :parent-path paste-parent-path)
                                (assoc :path (utils/contruct-path paste-parent-path new-module-name)))]

      {::cimi-api-fx/add [:module paste-module
                          #(dispatch [::main-events/reset-changes-protection
                                      [::routing-events/navigate
                                       (str-pathify (name->href routes/apps) (:path paste-module))]])
                          :on-error #(let [{:keys [status]} (response/parse-ex-info %)]
                                       (cimi-api-fx/default-add-on-error :module %)
                                       (when (= status 409)
                                         (dispatch [::name nil])
                                         (dispatch [::validate-form])))]})))


(reg-event-db
  ::reset-version
  (fn [db _]
    (assoc db ::spec/version nil)))


(defn publish-unpublish
  [module id publish?]
  (let [id            (if id id (:id module))
        version-index (version-id->index module)]
    {::cimi-api-fx/operation [(str id "_" version-index)
                              (if publish? "publish" "unpublish")
                              #(do (dispatch [::messages-events/add
                                              {:header  (if publish?
                                                          "Publication successful"
                                                          "Un-publication successful")
                                               :content (str "Module version: "
                                                             version-index
                                                             (if publish?
                                                               " is now published."
                                                               " is not published anymore."))
                                               :type    :success}])
                                   (dispatch [::get-module version-index]))]}))


(reg-event-fx
  ::publish
  (fn [{{:keys [::spec/module]} :db} [_ id]]
    (publish-unpublish module id true)))


(reg-event-fx
  ::un-publish
  (fn [{{:keys [::spec/module]} :db} [_ id]]
    (publish-unpublish module id false)))


(reg-event-db
  ::set-details-validation-error
  (fn [db [_ key error?]]
    (utils/set-reset-error db key error? ::spec/details-validation-errors)))


(reg-event-db
  ::set-tags
  (fn [db [_ tags]]
    (assoc-in db [::spec/module :tags] tags)))
