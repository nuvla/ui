(ns sixsq.nuvla.ui.dashboard.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.dashboard-detail.views :as dashboard-detail-views]
    [sixsq.nuvla.ui.dashboard.events :as events]
    [sixsq.nuvla.ui.dashboard.subs :as subs]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [taoensso.timbre :as log]))


(defn control-bar []
  (let [tr           (subscribe [::i18n-subs/tr])
        active-only? (subscribe [::subs/active-only?])]
    ^{:key (str "activeOnly:" @active-only?)}
    [ui/Checkbox {:defaultChecked @active-only?
                  :toggle         true
                  :fitted         true
                  :label          (@tr [:active?])
                  :on-change      (ui-callback/checked
                                    #(dispatch [::events/set-active-only? %]))}]))


(defn refresh-button
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        loading? (subscribe [::subs/loading?])]
    [uix/MenuItemWithIcon
     {:name      (@tr [:refresh])
      :icon-name "refresh"
      :loading?  @loading?
      :on-click  #(dispatch [::events/get-deployments])}]))


(defn menu-bar
  []
  (let [view (subscribe [::subs/view])]
    (fn []
      [:div
       [ui/Menu {:attached "top", :borderless true}
        #_[ui/MenuItem                                      ;FIXME use fulltext when available
           [ui/Input {:placeholder (@tr [:search])
                      :icon        "search"
                      :on-change   (ui-callback/input-callback #(dispatch [::events/set-full-text-search %]))}]]
        [ui/MenuItem {:icon     "grid layout"
                      :active   (= @view "cards")
                      :on-click #(dispatch [::events/set-view "cards"])}]
        [ui/MenuItem {:icon     "table"
                      :active   (= @view "table")
                      :on-click #(dispatch [::events/set-view "table"])}]
        [ui/MenuMenu {:position "right"}
         [refresh-button]]]

       [ui/Segment {:attached "bottom"}
        [control-bar]]])))


(defn row-fn
  [{:keys [id state module] :as deployment}]
  (let [credential-id (:credential-id deployment)
        creds-name    (subscribe [::subs/creds-name-map])
        [url-name url] @(subscribe [::subs/deployment-url deployment])]
    [ui/TableRow
     [ui/TableCell [dashboard-detail-views/link-short-uuid id]]
     [ui/TableCell {:style {:overflow      "hidden",
                            :text-overflow "ellipsis",
                            :max-width     "20ch"}} (:name module)]
     [ui/TableCell state]
     [ui/TableCell (when url
                     [:a {:href url, :target "_blank", :rel "noreferrer"}
                      [ui/Icon {:name "external"}]
                      url-name])]
     [ui/TableCell (-> deployment :created time/parse-iso8601 time/ago)]
     [ui/TableCell {:style {:overflow      "hidden",
                            :text-overflow "ellipsis",
                            :max-width     "20ch"}} (get @creds-name credential-id credential-id)]
     [ui/TableCell
      (cond
        (general-utils/can-operation? "stop" deployment) [dashboard-detail-views/stop-button deployment]
        (general-utils/can-delete? deployment) [dashboard-detail-views/delete-button deployment])]]))



(defn vertical-data-table
  [deployments-list]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [deployments-list]
      [ui/Table
       {:compact     "very"
        :single-line true
        :padded      false
        :unstackable true}
       [ui/TableHeader
        [ui/TableRow
         [ui/TableHeaderCell (@tr [:id])]
         [ui/TableHeaderCell (@tr [:module])]
         [ui/TableHeaderCell (@tr [:status])]
         [ui/TableHeaderCell (@tr [:url])]
         [ui/TableHeaderCell (@tr [:created])]
         [ui/TableHeaderCell (@tr [:infrastructure])]
         [ui/TableHeaderCell (@tr [:actions])]]]
       [ui/TableBody
        (for [{:keys [id] :as deployment} deployments-list]
          ^{:key id}
          [row-fn deployment])]])))


(defn cards-data-table
  [deployments-list]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [deployments-list]
      [:div [ui/Message {:info true}
             [ui/Icon {:name "info"}]
             (@tr [:click-for-depl-details])]
       [ui/CardGroup {:centered true}
        (for [{:keys [id] :as deployment} deployments-list]
          ^{:key id}
          [dashboard-detail-views/DeploymentCard deployment])]])))


(defn deployments-display
  [deployments-list]
  (let [tr       (subscribe [::i18n-subs/tr])
        loading? (subscribe [::subs/loading?])
        view     (subscribe [::subs/view])]
    (fn [deployments-list]
      [ui/Segment (merge style/basic
                         {:loading @loading?})
       (if (= @view "cards")
         [cards-data-table deployments-list]
         [vertical-data-table deployments-list])])))


(defn dashboard-main
  []
  (let [elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])
        deployments       (subscribe [::subs/deployments])
        tr                (subscribe [::i18n-subs/tr])]
    (dispatch [::main-events/action-interval
               {:action    :start
                :id        :dashboard-get-deployments
                :frequency 20000
                :event     [::events/get-deployments]}])
    (fn []
      (let [total-deployments (:count @deployments)
            total-pages       (general-utils/total-pages (get @deployments :count 0) @elements-per-page)
            deployments-list  (get @deployments :resources [])]

        [ui/Container {:fluid true}
         [:h2 [ui/Icon {:name "dashboard"}]
          " "
          (str/capitalize (@tr [:dashboard]))]
         [menu-bar]
         [ui/Segment style/basic
          [deployments-display deployments-list]]
         (when (> total-pages 1)
           [uix/Pagination
            {:totalitems   total-deployments
             :totalPages   total-pages
             :activePage   @page
             :onPageChange (ui-callback/callback :activePage #(dispatch [::events/set-page %]))}])]))))


(defmethod panel/render :dashboard
  [path]
  (let [n        (count path)
        [_ uuid] path
        root     [dashboard-main]
        children (case n
                   1 root
                   2 [dashboard-detail-views/deployment-detail uuid]
                   root)]
    [ui/Segment style/basic children]))
