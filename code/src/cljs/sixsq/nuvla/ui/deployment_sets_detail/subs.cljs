(ns sixsq.nuvla.ui.deployment-sets-detail.subs
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.deployment-sets-detail.spec :as spec]
    [sixsq.nuvla.ui.plugins.module :as module-plugin]
    [sixsq.nuvla.ui.utils.general :as general-utils]))

(reg-sub
  ::loading?
  (fn [db]
    (::spec/loading? db)))

(reg-sub
  ::deployment-set
  (fn [db]
    (::spec/deployment-set db)))

(reg-sub
  ::can-edit?
  :<- [::deployment-set]
  (fn [deployment-set _]
    (general-utils/can-edit? deployment-set)))

(reg-sub
  ::can-delete?
  :<- [::deployment-set]
  (fn [deployment-set _]
    (general-utils/can-delete? deployment-set)))

(reg-sub
  ::deployment-set-not-found?
  (fn [db]
    (::spec/deployment-set-not-found? db)))

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
  (fn [{:keys [resources]}]
    (reduce transform {} resources)))

(reg-sub
  ::apps-selected
  (fn [db]
    (::spec/apps-selected db)))

(reg-sub
  ::apps-selected?
  :<- [::apps-selected]
  (fn [apps-selected [_ module]]
    (contains? apps-selected module)))

(reg-sub
  ::apps-loading?
  (fn [db]
    (::spec/apps-loading? db)))

(reg-sub
  ::targets-loading?
  (fn [db]
    (::spec/targets-loading? db)))

(reg-sub
  ::edges
  (fn [db]
    (::spec/edges db)))

(reg-sub
  ::credentials
  (fn [db]
    (::spec/credentials db)))

(reg-sub
  ::credentials-grouped-by-parent
  :<- [::credentials]
  (fn [{:keys [resources]}]
    (group-by :parent resources)))

(reg-sub
  ::infrastructures
  (fn [db]
    (::spec/infrastructures db)))

(reg-sub
  ::infrastructures-with-credentials
  :<- [::infrastructures]
  :<- [::credentials-grouped-by-parent]
  :<- [::edges]
  (fn [[{:keys [resources]} creds-by-parent {edges :resources}]]
    (let [edges-by-infra-group (->> edges
                                    (map (juxt :infrastructure-service-group identity))
                                    (into {}))]
      (->> resources
           (map #(let [{:keys [id parent name description]} %
                   {edge-name  :name
                    edge-descr :description} (get edges-by-infra-group parent)]
               (assoc % :credentials (get creds-by-parent id)
                        :name (or edge-name name)
                        :description (or edge-descr description))))
           (sort-by (juxt :name :id))))))

(reg-sub
  ::infrastructures-with-credentials-by-parent
  :<- [::infrastructures-with-credentials]
  (fn [infras-with-creds]
    (group-by :parent infras-with-creds)))

(reg-sub
  ::edges-with-infras-creds
  :<- [::edges]
  :<- [::infrastructures-with-credentials-by-parent]
  (fn [[{:keys [resources]} infras-with-creds-by-parent]]
    (map #(assoc % :infrastructures
                   (get infras-with-creds-by-parent
                        (:infrastructure-service-group %))) resources)))

(reg-sub
  ::targets-selected
  (fn [db]
    (::spec/targets-selected db)))

(reg-sub
  ::targets-selected?
  :<- [::targets-selected]
  (fn [targets-selected [_ credentials]]
    (->> targets-selected
         (some (set credentials))
         boolean)))

(reg-sub
  ::configure-disabled?
  (fn [{:keys [::spec/targets-selected
               ::spec/apps-selected]}]
    (or (empty? apps-selected)
        (empty? targets-selected))))

(reg-sub
  ::some-license-not-accepted?
  (fn [{:keys [::spec/apps-selected] :as db}]
    (some #(false? (module-plugin/db-license-accepted? db [::spec/module-versions] (:id %))) apps-selected)))

(reg-sub
  ::some-price-not-accepted?
  (fn [{:keys [::spec/apps-selected] :as db}]
    (some #(false? (module-plugin/db-price-accepted? db [::spec/module-versions] (:id %))) apps-selected)))

(reg-sub
  ::create-disabled?
  :<- [::configure-disabled?]
  :<- [::some-license-not-accepted?]
  :<- [::some-price-not-accepted?]
  (fn [[configure-disabled?
        some-license-not-accepted?
        some-price-not-accepted?]]
    (or configure-disabled?
        some-license-not-accepted?
        some-price-not-accepted?)))
