(ns sixsq.nuvla.ui.pages.edges.bulk-update-modal
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.pages.edges-detail.views :as edges-detail]
            [sixsq.nuvla.ui.pages.edges.events :as events]
            [sixsq.nuvla.ui.pages.edges.spec :as spec]
            [sixsq.nuvla.ui.pages.edges.subs :as subs]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.common-components.plugins.table :as table-plugin]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(def default-state {})

(defn reset-state!
  [state]
  (reset! state default-state))

(defn set-state!
  [state path v]
  (r/rswap! state assoc-in path v))

(defn state-open? [state] (::open? @state))
(defn state-selected-release [state] (::selected-release @state))
(defn state-selected-modules [state] (::selected-modules @state))

(defn build-payload
  [state]
  (let [selected-modules (->> (state-selected-modules state)
                              (filter val)
                              (map key)
                              (remove nil?))]
    {:nuvlabox-release (-> state state-selected-release :id)
     :config-files     (concat ["docker-compose.yml"]
                               (map #(str "docker-compose." (name %) ".yml")
                                    selected-modules))}))

(defn BulkUpdateModalContent
  [state]
  (r/with-let [tr               (subscribe [::i18n-subs/tr])
               releases-by-id   (subscribe [::subs/nuvlabox-releases-by-id])
               selected-modules (r/track state-selected-modules state)
               selected-release (r/track state-selected-release state)]
    [:<>
     [ui/Segment
      [:p "Number of selected NuvlaEdges by ID: X"]
      [:p "Current NuvlaEdges matching the filter: Y"]]
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

(defn BulkUpdateModal
  [state]
  (r/with-let [tr       (subscribe [::i18n-subs/tr])
               close-fn #(reset-state! state)
               open?    (r/track state-open? state)]
    [uix/ModalDanger
     {:on-confirm         #(do (dispatch [::events/bulk-operation "bulk-update" (build-payload state)])
                               (close-fn))
      :with-confirm-step? true
      :open               @open?
      :content            [BulkUpdateModalContent state]
      :on-close           close-fn
      :header             "Bulk update edges" #_(@tr [:bulk-deployment-stop])
      :danger-msg         (@tr [:danger-action-cannot-be-undone])
      :button-text        "Update" #_(str/capitalize (@tr [:bulk-deployment-stop]))}]))

(defn BulkUpdateMenuitem
  [state]
  (r/with-let [open?     (r/track state-open? state)
               tr        (subscribe [::i18n-subs/tr])
               selection (subscribe [::table-plugin/selected-set-sub [::spec/select]])]
    [:<>
     [ui/Popup {:basic   true
                :content "pop content" #_(@tr [:deploy-with-static-edges])
                :trigger (r/as-element
                           [:div
                            [uix/HighlightableMenuItem
                             {:on-click          #(set-state! state [::open?] true)
                              :disabled          (not (seq @selection))
                              :query-param-value :bulk-update}
                             [icons/DownloadIcon]
                             "Bulk Update"
                             #_(@tr [:edges-bulk-deploy-app])]])}]
     (when @open?
       [BulkUpdateModal state])]))
