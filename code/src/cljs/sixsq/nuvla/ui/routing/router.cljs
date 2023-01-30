(ns sixsq.nuvla.ui.routing.router
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reitit.coercion.spec :as rss]
            [reitit.core :as r]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.frontend.history :as rfh]
            [sixsq.nuvla.ui.about.views :refer [about]]
            [sixsq.nuvla.ui.apps.views :as app-views]
            [sixsq.nuvla.ui.cimi.views :refer [api-view]]
            [sixsq.nuvla.ui.clouds.views :refer [clouds-view]]
            [sixsq.nuvla.ui.config :refer [base-path]]
            [sixsq.nuvla.ui.credentials.views :refer [credentials-view]]
            [sixsq.nuvla.ui.dashboard.views :refer [dashboard-view]]
            [sixsq.nuvla.ui.data-set.views :as data-set-views]
            [sixsq.nuvla.ui.data.views :refer [data-view]]
            [sixsq.nuvla.ui.deployment-sets.views :refer [deployment-sets-view]]
            [sixsq.nuvla.ui.deployments-detail.views :refer [DeploymentDetails]]
            [sixsq.nuvla.ui.deployments.views :refer [deployments-view]]
            [sixsq.nuvla.ui.docs.views :refer [documentation]]
            [sixsq.nuvla.ui.edges.views :refer [DetailedViewPage edges-view]]
            [sixsq.nuvla.ui.edges.views-cluster :as views-cluster]
            [sixsq.nuvla.ui.notifications.views :refer [notifications-view]]
            [sixsq.nuvla.ui.profile.views :refer [profile]]
            [sixsq.nuvla.ui.routing.events :as events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.subs :as subs]
            [sixsq.nuvla.ui.routing.utils :as utils]
            [sixsq.nuvla.ui.session.views :as session-views]
            [sixsq.nuvla.ui.unknown-resource :refer [UnknownResource]]
            [sixsq.nuvla.ui.welcome.views :refer [home-view]]))

(defn SessionPageWelcomeRedirect
  []
  [session-views/SessionPage true])

(defn SessionPageWithoutWelcomeRedirect
  []
  [session-views/SessionPage false])

(defn- create-route-name
  ([page-alias]
   (create-route-name page-alias nil))
  ([page-alias suffix]
   (keyword (str (some-> (namespace ::routes/edges) (str "/")) page-alias suffix))))

(def edges-routes
  (mapv (fn [page-alias]
          [[page-alias
            {:name      (create-route-name page-alias)
             :view      edges-view
             :dict-key  :edges}
            [""]
            ["/" (create-route-name page-alias "-slashed")]]
           [(str page-alias "/:uuid")
            {:name      (create-route-name page-alias "-details")
             :view      DetailedViewPage}]
           [(str page-alias "/nuvlabox-cluster/:uuid")
            {:name (create-route-name page-alias "-cluster-details")
             :view views-cluster/ClusterViewPage}]])
        (utils/canonical->all-page-names "edges")))

(def cloud-routes
  (mapv (fn [page-alias]
          [page-alias
           {:name      (create-route-name page-alias)
            :view      clouds-view
            :dict-key  :clouds}
           [""]
           ["/" (create-route-name page-alias "-slashed")]
           ["/:uuid"
            {:name      (create-route-name page-alias "-details")
             :view      clouds-view}]])
        (utils/canonical->all-page-names "clouds")))


(def deployment-routes
  (mapv (fn [page-alias]
          [page-alias
           {:name      (create-route-name page-alias)
            :view      deployments-view
            :dict-key  :deployments}
           [""]
           ["/" (create-route-name page-alias "-slashed")]
           ["/:uuid"
            {:name (create-route-name page-alias "-details")
             :view DeploymentDetails}]])
        (utils/canonical->all-page-names "deployments")))


