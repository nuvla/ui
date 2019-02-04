(ns sixsq.slipstream.webui.nuvlabox-detail.spec
  (:require-macros [sixsq.slipstream.webui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::loading? boolean?)
(s/def ::mac (s/nilable string?))
(s/def ::state (s/nilable any?))
(s/def ::record (s/nilable any?))


(s/def ::db (s/keys :req [::loading?
                          ::mac
                          ::state
                          ::record]))


(def defaults {::loading? false
               ::mac      nil
               ::state    nil
               ::record   nil})
