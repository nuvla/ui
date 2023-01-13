(ns sixsq.nuvla.ui.session.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.config :as config]))

(s/def ::session-loading? boolean?)

(s/def ::session (s/nilable any?))

(s/def ::error-message (s/nilable string?))

(s/def ::success-message (s/nilable keyword?))

(s/def ::redirect-uri (s/nilable string?))

(s/def ::server-redirect-uri string?)

(s/def ::loading? boolean?)

(s/def ::peers any?)

(s/def ::groups any?)

(s/def ::groups-hierarchies coll?)

(s/def ::callback-2fa (s/nilable string?))

(def defaults
  {::session-loading?    true
   ::session             nil
   ::error-message       nil
   ::success-message     nil
   ::redirect-uri        nil
   ::server-redirect-uri (str @config/path-prefix "/sign-in")
   ::loading?            false
   ::peers               nil
   ::groups              nil
   ::groups-hierarchies  []
   ::callback-2fa        nil})
