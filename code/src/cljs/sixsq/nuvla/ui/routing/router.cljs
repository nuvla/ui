(ns sixsq.nuvla.ui.routing.router
  (:require [clojure.string :as str]
            [re-frame.cofx :refer [reg-cofx]]
            [re-frame.core :as re-frame :refer [dispatch]]
            [reitit.core :as r]
            [reitit.frontend.controllers :as rfc]
            [reitit.frontend.easy :as rfe :refer [history]]
            [reitit.frontend.history :as rfh]
            [sixsq.nuvla.ui.config :as config]
            [sixsq.nuvla.ui.history.effects :as fx]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.routing.r-routes :as routes :refer [router]]
            [sixsq.nuvla.ui.routing.utils :refer [decode-query-string]]))

(def page-alias {"nuvlabox"        "edges"
                 "edge"            "edges"
                 "infrastructures" "clouds"
                 "deployment"      "deployments"})

(defn- strip-base-path [path]
  (-> path (str/replace-first config/base-path "")
      (str/replace #"^/|/$" "")))

(defn split-path-alias
  [path]
  (let [path (strip-base-path path)
        [page :as path-vec] (vec (str/split path #"/"))
        real-page (get page-alias page)]
    (if (and page real-page)
      (assoc path-vec 0 real-page)
      path-vec)))

;;; Effects ;;;

;; Triggering navigation from events using js/window.history.pushState
(re-frame/reg-fx
  :push-state
  (fn [path]
    (js/console.error "path" path)
    (.pushState js/window.history nil {} path)
    (rfh/-on-navigate @history path)))


;; Triggering navigation from events using reitit push state effects handler
;; route is a vector: [route-name path-params query-params]
;; route-name: name as configured in sixsq.nuvla.ui.routing.r-routes/r-routes
;; path-params: map of path keys to values, e.g. a ["apps/:id"] has path-params of {:id "some"} for path "apps/some"
;; query-params: map of path keys to values, e.g. a ["apps/:id"] has path-params of {:id "some"} for path "apps/some"
(re-frame/reg-fx
  :push-state-reitit
  (fn [route]
    (js/console.error "ROUTE" route)
    (apply rfe/push-state route)))


(re-frame/reg-event-fx
  ::push-state-by-path
  (fn [_ [_ new-path]]
    {:push-state new-path}))

(re-frame/reg-event-fx
  ::push-state-reitit
  (fn [_ [_ & route]]
    {:push-state-reitit route}))

(reg-cofx
 :get-path-parts-and-search-map
 (fn [coeffects]
   (let [location (.-location js/window)
         path-name (.-pathname location)
         _ (js/console.error path-name)
         path-parts   (->> (split-path-alias (.-pathname location))
                           (map js/decodeURIComponent)
                           vec)
         query-params (decode-query-string (.-search location))]
     (assoc coeffects
       :path-parts   path-parts
       :query-params query-params))))


(re-frame/reg-event-fx
  ::navigated
  (fn [{db :db} [_ new-match]]
    (let [old-match   (:current-route db)
          controllers (rfc/apply-controllers (:controllers old-match) new-match)
          new-match-with-controllers (assoc new-match :controllers controllers)]
      (js/console.log "NAVIGATING with path-raw" (:path new-match))
      {:db
       (-> db (assoc :current-route new-match-with-controllers))
       :fx [[:dispatch [::main-events/set-navigation-info]]]
       ::fx/set-window-title [(strip-base-path (:path new-match))]})))

;;; Subscriptions ;;;
(re-frame/reg-sub
  ::current-route
  (fn [db]
    (:current-route db)))


(defn href
  "Return relative url for given route. Url can be used in HTML links."
  ([k]
   (href k nil nil))
  ([k params]
   (href k params nil))
  ([k params query]
   (rfe/href k params query)))


(defn on-navigate [new-match]
  (when new-match
    (js/console.error "on-navigate" new-match)
    (re-frame/dispatch [::navigated new-match])))


(defn init-routes! []
  (rfe/start!
    router
    on-navigate
    {:use-fragment false
     :ignore-anchor-click? (fn [router e el uri]
                             (and (rfh/ignore-anchor-click? router e el uri)
                               (not= "false" (.getAttribute el "data-reitit-handle-click"))))}))

(defn nav [{:keys [router current-route]}]
  [:ul
   (for [route-name (r/route-names router)
         :let       [route (r/match-by-name router route-name)
                     text (-> route :data :link-text)]]
     [:li {:key route-name}
      (when (= route-name (-> current-route :data :name))
        "> ")
      [:a {:href (href route-name)} text]])])

(defn- router-component-internal []
  (let [current-route @(re-frame/subscribe [::current-route])
        view          (-> current-route :data :view)
        path          @(re-frame/subscribe [::main-subs/nav-path])]
    [:div
     (when current-route
       [view (assoc current-route :path path)])]))

(defn router-component []
  [router-component-internal {:router router}])


(comment (re-frame/dispatch [::push-state-reitit :r-routes/deployments]))

(comment
  (rfe/push-state :sixsq.nuvla.ui.routing.r-routes/apps {} nil)

  (rfe/push-state :sixsq.nuvla.ui.routing.r-routes/apps-details {:apps-path "hello/world"} {})


  (.pushState js/window.history nil {} "/ui/apps")
  ;; (-on-navigate history "/ui/apps/sixsq/blackbox?version=28")
  (rfh/-on-navigate @history "/ui/apps")
  )