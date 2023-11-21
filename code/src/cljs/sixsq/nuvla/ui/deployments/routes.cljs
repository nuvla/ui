(ns sixsq.nuvla.ui.deployments.routes
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.deployment-sets.views :refer [deployment-sets-view]]
            [sixsq.nuvla.ui.deployment-sets.events :as deployment-sets-events]
            [sixsq.nuvla.ui.deployments.events :as events]
            [sixsq.nuvla.ui.deployment-sets-detail.events :as dsd-events]
            [sixsq.nuvla.ui.deployments.views :refer [DeploymentsView MenuBar]]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.routing.subs :as routing-subs]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.style :as style]
            [reagent.core :as r]
            [sixsq.nuvla.ui.routing.utils :as routing-utils]
            [sixsq.nuvla.ui.utils.general :as general-utils]))

(defn DeploymentTabItem
  [{:keys [active? href label icon] }]
  [:a {:href href
       :active active?
       :style (cond->
                {:align-self :flex-end
                 :margin "0 0 -2px"
                 :padding ".85714286em 1.14285714em"
                 :border-bottom-width "2px"
                 :transition "color .1s ease"
                 :color      "rgba(0,0,0,.87)"}
                active?
                (merge {:border-bottom "2px solid #c10e12"
                        :font-weight   600}))}
   icon
   label])

(defn DeploymentsTabs
  []
  (let [tr         (subscribe [::i18n-subs/tr])
        route-name (subscribe [::routing-subs/route-name])]
    [:div {:style {:border-bottom "2px solid rgba(34,36,38,.15)"
                   :min-height    "2.85714286em"
                   :display :flex}}
     [DeploymentTabItem {:label (str/capitalize (@tr [:deployments]))
                         :href  (routing-utils/name->href routes/deployments)
                         :active? (= @route-name routes/deployments)
                         :icon [icons/RocketIcon]}]
     [DeploymentTabItem {:label (general-utils/capitalize-words (@tr [:deployment-groups]))
                         :href  (routing-utils/name->href routes/deployment-sets)
                         :active? (= @route-name routes/deployment-sets)
                         :icon [icons/BullseyeIcon]}]]))

(defn DeploymentsMainContent
  []
  (let [route-name    (subscribe [::routing-subs/route-name])]
    (fn []
      (case @route-name
        ::routes/deployments (dispatch [::events/init])

        ::routes/deployment-sets (dispatch [::deployment-sets-events/refresh])

        ::routes/deployment-sets-details (dispatch [::dsd-events/init]))
      [:<>
       (when-not (= routes/deployment-sets-details
                    @route-name)
         [DeploymentsTabs])
       [components/LoadingPage {}
        (case @route-name
          ::routes/deployments
          [DeploymentsView]

          (::routes/deployment-sets ::routes/deployment-sets-details)
          [deployment-sets-view])]])))

(defn deployments-view
  [route]
  [ui/Segment style/basic [DeploymentsMainContent (:data route)]])
