(ns sixsq.nuvla.ui.pages.groups.subs
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.pages.groups.spec :as spec]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.general :as general-utils]))

(reg-sub
  ::pending-invitations
  :-> ::spec/pending-invitations)

(reg-sub
  ::indexed-groups
  :<- [::session-subs/groups]
  :<- [::session-subs/peers]
  (fn [[groups peers]]
    (map (fn [{:keys [id name users] :as _group}]
           {:id       id
            :keywords (remove nil?
                              (concat
                                [id name]
                                users
                                (map #(get peers %) users)))})
         groups)))


(defn matching-group
  [pattern {:keys [keywords]}]
  (some #(re-matches pattern %) keywords))

(defn filter-groups
  [allowed-ids group]
  (let [children          (:children group)
        filtered-children (when children
                            (->> children
                                 (map #(filter-groups allowed-ids %))
                                 (remove nil?)))]
    ;; keep this group if its id is allowed OR it has any kept children
    (when (or (allowed-ids (:id group))
              (seq filtered-children))
      (cond-> (assoc group :children filtered-children)
              (empty? filtered-children) (dissoc :children)))))

(defn filter-groups-tree
  [allowed-ids groups]
  (->> groups
       (map #(filter-groups allowed-ids %))
       (remove nil?)))


(reg-sub
  ::filter-groups-hierarchy
  :<- [::indexed-groups]
  :<- [::session-subs/groups-hierarchies]
  (fn [[indexed-groups groups-hierarchies] [_ search]]
    (let [groups-hierarchies (filter (fn [root-group] (not (#{"group/nuvla-user" "group/nuvla-nuvlabox" "group/nuvla-anon" "group/nuvla-vpn"} (:id root-group)))) groups-hierarchies)]
      (if (str/blank? search)
        groups-hierarchies
        (let [pattern     (re-pattern (str "(?i).*" (general-utils/regex-escape search) ".*"))
              allowed-ids (->> indexed-groups
                               (keep #(when (matching-group pattern %) (:id %)))
                               set)]
          (filter-groups-tree allowed-ids groups-hierarchies))
        ))))
