(ns sixsq.nuvla.ui.dashboard.utils
  (:require
    [sixsq.nuvla.ui.apps-store.events :as apps-store-events]
    [sixsq.nuvla.ui.apps-store.utils :as apps-store-utils]))

(def type-apps "APPS")
(def type-deployments "DEPLOYMENTS")
(def type-nbs "NUVLABOXES")
(def type-creds "CREDENTIALS")

(def target-apps {:resource        "apps"
                  :tab-index       apps-store-utils/tab-app-store
                  :tab-index-event ::apps-store-events/set-active-tab-index})
(def target-deployments {:resource        "apps"
                         :tab-index       apps-store-utils/tab-deployments
                         :tab-index-event ::apps-store-events/set-active-tab-index})
(def target-navigator {:resource        "apps"
                       :tab-index       apps-store-utils/tab-navigator
                       :tab-index-event ::apps-store-events/set-active-tab-index})
(def target-nbs {:resource "edge"})
(def target-creds {:resource "credentials"})

(defn type->icon
  [type]
  (let [icons-map {type-apps        "fas fa-store"
                   type-deployments "fas fa-rocket"
                   type-nbs         "box"
                   type-creds       "key"}]
    (get icons-map type)))
