(ns sixsq.nuvla.ui.main.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::iframe? boolean?)


(s/def ::device #{:mobile :tablet :computer :large-screen :wide-screen})


(s/def ::sidebar-open? boolean?)


(s/def ::visible? boolean?)


(s/def ::nav-path any?)


(s/def ::nav-query-params any?)


(s/def ::changes-protection? boolean?)


(s/def ::ignore-changes-modal (s/nilable any?))


(s/def ::bootstrap-message (s/nilable keyword?))


(s/def ::db (s/keys :req [::iframe?
                          ::device
                          ::sidebar-open?
                          ::visible?
                          ::nav-path
                          ::nav-query-params
                          ::changes-protection?
                          ::ignore-changes-modal
                          ::bootstrap-message]))


(def defaults {::iframe?              false
               ::device               :computer
               ::sidebar-open?        false
               ::visible?             true
               ::nav-path             ["welcome"]
               ::nav-query-params     {}
               ::changes-protection?  false
               ::ignore-changes-modal nil
               ::bootstrap-message    nil})
