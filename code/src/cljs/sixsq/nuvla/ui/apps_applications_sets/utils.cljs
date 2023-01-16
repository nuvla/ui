(ns sixsq.nuvla.ui.apps-applications-sets.utils
  (:require
    [clojure.string :as str]
    [sixsq.nuvla.ui.apps-applications-sets.events :as events]
    [sixsq.nuvla.ui.apps-applications-sets.spec :as spec]
    [sixsq.nuvla.ui.apps.utils :as apps-utils]
    [sixsq.nuvla.ui.plugins.module :as module-plugin]
    [sixsq.nuvla.ui.plugins.module-selector :as module-selector]))


;; Deserialization functions: module->db

(defn apps-sets->db
  [applications-sets]
  (into (sorted-map)
        (for [[id {:keys [name description]}] (map-indexed vector applications-sets)]
          [id {:id                         id
               ::spec/apps-set-name        name
               ::spec/apps-set-description description
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

(defn db->applications-set
  [db id apps-selected]
  (->> apps-selected
       vals
       (map #(hash-map
               :id (:id %)
               :version (module-plugin/db-selected-version
                          db [::spec/apps-sets id] (:id %))))))

(defn db->applications-sets
  [db]
  (->> db
       ::spec/apps-sets
       vals
       (map (fn [{:keys [:id
                         ::spec/apps-set-name
                         ::spec/apps-set-description
                         ::spec/apps-selected]}]
              (cond-> {:name apps-set-name}

                      (not (str/blank? apps-set-description))
                      (assoc :description apps-set-description)

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
