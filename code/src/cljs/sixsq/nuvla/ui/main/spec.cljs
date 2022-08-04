(ns sixsq.nuvla.ui.main.spec
  (:require
    [clojure.spec.alpha :as s]))

(s/def ::loading? boolean?)

(s/def ::not-found? boolean?)

(s/def ::iframe? boolean?)

(s/def ::device #{:mobile :tablet :computer :large-screen :wide-screen})

(s/def ::sidebar-open? boolean?)

(s/def ::visible? boolean?)

(s/def ::nav-path any?)

(s/def ::nav-query-params any?)

(s/def ::changes-protection? boolean?)

(s/def ::ignore-changes-modal (s/nilable any?))

(s/def ::ui-version map?)

(s/def ::bootstrap-message (s/nilable keyword?))

(s/def ::message any?)

(s/def ::actions-interval map?)

(s/def ::content-key string?)

(s/def ::pages map?)

; from ui config file
(s/def ::config any?)

(s/def ::open-modal (s/nilable keyword?))

(s/def ::stripe any?)

(s/def ::db (s/keys :req [::loading?
                          ::not-found?
                          ::iframe?
                          ::device
                          ::sidebar-open?
                          ::visible?
                          ::nav-path
                          ::nav-query-params
                          ::changes-protection?
                          ::ignore-changes-modal
                          ::ui-version
                          ::bootstrap-message
                          ::message
                          ::actions-interval
                          ::content-key
                          ::open-modal
                          ::stripe]))


(def defaults {::loading?             true
               ::not-found?           false
               ::iframe?              false
               ::config               {}
               ::device               :computer
               ::sidebar-open?        true
               ::visible?             true
               ::nav-path             []
               ::nav-query-params     {}
               ::changes-protection?  false
               ::ignore-changes-modal nil
               ::ui-version           {:current-version nil
                                       :new-version     nil
                                       :modal-open?     false
                                       :notify?         true}
               ::bootstrap-message    nil
               ::message              nil
               ::actions-interval     {}
               ::content-key          (random-uuid)
               ::pages                {"welcome"       {:url        "welcome"
                                                        :label-kw   :home
                                                        :icon       "home"
                                                        :protected? false
                                                        :order      0}
                                       "documentation" {:url        "documentation"
                                                        :label-kw   :api-doc
                                                        :icon       "info"
                                                        :protected? false}
                                       "dashboard"     {:url        "dashboard"
                                                        :label-kw   :dashboard
                                                        :icon       "dashboard"
                                                        :protected? true
                                                        :order      1}
                                       "apps"          {:url        "apps"
                                                        :label-kw   :apps
                                                        :icon       "fas fa-store"
                                                        :protected? true
                                                        :order      2}
                                       "deployments"   {:url        "deployments"
                                                        :label-kw   :deployments
                                                        :name       "deployments"
                                                        :icon       "rocket"
                                                        :protected? true
                                                        :order      3}
                                       "edges"         {:url        "edges"
                                                        :label-kw   :edges
                                                        :name       "edges"
                                                        :icon       "box"
                                                        :protected? true
                                                        :order      4}
                                       "credentials"   {:url        "credentials"
                                                        :label-kw   :credentials
                                                        :icon       "key"
                                                        :protected? true
                                                        :order      5}
                                       "notifications" {:url        "notifications"
                                                        :label-kw   :notifications
                                                        :icon       "bell"
                                                        :protected? true
                                                        :order      6}
                                       "data"          {:url             "data"
                                                        :label-kw        :data
                                                        :icon            "database"
                                                        :protected?      true
                                                        :iframe-visible? true
                                                        :order           7}
                                       "clouds"        {:url        "clouds"
                                                        :label-kw   :infra-service-short
                                                        :icon       "cloud"
                                                        :protected? true
                                                        :order      8}
                                       "api"           {:url        "api"
                                                        :label-kw   :api
                                                        :icon       "code"
                                                        :protected? false
                                                        :order      9}
                                       }
               ::open-modal           nil
               ::stripe               nil})
