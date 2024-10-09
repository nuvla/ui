(ns sixsq.nuvla.ui.pages.edges-detail.views-coe-resources
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe reg-event-fx reg-sub]]
            [re-frame.cofx :refer [inject-cofx]]
            [sixsq.nuvla.ui.pages.edges-detail.events :as events]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.plugins.table-refactor :as table]
            [sixsq.nuvla.ui.pages.data.utils :as data-utils]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [reagent.core :as r]
            [sixsq.nuvla.ui.pages.edges-detail.subs :as subs]
            [sixsq.nuvla.ui.pages.edges-detail.spec :as spec]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(def local-storage-key "nuvla.ui.table.edges.docker.column-configs")

(reg-sub
  ::current-cols
  (fn [db [_ k]]
    (let [ls   (aget js/window "localStorage")
          data (or
                 (get db local-storage-key)
                 (edn/read-string (.getItem ls local-storage-key)))]
      (get data k))))

(reg-event-fx
  ::set-current-cols
  [(inject-cofx :storage/get {:name local-storage-key})]
  (fn [{storage :storage/get
        db      :db} [_ k columns]]
    {:db          (assoc-in db [local-storage-key k] columns)
     :storage/set {:session? false
                   :name     local-storage-key
                   :value    (merge (or (edn/read-string storage) {}) {k columns})}}))

(reg-event-fx
  ::coe-resource-actions
  (fn [{{:keys [::spec/nuvlabox]} :db} [_ payload close-modal-fn]]
    {:fx [[:dispatch [::events/operation (:id nuvlabox) "coe-resource-actions" payload
                      close-modal-fn close-modal-fn]]]}))

(defn CellBytes
  [cell-data _row _column]
  (data-utils/format-bytes cell-data))

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
                                              :max-height (if @show-more? nil "10ch")}}
                                [Component args]]
                               (when @overflow?
                                 [ui/Button {:style    {:margin-top "0.5em"}
                                             :basic    true
                                             :on-click #(swap! show-more? not)
                                             :size     :mini} (if @show-more? "▲" "▼")])])
       :component-did-mount #(reset! overflow? (general-utils/overflowed? @ref))})))

(defn KeyValueLabelGroup
  [cell-data _row _column]
  (label-group-overflow-detector
    (fn []
      [ui/LabelGroup
       (for [[k v] cell-data]
         ^{:key k}
         [ui/Label {:content k :detail (str v)}])])))

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
      [ui/LabelGroup
       (for [v cell-data]
         ^{:key (str v)}
         [ui/Label {:content v}])])))

(def field-id {::table/field-key      :id
               ::table/header-content "Id"})
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
  [{:keys [rows columns default-columns sort-config-key !selected
           set-selected-fn !global-filter]}]
  [table/TableController
   {:!columns               (r/atom columns)
    :!default-columns       (r/atom default-columns)
    :!current-columns       (subscribe [::current-cols sort-config-key])
    :set-current-columns-fn #(dispatch [::set-current-cols sort-config-key %])
    :!data                  rows
    :!enable-row-selection? (r/atom true)
    :!selected              !selected
    :set-selected-fn        set-selected-fn
    :!global-filter         !global-filter
    :!enable-global-filter? (r/atom true)}])

(defn DockerImagesTable
  [{:keys [::!selected ::set-selected-fn ::!global-filter ::global-filter-fn] :as control}]
  [DockerTable
   {:rows             (::!docker-images control)
    :columns          [field-id
                       {::table/field-key      :ParentId
                        ::table/header-content "Parent Id"
                        ::table/no-sort?       true}
                       {::table/field-key      :RepoDigests
                        ::table/header-content "Repo Digests"
                        ::table/field-cell     SecondaryLabelGroup
                        ::table/no-sort?       true}
                       {::table/field-key      :Size
                        ::table/header-content "Size"
                        ::table/field-cell     CellBytes}
                       field-created
                       {::table/field-key      :RepoTags
                        ::table/header-content "Tags"
                        ::table/no-sort?       true
                        ::table/field-cell     PrimaryLabelGroup}
                       field-labels
                       {::table/field-key      :Repository
                        ::table/header-content "Repository"}
                       {::table/field-key      :Tag
                        ::table/header-content "Tag"}]
    :default-columns  [:id :Size :Created :RepoTags]
    :!selected        !selected
    :set-selected-fn  set-selected-fn
    :sort-config-key  :docker-images
    :!global-filter   !global-filter}])

