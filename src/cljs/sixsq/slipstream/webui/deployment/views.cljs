(ns sixsq.slipstream.webui.deployment.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as reagent]
    [sixsq.slipstream.webui.deployment-detail.events :as deployment-detail-events]
    [sixsq.slipstream.webui.deployment-detail.utils :as deployment-detail-utils]
    [sixsq.slipstream.webui.deployment-detail.views :as deployment-detail-views]
    [sixsq.slipstream.webui.deployment.events :as events]
    [sixsq.slipstream.webui.deployment.subs :as subs]
    [sixsq.slipstream.webui.history.events :as history-events]
    [sixsq.slipstream.webui.history.views :as history]
    [sixsq.slipstream.webui.i18n.subs :as i18n-subs]
    [sixsq.slipstream.webui.main.events :as main-events]
    [sixsq.slipstream.webui.main.subs :as main-subs]
    [sixsq.slipstream.webui.panel :as panel]
    [sixsq.slipstream.webui.utils.general :as general-utils]
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]
    [sixsq.slipstream.webui.utils.semantic-ui-extensions :as uix]
    [sixsq.slipstream.webui.utils.style :as style]
    [sixsq.slipstream.webui.utils.time :as time]
    [sixsq.slipstream.webui.utils.ui-callback :as ui-callback]))


(defn deployment-active?
  [state ss-state]
  (or (str/ends-with? state "ING")
      (and (= state "STARTED")
           (not (#{"DONE" "ABORTED" "READY"} ss-state)))))


(defn deployment-state
  [state ss-state]
  (or (when (= state "STARTED") ss-state) state))


(defn control-bar []
  (let [tr (subscribe [::i18n-subs/tr])
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
  (let [tr (subscribe [::i18n-subs/tr])
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
  (let [deployments-creds-map (subscribe [::subs/deployments-creds-map])
        deployments-service-url-map (subscribe [::subs/deployments-service-url-map])
        deployments-ss-state-map (subscribe [::subs/deployments-ss-state-map])
        ss-state (some->> id (get @deployments-ss-state-map) str/upper-case)
        creds-name (subscribe [::subs/creds-name-map])
        service-url (get @deployments-service-url-map id)
        creds-ids (get @deployments-creds-map id [])]
    ^{:key id}
    [ui/TableRow
     [ui/TableCell [format-href id]]
     [ui/TableCell {:style {:overflow      "hidden",
                            :text-overflow "ellipsis",
                            :max-width     "20ch"}} (:name module)]
     [ui/TableCell (deployment-state state ss-state)]
     [ui/TableCell (when service-url
                     [:a {:href service-url, :target "_blank", :rel "noreferrer"}
                      [ui/Icon {:name "external"}]])]
     [ui/TableCell (-> deployment :created time/parse-iso8601 time/ago)]
     [ui/TableCell {:style {:overflow      "hidden",
                            :text-overflow "ellipsis",
                            :max-width     "20ch"}} (str/join ", " (map #(get @creds-name % %) creds-ids))]
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
  (let [tr (subscribe [::i18n-subs/tr])
        deployments-creds-map (subscribe [::subs/deployments-creds-map])
        deployments-service-url-map (subscribe [::subs/deployments-service-url-map])
        deployments-ss-state-map (subscribe [::subs/deployments-ss-state-map])
        creds-name (subscribe [::subs/creds-name-map])
        service-url (get @deployments-service-url-map id)
        ss-state (some->> id (get @deployments-ss-state-map) str/upper-case)
        creds-ids (get @deployments-creds-map id [])
        logoURL (:logoURL module)
        cred-info (str/join ", " (map #(get @creds-name % %) creds-ids))]
    ^{:key id}
    [ui/Card
     [ui/Image {:src      (or logoURL "")
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
       [:p (deployment-state state ss-state)]
       [ui/Loader {:active        (deployment-active? state ss-state)
                   :indeterminate true}]]

      [ui/CardHeader [:span [:p {:style {:overflow      "hidden",
                                         :text-overflow "ellipsis",
                                         :max-width     "20ch"}} (:name module)]]]

      [ui/CardMeta (str (@tr [:created]) " " (-> deployment :created time/parse-iso8601 time/ago))]

      [ui/CardDescription (when-not (str/blank? cred-info)
                            [:div [ui/Icon {:name "key"}] cred-info])]]
     (when service-url
       [ui/Button {:color   "green"
                   :icon    "external"
                   :content (@tr [:access-deployment])
                   :fluid   true
                   :href    service-url
                   :target  "_blank"
                   :rel     "noreferrer"}])]))


(defn cards-data-table
  [deployments-list]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [deployments-list]
      [:div [ui/Message {:info true}
             [ui/Icon {:name "info"}]
             (@tr [:click-for-depl-details])]
       (vec (concat [ui/CardGroup]
                    (map card-fn deployments-list)))])))


(defn deployments-display
  [deployments-list]
  (let [tr (subscribe [::i18n-subs/tr])
        loading? (subscribe [::subs/loading?])
        view (subscribe [::subs/view])]
    (fn [deployments-list]
      [ui/Segment (merge style/basic
                         {:loading @loading?})
       (if (= @view "cards")
         [cards-data-table deployments-list]
         [vertical-data-table deployments-list])])))


(defn deployments-main
  []
  (let [elements-per-page (subscribe [::subs/elements-per-page])
        page (subscribe [::subs/page])
        deployments (subscribe [::subs/deployments])
        tr (subscribe [::i18n-subs/tr])]
    (fn []
      (let [total-pages (general-utils/total-pages (get @deployments :count 0) @elements-per-page)
            deployments-list (get @deployments :deployments [])]
        [ui/Container {:fluid true}
         [menu-bar]
         [ui/Segment style/basic
          [deployments-display deployments-list]]
         [ui/Label (@tr [:total]) [ui/LabelDetail (get @deployments :count "-")]]
         (when (> total-pages 1)
           [uix/Pagination
            {:totalPages   total-pages
             :activePage   @page
             :onPageChange (ui-callback/callback :activePage #(dispatch [::events/set-page %]))}])]))))


(defn deployment-resources
  []
  (let [path (subscribe [::main-subs/nav-path])]
    (fn []
      (let [n (count @path)
            [collection-name resource-id] @path
            children (case n
                       1 [[deployments-main]]
                       2 [[deployment-detail-views/deployment-detail (str collection-name "/" resource-id)]]
                       [[deployments-main]])]
        (vec (concat [ui/Segment style/basic] children))))))


(defmethod panel/render :deployment
  [path]
  (dispatch [::main-events/action-interval
             {:action    :start
              :id        :deployment-get-deployments
              :frequency 20000
              :event     [::events/get-deployments]}])
  [deployment-resources])
