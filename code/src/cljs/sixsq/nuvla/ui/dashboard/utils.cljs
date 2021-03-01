(ns sixsq.nuvla.ui.dashboard.utils
  (:require
    [clojure.string :as str]
    [sixsq.nuvla.ui.apps-store.events :as apps-store-events]
    [sixsq.nuvla.ui.utils.time :as time]))

(def type-apps "APPS")
(def type-deployments "DEPLOYMENTS")
(def type-nbs "NUVLABOXES")
(def type-creds "CREDENTIALS")

(def target-apps {:resource "apps", :tab-index 1, :tab-index-event ::apps-store-events/set-active-tab-index})
(def target-deployments {:resource "apps", :tab-index 2, :tab-index-event ::apps-store-events/set-active-tab-index})
(def target-nbs {:resource "edge"})
(def target-creds {:resource "credentials"})

(defn type->icon
  [type]
  (let [icons-map {type-apps        "fas fa-store"
                   type-deployments "fas fa-rocket"
                   type-nbs         "box"
                   type-creds       "key"}]
    (get icons-map type)))
