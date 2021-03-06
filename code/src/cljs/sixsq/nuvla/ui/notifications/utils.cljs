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
    (-> {}
        (assoc :name name)
        (assoc :description description)
        (assoc :method method)
        (assoc :destination destination)
        (assoc :acl acl))))


(defn db->new-subscription-config
  [db]
  (let [name (get-in db [::spec/notification-subscription-config :name])
        description (get-in db [::spec/notification-subscription-config :description])
        resource-kind (or (get-in db [::spec/notification-subscription-config :collection])
                          (get-in db [::spec/notification-subscription-config :resource-kind]))
        resource-filter (get-in db [::spec/notification-subscription-config :resource-filter] "")
        category (get-in db [::spec/notification-subscription-config :category])
        method-id (get-in db [::spec/notification-subscription-config :method-id])
        enabled (get-in db [::spec/notification-subscription-config :enabled])
        criteria (get-in db [::spec/notification-subscription-config :criteria])
        criteria (if (= "boolean" (:kind criteria))
                   (assoc criteria :value "true")
                   criteria)
        acl (get-in db [::spec/notification-subscription-config :acl])]
    (-> {}
        (assoc :name name)
        (assoc :description description)
        (assoc :enabled enabled)
        (assoc :method-id method-id)
        (assoc :resource-kind resource-kind)
        (assoc :resource-filter resource-filter)
        (assoc :category category)
        (assoc :criteria criteria)
        (assoc :acl acl))))


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
    (-> {}
        (assoc :name name)
        (assoc :description description)
        (assoc :type type)
        (assoc :kind kind)
        (assoc :category category)
        (assoc :method method)
        (assoc :resource resource)
        (assoc :status status)
        (assoc :acl acl))))
