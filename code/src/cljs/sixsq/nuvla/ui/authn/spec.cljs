(ns sixsq.nuvla.ui.authn.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.config :as config]))


(s/def ::open-modal (s/nilable keyword?))

(s/def ::session (s/nilable any?))

(s/def ::error-message (s/nilable string?))

(s/def ::success-message (s/nilable string?))

(s/def ::redirect-uri (s/nilable string?))

(s/def ::server-redirect-uri string?)

(s/def ::loading? boolean?)


(s/def ::db (s/keys :req [::open-modal
                          ::session
                          ::error-message
                          ::success-message
                          ::redirect-uri
                          ::server-redirect-uri
                          ::loading?]))


(def defaults
  {::open-modal            nil
   ::session               nil
   ::error-message         nil
   ::success-message       nil
   ::redirect-uri          nil
   ::server-redirect-uri   (str @config/path-prefix "/sign-in")
   ::loading?              false})
