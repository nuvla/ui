(ns sixsq.nuvla.ui.apps-component.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.utils.spec :as spec-utils]
            [taoensso.timbre :as log]))


; create an initial entry for new components
(def defaults {::module-component {::image             {}
                                   ::ports             {}
                                   ::mounts            {}
                                   ::urls              {}
                                   ::output-parameters {}
                                   ::architectures     ["amd64"]
                                   ::data-types        {}}})


; Image

(s/def ::image-name spec-utils/nonblank-string)
(s/def ::repository (s/nilable string?))
(s/def ::registry (s/nilable string?))
(s/def ::tag (s/nilable string?))

(s/def ::image (s/keys :req [::image-name]
                       :opt [::registry
                             ::tag
                             ::repository]))


; Ports

(s/def ::target-port int?)
(s/def ::published-port (s/nilable int?))
(s/def ::protocol (s/nilable string?))

(s/def ::port (s/keys :req [::target-port]
                      :opt [::protocol
                            ::published-port]))

(s/def ::ports (s/map-of any? (s/merge ::port)))


; Environmental-variables

(def env-var-regex #"^[A-Z_]+$")
(def reserved-env-var-regex #"NUVLA_.*")
(s/def ::env-name (s/and spec-utils/nonblank-string
                         #(re-matches env-var-regex %)
                         #(not (re-matches reserved-env-var-regex %))))

(s/def ::env-description (s/nilable spec-utils/nonblank-string))

(s/def ::env-value (s/nilable spec-utils/nonblank-string))

(s/def ::env-required boolean?)


(s/def ::env-variable
  (s/keys :req [::env-name]
          :opt [::env-description ::env-required ::env-value]))

(s/def ::env-variables (s/map-of any? (s/merge ::env-variable)))


; Volumes (mounts)

(s/def ::mount-source spec-utils/nonblank-string)

(s/def ::mount-target spec-utils/nonblank-string)

(s/def ::mount-read-only boolean?)

(s/def ::mount-type #{"bind" "volume"})

(s/def ::mount (s/keys :req [::mount-type
                             ::mount-target]
                       :opt [::mount-source
                             ::mount-read-only
                             ;::volume-options
                             ]))

(s/def ::mounts (s/map-of any? (s/merge ::mount)))


(s/def ::input-value spec-utils/nonblank-string)


; URLs

(s/def ::url-name spec-utils/nonblank-string)

(s/def ::url spec-utils/nonblank-string)

(s/def ::single-url (s/keys :req [::url-name
                                  ::url]))

(s/def ::urls (s/map-of any? (s/merge ::single-url)))


; Output parameters

(s/def ::output-parameter-name spec-utils/nonblank-string)

(s/def ::output-parameter-description spec-utils/nonblank-string)

(s/def ::output-parameter (s/keys :req [::output-parameter-name
                                        ::output-parameter-description]))

(s/def ::output-parameters (s/map-of any? (s/merge ::output-parameter)))


(s/def ::architectures (s/coll-of string? :min-count 1))

(s/def ::data-type string?)


; Module

(s/def ::module-component (s/keys :req [::image
                                        ::architectures]
                                  :opt [::ports
                                        ::mounts
                                        ::output-parameters
                                        ::urls
                                        ::data-types]))
