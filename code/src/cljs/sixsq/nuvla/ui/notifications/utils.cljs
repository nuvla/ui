(ns sixsq.nuvla.ui.notifications.utils
  (:require [clojure.string :as str]
            [sixsq.nuvla.ui.notifications.spec :as spec]))

(def ^:const cpu-load "load")
(def ^:const ram "ram")
(def ^:const disk "disk")
(def ^:const state "state")
(def ^:const network-rx "network-rx")
(def ^:const network-tx "network-tx")
(def ^:const status "status")
(def ^:const content-type "content-type")
(def ^:const app-publish-app-bq "app-publish-app-bq")
(def ^:const app-publish-deployment "app-publish-deployment")

(def ^:const event-module-publish "module.publish")
(def ^:const event-module-publish-app-bouquet (str/join "." [event-module-publish "apps-bouquet"]))
(def ^:const event-module-publish-deployment (str/join "." [event-module-publish "deployment"]))

(defn app-publish-metric?
  [metric]
  (contains? #{app-publish-app-bq app-publish-deployment} metric))

(def app-publish->resource-kind
  {app-publish-app-bq "apps-bouquet"
   app-publish-deployment "deployment"})

(def resource-kind->app-publish
  {"apps-bouquet" app-publish-app-bq
   "deployment" app-publish-deployment})


(defn app-publish->criteria
  [criteria-metric]
  {:metric "name"
   :kind "string"
   :condition "is"
   :value (str event-module-publish "." (get app-publish->resource-kind criteria-metric))})

(defn app-publish->name
  [target]
  (case target
    app-publish-app-bq "App Published for App Bouquet"
    app-publish-deployment "App Published for Deployment"
    nil))

(defn metric-condition-exclude
  [collection metric]
  (or (and (= collection "nuvlabox") (= metric "state"))
      (and (= collection "application") (= metric "name"))))

(defn metric-condition-exclude-defaults
  [collection metric]
  (cond
    (and (= collection "nuvlabox") (= metric "state")) "no"
    (and (= collection "application") (= metric "name")) "is"))

(defn db->new-notification-method
  [db]
  (let [name        (get-in db [::spec/notification-method :name])
        description (get-in db [::spec/notification-method :description])
        method      (get-in db [::spec/notification-method :method])
        destination (get-in db [::spec/notification-method :destination])
        acl         (get-in db [::spec/notification-method :acl])]
    {:name        name
     :description description
     :method      method
     :destination destination
     :acl         acl}))

(defn data-record-content-type
  [m]
  (and (= "data-record" (:resource-kind m)) (= content-type (get-in m [:criteria :metric]))))

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
                      (assoc-in [:criteria :metric] content-type))
    :else m))

(def metrics-with-reset-windows #{network-rx network-tx})

(defn- clean-criteria
  [criteria]
  (let [metric-name (criteria :metric)
        cr-cleaned  (dissoc criteria :reset-in-days)]
    (cond
      (metrics-with-reset-windows metric-name) (if (= (:reset-interval cr-cleaned) "month")
                                                 cr-cleaned
                                                 (dissoc cr-cleaned :reset-start-date))
      (= disk metric-name) (dissoc cr-cleaned :reset-interval :reset-start-date)
      :else (dissoc cr-cleaned :dev-name :reset-interval :reset-start-date))))

(defn- db->resource-kind
  [db]
  (let [resource-kind (get-in db [::spec/notification-subscription-config :resource-kind])]
    (if (contains? resource-kind->app-publish resource-kind)
      resource-kind
      (or (get-in db [::spec/notification-subscription-config :collection])
          resource-kind))))

(defn db->new-subscription-config
  [db]
  (let [name            (get-in db [::spec/notification-subscription-config :name])
        description     (get-in db [::spec/notification-subscription-config :description])
        resource-kind   (db->resource-kind db)
        resource-filter (get-in db [::spec/notification-subscription-config :resource-filter] "")
        category        (get-in db [::spec/notification-subscription-config :category])
        method-ids      (get-in db [::spec/notification-subscription-config :method-ids])
        enabled         (get-in db [::spec/notification-subscription-config :enabled])
        criteria        (get-in db [::spec/notification-subscription-config :criteria])
        criteria        (clean-criteria criteria)
        acl             (get-in db [::spec/notification-subscription-config :acl])]
    {:name            name
     :description     description
     :enabled         enabled
     :method-ids      method-ids
     :resource-kind   resource-kind
     :resource-filter resource-filter
     :category        category
     :criteria        criteria
     :acl             acl}))


(defn db->new-subscription
  [db]
  (let [name        (get-in db [::spec/subscription :name])
        description (get-in db [::spec/subscription :description])
        type        (get-in db [::spec/subscription :type])
        kind        (get-in db [::spec/subscription :kind])
        category    (get-in db [::spec/subscription :category])
        resource    (get-in db [::spec/subscription :resource])
        method      (get-in db [::spec/subscription :method])
        status      (get-in db [::spec/subscription :status])
        acl         (get-in db [::spec/subscription :acl])]
    {:name        name
     :description description
     :type        type
     :kind        kind
     :category    category
     :method      method
     :resource    resource
     :status      status
     :acl         acl}))