(defn PullImageMenuItem
  [opts]
  [ui/MenuItem {:disabled @(::!can-manage? opts)
                :on-click (::docker-image-pull-modal-open-fn opts)}
   [icons/DownloadIcon] "Pull"])

(defn PullImageActionButton
  [control image-value]
  [uix/Button {:primary  true
               :disabled @(r/track #(str/blank? @image-value))
               :icon     icons/i-download
               :content  "Pull image"
               :on-click #((::docker-image-pull-action-fn control) @image-value)}])

(defn PullImageModal
  [control]
  (r/with-let [image-value           (r/atom "")
               update-image-value-fn #(reset! image-value %)]
    [ui/Modal {:open       @(::!docker-image-pull-modal-open? control)
               :close-icon true
               :on-close   (::docker-image-pull-modal-close-fn control)
               :trigger    (r/as-element [PullImageMenuItem control])}
     [ui/ModalHeader "Pull image"]
     [ui/ModalContent
      [ui/Form
       [ui/FormInput {:label     "Image" :required true :placeholder "e.g. registry:port/image:tag"
                      :on-change (ui-callback/input-callback update-image-value-fn)}]]]
     [ui/ModalActions
      [PullImageActionButton control image-value]]]))

(defn DeleteMenuItem
  [{:keys [on-confirm enabled? !delete-modal-open? on-click]}]
  (r/with-let [tr     (subscribe [::i18n-subs/tr])
               header (str (str/capitalize (@tr [:delete])) " " "images")]
    [uix/ModalDanger
     {:with-confirm-step? true
      :button-text        header
      :on-confirm         on-confirm
      :content            (@tr [:are-you-sure?])
      :open               @!delete-modal-open?
      :trigger            (r/as-element
                            [ui/MenuItem {:disabled (not enabled?)
                                          :on-click on-click}
                             [icons/TrashIcon] (@tr [:delete])])
      :header             header
      :header-class       [:nuvla-edges :delete-modal-header]}]))

(defn DeleteImageMenuItem
  [{:keys [::docker-image-delete-action-fn ::!selected ::set-selected-fn
           ::!delete-modal-open? ::delete-modal-open-fn] :as _control}]
  [DeleteMenuItem {:on-confirm          #(do (docker-image-delete-action-fn @!selected)
                                             (set-selected-fn #{}))
                   :enabled?            (seq @!selected)
                   :!delete-modal-open? !delete-modal-open?
                   :on-click            delete-modal-open-fn}])

(defn SearchInput
  [{:keys [::!global-filter] :as _control}]
  (r/with-let [tr (subscribe [::i18n-subs/tr])]
    [ui/MenuItem
     [ui/Input {:transparent true
                :placeholder (str (@tr [:search]) "...")
                :icon        (r/as-element [icons/SearchIcon])
                :on-change   (ui-callback/input-callback #(reset! !global-filter %))}]]))

(defn ImageActionBar
  [control]
  (r/with-let [tr (subscribe [::i18n-subs/tr])]
    [ui/Menu
     [PullImageModal control]
     [DeleteImageMenuItem control]
     [ui/MenuMenu {:position "right"}
      [SearchInput control]]]))

(defn DockerImagePane
  [control]
  [ui/TabPane
   [ImageActionBar control]
   [DockerImagesTable control]])

(defn DockerVolumesTable [control]
  [DockerTable
   {:rows            (::!docker-volumes control)
    :columns         [{::table/field-key      :id
                       ::table/header-content "Name"}
                      field-driver
                      {::table/field-key      :Scope
                       ::table/header-content "Scope"}
                      {::table/field-key      :Mountpoint
                       ::table/header-content "Mount point"}
                      field-created-at
                      field-labels
                      {::table/field-key      :Options
                       ::table/header-content "Options"}]
    :default-columns [:id :Driver :Mountpoint :CreatedAt :Labels]
    :sort-config-key :docker-volumes}])

(defn DockerVolumePane [control]
  [ui/TabPane [DockerVolumesTable control]])

(defn- DockerContainersTable
  [control]
  [DockerTable
   {:rows            (::!docker-containers control)
    :columns         [field-id
                      {::table/field-key      :Image
                       ::table/header-content "Image"}
                      field-created
                      {::table/field-key      :Status
                       ::table/header-content "Status"}
                      {::table/field-key      :SizeRootFs
                       ::table/header-content "Size RootFs"
                       ::table/field-cell     CellBytes}
                      field-labels
                      {::table/field-key      :HostConfig
                       ::table/header-content "Host config"
                       ::table/field-cell     KeyValueLabelGroup}
                      {::table/field-key      :Names
                       ::table/header-content "Names"
                       ::table/field-cell     SecondaryLabelGroup}
                      {::table/field-key      :SizeRw
                       ::table/header-content "Size RW"
                       ::table/field-cell     CellBytes}
                      {::table/field-key      :ImageID
                       ::table/header-content "Image Id"}
                      {::table/field-key      :Mounts
                       ::table/header-content "Mounts"
                       ::table/no-sort?       true}
                      {::table/field-key      :Name
                       ::table/header-content "Name"}
                      {::table/field-key      :NetworkSettings
                       ::table/header-content "NetworkSettings"
                       ::table/no-sort?       true}
                      {::table/field-key      :State
                       ::table/header-content "State"}
                      {::table/field-key      :Command
                       ::table/header-content "Command"}
                      {::table/field-key      :Ports
                       ::table/header-content "Ports"}]
    :default-columns [:id :Image :Created :Status :SizeRootFs]
    :sort-config-key :docker-containers}])

(defn DockerContainerPane [control]
  [ui/TabPane [DockerContainersTable control]])

(defn- DockerNetworksTable [control]
  [DockerTable
   {:rows            (::!docker-networks control)
    :columns         [field-id
                      field-name
                      field-created-iso
                      field-driver
                      {::table/field-key      :Options
                       ::table/header-content "Options"
                       ::table/field-cell     KeyValueLabelGroup}
                      field-labels
                      {::table/field-key      :Attachable
                       ::table/header-content "Attachable"}
                      {::table/field-key      :ConfigFrom
                       ::table/header-content "ConfigFrom"}
                      {::table/field-key      :ConfigOnly
                       ::table/header-content "ConfigOnly"}
                      {::table/field-key      :EnableIPv6
                       ::table/header-content "EnableIPv6"}
                      {::table/field-key      :IPAM
                       ::table/header-content "IPAM"}
                      {::table/field-key      :Ingress
                       ::table/header-content "Internal"}
                      {::table/field-key      :Scope
                       ::table/header-content "Scope"}]
    :default-columns [:id :Name :Created :Labels :Driver]
    :sort-config-key :docker-networks}])

(defn DockerNetworkPane [control]
  [ui/TabPane [DockerNetworksTable control]])

(defn Tab
  []
  (r/with-let [!can-manage?        (r/atom false)
               !pull-modal-open?   (r/atom false)
               !delete-modal-open? (r/atom false)
               !selected           (r/atom #{})
               set-selected-fn     #(reset! !selected %)
               !global-filter      (r/atom "")
               close-pull-modal    #(reset! !pull-modal-open? false)
               control             {::docker-image-pull-modal-open-fn  #(reset! !pull-modal-open? true)
                                    ::docker-image-pull-modal-close-fn close-pull-modal
                                    ::docker-image-pull-action-fn      #(dispatch [::coe-resource-actions {:docker [{:resource "image" :action "pull" :id %}]}
                                                                                   close-pull-modal])
                                    ::!delete-modal-open?              !delete-modal-open?
                                    ::delete-modal-open-fn             #(reset! !delete-modal-open? true)
                                    ::docker-image-delete-action-fn    #(dispatch [::coe-resource-actions {:docker (mapv (fn [id] {:resource "image" :action "remove" :id id}) %)}
                                                                                   (fn [] (reset! !delete-modal-open? false))])
                                    ::!can-manage?                     !can-manage?
                                    ::!docker-image-pull-modal-open?   !pull-modal-open?
                                    ::!selected                        !selected
                                    ::set-selected-fn                  set-selected-fn
                                    ::!docker-images                   (subscribe [::subs/docker-images-clean])
                                    ::!docker-containers               (subscribe [::subs/docker-containers-clean])
                                    ::!docker-networks                 (subscribe [::subs/docker-networks-clean])
                                    ::!docker-volumes                  (subscribe [::subs/docker-volumes-clean])
                                    ::!global-filter                   !global-filter}]
    [ui/Tab
     {:on-tab-change #(do (set-selected-fn #{})
                          (reset! !global-filter ""))
      :panes         [{:menuItem "Containers", :render #(r/as-element [DockerContainerPane control])}
                      {:menuItem "Images", :render #(r/as-element [DockerImagePane control])}
                      {:menuItem "Volumes", :render #(r/as-element [DockerVolumePane control])}
                      {:menuItem "Networks", :render #(r/as-element [DockerNetworkPane control])}]}]))
