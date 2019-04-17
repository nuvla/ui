(ns sixsq.nuvla.ui.apps-project.utils
  (:require [sixsq.nuvla.ui.apps.utils :as apps-utils]))

(defn db->module
  [module commit-map db]
  module)


(defn module->db
  [db module]
  (-> db
      (apps-utils/module->db module)))
