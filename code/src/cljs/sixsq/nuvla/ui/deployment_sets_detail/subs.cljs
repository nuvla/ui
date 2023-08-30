(ns sixsq.nuvla.ui.deployment-sets-detail.subs
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.apps.spec :refer [nonblank-string]]
            [sixsq.nuvla.ui.deployment-sets-detail.spec :as spec]
            [sixsq.nuvla.ui.edges.utils :as edges-utils]
            [sixsq.nuvla.ui.plugins.module :as module-plugin]
            [sixsq.nuvla.ui.routing.utils :as routing-utils]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.time :as time]))

(def creation-temp-id-key :temp-id)

(reg-sub
  ::loading?
  :-> ::spec/loading?)

(reg-sub
  ::deployment-set-stored
  :-> ::spec/deployment-set)

(reg-sub
  ::deployment-set-edited
  :-> ::spec/deployment-set-edited)

(reg-sub
  ::deployment-set-stored-and-edited
  :<- [::deployment-set-stored]
  :<- [::deployment-set-edited]
  (fn [[stored edited]]
    [stored edited]))

(reg-sub
  ::deployment-set
  :<- [::deployment-set-stored-and-edited]
  (fn [[stored edited]]
    (merge stored edited)))

;; Please ignore unused new subs: They're used in follow up branch
(reg-sub
  ::deployment-set-id
  :<- [::deployment-set]
  :-> :id)

(reg-sub
  ::deployment-set-name
  :<- [::deployment-set]
  :-> :name)

(reg-sub
  ::apps
  :<- [::deployment-set]
  (fn [deployment-set]
    (->> deployment-set
         :applications-sets
         (mapcat :overwrites)
         (mapcat :applications))))

(reg-sub
  ::can-edit?
  :<- [::deployment-set]
  (fn [deployment-set]
    (general-utils/can-edit? deployment-set)))

(reg-sub
  ::can-delete?
  :<- [::deployment-set]
  (fn [deployment-set]
    (general-utils/can-delete? deployment-set)))

(reg-sub
  ::deployment-set-not-found?
  :-> ::spec/deployment-set-not-found?)

(defn get-db-targets-selected
  [db i]
  (get-in db [::spec/apps-sets i ::spec/targets-selected]))

(defn get-db-targets-selected-ids
  [db i]
  (map first (get-db-targets-selected db i)))

(reg-sub
  ::targets-selected
  (fn [db [_ i]]
    (vals (get-db-targets-selected db i))))

(reg-sub
  ::get
  (fn [db [_ k]]
    (get db k)))

(defn applications-sets
  [db]
  (get-in db [::spec/module-applications-sets :content :applications-sets]))

(reg-sub
  ::applications-sets
  :-> applications-sets)

(reg-sub
  ::applications-sets-apps-targets
  (fn [db]
    (->> (applications-sets db)
         (map-indexed
           (fn [i {:keys [applications]}]
             (map (fn [{:keys [id]}]
                    (let [targets (get-db-targets-selected-ids db i)]
                      {:i                      i
                       :application            (module-plugin/db-module db [::spec/apps-sets i] id)
                       :registries-credentials (module-plugin/db-module-registries-credentials
                                                 db [::spec/apps-sets i] id)
                       :targets                targets
                       :targets-count          (count targets)})
                    ) applications)))
         (apply concat))))


(defn- app->app-row-data [{:keys [application i]}]
  {:idx i
   :href (:id application)
   :app-name (:name application)
   :version  (str "v" (module-plugin/get-version-id
                        (map-indexed vector (:versions application))
                        (-> application :content :id)))
   :status "yeah, good question"
   :last-update (time/time->format (js/Date.))})

(reg-sub
  ::applications-overview-row-data
  :<- [::applications-sets-apps-targets]
  (fn [apps]
    (mapv app->app-row-data apps)))


(defn license-set-apps-targets
  [sets-apps-targets]
  (get-in sets-apps-targets [:application :license]))

(defn price-set-apps-targets
  [sets-apps-targets]
  (get-in sets-apps-targets [:application :price]))

(reg-sub
  ::deployment-set-licenses
  :<- [::applications-sets-apps-targets]
  (fn [sets-apps-targets]
    (->> sets-apps-targets
         (filter license-set-apps-targets)
         (group-by license-set-apps-targets))))

(reg-sub
  ::deployment-set-apps-targets-total-price
  :<- [::applications-sets-apps-targets]
  (fn [apps-targets]
    (->> apps-targets
         (filter price-set-apps-targets)
         (map #(assoc %
                 :total-price
                 (* (get-in % [:application :price :cent-amount-daily])
                    (:targets-count %)))))))

