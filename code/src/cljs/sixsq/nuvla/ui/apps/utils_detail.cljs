(ns sixsq.nuvla.ui.apps.utils-detail
  (:require
    [sixsq.nuvla.ui.apps.spec :as spec]
    [sixsq.nuvla.ui.apps-component.utils :as apps-component-utils]
    [sixsq.nuvla.ui.apps-project.utils :as apps-project-utils]
    [sixsq.nuvla.ui.apps.utils :as utils]
    [taoensso.timbre :as log]))

(defn db->module
  [module commit db]
  (let [type (::spec/module-type db)
        module (utils/db->module module commit db)]
    (log/infof "IN... %s" type)
    (case type
      :component (apps-component-utils/db->module module commit db)
      :project (apps-project-utils/db->module module commit db)
      module)))