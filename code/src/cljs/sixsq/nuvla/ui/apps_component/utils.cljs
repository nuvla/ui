(ns sixsq.nuvla.ui.apps-component.utils
  (:require [sixsq.nuvla.ui.apps-component.spec :as spec]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]))

;; Deserialization functions: module->db

(defn image->db
  [{:keys [registry repository image-name tag]}]
  {::spec/registry   registry
   ::spec/repository repository
   ::spec/image-name image-name
   ::spec/tag        tag})


(defn ports->db
  [ports]
  (into
    (sorted-map)
    (for [[id {:keys [target-port published-port protocol]}] (map-indexed vector ports)]
      [id {:id                   id
           ::spec/target-port    target-port
           ::spec/published-port published-port
           ::spec/protocol       protocol}])))


(defn mounts->db
  [mounts]
  (into
    (sorted-map)
    (for [[id {:keys [source target read-only mount-type]}] (map-indexed vector mounts)]
      [id {:id                    id
           ::spec/mount-source    source
           ::spec/mount-target    target
           ::spec/mount-read-only (boolean read-only)
           ::spec/mount-type      mount-type}])))


(defn module->db
  [db {:keys [content] :as module}]
  (let [{:keys [image architectures ports mounts]} content]
    (-> db
        (apps-utils/module->db module)
        (assoc-in [::spec/module-component ::spec/image] (image->db image))
        (assoc-in [::spec/module-component ::spec/architectures] architectures)
        (assoc-in [::spec/module-component ::spec/ports] (ports->db ports))
        (assoc-in [::spec/module-component ::spec/mounts] (mounts->db mounts)))))


;; Serialization functions: db->module

(defn image->module
  [db]
  (let [{:keys [::spec/registry ::spec/repository ::spec/image-name ::spec/tag]}
        (get-in db [::spec/module-component ::spec/image])]
    (conj {:image-name image-name}
          (when (not (nil? registry)) {:registry registry})
          (when (not (nil? repository)) {:repository repository})
          (when (not (nil? tag)) {:tag tag}))))


(defn ports->module
  [db]
  (into
    []
    (for [[id p] (get-in db [::spec/module-component ::spec/ports])]
      (let [{:keys [::spec/target-port ::spec/published-port ::spec/protocol]
             :or   {target-port nil published-port nil protocol "tcp"}} p]
        (conj {:target-port target-port}
              (when (not (nil? published-port)) {:published-port published-port})
              (when (not (nil? protocol)) {:protocol protocol}))))))


; TODO: add options
(defn mounts->module
  [db]
  (into
    []
    (for [[id m] (get-in db [::spec/module-component ::spec/mounts])]
      (let [{:keys [::spec/mount-source ::spec/mount-target
                    ::spec/mount-read-only ::spec/mount-type]
             :or   {mount-read-only false}} m]
        (conj {:source mount-source}
              {:target mount-target}
              {:mount-type mount-type}
              (when (not (nil? mount-read-only)) {:read-only mount-read-only}))))))


(defn db->module
  [module commit-map db]
  (let [{:keys [author commit]} commit-map
        architectures (get-in db [::spec/module-component ::spec/architectures])
        image         (image->module db)
        ports         (ports->module db)
        mounts        (mounts->module db)]
    (as-> module m
          (assoc-in m [:content :author] author)
          (assoc-in m [:content :commit] (if (empty? commit) "no commit message" commit))
          (assoc-in m [:content :architectures] architectures)
          (assoc-in m [:content :image] image)
          (assoc-in m [:content :ports] ports)
          (assoc-in m [:content :mounts] mounts))))
