(ns sixsq.nuvla.ui.pages.apps.apps-project.utils
  (:require [sixsq.nuvla.ui.pages.apps.utils :as apps-utils]))


(defn module->db
  [db module]
  (apps-utils/module->db db module))
