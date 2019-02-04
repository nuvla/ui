(ns sixsq.slipstream.webui.quota.views
  (:require
    [cljs.core.async :refer [<!]]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as reagent]
    [sixsq.slipstream.webui.i18n.subs :as i18n-subs]
    [sixsq.slipstream.webui.panel :as panel]
    [sixsq.slipstream.webui.quota.events :as quota-events]
    [sixsq.slipstream.webui.quota.subs :as quota-subs]
    [sixsq.slipstream.webui.utils.general :as general-utils]
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]
    [sixsq.slipstream.webui.utils.semantic-ui-extensions :as uix]
    [sixsq.slipstream.webui.utils.ui-callback :as ui-callback]))


(defn control-bar
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    (dispatch [::quota-events/get-quotas])
    (fn []
      [:div
       [ui/Menu {:attached "top", :borderless true}
        [uix/MenuItemWithIcon
         {:name      (@tr [:refresh])
          :icon-name "refresh"
          :position  "right"
          :on-click  #(dispatch [::quota-events/get-quotas])}]]])))


(defn fancy-quota-name
  [{:keys [resource aggregation name]}]
  (if (= resource "VirtualMachine")
    (case aggregation
      "count:id" "VMs"
      "sum:serviceOffer/resource:vcpu" "CPUs"
      "sum:serviceOffer/resource:ram" "RAM"
      "sum:serviceOffer/resource:disk" "Disk"
      name)
    name))


(defn get-color
  [quota-value limit]
  (let [progress (/ quota-value limit)]
    (cond
      (< progress 0.20) "green"
      (< progress 0.40) "olive"
      (< progress 0.60) "yellow"
      (< progress 0.80) "orange"
      true "red")))


(defn quota-view [{:keys [id] :as quota}]
  (let [collect-value (reagent/atom {:currentUser "-"
                                     :currentAll  "-"})
        set-collect-value #(reset! collect-value %)
        tr (subscribe [::i18n-subs/tr])]
    (dispatch [::quota-events/collect id set-collect-value])
    (fn [{:keys [name description limit] :as quota}]
      (let [{:keys [currentUser currentAll]} @collect-value]
        [ui/Popup
         {:header   name
          :content  (reagent/as-element [:p description [:br]
                                         [:b (@tr [:current-user]) " : " currentUser] [:br]
                                         [:b (@tr [:all-users]) " : " currentAll]])
          :position "top center"
          :trigger  (reagent/as-element
                      [ui/Progress
                       (cond-> {:value    currentAll
                                :size     "small"
                                :total    limit
                                :progress "value"
                                :label    (str (fancy-quota-name quota) " [" currentAll "/" limit "]")}
                               (number? currentAll) (assoc :color (get-color currentAll limit)))])}]))))


(defn quota-view-comp
  [{id :id :as quota}]
  ^{:key id} [quota-view quota])


(defn credential-view
  [credential-id credential-quotas]
  (let [credential-info (reagent/atom {})
        set-credential-info #(reset! credential-info %)]
    (dispatch [::quota-events/get-credential-info credential-id set-credential-info])
    (fn [credential-id credential-quotas]
      (let [{:keys [name description]} @credential-info]
        [ui/Card
         [ui/CardContent
          [ui/CardHeader (or name credential-id)]
          [ui/CardMeta description]
          [ui/CardDescription
           (map quota-view-comp credential-quotas)]]]))))


(defn credential-view-comp
  [selection credential-quotas]
  (let [credential-id (re-find #"credential/[\w-]*" selection)]
    ^{:key selection}
    [credential-view credential-id credential-quotas]))


(defn search-result
  []
  (let [loading-quotas? (subscribe [::quota-subs/loading-quotas?])
        credentials-quotas-map (subscribe [::quota-subs/credentials-quotas-map])
        active-page (reagent/atom 1)
        elements-per-page 8
        set-page #(reset! active-page %)]
    (fn []
      (let [total-elements (count @credentials-quotas-map)
            start-slice (* (dec @active-page) elements-per-page)
            end-slice (let [end (* @active-page elements-per-page)]
                        (if (> end total-elements) total-elements end))
            total-pages (general-utils/total-pages total-elements elements-per-page)]
        [:div
         [ui/Segment {:loading @loading-quotas?
                      :padded  true}
          (when (not-empty @credentials-quotas-map)
            (when (> @active-page total-pages) (set-page 1))
            (vec
              (concat [ui/CardGroup]
                      (map credential-view-comp
                           (subvec (vec (keys @credentials-quotas-map))
                                   start-slice
                                   end-slice)
                           (subvec (vec (vals @credentials-quotas-map))
                                   start-slice
                                   end-slice)))))]
         (when (> total-pages 1)
           [uix/Pagination {:totalPages   total-pages
                            :activePage   @active-page
                            :onPageChange (ui-callback/callback :activePage set-page)}])]))))


(defn quota
  []
  [ui/Container {:fluid true}
   [control-bar]
   [search-result]])


(defmethod panel/render :quota
  [_]
  [quota])
