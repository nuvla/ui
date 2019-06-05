(ns sixsq.nuvla.ui.apps.utils-detail
  (:require
    [sixsq.nuvla.ui.apps-component.utils :as apps-component-utils]
    [sixsq.nuvla.ui.apps-project.utils :as apps-project-utils]
    [sixsq.nuvla.ui.apps.spec :as spec]
    [sixsq.nuvla.ui.apps.utils :as utils]
    [taoensso.timbre :as log]))

(defn db->module
  [module commit db]
  (let [subtype (::spec/module-subtype db)
        module  (utils/db->module module commit db)]
    (case subtype
      :component (apps-component-utils/db->module module commit db)
      :project module
      module)))