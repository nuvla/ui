(ns sixsq.nuvla.ui.pages.clouds.utils
  (:require [sixsq.nuvla.ui.pages.clouds.spec :as spec]))

(defn db->new-service-group
  [db]
  (let [name        (get-in db [::spec/infra-service :name])
        description (get-in db [::spec/infra-service :description])]
    {:name        name
     :description description}))

(defn db->new-service
  [{{:keys [name description parent subtype endpoint acl]} ::spec/infra-service :as _db}]
  {:template (cond-> {:href        "infrastructure-service-template/generic"
                      :name        name
                      :description description
                      :parent      parent
                      :subtype     subtype
                      :endpoint    endpoint}
                     acl (assoc :acl acl))})

(defn swarm-manager?
  [{:keys [swarm-enabled swarm-manager] :as _infra-service}]
  (and (true? swarm-enabled)
       (or (true? swarm-manager)
           (nil? swarm-manager))))

(defn swarm-worker?
  [{:keys [swarm-enabled swarm-manager] :as _infra-service}]
  (and (true? swarm-enabled)
       (false? swarm-manager)))

(defn swarm-disabled?
  [{:keys [swarm-enabled] :as _infra-service}]
  (false? swarm-enabled))
