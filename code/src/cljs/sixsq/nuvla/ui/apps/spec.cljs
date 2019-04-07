(ns sixsq.nuvla.ui.apps.spec
  (:require
    [clojure.spec.alpha :as s]))


;; Module

(s/def ::name #(and (not (empty? %)) string? %))

(s/def ::description (s/nilable string?))

(s/def ::module-path (s/nilable string?))

(s/def ::logo-url (s/nilable string?))

(s/def ::module any?)

(s/def ::summary (s/merge ::name
                          ::description
                          ::parent-path
                          ::module-path
                          ::logo-url))

;; Page

(s/def ::default-logo-url (s/nilable string?))

(s/def ::logo-url-modal-visible? boolean?)

(s/def ::active-input (s/nilable string?))

(s/def ::version-warning? boolean?)

(s/def ::form-valid? boolean?)

(s/def ::is-new? boolean?)

(s/def ::completed? boolean?)

(s/def ::save-modal-visible? boolean?)

(s/def ::add-modal-visible? boolean?)

(s/def ::commit-message (s/nilable string?))

(s/def ::db (s/keys :req [::active-input
                          ::version-warning?
                          ::form-valid?
                          ::is-new?
                          ::completed?
                          ::module-path
                          ::module
                          ::add-modal-visible?
                          ::default-logo-url
                          ::logo-url-modal-visible?
                          ::save-modal-visible?
                          ::commit-message]))

(def defaults {::active-input                    nil
               ::version-warning?                false
               ::form-valid?                     true
               ::is-new?                         false
               ::completed?                      true
               ::module-path                     nil
               ::module                          {}
               ::add-modal-visible?              false
               ::logo-url-modal-visible?         false
               ::save-modal-visible?             false
               ::default-logo-url                "/ui/images/noimage.png"
               ::commit-message                  ""})