(reg-sub
  ::deployment-set-total-price
  :<- [::deployment-set-apps-targets-total-price]
  (fn [apps-targets-total-price]
    (reduce #(+ %1 (:total-price %2)) 0 apps-targets-total-price)))

(reg-sub
  ::step-name-complete?
  :<- [::get ::spec/create-name]
  :-> (comp not str/blank?))

(reg-sub
  ::step-licenses-prices-complete?
  :<- [::deployment-set-licenses]
  :<- [::get ::spec/licenses-accepted?]
  :<- [::deployment-set-apps-targets-total-price]
  :<- [::get ::spec/prices-accepted?]
  (fn [[licenses licenses-accepted? prices prices-accepted?]]
    (boolean
      (and (or licenses-accepted?
               (not (seq licenses)))
           (or prices-accepted?
               (not (seq prices)))))))

(reg-sub
  ::deployment-set-registries-creds-complete?
  :<- [::applications-sets-apps-targets]
  (fn [sets-apps-targets]
    (every?
      (fn [{:keys [application registries-credentials]}]
        (= (count (get-in application [:content :private-registries]))
           (count (remove str/blank? registries-credentials))))
      sets-apps-targets)))

(reg-sub
  ::targets-sets-complete?
  :<- [::applications-sets-apps-targets]
  (fn [sets-apps-targets]
    (some #(pos? (:targets-count %)) sets-apps-targets)))

(reg-sub
  ::step-apps-targets-complete?
  :<- [::deployment-set-registries-creds-complete?]
  :<- [::targets-sets-complete?]
  :-> #(every? true? %))

(reg-sub
  ::create-start-disabled?
  :<- [::step-name-complete?]
  :<- [::step-licenses-prices-complete?]
  :<- [::step-apps-targets-complete?]
  ;;todo require all mandatory params to be filled up?
  :-> #(some false? %))

(defn create-db-path [db-path temp-id]
  (cond->> db-path
    (nonblank-string (str temp-id)) (into [::spec/temp-db temp-id])))

(defn- get-temp-db-id [current-route]
  (routing-utils/get-query-param current-route creation-temp-id-key))


(defn current-route->edges-db-path
  [current-route]
  (create-db-path [::spec/edges]
    (get-temp-db-id current-route)))

(defn create-apps-creation-db-path
  [current-route]
  (create-db-path [::spec/apps-creation]
    (get-temp-db-id current-route)))

(reg-sub
  ::apps-creation
  (fn [{:keys [current-route] :as db}]
    (let [apps-db-path (create-apps-creation-db-path current-route)]
      (get-in db apps-db-path))))

(reg-sub
  ::apps-creation-row-data
  :<- [::apps-creation]
  (fn [apps]
    (map-indexed
      (fn [idx app]
        (app->app-row-data {:i idx
                            :application app}))
      apps)))

(reg-sub
  ::edges-in-deployment-group-response
  (fn [{:keys [current-route] :as db}]
    (let [edges-db-path (current-route->edges-db-path current-route)]
      (get-in db edges-db-path))))

(reg-sub
  ::edges-summary-stats
  :<- [::edges-in-deployment-group-response]
  (fn [edges]
    (edges-utils/summary-stats edges)))

(reg-sub
  ::edges-count
  :<- [::edges-in-deployment-group-response]
  (fn [edges-response]
    (:count edges-response)))


(reg-sub
  ::all-edges-ids
  :<- [::edges-in-deployment-group-response]
  (fn [edges-response]
    (:resources edges-response)))

(reg-sub
  ::edges-documents-response
  :-> ::spec/edges-documents)

(reg-sub
  ::edges-documents
  :<- [::edges-documents-response]
  (fn [edges-documents-response]
    (:resources edges-documents-response)))

(reg-sub
  ::edges-by-id
  :<- [::edges-documents]
  (fn [edges]
    (zipmap
      (map :id edges)
      edges)))

(reg-sub
  ::get-edge-by-id
  :<- [::edges-by-id]
  (fn [edges-by-id [_ id]]
    (edges-by-id id)))

(reg-sub
  ::edges-filter
  :<- [::edges-in-deployment-group-response]
  (fn [edges]
    (general-utils/ids->inclusion-filter-string (->> edges :resources (map :id)))))

(reg-sub
  ::deployment-set-edited
  :-> ::spec/deployment-set-edited)

(reg-sub
  ::unsaved-changes?
  :<- [::deployment-set]
  :<- [::deployment-set-edited]
  (fn [[stored edited]]
    (not= stored edited)))

(reg-sub
  ::save-disabled?
  :<- [::deployment-set]
  :<- [::edges-in-deployment-group-response]
  :<- [::apps-creation]
  :<- [::applications-sets]
  :<- [::unsaved-changes?]
  (fn [[deployment-set edges apps-creation apps-sets unsaved-changes?] [_ creating?]]
    (and
      (nonblank-string (:name deployment-set))
      (seq edges)
      (seq (if creating? apps-creation apps-sets))
      (or creating? unsaved-changes?))))

(reg-sub
  ::opened-modal
  :-> ::spec/opened-modal)

(reg-sub
  ::modal-open?
  :<- [::opened-modal]
  (fn [opened-modal [_ id]]
    (= id opened-modal)))
