(ns sixsq.nuvla.ui.common-components.acl.utils
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]))

(def rights-hierarchy (-> (make-hierarchy)

                          (derive :edit-acl :edit-data)
                          (derive :edit-acl :delete)
                          (derive :edit-acl :manage)
                          (derive :edit-acl :view-acl)

                          (derive :edit-data :edit-meta)
                          (derive :edit-data :view-data)

                          (derive :edit-meta :view-meta)

                          (derive :delete :view-meta)

                          (derive :view-acl :view-data)
                          (derive :view-data :view-meta)))

(defn extent-right
  [right-kw]
  (-> rights-hierarchy
      (ancestors right-kw)
      (conj right-kw)
      (set)))

(def all-defined-rights [:edit-acl :edit-data :edit-meta :view-acl :view-data :view-meta :manage :delete])

(def subset-defined-rights [:edit-acl :view-acl :manage :delete])

(defn val-as-set
  [[k v]]
  [k (set v)])

(defn val-as-vector
  [[k v]]
  [k (vec (sort v))])

(defn remove-owners-from-rights
  [owners-set [right principals]]
  [right (set/difference principals owners-set)])

(defn merge-rights
  ([] {})
  ([acl-a] acl-a)
  ([acl-a acl-b] (merge-with set/union acl-a acl-b)))

(defn extend-rights
  [[right principals]]
  (let [sub-rights (ancestors rights-hierarchy right)]
    (conj
      (map (fn [sub-right] {sub-right principals}) sub-rights)
      {right principals})))

(defn normalize-acl
  "Takes an ACL and returns a normalized version of the ACL where all
   rights are listed explicitly and owners do not appear in the lists for
   individual rights."
  [{:keys [owners] :as acl}]
  (let [owners-set        (set owners)
        normalized-rights (->> (dissoc acl :owners)
                               (map val-as-set)
                               (map (partial remove-owners-from-rights owners-set))
                               (remove (fn [[_ principals]] (empty? principals)))
                               (mapcat extend-rights)
                               (reduce merge-rights))]
    (->> (assoc normalized-rights :owners owners-set)
         (map val-as-vector)
         (into {}))))

(defn same-base-right
  [right-kw]
  (case right-kw
    :edit-acl [:edit-acl :edit-data :edit-meta]
    :edit-data [:edit-data :edit-meta]
    :view-acl [:view-acl :view-data :view-meta]
    :view-data [:view-data :view-meta]
    [right-kw]))

(defn get-principals
  [acl]
  (->> acl
       (mapcat (fn [[_right principal]] principal))
       (set)))

(defn acl->ui-acl-format
  [acl]
  (let [normalized-acl     (normalize-acl acl)
        local-owners       (-> normalized-acl
                               :owners
                               sort
                               vec)
        acl-without-owners (dissoc normalized-acl :owners)
        principals-rights  (vec
                             (sort
                               (reduce
                                 (partial merge-with set/union)
                                 (map
                                   (fn [[right-kw principals]]
                                     (into {} (map (fn [principal] [principal #{right-kw}]) principals)))
                                   acl-without-owners))))]
    {:owners     local-owners
     :principals principals-rights}))

(defn ui-acl-format->acl
  [{:keys [owners principals]}]
  (let [rights-principals (reduce (partial merge-with concat)
                                  (mapcat (fn [[principal rights]]
                                            (map (fn [right]
                                                   {right [principal]}) rights))
                                          principals))]
    (assoc rights-principals :owners owners)))

(defn acl-remove-owner
  [{:keys [owners] :as ui-acl} principal]
  (->> owners
       (filterv #(not= principal %))
       (assoc ui-acl :owners)))

(defn acl-get-owners-set
  [{:keys [owners] :as _ui-acl}]
  (set owners))

(defn acl-get-principals-set
  [{:keys [principals] :as _ui-acl}]
  (set (map first principals)))

(defn acl-get-all-principals-set
  [ui-acl]
  (set/union
    (acl-get-owners-set ui-acl)
    (acl-get-principals-set ui-acl)))

(defn acl-add-owner
  [ui-acl principal]
  (update ui-acl :owners conj principal))

(defn acl-remove-principle-from-rights
  [{:keys [principals] :as ui-acl} principal]
  (->> principals
       (filterv #(not= principal (first %)))
       (assoc ui-acl :principals)))

(defn acl-get-all-used-rights-set
  [{:keys [principals] :as _ui-acl}]
  (set (mapcat second principals)))

(defn acl-rights-empty?
  [{:keys [principals] :as _ui-acl}]
  (empty? principals))

(defn acl-add-principal-with-right
  [ui-acl principal right]
  (update ui-acl :principals conj [principal (extent-right right)]))

(defn acl-change-rights-for-row
  [ui-acl row-nubmer principal rights]
  (update ui-acl :principals assoc row-nubmer [principal rights]))

(defn find-group
  [id groups]
  (some #(when (= (first %) id) %) groups))

(defn id->icon
  [id]
  (case (general-utils/id->resource-name id)
    "user" icons/i-user
    "group" icons/i-users
    "nuvlabox" icons/i-box
    "infrastructure-service" icons/i-cloud
    "deployment" icons/i-rocket
    icons/i-question-circle-outline))

;; The following code is cherry-picked from server-side `sixsq.nuvla.auth.acl-resource`.
;; Cherry-pick only what is needed, and keep it aligned if there are changes server-side.
;; Only the `has-rights?` fn is slightly adapted to get the claims from a parameter instead
;; of deriving them from the request.

(defn unqualify
  [kw]
  (-> kw name keyword))

(defn- add-rights-entry
  [m kw]
  (-> m
      (assoc kw kw)
      (assoc (unqualify kw) kw)))

;; provides mappings between the namespaced keywords and themselves,
;; as well as the un-namespaced keywords and the namespaced ones.
(def rights-keywords
  (reduce add-rights-entry {} all-defined-rights))

(defn is-owner?
  [{:keys [claims] :as _authn-info}
   {:keys [owners] :as _acl}]
  (some (set claims) owners))

(defn is-admin?
  [{:keys [claims] :as _authn-info}]
  (contains? (set claims) "group/nuvla-admin"))

(defn extract-right
  "Given the identity map, this extracts the associated right.
  If the right does not apply, then nil is returned."
  [{:keys [claims]} [right principals]]
  (let [principals-with-admin (conj principals "group/nuvla-admin")]
    (when claims
      (when (some claims principals-with-admin)
        (get rights-keywords right)))))

(defn extract-rights
  "Returns a set containing all of the applicable rights from an ACL
   for the given identity map."
  [authn-info acl]
  (if (or (is-owner? authn-info acl)
          (is-admin? authn-info))
    (set all-defined-rights)
    (->> (dissoc acl :owners)
         (map (partial extract-right authn-info))
         (remove nil?)
         (set))))

(defn has-rights?
  "Based on the rights derived from the authentication information and the
   acl, this function returns true if the given `right` is allowed."
  [required-rights {:keys [acl] :as _resource} active-claim]
  (let [authn-info {:claims #{active-claim}}]
    (boolean
      (seq (set/intersection
             required-rights
             (extract-rights authn-info acl))))))

(def can-edit-data? (partial has-rights? #{:edit-data}))

(defn principal?
  [principal]
  (or (str/starts-with? principal "group/")
      (str/starts-with? principal "user/")))
