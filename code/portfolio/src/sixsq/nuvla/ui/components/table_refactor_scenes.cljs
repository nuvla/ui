(ns sixsq.nuvla.ui.components.table-refactor-scenes
  (:require [sixsq.nuvla.ui.portfolio-utils :refer [defscene]]
            [sixsq.nuvla.ui.common-components.plugins.table-refactor :as table-refactor]))

(defscene table-refactor
  [table-refactor/TableController])
