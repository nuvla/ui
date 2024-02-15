(ns sixsq.nuvla.ui.pages.apps.apps-project.utils
  (:require [sixsq.nuvla.ui.pages.apps.utils :as apps-utils]))


(defn module->db
  [db module]
  (-> db
      (apps-utils/module->db module)))
