(ns sixsq.slipstream.webui.legacy-application.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::completed? boolean?)

(s/def ::module-id (s/nilable string?))

(s/def ::module any?)

(s/def ::db (s/keys :req [::completed? ::module-id ::module]))

(def defaults {::completed? true
               ::module-id nil
               ::module nil})
