(ns sixsq.nuvla.ui.routing.router
  (:require [clojure.string :as str]
            [re-frame.cofx :refer [reg-cofx]]
            [re-frame.core :as re-frame]
            [reitit.core :as r]
            [reitit.frontend.controllers :as rfc]
            [reitit.frontend.easy :as rfe :refer [history]]
            [reitit.frontend.history :as rfh]
            [sixsq.nuvla.ui.config :as config]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.main.spec :as main-spec]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.routing.effects :as fx]
            [sixsq.nuvla.ui.routing.routes :refer [router]]
            [sixsq.nuvla.ui.routing.utils :as utils]))

(def page-alias {"nuvlabox"        "edges"
                 "edge"            "edges"
                 "infrastructures" "clouds"
                 "deployment"      "deployments"})

(defn- strip-base-path [path]
  (-> path (str/replace-first config/base-path "")
      (str/replace #"^/|/$" "")))

(defn split-path-alias
  [path]
  (let [path      (strip-base-path path)
        [page :as path-vec] (vec (str/split path #"/"))
        real-page (get page-alias page)]
    (if (and page real-page)
      (assoc path-vec 0 real-page)
      path-vec)))

;;; Effects ;;;

;; Triggering navigation from events by using js/window.history.pushState directly,
;; expects a string as path argument
(re-frame/reg-fx
  :push-state
  (fn [path]
    ;; .pushState does not call popState, that's why we have to call rfh/-on-navigate
    ;; when navigating by raw path (from reitit source)
    (.pushState js/window.history nil {} path)
    (rfh/-on-navigate @history path)))

(re-frame/reg-event-fx
  ::push-state-by-path
  (fn [_ [_ new-path]]
    {:push-state new-path}))


(reg-cofx
  :get-path-parts-and-search-map
  (fn [coeffects]
    (let [location     (.-location js/window)
          path-name    (.-pathname location)
          path-parts   (->> (split-path-alias path-name)
                            (map js/decodeURIComponent)
                            vec)
          query-params (utils/decode-query-string (.-search location))]
      (assoc coeffects
        :path-parts path-parts
        :query-params query-params))))

(re-frame/reg-fx
  :navigate-back!
  (fn []
    (.back js/window.history)))

(re-frame/reg-event-fx
  ::navigate-back
  (fn []
    {:fx [[:navigate-back!]]}))


(re-frame/reg-event-fx
  ::navigated
  [(re-frame/inject-cofx :get-path-parts-and-search-map)]
  (fn [{db           :db
        path-parts   :path-parts
        query-params :query-params} [_ new-match]]
    (let [old-match                  (:current-route db)
          controllers                (rfc/apply-controllers (:controllers old-match) new-match)
          new-match-with-controllers (assoc new-match :controllers controllers)]
      {:db
       (-> db (assoc :current-route new-match-with-controllers
                     ::main-spec/nav-path path-parts
                     ::main-spec/nav-query-params query-params))
       :fx                   [[:dispatch [::main-events/bulk-actions-interval-after-navigation]]]
       ::fx/set-window-title [(strip-base-path (:path new-match))]})))

(re-frame/reg-event-fx
  ;; In case of normal anchor tag click, we do not fire ::history-events/navigate
  ;; but let reitit/browser handle the .pushState to the history stack,
  ;; which then fires `on-navigate` after URL changed already.
  ;; That's why we test here for changes-protection? (which we also do in ::history-events/navigate)
  ;; and revert by navigating back if changes-protection? is true.
  ::navigated-protected
  (fn [{{:keys [::main-spec/changes-protection?
                ::ignore-changes-protection] :as db} :db} [_ new-match]]
    (let [new-db (assoc db ::ignore-changes-protection false)
          event  {:fx [[:dispatch [::navigated new-match]]]
                  :db new-db}
          revert {:fx [[:dispatch [::navigate-back]]]
                  :db new-db}]
      (if (and changes-protection? (not ignore-changes-protection))
        {:db (assoc db
               ::main-spec/ignore-changes-modal event
               ::main-spec/do-not-ignore-changes-modal revert

               ;; In case of not confirming ignore-chagnes-modal,
               ;; `revert` event navigates back, again triggering this
               ;; protected naviation event: ::ingore-changes-protection temporarily
               ;; disables the protection.
               ::ignore-changes-protection true)}
        (merge {:db (assoc db ::ignore-changes-protection false)}
               event)))))

;;; Subscriptions ;;;
(re-frame/reg-sub
  ::current-route
  (fn [db]
    (:current-route db)))


(defn on-navigate [new-match]
  (when new-match
    (re-frame/dispatch [::navigated-protected new-match])))


(defn init-routes! []
  (rfe/start!
    router
    on-navigate
    {:use-fragment         false
     :ignore-anchor-click? (fn [router e el uri]
                             (and (rfh/ignore-anchor-click? router e el uri)
                                  (not= "false" (.getAttribute el "data-reitit-handle-click"))))}))


(defn router-component []
  (let [current-route @(re-frame/subscribe [::current-route])
        view          (-> current-route :data :view)
        path          @(re-frame/subscribe [::main-subs/nav-path])]
    [:div
     (when current-route
       [view (assoc current-route :path path
                                  :pathname (:path current-route))])]))
