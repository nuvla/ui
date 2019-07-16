(ns sixsq.nuvla.ui.apps-application.utils
  (:require [sixsq.nuvla.ui.apps-application.spec :as spec]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]
            [taoensso.timbre :as log]))

;; Deserialization functions: module->db


(defn module->db
  [db {:keys [content] :as module}]
  (let [{:keys [docker-compose]} content]
    (-> db
        (apps-utils/module->db module)
        (assoc-in [::spec/module-application ::spec/docker-compose] docker-compose))))


;; Serialization functions: db->module

(defn db->module
  [module commit-map db]
  (let [{:keys [author commit]} commit-map
        docker-compose (get-in db [::spec/module-application ::spec/docker-compose])]
    (as-> module m
          (assoc-in m [:content :author] author)
          (assoc-in m [:content :commit] (if (empty? commit) "no commit message" commit))
          (assoc-in m [:content :docker-compose] docker-compose))))
