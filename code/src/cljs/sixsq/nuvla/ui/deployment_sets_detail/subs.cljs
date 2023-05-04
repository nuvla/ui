(ns sixsq.nuvla.ui.deployment-sets-detail.subs
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-sub subscribe]]
            [re-frame.interop :refer [reactive?]]
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

(reg-sub
  ::targets-selected
  (fn [db [_ i]]
    (vals (get-in db [::spec/apps-sets i ::spec/targets-selected]))))

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
  ::applications-sets-all-applications
  (fn [db]
    (->> (applications-sets db)
         (map-indexed
           (fn [i {:keys [applications]}]
             (map (fn [{:keys [id]}]
                    (module-plugin/module-db db [::spec/apps-sets i] id)
                    ) applications)))
         (apply concat))))

(reg-sub
  ::deployment-set-licenses
  :<- [::applications-sets-all-applications]
  (fn [all-applications]
    (keep (fn [{:keys [license]}]
            (when license license))
          all-applications)))

(reg-sub
  ::deployment-set-prices
  :<- [::applications-sets-all-applications]
  (fn [all-applications]
    (keep (fn [{:keys [price]}]
            (when price price))
          all-applications)))

(reg-sub
  ::create-start-disabled?
  :<- [::get ::spec/create-name]
  :<- [::deployment-set-licenses]
  :<- [::get ::spec/licenses-accepted?]
  :<- [::deployment-set-prices]
  :<- [::get ::spec/prices-accepted?]
  (fn [[create-name licenses licenses-accepted? prices prices-accepted?]]
    (or (str/blank? create-name)
        (and (seq licenses)
             (not licenses-accepted?))
        (and (seq prices)
             (not prices-accepted?)))))
