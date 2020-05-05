(ns sixsq.nuvla.ui.intercom.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::app-id any?)


(s/def ::events any?)


(s/def ::db (s/keys :req [::app-id ::events]))


(def defaults {::app-id ""                                  ; "qjggmp6w"
               ::events {}})
