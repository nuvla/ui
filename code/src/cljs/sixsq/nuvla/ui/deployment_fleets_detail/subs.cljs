(ns sixsq.nuvla.ui.deployment-fleets-detail.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.deployment-fleets-detail.spec :as spec]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [clojure.string :as str]))

(reg-sub
  ::loading?
  (fn [db]
    (::spec/loading? db)))

(reg-sub
  ::deployment-fleet
  (fn [db]
    (::spec/deployment-fleet db)))

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
  ::deployment-fleet-not-found?
  (fn [db]
    (::spec/deployment-fleet-not-found? db)))

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
  ::apps-loading?
  (fn [db]
    (::spec/apps-loading? db)))

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
