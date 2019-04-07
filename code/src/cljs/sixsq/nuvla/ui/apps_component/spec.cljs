(ns sixsq.nuvla.ui.apps-component.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.apps.spec :as apps-spec]))

; create an initial entry for new components
(def defaults {::ports             {1 {:published-port 11
                                       :target-port    22
                                       :protocol       "tcp"}
                                    2 {:published-port 33
                                       :target-port    44
                                       :protocol       "udp"}}
               ::mounts            {1 {:mount-type "volume"
                                       :source     "source"
                                       :target     "target"
                                       :driver     "local"
                                       :read-only? false
                                       :volume-options [{:option-key "key1"
                                                         :option-value "val1"}
                                                        {:option-key "key2"
                                                         :option-value "val2"}]}}
               ::urls              {1 {:id 1}}
               ::output-parameters {1 {}}
               ::architecture      "x86"
               ::data-types        {1 ""}})

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
