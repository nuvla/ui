(ns sixsq.nuvla.ui.main.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.config :as config]
            [sixsq.nuvla.ui.routing.routes :as routes]))

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
(s/def ::after-clear-event (s/nilable any?))

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
               ::pages                {"welcome"         {:key        routes/home
                                                          :label-kw   :home
                                                          :icon       "fa-light fa-house"
                                                          :protected? false
                                                          :order      0}
                                       "documentation"   {:key        routes/documentation
                                                          :label-kw   :api-doc
                                                          :icon       "info"
                                                          :protected? false}
                                       "dashboard"       {:key        routes/dashboard
                                                          :label-kw   :dashboard
                                                          :icon       "fa-light fa-gauge-min"
                                                          :protected? true
                                                          :order      10}
                                       "apps"            {:key        routes/apps
                                                          :label-kw   :apps
                                                          :icon       "fa-light fa-layer-group"
                                                          :protected? true
                                                          :order      20}
                                       "deployments"     {:key        routes/deployments
                                                          :label-kw   :deployments
                                                          :name       "deployments"
                                                          :icon       "fa-light fa-rocket-launch"
                                                          :protected? true
                                                          :order      30}
                                       "deployment-sets" {:key        routes/deployment-sets
                                                          :label-kw   :deployment-sets
                                                          :name       "deployments-sets"
                                                          :icon       "fa-light fa-bullseye"
                                                          :protected? true
                                                          :hidden?    (not config/debug?)
                                                          :order      31}
                                       "edges"           {:key        routes/edges
                                                          :label-kw   :edges
                                                          :name       "edges"
                                                          :icon       "fa-light fa-box"
                                                          :protected? true
                                                          :order      40}
                                       "credentials"     {:key        routes/credentials
                                                          :label-kw   :credentials
                                                          :icon       "fa-light fa-key"
                                                          :protected? true
                                                          :order      50}
                                       "notifications"   {:key        routes/notifications
                                                          :label-kw   :notifications
                                                          :icon       "fa-light fa-bell"
                                                          :protected? true
                                                          :order      60}
                                       "data"            {:key             routes/data
                                                          :label-kw        :data
                                                          :icon            "fa-light fa-database"
                                                          :protected?      true
                                                          :iframe-visible? true
                                                          :order           70}
                                       "clouds"          {:key        routes/clouds
                                                          :label-kw   :infra-service-short
                                                          :icon       "fa-light fa-cloud"
                                                          :protected? true
                                                          :order      80}
                                       "api"             {:key        routes/api
                                                          :label-kw   :api
                                                          :icon       "fa-light fa-code"
                                                          :protected? false
                                                          :order      90}}
               ::open-modal           nil
               ::stripe               nil})
