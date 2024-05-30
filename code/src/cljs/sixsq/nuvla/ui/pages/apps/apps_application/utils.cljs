(ns sixsq.nuvla.ui.pages.apps.apps-application.utils
  (:require [clojure.string :as str]
            [sixsq.nuvla.ui.pages.apps.apps-application.spec :as spec]
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
        docker-compose       (get-in db [::spec/module-application ::spec/docker-compose])
        compatibility        (get-in db [::spec/module-application ::spec/compatibility])
        files                (files->module db)
        requires-user-rights (get-in db [::spec/module-application ::spec/requires-user-rights])]
    (as-> module m
          (assoc-in m [:content :author] author)
          (assoc-in m [:content :commit] (if (empty? commit) "no commit message" commit))
          (assoc-in m [:content :requires-user-rights] requires-user-rights)
          (assoc-in m [:content :docker-compose] docker-compose)
          (if (= subtype "application")
            (assoc m :compatibility compatibility)
            m)
          (if (empty? files)
            (update-in m [:content] dissoc :files)
            (assoc-in m [:content :files] files)))))

(defn db->helm-module
  [helm-module commit-map db]
  (let [{:keys [author commit]}                  commit-map
        files                                    (files->module db)
        requires-user-rights                     (get-in db [::spec/module-application ::spec/requires-user-rights])
        helm-info                                (::apps-spec/helm-info db)
        {:keys [repo-or-url? helm-chart-values]} helm-info
        helm-info-to-submit                      (if (= :url repo-or-url?)
                                                   (select-keys helm-info [:helm-absolute-url])
                                                   (select-keys helm-info [:helm-repo-url
                                                                           :helm-chart-name
                                                                           :helm-repo-creds
                                                                           :helm-chart-version]))]
    (as-> helm-module m
          (assoc-in m [:content :author] author)
          (assoc-in m [:content :commit] (if (empty? commit) "no commit message" commit))
          (assoc-in m [:content :requires-user-rights] requires-user-rights)
          (update-in m [:content] dissoc :helm-repo-url
                                         :helm-chart-name
                                         :helm-repo-creds
                                         :helm-chart-version
                                         :helm-absolute-url)
          (update-in m [:content] #(merge % helm-info-to-submit))
          (if helm-chart-values
            (assoc-in m [:content :helm-chart-values] helm-chart-values)
            m)
          (if (empty? files)
            (update-in m [:content] dissoc :files)
            (assoc-in m [:content :files] files)))))
