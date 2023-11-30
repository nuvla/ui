(ns sixsq.nuvla.ui.deployments.routes
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.deployment-sets-detail.events :as dsd-events]
            [sixsq.nuvla.ui.deployment-sets.events :as deployment-sets-events]
            [sixsq.nuvla.ui.deployment-sets.views :refer [deployment-sets-views]]
            [sixsq.nuvla.ui.deployments.events :as events]
            [sixsq.nuvla.ui.deployments.views :refer [DeploymentsView]]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.subs :as routing-subs]
            [sixsq.nuvla.ui.routing.utils :as routing-utils]
            [sixsq.nuvla.ui.unknown-resource :refer [UnknownResource]]
            [sixsq.nuvla.ui.utils.general :as general-utils]
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
        route-name (subscribe [::routing-subs/route-name])]
    [:div {:style {:border-bottom "2px solid rgba(34,36,38,.15)"
                   :min-height    "2.85714286em"
                   :display       :flex}}
     [DeploymentTabItem {:label   (str/capitalize (@tr [:deployments]))
                         :href    (routing-utils/name->href routes/deployments)
                         :active? (= @route-name routes/deployments)
                         :icon    [icons/RocketIcon]}]
     [DeploymentTabItem {:label   (general-utils/capitalize-words (@tr [:deployment-groups]))
                         :href    (routing-utils/name->href routes/deployment-sets)
                         :active? (boolean (#{routes/deployment-set
                                              routes/deployment-sets}
                                            @route-name))
                         :icon    [icons/BullseyeIcon]}]]))

(defn DeploymentsMainContent
  []
  (let [route-name          (subscribe [::routing-subs/route-name])
        uuid                (subscribe [::routing-subs/path-param :uuid])]
    (fn []
      (case @route-name
        ::routes/deployments (dispatch [::events/init])

        (::routes/deployment-set ::routes/deployment-sets) (dispatch [::deployment-sets-events/refresh])

        (::routes/deployment-groups-details ::routes/deployment-sets-details)
        (if (= "create" @uuid)
          (dispatch [::dsd-events/init-create])
          (dispatch [::dsd-events/init]))

        nil)
      [:<>
       (when-not (#{routes/deployment-groups-details
                    routes/deployment-sets-details}
                  @route-name)
         [DeploymentsTabs])
       [components/LoadingPage {}
        (case @route-name
          ::routes/deployments
          [DeploymentsView]

          (::routes/deployment-set
            ::routes/deployment-sets
            ::routes/deployment-groups-details
            ::routes/deployment-sets-details)
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