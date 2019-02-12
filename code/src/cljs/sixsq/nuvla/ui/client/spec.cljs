(ns sixsq.nuvla.ui.client.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::client any?)

(s/def ::nuvla-url string?)

(s/def ::db (s/keys :req [::client
                          ::nuvla-url]))

(def defaults {::client    nil
               ::nuvla-url ""})
