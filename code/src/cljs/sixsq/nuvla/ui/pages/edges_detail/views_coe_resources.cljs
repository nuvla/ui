(ns sixsq.nuvla.ui.pages.edges-detail.views-coe-resources
  (:require [re-frame.core :refer [dispatch subscribe reg-event-fx reg-sub]]
            [sixsq.nuvla.ui.common-components.plugins.table-refactor :as table-refactor]
            [sixsq.nuvla.ui.pages.data.utils :as data-utils]
            [sixsq.nuvla.ui.common-components.plugins.table :as table-plugin]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [reagent.core :as r]
            [sixsq.nuvla.ui.pages.edges-detail.subs :as subs]
            [sixsq.nuvla.ui.pages.edges-detail.spec :as spec]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))

(defn CellBytes
  [{cell-data :cell-data}]
  (data-utils/format-bytes cell-data))

(defn CellBytesBis
  [cell-data _row _column]
  (data-utils/format-bytes cell-data))

(defn CellTimeAgo
  [{:keys [cell-data]}]
  [uix/TimeAgo cell-data])

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
  [{:keys [cell-data row-data]}]
  (label-group-overflow-detector
    (fn []
      [ui/LabelGroup
       (for [[k v] cell-data]
         ^{:key (str (:Id row-data) k)}
         [ui/Label {:content k :detail (str v)}])])))

(defn PrimaryLabelGroup
  [{:keys [cell-data row-data]}]
  (label-group-overflow-detector
    (fn []
      [ui/LabelGroup {:color :blue}
       (for [v cell-data]
         ^{:key (str (:Id row-data) v)}
         [ui/Label {:content v}])])))

(defn SecondaryLabelGroup
  [{:keys [cell-data row-data]}]
  (label-group-overflow-detector
    (fn []
      [ui/LabelGroup
       (for [v cell-data]
         ^{:key (str (:Id row-data) v)}
         [ui/Label {:content v}])])))


(def field-id {:field-key      :id
               :header-content "Id"})
(def field-created {:field-key      :Created
                    :header-content "Created"
                    :cell           CellTimeAgo})
(def field-created-iso {:field-key      :Created
                        :header-content "Created"
                        :cell           CellTimeAgo})
(def field-created-at {:field-key      :CreatedAt
                       :header-content "Created"
                       :cell           CellTimeAgo})
;(def field-updated-at {:field-key      :UpdatedAt
;                       :header-content "Updated"
;                       :cell           CellTimeAgo})
(def field-labels {:field-key      :Labels
                   :header-content "Labels"
                   :no-sort?       true
                   :cell           KeyValueLabelGroup})
(def field-name {:field-key      :Name
                 :header-content "Name"})
(def field-driver {:field-key      :Driver
                   :header-content "Driver"})

(def local-storage-key "nuvla.ui.table.edges.docker.column-configs")

(main-events/reg-set-current-cols-event-fx ::set-current-cols local-storage-key)

(defn DockerTable
  [{:keys [rows columns default-columns sort-config-db-path db-path]}]
  [:<>
   [table-refactor/TableController
    {:!columns               (r/atom (mapv (fn [{:keys [field-key header-content no-sort?] :as c}]
                                             (cond-> {::table-refactor/field-key      field-key
                                                      ::table-refactor/header-content header-content
                                                      ::table-refactor/no-sort?       no-sort?}
                                                     (= field-key :SizeRw) (assoc ::table-refactor/field-cell CellBytesBis))) columns))
     :!default-columns       (r/atom (vec default-columns))
     :!current-columns       (subscribe [::main-subs/current-cols local-storage-key sort-config-db-path])
     :set-current-columns-fn #(dispatch [::set-current-cols sort-config-db-path %])
     :!data                  rows
     :!selectable?           (r/atom true)}]
   [table-plugin/TableColsEditable
    {:columns           columns
     :sort-config       {:db-path sort-config-db-path}
     :default-columns   (set default-columns)
     :table-props       {:stackable true}
     :wrapper-div-class nil
     :cell-props        {:header {:single-line true}}
     :rows              @rows}
    db-path]])

