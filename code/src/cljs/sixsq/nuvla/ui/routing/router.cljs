(ns sixsq.nuvla.ui.routing.router
  (:require [re-frame.core :refer [dispatch]]
            [reitit.coercion.spec :as rss]
            [reitit.core :as r]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.frontend.history :as rfh]
            [sixsq.nuvla.ui.app.view :refer [LayoutAuthentication
                                             LayoutPage]]
            [sixsq.nuvla.ui.common-components.notifications.views :refer [notifications-view]]
            [sixsq.nuvla.ui.config :refer [base-path]]
            [sixsq.nuvla.ui.pages.about.views :refer [About]]
            [sixsq.nuvla.ui.pages.apps.views :as app-views]
            [sixsq.nuvla.ui.pages.cimi.views :refer [ApiView]]
            [sixsq.nuvla.ui.pages.clouds.views :refer [clouds-view]]
            [sixsq.nuvla.ui.pages.credentials.views :refer [credentials-view]]
            [sixsq.nuvla.ui.pages.dashboard.views :refer [dashboard-view]]
            [sixsq.nuvla.ui.pages.data-set.views :as data-set-views]
            [sixsq.nuvla.ui.pages.data.views :refer [data-view]]
            [sixsq.nuvla.ui.pages.deployments-detail.views :refer [DeploymentDetails]]
            [sixsq.nuvla.ui.pages.deployments.routes
             :refer [deployment-sets-details-view deployment-sets-view deployments-view]]
            [sixsq.nuvla.ui.pages.docs.views :refer [documentation]]
            [sixsq.nuvla.ui.pages.edges.views :refer [DetailedViewPage edges-view]]
            [sixsq.nuvla.ui.pages.edges.views-cluster :as views-cluster]
            [sixsq.nuvla.ui.pages.profile.views :refer [profile]]
            [sixsq.nuvla.ui.pages.groups.views :refer [GroupsViewPage]]
            [sixsq.nuvla.ui.pages.welcome.views :refer [home-view]]
            [sixsq.nuvla.ui.routing.events :as events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.utils :as utils]
            [sixsq.nuvla.ui.session.reset-password-views :as reset-password-views]
            [sixsq.nuvla.ui.session.set-password-views :as set-password-views]
            [sixsq.nuvla.ui.session.sign-in-views :as sign-in-views]
            [sixsq.nuvla.ui.session.sign-up-views :as sign-up-views]
            [sixsq.nuvla.ui.unknown-resource :refer [UnknownResource]]))

(defn- create-route-name
  ([page-alias]
   (create-route-name page-alias nil))
  ([page-alias suffix]
   (keyword (str (some-> (namespace ::routes/edges) (str "/")) page-alias suffix))))

