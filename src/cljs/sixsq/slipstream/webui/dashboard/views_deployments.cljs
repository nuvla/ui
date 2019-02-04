(ns sixsq.slipstream.webui.dashboard.views-deployments
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<! >! chan timeout]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.slipstream.webui.client.subs :as client-subs]
    [sixsq.slipstream.webui.dashboard.events :as dashboard-events]
    [sixsq.slipstream.webui.dashboard.subs :as dashboard-subs]
    [sixsq.slipstream.webui.i18n.subs :as i18n-subs]
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]
    [sixsq.slipstream.webui.utils.semantic-ui-extensions :as uix]
    [sixsq.slipstream.webui.utils.ui-callback :as ui-callback]))

(defn extract-deployment-data
  [{:keys [uuid moduleResourceUri serviceUrl status startTime cloudServiceNames username
           abort type tags activeVm]}]
  {:deployment-uuid uuid
   :module-uri      moduleResourceUri
   :service-url     serviceUrl
   :state           status
   :start-time      startTime
   :clouds          cloudServiceNames
   :user            username
   :tags            tags
   :abort           abort
   :type            type
   :activeVm        activeVm})


(defn extract-deployments-data
  [deployment-resp]
  (let [deployments (get-in deployment-resp [:runs :item] [])]
    (map extract-deployment-data deployments)))


