(ns sixsq.nuvla.ui.deployment.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as reagent]
    [sixsq.nuvla.ui.deployment-detail.events :as deployment-detail-events]
    [sixsq.nuvla.ui.deployment-detail.utils :as deployment-detail-utils]
    [sixsq.nuvla.ui.deployment-detail.views :as deployment-detail-views]
    [sixsq.nuvla.ui.deployment.events :as events]
    [sixsq.nuvla.ui.deployment.subs :as subs]
    [sixsq.nuvla.ui.deployment.utils :as utils]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.history.views :as history]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [taoensso.timbre :as log]))


(defn deployment-active?
  [state]
  (str/ends-with? state "ING"))


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


(defn action-button
  [popup-text icon-name event-kw deployment-id]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Modal
     {:trigger (reagent/as-element
                 [:div
                  [ui/Popup {:content  (@tr [popup-text])
                             :size     "tiny"
                             :position "top center"
                             :trigger  (reagent/as-element
                                         [ui/Icon {:name  icon-name
                                                   :style {:cursor "pointer"}
                                                   :color "red"
                                                   :size  "large"}])}]])
      :header  (@tr [popup-text])
      :content (@tr [:are-you-sure?])
      :actions [{:key "cancel", :content (@tr [:cancel])}
                {:key     "yes", :content (@tr [:yes]), :positive true,
                 :onClick #(dispatch [event-kw deployment-id])}]}]))


(defn stop-button
  [{:keys [id] :as deployment}]
  [action-button :stop "close" ::events/stop-deployment id])


(defn delete-button
  [{:keys [id] :as deployment}]
  [action-button :delete "trash" ::deployment-detail-events/delete id])


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


(defn format-href
  [href]
  (let [tag (subs href 11 19)]
    [history/link (str href) tag]))


(defn row-fn
  [{:keys [id state module] :as deployment}]
  (let [credential-id (:credential-id deployment)
        creds-name    (subscribe [::subs/creds-name-map])
        [url-name url] @(subscribe [::subs/deployment-url deployment])]
    ^{:key id}
    [ui/TableRow
     [ui/TableCell [format-href id]]
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
     [ui/TableCell (cond
                     (deployment-detail-utils/stop-action? deployment) [stop-button deployment]
                     (deployment-detail-utils/delete-action? deployment) [delete-button deployment])]]))



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
         [ui/TableHeaderCell (@tr [:cloud])]
         [ui/TableHeaderCell (@tr [:actions])]]]
       (vec (concat [ui/TableBody]
                    (map row-fn deployments-list)))])))


(defn card-fn
  [{:keys [id state module] :as deployment}]
  (let [tr            (subscribe [::i18n-subs/tr])
        creds-name    (subscribe [::subs/creds-name-map])
        credential-id (:credential-id deployment)
        logo-url      (:logo-url module)
        cred-info     (get @creds-name credential-id credential-id)
        _             (log/error @creds-name)
        [url-name url] @(subscribe [::subs/deployment-url deployment])]
    ^{:key id}
    [ui/Card
     [ui/Image {:src      (or logo-url "")
                :bordered true
                :style    {:width      "auto"
                           :height     "100px"
                           :padding    "20px"
                           :object-fit "contain"}}]

     (cond
       (deployment-detail-utils/stop-action? deployment) [ui/Label {:corner true, :size "small"}
                                                          [stop-button deployment]]
       (deployment-detail-utils/delete-action? deployment) [ui/Label {:corner true, :size "small"}
                                                            [delete-button deployment]])

     [ui/CardContent {:href     id
                      :on-click (fn [event]
                                  (dispatch [::history-events/navigate id])
                                  (.preventDefault event))}




      [ui/Segment (merge style/basic {:floated "right"})
       [:p state]
       [ui/Loader {:active        (deployment-active? state)
                   :indeterminate true}]]

      [ui/CardHeader [:span [:p {:style {:overflow      "hidden",
                                         :text-overflow "ellipsis",
                                         :max-width     "20ch"}} (:name module)]]]

      [ui/CardMeta (str (@tr [:created]) " " (-> deployment :created time/parse-iso8601 time/ago))]

      [ui/CardDescription (when-not (str/blank? cred-info)
                            [:div [ui/Icon {:name "key"}] cred-info])]]
     (when url
       [ui/Button {:color   "green"
                   :icon    "external"
                   :content url-name
                   :fluid   true
                   :href    url
                   :target  "_blank"
                   :rel     "noreferrer"}])]))


(defn cards-data-table
  [deployments-list]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [deployments-list]
      [:div [ui/Message {:info true}
             [ui/Icon {:name "info"}]
             (@tr [:click-for-depl-details])]
       (vec (concat [ui/CardGroup {:centered true}]
                    (map card-fn deployments-list)))])))


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


(defn deployments-main
  []
  (let [elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])
        deployments       (subscribe [::subs/deployments])
        tr                (subscribe [::i18n-subs/tr])]
    (dispatch [::main-events/action-interval
               {:action    :start
                :id        :deployment-get-deployments
                :frequency 20000
                :event     [::events/get-deployments]}])
    (fn []
      (let [total-deployments (:count @deployments)
            total-pages       (general-utils/total-pages (get @deployments :count 0) @elements-per-page)
            deployments-list  (get @deployments :resources [])]
        [ui/Container {:fluid true}
         [menu-bar]
         [ui/Segment style/basic
          [deployments-display deployments-list]]
         (when (> total-pages 1)
           [uix/Pagination
            {:totalitems   total-deployments
             :totalPages   total-pages
             :activePage   @page
             :onPageChange (ui-callback/callback :activePage #(dispatch [::events/set-page %]))}])]))))


(defn deployment-resources
  []
  (let [path (subscribe [::main-subs/nav-path])]
    (fn []
      (let [n        (count @path)
            [collection-name resource-id] @path
            children (case n
                       1 [[deployments-main]]
                       2 [[deployment-detail-views/deployment-detail (str collection-name "/" resource-id)]]
                       [[deployments-main]])]
        (vec (concat [ui/Segment style/basic] children))))))


(defmethod panel/render :deployment
  [path]
  [deployment-resources])
