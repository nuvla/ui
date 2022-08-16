(ns sixsq.nuvla.ui.deployment-fleets-detail.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.deployment-fleets-detail.spec :as spec]
    [sixsq.nuvla.ui.edges-detail.spec :as edges-detail-spec]
    [sixsq.nuvla.ui.edges.utils :as edges-utils]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.time :as time]
    [clojure.string :as str]
    [clojure.set :as set]))


(reg-sub
  ::loading?
  (fn [db]
    (::spec/loading? db)))


(reg-sub
  ::nuvlabox-status
  (fn [db]
    (::edges-detail-spec/nuvlabox-status db)))


(reg-sub
  ::nuvlabox-online-status
  :<- [::nuvlabox-status]
  (fn [{:keys [online]}]
    (edges-utils/status->keyword online)))


(reg-sub
  ::nuvlabox-components
  :<- [::nuvlabox-status]
  (fn [{:keys [components]}]
    components))


(reg-sub
  ::nuvlabox-vulns
  (fn [db]
    (::edges-detail-spec/nuvlabox-vulns db)))


(reg-sub
  ::nuvlabox-associated-ssh-keys
  (fn [db]
    (::edges-detail-spec/nuvlabox-associated-ssh-keys db)))


(reg-sub
  ::nuvlabox-peripherals
  (fn [db]
    (::edges-detail-spec/nuvlabox-peripherals db)))


(reg-sub
  ::nuvlabox-peripherals-ids
  :<- [::nuvlabox-peripherals]
  (fn [nuvlabox-peripherals]
    (keys nuvlabox-peripherals)))


(reg-sub
  ::nuvlabox-peripheral
  :<- [::nuvlabox-peripherals]
  (fn [nuvlabox-peripherals [_ id]]
    (get nuvlabox-peripherals id)))


(reg-sub
  ::deployment-fleet-events
  (fn [db]
    (::spec/deployment-fleet-events db)))

(reg-sub
  ::elements-per-page
  (fn [db]
    (::spec/elements-per-page db)))

(reg-sub
  ::page
  (fn [db]
    (::spec/page db)))

(reg-sub
  ::vuln-severity-selector
  (fn [db]
    (::edges-detail-spec/vuln-severity-selector db)))

(reg-sub
  ::matching-vulns-from-db
  (fn [db]
    (::edges-detail-spec/matching-vulns-from-db db)))

(reg-sub
  ::next-heartbeat-moment
  :<- [::nuvlabox-status]
  (fn [{:keys [next-heartbeat]}]
    (some-> next-heartbeat time/parse-iso8601)))


(reg-sub
  ::deployment-fleet
  (fn [db]
    (::spec/deployment-fleet db)))


(reg-sub
  ::can-decommission?
  :<- [::nuvlabox]
  (fn [nuvlabox _]
    (general-utils/can-operation? "decommission" nuvlabox)))


(reg-sub
  ::can-edit?
  :<- [::deployment-fleet]
  (fn [deployment-fleet _]
    (general-utils/can-edit? deployment-fleet)))


(reg-sub
  ::can-delete?
  :<- [::deployment-fleet]
  (fn [deployment-fleet _]
    (general-utils/can-delete? deployment-fleet)))


(reg-sub
  ::active-tab
  (fn [db]
    (::spec/active-tab db)))


(reg-sub
  ::nuvlabox-managers
  (fn [db]
    (::edges-detail-spec/nuvlabox-managers db)))


(reg-sub
  ::join-token
  (fn [db]
    (::edges-detail-spec/join-token db)))


(reg-sub
  ::nuvlabox-cluster
  (fn [db]
    (::edges-detail-spec/nuvlabox-cluster db)))


(reg-sub
  ::deployment-fleet-not-found?
  (fn [db]
    (::spec/deployment-fleet-not-found? db)))


(reg-sub
  ::nuvlabox-playbooks
  (fn [db]
    (::edges-detail-spec/nuvlabox-playbooks db)))


(reg-sub
  ::infra-services
  (fn [db]
    (::edges-detail-spec/infra-services db)))


(reg-sub
  ::nuvlabox-emergency-playbooks
  (fn [db]
    (::edges-detail-spec/nuvlabox-emergency-playbooks db)))


(reg-sub
  ::nuvlabox-current-playbook
  (fn [db]
    (::edges-detail-spec/nuvlabox-current-playbook db)))

(reg-sub
  ::apps
  (fn [db]
    (::spec/apps db)))

(defn transform
  [tree {:keys [parent-path] :as app}]
  (let [paths (if (str/blank? parent-path)
                [:applications]
                (-> parent-path
                    (str/split "/")
                    (conj :applications)))]
    (update-in tree paths conj app)))


(reg-sub
  ::apps-tree
  :<- [::apps]
  (fn [apps]
    (reduce transform {} apps)))

(reg-sub
  ::apps-fulltext-search
  (fn [db]
    (::spec/apps-fulltext-search db)))

(reg-sub
  ::app-selected?
  (fn [{:keys [::spec/apps-selected]} [_ id]]
    (contains? apps-selected id)))

(reg-sub
  ::creds
  (fn [db]
    (::spec/creds db)))

(reg-sub
  ::creds-fulltext-search
  (fn [db]
    (::spec/creds-fulltext-search db)))


(reg-sub
  ::creds-selected?
  (fn [{:keys [::spec/creds-selected]} [_ ids]]
    (->> creds-selected
         (some (set ids))
         boolean)))
(reg-sub
  ::create-disabled?
  (fn [{:keys [::spec/creds-selected
               ::spec/apps-selected]}]
    (boolean
      (or (empty? apps-selected)
          (empty? creds-selected)))))