(def edges-routes
  (mapv (fn [page-alias]
          [[page-alias
            {:name       (create-route-name page-alias)
             :layout     #'LayoutPage
             :view       #'edges-view
             :protected? true
             :dict-key   :edges}
            [""]
            ["/" (create-route-name page-alias "-slashed")]]
           [(str page-alias "/:uuid")
            {:name   (create-route-name page-alias "-details")
             :layout #'LayoutPage
             :view   #'DetailedViewPage}]
           [(str page-alias "/nuvlabox-cluster/:uuid")
            {:name   (create-route-name page-alias "-cluster-details")
             :layout #'LayoutPage
             :view   #'views-cluster/ClusterViewPage}]])
        (utils/canonical->all-page-names "edges")))

(def groups-routes
  (mapv (fn [page-alias]
          [page-alias
           {:name       (create-route-name page-alias)
            :layout     #'LayoutPage
            :view       #'GroupsViewPage
            :protected? true
            :dict-key   :groups}
           [""]
           ["/" (create-route-name page-alias "-slashed")]
           ["/:uuid"
            {:name   (create-route-name page-alias "-details")
             :layout #'LayoutPage
             :view   #'GroupsViewPage}]])
        (utils/canonical->all-page-names "groups")))

(def cloud-routes
  (mapv (fn [page-alias]
          [page-alias
           {:name       (create-route-name page-alias)
            :layout     #'LayoutPage
            :view       #'clouds-view
            :protected? true
            :dict-key   :clouds}
           [""]
           ["/" (create-route-name page-alias "-slashed")]
           ["/:uuid"
            {:name   (create-route-name page-alias "-details")
             :layout #'LayoutPage
             :view   #'clouds-view}]])
        (utils/canonical->all-page-names "clouds")))

(def deployment-routes
  (mapv (fn [page-alias]
          [page-alias
           {:name       (create-route-name page-alias)
            :layout     #'LayoutPage
            :view       #'deployments-view
            :protected? true
            :dict-key   :deployments}
           [""]
           ["/" (create-route-name page-alias "-slashed")]
           ["/:uuid"
            {:name   (create-route-name page-alias "-details")
             :layout #'LayoutPage
             :view   #'DeploymentDetails}]])
        (utils/canonical->all-page-names "deployments")))

(def deployment-group-routes
  (mapv (fn [page-alias]
          [page-alias
           {:name     (create-route-name page-alias)
            :layout   #'LayoutPage
            :view     #'deployment-sets-view
            :dict-key :deployment-groups}
           [""]
           ["/" (create-route-name page-alias "-slashed")]
           ["/:uuid"
            {:name   (create-route-name page-alias "-details")
             :layout #'LayoutPage
             :view   #'deployment-sets-details-view}]])
        (utils/canonical->all-page-names "deployment-groups")))

(def r-routes
  [""
   {:name        ::routes/root
    :layout      #'LayoutPage
    :view        #'home-view}
   ["/"]
   [(str base-path "/")                                     ;; sixsq.nuvla.ui.config/base-path = "/ui" on nuvla.io
    [""
     {:name      ::routes/home-root
      :link-text "Home"}]
    edges-routes
    cloud-routes
    deployment-routes
    deployment-group-routes
    groups-routes
    ["sign-up"
     {:name          ::routes/sign-up
      :layout        #'LayoutAuthentication
      :view          #'sign-up-views/Form
      :link-text     "Sign up"}]
    ["sign-in"
     {:name          ::routes/sign-in
      :layout        #'LayoutAuthentication
      :view          #'sign-in-views/Form
      :link-text     "login"}]
    ["reset-password"
     {:name          ::routes/reset-password
      :layout        #'LayoutAuthentication
      :view          #'reset-password-views/Form
      :link-text     "Reset password"}]
    ["set-password"
     {:name          ::routes/set-password
      :layout        #'LayoutAuthentication
      :view          #'set-password-views/Form
      :link-text     "Set password"}]
    ["sign-in-token"
     {:name          ::routes/sign-in-token
      :layout        #'LayoutAuthentication
      :view          #'sign-in-views/FormTokenValidation
      :link-text     "sign in token"}]
    ["about"
     {:name      ::routes/about
      :layout    #'LayoutPage
      :view      #'About
      :link-text "About"}]
    ["welcome"
     {:name      ::routes/home
      :link-text "home"}]
    ["welcome/"
     {:name      ::routes/home-slash
      :link-text "home"}]
    ["dashboard"
     {:name       ::routes/dashboard
      :layout     #'LayoutPage
      :view       #'dashboard-view
      :protected? true
      :link-text  "dashboard"}]
    ["apps"
     {:name       ::routes/apps
      :layout     #'LayoutPage
      :view       #'app-views/AppsOverview
      :protected? true
      :link-text  "Apps"}
     [""]
     ["/" ::routes/apps-slashed]
     ["/*sub-path"
      {:name   ::routes/apps-details
       :layout #'LayoutPage
       :view   #'app-views/AppDetailsRoute}]]
    ["credentials"
     {:name       ::routes/credentials
      :layout     #'LayoutPage
      :view       #'credentials-view
      :protected? true
      :link-text  "credentials"}
     [""]
     ["/" ::routes/credentials-slash]]
    ["notifications"
     {:name       ::routes/notifications
      :layout     #'LayoutPage
      :view       #'notifications-view
      :protected? true
      :link-text  "notifications"}]
    ["data"
     {:name       ::routes/data
      :layout     #'LayoutPage
      :view       #'data-view
      :protected? true
      :link-text  "data"}]
    ["data/*uuid"
     {:name   ::routes/data-details
      :layout #'LayoutPage
      :view   #'data-set-views/DataSet}]
    ["documentation"
     {:name      ::routes/documentation
      :layout    #'LayoutPage
      :view      #'documentation
      :link-text "documentation"}
     [""]
     ["/*sub-path" ::routes/documentation-sub-page]]
    ["api"
     {:name      ::routes/api
      :layout    #'LayoutPage
      :view      #'ApiView
      :link-text "api"}
     [""]
     ["/" ::routes/api-slashed]
     ["/*sub-path"
      {:name ::routes/api-sub-page}]]
    ["profile"
     {:name   ::routes/profile
      :layout #'LayoutPage
      :view   #'profile}]]
   ["/*"
    {:name   ::routes/catch-all
     :layout #'LayoutPage
     :view   #'UnknownResource}]])

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
