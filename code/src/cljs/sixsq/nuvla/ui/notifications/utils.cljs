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
        type (get-in db [::spec/notification-subscription-config :type])
        collection (get-in db [::spec/notification-subscription-config :collection])
        category (get-in db [::spec/notification-subscription-config :category])
        method (get-in db [::spec/notification-subscription-config :method])
        enabled (get-in db [::spec/notification-subscription-config :enabled])
        acl (get-in db [::spec/notification-subscription-config :acl])]
    (-> {}
        (assoc :name name)
        (assoc :description description)
        (assoc :type type)
        (assoc :collection collection)
        (assoc :category category)
        (assoc :method method)
        (assoc :enabled enabled)
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
