(ns sixsq.nuvla.ui.acl.utils)


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


(defn update-acl-principal
  [acl right-keys fn-update]
  (loop [updated-acl acl
         left-keys (vec right-keys)]
    (let [key (peek left-keys)
          new-acl (update updated-acl key fn-update)]
      (if (empty? left-keys)
        updated-acl
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

