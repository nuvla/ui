(ns sixsq.nuvla.ui.pages.apps.apps-project.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.common-components.plugins.nav-tab :as nav-tab]))

(s/def ::tab any?)

; create an initial entry for new components
(def defaults {::tab (nav-tab/build-spec :default-tab :overview)})

(s/def ::module-project nil?)