(def r-routes
  [""
   {:name ::routes/root
    :view home-view}
   ["/"]
   [(str base-path "/")                                     ;; sixsq.nuvla.ui.config/base-path = "/ui" on nuvla.io
    [""
     {:name      ::routes/home-root
      :link-text "Home"}]
    edges-routes
    cloud-routes
    deployment-routes
    ["sign-up"
     {:name      ::routes/sign-up
      :view      SessionPageWelcomeRedirect
      :link-text "Sign up"}]
    ["sign-in"
     {:name      ::routes/sign-in
      :view      SessionPageWelcomeRedirect
      :link-text "login"}]
    ["reset-password"
     {:name      ::routes/reset-password
      :view      SessionPageWelcomeRedirect
      :link-text "Reset password"}]
    ["set-password"
     {:name      ::routes/set-password
      :view      SessionPageWithoutWelcomeRedirect
      :link-text "Set password"}]
    ["sign-in-token"
     {:name      ::routes/sign-in-token
      :view      SessionPageWelcomeRedirect
      :link-text "sign in token"}]
    ["about"
     {:name      ::routes/about
      :view      about
      :link-text "About"}]
    ["welcome"
     {:name      ::routes/home
      :link-text "home"}]
    ["welcome/"
     {:name      ::routes/home-slash
      :link-text "home"}]
    ["dashboard"
     {:name      ::routes/dashboard
      :view      dashboard-view
      :link-text "dashboard"}]
    ["apps"
     {:name      ::routes/apps
      :view      app-views/AppsOverview
      :link-text "Apps"}
     [""]
     ["/" ::routes/apps-slashed]
     ["/*sub-path"
      {:name ::routes/apps-details
       :view app-views/AppDetails}]]
    ["credentials"
     {:name      ::routes/credentials
      :view      credentials-view
      :link-text "credentials"}
     [""]
     ["/" ::routes/credentials-slash]]
    ["notifications"
     {:name      ::routes/notifications
      :view      notifications-view
      :link-text "notifications"}]
    ["data"
     {:name      ::routes/data
      :view      data-view
      :link-text "data"}]
    ["data/*uuid"
     {:name ::routes/data-details
      :view data-set-views/DataSet}]
    ["deployment-sets"
     {:name      ::routes/deployment-sets
      :view      deployment-sets-view
      :link-text "deployment-sets"}
     [""]
     ["/" ::routes/deployment-sets-slashed]]
    ["deployment-sets/:uuid"
     {:name      ::routes/deployment-sets-details
      :view      deployment-sets-view
      :link-text "deployment-sets"}]
    ["documentation"
     {:name      ::routes/documentation
      :view      documentation
      :link-text "documentation"}
     [""]
     ["/*sub-path" ::routes/documentation-sub-page]]
    ["api"
     {:name      ::routes/api
      :view      api-view
      :link-text "api"}
     [""]
     ["/" ::routes/api-slashed]
     ["/*sub-path"
      {:name ::routes/api-sub-page}]]
    ["profile"
     {:name ::routes/profile
      :view profile}]]
   ["/*"
    {:name ::routes/catch-all
     :view UnknownResource}]])

(def router
  (rf/router
    r-routes
    {:data      {:coercion rss/coercion}
     :router    r/linear-router
     :conflicts nil
     #_(fn [conflicts]
         (when debug?
           (println (exception/format-exception :path-conflicts nil conflicts))))}))

(defn on-navigate [new-match]
  (when new-match
    (dispatch [::events/navigated-protected new-match])))

(defn init-routes! []
  (rfe/start!
    router
    on-navigate
    {:use-fragment         false
     :ignore-anchor-click? (fn [router e el uri]
                             (and (rfh/ignore-anchor-click? router e el uri)
                                  (not= "false" (.getAttribute el "data-reitit-handle-click"))
                                  (not= "#" (first (.getAttribute el "href")))))}))

(defn router-component []
  (let [current-route @(subscribe [::subs/current-route])
        path          @(subscribe [::subs/nav-path])
        view          (-> current-route :data :view)]
    [:<>
     (when current-route
       [view (assoc current-route :path path
                                  :pathname (:path current-route))])]))

(comment
  (r/match-by-name router ::routes/nuvlabox)
  (r/match-by-name router routes/nuvlabox)
  (r/match-by-name router routes/home)
  (r/match-by-name router ::routes/nuvlabox)
  (r/match-by-name router :catch-all {":" "blabla/hello"})

  (->> (r/match-by-path router "/ui/blabla")
       :path-params
       keys
       first
       type)
  )
