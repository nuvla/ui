(ns sixsq.nuvla.ui.pages.edges-detail.views-coe-resources
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe reg-event-fx reg-sub]]
            [sixsq.nuvla.ui.pages.edges-detail.events :as events]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.plugins.table-refactor :as table]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.job.views :as job-views]
            [sixsq.nuvla.ui.pages.edges-detail.subs :as subs]
            [sixsq.nuvla.ui.pages.edges-detail.spec :as spec]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(def local-storage-key "nuvla.ui.table.edges.docker.column-configs")

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

(defn label-group-overflow-detector
  [Component]
  (r/with-let [ref        (atom nil)
               overflow?  (r/atom false)
               show-more? (r/atom false)]
    (r/create-class
      {:display-name        "LabelGroupOverflow"
       :reagent-render      (fn [args]
                              [:div
                               [:div {:ref   #(reset! ref %)
                                      :style {:overflow   :hidden
                                              :max-height (if @show-more? nil "15ch")}}
                                [Component args]]
                               (when @overflow?
                                 [:div {:style {:display         :flex
                                                :justify-content :center}}
                                  [ui/Button {:style    {:margin-top    "0.5em"
                                                         :margin-bottom "0.5em"}
                                              :basic    true
                                              :on-click #(swap! show-more? not)
                                              :size     :mini} (if @show-more? "▲" "▼")]])])
       :component-did-mount #(reset! overflow? (general-utils/overflowed? @ref))})))

(defmethod job-views/JobCell "coe_resource_actions"
  [{:keys [status-message] :as resource}]
  (if-let [responses (some-> status-message general-utils/json->edn :docker)]
    (label-group-overflow-detector
      (fn []
        [ui/ListSA {:divided true :relaxed :very}
         (for [{:keys [success content message return-code] :as response} responses]
           ^{:key (random-uuid)}                           ;;fixme
           [ui/ListItem {:style {:display     :flex
                                 :align-items :center}}
            [:div
             [ui/Label {:horizontal true
                        :color      (if success "green" "red")} return-code]]
            [ui/ListContent
             (or message content)]])]))
    [job-views/DefaultJobCell resource]))

(defn KeyValueLabelGroup
  [cell-data _row _column]
  (label-group-overflow-detector
    (fn []
      [ui/LabelGroup
       (for [[k v] cell-data]
         ^{:key k}
         [ui/Label {:style   {:white-space :pre}
                    :content k :detail (str v)}])])))

(defn PrimaryLabelGroup
  [cell-data _row _column]
  (label-group-overflow-detector
    (fn []
      [ui/LabelGroup {:color :blue}
       (for [v cell-data]
         ^{:key (str v)}
         [ui/Label {:content v}])])))

(defn SecondaryLabelGroup
  [cell-data _row _column]
  (label-group-overflow-detector
    (fn []
      [ui/ListSA
       (for [v cell-data]
         ^{:key (str v)}
         [ui/ListItem [ui/Label {:style {:white-space :pre} :content v}]])])))

(defn Boolean
  [cell-data _row _column]
  [:div {:style {:text-align :center}}
   (if (true? cell-data)
     [icons/CheckIconFull]
     [icons/MinusIcon])])

(def field-id {::table/field-key      :Id
               ::table/header-content "Id"
               ::table/field-cell     (table/CellOverflowTooltipAs :div.max-width-12ch.ellipsing)})
(def field-created {::table/field-key      :Created
                    ::table/header-content "Created"
                    ::table/field-cell     table/CellTimeAgo})
(def field-created-iso {::table/field-key      :Created
                        ::table/header-content "Created"
                        ::table/field-cell     table/CellTimeAgo})
(def field-created-at {::table/field-key      :CreatedAt
                       ::table/header-content "Created"
                       ::table/field-cell     table/CellTimeAgo})
(def field-labels {::table/field-key      :Labels
                   ::table/header-content "Labels"
                   ::table/no-sort?       true
                   ::table/field-cell     KeyValueLabelGroup})
(def field-name {::table/field-key      :Name
                 ::table/header-content "Name"})
(def field-driver {::table/field-key      :Driver
                   ::table/header-content "Driver"})

(defn DockerTable
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

(defn PullImageModal
  [{:keys [::!can-action? ::!selected] :as control} k]
  (r/with-let [image-value           (r/atom "")
               update-image-value-fn #(reset! image-value %)
               {:keys [::!pull-modal-open? ::pull-modal-close-fn
                       ::pull-modal-open-fn ::pull-action-fn]} (get control k)]
    [ui/Modal {:open       @!pull-modal-open?
               :close-icon true
               :on-close   pull-modal-close-fn
               :trigger    (r/as-element
                             [ui/MenuItem {:disabled (not @!can-action?)
                                           :on-click pull-modal-open-fn}
                              [icons/DownloadIcon] "Pull"])}
     [ui/ModalHeader "Pull image"]
     [ui/ModalContent
      [ui/Form
       [ui/FormInput {:label     "Image" :required true :placeholder "e.g. registry:port/image:tag"
                      :on-change (ui-callback/input-callback update-image-value-fn)}]]]
     [ui/ModalActions
      [uix/Button {:primary  true
                   :disabled @(r/track #(str/blank? @image-value))
                   :icon     icons/i-download
                   :content  "Pull image"
                   :on-click #(pull-action-fn @image-value)}]]]))

(defn DeleteMenuItem
  [{:keys [::docker-image-delete-action-fn ::!selected ::set-selected-fn ::!action-disabled
           ::!delete-modal-open? ::delete-modal-open-fn ::delete-modal-close-fn] :as control} k]
  (r/with-let [tr  (subscribe [::i18n-subs/tr])
               {:keys [::delete-fn ::resource-type]} (get control k)
               msg (str (str/capitalize (@tr [:delete])) " " resource-type)]
    [uix/ModalDanger
     {:with-confirm-step? true
      :button-text        msg
      :on-confirm         #(do
                             (delete-fn)
                             (set-selected-fn #{}))
      :content            (@tr [:are-you-sure?])
      :open               @!delete-modal-open?
      :on-close           delete-modal-close-fn
      :trigger            (r/as-element
                            [ui/MenuItem {:disabled @!action-disabled
                                          :on-click delete-modal-open-fn}
                             [icons/TrashIcon] (@tr [:delete])])
      :header             msg
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

(defn ActionBar
  [control k]
  [ui/Menu
   [DeleteMenuItem control k]
   (when (= k ::docker-images)
     [PullImageModal control k])
   [ui/MenuMenu {:position "right"}
    [SearchInput control]]])

(defn COETabPane [control k]
  [ui/TabPane {:key k}
   [ActionBar control k]
   [DockerTable control k]])

(defn Tab []
  (r/with-let [!can-actions?         (subscribe [::subs/can-coe-resource-actions?])
               !pull-modal-open?     (r/atom false)
               !delete-modal-open?   (r/atom false)
               !selected             (r/atom #{})
               set-selected-fn       #(reset! !selected %)
               !global-filter        (r/atom "")
               !pagination           (r/atom default-pagination)
               close-pull-modal      #(reset! !pull-modal-open? false)
               delete-modal-close-fn #(reset! !delete-modal-open? false)
               delete-resource-fn    (fn [resource-type]
                                       (dispatch [::coe-resource-actions {:docker (mapv (fn [id] {:resource resource-type :action "remove" :id id}) @!selected)}
                                                  delete-modal-close-fn]))
               docker-images         {::!data               (subscribe [::subs/docker-images-clean])
                                      ::!columns            (r/atom [field-id
                                                                     {::table/field-key      :ParentId
                                                                      ::table/header-content "Parent Id"
                                                                      ::table/no-sort?       true}
                                                                     {::table/field-key      :RepoDigests
                                                                      ::table/header-content "Repo Digests"
                                                                      ::table/field-cell     SecondaryLabelGroup
                                                                      ::table/no-sort?       true}
                                                                     {::table/field-key      :Size
                                                                      ::table/header-content "Size"
                                                                      ::table/field-cell     table/CellBytes}
                                                                     field-created
                                                                     {::table/field-key      :RepoTags
                                                                      ::table/header-content "Tags"
                                                                      ;::table/no-sort?       true
                                                                      ::table/field-cell     PrimaryLabelGroup}
                                                                     field-labels
                                                                     {::table/field-key      :Repository
                                                                      ::table/header-content "Repository"}
                                                                     {::table/field-key      :Tag
                                                                      ::table/header-content "Tag"}])
                                      ::!default-columns    (r/atom [:Id :Size :Created :RepoTags])
                                      ::resource-type       "images"
                                      ::delete-fn           (partial delete-resource-fn "image")
                                      ::!pull-modal-open?   !pull-modal-open?
                                      ::pull-modal-open-fn  #(reset! !pull-modal-open? true)
                                      ::pull-modal-close-fn close-pull-modal
                                      ::pull-action-fn      #(dispatch [::coe-resource-actions {:docker [{:resource "image" :action "pull" :id %}]}
                                                                        close-pull-modal])}
               docker-containers     {::!data            (subscribe [::subs/docker-containers-clean])
                                      ::!columns         (r/atom [field-id
                                                                  {::table/field-key      :Image
                                                                   ::table/header-content "Image"}
                                                                  field-created
                                                                  {::table/field-key      :Status
                                                                   ::table/header-content "Status"}
                                                                  {::table/field-key      :SizeRootFs
                                                                   ::table/header-content "Size RootFs"
                                                                   ::table/field-cell     table/CellBytes}
                                                                  field-labels
                                                                  {::table/field-key      :HostConfig
                                                                   ::table/header-content "Host config"
                                                                   ::table/no-sort?       true
                                                                   ::table/field-cell     KeyValueLabelGroup}
                                                                  {::table/field-key      :Names
                                                                   ::table/header-content "Names"
                                                                   ::table/field-cell     SecondaryLabelGroup}
                                                                  {::table/field-key      :SizeRw
                                                                   ::table/header-content "Size RW"
                                                                   ::table/field-cell     table/CellBytes}
                                                                  {::table/field-key      :ImageID
                                                                   ::table/header-content "Image Id"}
                                                                  {::table/field-key      :Mounts
                                                                   ::table/header-content "Mounts"
                                                                   ::table/field-cell     SecondaryLabelGroup
                                                                   ::table/no-sort?       true}
                                                                  {::table/field-key      :Name
                                                                   ::table/header-content "Name"}
                                                                  {::table/field-key      :NetworkSettings
                                                                   ::table/header-content "Networks"
                                                                   ::table/field-cell     SecondaryLabelGroup
                                                                   ::table/no-sort?       true}
                                                                  {::table/field-key      :State
                                                                   ::table/header-content "State"}
                                                                  {::table/field-key      :Command
                                                                   ::table/header-content "Command"}
                                                                  {::table/field-key      :Ports
                                                                   ::table/header-content "Ports"
                                                                   ::table/no-sort?       true
                                                                   ::table/field-cell     SecondaryLabelGroup}])
                                      ::!default-columns (r/atom [:Id :Name :Image :Status :Created :Ports])
                                      ::resource-type    "containers"
                                      ::delete-fn        (partial delete-resource-fn "container")}
               docker-volumes        {::!data            (subscribe [::subs/docker-volumes])
                                      ::!columns         (r/atom [{::table/field-key      :Name
                                                                   ::table/header-content "Name"
                                                                   ::table/field-cell     (table/CellOverflowTooltipAs :div.max-width-50ch.ellipsing)}
                                                                  field-driver
                                                                  {::table/field-key      :Scope
                                                                   ::table/header-content "Scope"}
                                                                  {::table/field-key      :Mountpoint
                                                                   ::table/header-content "Mount point"}
                                                                  field-created-at
                                                                  field-labels
                                                                  {::table/field-key      :Options
                                                                   ::table/header-content "Options"}])
                                      ::row-id-fn        :Name
                                      ::!default-columns (r/atom [:Name :CreatedAt :Driver])
                                      ::resource-type    "volumes"}
               docker-networks       {::!data            (subscribe [::subs/docker-networks-clean])
                                      ::!columns         (r/atom [field-id
                                                                  field-name
                                                                  field-created-iso
                                                                  field-driver
                                                                  {::table/field-key      :Options
                                                                   ::table/header-content "Options"
                                                                   ::table/field-cell     KeyValueLabelGroup}
                                                                  field-labels
                                                                  {::table/field-key      :ConfigFrom
                                                                   ::table/header-content "ConfigFrom"}
                                                                  {::table/field-key      :ConfigOnly
                                                                   ::table/header-content "ConfigOnly"}
                                                                  {::table/field-key      :IPAM
                                                                   ::table/header-content "IPAM"
                                                                   ::table/field-cell     KeyValueLabelGroup}
                                                                  {::table/field-key      :Attachable
                                                                   ::table/header-content "Attachable"
                                                                   ::table/field-cell     Boolean
                                                                   ::table/div-class      ["slideways-lr"]}
                                                                  {::table/field-key      :EnableIPv6
                                                                   ::table/header-content "IPv6"
                                                                   ::table/field-cell     Boolean
                                                                   ::table/div-class      ["slideways-lr"]}
                                                                  {::table/field-key      :Ingress
                                                                   ::table/header-content "Ingress"
                                                                   ::table/field-cell     Boolean
                                                                   ::table/div-class      ["slideways-lr"]}
                                                                  {::table/field-key      :Internal
                                                                   ::table/header-content "Internal"
                                                                   ::table/field-cell     Boolean
                                                                   ::table/div-class      ["slideways-lr"]}
                                                                  {::table/field-key      :Scope
                                                                   ::table/header-content "Scope"}])
                                      ::!default-columns (r/atom [:Id :Name :Created :Driver :Scope :Attachable :Internal :Ingress :EnableIPv6 :IPAM])
                                      ::resource-type    "networks"
                                      ::delete-fn        (partial delete-resource-fn "network")}
               control               {::docker-images         docker-images
                                      ::docker-containers     docker-containers
                                      ::docker-volumes        docker-volumes
                                      ::docker-networks       docker-networks
                                      ::!can-action?          !can-actions?
                                      ::!delete-modal-open?   !delete-modal-open?
                                      ::delete-modal-open-fn  #(reset! !delete-modal-open? true)
                                      ::delete-modal-close-fn delete-modal-close-fn
                                      ::!selected             !selected
                                      ::!action-disabled      (r/track (fn action-disabled [] (not (and @!can-actions? (seq @!selected)))))
                                      ::set-selected-fn       set-selected-fn
                                      ::!global-filter        !global-filter
                                      ::!pagination           !pagination}]
    [ui/Tab
     {:on-tab-change #(do (set-selected-fn #{})
                          (reset! !pagination default-pagination)
                          (reset! !global-filter ""))
      :panes         [{:menuItem "Containers",
                       :render   #(r/as-element [COETabPane control ::docker-containers])}
                      {:menuItem "Images",
                       :render   #(r/as-element [COETabPane control ::docker-images])}
                      {:menuItem "Volumes",
                       :render   #(r/as-element [COETabPane control ::docker-volumes])}
                      {:menuItem "Networks",
                       :render   #(r/as-element [COETabPane control ::docker-networks])}]}]))
