(ns sixsq.nuvla.ui.apps-component.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.apps.spec :as apps-spec]))

; create an initial entry for new components
(def defaults {::port-mappings     {1 {:source      ""
                                       :destination ""
                                       :port-type   "TCP"}}
               ::volumes           {1 {:type        "volume"
                                       :source      ""
                                       :destination ""
                                       :driver      "local"
                                       :read-only?  false}}
               ::urls              {1 {:id 1}}
               ::output-parameters {1 {}}
               ::architecture      "x86"
               ::data-types        {1 ""}})

(s/def ::docker-image #(and (not (empty? %)) string? %))

(s/def ::port-mappings any?)

(s/def ::volumes any?)

(s/def ::content (s/merge ::docker-image
                          ::port-mappings
                          ::volumes))

(s/def ::module-component (s/merge ::apps-spec/summary
                                   ::content))

(s/def ::input-value #(and (not (empty? %)) string? %))

(s/def ::urls any?)

(s/def ::output-parameters any?)

(s/def ::architecture string?)

(s/def ::data-type string?)
