(ns sixsq.nuvla.ui.routing.r-routes
  (:require [re-frame.core :as re-frame]
            [reitit.coercion.spec :as rss]
            [reitit.core :as r]
            [reitit.exception :as exception]
            [reitit.frontend :as rf]
            [sixsq.nuvla.ui.about.views :refer [about]]
            [sixsq.nuvla.ui.apps.views :as app-views]
            [sixsq.nuvla.ui.cimi.views :refer [api-view]]
            [sixsq.nuvla.ui.clouds.views :refer [clouds-view]]
            [sixsq.nuvla.ui.credentials.views :refer [credentials-view]]
            [sixsq.nuvla.ui.dashboard.views :refer [dashboard-view]] ;; [sixsq.nuvla.ui.data.views :refer [data-view]]
            [sixsq.nuvla.ui.data-set.views :as data-set-views]
            [sixsq.nuvla.ui.data.views :refer [data-view]]
            [sixsq.nuvla.ui.deployment-sets.views :refer [deployment-sets-view]]
            [sixsq.nuvla.ui.deployments-detail.views :refer [DeploymentDetails]]
            [sixsq.nuvla.ui.deployments.views :refer [deployments-view]]
            [sixsq.nuvla.ui.edges.views :refer [DetailedView edges-view]]
            [sixsq.nuvla.ui.notifications.views :refer [notifications-view]]
            [sixsq.nuvla.ui.panel :refer [UnknownResource]]
            [sixsq.nuvla.ui.session.views :as session-views]
            [sixsq.nuvla.ui.welcome.views :refer [home-view]]))

;;; Views ;;;

(defn home-page []
  [:div
   [:h1 "This is home page"]
   [:button
    ;; Dispatch navigate event that triggers a (side)effect.
    {:on-click #(re-frame/dispatch [:sixsq.nuvla.ui.routing.router/push-state ::sub-page2])}
    "Go to sub-page 2"]])

(defn sub-page1 []
  [:div
   [:h1 "This is sub-page 1"]])

(defn sub-page2 []
  [:div
   [:h1 "This is sub-page 2"]])


(defn SessionPageWelcomeRedirect
  []
  [session-views/SessionPage true])

(defn SessionPageWithoutWelcomeRedirect
  []
  [session-views/SessionPage false])

(def r-routes
  [""
   {:name ::root
    :view home-view}
   ["/"]
   ["/ui/"
    [""
     {:name ::home-root
      :link-text "Home"}]
    ["sign-up"
     {:name ::sign-up
      :view SessionPageWelcomeRedirect
      :link-text "Sign up"}]
    ["sign-in"
     {:name ::sign-in
      :view SessionPageWelcomeRedirect
      :link-text "login"}]
    ["reset-password"
     {:name ::reset-password
      :view SessionPageWelcomeRedirect
      :link-text "Reset password"}]
    ["set-password"
     {:name ::set-password
      :view SessionPageWithoutWelcomeRedirect
      :link-text "Set password"}]
    ["sign-in-token"
     {:name ::sign-in-token
      :view SessionPageWelcomeRedirect
      :link-text "sign in token"}]
    ["about"
     {:name ::about
      :view about
      :link-text "About"}]
    ["welcome"
     {:name ::home
      :link-text "home"}]
    ["dashboard"
     {:name ::dashboard
      :view dashboard-view
      :link-text "dashboard"}]
    ["apps"
     {:name ::apps
      :view app-views/AppsOverview
      :link-text "Apps"}
     [""]
     ["/*apps-path"
      {:name ::apps-details
       :view app-views/AppDetails}]]
    ["deployments"
     {:name ::deployments
      :view deployments-view
      :link-text "deployments"}]
    ["deployment/:id"
     {:name ::deployment
      :view DeploymentDetails}]
    ["deployment-sets"
     {:name ::deployment-sets
      :view deployment-sets-view
      :link-text "deployment-sets"}]
    ["edges"
     {:name ::edges
      :view edges-view
      :link-text "edges"}]
    ["edges/:id"
     {:name ::edges-details
      :view DetailedView
      :link-text "edges-details"}]
    ["credentials"
     {:name ::credentials
      :view credentials-view
      :link-text "credentials"}]
    ["notifications"
     {:name ::notifications
      :view notifications-view
      :link-text "notifications"}]
    ["data"
     {:name ::data
      :view data-view
      :link-text "data"}]
    ["data/*uuid"
     {:name ::data-details
      :view data-set-views/DataSet}]
    ["clouds"
     {:name ::clouds
      :view clouds-view
      :link-text "clouds"}]
    ["api"
     {:name ::api
      :view api-view
      :link-text "api"}]]
   ["/*resource"
    {:name ::catch-all
     :view UnknownResource}]])

(def router
  (rf/router
    r-routes
    {:data {:coercion rss/coercion}
     :router r/linear-router
     :conflicts (fn [conflicts]
                  (println (exception/format-exception :path-conflicts nil conflicts)))
     }))
  ;; => #object[reitit.core.t_reitit$core60844]



(comment

    (r/match-by-path router "/ui/apps/")

  (let [router (rf/router
                 ["apps"
                  {:name :bla
                   :views [:yeah]
                   :conflicting true}
                  ["/" :dadada]
                  ["/yeah" :a]
                  ["/hello" :blo]
                  ["/*path"]
                  ]

                 {
                  ;; :router r/linear-router
                  ;; :conflicts (fn [conflicts]
                  ;;              (println (exception/format-exception :path-conflicts nil conflicts)))
                  })]

    (r/match-by-path router "apps/yeah/h")
    #_(r/router-name router))

 (-> (rf/router
       [["/ping" ::ping]
        ["/api" ::api]
        ["/api/fix" ::api-fix]
        ["/api/:users" ::users]]
       {:router r/quarantine-router})
     r/router-name)

  (r/match-by-path router "/ui/apps")



  (def router-test2
    (r/router
      ["/api"
       ["/ping" ::ping]
       ["/user/:id" ::user]]))

  (r/match-by-path router-test2 "/api"))
