 (ns sixsq.nuvla.ui.pages.mec.spec
   (:require [clojure.spec.alpha :as s]
             [sixsq.nuvla.ui.common-components.plugins.nav-tab :as tab-plugin]))

 (s/def ::mepms any?)
 (s/def ::subscriptions any?)
 (s/def ::selected-mepm-id (s/nilable string?))
 (s/def ::selected-mepm any?)
 (s/def ::loading-mepms? boolean?)
 (s/def ::loading-subscriptions? boolean?)
 (s/def ::loading-selected-mepm? boolean?)

 (def defaults
   {::tab                   (tab-plugin/build-spec :default-tab :mepms)
    ::mepms                 []
    ::subscriptions         []
    ::selected-mepm-id      nil
    ::selected-mepm         nil
    ::loading-mepms?        false
    ::loading-subscriptions? false
    ::loading-selected-mepm? false})
