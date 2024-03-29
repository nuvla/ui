(ns sixsq.nuvla.ui.pages.deployments.routes
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.pages.deployment-sets-detail.events :as dsd-events]
            [sixsq.nuvla.ui.pages.deployment-sets.events :as deployment-sets-events]
            [sixsq.nuvla.ui.pages.deployment-sets.views :refer [deployment-sets-views]]
            [sixsq.nuvla.ui.pages.deployments.events :as events]
            [sixsq.nuvla.ui.pages.deployments.views :refer [DeploymentsView]]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.subs :as routing-subs]
            [sixsq.nuvla.ui.routing.utils :as routing-utils]
            [sixsq.nuvla.ui.unknown-resource :refer [UnknownResource]]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.style :as style]))

(defn DeploymentTabItem
  [{:keys [active? href label icon]}]
  [:a {:href  href
       :style (cond->
                {:align-self          :flex-end
                 :margin              "0 0 -2px"
                 :padding             ".85714286em 1.14285714em"
                 :border-bottom-width "2px"
                 :transition          "color .1s ease"
                 :color               "rgba(0,0,0,.87)"}
                active?
                (merge {:border-bottom "2px solid #c10e12"
                        :font-weight   600}))}
   icon
   label])

(defn DeploymentsTabs
  []
  (let [tr         (subscribe [::i18n-subs/tr])
        route-name (subscribe [::routing-subs/canonical-route-name])]
    [:div {:style {:border-bottom "2px solid rgba(34,36,38,.15)"
                   :min-height    "2.85714286em"
                   :display       :flex}}
     [DeploymentTabItem {:label   (str/capitalize (@tr [:deployments]))
                         :href    (routing-utils/name->href routes/deployments)
                         :active? (= @route-name routes/deployments)
                         :icon    [icons/RocketIcon]}]
     [DeploymentTabItem {:label   (str/capitalize (@tr [:deployment-groups]))
                         :href    (routing-utils/name->href routes/deployment-groups)
                         :active? (= @route-name routes/deployment-groups)
                         :icon    [icons/BullseyeIcon]}]]))

(defn DeploymentsMainContent
  []
  (let [route-name (subscribe [::routing-subs/canonical-route-name])
        uuid       (subscribe [::routing-subs/path-param :uuid])]
    (fn []
      (case @route-name
        ::routes/deployments (dispatch [::events/init])

        ::routes/deployment-groups (dispatch [::deployment-sets-events/refresh])

        ::routes/deployment-groups-details
        (if (= @uuid "create")
          (dispatch [::dsd-events/init-create])
          (dispatch [::dsd-events/init]))

        nil)
      [:<>
       (when-not (= @route-name routes/deployment-groups-details)
         [DeploymentsTabs])
       [components/LoadingPage {}
        (case @route-name
          ::routes/deployments
          [DeploymentsView]

          (::routes/deployment-groups ::routes/deployment-groups-details)
          [deployment-sets-views]

          [UnknownResource])]])))

(defn deployments-view
  []
  [ui/Segment style/basic [DeploymentsMainContent]])

(defn deployment-sets-view
  []
  [ui/Segment style/basic
   [DeploymentsMainContent]])

(defn deployment-sets-details-view
  []
  [ui/Segment style/basic
   [DeploymentsMainContent]])