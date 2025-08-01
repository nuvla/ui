(ns sixsq.nuvla.ui.pages.edges-detail.views-coe-resources
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe reg-event-fx reg-sub]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.plugins.table-refactor :as table]
            [sixsq.nuvla.ui.pages.edges-detail.events :as events]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.pages.edges-detail.subs :as subs]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.common-components.job.views :as job-views]
            [sixsq.nuvla.ui.pages.edges-detail.spec :as spec]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.tooltip :as tt]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
            [sixsq.nuvla.ui.pages.edges.utils :as edges-utils]))

(def local-storage-key "nuvla.ui.table.edges.coe-resources.column-configs")

(def default-pagination {:page-index 0
                         :page-size  25})

(reg-sub
  ::current-cols
  (fn [db [_ k]]
    (let [ls   (aget js/window "localStorage")
          data (or
                 (get db local-storage-key)
                 (edn/read-string (.getItem ls local-storage-key)))]
      (get data k))))

(main-events/reg-set-current-cols-event-fx ::set-current-cols local-storage-key)

(reg-event-fx
  ::coe-resource-actions
  (fn [{{:keys [::spec/nuvlabox]} :db} [_ payload close-modal-fn]]
    {:fx [[:dispatch [::events/operation (:id nuvlabox) "coe-resource-actions" payload
                      close-modal-fn close-modal-fn]]]}))

(defmethod job-views/JobCell "coe_resource_actions"
  [{:keys [id status-message] :as resource}]
  (if-let [responses (some-> status-message general-utils/json->edn :docker)]
    (uix/label-group-overflow-detector
      (fn []
        [ui/ListSA {:divided true :relaxed :very}
         (for [[i {:keys [success content message return-code] :as _response}] (map-indexed vector responses)]
           ^{:key (str id "-" i)}
           [ui/ListItem {:style {:display     :flex
                                 :align-items :center}}
            [:div
             [ui/Label {:horizontal true
                        :color      (if success "green" "red")} return-code]]
            [ui/ListContent
             (or message content)]])]))
    [job-views/DefaultJobCell resource]))

(defn CellKeyValueLabelGroup
  [cell-data _row _column]
  (uix/label-group-overflow-detector
    (fn []
      [ui/LabelGroup
       (for [[k v] cell-data]
         ^{:key k}
         [ui/Label {:style   {:white-space :pre}
                    :content k :detail (str v)}])])))

(defn CellLabelGroup
  [cell-data _row _column]
  (uix/label-group-overflow-detector
    (fn []
      [ui/ListSA
       (for [v cell-data]
         ^{:key (str v)}
         [ui/ListItem [ui/Label {:style {:white-space :pre} :content v}]])])))

(defn CellBoolean
  [cell-data _row _column]
  [:div {:style {:text-align :center}}
   (if (true? cell-data)
     [icons/CheckIconFull]
     [icons/MinusIcon])])

(defn CellJson
  [cell-data row _column]
  [ui/Modal {:trigger    (r/as-element [:span [icons/ZoomInIcon {:style {:cursor :pointer}}]])
             :close-icon true}
   [ui/ModalHeader (or (:name row) (first (:names row)) (:uid row))]
   [ui/ModalContent {:scrolling true}
    [uix/EditorJson {:value     (general-utils/edn->json cell-data)
                     :read-only true}]]])

(defn CellRawText
  [cell-data row _column]
  [ui/Modal {:trigger    (r/as-element [:span [icons/ZoomInIcon {:style {:cursor :pointer}}]])
             :close-icon true}
   [ui/ModalHeader (or (:name row) (first (:names row)) (:uid row))]
   [ui/ModalContent {:scrolling true}
    [uix/EditorRawText {:value     cell-data
                        :read-only true}]]])

(defn CoeTable
  [{:keys [::!selected ::set-selected-fn ::!global-filter ::!pagination ::!can-action?] :as control} k]
  (let [{:keys [::!data ::!columns ::!default-columns ::row-id-fn]} (get control k)]
    [table/TableController
     {:!columns                      !columns
      :!default-columns              !default-columns
      :!current-columns              (subscribe [::main-subs/current-cols local-storage-key k])
      :set-current-columns-fn        #(dispatch [::set-current-cols k %])
      :!data                         !data
      :!enable-row-selection?        !can-action?
      :!selected                     !selected
      :set-selected-fn               set-selected-fn
      :!global-filter                !global-filter
      :!enable-pagination?           (r/atom true)
      :!pagination                   !pagination
      :!enable-global-filter?        (r/atom true)
      :!enable-column-customization? (r/atom true)
      :row-id-fn                     (or row-id-fn :Id)}]))

