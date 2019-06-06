(ns sixsq.nuvla.ui.apps.events
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.apps-component.spec :as apps-component-spec]
    [sixsq.nuvla.ui.apps-component.utils :as apps-component-utils]
    [sixsq.nuvla.ui.apps-project.spec :as apps-project-spec]
    [sixsq.nuvla.ui.apps-project.utils :as apps-project-utils]
    [sixsq.nuvla.ui.apps.effects :as apps-fx]
    [sixsq.nuvla.ui.apps.spec :as spec]
    [sixsq.nuvla.ui.apps.utils :as utils]
    [sixsq.nuvla.ui.apps.utils-detail :as utils-detail]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.cimi-detail.events :as cimi-detail-events]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.main.spec :as main-spec]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.response :as response]
    [taoensso.timbre :as log]))


;; Validation

(defn get-module
  [module-subtype db]
  (let [component (get db ::apps-component-spec/module-component)
        project   (get db ::apps-project-spec/module-project)]
    (case module-subtype
      :component component
      :project project
      project)))


(reg-event-db
  ::set-validate-form?
  (fn [db [_ validate-form?]]
    (assoc db ::spec/validate-form? validate-form?)))


(reg-event-db
  ::set-module-spec
  (fn [db [_ module-spec]]
    (assoc db ::spec/module-spec module-spec)))


(reg-event-db
  ::set-module-subtype
  (fn [db [_ module-subtype]]
    (assoc db ::spec/module-subtype module-subtype)))


; Perform form validation if validate-form? is true.
(reg-event-db
  ::validate-form
  (fn [db [_]]
    (let [form-spec      (get db ::spec/form-spec)
          module-subtype (get db ::spec/module-subtype)
          module-common  (get db ::spec/module-common)
          module         (get-module module-subtype db)
          validate-form? (get db ::spec/validate-form?)
          valid?         (if validate-form? (and
                                              (s/valid? ::spec/module-common module-common)
                                              (if (nil? form-spec) true (s/valid? form-spec module)))
                                            true)]
      (s/explain ::spec/module-common module-common)
      (s/explain form-spec module)
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
  ::set-version-warning
  (fn [db [_]]
    (assoc db ::spec/version-warning? true)))


(reg-event-db
  ::clear-version-warning
  (fn [db [_]]
    (assoc db ::spec/version-warning? false)))


(reg-event-db
  ::set-module
  (fn [db [_ module]]
    (let [db      (assoc db ::spec/completed? true
                            ::spec/module-path (:path module)
                            ::spec/module (if (nil? module) {} module))
          subtype (:subtype module)]
      (case subtype                                         ;;FIXME no default case cause stack trace
        "component" (apps-component-utils/module->db db module)
        "project" (apps-project-utils/module->db db module)))))


(reg-event-db
  ::open-add-modal
  (fn [db _]
    (assoc db ::spec/add-modal-visible? true)))


(reg-event-db
  ::close-add-modal
  (fn [db _]
    (assoc db ::spec/add-modal-visible? false
              ::spec/active-tab :project
              ::spec/add-data nil)))


(reg-event-db
  ::clear-module
  (fn [db [_ new-name new-parent new-subtype]]
    (let [new-parent       (or new-parent "")
          default-logo-url (::spec/default-logo-url db)]
      (-> db
          (assoc ::spec/module {})
          (assoc ::spec/module-common {})
          (assoc-in [::spec/module-common ::spec/name] new-name)
          (assoc-in [::spec/module-common ::spec/description] "")
          (assoc-in [::spec/module-common ::spec/logo-url] default-logo-url)
          (assoc-in [::spec/module-common ::spec/parent-path] new-parent)
          (assoc-in [::spec/module-common ::spec/subtype] new-subtype)))))


(reg-event-fx
  ::get-module
  (fn [{{:keys [::main-spec/nav-path] :as db} :db} [_ version]]
    (let [path (utils/nav-path->module-path nav-path)]
      {:db                  (assoc db ::spec/completed? false
                                      ::spec/module nil)
       ::apps-fx/get-module [path version #(dispatch [::set-module %])]})))


(reg-event-db
  ::update-add-data
  (fn [{:keys [::spec/add-data] :as db} [_ path value]]
    (assoc-in db (concat [::spec/add-data] path) value)))


(reg-event-db
  ::set-active-tab
  (fn [db [_ active-tab]]
    (assoc db ::spec/active-tab active-tab)))


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
    (assoc-in db [::spec/module-common :subtype] (if (nil? subtype) nil (str/upper-case subtype)))))


(reg-event-db
  ::path
  (fn [db [_ path]]
    (assoc-in db [::spec/module-common :path] path)))

(reg-event-db
  ::acl
  (fn [db [_ acl]]
    (assoc-in db [::spec/module-common ::spec/acl] acl)))


(reg-event-db
  ::parent
  (fn [db [_ parent]]
    (assoc-in db [::spec/module-common :parent-path] parent)))


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


(reg-event-db
  ::save-logo-url
  (fn [db [_ logo-url]]
    (dispatch [::main-events/changes-protection? true])
    (-> db
        (assoc-in [::spec/module-common ::spec/logo-url] logo-url)
        (assoc-in [::spec/logo-url-modal-visible?] false))))


(reg-event-db
  ::open-logo-url-modal
  (fn [db _]
    (assoc db ::spec/logo-url-modal-visible? true)))


(reg-event-db
  ::close-logo-url-modal
  (fn [db _]
    (assoc db ::spec/logo-url-modal-visible? false)))


(reg-event-fx
  ::edit-module
  (fn [{{:keys [::spec/module] :as db} :db} [_ commit-map]]
    (let [id               (:id module)
          sanitized-module (utils-detail/db->module module commit-map db)]
      (if (nil? id)
        {::cimi-api-fx/add [:module sanitized-module
                            #(if (instance? js/Error %)
                               (let [{:keys [status message]} (response/parse-ex-info %)]
                                 (dispatch [::messages-events/add
                                            {:header  (cond-> (str "error editing " id)
                                                              status (str " (" status ")"))
                                             :content message
                                             :type    :error}]))
                               (do (dispatch [::cimi-detail-events/get (:resource-id %)])
                                   (dispatch [::set-module sanitized-module]) ;Needed?
                                   (dispatch [::main-events/changes-protection? false])
                                   (dispatch [::history-events/navigate (str "apps/" (:path sanitized-module))])))]}
        {::cimi-api-fx/edit [id sanitized-module
                             #(if (instance? js/Error %)
                                (let [{:keys [status message]} (response/parse-ex-info %)]
                                  (dispatch [::messages-events/add
                                             {:header  (cond-> (str "error editing " id)
                                                               status (str " (" status ")"))
                                              :content message
                                              :type    :error}]))
                                (do (dispatch [::cimi-detail-events/get (:id %)])
                                    (dispatch [::get-module])
                                    (dispatch [::main-events/changes-protection? false])))]}))))

(reg-event-fx
  ::delete-module
  (fn [{:keys [db]} [_ id]]
    {:db                  (dissoc db ::spec/module)
     ::cimi-api-fx/delete [id #(do
                                 (dispatch [::main-events/changes-protection? false])
                                 (dispatch [::history-events/navigate "apps/"]))]}))
