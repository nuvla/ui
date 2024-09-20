(ns sixsq.nuvla.ui.app.view
  (:require [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.intercom.views :as intercom]
            [sixsq.nuvla.ui.main.components :as main-components]
            [sixsq.nuvla.ui.main.subs :as subs]
            [sixsq.nuvla.ui.common-components.plugins.table-refactor :as table-refactor]
            [sixsq.nuvla.ui.main.views :as main-views]
            [sixsq.nuvla.ui.main.views-sidebar :as sidebar]
            [sixsq.nuvla.ui.pages.apps.apps-applications-sets.views]
            [sixsq.nuvla.ui.pages.cimi.subs :as api-subs]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.subs :as route-subs]
            [sixsq.nuvla.ui.routing.utils :as routing-utils]
            [sixsq.nuvla.ui.session.events :as session-events]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.session.views :as session-views]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(defn WatcherRedirectProtectedPage
  []
  (when (and @(subscribe [::route-subs/protected-page?])
             (not @(subscribe [::session-subs/session])))
    (dispatch [::routing-events/navigate routes/sign-in nil
               {:redirect (-> @(subscribe [::route-subs/current-route])
                              (routing-utils/gen-href nil)
                              routing-utils/strip-base-path)}])))

(defn FollowRedirect
  []
  (let [redirect @(subscribe [::route-subs/query-param :redirect])]
    (dispatch [::routing-events/navigate
               (or (some-> redirect js/decodeURIComponent)
                   (routing-utils/name->href routes/home))])))

(defn SessionRedirector
  []
  [:<>
   (let [session @(subscribe [::session-subs/session])]
     (if session
      [FollowRedirect]
      [WatcherRedirectProtectedPage]))])

(defn RouterView []
  (let [CurrentView   @(subscribe [::route-subs/current-view])
        current-route @(subscribe [::route-subs/current-route])
        path          @(subscribe [::route-subs/nav-path])]
    (when current-route
      [CurrentView
       (assoc current-route :path path
                            :pathname (:path current-route))])))

(defn LayoutAuthentication []
  (dispatch [::session-events/init])
  [:<>
   [SessionRedirector]
   [ui/Grid {:stackable true
             :columns   2
             :style     {:margin           0
                         :background-color "white"
                         :padding          0}}
    [ui/GridColumn {:class "login-left"}
     [session-views/LeftPanel]]
    [ui/GridColumn
     [RouterView]]]])

(defn LayoutPage []
  [:<>
   [WatcherRedirectProtectedPage]
   [intercom/Widget]
   [sidebar/Menu]
   [main-views/MainDiv
    [:<>
     [main-views/Header]
     [:div {:ref main-components/ref}
      [main-views/MessageSubscriptionCanceled]
      [main-views/Contents [RouterView]]
      [main-views/IgnoreChangesModal]
      [main-views/SubscriptionRequiredModal]
      [main-views/Footer]]]]])

(defn Loader []
  (let [tr     (subscribe [::i18n-subs/tr])
        error? (subscribe [::api-subs/cloud-entry-point-error?])]
    [ui/Container
     [ui/Loader {:active true :size "massive"}
      (when @error?
        [ui/Header {:text-align :center
                    :as         :h2
                    :content    (@tr [:service-unavailable])
                    :subheader  (@tr [:take-coffee-back-soon])}])]]))

(defn Render []
  (let [Layout @(subscribe [::route-subs/current-layout])]
    [:div {:id "nuvla-ui-main"}
     [Layout]]))

(defn App []
  #_(if (subscribe [::subs/app-loading?])
    [Loader]
    [Render])
  [table-refactor/TableController])