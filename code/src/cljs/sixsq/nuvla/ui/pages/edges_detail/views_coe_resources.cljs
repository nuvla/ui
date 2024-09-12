(ns sixsq.nuvla.ui.pages.edges-detail.views-coe-resources
  (:require [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.pages.data.utils :as data-utils]
            [sixsq.nuvla.ui.common-components.plugins.table :as table-plugin]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [reagent.core :as r]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.pages.edges-detail.subs :as subs]
            [sixsq.nuvla.ui.pages.edges-detail.spec :as spec]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))

(defn DockerLabelGroup
  [{:keys [cell-data row-data]}]
  [ui/LabelGroup
   (for [[label-k label-v] cell-data]
     ^{:key (str (:Id row-data) label-k)}
     [ui/Label {:content label-k :detail label-v}])])

(defn DockerTagGroup
  [{:keys [cell-data row-data]}]
  [ui/LabelGroup {:color :blue}
   (for [tag cell-data]
     ^{:key (str (:Id row-data) tag)}
     [ui/Label {:content tag}])])

(defn DockerRepoDigestsGroup
  [{:keys [cell-data row-data]}]
  [ui/LabelGroup
   (for [digest cell-data]
     ^{:key (str (:Id row-data) digest)}
     [ui/Label {:content digest}])])

(defn LabelGroupOverflow
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

(defn- DockerImagesTable []
  (let [images-ordered @(subscribe [::subs/docker-images-ordered])
        cell-bytes     (fn [{cell-data :cell-data}]
                         (data-utils/format-bytes cell-data))]
    [table-plugin/TableColsEditable
     {:columns           [{:field-key      :Id
                           :header-content "Id"
                           :no-sort?       true}
                          {:field-key      :ParentId
                           :header-content "Parent Id"
                           :no-sort?       true}
                          {:field-key      :RepoDigests
                           :header-content "Repo Digests"
                           :no-sort?       true
                           :cell           (partial LabelGroupOverflow DockerRepoDigestsGroup)}
                          {:field-key      :Size
                           :header-content "Size"
                           :cell           cell-bytes}
                          {:field-key      :Created
                           :header-content "Created"
                           :cell           (fn [{:keys [cell-data]}]
                                             [uix/TimeAgo (some-> cell-data
                                                                  time/parse-unix
                                                                  time/time->utc-str)])}
                          {:field-key      :RepoTags
                           :header-content "Tags"
                           :no-sort?       true
                           :cell           (partial LabelGroupOverflow DockerTagGroup)}
                          {:field-key      :Labels
                           :header-content "Labels"
                           :no-sort?       true
                           :cell           (partial LabelGroupOverflow DockerLabelGroup)}]
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

(defn ImagesPane
  []
  [ui/TabPane
   [DockerImagesTable]])

(defn VolumesPane
  []
  [ui/TabPane
   "Volumes 1 content"
   ])

(defn Tab
  []
  [ui/Tab
   {:panes [{:menuItem "Images", :render #(r/as-element [ImagesPane])}
            {:menuItem "Volumes", :render #(r/as-element [VolumesPane])}]}])