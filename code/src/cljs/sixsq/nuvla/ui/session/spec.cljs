(ns sixsq.nuvla.ui.session.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.config :as config]))

(s/def ::session-loading? boolean?)

(s/def ::open-modal (s/nilable keyword?))

(s/def ::session (s/nilable any?))

(s/def ::error-message (s/nilable string?))

(s/def ::success-message (s/nilable keyword?))

(s/def ::redirect-uri (s/nilable string?))

(s/def ::server-redirect-uri string?)

(s/def ::loading? boolean?)

(s/def ::peers any?)

(s/def ::groups any?)

(s/def ::callback-2fa (s/nilable string?))


(s/def ::db (s/keys :req [::session-loading?
                          ::open-modal
                          ::session
                          ::error-message
                          ::success-message
                          ::redirect-uri
                          ::server-redirect-uri
                          ::loading?
                          ::peers
                          ::groups
                          ::callback-2fa]))


(def defaults
  {::session-loading?    true
   ::open-modal          nil
   ::session             nil
   ::error-message       nil
   ::success-message     nil
   ::redirect-uri        nil
   ::server-redirect-uri (str @config/path-prefix "/sign-in")
   ::loading?            false
   ::peers               nil
   ::groups              nil
   ::callback-2fa        nil})
