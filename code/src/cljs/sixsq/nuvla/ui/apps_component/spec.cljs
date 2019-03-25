(ns sixsq.nuvla.ui.apps-component.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]))

(def defaults {::port-mappings {1 {}}                       ; create an initial entry for new components
               ::volumes       {1 {}}                       ; create an initial entry for new components
               })

(s/def ::port-mappings any?)
(s/def ::volumes any?)
