(ns sixsq.nuvla.ui.main.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.pages.about.utils :as about-utils]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.utils.icons :as icons]))

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

;; reset-changes-event should be set when changes protection is set to true
;; reset-changes-event will be dispatched after the user confirms to ignore changes
(s/def ::reset-changes-event (s/nilable any?))

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
                                                          :icon       icons/i-house
                                                          :order      0}
                                       "documentation"   {:key        routes/documentation
                                                          :label-kw   :api-doc
                                                          :icon       "info"}
                                       "dashboard"       {:key        routes/dashboard
                                                          :label-kw   :dashboard
                                                          :icon       icons/i-gauge
                                                          :order      10}
                                       "apps"            {:key        routes/apps
                                                          :label-kw   :apps
                                                          :icon       icons/i-layer-group
                                                          :order      20}
                                       "deployments"     {:key        routes/deployments
                                                          :route-names #{routes/deployments
                                                                         routes/deployment-groups-details
                                                                         routes/deployment-groups}
                                                          :label-kw :deployments
                                                          :name "deployments"
                                                          :icon icons/i-rocket
                                                          :order 30}
                                       "edges"           {:key        routes/edges
                                                          :label-kw   :edges
                                                          :name       "edges"
                                                          :icon       icons/i-box
                                                          :order      40}
                                       "credentials"     {:key        routes/credentials
                                                          :label-kw   :credentials
                                                          :icon       icons/i-key
                                                          :order      50}
                                       "notifications"   {:key        routes/notifications
                                                          :label-kw   :notifications
                                                          :icon       icons/i-bell
                                                          :order      60}
                                       "data"            {:key             routes/data
                                                          :label-kw        :data
                                                          :icon            icons/i-db
                                                          :iframe-visible? true
                                                          :order           70}
                                       "clouds"          {:key        routes/clouds
                                                          :label-kw   :infra-service-short
                                                          :icon       icons/i-cloud
                                                          :order      80}
                                       "api"             {:key        routes/api
                                                          :label-kw   :api
                                                          :icon       icons/i-code
                                                          :order      90}
                                       "ui-demo"         {:key             routes/ui-demo
                                                          :label-kw        :ui-demo
                                                          :icon            icons/i-world
                                                          :feature-flag-kw about-utils/feature-internal-ui-demo-page
                                                          :order           100}}
               ::open-modal           nil
               ::stripe               nil})
