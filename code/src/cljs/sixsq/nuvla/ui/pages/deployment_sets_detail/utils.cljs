(ns sixsq.nuvla.ui.pages.deployment-sets-detail.utils
  (:require [clojure.data :as data]
            [cljs.pprint :as pprint]
            [sixsq.nuvla.ui.common-components.plugins.module :refer [get-version-id]]
            [sixsq.nuvla.ui.pages.apps.apps-applications-sets.spec :as apps-set-spec]
            [sixsq.nuvla.ui.pages.apps.apps-store.spec :as apps-store-spec]
            [sixsq.nuvla.ui.pages.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.pages.deployment-sets-detail.spec :as spec]))

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

(def editable-keys [:name :description :applications-sets :auto-update :auto-update-interval])

(defn unsaved-changes?
  [deployment-set deployment-set-edited]
  (and (some? deployment-set-edited)
       (not= (select-keys deployment-set-edited editable-keys)
             (select-keys deployment-set editable-keys))))

(defn server-side-changes
  [deployment-set deployment-set-from-server]
  (let [a (select-keys deployment-set-from-server editable-keys)
        b (select-keys deployment-set editable-keys)]
    (when (not= a b)
      (vec (take 2 (data/diff a b))))))

(defn pprint-server-side-changes-str
  [[after before]]
  (str (with-out-str (pprint/pprint before))
       "=>\n"
       (with-out-str (pprint/pprint after))))

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
       (not= apps-store-spec/virtual-apps-set-parent-path (:parent-path apps-set))))

(defn module->dg-subtype
  [{:keys [subtype compatibility content]}]
  (cond
    (and (= apps-utils/subtype-application subtype)
         (= apps-utils/compatibility-docker-compose compatibility))
    spec/subtype-docker-compose

    (and (= apps-utils/subtype-application subtype)
         (= apps-utils/compatibility-swarm compatibility))
    spec/subtype-docker-swarm

    (#{apps-utils/subtype-application-k8s apps-utils/subtype-application-helm} subtype)
    spec/subtype-kubernetes

    (= apps-utils/subtype-applications-sets subtype)
    (let [apps-set-subtype (-> content :applications-sets first :subtype)]
      (condp = apps-set-subtype
        apps-set-spec/app-set-docker-subtype
        spec/subtype-docker-swarm

        apps-set-spec/app-set-k8s-subtype
        spec/subtype-kubernetes))))

