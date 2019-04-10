(ns sixsq.nuvla.ui.apps-component.utils
  (:require [sixsq.nuvla.ui.apps-component.spec :as spec]
            [sixsq.nuvla.ui.apps.spec :as apps-spec]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.utils.general :as utils-general]
            [taoensso.timbre :as log]))

;; Deserialization functions: module->db

(defn urls->db
  [tuples]
  (into {}
        (for [[name url] tuples]
          (let [id (random-uuid)]
            {id {:id id :name name :url url}}))))


(defn ports->db
  [ports]
  (into {}
        (for [p ports]
          (let [id (random-uuid)]
            {id (assoc p :id id)}))))


(defn mounts->db
  [mounts]
  (into {}
        (for [m mounts]
          (let [id (random-uuid)]
            {id (assoc m :id id)}))))


(defn output-parameters->db
  [params]
  (into {}
        (for [p params]
          (let [id (random-uuid)]
            {id (assoc p :id id)}))))


(defn data-types->db
  [dts]
  (into {}
        (for [dt dts]
          (let [id (random-uuid)]
            {id {:id id :data-type dt}}))))


(defn module->db
  [module db]
  (-> db
      (assoc-in [::spec/urls] (urls->db (get-in module [:content :urls])))
      (assoc-in [::spec/architecture] (get-in module [:content :architecture]))
      (assoc-in [::spec/output-parameters] (output-parameters->db (get-in module [:content :output-parameters])))
      (assoc-in [::spec/data-types] (data-types->db (get-in module [:data-accept-content-types])))
      (assoc-in [::spec/ports] (ports->db (get-in module [:content :ports])))
      (assoc-in [::spec/mounts] (mounts->db (get-in module [:content :mounts])))
      ))


;; Serialization functions: db->module

(defn urls->module
  [db]
  (into []
        (for [[id u] (::spec/urls db)]
          [(:name u) (:url u)])))


(defn ports->module
  [db]
  (into []
        (for [[id p] (::spec/ports db)]
          (let [{:keys [target-port published-port protocol] :or {target-port nil published-port nil protocol "tcp"}} p]
            (conj {:target-port target-port}
                  (when (not (nil? published-port)) {:published-port published-port})
                  (when (not (nil? protocol)) {:protocol protocol}))
            ))))


; TODO: add options
(defn mounts->module
  [db]
  (into []
        (for [[id m] (::spec/mounts db)]
          (let [{:keys [source target read-only mount-type] :or {read-only false}} m]
            (conj {:source source}
                  {:target target}
                  {:mount-type mount-type}
                  (when (not (nil? read-only)) {:read-only read-only}))
            ))))


(defn output-parameters->module
  [db]
  (into []
        (for [[id op] (::spec/output-parameters db)]
          (let [{:keys [name description]} op]
            (conj
              {:name name}
              {:description description})
            ))))


(defn data-binding->module
  [db]
  (into []
        (for [[id binding] (::spec/data-types db)]
          (let [{:keys [data-type]} binding]
            (conj
              data-type)))))


(defn db->module
  [module commit-map db]
  (let [{:keys [author commit]} commit-map
        urls              (urls->module db)
        ports             (ports->module db)
        mounts            (mounts->module db)
        output-parameters (output-parameters->module db)
        bindings (data-binding->module db)
        ]
    (-> module
        (apps-utils/sanitize-base)
        (assoc-in [:content :author] author)
        (assoc-in [:content :commit] (if (empty? commit) "no commit message" commit))
        (assoc-in [:content :architecture] (::spec/architecture db))
        (assoc-in [:content :urls] urls)
        (assoc-in [:content :ports] ports)
        (assoc-in [:content :mounts] mounts)
        (assoc-in [:content :output-parameters] output-parameters)
        (assoc-in [:data-accept-content-types] bindings)
        )))