(defn DockerImagesTable [control]
  [DockerTable
   {:db-path             ::table-cols-edge-detail-coe-resource-docker-images
    :rows                (::!docker-images control)
    :columns             [field-id
                          {:field-key      :ParentId
                           :header-content "Parent Id"
                           :no-sort?       true}
                          {:field-key      :RepoDigests
                           :header-content "Repo Digests"
                           :no-sort?       true
                           :cell           SecondaryLabelGroup}
                          {:field-key      :Size
                           :header-content "Size"
                           :cell           CellBytes}
                          field-created
                          {:field-key      :RepoTags
                           :header-content "Tags"
                           :no-sort?       true
                           :cell           PrimaryLabelGroup}
                          field-labels]
    :sort-config-db-path ::spec/docker-images-ordering
    :default-columns     [:id :Size :Created :RepoTags]}])

;(defn PullImageMenuItem
;  [opts]
;  [ui/MenuItem {:disabled @(::!can-manage? opts)
;                :on-click (::docker-image-pull-modal-open-fn opts)}
;   [icons/DownloadIcon] "Pull"])
;
;(defn PullImageActionButton
;  [control image-value]
;  [uix/Button {:primary  true
;               :disabled @(r/track #(str/blank? @image-value))
;               :icon     icons/i-download
;               :content  "Pull image"
;               :on-click #((::docker-image-pull-action-fn control) @image-value)}])

;(defn PullImageModal
;  [control]
;  (r/with-let [image-value           (r/atom "")
;               update-image-value-fn #(reset! image-value %)]
;    [ui/Modal {:open       @(::!docker-image-pull-modal-open? control)
;               :close-icon true
;               :on-close   (::docker-image-pull-modal-close-fn control)
;               :trigger    (r/as-element [PullImageMenuItem control])}
;     [ui/ModalHeader "Pull image"]
;     [ui/ModalContent
;      [ui/Form
;       [ui/FormInput {:label     "Image" :required true :placeholder "e.g. registry:port/image:tag"
;                      :on-change (ui-callback/input-callback update-image-value-fn)}]]]
;     [ui/ModalActions
;      [PullImageActionButton control image-value]]]))

;(defn ImageActionBar
;  [control]
;  (r/with-let [tr (subscribe [::i18n-subs/tr])]
;    [ui/Menu
;     [PullImageModal control]
;     [ui/MenuItem {:on-click #()}
;      [icons/TrashIcon] "Remove"]
;     [ui/MenuMenu {:position "right"}
;      [ui/MenuItem
;       [ui/Input {:transparent true
;                  :placeholder (str (@tr [:search]) "...")
;                  :icon        (r/as-element [icons/SearchIcon])}]]]]))

(defn DockerImagePane
  [control]
  [ui/TabPane
   ;[ImageActionBar control]
   [DockerImagesTable control]])

(defn DockerVolumesTable [control]
  [DockerTable
   {:db-path             ::table-cols-edge-detail-coe-resource-docker-volumes
    :rows                (::!docker-volumes control)
    :columns             [{:field-key      :id
                           :header-content "Name"}
                          field-driver
                          {:field-key      :Scope
                           :header-content "Scope"}
                          {:field-key      :Mountpoint
                           :header-content "Mount point"}
                          field-created-at
                          field-labels]
    :sort-config-db-path ::spec/docker-volumes-ordering
    :default-columns     [:id :Driver :Mountpoint :CreatedAt :Labels]}])

(defn DockerVolumePane [control]
  [ui/TabPane [DockerVolumesTable control]])

(defn- DockerContainersTable [control]
  [DockerTable
   {:db-path             ::table-cols-edge-detail-coe-resource-docker-containers
    :rows                (::!docker-containers control)
    :columns             [field-id
                          {:field-key      :Image
                           :header-content "Image"}
                          field-created
                          {:field-key      :Status
                           :header-content "Status"}
                          {:field-key      :SizeRootFs
                           :header-content "Size RootFs"
                           :cell           CellBytes}
                          field-labels
                          {:field-key      :HostConfig
                           :header-content "Host config"
                           :cell           KeyValueLabelGroup}
                          {:field-key      :Names
                           :header-content "Names"
                           :no-sort?       true
                           :cell           SecondaryLabelGroup}
                          {:field-key      :SizeRw
                           :header-content "Size RW"
                           :cell           CellBytes}
                          {:field-key      :ImageID
                           :header-content "Image Id"}]
    :sort-config-db-path ::spec/docker-containers-ordering
    :default-columns     [:id :Image :SizeRootFs :Created :Status]}])

(defn DockerContainerPane [control]
  [ui/TabPane [DockerContainersTable control]])

(defn- DockerNetworksTable [control]
  [DockerTable
   {:db-path             ::table-cols-edge-detail-coe-resource-docker-networks
    :rows                (::!docker-networks control)
    :columns             [field-id
                          field-name
                          field-created-iso
                          field-driver
                          {:field-key      :Options
                           :header-content "Options"
                           :cell           KeyValueLabelGroup}
                          field-labels]
    :sort-config-db-path ::spec/docker-networks-ordering
    :default-columns     [:id :Name :Created :Labels :Driver]}])

(defn DockerNetworkPane [control]
  [ui/TabPane [DockerNetworksTable control]])

;(defn- DockerConfigsTable [control]
;  [DockerTable
;   {:db-path             ::table-cols-edge-detail-coe-resource-docker-configs
;    :rows                (::!docker-configs control)
;    :columns             [field-id
;                          field-name
;                          field-created-at
;                          field-updated-at
;                          {:field-key      :Version
;                           :header-content "Version"}
;                          {:field-key      :Data
;                           :header-content "Data"
;                           :no-sort?       true
;                           :cell           CellModalTextArea}
;                          field-labels]
;    :sort-config-db-path ::spec/docker-configs-ordering
;    :default-columns     #{:id :Name :CreatedAt :UpdatedAt :Data}}])
;
;(defn DockerConfigPane [control]
;  [ui/TabPane [DockerConfigsTable control]])

(defn Tab
  []
  (r/with-let [!can-manage?      (r/atom false)
               !pull-modal-open? (r/atom false)
               control           {::docker-image-pull-modal-open-fn  #(reset! !pull-modal-open? true)
                                  ::docker-image-pull-modal-close-fn #(reset! !pull-modal-open? false)
                                  ::docker-image-pull-action-fn      #(js/console.info ::docker-image-pull-action-fn %)
                                  ::!can-manage?                     !can-manage?
                                  ::!docker-image-pull-modal-open?   !pull-modal-open?
                                  ::!docker-images                   (subscribe [::subs/docker-images-ordered])
                                  ::!docker-containers               (subscribe [::subs/docker-containers-ordered])
                                  ::!docker-networks                 (subscribe [::subs/docker-networks-ordered])
                                  ;::!docker-configs                  (subscribe [::subs/docker-configs-ordered])
                                  ::!docker-volumes                  (subscribe [::subs/docker-volumes-ordered])}]
    [ui/Tab
     {:panes [{:menuItem "Containers", :render #(r/as-element [DockerContainerPane control])}
              {:menuItem "Images", :render #(r/as-element [DockerImagePane control])}
              {:menuItem "Volumes", :render #(r/as-element [DockerVolumePane control])}
              {:menuItem "Networks", :render #(r/as-element [DockerNetworkPane control])}
              ;{:menuItem "Configs", :render #(r/as-element [DockerConfigPane control])}
              ]}]))
