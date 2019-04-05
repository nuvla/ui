(ns sixsq.nuvla.ui.apps-component.utils
  (:require [taoensso.timbre :as log]
            [sixsq.nuvla.ui.apps.spec :as apps-spec]
            [sixsq.nuvla.ui.apps-component.spec :as spec]))

;; Deserialization functions: module->db

(defn urls-tuples->map
  [tuples]
  (for [[name url] tuples]
    (let [id (random-uuid)]
      (conj {id {:id id :name name :url url}}))))


(defn output-parameters
  [params]
  (for [p params]
    (let [id (random-uuid)]
      (conj {id (assoc p :id id)}))))


(defn data-types
  [dts]
  (for [dt dts]
    (let [id (random-uuid)]
      (log/infof "dt: %s" dt)
      (conj {id {:id id :data-type dt}}))))


(defn module->db
  [module db]
  (log/infof "arch from module: %s" (get-in module [:content :architecture]))
  (-> db
      (assoc-in [::spec/urls] (first (urls-tuples->map (get-in module [:content :urls]))))
      (assoc-in [::spec/architecture] (get-in module [:content :architecture]))
      (assoc-in [::spec/output-parameters] (first (output-parameters (get-in module [:content :output-parameters]))))
      (assoc-in [::spec/data-types] (first (data-types (get-in module [:data-accept-content-types]))))))
