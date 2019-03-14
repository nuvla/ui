(ns sixsq.nuvla.ui.module-project.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]))

(def defaults {::page-changed?  false
               ::name ""
               ::parent ""
               ::description ""
               ::default-logo-url "https://semantic-ui.com/images/wireframe/image.png"})

(s/def ::page-changed? boolean?)

(s/def ::name (s/nilable string?))
(s/def ::parent (s/nilable string?))
(s/def ::description (s/nilable string?))

(s/def ::logo-url (s/nilable string?))
(s/def ::default-logo-url (s/nilable string?))
(s/def ::logo-url-modal-visible? boolean?)
