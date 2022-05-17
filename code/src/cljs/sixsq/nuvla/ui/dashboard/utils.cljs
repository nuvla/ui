(ns sixsq.nuvla.ui.dashboard.utils
  (:require
    [sixsq.nuvla.ui.apps-store.events :as apps-store-events]))

(def type-apps "APPS")
(def type-deployments "DEPLOYMENTS")
(def type-nbs "NUVLAEDGES")
(def type-creds "CREDENTIALS")

(def target-apps {:resource  "apps"
                  :tab-key   :appstore
                  :tab-event ::apps-store-events/set-active-tab})
(def target-deployments {:resource  "apps"
                         :tab-key   :deployments
                         :tab-event ::apps-store-events/set-active-tab})
(def target-navigator {:resource  "apps"
                       :tab-key   :navigate
                       :tab-event ::apps-store-events/set-active-tab})
(def target-nbs {:resource "edges"})
(def target-creds {:resource "credentials"})

(defn type->icon
  [type]
  (let [icons-map {type-apps        "fas fa-store"
                   type-deployments "fas fa-rocket"
                   type-nbs         "box"
                   type-creds       "key"}]
    (get icons-map type)))
