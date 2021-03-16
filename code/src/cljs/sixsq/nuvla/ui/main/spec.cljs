(ns sixsq.nuvla.ui.main.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::iframe? boolean?)


(s/def ::device #{:mobile :tablet :computer :large-screen :wide-screen})


(s/def ::sidebar-open? boolean?)


(s/def ::visible? boolean?)


(s/def ::nav-path any?)


(s/def ::nav-query-params any?)


(s/def ::changes-protection? boolean?)


(s/def ::ignore-changes-modal (s/nilable any?))


(s/def ::bootstrap-message (s/nilable keyword?))


(s/def ::message any?)


(s/def ::actions-interval map?)


(s/def ::content-key string?)


(s/def ::pages map?)

; from ui config file
(s/def ::config any?)

(s/def ::open-modal (s/nilable keyword?))

(s/def ::theme-hostname any?)
(s/def ::theme-hostname-ready? boolean?)

;(s/def ::theme-session any?)
;(s/def ::theme-session-ready? boolean?)

(s/def ::theme-ready? boolean?)

(s/def ::theme any?)

(s/def ::stripe any?)

(s/def ::db (s/keys :req [::iframe?
                          ::config
                          ::theme-hostname
                          ::theme-hostname-ready?
                          ;::theme-session
                          ;::theme-session-ready?
                          ::theme
                          ::theme-ready?
                          ::device
                          ::sidebar-open?
                          ::visible?
                          ::nav-path
                          ::nav-query-params
                          ::changes-protection?
                          ::ignore-changes-modal
                          ::bootstrap-message
                          ::message
                          ::actions-interval
                          ::content-key
                          ::open-modal
                          ::stripe]))


(def defaults {::iframe?               false
               ::config                {}
               ::theme                 nil
               ::theme-hostname        nil
               ::theme-hostname-ready? false
               ;::theme-session         nil
               ;::theme-session-ready?  false
               ::theme-ready?          false
               ::theme-root            nil
               ::device                :computer
               ::sidebar-open?         true
               ::visible?              true
               ::nav-path              []
               ::nav-query-params      {}
               ::changes-protection?   false
               ::ignore-changes-modal  nil
               ::bootstrap-message     nil
               ::message               nil
               ::actions-interval      {}
               ::content-key           (random-uuid)
               ::pages                 {"welcome"         {:url        "welcome"
                                                           :label-kw   :home
                                                           :icon       "home"
                                                           :protected? false
                                                           :order      0}
                                        "documentation"   {:url        "documentation"
                                                           :label-kw   :api-doc
                                                           :icon       "info"
                                                           :protected? false}
                                        "dashboard"       {:url        "dashboard"
                                                           :label-kw   :dashboard
                                                           :icon       "dashboard"
                                                           :protected? true
                                                           :order      1}
                                        "apps"            {:url        "apps"
                                                           :label-kw   :apps
                                                           :icon       "fas fa-store"
                                                           :protected? true
                                                           :order      2}
                                        "data"            {:url             "data"
                                                           :label-kw        :data
                                                           :icon            "database"
                                                           :protected?      true
                                                           :iframe-visible? true
                                                           :order           3}
                                        "infrastructures" {:url        "infrastructures"
                                                           :label-kw   :infra-service-short
                                                           :icon       "cloud"
                                                           :protected? true
                                                           :order      4}
                                        "credentials"     {:url        "credentials"
                                                           :label-kw   :credentials
                                                           :icon       "key"
                                                           :protected? true
                                                           :order      5}
                                        "edge"            {:url        "edge"
                                                           :label-kw   :nuvlabox
                                                           :name       "NuvlaBox"
                                                           :icon       "box"
                                                           :protected? true
                                                           :order      6}
                                        "notifications"   {:url        "notifications"
                                                           :label-kw   :notifications
                                                           :icon       "bell"
                                                           :protected? false
                                                           :order      7}
                                        "api"             {:url        "api"
                                                           :label-kw   :api
                                                           :icon       "code"
                                                           :protected? false
                                                           :order      8}
                                        }
               ::open-modal            nil
               ::stripe                nil})
