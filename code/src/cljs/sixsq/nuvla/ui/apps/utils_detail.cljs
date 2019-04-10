(ns sixsq.nuvla.ui.apps.utils-detail
  (:require
    [sixsq.nuvla.ui.apps-component.utils :as apps-component-utils]
    [sixsq.nuvla.ui.apps-project.utils :as apps-project-utils]))

(defn db->module
  [module commit db]
  (let [type (:type module)]
    (cond
      (= "COMPONENT" type) (apps-component-utils/db->module module commit db)
      (= "PROJECT" type) (apps-project-utils/db->module module)
      :else module)))
