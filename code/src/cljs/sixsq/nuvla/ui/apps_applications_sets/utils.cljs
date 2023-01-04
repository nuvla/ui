(ns sixsq.nuvla.ui.apps-applications-sets.utils
  (:require
    [clojure.string :as str]
    [sixsq.nuvla.ui.apps-applications-sets.spec :as spec]
    [sixsq.nuvla.ui.apps.utils :as apps-utils]))


;; Deserialization functions: module->db

(defn apps-sets->db
  [applications-sets]
  (into (sorted-map)
        (for [[id {:keys [name description]}] (map-indexed vector applications-sets)]
          [id {:id                         id
               ::spec/apps-set-name        name
               ::spec/apps-set-description description
               ::spec/apps-selected        #{}}])))

(defn module->db
  [db {:keys [content] :as module}]
  (-> db
      (apps-utils/module->db module)
      (assoc ::spec/apps-sets (apps-sets->db (:applications-sets content)))))


;; Serialization functions: db->module

(defn db->module
  [module commit-map db]
  (let [{:keys [author commit]} commit-map]
    (as-> module m
          (assoc-in m [:content :author] author)
          (assoc-in m [:content :commit] (if (empty? commit) "no commit message" commit))
          (update m :content dissoc :output-parameters)
          (assoc-in m [:content :applications-sets]
                    (->> db
                         ::spec/apps-sets
                         vals
                         (map (fn [{:keys [::spec/apps-set-name
                                           ::spec/apps-set-description
                                           ::spec/apps-selected]}]
                                (cond-> {:name apps-set-name}
                                        (seq apps-selected) (assoc :applications
                                                                   (->> apps-selected
                                                                        vals
                                                                        (map #(hash-map :id (:id %)
                                                                                        :version 0))))
                                        (not (str/blank? apps-set-description)) (assoc :description apps-set-description)))))))))
