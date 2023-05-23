(ns sixsq.nuvla.ui.dashboard.utils
  (:require [sixsq.nuvla.ui.utils.icons :as icons]))

(def type-apps "APPS")
(def type-deployments "DEPLOYMENTS")
(def type-nbs "NUVLAEDGES")
(def type-creds "CREDENTIALS")

(def target-apps {:resource "apps"})
(def target-deployments {:resource "deployments"})
(def target-nbs {:resource "edges"})
(def target-creds {:resource "credentials"})

(defn type->icon
  [type]
  (let [icons-map {type-apps        icons/layer-group
                   type-deployments icons/rocket
                   type-nbs         "fa-light fa-box"
                   type-creds       "fa-light fa-key"}]
    (get icons-map type)))
