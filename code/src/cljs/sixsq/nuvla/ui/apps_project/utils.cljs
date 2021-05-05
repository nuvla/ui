(ns sixsq.nuvla.ui.apps-project.utils
  (:require [sixsq.nuvla.ui.apps.utils :as apps-utils]))


(def tab-details 1)


(defn module->db
  [db module]
  (-> db
      (apps-utils/module->db module)))
