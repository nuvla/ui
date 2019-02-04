(ns sixsq.slipstream.webui.main.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [reg-event-db reg-event-fx]]
    [sixsq.slipstream.webui.history.effects :as history-fx]
    [sixsq.slipstream.webui.main.effects :as main-fx]
    [sixsq.slipstream.webui.main.spec :as spec]
    [taoensso.timbre :as log]))


(reg-event-db
  ::check-iframe
  (fn [db _]
    (let [location (.-location js/window)
          parent-location (.-location (.-parent js/window))
          iframe? (and location parent-location (not= location parent-location))]
      (log/info "running within iframe?" iframe?)
      (assoc db ::spec/iframe? iframe?))))


(reg-event-db
  ::set-device
  (fn [db [_ device]]
    (log/info "setting device:" device)
    (cond-> (assoc db ::spec/device device)
            (not (#{:mobile :table} device)) (assoc ::spec/sidebar-open? true))))


(reg-event-db
  ::close-sidebar
  (fn [db _]
    (assoc db ::spec/sidebar-open? false)))


(reg-event-db
  ::toggle-sidebar
  (fn [{:keys [::spec/sidebar-open?] :as db} _]
    (update db ::spec/sidebar-open? not ::spec/sidebar-open?)))


(reg-event-fx
  ::visible
  (fn [{:keys [db]} [_ v]]
    {:db                       (assoc db ::spec/visible? v)
     ::main-fx/action-interval (if v [{:action :resume}] [{:action :pause}])}))


(reg-event-fx
  ::set-navigation-info
  (fn [{:keys [db]} [_ path query-params]]
    (let [path-vec (vec (str/split path #"/"))]
      (log/info "navigation path:" path)
      (log/info "navigation query params:" query-params)
      {:db                       (merge db {::spec/nav-path         path-vec
                                            ::spec/nav-query-params query-params})
       ::main-fx/action-interval [{:action :clean}]
       ::history-fx/replace-url-history [path]})))


(reg-event-fx
  ::action-interval
  (fn [_ [_ opts]]
    {::main-fx/action-interval [opts]}))


(reg-event-fx
  ::push-breadcrumb
  (fn [{{:keys [::spec/nav-path] :as db} :db} [_ path-element]]
    {::history-fx/navigate [(str/join "/" (conj nav-path path-element))]}))


(reg-event-fx
  ::trim-breadcrumb
  (fn [{{:keys [::spec/nav-path] :as db} :db} [_ index]]
    {::history-fx/navigate [(str/join "/" (take (inc index) nav-path))]}))

(reg-event-fx
  ::open-link
  (fn [_ [_ uri]]
    {::main-fx/open-new-window [uri]}))
