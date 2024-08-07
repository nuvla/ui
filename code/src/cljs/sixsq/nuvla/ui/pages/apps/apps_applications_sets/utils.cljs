(ns sixsq.nuvla.ui.pages.apps.apps-applications-sets.utils
  (:require
    [clojure.string :as str]
    [sixsq.nuvla.ui.common-components.plugins.module :as module-plugin]
    [sixsq.nuvla.ui.common-components.plugins.module-selector :as module-selector]
    [sixsq.nuvla.ui.pages.apps.apps-applications-sets.events :as events]
    [sixsq.nuvla.ui.pages.apps.apps-applications-sets.spec :as spec]
    [sixsq.nuvla.ui.pages.apps.utils :as apps-utils]))

(def app-set-app-subtypes
  {spec/app-set-docker-subtype #{apps-utils/subtype-component apps-utils/subtype-application}
   spec/app-set-k8s-subtype    #{apps-utils/subtype-application-k8s apps-utils/subtype-application-helm}})

;; Deserialization functions: module->db

(defn apps-sets->db
  [applications-sets]
  (into (sorted-map)
        (for [[id {:keys [name description subtype]}] (map-indexed vector applications-sets)]
          [id {:id                         id
               ::spec/apps-set-name        name
               ::spec/apps-set-description description
               ::spec/apps-set-subtype     subtype
               ::spec/apps-selector        (module-selector/build-spec)}])))

(defn module->db
  [db {:keys [content] :as module}]
  (-> db
      (apps-utils/module->db module)
      (assoc ::spec/apps-sets (apps-sets->db (:applications-sets content)))))

(defn module->fx
  [module]
  [[:dispatch [::events/reload-apps-sets module]]])

;; search selected applications to load into apps-selected, if not exist put it as unknown
;; load each module version selected

;; Serialization functions: db->module

(defn app-selected->application
  [db id {app-id :id :as _app-selected}]
  (let [db-path    [::spec/apps-sets id]
        env-vars   (module-plugin/db-changed-env-vars db db-path app-id)
        files      (module-plugin/db-changed-files db db-path app-id)
        regs-creds (module-plugin/db-module-registries-credentials
                     db db-path app-id)]
    (cond-> {:id      app-id
             :version (module-plugin/db-selected-version
                        db [::spec/apps-sets id] app-id)}
            (seq env-vars) (assoc :environmental-variables env-vars)
            (seq files) (assoc :files files)
            (seq regs-creds) (assoc :registries-credentials regs-creds))))

(defn db->applications-set
  [db id apps-selected]
  (->> apps-selected
       vals
       (map (partial app-selected->application db id))))

(defn db->applications-sets
  [db]
  (->> db
       ::spec/apps-sets
       vals
       (map (fn [{:keys [:id
                         ::spec/apps-set-name
                         ::spec/apps-set-description
                         ::spec/apps-set-subtype
                         ::spec/apps-selected]}]
              (cond-> {:name apps-set-name}

                      (not (str/blank? apps-set-description))
                      (assoc :description apps-set-description)

                      apps-set-subtype
                      (assoc :subtype apps-set-subtype)

                      (seq apps-selected)
                      (assoc :applications (db->applications-set db id apps-selected)))))))

(defn db->module
  [module commit-map db]
  (let [{:keys [author commit]} commit-map]
    (as-> module m
          (assoc-in m [:content :author] author)
          (assoc-in m [:content :commit] (if (empty? commit) "no commit message" commit))
          (update m :content dissoc :output-parameters)
          (assoc-in m [:content :applications-sets] (db->applications-sets db)))))
