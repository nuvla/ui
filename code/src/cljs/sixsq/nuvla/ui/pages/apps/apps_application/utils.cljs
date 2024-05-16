(ns sixsq.nuvla.ui.pages.apps.apps-application.utils
  (:require [sixsq.nuvla.ui.pages.apps.apps-application.spec :as spec]
            [sixsq.nuvla.ui.pages.apps.spec :as apps-spec]
            [sixsq.nuvla.ui.pages.apps.utils :as apps-utils]))


;; Deserialization functions: module->db

(defn files->db
  [files]
  (into
    (sorted-map)
    (for [[id {:keys [file-name file-content]}] (map-indexed vector files)]
      [id {:id                 id
           ::spec/file-name    file-name
           ::spec/file-content file-content}])))


(defn module->db
  [db {:keys [compatibility content] :as module}]
  (let [{:keys [docker-compose requires-user-rights]} content]
    (-> db
        (apps-utils/module->db module)
        (assoc-in [::spec/module-application ::spec/requires-user-rights] (true? requires-user-rights))
        (assoc-in [::spec/module-application ::spec/docker-compose] docker-compose)
        (assoc-in [::spec/module-application ::spec/compatibility] compatibility)
        (assoc-in [::spec/module-application ::spec/files]
                  (files->db (:files content))))))


;; Serialization functions: db->module

(defn files->module
  [db]
  (into
    []
    (for [[_id file] (get-in db [::spec/module-application ::spec/files])]
      (let [{:keys [::spec/file-name ::spec/file-content]} file]
        (conj
          {:file-name file-name}
          {:file-content file-content})))))


(defn db->module
  [{:keys [subtype] :as module} commit-map db]
  (let [{:keys [author commit]} commit-map
        docker-compose                (get-in db [::spec/module-application ::spec/docker-compose])
        compatibility                 (get-in db [::spec/module-application ::spec/compatibility])
        files                         (files->module db)
        requires-user-rights          (get-in db [::spec/module-application ::spec/requires-user-rights])
        helm-info                     (::apps-spec/helm-info db)
        helm-absolute-url-provided?   (:helm-absolute-url helm-info)]
    (as-> module m
          (if (= subtype "application_helm")

            m)
          (if (and (= subtype "application_helm") helm-absolute-url-provided?)
            (assoc-in m [:content :helm-absolute-url] (:helm-absolute-url helm-info))
            m)
          (if (and (= subtype "application_helm") (not helm-absolute-url-provided?))
            (-> m
                (assoc-in [:content :helm-repo-url] (:helm-repo-url helm-info))
                (assoc-in [:content :helm-chart-name] (:helm-chart-name helm-info)))
            m)
          (assoc-in m [:content :author] author)
          (assoc-in m [:content :commit] (if (empty? commit) "no commit message" commit))
          (if docker-compose
            (assoc-in m [:content :docker-compose] docker-compose)
            m)
          (assoc-in m [:content :requires-user-rights] requires-user-rights)
          (if (= subtype "application")
            (assoc m :compatibility compatibility)
            m)
          (if (empty? files)
            (update-in m [:content] dissoc :files)
            (assoc-in m [:content :files] files)))))
