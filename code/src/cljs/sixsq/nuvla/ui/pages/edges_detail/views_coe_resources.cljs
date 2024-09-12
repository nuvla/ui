(ns sixsq.nuvla.ui.pages.edges-detail.views-coe-resources
  (:require [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.pages.data.utils :as data-utils]
            [sixsq.nuvla.ui.common-components.plugins.table :as table-plugin]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [reagent.core :as r]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.pages.edges-detail.subs :as subs]
            [sixsq.nuvla.ui.pages.edges-detail.spec :as spec]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))

(defn CellBytes
  [{cell-data :cell-data}]
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


(def field-id {:field-key      :Id
               :header-content "Id"})
(def field-created {:field-key      :Created
                    :header-content "Created"
                    :cell           (fn [{:keys [cell-data]}]
                                      [uix/TimeAgo (some-> cell-data
                                                           time/parse-unix
                                                           time/time->utc-str)])})
(def field-labels {:field-key      :Labels
                   :header-content "Labels"
                   :no-sort?       true
                   :cell           KeyValueLabelGroup})

(defn DockerImagesTable []
  (let [images-ordered @(subscribe [::subs/docker-images-ordered])]
    [table-plugin/TableColsEditable
     {:columns           [field-id
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
      :sort-config       {:db-path ::spec/docker-images-ordering}
      :default-columns   #{:Id
                           :Size
                           :Created
                           :RepoTags
                           :Labels}
      :table-props       {:stackable true}
      :wrapper-div-class nil
      :cell-props        {:header {:single-line true}}
      :rows              images-ordered}
     ::table-cols-edge-detail-coe-resource-docker-images]))

(defn DockerVolumesTable []
  (let [volumes-ordered @(subscribe [::subs/docker-volumes-ordered])]
    [table-plugin/TableColsEditable
     {:columns           [{:field-key      :Name
                           :header-content "Name"}
                          {:field-key      :Driver
                           :header-content "Driver"}
                          {:field-key      :Scope
                           :header-content "Scope"}
                          {:field-key      :Mountpoint
                           :header-content "Mount point"}
                          field-labels]
      :sort-config       {:db-path ::spec/docker-volumes-ordering}
      :default-columns   #{:Name :Driver :Mountpoint :Labels}
      :table-props       {:stackable true}
      :wrapper-div-class nil
      :cell-props        {:header {:single-line true}}
      :rows              volumes-ordered}
     ::table-cols-edge-detail-coe-resource-docker-volumes]))

(defn- DockerContainersTable []
  (let [containers-ordered @(subscribe [::subs/docker-containers-ordered])]
    [table-plugin/TableColsEditable
     {:columns           [field-id
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
                           :cell           CellBytes}]
      :sort-config       {:db-path ::spec/docker-volumes-ordering}
      :default-columns   #{:Id :Image :SizeRootFs :Created :Labels :Status}
      :table-props       {:stackable true}
      :wrapper-div-class nil
      :cell-props        {:header {:single-line true}}
      :rows              containers-ordered}
     ::table-cols-edge-detail-coe-resource-docker-containers]))

(defn Tab
  []
  [ui/Tab
   {:panes [{:menuItem "Containers", :render #(r/as-element [ui/TabPane [DockerContainersTable]])}
            {:menuItem "Images", :render #(r/as-element [ui/TabPane [DockerImagesTable]])}
            {:menuItem "Volumes", :render #(r/as-element [ui/TabPane [DockerVolumesTable]])}
            ]}])
