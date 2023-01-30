(ns sixsq.nuvla.ui.session.subs
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.cimi.subs :as cimi-subs]
            [sixsq.nuvla.ui.session.spec :as spec]
            [sixsq.nuvla.ui.session.utils :as utils]
            [sixsq.nuvla.ui.utils.general :as general-utils]))

(reg-sub
  ::session-loading?
  (fn [db]
    (::spec/session-loading? db)))

(reg-sub
  ::session
  (fn [db]
    (::spec/session db)))

(reg-sub
  ::active-claim
  :<- [::session]
  :-> utils/get-active-claim)

(reg-sub
  ::groups-hierarchies
  (fn [db]
    (::spec/groups-hierarchies db)))

(reg-sub
  ::groups-user
  :<- [::session]
  :<- [::groups-hierarchies]
  (fn [[{:keys [user identifier active-claim]} groups-hierarchies]]
    (loop [acc [{:text     identifier
                 :value    user
                 :level    0
                 :icon     "user"
                 :selected (= active-claim user)}]
           h   groups-hierarchies]
      (if (nil? (seq h))
        acc
        (let [{:keys [id name level children]} (first h)]
          (recur (conj acc {:text     (or name (general-utils/id->uuid id))
                            :value    id
                            :level    (or level 0)
                            :icon     "group"})
                 (concat
                   (->> children
                        (map #(assoc % :level (inc level)))
                        (sort (juxt :name :id)))
                   (next h))))))))

(reg-sub
  ::root-groups
  :<- [::groups-hierarchies]
  (fn [hierarchies]
    (set (map :id hierarchies))))

(reg-sub
  ::is-group?
  :<- [::active-claim]
  (fn [active-claim]
    (utils/is-group? active-claim)))

(reg-sub
  ::switch-group-options
  :<- [::session]
  :<- [::groups-user]
  (fn [[session groups-user]]
    (when (and (general-utils/can-operation? "switch-group" session)
               (-> groups-user count (> 1)))
      groups-user)))

(reg-sub
  ::loading?
  (fn [db]
    (::spec/loading? db)))

(reg-sub
  ::roles
  :<- [::session]
  (fn [session]
    (set (some-> session :roles (str/split #"\s+")))))

(reg-sub
  ::has-role?
  :<- [::roles]
  (fn [roles [_ role]]
    (contains? roles role)))

(reg-sub
  ::is-admin?
  :<- [::has-role? "group/nuvla-admin"]
  (fn [is-admin?]
    is-admin?))

(reg-sub
  ::is-user?
  :<- [::has-role? "group/nuvla-user"]
  (fn [is-user?]
    is-user?))

(reg-sub
  ::is-subgroup?
  :<- [::active-claim]
  :<- [::is-group?]
  :<- [::root-groups]
  (fn [[active-claim is-group? root-groups]]
    (and is-group?
         (not (root-groups active-claim)))))

(reg-sub
  ::user
  :<- [::active-claim]
  :<- [::identifier]
  :<- [::is-group?]
  (fn [[active-claim identifier is-group?]]
    (or
      (when is-group? active-claim)
      identifier)))

(reg-sub
  ::user-id
  :<- [::session]
  (fn [session]
    (:user session)))

(reg-sub
  ::identifier
  :<- [::session]
  (fn [session]
    (:identifier session)))

(reg-sub
  ::logged-in?
  :<- [::session]
  (fn [session]
    (some? session)))

(reg-sub
  ::error-message
  (fn [db]
    (::spec/error-message db)))

(reg-sub
  ::success-message
  (fn [db]
    (::spec/success-message db)))

(reg-sub
  ::server-redirect-uri
  (fn [db]
    (::spec/server-redirect-uri db)))

(reg-sub
  ::user-templates
  :<- [::cimi-subs/collection-templates :user-template]
  (fn [user-templates]
    user-templates))

(reg-sub
  ::user-template-exist?
  :<- [::user-templates]
  (fn [user-templates [_ template-id]]
    (contains? user-templates template-id)))

(reg-sub
  ::session-templates
  :<- [::cimi-subs/collection-templates :session-template]
  (fn [session-templates _]
    session-templates))

(reg-sub
  ::session-template-exist?
  :<- [::session-templates]
  (fn [session-templates [_ template-id]]
    (contains? session-templates template-id)))

(reg-sub
  ::peers
  (fn [db]
    (::spec/peers db)))

(reg-sub
  ::peers-options
  :<- [::peers]
  (fn [peers]
    (map (fn [[k v]] {:key k, :value k, :text v}) peers)))

(reg-sub
  ::resolve-user
  :<- [::user-id]
  :<- [::identifier]
  :<- [::peers]
  (fn [[current-user-id identifier peers] [_ user-id]]
    (if (= user-id current-user-id)
      identifier
      (get peers user-id user-id))))

(reg-sub
  ::resolve-users
  :<- [::user-id]
  :<- [::identifier]
  :<- [::peers]
  (fn [[current-user-id identifier peers] [_ users]]
    (into [] (map #(if (= % current-user-id)
                     identifier
                     (get peers % %)) users))))

(reg-sub
  ::groups
  (fn [{:keys [::spec/groups]}]
    groups))

(reg-sub
  ::groups-mapping
  :<- [::groups]
  (fn [groups]
    (->> groups
         (map (juxt :id :name))
         (into {}))))

(reg-sub
  ::groups-options
  :<- [::groups-mapping]
  (fn [groups-mapping]
    (map (fn [[id name]] {:key id, :value id, :text (or name (utils/remove-group-prefix id))}) groups-mapping)))

(reg-sub
  ::resolve-principal
  :<- [::user-id]
  :<- [::identifier]
  :<- [::peers]
  :<- [::groups-mapping]
  (fn [[current-user-id identifier peers groups] [_ id]]
    (if (string? id)
      (cond
        (str/starts-with? id "group/") (or (get groups id) (utils/remove-group-prefix id))
        (= id current-user-id) identifier
        :else (or (get peers id) id))
      id)))

(reg-sub
  ::peers-groups-options
  :<- [::peers-options]
  :<- [::groups-options]
  (fn [[peers groups] [_ filter-set]]
    (remove #(contains? filter-set (:value %))
            (concat (map #(assoc % :icon "user") peers)
                    (map #(assoc % :icon "group") groups)))))
