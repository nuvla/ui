(ns sixsq.nuvla.ui.apps-component.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.apps.spec :as apps-spec]))

; create an initial entry for new components
(def defaults {::ports             {}
               ::mounts            {}
               ::urls              {}
               ::output-parameters {}
               ::architecture      "x86"
               ::data-types        {}})

; Image

(s/def ::image-name string?)
(s/def ::repository string?)
(s/def ::registry string?)
(s/def ::tag string?)

(s/def ::image (s/keys :req-un [::image-name]
                       :opt-un [::registry
                                ::tag
                                ::repository]))

; Ports

(s/def ::target-port int?)
(s/def ::published-port (s/nilable int?))
(s/def ::protocol (s/nilable string?))

(s/def ::port (s/keys :req-un [::target-port]
                      :opt-un [::protocol
                               ::published-port]))

(s/def ::ports any?)

; Volumes (mounts)

(s/def ::mounts any?)

(s/def ::content (s/merge ::docker-image
                          ::ports
                          ::mounts))

(s/def ::module-component (s/merge ::apps-spec/summary
                                   ::content))

(s/def ::input-value #(and (not (empty? %)) string? %))

(s/def ::urls any?)

(s/def ::output-parameters any?)

(s/def ::architecture string?)

(s/def ::data-type string?)
