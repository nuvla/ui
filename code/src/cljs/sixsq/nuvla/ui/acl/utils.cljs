(ns sixsq.nuvla.ui.acl.utils
  (:require [clojure.set :as set]))


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


(defn update-acl-principal
  [acl right-keys fn-update]
  (loop [updated-acl acl
         left-keys   (vec right-keys)]
    (let [key     (peek left-keys)
          new-acl (update updated-acl key fn-update)]
      (if (empty? left-keys)
        (normalize-acl updated-acl)
        (recur new-acl (pop left-keys))))))


(defn remove-principal
  [acl right-keys principal]
  (update-acl-principal acl right-keys
                        (fn [collection]
                          (remove #(= % principal) collection))))


(defn add-principal
  [acl right-keys principal]
  (update-acl-principal acl right-keys
                        (fn [collection]
                          (->> principal
                               (conj collection)
                               (set)
                               (sort)))))


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
       (mapcat (fn [[right principal]] principal))
       (set)))


(defn some-principal?
  [principal principals]
  (boolean (some #(= % principal) principals)))
