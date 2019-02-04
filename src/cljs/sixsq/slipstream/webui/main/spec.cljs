(ns sixsq.slipstream.webui.main.spec
  (:require-macros [sixsq.slipstream.webui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::iframe? boolean?)


(s/def ::device #{:mobile :tablet :computer :large-screen :wide-screen})


(s/def ::sidebar-open? boolean?)


(s/def ::visible? boolean?)


(s/def ::nav-path any?)


(s/def ::nav-query-params any?)


(s/def ::db (s/keys :req [::iframe?
                          ::device
                          ::sidebar-open?
                          ::visible?
                          ::nav-path
                          ::nav-query-params]))


(def defaults {::iframe?          false
               ::device           :computer
               ::sidebar-open?    false
               ::visible?         true
               ::nav-path         ["welcome"]
               ::nav-query-params {}})
