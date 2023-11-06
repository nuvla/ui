(ns sixsq.nuvla.ui.deployment-sets-detail.subs
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.apps.spec :refer [nonblank-string]]
            [sixsq.nuvla.ui.deployment-sets-detail.spec :as spec]
            [sixsq.nuvla.ui.deployment-sets-detail.utils :as utils]
            [sixsq.nuvla.ui.edges.utils :as edges-utils]
            [sixsq.nuvla.ui.plugins.module :as module-plugin]
            [sixsq.nuvla.ui.routing.utils :as routing-utils]
            [sixsq.nuvla.ui.utils.general :as general-utils]))

(def creation-temp-id-key :temp-id)

(defn get-target-fleet-ids
  [depl-set]
  (->> depl-set
       :applications-sets
       (mapcat :overwrites)
       (mapcat (juxt :targets :fleet))
       (remove nil?)
       flatten))

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
  ::fleet-changes
  :<- [::deployment-set-stored]
  :<- [::deployment-set-edited]
  (fn [[stored-set changed-set]]
    (let [stored-fleet  (set (get-target-fleet-ids stored-set))
          changed-fleet (set (get-target-fleet-ids changed-set))
          added-edges   (seq (set/difference changed-fleet stored-fleet))
          removed-edges (seq (set/difference stored-fleet changed-fleet))]
      (when (or added-edges removed-edges)
        {:removed removed-edges
         :added   added-edges}))))

(reg-sub
  ::show-only-changed-fleet?
  (fn [db]
    (boolean (db ::spec/changed-edges))))

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
    (merge stored (select-keys edited [:name :description :applications-sets]))))


(reg-sub
  ::deployment-set-name
  :<- [::deployment-set]
  :-> :name)


(reg-sub
  ::apps-count
  :<- [::applications-sets-apps-targets]
  (fn [apps]
    (count apps)))

(reg-sub
  ::can-edit?
  :<- [::deployment-set]
  (fn [deployment-set]
    (general-utils/can-edit? deployment-set)))

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
  ::applications
  (fn [db]
    (->> (applications-sets db)
         (map-indexed
           (fn [i {:keys [applications]}]
             (map (fn [{:keys [id]}]
                    (module-plugin/db-module db [::spec/apps-sets i] id)
                    ) applications)))
         (apply concat))))

(reg-sub
  ::applications-sets-apps-targets
  (fn [db]
    (->> (applications-sets db)
         (map-indexed
           (fn [i {:keys [applications]}]
             (keep (fn [{:keys [id]}]
                     (let [targets (get-db-targets-selected-ids db i)]
                       (when-let [module (module-plugin/db-module db [::spec/apps-sets i] id)]
                         {:i                      i
                          :application            module
                          :registries-credentials (module-plugin/db-module-registries-credentials
                                                    db [::spec/apps-sets i] id)
                          :targets                targets
                          :targets-count          (count targets)}))
                     ) applications)))
         (apply concat))))


(defn- get-app-version-no
  [application]
  (module-plugin/get-version-id
    (map-indexed vector (:versions application))
    (-> application :content :id)))

(defn- app->app-row-data [{:keys [application i]}]
  {:id      (str i "_" (:id application))
   :idx     i
   :href    (:id application)
   :app     (:name application)
   :version {:label   (str "v" (get-app-version-no application))
             :created (-> application :content :created)}})


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

(defn current-route->fleet-filter-db-path
  [current-route]
  (create-db-path [::spec/fleet-filter]
                  (get-temp-db-id current-route)))

(defn current-route->fleet-filter-edited-db-path
  [current-route]
  (create-db-path [::spec/fleet-filter-edited]
                  (get-temp-db-id current-route)))

(defn create-apps-creation-db-path
  [current-route]
  (create-db-path [::spec/apps-creation]
                  (get-temp-db-id current-route)))

(defn apps-creation [{:keys [current-route] :as db}]
  (let [apps-db-path (create-apps-creation-db-path current-route)]
    (get-in db apps-db-path)))

(reg-sub
  ::apps-creation
  apps-creation)

(reg-sub
  ::apps-creation-row-data
  :<- [::apps-creation]
  (fn [apps]
    (map-indexed
      (fn [idx app]
        (app->app-row-data {:i           idx
                            :application app}))
      apps)))

(reg-sub
  ::apps-edited?
  :-> ::spec/apps-edited?)

(reg-sub
  ::apps-row-data
  :<- [::apps-creation-row-data]
  :<- [::applications-overview-row-data]
  :<- [::apps-edited?]
  (fn [[apps-creation apps apps-edited?] [_ creating?]]
    (if (or creating? apps-edited?)
      apps-creation
      apps)))

(def fleet-filter-path [:applications-sets 0 :overwrites 0 :fleet-filter])

(reg-sub
  ::fleet-filter-edited?
  (fn [{:keys [::spec/fleet-filter ::spec/fleet-filter-edited]}]
    (and fleet-filter-edited
        (not= fleet-filter fleet-filter-edited))))

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
    (general-utils/filter-eq-ids (->> edges :resources (mapv :id)))))

