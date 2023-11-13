(ns sixsq.nuvla.ui.deployment-sets-detail.utils
  (:require [sixsq.nuvla.ui.apps-store.spec :as spec]
            [sixsq.nuvla.ui.plugins.module :refer [get-version-id]]))

(def state-new "NEW")
(def state-starting "STARTING")
(def state-started "STARTED")
(def state-stopping "STOPPING")
(def state-stopped "STOPPED")
(def state-partially-started "PARTIALLY-STARTED")
(def state-partially-updated "PARTIALLY-UPDATED")
(def state-partially-stopped "PARTIALLY-STOPPED")
(def state-updating "UPDATING")
(def state-updated "UPDATED")

(defn action-in-progress?
  [{:keys [state] :as _deployment-set}]
  (#{state-starting state-stopping state-updating} state))

(def editable-keys [:name :description :applications-sets])

(defn unsaved-changes?
  [deployment-set deployment-set-edited]
  (and (some? deployment-set-edited)
       (not= (select-keys deployment-set-edited editable-keys)
             (select-keys deployment-set editable-keys))))

(defn enrich-app
  [app]
  (let [versions   (:versions app)
        version-id (-> app :content :id)
        version-no (get-version-id (map-indexed vector versions) version-id)]
    (assoc app :version version-no)))

(defn app-version-from-apps-set
  [apps-set app-id]
  (->> (get-in apps-set
               [:content
                :applications-sets
                0
                :applications])
       (filter #(= app-id (:id %)))
       first
       :version))

(defn is-controlled-by-apps-set
  [apps-set]
  (and apps-set
       (not= spec/virtual-apps-set-parent-path (:parent-path apps-set))))
