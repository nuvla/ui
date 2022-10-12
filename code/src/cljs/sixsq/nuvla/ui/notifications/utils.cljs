(ns sixsq.nuvla.ui.notifications.utils
  (:require
    [sixsq.nuvla.ui.notifications.spec :as spec]))


(defn db->new-notification-method
  [db]
  (let [name (get-in db [::spec/notification-method :name])
        description (get-in db [::spec/notification-method :description])
        method (get-in db [::spec/notification-method :method])
        destination (get-in db [::spec/notification-method :destination])
        acl (get-in db [::spec/notification-method :acl])]
       {:name name
        :description description
        :method method
        :destination destination
        :acl acl}))

(defn data-record-content-type
  [m]
  (and (= "data-record" (:resource-kind m)) (= "content-type" (get-in m [:criteria :metric]))))

(defn view->model
  [v]
  (cond
    (data-record-content-type v) (-> v
                                     (assoc :resource-kind "event")
                                     (assoc-in [:criteria :metric] "tag")
                                     (assoc :resource-filter (str "tag='" (get-in v [:criteria :value]) "'")))
    :else v))

(defn event-tag
  [m]
  (and (= "event" (:resource-kind m)) (= "tag" (get-in m [:criteria :metric]))))

(defn model->view
  [m]
  (cond
    (event-tag m) (-> m
                      (assoc :resource-kind "data-record")
                      (assoc-in [:criteria :metric] "content-type"))
    :else m))

(def metrics-with-reset-windows #{"network-rx" "network-tx"})

(defn- clean-criteria
  [criteria]
  (let [metric-name (criteria :metric)
        cr-cleaned (dissoc criteria :reset-in-days)]
    (cond
      (metrics-with-reset-windows metric-name) (if (= (:reset-interval cr-cleaned) "month")
                                                 cr-cleaned
                                                 (dissoc cr-cleaned :reset-start-date))
      (= "disk" metric-name) (dissoc cr-cleaned :reset-interval :reset-start-date)
      :else (dissoc cr-cleaned :dev-name :reset-interval :reset-start-date))))

(defn db->new-subscription-config
  [db]
  (let [name (get-in db [::spec/notification-subscription-config :name])
        description (get-in db [::spec/notification-subscription-config :description])
        resource-kind (or (get-in db [::spec/notification-subscription-config :collection])
                          (get-in db [::spec/notification-subscription-config :resource-kind]))
        resource-filter (get-in db [::spec/notification-subscription-config :resource-filter] "")
        category (get-in db [::spec/notification-subscription-config :category])
        method-ids (get-in db [::spec/notification-subscription-config :method-ids])
        enabled (get-in db [::spec/notification-subscription-config :enabled])
        criteria (get-in db [::spec/notification-subscription-config :criteria])
        criteria (clean-criteria criteria)
        acl (get-in db [::spec/notification-subscription-config :acl])]
       {:name name
        :description description
        :enabled enabled
        :method-ids method-ids
        :resource-kind resource-kind
        :resource-filter resource-filter
        :category category
        :criteria criteria
        :acl acl}))


(defn db->new-subscription
  [db]
  (let [name (get-in db [::spec/subscription :name])
        description (get-in db [::spec/subscription :description])
        type (get-in db [::spec/subscription :type])
        kind (get-in db [::spec/subscription :kind])
        category (get-in db [::spec/subscription :category])
        resource (get-in db [::spec/subscription :resource])
        method (get-in db [::spec/subscription :method])
        status (get-in db [::spec/subscription :status])
        acl (get-in db [::spec/subscription :acl])]
       {:name name
        :description description
        :type type
        :kind kind
        :category category
        :method method
        :resource resource
        :status status
        :acl acl}))
