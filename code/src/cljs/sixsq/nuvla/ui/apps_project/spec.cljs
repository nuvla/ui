(ns sixsq.nuvla.ui.apps-project.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.apps.spec :as apps-spec]))

; create an initial entry for new components
(def defaults {})

(s/def ::module-project (s/merge ::apps-spec/summary))
