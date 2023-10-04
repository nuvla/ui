(ns sixsq.nuvla.ui.edges-detail.subs
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.edges-detail.spec :as spec]
            [sixsq.nuvla.ui.edges.utils :as edges-utils]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.time :as time]))

(reg-sub
  ::loading?
  (fn [db]
    (::spec/loading? db)))

(reg-sub
  ::nuvlabox-status
  (fn [db]
    (::spec/nuvlabox-status db)))

(reg-sub
  ::next-heartbeat-moment
  :<- [::nuvlabox-status]
  (fn [{:keys [next-heartbeat]}]
    (some-> next-heartbeat time/parse-iso8601)))

(reg-sub
  ::nuvlaedge-release
  :-> ::spec/nuvlaedge-release)


(defn- version-string->number-vec [version]
  (map js/Number (str/split version ".")))


(defn security-available? [version]
  (let [[major minor _] (version-string->number-vec version)]
    (and (<= 2 major)
         (<= 3 minor))))

(reg-sub
  ::nuvlabox-modules
  :<- [::nuvlabox-status]
  (fn [nuvlabox-status]
    (let [scope-regex-match        #".*docker-compose\.([a-z]+)\.yml$"
          version                  (:nuvlabox-engine-version nuvlabox-status)
          invalid-version-string?  (not (every? int? (take 2 (version-string->number-vec version))))
          version-with-sec-module? (security-available? version)
          config-files             (get-in nuvlabox-status [:installation-parameters :config-files])
          modules                  (remove nil?
                                           (map #(-> (re-matches scope-regex-match %)
                                                     second
                                                     keyword) config-files))
          modules->bool            (zipmap modules (cycle [true]))]
      (if (and (or version-with-sec-module?
                   invalid-version-string?)
               (nil? (modules->bool :security)))
        (assoc modules->bool :security false)
        modules->bool))))

(reg-sub
  ::nuvlabox-components
  :<- [::nuvlabox-status]
  (fn [{:keys [components]}]
    components))

(reg-sub
  ::nuvlabox-vulns
  (fn [{{:keys [items] :as vulns} ::spec/nuvlabox-vulns}]
    (assoc vulns :items (map edges-utils/score-vulnerability items))))

(reg-sub
  ::nuvlabox-associated-ssh-keys
  (fn [db]
    (::spec/nuvlabox-associated-ssh-keys db)))

(reg-sub
  ::nuvlabox-peripherals
  (fn [db]
    (::spec/nuvlabox-peripherals db)))

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
  ::vuln-severity-selector
  (fn [db]
    (::spec/vuln-severity-selector db)))

(reg-sub
  ::matching-vulns-from-db
  (fn [db]
    (::spec/matching-vulns-from-db db)))

(reg-sub
  ::nuvlabox
  (fn [db]
    (::spec/nuvlabox db)))

(reg-sub
  ::capabilities
  :<- [::nuvlabox]
  :-> (comp set :capabilities))

(reg-sub
  ::has-capability-heartbeat?
  :<- [::capabilities]
  (fn [capabilities]
    (contains? capabilities "NUVLA_HEARTBEAT")))

(reg-sub
  ::can-decommission?
  :<- [::nuvlabox]
  (fn [nuvlabox]
    (general-utils/can-operation? "decommission" nuvlabox)))

;(defn NextTelemetryStatus
;  [{:keys [next-heartbeat] :as _nb-status}]
;  (let [{:keys [refresh-interval]} @(subscribe [::subs/nuvlabox])
;        tr                       @(subscribe [::i18n-subs/tr])
;        next-heartbeat-moment    (some-> next-heartbeat time/parse-iso8601)
;        next-heartbeat-times-ago (when next-heartbeat-moment
;                                   [uix/TimeAgo next-heartbeat-moment])]
;    (when next-heartbeat-moment
;      [:<>
;       (if (time/before-now? next-heartbeat-moment)
;         [:p (tr [:nuvlaedge-next-telemetry-missing-since])
;          next-heartbeat-times-ago "."]
;         [:p (tr [:nuvlaedge-next-telemetry-expected])
;          next-heartbeat-times-ago "."])
;       [:p (tr [:nuvlaedge-last-telemetry-was])
;        [uix/TimeAgo (utils/last-time-online
;                       next-heartbeat-moment
;                       refresh-interval)]
;        "."]])))

(reg-sub
  ::last-telemetry-message

  :<- [::next-heartbeat-moment]
  (fn [[tr next-heartbeat-moment]]


    )
  )

(reg-sub
  ::can-edit?
  :<- [::nuvlabox]
  (fn [nuvlabox]
    (general-utils/can-edit? nuvlabox)))

(reg-sub
  ::can-delete?
  :<- [::nuvlabox]
  (fn [nuvlabox]
    (general-utils/can-delete? nuvlabox)))

(reg-sub
  ::nuvlabox-managers
  (fn [db]
    (::spec/nuvlabox-managers db)))

(reg-sub
  ::join-token
  (fn [db]
    (::spec/join-token db)))

(reg-sub
  ::nuvlabox-cluster
  (fn [db]
    (::spec/nuvlabox-cluster db)))

(reg-sub
  ::nuvlabox-not-found?
  (fn [db]
    (::spec/nuvlabox-not-found? db)))

(reg-sub
  ::nuvlabox-playbooks
  (fn [db]
    (::spec/nuvlabox-playbooks db)))

(reg-sub
  ::infra-services
  (fn [db]
    (::spec/infra-services db)))

(reg-sub
  ::nuvlabox-emergency-playbooks
  (fn [db]
    (::spec/nuvlabox-emergency-playbooks db)))

(reg-sub
  ::nuvlabox-current-playbook
  (fn [db]
    (::spec/nuvlabox-current-playbook db)))
