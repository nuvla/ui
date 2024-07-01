(ns sixsq.nuvla.ui.pages.dashboard.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.common-components.plugins.audit-log :as audit-log-plugin]))

(s/def ::events any?)

(def defaults {::events (audit-log-plugin/build-spec
                          :default-items-per-page 4)})
