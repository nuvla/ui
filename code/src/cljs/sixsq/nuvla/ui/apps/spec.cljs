(ns sixsq.nuvla.ui.apps.spec
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::version-warning? boolean?)

(s/def ::is-new? boolean?)

(s/def ::completed? boolean?)

(s/def ::module-path (s/nilable string?))

(s/def ::module any?)

(s/def ::default-logo-url (s/nilable string?))

(s/def ::logo-url-modal-visible? boolean?)

(s/def ::save-modal-visible? boolean?)

(s/def ::add-modal-visible? boolean?)

(s/def ::page-changed? boolean?)

(s/def ::commit-message (s/nilable string?))

(s/def ::db (s/keys :req [::version-warning?
                          ::is-new?
                          ::completed?
                          ::module-path
                          ::module
                          ::add-modal-visible?
                          ::page-changed?
                          ::default-logo-url
                          ::logo-url-modal-visible?
                          ::save-modal-visible?
                          ::commit-message]))

(def defaults {::version-warning?        false
               ::is-new?                 false
               ::completed?              true
               ::module-path             nil
               ::module                  {}
               ::add-modal-visible?      false
               ::page-changed?           false
               ::logo-url-modal-visible? false
               ::save-modal-visible?     false
               ::default-logo-url        "/ui/images/noimage.png"
               ::commit-message          nil})
