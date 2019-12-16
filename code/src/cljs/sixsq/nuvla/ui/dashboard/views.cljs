(ns sixsq.nuvla.ui.dashboard.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.dashboard-detail.views :as dashboard-detail-views]
    [sixsq.nuvla.ui.dashboard.events :as events]
    [sixsq.nuvla.ui.dashboard.subs :as subs]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as values]
    [taoensso.timbre :as log]))


(defn refresh
  []
  (dispatch [::events/refresh]))


(defn control-bar []
  (let [tr           (subscribe [::i18n-subs/tr])
        active-only? (subscribe [::subs/active-only?])]
    [:span

     [main-components/SearchInput #(dispatch [::events/set-full-text-search %])]

     ^{:key (str "activeOnly:" @active-only?)}
     [ui/Checkbox {:style          {:margin-left 10}
                   :defaultChecked @active-only?
                   :toggle         true
                   :fitted         true
                   :label          (@tr [:active?])
                   :on-change      (ui-callback/checked
                                     #(dispatch [::events/set-active-only? %]))}]]))


(defn menu-bar
  []
  (let [view     (subscribe [::subs/view])
        loading? (subscribe [::subs/loading?])]
    (fn []
      [:<>
       [ui/Menu {:borderless true, :stackable true}
        [ui/MenuItem {:icon     "grid layout"
                      :active   (= @view "cards")
                      :on-click #(dispatch [::events/set-view "cards"])}]
        [ui/MenuItem {:icon     "table"
                      :active   (= @view "table")
                      :on-click #(dispatch [::events/set-view "table"])}]

        [main-components/RefreshMenu
         {:action-id  events/refresh-action-id
          :loading?   @loading?
          :on-refresh refresh}]]

       [control-bar]])))


(defn row-fn
  [{:keys [id state module] :as deployment}]
  (let [credential-id (:parent deployment)
        creds-name    (subscribe [::subs/creds-name-map])
        [url-name url] @(subscribe [::subs/deployment-url deployment])]
    [ui/TableRow
     [ui/TableCell [values/as-link (general-utils/id->uuid id)
                    :page "dashboard" :label (general-utils/id->short-uuid id)]]
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
        (general-utils/can-operation? "stop" deployment)
        [dashboard-detail-views/stop-button deployment :label false]

        (general-utils/can-delete? deployment)
        [dashboard-detail-views/delete-button deployment :label false])]]))


(defn vertical-data-table
  [deployments-list]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [deployments-list]
      [ui/Segment style/autoscroll-x
       [ui/Table
        (merge style/single-line {:unstackable true})
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
           [row-fn deployment])]]])))


(defn cards-data-table
  [deployments-list]
  [ui/CardGroup {:centered true}
   (for [{:keys [id] :as deployment} deployments-list]
     ^{:key id}
     [dashboard-detail-views/DeploymentCard deployment])])


(defn deployments-display
  [deployments-list]
  (let [loading? (subscribe [::subs/loading?])
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
    (refresh)
    (fn []
      (let [total-deployments (:count @deployments)
            total-pages       (general-utils/total-pages
                                (get @deployments :count 0) @elements-per-page)
            deployments-list  (get @deployments :resources [])]

        [ui/Container {:fluid true}
         [uix/PageHeader "dashboard" (str/capitalize (@tr [:dashboard]))]
         [menu-bar]
         [ui/Segment style/basic
          [deployments-display deployments-list]]
         [uix/Pagination
          {:totalitems   total-deployments
           :totalPages   total-pages
           :activePage   @page
           :onPageChange (ui-callback/callback :activePage #(dispatch [::events/set-page %]))}]]))))


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
