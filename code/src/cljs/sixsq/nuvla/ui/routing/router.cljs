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
            [sixsq.nuvla.ui.routing.utils :as utils :refer [name->href pathify
                                                            to-pathname]]))

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


(defn- router-component-internal []
  (let [current-route @(re-frame/subscribe [::current-route])
        view          (-> current-route :data :view)
        path          @(re-frame/subscribe [::main-subs/nav-path])]
    [:div
     (when current-route
       [view (assoc current-route :path path
                                  :pathname (:path current-route))])]))

(defn router-component []
  [router-component-internal {:router router}])


(comment
  ;;;; MATCHING ;;;;

  ;; Two kinds of routes with dynamic path segments (as opposed to static paths, e.g. "ui/welcome" or "/ui/edges"):
  ;;  1. single segments: "/ui/edges/:uuid"
  ;;     -> only matches "/ui/edges/1234"
  (reitit.core/match-by-path router "/ui/edges/1234")
  ;;     => #reitit.core.Match{:template "/ui/edges/:uuid", :data {:coercion #object[reitit.coercion.spec.t_reitit$coercion$spec64688], :name :sixsq.nuvla.ui.routing.r-routes/edges-details, :view #object[sixsq$nuvla$ui$edges$views$edges_view], :link-text "edges-details"}, :result nil, :path-params {:uuid "1234"}, :path "/ui/edges/1234"}
  ;;         -> but not "/ui/edges/1234/5678"
  (reitit.core/match-by-path router "/ui/edges/1234/5678")
  ;;     => #reitit.core.Match{:template "/*sub-path", :data {:coercion #object[reitit.coercion.spec.t_reitit$coercion$spec64688], :name :sixsq.nuvla.ui.routing.r-routes/catch-all, :view #object[sixsq$nuvla$ui$unknown_resource$UnknownResource]}, :result nil, :path-params {:sub-path "ui/edges/1234/5678"}, :path "/ui/edges/1234/5678"}
  ;;        This matches the top level catch-all route "/*sub-path", which returns UnknownResource view,
  ;;        and not "/ui/edges/:uuid"


  ;;  2. catch-alls "/apps/*sub-path"
  ;;     -> matches all sub paths of apps:
  (r/match-by-path router "/ui/apps/1234")
  ;;     => #reitit.core.Match{:template "/ui/apps/*sub-path", :data {:coercion #object[reitit.coercion.spec.t_reitit$coercion$spec64688], :name :sixsq.nuvla.ui.routing.r-routes/apps-details, :view #object[sixsq$nuvla$ui$apps$views$AppDetails], :link-text "Apps"}, :result nil, :path-params {:sub-path "1234"}, :path "/ui/apps/1234"}
  (r/match-by-path router "/ui/apps/1234/5678")
  ;;     => #reitit.core.Match{:template "/ui/apps/*sub-path", :data {:coercion #object[reitit.coercion.spec.t_reitit$coercion$spec64688], :name :sixsq.nuvla.ui.routing.r-routes/apps-details, :view #object[sixsq$nuvla$ui$apps$views$AppDetails], :link-text "Apps"}, :result nil, :path-params {:sub-path "1234/5678"}, :path "/ui/apps/1234/5678"}


  ;;;; CONSTRUCTING PATHS ;;;;

  ;; Static routes
  (name->href :edges)
  ;; => "/ui/edges"

  ;; - path params get ignored with static routes
  (name->href :edges {:uuid "1234"})
  ;; => "/ui/edges"

  ;; - providing query params
  (name->href :edges nil {:hello "world", :this :is-nice})
  ;; => "/ui/edges?hello=world&this=is-nice"

  ;; Dynamic routes
  (name->href :edges-details {:uuid "1234"})
  ;; => "/ui/edges/1234"

  ;; - this works, but omits :hello "world" path parameter, because it wasn't declared for ::routes/edges-details
  (name->href :edges-details {:uuid "1234" :hello "world"})
  ;; => "/ui/edges/1234"

  ;; - providing query params
  (name->href :edges-details {:uuid "1234"} {:hello "world", :this :is-nice})
  ;; => "/ui/edges/1234?hello=world&this=is-nice"

  ;; without path-params map as second parameter, dynamic routes return nil
  ;; So this doesn't work:
  (name->href :edges-details)
  ;; => nil

  ;; ...neither does this (needs :uuid)
  (name->href :edges-details {:no-match "here"})
  ;; => nil



  ;; MORE ABOUT INTERNALS AND ONE WARNINGS

  ;; internally `rfe/push-state` matches route by calling match-by-name,
  ;; then calls:
  ;; 1) js/window.pushState with found path and
  ;; 2) provided -on-navigate handler

  ;; this navigates to "/ui/apps"
  (rfe/push-state :apps {} nil)

  ;; this navigates to "/ui/apps/sixsq"
  (rfe/push-state :apps-details {:sub-path "sixsq"} nil)


  ;; ...but be warned: this call navigates to "/ui/apps/sixsq%2Fblackbox":
  (rfe/push-state :apps-details {:sub-path "sixsq/blackbox"} nil)
  ;; ...and this to "/ui/apps/sixsq%2FNew%20Project?subtype=project":
  (rfe/push-state :apps-details {:sub-path "sixsq/New Project"} {:subtype "project"})


  ;;;; This happens because reitit uses `js/encodeURIComponent` to turn path param values into a `path`.


  ;; This has two implications:

  ;; 1. to navigate by path, we have a
  ;;    `::push-state` event dispatching `:push-state` effect which manually calls
  ;;    (.pushState js/window.history nil {} path)
  ;;    followed by
  ;;    (rfh/-on-navigate @history path)
  ;;    -> because pushState does not call popState, we have to call -on-navigate manually.
  ;;    That's how it is done in reitit source and also what they recommended via Slack.

  ;; 2. `href` works the same, so be mindful when you use it
  ;;    This means we cannot call this with a :sub-path value comprised of multiple path segments
  ;;    e.g. to navigate to "/ui/apps/this-works/perhaps/unexpected"
  (name->href :apps-details {:sub-path "this-works/perhaps/unexpected"} {:query-param "hello/world"})
  ;; => "/ui/apps/this-works%2Fperhaps%2Funexpected?query-param=hello%2Fworld"
  (name->href :apps-details {:sub-path "sixsq/blackbox"})

  ;; so construct the path using the parent route...
  (str (name->href :apps) "/" "this-works/as/expected")
  ;; => "/ui/apps/this-works/as/expected"
  (str (name->href :apps) "/" "sixsq/blackbox")

  ;; ...or with pathify helper
  (pathify [(name->href :apps) "this-works" "as" "expected"])
  ;; => "/ui/apps/this-works/as/expected"

  ;; or with to-pathname helper, which adds base-path
  (to-pathname ["apps" "this" "works" "too"])
  ;; => "/ui/apps/this/works/too"
  ;; this last one shouldn't be used in new code anymore, because
  ;; we path strings directly, but it's useful in legacy code.
  )