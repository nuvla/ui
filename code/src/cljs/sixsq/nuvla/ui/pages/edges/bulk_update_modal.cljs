(ns sixsq.nuvla.ui.pages.edges.bulk-update-modal
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.pages.edges-detail.views :as edges-detail]
            [sixsq.nuvla.ui.pages.edges.events :as events]
            [sixsq.nuvla.ui.pages.edges.spec :as spec]
            [sixsq.nuvla.ui.pages.edges.subs :as subs]
            [sixsq.nuvla.ui.pages.edges.utils :as utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.common-components.plugins.table :as table-plugin]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(def ^{:private true} default-state {})

(defn- reset-state!
  [state]
  (reset! state default-state))

(defn- set-state!
  [state path v]
  (r/rswap! state assoc-in path v))

(defn- state-open? [state] (get @state ::open? false))
(defn- state-selected-release [state] (::selected-release @state))
(defn- state-selected-modules [state] (::selected-modules @state))
(defn- state-summary [state] (::summary @state))

(defn- collect-distribution
  [aggregations term]
  (reduce (fn [acc {k :key v :doc_count}] (assoc acc k v)) {} (get-in aggregations [term :buckets])))

(defn- compute-summary
  [response]
  (let [aggregations          (:aggregations response)
        versions-distribution (collect-distribution aggregations :terms:nuvlabox-engine-version)
        aggregation-count     (get-in aggregations [:value_count:id :value])]
    {:ne-versions-distribution versions-distribution
     :count                    aggregation-count}))

(defn- build-payload
  [state]
  (let [selected-modules (->> (state-selected-modules state)
                              (filter val)
                              (map key)
                              (remove nil?))]
    {:nuvlabox-release (-> state state-selected-release :id)
     :config-files     (concat ["docker-compose.yml"]
                               (map #(str "docker-compose." (name %) ".yml")
                                    selected-modules))}))

(defn- SummarySegment
  [state]
  (let [summary     (r/track state-summary state)
        selection   (subscribe [::table-plugin/selected-set-sub [::spec/select]])
        select-all? (subscribe [::table-plugin/select-all?-sub [::spec/select]])]
    (dispatch [::events/bulk-update-aggregations
               #(set-state! state [::summary] (compute-summary %))])
    (fn [_state]
      (let [total-count           (if @select-all?
                                    @(subscribe [::subs/nuvlaboxes-count])
                                    (count @selection))
            count-summary         (:count @summary)
            versions-distribution (:ne-versions-distribution @summary)]
        [ui/Segment
         [:p (if @select-all?
               "Current number of NuvlaEdges matching the filter: "
               "Number of selected NuvlaEdges: ")
          [:b total-count]]
         (when (> total-count count-summary)
           [ui/Message {:warning true}
            [ui/MessageContent
             "You selected NuvlaEdges that are not eligible to an update. Number of NuvlaEdges that will be updated: "
             [:b count-summary]]])
         [ui/Segment
          [:p (str (when (> count-summary (reduce + (vals versions-distribution)))
                     "Top 10 ")
                   "Nuvlaedges versions in use:")]
          [ui/LabelGroup {:color :blue}
           (for [[version version-count] versions-distribution]
             ^{:key (str version version-count)}
             [ui/Label version [ui/LabelDetail version-count]])]]]))))

(defn- Content
  [state]
  (r/with-let [tr               (subscribe [::i18n-subs/tr])
               releases-by-id   (subscribe [::subs/nuvlabox-releases-by-id])
               selected-modules (r/track state-selected-modules state)
               selected-release (r/track state-selected-release state)]
    [:<>
     [SummarySegment state]
     [ui/Segment
      [:b (@tr [:update-to])]
      [edges-detail/DropdownReleases {:placeholder (@tr [:select-version])
                                      :on-change   (ui-callback/value
                                                     #(set-state! state [::selected-release] (get @releases-by-id %)))}]
      (let [{:keys [compose-files]} @selected-release]
        (when (seq compose-files)
          [edges-detail/AdditionalModulesTable compose-files
           {:on-module-change (fn [scope]
                                (let [scope-key (keyword scope)]
                                  (ui-callback/checked
                                    (fn [checked]
                                      (set-state! state [::selected-modules scope-key] checked)))))
            :module-checked?  (fn [scope] (get @selected-modules (keyword scope) false))}]))]
     [ui/Segment
      [:p "List env vars to apply on all of them"]]]))

(defn init-state [] (r/atom default-state))

(defn open? [state] (r/track state-open? state))

(defn open-modal [state] (set-state! state [::open?] true))

(defn Modal
  [state]
  (r/with-let [tr       (subscribe [::i18n-subs/tr])
               close-fn #(reset-state! state)
               open?    (open? state)]
    [uix/ModalDanger
     {:on-confirm         #(do (dispatch [::events/bulk-operation "bulk-update"
                                          utils/build-bulk-update-filter (build-payload state)])
                               (close-fn))
      :with-confirm-step? true
      :open               @open?
      :content            [Content state]
      :on-close           close-fn
      :header             "Bulk update edges" #_(@tr [:bulk-deployment-stop])
      :danger-msg         (@tr [:danger-action-cannot-be-undone])
      :button-text        "Update" #_(str/capitalize (@tr [:bulk-deployment-stop]))}]))
