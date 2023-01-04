(ns sixsq.nuvla.ui.main.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.config :as config]))

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

(s/def ::do-not-ignore-changes-modal (s/nilable any?))

(s/def ::ui-version map?)

(s/def ::actions-interval map?)

(s/def ::content-key string?)

(s/def ::pages map?)

; from ui config file
(s/def ::config any?)

(s/def ::open-modal (s/nilable keyword?))

(s/def ::stripe any?)

(def defaults {::loading?             true
               ::not-found?           false
               ::iframe?              false
               ::config               {}
               ::device               :computer
               ::sidebar-open?        true
               ::visible?             true
               ::nav-path             ["welcome"]
               ::nav-query-params     {}
               ::changes-protection?  false
               ::ignore-changes-modal nil
               ::ui-version           {:current-version nil
                                       :new-version     nil
                                       :modal-open?     false
                                       :notify?         true}
               ::actions-interval     {}
               ::content-key          (random-uuid)
               ::pages                {"welcome"         {:url        "/ui/welcome"
                                                          :label-kw   :home
                                                          :icon       "home"
                                                          :protected? false
                                                          :order      0}
                                       "documentation"   {:url        "/ui/documentation"
                                                          :label-kw   :api-doc
                                                          :icon       "info"
                                                          :protected? false}
                                       "dashboard"       {:url        "/ui/dashboard"
                                                          :label-kw   :dashboard
                                                          :icon       "dashboard"
                                                          :protected? true
                                                          :order      10}
                                       "apps"            {:url        "/ui/apps"
                                                          :label-kw   :apps
                                                          :icon       "fas fa-store"
                                                          :protected? true
                                                          :order      20}
                                       "deployments"     {:url        "/ui/deployments"
                                                          :label-kw   :deployments
                                                          :name       "deployments"
                                                          :icon       "rocket"
                                                          :protected? true
                                                          :order      30}
                                       "deployment-sets" {:url        "/ui/deployment-sets"
                                                          :label-kw   :deployment-sets
                                                          :name       "deployments-sets"
                                                          :icon       "bullseye"
                                                          :protected? true
                                                          :hidden?    (not config/debug?)
                                                          :order      31}
                                       "edges"           {:url        "/ui/edges"
                                                          :label-kw   :edges
                                                          :name       "edges"
                                                          :icon       "box"
                                                          :protected? true
                                                          :order      40}
                                       "credentials"     {:url        "/ui/credentials"
                                                          :label-kw   :credentials
                                                          :icon       "key"
                                                          :protected? true
                                                          :order      50}
                                       "notifications"   {:url        "/ui/notifications"
                                                          :label-kw   :notifications
                                                          :icon       "bell"
                                                          :protected? true
                                                          :order      60}
                                       "data"            {:url             "/ui/data"
                                                          :label-kw        :data
                                                          :icon            "database"
                                                          :protected?      true
                                                          :iframe-visible? true
                                                          :order           70}
                                       "clouds"          {:url        "/ui/clouds"
                                                          :label-kw   :infra-service-short
                                                          :icon       "cloud"
                                                          :protected? true
                                                          :order      80}
                                       "api"             {:url        "/ui/api"
                                                          :label-kw   :api
                                                          :icon       "code"
                                                          :protected? false
                                                          :order      90}
                                       }
               ::open-modal           nil
               ::stripe               nil})
