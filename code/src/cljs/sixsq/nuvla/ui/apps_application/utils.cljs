(ns sixsq.nuvla.ui.apps-application.utils
  (:require [sixsq.nuvla.ui.apps-application.spec :as spec]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]))

;; Deserialization functions: module->db

(defn files->db
  [files]
  (into {}
        (for [{:keys [file-name file-content]} files]
          (let [id (random-uuid)]
            {id {:id                 id
                 ::spec/file-name    file-name
                 ::spec/file-content file-content}}))))


(defn module->db
  [db {:keys [content] :as module}]
  (let [{:keys [docker-compose]} content]
    (-> db
        (apps-utils/module->db module)
        (assoc-in [::spec/module-application ::spec/docker-compose] docker-compose)
        (assoc-in [::spec/module-application ::spec/files]
                  (files->db (:files content))))))


;; Serialization functions: db->module

(defn files->module
  [db]
  (into []
        (for [[id file] (get-in db [::spec/module-application ::spec/files])]
          (let [{:keys [::spec/file-name ::spec/file-content]} file]
            (conj
              {:file-name file-name}
              {:file-content file-content})))))


(defn db->module
  [module commit-map db]
  (let [{:keys [author commit]} commit-map
        docker-compose (get-in db [::spec/module-application ::spec/docker-compose])
        files          (files->module db)]
    (as-> module m
          (assoc-in m [:content :author] author)
          (assoc-in m [:content :commit] (if (empty? commit) "no commit message" commit))
          (assoc-in m [:content :docker-compose] docker-compose)
          (if (empty? files)
            (update-in m [:content] dissoc :files)
            (assoc-in m [:content :files] files)))))