(defn MsgWarnDataDelayed []
  (r/with-let [tr (subscribe [::i18n-subs/tr])
               nb (subscribe [::subs/nuvlabox])]
    [uix/MsgWarn {:content [:span (@tr [:nuvlaedge-outdated-coe-info] [(:refresh-interval @nb)])]}]))

(defn DeleteMenuItem
  [{:keys [::docker-image-delete-action-fn ::!selected ::set-selected-fn
           ::!delete-modal-open? ::delete-modal-open-fn ::delete-modal-close-fn] :as control} k]
  (r/with-let [tr         (subscribe [::i18n-subs/tr])
               {:keys [::resource-type]} (get control k)
               header     (str/capitalize (@tr [:delete]))
               on-confirm #(do (dispatch [::coe-resource-actions
                                          {:docker (mapv (fn [id] {:resource resource-type :action "remove" :id id}) @!selected)}
                                          delete-modal-close-fn])
                               (set-selected-fn #{}))]
    [uix/ModalDanger
     {:with-confirm-step? true
      :button-text        header
      :on-confirm         on-confirm
      :content            [:<>
                           [MsgWarnDataDelayed]
                           [:p (@tr [:you-selected]) " "
                            (as-> (count @!selected) selected-count
                                  [:b selected-count " " resource-type (when (> selected-count 1) "s")]) " "
                            (@tr [:to-be-deleted]) ". " (@tr [:do-you-want-to-proceed?])]]
      :open               @!delete-modal-open?
      :on-close           delete-modal-close-fn
      :trigger            (r/as-element
                            [ui/MenuItem {:disabled (not (seq @!selected))
                                          :on-click delete-modal-open-fn}
                             [icons/TrashIcon] (@tr [:delete])])
      :header             header
      :header-class       [:nuvla-edges :delete-modal-header]}]))

(defn SearchInput
  [{:keys [::!global-filter ::!pagination] :as _control}]
  (r/with-let [tr (subscribe [::i18n-subs/tr])]
    [ui/MenuItem
     [ui/Input {:transparent true
                :placeholder (str (@tr [:search]) "...")
                :icon        (r/as-element [icons/SearchIcon])
                :on-change   (ui-callback/input-callback
                               #(do (reset! !global-filter %)
                                    (reset! !pagination default-pagination)))}]]))

