(ns sixsq.nuvla.ui.pages.edges-detail.views-coe-resources
  (:require [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.pages.data.utils :as data-utils]
            [sixsq.nuvla.ui.common-components.plugins.table :as table-plugin]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [reagent.core :as r]
            [sixsq.nuvla.ui.pages.edges-detail.subs :as subs]
            [sixsq.nuvla.ui.pages.edges-detail.spec :as spec]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))

(defn CellBytes
  [{cell-data :cell-data}]
  (data-utils/format-bytes cell-data))

(defn CellTimeAgo
  [{:keys [cell-data]}]
  [uix/TimeAgo cell-data])

(defn CellModalTextArea
  [{:keys [cell-data]}]
  [ui/Modal {:close-icon true
             :trigger    (r/as-element [ui/Icon {:name "magnify"
                                                 :style {:cursor :pointer}}])}
   [ui/ModalHeader "Data"]
   [ui/ModalContent
    [ui/Form
     [ui/TextArea {:default-value cell-data
                   :disabled      true
                   :rows          10}]]]])

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
(def field-updated-at {:field-key      :UpdatedAt
                       :header-content "Updated"
                       :cell           CellTimeAgo})
(def field-labels {:field-key      :Labels
                   :header-content "Labels"
                   :no-sort?       true
                   :cell           KeyValueLabelGroup})
(def field-name {:field-key      :Name
                 :header-content "Name"})
(def field-driver {:field-key      :Driver
                   :header-content "Driver"})

(defn DockerTable
  [{:keys [columns default-columns sort-config-db-path subscribe-key db-path]}]
  (let [images-ordered @(subscribe [subscribe-key])]
    [table-plugin/TableColsEditable
     {:columns           columns
      :sort-config       {:db-path sort-config-db-path}
      :default-columns   default-columns
      :table-props       {:stackable true}
      :wrapper-div-class nil
      :cell-props        {:header {:single-line true}}
      :rows              images-ordered}
     db-path]))

(defn DockerImagesTable []
  [DockerTable
   {:db-path             ::table-cols-edge-detail-coe-resource-docker-images
    :subscribe-key       ::subs/docker-images-ordered
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
    :default-columns     #{:id :Size :Created :RepoTags}}])

(defn DockerVolumesTable []
  [DockerTable
   {:db-path             ::table-cols-edge-detail-coe-resource-docker-volumes
    :subscribe-key       ::subs/docker-volumes-ordered
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
    :default-columns     #{:id :Driver :Mountpoint :CreatedAt :Labels}}])

(defn- DockerContainersTable []
  [DockerTable
   {:db-path             ::table-cols-edge-detail-coe-resource-docker-containers
    :subscribe-key       ::subs/docker-containers-ordered
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
    :sort-config-db-path ::spec/docker-volumes-ordering
    :default-columns     #{:id :Image :SizeRootFs :Created :Status}}])

(defn- DockerNetworksTable []
  [DockerTable
   {:db-path             ::table-cols-edge-detail-coe-resource-docker-networks
    :subscribe-key       ::subs/docker-networks-ordered
    :columns             [field-id
                          field-name
                          field-created-iso
                          field-driver
                          {:field-key      :Options
                           :header-content "Options"
                           :cell           KeyValueLabelGroup}
                          field-labels]
    :sort-config-db-path ::spec/docker-networks-ordering
    :default-columns     #{:id :Name :Created :Labels :Driver}}])

(defn- DockerConfigsTable []
  [DockerTable
   {:db-path             ::table-cols-edge-detail-coe-resource-docker-configs
    :subscribe-key       ::subs/docker-configs-ordered
    :columns             [field-id
                          field-name
                          field-created-at
                          field-updated-at
                          {:field-key      :Version
                           :header-content "Version"}
                          {:field-key      :Data
                           :header-content "Data"
                           :no-sort?       true
                           :cell           CellModalTextArea}
                          field-labels]
    :sort-config-db-path ::spec/docker-configs-ordering
    :default-columns     #{:id :Name :CreatedAt :UpdatedAt :Data}}])

(defn Tab
  []
  [ui/Tab
   {:panes [{:menuItem "Containers", :render #(r/as-element [ui/TabPane [DockerContainersTable]])}
            {:menuItem "Images", :render #(r/as-element [ui/TabPane [DockerImagesTable]])}
            {:menuItem "Volumes", :render #(r/as-element [ui/TabPane [DockerVolumesTable]])}
            {:menuItem "Networks", :render #(r/as-element [ui/TabPane [DockerNetworksTable]])}
            {:menuItem "Configs", :render #(r/as-element [ui/TabPane [DockerConfigsTable]])}]}])