(reg-sub
  ::fleet-filter
  (fn [{:keys [current-route] :as db}]
    (let [path (current-route->fleet-filter-db-path current-route)
          path-edited (current-route->fleet-filter-edited-db-path current-route)]
      (or (get-in db path-edited) (get-in db path)))))

(reg-sub
  ::unsaved-changes?
  :<- [::deployment-set-stored-and-edited]
  :<- [::apps-edited?]
  :<- [::fleet-filter-edited?]
  (fn [[[stored edited] apps-edited? fleet-filter-edited?]]
    (or apps-edited?
        fleet-filter-edited?
        (utils/unsaved-changes? stored edited))))

(reg-sub
  ::save-enabled?
  :<- [::deployment-set-edited]
  :<- [::edges-in-deployment-group-response]
  :<- [::apps-creation]
  :<- [::applications-sets]
  :<- [::unsaved-changes?]
  (fn [[deployment-set-edited edges apps-creation _apps-sets unsaved-changes?] [_ creating?]]
    (if creating?
      (and (not (str/blank? (:name deployment-set-edited)))
           (seq edges)
           (seq apps-creation))
      unsaved-changes?)))

(reg-sub
  ::operation-enabled?
  :<- [::deployment-set]
  :<- [::save-enabled?]
  (fn [[deployment-set save-enabled?] [_ operation]]
    (and (not save-enabled?)
         (general-utils/can-operation? operation deployment-set))))

(reg-sub
  ::opened-modal
  :-> ::spec/opened-modal)

(reg-sub
  ::modal-open?
  :<- [::opened-modal]
  (fn [opened-modal [_ id]]
    (= id opened-modal)))

(defn apps-errors
  [db]
  (when-not (seq (apps-creation db))
    [{:type    :missing-apps
      :path    [:apps]
      :message [:depl-group-apps-missing]}]))

(defn env-vars-errors
  [{:keys [::spec/apps-edited?] :as db}]
  (let [apps (if apps-edited?
               (map (fn [{:keys [id] :as app}]
                      [0 {:id id :version (get-app-version-no app)}])
                    (apps-creation db))
               (->> (applications-sets db)
                    (keep-indexed
                      (fn [i {:keys [applications]}]
                        (map (fn [app] [i app]) applications)))
                    (apply concat)))]
    (->> apps
         (keep
           (fn [[i {:keys [id]}]]
             (when (seq (module-plugin/db-module-env-vars-in-error
                          db [::spec/apps-sets i] id))
               {:type    :missing-required-env-vars
                :path    [:apps-config i id]
                :message [:depl-group-mandatory-app-env-var-missing]}))))))

(defn deployment-set-validation
  "Returns an array of error maps of the form:
   ```
   {:type    <error type>
    :path    <vector representing the `path` through the ui component hierarchy where the validation error occurred>
    :message <vector to pass to the `tr` function to obtain a human-readable message for the error>}
   ```
  "
  [db]
  (let [errors (concat (apps-errors db)
                       (env-vars-errors db))]
    {:valid? (not (seq errors))
     :errors errors}))

(reg-sub
  ::deployment-set-validation
  deployment-set-validation)

(reg-sub
  ::apps-validation-error?
  (fn [{:keys [::spec/validate-form?] :as db}]
    (and validate-form?
         (some #{:apps}
               (map (comp first :path) (:errors (deployment-set-validation db)))))))

(reg-sub
  ::apps-config-validation-error?
  (fn [{:keys [::spec/validate-form?] :as db}]
    (and validate-form?
         (some #{:apps-config}
               (map (comp first :path) (:errors (deployment-set-validation db)))))))

(reg-sub
  ::app-config-validation-error?
  (fn [{:keys [::spec/validate-form?] :as db} [_ app-set-idx app-id]]
    (and validate-form?
         (some #{[:apps-config app-set-idx app-id]}
               (map (comp vec (partial take 3) :path)
                    (:errors (deployment-set-validation db)))))))

(reg-sub
  ::form-valid?
  (fn [{:keys [::spec/validate-form?] :as db}]
    (or (not validate-form?)
        (:valid? (deployment-set-validation db)))))

(reg-sub
  ::edge-picker-edges
  :-> ::spec/edge-picker-edges)

(reg-sub
  ::edge-picker-edges-resources
  :<- [::edge-picker-edges]
  (fn [edges]
    (:resources edges)))

(reg-sub
  ::edges-additional-filter
  :-> ::spec/edges-additional-filter)

(reg-sub
  ::edge-picker-additional-filter
  :-> ::spec/edge-picker-additional-filter)

(reg-sub
  ::edge-picker-edges-summary
  :-> ::spec/edge-picker-edges-summary)

(reg-sub
  ::edge-picker-edges-count
  :<- [::edge-picker-edges-summary]
  (fn [summary]
    (:count summary)))

(reg-sub
  ::edge-picker-edges-summary-stats
  :<- [::edge-picker-edges-summary]
  (fn [edges]
    (edges-utils/summary-stats edges)))

(reg-sub
  ::state-selector
  :-> ::spec/edge-picker-state-selector)
