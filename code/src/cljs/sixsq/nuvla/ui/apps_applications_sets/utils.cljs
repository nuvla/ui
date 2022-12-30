(ns sixsq.nuvla.ui.apps-applications-sets.utils
  (:require [sixsq.nuvla.ui.apps-applications-sets.spec :as spec]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]))


;; Deserialization functions: module->db


(defn module->db
  [db module]
  (-> db
      (apps-utils/module->db module)))


;; Serialization functions: db->module

(defn db->module
  [module commit-map db]
  (let [{:keys [author commit]} commit-map]
    (as-> module m
          (assoc-in m [:content :author] author)
          (assoc-in m [:content :commit] (if (empty? commit) "no commit message" commit)))))