(defn PullImageForm
  [{:keys [::!image-spec] :as _control}]
  (r/with-let [tr                           (subscribe [::i18n-subs/tr])
               ne-version                   (subscribe [::subs/ne-version])
               registries                   (subscribe [::subs/registries])
               set-use-private-registry?    #(swap! !image-spec assoc :use-private-registry? %)
               set-private-registry-id      (fn [id]
                                              (swap! !image-spec assoc
                                                     :private-registry-id id
                                                     :private-registry-cred-id nil))
               set-private-registry-cred-id #(swap! !image-spec assoc :private-registry-cred-id %)
               set-image-name               #(swap! !image-spec assoc :image %)]
    (let [{:keys [image use-private-registry? private-registry-id private-registry-cred-id]} @!image-spec
          registries-options      (subscribe [::subs/registries-options])
          registries-cred-options (subscribe [::subs/registries-credentials-options private-registry-id])]
      [ui/Form
       (when-not (empty? @registries)
         [:<>
          [tt/WithTooltip
           [:div [ui/FormCheckbox {:label     (@tr [:private-registry])
                                   :checked   use-private-registry?
                                   :disabled  (edges-utils/before-v2-19-2? @ne-version)
                                   :on-change (ui-callback/checked set-use-private-registry?)}]]
           (when (edges-utils/before-v2-19-2? @ne-version)
             (@tr [:image-pull-with-credentials-unavailable]))]
          (when use-private-registry?
            [ui/Table {:style {:margin-top 10}}
             [ui/TableHeader
              [ui/TableRow
               [ui/TableHeaderCell {:content (r/as-element
                                               [:<>
                                                (@tr [:private-registry])
                                                [:span {:style {:color "red" :margin-left "4px"}} "*"]])}]
               [ui/TableHeaderCell {:content (str/capitalize (@tr [:credential]))}]]]
             [ui/TableBody
              [ui/TableRow
               [ui/TableCell {:width 7}
                [ui/Dropdown
                 {:selection true
                  :fluid     true
                  :value     private-registry-id
                  :options   @registries-options
                  :on-change (ui-callback/value set-private-registry-id)}]]
               [ui/TableCell {:width 7}
                [ui/Dropdown
                 {:selection true
                  :fluid     true
                  :value     private-registry-cred-id
                  :options   @registries-cred-options
                  :on-change (ui-callback/value set-private-registry-cred-id)}]]]]])])
       [:div.field.required
        [:label "Image"]
        [ui/Input {:value       image
                   :placeholder "e.g. registry:port/image:tag"
                   :on-change   (ui-callback/input-callback set-image-name)}]]])))

(defn PullImageModal
  [_control]
  (let [!image-spec (r/atom {:use-private-registry? false
                             :image                 ""})]
    (dispatch [::events/get-registries])
    (dispatch [::events/get-registries-credentials])
    (fn [{:keys [::!selected ::!pull-modal-open? ::pull-modal-close-fn
                 ::pull-modal-open-fn ::pull-action-fn] :as control}]
      (let [{:keys [use-private-registry? private-registry-id image]} @!image-spec
            valid-form? (and (not (str/blank? image))
                             (or (not use-private-registry?)
                                 (some? private-registry-id)))]
        [ui/Modal {:open       @!pull-modal-open?
                   :close-icon true
                   :on-close   pull-modal-close-fn
                   :trigger    (r/as-element
                                 [ui/MenuItem {:on-click pull-modal-open-fn}
                                  [icons/DownloadIcon] "Pull"])}
         [ui/ModalHeader "Pull image"]
         [ui/ModalContent
          [:<>
           [MsgWarnDataDelayed]
           [PullImageForm (assoc control ::!image-spec !image-spec)]]]
         [ui/ModalActions
          [uix/Button {:primary  true
                       :disabled (not valid-form?)
                       :icon     icons/i-download
                       :content  "Pull image"
                       :on-click #(pull-action-fn @!image-spec)}]]]))))

(defmulti delete-available? identity)

(defmethod delete-available? :default [_k]
  false)

(defmulti pull-image-available? identity)

(defmethod pull-image-available? :default [_k]
  false)

(defn ActionBar
  [{:keys [::!can-action?] :as control} k]
  [ui/Menu
   (when @!can-action?
     [:<>
      (when (delete-available? k)
        [DeleteMenuItem control k])
      (when (pull-image-available? k)
        [PullImageModal control])])
   [ui/MenuMenu {:position "right"}
    [SearchInput control]]])

(defn COETabPane [control k]
  [ui/TabPane {:key k}
   [ActionBar control k]
   [CoeTable control k]])

(defn Tab [title-k-data-list]
  (r/with-let [!can-actions?         (subscribe [::subs/can-coe-resource-actions?])
               !pull-modal-open?     (r/atom false)
               !delete-modal-open?   (r/atom false)
               !selected             (r/atom #{})
               set-selected-fn       #(reset! !selected %)
               !global-filter        (r/atom "")
               !pagination           (r/atom default-pagination)
               close-pull-modal      #(reset! !pull-modal-open? false)
               delete-modal-close-fn #(reset! !delete-modal-open? false)
               pull-action-fn        (fn [{:keys [use-private-registry? private-registry-cred-id image]}]
                                       (dispatch [::coe-resource-actions
                                                  {:docker [(cond-> {:resource "image"
                                                                     :action   "pull"
                                                                     :id       image}
                                                                    (and use-private-registry? private-registry-cred-id)
                                                                    (assoc :credential private-registry-cred-id))]}
                                                  close-pull-modal]))
               control               (merge {::!can-action?          !can-actions?
                                             ::!delete-modal-open?   !delete-modal-open?
                                             ::delete-modal-open-fn  #(reset! !delete-modal-open? true)
                                             ::delete-modal-close-fn delete-modal-close-fn
                                             ::!selected             !selected
                                             ::set-selected-fn       set-selected-fn
                                             ::!global-filter        !global-filter
                                             ::!pagination           !pagination
                                             ::!pull-modal-open?     !pull-modal-open?
                                             ::pull-modal-open-fn    #(reset! !pull-modal-open? true)
                                             ::pull-modal-close-fn   close-pull-modal
                                             ::pull-action-fn        pull-action-fn}
                                            (into {} (mapv
                                                       (fn [[_title k data]]
                                                         [k data])
                                                       title-k-data-list)))]
    [ui/Tab
     {:menu          {:style     {:display   :flex
                                  :flex-wrap :wrap}
                      :secondary true
                      :pointing  true}
      :on-tab-change #(do (set-selected-fn #{})
                          (reset! !pagination default-pagination)
                          (reset! !global-filter ""))
      :panes         (mapv
                       (fn [[title k _data]]
                         {:menuItem title,
                          :render   #(r/as-element [COETabPane control k])})
                       title-k-data-list)}]))
