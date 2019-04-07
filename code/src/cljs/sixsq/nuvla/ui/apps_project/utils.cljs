(ns sixsq.nuvla.ui.apps-project.utils
  (:require [sixsq.nuvla.ui.apps.utils :as apps-utils]))

(defn db->module
  [module]
  (-> module
      (apps-utils/sanitize-base)
      (dissoc :children)))
