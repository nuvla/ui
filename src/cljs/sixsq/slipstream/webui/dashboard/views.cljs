(ns sixsq.slipstream.webui.dashboard.views
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<! >! chan timeout]]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.slipstream.webui.dashboard.events :as dashboard-events]
    [sixsq.slipstream.webui.dashboard.subs :as dashboard-subs]
    [sixsq.slipstream.webui.dashboard.views-deployments :as dep]
    [sixsq.slipstream.webui.dashboard.views-vms :as vms]
    [sixsq.slipstream.webui.i18n.subs :as i18n-subs]
    [sixsq.slipstream.webui.main.events :as main-events]
    [sixsq.slipstream.webui.panel :as panel]
    [sixsq.slipstream.webui.utils.collapsible-card :as cc]
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]
    [sixsq.slipstream.webui.utils.semantic-ui-extensions :as uix]))


(defn as-statistic [{:keys [label value]}]
  ^{:key label}
  [ui/Statistic
   [ui/StatisticValue value]
   [ui/StatisticLabel label]])


(defn active-index
  [tab-name]
  (case tab-name
    "deployments" 0
    "virtual-machines" 1))


(defn vms-deployments []
  (let [selected-tab (subscribe [::dashboard-subs/selected-tab])]
    (fn []
      [ui/Tab
       {:activeIndex (active-index @selected-tab)
        :menu        {:attached true :tabular true :size "tiny"}
        :onTabChange (fn [e, d]
                       (let [activeTabIndex (:activeIndex (js->clj d :keywordize-keys true))
                             activeTab (case activeTabIndex
                                         0 "deployments"
                                         1 "virtual-machines")]
                         (dispatch [::dashboard-events/set-selected-tab activeTab])))
        :panes       [{:menuItem "Deployments"
                       :render   (fn [] (r/as-element
                                          [:div {:style {:width "auto" :overflow-x "auto"}}
                                           [ui/TabPane {:as :div :style {:margin "10px"}}
                                            [dep/deployments-table]]
                                           ]))}
                      {:menuItem "Virtual Machines"
                       :render   (fn [] (r/as-element
                                          [:div {:style {:width "auto" :overflow-x "auto"}}
                                           [ui/TabPane {:as :div :style {:margin "10px"}}
                                            [vms/vms-table]]
                                           ]))}]}])))


(defn dashboard-resource
  []
  (let [tr (subscribe [::i18n-subs/tr])
        statistics (subscribe [::dashboard-subs/statistics])
        loading? (subscribe [::dashboard-subs/loading?])]
    (dispatch [::main-events/action-interval {:action    :start
                                              :id        :dashboard-tab
                                              :frequency 15000
                                              :event     [::dashboard-events/fetch-tab-records]}])
    (fn []
      [ui/Container {:fluid true}
       [ui/Menu {:borderless true}
        [uix/MenuItemWithIcon
         {:name      (@tr [:refresh])
          :icon-name "refresh"
          :position  "right"
          :loading?  @loading?
          :on-click  #(dispatch [::dashboard-events/get-statistics])}]]
       (when-not @loading?
         (let [stats (->> @statistics
                          (sort-by :order)
                          (map as-statistic))]
           [cc/collapsible-card (@tr [:statistics]) [ui/StatisticGroup {:size :tiny} stats]]))
       [vms-deployments]])))


(defn ^:export set-cloud-filter [cloud]
  (dispatch [::dashboard-events/set-filtered-cloud cloud]))

(defn ^:export fetch-tab-records []
  (dispatch [::dashboard-events/fetch-tab-records]))


(defmethod panel/render :dashboard
  [path]
  [dashboard-resource])