(defn is-terminated-state?
  [state]
  (#{"Finalizing" "Done" "Aborted" "Cancelled"} state))


(defn terminate-confirm
  []
  (let [deployment (subscribe [::dashboard-subs/delete-deployment-modal])
        slipstream-url (subscribe [::client-subs/slipstream-url])]
    (fn []
      (let [{:keys [deployment-uuid module-uri state clouds start-time abort tags service-url]} @deployment]
        [ui/Confirm
         {:open      (boolean deployment-uuid)
          :basic     true
          :content   (r/as-element
                       [ui/Table {:unstackable true
                                  :celled      false
                                  :single-line true
                                  :inverted    true
                                  :size        "small"
                                  :padded      false}
                        [ui/TableBody
                         [ui/TableRow
                          [ui/TableCell "ID:"]
                          [ui/TableCell [:a {:href (str @slipstream-url "/run/" deployment-uuid)} deployment-uuid]]]
                         [ui/TableRow
                          [ui/TableCell "Module URI:"]
                          [ui/TableCell [:a {:href (str @slipstream-url "/" module-uri)} module-uri]]]
                         [ui/TableRow [ui/TableCell "Start time:"] [ui/TableCell start-time]]
                         [ui/TableRow [ui/TableCell "State:"] [ui/TableCell state]]
                         [ui/TableRow
                          [ui/TableCell "Service URL:"]
                          [ui/TableCell [:a {:href service-url, :target "_blank", :rel "noreferrer"} service-url]]]
                         [ui/TableRow [ui/TableCell "State:"] [ui/TableCell state]]
                         [ui/TableRow [ui/TableCell "Clouds:"] [ui/TableCell clouds]]
                         [ui/TableRow [ui/TableCell "Abort:"] [ui/TableCell abort]]
                         [ui/TableRow [ui/TableCell "Tags:"] [ui/TableCell tags]]]])
          :onCancel  #(dispatch [::dashboard-events/delete-deployment-modal nil])
          :onConfirm #(dispatch [::dashboard-events/delete-deployment deployment-uuid])}]))))


(defn table-deployment-row
  [deployment]
  (let [slipstream-url (subscribe [::client-subs/slipstream-url])
        deleted (subscribe [::dashboard-subs/deleted-deployments])]
    (fn [{:keys [deployment-uuid module-uri service-url start-time clouds user state tags abort type activeVm]
          :as   deployment}]
      (let [global-prop (if (is-terminated-state? state) {:disabled true} {})
            aborted (not-empty abort)
            row [ui/TableRow (cond
                               (and (= state "Ready") (not aborted)) {:positive true}
                               aborted {:error true}
                               :else {})

                 [ui/TableCell (merge {:collapsing true} global-prop)
                  [ui/Icon (cond
                             (and (= state "Ready") (empty? abort)) {:name "checkmark"}
                             (not-empty abort) {:name "exclamation circle"}
                             (is-terminated-state? state) {:name "power"}
                             :else {:loading true :name "spinner"}
                             )]
                  [ui/Icon {:name (case type
                                    "Orchestration" "grid layout"
                                    "Run" "laptop"
                                    "Machine" "industry"
                                    ""
                                    )}]]

                 [ui/TableCell {:collapsing true}
                  [:a {:href (str @slipstream-url "/run/" deployment-uuid)}
                   (-> deployment-uuid
                       (str/split #"-")
                       (first))]]

                 [ui/TableCell {:collapsing true :style {:max-width     "150px"
                                                         :overflow      "hidden"
                                                         :text-overflow "ellipsis"}}
                  (let [module-uri-vec (str/split module-uri #"/")
                        module-uri-version (str (nth module-uri-vec
                                                     (- (count module-uri-vec) 2))
                                                " " (last module-uri-vec))]
                    [:a {:href (str @slipstream-url "/" module-uri)} module-uri-version])]

                 [ui/TableCell {:collapsing true :style {:max-width     "200px"
                                                         :overflow      "hidden"
                                                         :text-overflow "ellipsis"}}
                  [:a {:href service-url, :target "_blank", :rel "noreferrer"}
                   (when (seq service-url) [ui/Icon {:name "external"}])
                   service-url]]

                 [ui/TableCell (merge {:collapsing true} global-prop) state]

                 [ui/TableCell (merge {:collapsing true
                                       :textAlign  "center"} global-prop) activeVm]

                 [ui/TableCell (merge {:collapsing true} global-prop)
                  (first (str/split start-time #"\."))]

                 [ui/TableCell (merge {:collapsing true} global-prop) clouds]

                 [ui/TableCell (merge {:collapsing true :style
                                                   {:max-width     "100px"
                                                    :overflow      "hidden"
                                                    :text-overflow "ellipsis"}}
                                      global-prop) tags]

                 [ui/TableCell {:style {:max-width     "100px"
                                        :overflow      "hidden"
                                        :text-overflow "ellipsis"}}
                  [:a {:href (str @slipstream-url "/user/" user)} user]]

                 (if (is-terminated-state? state)
                   [ui/TableCell {:collapsing true}]
                   [ui/TableCell {:collapsing true}
                    [ui/Icon
                     (if (contains? @deleted deployment-uuid)
                       {:name "trash"}
                       {:name    "remove" :link true
                        :onClick #(dispatch [::dashboard-events/delete-deployment-modal deployment])})]])]]
        (if aborted
          [ui/Popup {:trigger  (r/as-element row)
                     :inverted true
                     :size     "mini" :header "ss:abort"
                     :content  abort :position "top center"}]
          row)
        ))))


(defn deployments-table
  []
  (let [tr (subscribe [::i18n-subs/tr])
        deployments (subscribe [::dashboard-subs/deployments])
        headers ["" "ID" "Application / Component" "Service URL" "State" "VMs"
                 "Start Time [UTC]" "Clouds" "Tags" "User" ""]
        page (subscribe [::dashboard-subs/page])
        total-pages (subscribe [::dashboard-subs/total-pages])
        error-message (subscribe [::dashboard-subs/error-message-deployment])
        loading? (subscribe [::dashboard-subs/loading-tab?])
        set-page #(dispatch [::dashboard-events/set-page %])
        set-active-deployments-only #(dispatch [::dashboard-events/active-deployments-only (not %)])]
    (fn []
      (let [deployments-count (get-in @deployments [:runs :totalCount] 0)
            deployments-data (extract-deployments-data @deployments)]
        [ui/Segment {:basic   true
                     :loading @loading?}
         (when-let [{:keys [reason code detail]} @error-message]
           [ui/Message {:header    (r/as-element [:div (str reason ": " code)])
                        :content   (r/as-element [:div detail])
                        :icon      "exclamation circle"
                        :error     true
                        :onDismiss #(dispatch [::dashboard-events/clear-error-message-deployment])}])
         [ui/Checkbox {:slider   true :fitted true :label "Include inactive runs"
                       :onChange (ui-callback/callback :checked set-active-deployments-only)}]

         [ui/Table
          {:compact     "very"
           :size        "small"
           :selectable  true
           :unstackable true
           :celled      false
           :single-line true
           :collapsing  false
           :padded      false}

          [ui/TableHeader
           (vec (concat [ui/TableRow]
                        (mapv (fn [i label] ^{:key (str i "_" label)}
                        [ui/TableHeaderCell label]) (range) headers)))]

          (vec (concat [ui/TableBody]
                       (mapv (fn [deployment]
                               ^{:key (:deployment-uuid deployment)}
                               [table-deployment-row deployment]) deployments-data)))

          [ui/TableFooter
           [ui/TableRow
            [ui/TableHeaderCell {:col-span (str 3)}
             [ui/Label (@tr [:total]) [ui/LabelDetail deployments-count]]]
            [ui/TableHeaderCell {:textAlign "right"
                                 :col-span  (str (- (count headers) 3))}
             [uix/Pagination
              {:size         "tiny"
               :totalPages   @total-pages
               :activePage   @page
               :onPageChange (ui-callback/callback :activePage set-page)}]]]]]

         [terminate-confirm]]))))
