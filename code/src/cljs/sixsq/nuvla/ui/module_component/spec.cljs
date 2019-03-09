(ns sixsq.nuvla.ui.module-component.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]))

(def defaults {::page-changed?  false
               ::name ""
               ::description ""
               ::default-logo-url "https://semantic-ui.com/images/wireframe/image.png"
               ::port-mappings {1 {}}                       ; create an initial entry for new components
               ::save-modal-visible? false
               ;::volumes {}
               ;::env-vars {}
               })

(s/def ::page-changed? boolean?)

(s/def ::name (s/nilable string?))

(s/def ::logo-url (s/nilable string?))
(s/def ::default-logo-url (s/nilable string?))
(s/def ::logo-url-modal-visible? boolean?)

(s/def ::save-modal-visible? boolean?)
(s/def ::commit-message (s/nilable string?))

(s/def ::port-mappings any?)
