(ns sixsq.nuvla.ui.apps-application.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.utils.spec :as spec-utils]
            [taoensso.timbre :as log]))


; create an initial entry for new application
(def defaults {::module-application {::docker-compose nil}})

; Docker-compose

(s/def ::docker-compose spec-utils/nonblank-string)


; Files

(s/def ::file-content string?)


(s/def ::file-name ::spec-utils/filename)


(s/def ::file (s/keys :req [::file-name ::file-content]))


(s/def ::files (s/map-of any? (s/merge ::file)))


; Module

(s/def ::module-application (s/keys :req [::docker-compose]
                                    :opt [::files]))
