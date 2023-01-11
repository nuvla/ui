(ns sixsq.nuvla.ui.routing.routes
  (:require [reitit.coercion.spec :as rss]
            [reitit.core :as r]
            [reitit.frontend :as rf]
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
            [sixsq.nuvla.ui.edges.views :refer [edges-view]]
            [sixsq.nuvla.ui.notifications.views :refer [notifications-view]]
            [sixsq.nuvla.ui.profile.views :refer [profile]]
            [sixsq.nuvla.ui.session.views :as session-views]
            [sixsq.nuvla.ui.unknown-resource :refer [UnknownResource]]
            [sixsq.nuvla.ui.welcome.views :refer [home-view]]))


(def alias->canonical {"nuvlabox"        "edges"
                       "edge"            "edges"
                       "infrastructures" "clouds"
                       "deployment"      "deployments"})

(defn- canonical->all-page-names
  ([canonical]
   (canonical->all-page-names canonical alias->canonical))
  ([canonical aliases-map]
   (->> aliases-map
        (filter #(= canonical (val %)))
        flatten
        set)))

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
   (keyword (str (some-> (namespace :edges) (str "/")) page-alias suffix))))

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
             :view      edges-view}]
           [(str page-alias "/nuvlabox-cluster/:uuid")
            {:name (create-route-name page-alias "-cluster-details")
             :view edges-view}]])
    (canonical->all-page-names "edges")))

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
    (canonical->all-page-names "clouds")))


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
    (canonical->all-page-names "deployments")))


(def r-routes
  [""
   {:name :root
    :view home-view}
   ["/"]
   [(str base-path "/")                                     ;; sixsq.nuvla.ui.config/base-path = "/ui" on nuvla.io
    [""
     {:name      :home-root
      :link-text "Home"}]
    edges-routes
    cloud-routes
    deployment-routes
    ["sign-up"
     {:name      :sign-up
      :view      SessionPageWelcomeRedirect
      :link-text "Sign up"}]
    ["sign-in"
     {:name      :sign-in
      :view      SessionPageWelcomeRedirect
      :link-text "login"}]
    ["reset-password"
     {:name      :reset-password
      :view      SessionPageWelcomeRedirect
      :link-text "Reset password"}]
    ["set-password"
     {:name      :set-password
      :view      SessionPageWithoutWelcomeRedirect
      :link-text "Set password"}]
    ["sign-in-token"
     {:name      :sign-in-token
      :view      SessionPageWelcomeRedirect
      :link-text "sign in token"}]
    ["about"
     {:name      :about
      :view      about
      :link-text "About"}]
    ["welcome"
     {:name      :home
      :link-text "home"}]
    ["welcome/"
     {:name      :home-slash
      :link-text "home"}]
    ["dashboard"
     {:name      :dashboard
      :view      dashboard-view
      :link-text "dashboard"}]
    ["apps"
     {:name      :apps
      :view      app-views/AppsOverview
      :link-text "Apps"}
     [""]
     ["/" :apps-slashed]
     ["/*sub-path"
      {:name :apps-details
       :view app-views/AppDetails}]]
    ["credentials"
     {:name      :credentials
      :view      credentials-view
      :link-text "credentials"}
     [""]
     ["/" :credentials-slash]]
    ["notifications"
     {:name      :notifications
      :view      notifications-view
      :link-text "notifications"}]
    ["data"
     {:name      :data
      :view      data-view
      :link-text "data"}]
    ["data/*uuid"
     {:name :data-details
      :view data-set-views/DataSet}]
    ["deployment-sets"
     {:name      :deployment-sets
      :view      deployment-sets-view
      :link-text "deployment-sets"}
     [""]
     ["/" :deployment-sets-slashed]]
    ["deployment-sets/:uuid"
     {:name      :deployment-sets-details
      :view      deployment-sets-view
      :link-text "deployment-sets"}]
    ["documentation"
     {:name      :documentation
      :view      documentation
      :link-text "documentation"}
     [""]
     ["/*sub-path" :documentation-sub-page]]
    ["api"
     {:name      :api
      :view      api-view
      :link-text "api"}
     [""]
     ["/" :api-slashed]
     ["/*sub-path"
      {:name :api-sub-page}]]
    ["profile"
     {:name :profile
      :view profile}]]
   ["/*"
    {:name :catch-all
     :view UnknownResource}]])

(def router
  (rf/router
    r-routes
    {:data      {:coercion rss/coercion}
     :router    r/linear-router
     :conflicts  nil
     #_(fn [conflicts]
       (when debug?
         (println (exception/format-exception :path-conflicts nil conflicts))))}))

(comment
  (r/match-by-name router ::nuvlabox)
  (r/match-by-name router ::nuvlabox)
  (r/match-by-name router :catch-all {":" "blabla/hello"})

  (->> (r/match-by-path router "/ui/blabla")
       :path-params
       keys
       first
       type)
  )
