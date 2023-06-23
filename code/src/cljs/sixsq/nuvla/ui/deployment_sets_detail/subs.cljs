(ns sixsq.nuvla.ui.deployment-sets-detail.subs
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-sub subscribe]]
            [sixsq.nuvla.ui.deployment-sets-detail.spec :as spec]
            [sixsq.nuvla.ui.plugins.module :as module-plugin]
            [sixsq.nuvla.ui.utils.general :as general-utils]))

(reg-sub
  ::loading?
  :-> ::spec/loading?)

(reg-sub
  ::deployment-set
  :-> ::spec/deployment-set)

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
