 (ns sixsq.nuvla.ui.pages.mec.views
   (:require [cljs.pprint :refer [pprint]]
             [clojure.string :as str]
             [re-frame.core :refer [dispatch subscribe]]
             [reagent.core :as r]
             [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
             [sixsq.nuvla.ui.common-components.plugins.nav-tab :as tab-plugin]
             [sixsq.nuvla.ui.pages.about.subs :as about-subs]
             [sixsq.nuvla.ui.pages.about.utils :as about-utils]
             [sixsq.nuvla.ui.pages.mec.events :as events]
             [sixsq.nuvla.ui.pages.mec.spec :as spec]
             [sixsq.nuvla.ui.pages.mec.subs :as subs]
             [sixsq.nuvla.ui.session.subs :as session-subs]
             [sixsq.nuvla.ui.utils.forms :as forms]
             [sixsq.nuvla.ui.utils.general :as general-utils]
             [sixsq.nuvla.ui.utils.icons :as icons]
             [sixsq.nuvla.ui.utils.semantic-ui :as ui]
             [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
             [sixsq.nuvla.ui.utils.values :as values]))

 (defn pretty-json
   [value]
   (when value
     (with-out-str (pprint value))))

(def wrap-anywhere-style
  {:overflow-wrap "anywhere"
   :word-break    "break-word"})

 (defn mepm-template
   []
   {:name         "Example MEPM"
    :description  "MEPM created from the MEC admin page."
    :endpoint     "http://localhost:8080/mepm"
   :backend-mode "MOCK"
   :managed-edges []
    :capabilities {:platforms   ["kubernetes"]
                   :services    ["traffic-rules"]
                   :api-version "2.2.1"}
    :status       "ONLINE"})

 (defn subscription-template
   [owner]
  {:subscription-type   "AppLcmOpOccStateChange"
    :callback-uri        "https://example.com/mec/notifications"
    :owner               owner
    :active              true
    :app-lcm-op-occ-filter {:operation-type "INSTANTIATE"}})

(defn JsonModalButton
  [{:keys [title menu-item-label button-confirm-label initial-value on-confirm validate-fn]}]
   (let [text (r/atom (general-utils/edn->json initial-value))]
    (fn [{:keys [title menu-item-label button-confirm-label initial-value on-confirm validate-fn]}]
       (let [reset-fn #(reset! text (general-utils/edn->json initial-value))]
         [uix/ModalActionButton
          {:menu-item-label      menu-item-label
           :button-confirm-label button-confirm-label
           :icon                 icons/i-plus
           :title-text           title
           :Content              [forms/resource-editor title text]
           :on-confirm           on-confirm
           :on-cancel            reset-fn
           :scrolling?           true
           :validate-fn          #(try
                                    (let [value (general-utils/json->edn @text :throw-exceptions true)]
                                      (if validate-fn
                                        (validate-fn value)
                                        value))
                                    (catch js/Error e e))}]))))

(defn normalize-endpoint
  [endpoint]
  (some-> endpoint
          str
          str/trim
          (str/replace #"/+$" "")))

(defn mepm-managed-edges
  [mepm]
  (let [managed-edges (:managed-edges mepm)]
    (if (contains? mepm :managed-edges)
      (vec managed-edges)
      (cond-> []
        (:mec-host-id mepm) (conj (:mec-host-id mepm))))))

(defn managed-edges-summary
  [mepm]
  (let [managed-edges (mepm-managed-edges mepm)]
    (cond
      (empty? managed-edges) "-"
      (= 1 (count managed-edges)) (first managed-edges)
      :else (str (first managed-edges) " +" (dec (count managed-edges))))))

(defn validate-unique-mepm
  [existing-mepms candidate]
  (let [candidate-endpoint (normalize-endpoint (:endpoint candidate))
        existing-endpoints (->> existing-mepms
                                (keep #(normalize-endpoint (:endpoint %)))
                                set)]
    (cond
      (str/blank? candidate-endpoint)
      (js/Error. "MEPM endpoint is required.")

      (contains? existing-endpoints candidate-endpoint)
      (js/Error. (str "A MEPM with endpoint " candidate-endpoint " already exists."))

      :else
      candidate)))

(defn AddManagedEdgeButton
  [{:keys [mepm-id managed-edges]}]
  (let [available-edges (subscribe [::subs/available-edges])
        loading-edges?  (subscribe [::subs/loading-available-edges?])
        show?           (r/atom false)
        selected-edge   (r/atom nil)]
    (fn [{:keys [mepm-id managed-edges]}]
      (let [assigned-edges   (set managed-edges)
            edge-options     (->> @available-edges
                                  (remove #(contains? assigned-edges (:id %)))
                                  (mapv (fn [{:keys [id name online state]}]
                                          {:key   id
                                           :text  (str (or name id)
                                                       " ["
                                                       (if online "online" "offline")
                                                       ", "
                                                       (or state "unknown")
                                                       "]")
                                           :value id})))]
        [uix/ModalActionButton
         {:show?                show?
          :title-text           "Add managed Nuvla Edge"
          :button-confirm-label "Add"
          :icon                 icons/i-plus
          :menu-item-label      "Add managed Edge"
          :Trigger              [uix/Button {:text "Add Edge"
                                             :icon icons/i-plus
                                             :on-click #(reset! show? true)}]
          :Content              [ui/Form
                                 [ui/FormField
                                  [:label "Nuvla Edge"]
                                  [ui/Dropdown {:fluid       true
                                                :selection   true
                                                :search      true
                                                :placeholder "Select a Nuvla Edge"
                                                :loading     @loading-edges?
                                                :value       @selected-edge
                                                :options     edge-options
                                                :on-change   (fn [_ data]
                                                               (reset! selected-edge (.-value data)))}]]
                                 (when (empty? edge-options)
                                   [uix/MsgInfo {:header  "No unassigned Edges available"
                                                 :content "All discovered Nuvla Edges are already managed by this MEPM, or no Edges are currently available."}])]
          :validate-fn          #(if (str/blank? @selected-edge)
                                   (js/Error. "Select a Nuvla Edge to add.")
                                   @selected-edge)
          :on-confirm           #(do
                                   (reset! selected-edge nil)
                                   (dispatch [::events/add-managed-edge mepm-id %]))
          :on-cancel            #(reset! selected-edge nil)}]))))

(defn JsonDataPanel
  [title value]
  (when value
    [ui/Segment {:secondary true
                 :style     {:max-width "100%"
                             :overflow-x "auto"}}
     [:h4 title]
     [:pre {:style {:white-space "pre-wrap"
                    :overflow-wrap "anywhere"
                    :word-break    "break-word"
                    :max-width     "100%"}}
      (pretty-json value)]]))

(defn ManagedEdgeRow
  [mepm-id edge-id]
  [:div {:style {:display         "flex"
                 :justify-content "space-between"
                 :align-items     "center"
                 :gap             "1rem"
                 :padding         "0.5rem 0"
                 :border-bottom   "1px solid rgba(34,36,38,.08)"}}
   [:div {:style {:min-width 0}}
    [values/AsPageLink edge-id
     :label (general-utils/id->short-uuid edge-id)]
    [:div {:style {:color "#666"
                   :font-size "0.9em"
                   :overflow-wrap "anywhere"}}
     edge-id]]
   [ui/Button {:basic    true
               :size     "tiny"
               :on-click #(dispatch [::events/remove-managed-edge mepm-id edge-id])}
    [icons/Icon {:name icons/i-trash}]
    "Remove"]])

(defn ManagedEdgesPanel
  [mepm-id managed-edges]
  [ui/Segment {:secondary true}
   [:div {:style {:display         "flex"
                  :justify-content "space-between"
                  :align-items     "center"
                  :gap             "0.75rem"
                  :margin-bottom   "1rem"}}
    [:h4 {:style {:margin 0}} "Managed Nuvla Edges"]
    [:span {:style {:color "#666"}}
     (str (count managed-edges) " edge"
          (when (not= 1 (count managed-edges)) "s"))]]
   (if (empty? managed-edges)
     [uix/MsgNoItemsToShow "No Nuvla Edges are currently managed by this MEPM."]
     [:div {:style {:display        "flex"
                    :flex-direction "column"
                    :gap            "0.75rem"}}
      (doall
        (for [edge-id managed-edges]
          ^{:key edge-id}
          [ManagedEdgeRow mepm-id edge-id]))])])

(defn MepmRow
  [{:keys [id name endpoint status last-check backend-mode] :as mepm}]
   (let [selected-id (subscribe [::subs/selected-mepm-id])]
     [ui/TableRow {:active   (= id @selected-id)
                   :style    {:cursor "pointer"}
                   :on-click #(dispatch [::events/get-mepm id])}
      [ui/TableCell {:style wrap-anywhere-style}
       (or name [values/AsLink id :label (general-utils/id->short-uuid id)])]
      [ui/TableCell status]
      [ui/TableCell (or backend-mode "MOCK")]
      [ui/TableCell {:style wrap-anywhere-style} endpoint]
     [ui/TableCell {:style wrap-anywhere-style} (managed-edges-summary mepm)]
      [ui/TableCell (if last-check [uix/TimeAgo last-check] "-")]]))

 (defn MepmList
   []
   (let [mepms         (subscribe [::subs/mepms])
         loading-mepms? (subscribe [::subs/loading-mepms?])]
     [ui/Segment
      [:div {:style {:display         "flex"
                     :justify-content "space-between"
                     :align-items     "center"
                     :margin-bottom   "1rem"}}
       [:h3 {:style {:margin 0}} "MEPMs"]
      [:div {:style {:display "flex"
                     :gap     "0.75rem"
                     :flex-wrap "wrap"}}
        [JsonModalButton {:title                "Add MEPM"
                          :menu-item-label      "Add MEPM"
                          :button-confirm-label "Create"
                          :initial-value        (mepm-template)
                          :validate-fn          #(validate-unique-mepm @mepms %)
                          :on-confirm           #(dispatch [::events/add-mepm %])}]
        [uix/Button {:text     "Refresh"
                     :icon     icons/i-arrow-rotate
                     :loading  @loading-mepms?
                     :on-click #(dispatch [::events/get-mepms])}]]]
      (if (empty? @mepms)
        [uix/MsgNoItemsToShow "No MEPM resources available."]
        [:div {:style {:overflow-x "auto"}}
         [ui/Table {:basic "very"
                    :selectable true
                    :style {:table-layout "fixed"
                            :width        "100%"
                            :min-width    "100%"}}
          [ui/TableHeader
           [ui/TableRow
            [ui/TableHeaderCell "Name"]
            [ui/TableHeaderCell "Status"]
            [ui/TableHeaderCell "Mode"]
            [ui/TableHeaderCell "Endpoint"]
            [ui/TableHeaderCell "Managed Edges"]
            [ui/TableHeaderCell "Last check"]]]
          [ui/TableBody
           (for [mepm @mepms]
             ^{:key (:id mepm)}
             [MepmRow mepm])]]])]))

(defn MepmDetailContent
  [{:keys [id name endpoint status backend-mode mec-host-id credential-id
           version last-check capabilities resources] :as mepm}]
  (let [managed-edges (mepm-managed-edges mepm)]
    [:div
     [:div {:style {:display         "flex"
                    :justify-content "space-between"
                    :align-items     "flex-start"
                    :flex-wrap       "wrap"
                    :gap             "0.75rem"
                    :margin-bottom   "1rem"}}
      [:h3 {:style {:margin 0}} (or name id)]
      [:div {:style {:display   "flex"
                     :gap       "0.75rem"
                     :flex-wrap "wrap"}}
       [uix/Button {:text     "Check health"
                    :icon     icons/i-heartbeat
                    :on-click #(dispatch [::events/run-mepm-action id "check-health"])}]
       [uix/Button {:text     "Query capabilities"
                    :icon     icons/i-info
                    :on-click #(dispatch [::events/run-mepm-action id "query-capabilities"])}]
       [uix/Button {:text     "Query resources"
                    :icon     icons/i-db
                    :on-click #(dispatch [::events/run-mepm-action id "query-resources"])}]
       [AddManagedEdgeButton {:mepm-id       id
                              :managed-edges managed-edges}]]]
     [ui/Table {:basic "very"
                :style {:table-layout "fixed"
                        :width        "100%"}}
      [ui/TableBody
       [ui/TableRow
        [ui/TableCell "ID"]
        [ui/TableCell {:style {:overflow-wrap "anywhere"}}
         [values/AsLink id :label (general-utils/id->uuid id)]]]
       [ui/TableRow
        [ui/TableCell "Endpoint"]
        [ui/TableCell {:style {:overflow-wrap "anywhere"}} endpoint]]
       [ui/TableRow
        [ui/TableCell "Status"]
        [ui/TableCell status]]
       [ui/TableRow
        [ui/TableCell "Mode"]
        [ui/TableCell (or backend-mode "MOCK")]]
       (when version
         [ui/TableRow
          [ui/TableCell "Version"]
          [ui/TableCell version]])
       (when last-check
         [ui/TableRow
          [ui/TableCell "Last check"]
          [ui/TableCell [uix/TimeAgo last-check]]])
       (when (and mec-host-id (not (contains? mepm :managed-edges)))
         [ui/TableRow
          [ui/TableCell "MEC host"]
          [ui/TableCell
           [values/AsPageLink mec-host-id
            :label (general-utils/id->short-uuid mec-host-id)]]])
       (when credential-id
         [ui/TableRow
          [ui/TableCell "Credential"]
          [ui/TableCell
           [values/AsPageLink credential-id
            :label (general-utils/id->short-uuid credential-id)]]])]]
     [ManagedEdgesPanel id managed-edges]
     [JsonDataPanel "Capabilities" capabilities]
     [JsonDataPanel "Resources" resources]]))

(defn MepmDetail
  []
  (let [selected-mepm          (subscribe [::subs/selected-mepm])
        loading-selected-mepm? (subscribe [::subs/loading-selected-mepm?])]
    (fn []
      [ui/Segment {:style {:overflow "hidden"}}
       (cond
         @loading-selected-mepm?
         [ui/Loader {:active true :inline "centered"}]

         (not @selected-mepm)
         [uix/MsgNoItemsToShow "Select an MEPM to inspect its status and actions."]

         :else
         [MepmDetailContent @selected-mepm])])))

 (defn MepmsPane
   []
   [ui/TabPane
   [ui/Grid {:stackable true :columns 1}
     [ui/GridRow
     [ui/GridColumn {:width 16}
       [MepmList]]
     [ui/GridColumn {:width 16}
       [MepmDetail]]]]])

 (defn subscription-filter-summary
   [{:keys [app-instance-filter app-lcm-op-occ-filter]}]
   (cond
     app-instance-filter (str/replace (general-utils/edn->json app-instance-filter) #"\n" " ")
     app-lcm-op-occ-filter (str/replace (general-utils/edn->json app-lcm-op-occ-filter) #"\n" " ")
     :else "-"))

 (defn SubscriptionRow
   [{:keys [id subscription-type callback-uri active owner] :as subscription}]
  [ui/Segment {:secondary true}
   [:div {:style {:display         "flex"
                  :justify-content "space-between"
                  :align-items     "flex-start"
                  :gap             "1rem"}}
    [:div {:style {:min-width 0}}
     [:div {:style {:font-weight 600
                    :margin-bottom "0.5rem"}}
      (or subscription-type "Subscription")
      " "
      [values/AsLink (str "mec-subscription/" (general-utils/id->uuid id))
       :label (general-utils/id->short-uuid id)]]
     [:div [:b "Callback URI:"] " " callback-uri]
     [:div [:b "Active:"] " " (str active)]
     [:div [:b "Owner:"] " " (or owner "-")]
     [:div {:style {:overflow-wrap "anywhere"}}
      [:b "Filter:"] " " (subscription-filter-summary subscription)]]]])

 (defn SubscriptionsPane
   []
   (let [subscriptions          (subscribe [::subs/subscriptions])
         loading-subscriptions? (subscribe [::subs/loading-subscriptions?])
         user                   (subscribe [::session-subs/user])]
     [ui/TabPane
      [ui/Segment
       [:div {:style {:display         "flex"
                      :justify-content "space-between"
                      :align-items     "center"
                      :margin-bottom   "1rem"}}
        [:h3 {:style {:margin 0}} "Subscriptions"]
        [:div {:style {:display "flex"
                       :gap     "0.75rem"
                       :flex-wrap "wrap"}}
         ^{:key (str @user)}
         [JsonModalButton {:title                "Add MEC subscription"
                           :menu-item-label      "Add subscription"
                           :button-confirm-label "Create"
                           :initial-value        (subscription-template @user)
                           :on-confirm           #(dispatch [::events/add-subscription %])}]
         [uix/Button {:text     "Refresh"
                      :icon     icons/i-arrow-rotate
                      :loading  @loading-subscriptions?
                      :on-click #(dispatch [::events/get-subscriptions])}]]]
       (if (empty? @subscriptions)
         [uix/MsgNoItemsToShow "No MEC subscriptions available."]
         [:div
          [:p {:style {:color "#666" :margin-bottom "1rem"}}
           (str (count @subscriptions) " subscription"
                (when (not= 1 (count @subscriptions)) "s"))]
          (doall
            (for [subscription @subscriptions]
              ^{:key (:id subscription)}
              [SubscriptionRow subscription]))])]]))

 (defn mec-panes
   []
   [{:menuItem {:content "MEPMs"
                :key     :mepms
                :icon    icons/i-cubes}
    :render   #(r/as-element [MepmsPane])}
    {:menuItem {:content "Subscriptions"
                :key     :subscriptions
                :icon    icons/i-bell}
    :render   #(r/as-element [SubscriptionsPane])}])

 (defn DisabledMessage
   []
   [ui/Segment
    [uix/MsgInfo {:header  "ETSI MEC UI is disabled"
                  :content "Enable the etsi-mec feature flag from the About page to access MEC administration."}]])

 (defn mec-view
   [_path]
  (r/create-class
    {:component-did-mount #(dispatch [::events/init])
     :reagent-render
     (fn [_]
       (let [etsi-mec-enabled? (subscribe [::about-subs/feature-flag-enabled? about-utils/feature-etsi-mec])
             tr                (subscribe [::i18n-subs/tr])]
         [:<>
          [:h2 {:style {:margin-top 0}}
           [icons/CubesIcon {:style {:margin-right "0.5rem"}}]
           (or (@tr [:mec]) "MEC")]
          (if-not @etsi-mec-enabled?
            [DisabledMessage]
            [tab-plugin/Tab
             {:db-path [::spec/tab]
              :menu    {:secondary true
                        :pointing  true}
              :panes   (mec-panes)}])]))}))
