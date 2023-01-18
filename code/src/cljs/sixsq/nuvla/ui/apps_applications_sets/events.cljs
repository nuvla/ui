(ns sixsq.nuvla.ui.apps-applications-sets.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.apps-applications-sets.spec :as spec]
    [sixsq.nuvla.ui.apps.utils :as utils]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.plugins.module :as module-plugin]
    [sixsq.nuvla.ui.plugins.module-selector :as module-selector]
    [sixsq.nuvla.ui.utils.general :as general-utils]))

;; user should be told that some applications has been deleted!!

(reg-event-db
  ::clear-apps-applications-sets
  (fn [db [_]]
    (merge db spec/defaults)))

(reg-event-db
  ::update-apps-set-name
  (fn [db [_ id name]]
    (assoc-in db [::spec/apps-sets id
                  ::spec/apps-set-name] name)))

(reg-event-db
  ::update-apps-set-description
  (fn [db [_ id description]]
    (assoc-in db [::spec/apps-sets id
                  ::spec/apps-set-description] description)))

(defn restore-apps-selected
  [modules-by-id db [id {:keys [applications]}]]
  (->> applications
       (map (fn [{module-id :id}]
              [module-id (get modules-by-id module-id
                              {:subtype "unknown" :id module-id})]))
       (into {})
       (assoc-in db [::spec/apps-sets id ::spec/apps-selected])))

(defn load-module-configurations
  [modules-by-id fx [id {:keys [applications]}]]
  (->> applications
       (map (fn [{module-id :id :keys [version environmental-variables]}]
              (when (get modules-by-id module-id)
                [:dispatch [::module-plugin/load-module
                            [::spec/apps-sets id]
                            (str module-id "_" version)
                            (when (seq environmental-variables)
                              {:env (->> environmental-variables
                                         (map (juxt :name :value))
                                         (into {}))})]])))
       (concat fx)))

(reg-event-fx
  ::reload-apps-sets-response
  (fn [{{:keys [::spec/apps-sets] :as db} :db} [_ module {:keys [resources]}]]
    (let [modules-by-id     (->> resources (map (juxt :id identity)) (into {}))
          indexed-apps-sets (->> module
                                 :content
                                 :applications-sets
                                 (map-indexed vector))
          new-db            (reduce (partial restore-apps-selected modules-by-id)
                                    db indexed-apps-sets)
          fx                (reduce (partial load-module-configurations modules-by-id)
                                    [] indexed-apps-sets)]
      {:db new-db
       :fx fx})))

(reg-event-fx
  ::reload-apps-sets
  (fn [_ [_ module]]
    (let [apps-urls  (->> module
                          :content
                          :applications-sets
                          (mapcat :applications)
                          (map :id)
                          distinct)
          filter-str (apply general-utils/join-or (map #(str "id='" % "'") apps-urls))
          params     {:filter filter-str
                      :last   1000}
          callback   #(if (instance? js/Error %)
                        (cimi-api-fx/default-error-message % "load applications sets failed")
                        (dispatch [::reload-apps-sets-response module %]))]
      (when (seq apps-urls)
        {::cimi-api-fx/search [:module params callback]}))))


(reg-event-db
  ::add-apps-set
  (fn [db]
    (let [id (-> db
                 ::spec/apps-sets
                 utils/sorted-map-new-idx)]
      (assoc-in db [::spec/apps-sets id] {:id                  id
                                          ::spec/apps-set-name ""
                                          ::spec/apps-selector (module-selector/build-spec)}))))

(reg-event-db
  ::remove-apps-set
  (fn [db [_ id]]
    (update db ::spec/apps-sets dissoc id)))


(reg-event-db
  ::set-apps-validation-error
  (fn [db [_ key error?]]
    (utils/set-reset-error db key error? ::spec/apps-validation-errors)))

(reg-event-fx
  ::set-apps-selected
  (fn [{db :db} [_ id db-path]]
    (let [selected (module-selector/db-selected db db-path)]
      {:db (->> selected
                (map (juxt :id identity))
                (into {})
                (assoc-in db [::spec/apps-sets id ::spec/apps-selected]))
       :fx (map (fn [{module-id :id}]
                  [:dispatch [::module-plugin/load-module [::spec/apps-sets id] module-id]])
                selected)})))

(reg-event-db
  ::remove-app
  (fn [db [_ id module-id]]
    (update-in db [::spec/apps-sets id ::spec/apps-selected] dissoc module-id)))
