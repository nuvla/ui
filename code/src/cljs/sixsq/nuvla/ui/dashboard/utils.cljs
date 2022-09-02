(ns sixsq.nuvla.ui.dashboard.utils)

(def type-apps "APPS")
(def type-deployments "DEPLOYMENTS")
(def type-nbs "NUVLAEDGES")
(def type-creds "CREDENTIALS")

(def target-apps {:resource  "apps"})
(def target-deployments {:resource "deployments"})
(def target-nbs {:resource "edges"})
(def target-creds {:resource "credentials"})

(defn type->icon
  [type]
  (let [icons-map {type-apps        "fas fa-store"
                   type-deployments "fas fa-rocket"
                   type-nbs         "box"
                   type-creds       "key"}]
    (get icons-map type)))
