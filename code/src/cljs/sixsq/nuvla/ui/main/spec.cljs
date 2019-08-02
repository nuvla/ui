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


(s/def ::welcome-message (s/nilable keyword?))


(s/def ::actions-interval map?)


(s/def ::content-key string?)


(s/def ::pages map?)


(s/def ::db (s/keys :req [::iframe?
                          ::device
                          ::sidebar-open?
                          ::visible?
                          ::nav-path
                          ::nav-query-params
                          ::changes-protection?
                          ::ignore-changes-modal
                          ::bootstrap-message
                          ::welcome-message
                          ::actions-interval
                          ::content-key]))


(def defaults {::iframe?              false
               ::device               :computer
               ::sidebar-open?        false
               ::visible?             true
               ::nav-path             []
               ::nav-query-params     {}
               ::changes-protection?  false
               ::ignore-changes-modal nil
               ::bootstrap-message    nil
               ::welcome-message      nil
               ::actions-interval     {}
               ::content-key          (random-uuid)
               ::pages                {"welcome"         {:url        "welcome"
                                                          :protected? false}
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
                                                          :icon       "play"
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
                                                          :label-kw   :edge
                                                          :icon       "box"
                                                          :protected? true
                                                          :order      6}
                                       "api"             {:url        "api"
                                                          :label-kw   :api
                                                          :icon       "code"
                                                          :protected? false
                                                          :order      7}}})
