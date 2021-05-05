(ns sixsq.nuvla.ui.apps-component.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.utils.spec :as spec-utils]))


; create an initial entry for new components
(def defaults {::module-component {::image         {}
                                   ::ports         (sorted-map)
                                   ::mounts        (sorted-map)
                                   ::architectures ["amd64"]}})


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


(s/def ::architectures (s/coll-of string? :min-count 1))


; Module

(s/def ::module-component (s/keys :req [::image
                                        ::architectures]
                                  :opt [::ports
                                        ::mounts
                                        ::data-types]))
