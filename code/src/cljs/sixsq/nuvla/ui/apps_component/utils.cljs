(ns sixsq.nuvla.ui.apps-component.utils
  (:require [sixsq.nuvla.ui.apps-component.spec :as spec]
            [sixsq.nuvla.ui.apps.spec :as apps-spec]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.utils.general :as utils-general]
            [taoensso.timbre :as log]))

;; Deserialization functions: module->db

(defn image->db
  [{:keys [registry repository image-name tag]}]
  {::spec/registry   registry
   ::spec/repository repository
   ::spec/image-name image-name
   ::spec/tag        tag})


(defn urls->db
  [tuples]
  (into {}
        (for [[name url] tuples]
          (let [id (random-uuid)]
            {id {:id             id
                 ::spec/url-name name
                 ::spec/url      url}}))))


(defn ports->db
  [ports]
  (into {}
        (for [{:keys [target-port published-port protocol]} ports]
          (let [id (random-uuid)]
            {id {:id                   id
                 ::spec/target-port    target-port
                 ::spec/published-port published-port
                 ::spec/protocol       protocol}}))))


(defn mounts->db
  [mounts]
  (into {}
        (for [{:keys [source target read-only mount-type]} mounts]
          (let [id (random-uuid)]
            {id {:id                    id
                 ::spec/mount-source    source
                 ::spec/mount-target    target
                 ::spec/mount-read-only (boolean read-only)
                 ::spec/mount-type      mount-type}}))))


(defn env-variables->db
  [env-variables]
  (into {}
        (for [{:keys [name description value required]} env-variables]
          (let [id (random-uuid)]
            {id {:id                    id
                 ::spec/env-name        name
                 ::spec/env-value       value
                 ::spec/env-description description
                 ::spec/env-required    (or required false)}}))))


(defn output-parameters->db
  [params]
  (into {}
        (for [{:keys [name description]} params]
          (let [id (random-uuid)]
            {id {:id                                 id
                 ::spec/output-parameter-name        name
                 ::spec/output-parameter-description description}}))))


(defn data-types->db
  [dts]
  (into {}
        (for [dt dts]
          (let [id (random-uuid)]
            {id {:id              id
                 ::spec/data-type dt}}))))


(defn module->db
  [db {:keys [content] :as module}]
  (let [{:keys [image urls architecture output-parameters
                data-accept-content-types ports mounts environmental-variables]} content]
    (-> db
        (apps-utils/module->db module)
        (assoc-in [::spec/module-component ::spec/image] (image->db image))
        (assoc-in [::spec/module-component ::spec/urls] (urls->db urls))
        (assoc-in [::spec/module-component ::spec/architecture] architecture)
        (assoc-in [::spec/module-component ::spec/output-parameters] (output-parameters->db output-parameters))
        (assoc-in [::spec/module-component ::spec/data-types] (data-types->db data-accept-content-types))
        (assoc-in [::spec/module-component ::spec/ports] (ports->db ports))
        (assoc-in [::spec/module-component ::spec/mounts] (mounts->db mounts))
        (assoc-in [::spec/module-component ::spec/env-variables] (env-variables->db environmental-variables)))))


;; Serialization functions: db->module

(defn image->module
  [db]
  (let [{:keys [::spec/registry ::spec/repository ::spec/image-name ::spec/tag]}
        (get-in db [::spec/module-component ::spec/image])]
    (conj {:image-name image-name}
          (when (not (nil? registry)) {:registry registry})
          (when (not (nil? repository)) {:repository repository})
          (when (not (nil? tag)) {:tag tag}))))


(defn urls->module
  [db]
  (into []
        (for [[id u] (get-in db [::spec/module-component ::spec/urls])]
          (do
            [(::spec/url-name u) (::spec/url u)]))))


(defn ports->module
  [db]
  (into []
        (for [[id p] (get-in db [::spec/module-component ::spec/ports])]
          (let [{:keys [::spec/target-port ::spec/published-port ::spec/protocol]
                 :or   {target-port nil published-port nil protocol "tcp"}} p]
            (conj {:target-port target-port}
                  (when (not (nil? published-port)) {:published-port published-port})
                  (when (not (nil? protocol)) {:protocol protocol}))))))


; TODO: add options
(defn mounts->module
  [db]
  (into []
        (for [[id m] (get-in db [::spec/module-component ::spec/mounts])]
          (let [{:keys [::spec/mount-source ::spec/mount-target ::spec/mount-read-only ::spec/mount-type]
                 :or   {mount-read-only false}} m]
            (conj {:source mount-source}
                  {:target mount-target}
                  {:mount-type mount-type}
                  (when (not (nil? mount-read-only)) {:read-only mount-read-only}))))))


(defn env-variables->module
  [db]
  (into []
        (for [[id m] (get-in db [::spec/module-component ::spec/env-variables])]
          (let [{:keys [::spec/env-name ::spec/env-description ::spec/env-value ::spec/env-required]
                 :or   {env-required false}} m]
            (cond-> {:name     env-name
                     :required env-required}
                    env-value (assoc :value env-value)
                    env-description (assoc :description env-description))))))


(defn output-parameters->module
  [db]
  (into []
        (for [[id op] (get-in db [::spec/module-component ::spec/output-parameters])]
          (let [{:keys [::spec/output-parameter-name ::spec/output-parameter-description]} op]
            (conj
              {:name output-parameter-name}
              {:description output-parameter-description})))))


(defn data-binding->module
  [db]
  (into []
        (for [[id binding] (get-in db [::spec/module-component ::spec/data-types])]
          (let [{:keys [::spec/data-type]} binding]
            (conj
              data-type)))))


(defn db->module
  [module commit-map db]
  (let [{:keys [author commit]} commit-map
        architecture      (get-in db [::spec/module-component ::spec/architecture])
        image             (image->module db)
        urls              (urls->module db)
        ports             (ports->module db)
        mounts            (mounts->module db)
        env-variables     (env-variables->module db)
        output-parameters (output-parameters->module db)
        bindings          (data-binding->module db)]
    (as-> module m
          (assoc-in m [:content :author] author)
          (assoc-in m [:content :commit] (if (empty? commit) "no commit message" commit))
          (assoc-in m [:content :architecture] architecture)
          (assoc-in m [:content :image] image)
          (if (empty? urls)
            (update-in m [:content] dissoc :urls)
            (assoc-in m [:content :urls] urls))
          (assoc-in m [:content :ports] ports)
          (assoc-in m [:content :mounts] mounts)
          (if (empty? env-variables)
            (update-in m [:content] dissoc :environmental-variables)
            (assoc-in m [:content :environmental-variables] env-variables))
          (assoc-in m [:content :output-parameters] output-parameters)
          (assoc-in m [:data-accept-content-types] bindings))))
