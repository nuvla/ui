(ns sixsq.slipstream.webui.client.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::client any?)

(s/def ::slipstream-url string?)

(s/def ::db (s/keys :req [::client
                          ::slipstream-url]))

(def defaults {::client nil
               ::slipstream-url ""})
