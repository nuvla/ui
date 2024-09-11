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
  (let [images     (subscribe [::subs/docker-images])
        cell-bytes (fn [{cell-data :cell-data}]
                     (data-utils/format-bytes cell-data))]
    [table-plugin/TableColsEditable
     {:columns           [{:field-key      :Id
                           :header-content "Id"}
                          {:field-key      :ParentId
                           :header-content "Parent Id"}
                          {:field-key      :RepoDigests
                           :header-content "Repo Digests"
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
                           :cell           (partial LabelGroupOverflow DockerTagGroup)}
                          {:field-key      :Labels
                           :header-content "Labels"
                           :cell           (partial LabelGroupOverflow DockerLabelGroup)}
                          ;{:field-key      :cpu-usage
                          ; :header-content "CPU %"
                          ; :cell           (fn [{value :cell-data}]
                          ;                   (if value
                          ;                     (str (general-utils/to-fixed value) " %")
                          ;                     "-"))}
                          ;{:field-key      :mem-usage
                          ; :header-content "Mem Usage"
                          ; :cell           cell-bytes}
                          ;{:field-key      :mem-limit
                          ; :header-content "Mem Limit"
                          ; :cell           cell-bytes}
                          ;{:field-key      :mem-usage-perc
                          ; :header-content "Mem Usage %"
                          ; :cell           (fn [{{:keys [mem-usage mem-limit]} :row-data}]
                          ;                   [BytesUsage mem-usage mem-limit])
                          ; :cell-props     {:style {:text-align "right"}}
                          ; :sort-value-fn  (fn [{:keys [mem-usage mem-limit]}]
                          ;                   (when (and (number? mem-usage) (number? mem-limit) (not (zero? mem-limit)))
                          ;                     (/ (double mem-usage) mem-limit)))}
                          ;{:field-key      :status
                          ; :header-content "Status"}
                          ;{:field-key      :restart-count
                          ; :header-content "Restart Count"}
                          ;{:field-key      :disk-in
                          ; :header-content "Disk In"
                          ; :cell           cell-bytes}
                          ;{:field-key      :disk-out
                          ; :header-content "Disk Out"
                          ; :cell           cell-bytes}
                          ;{:field-key      :net-in
                          ; :header-content "Network In"
                          ; :cell           cell-bytes}
                          ;{:field-key      :net-out
                          ; :header-content "Network Out"
                          ; :cell           cell-bytes}
                          ;{:field-key      :created-at
                          ; :header-content "Created"
                          ; :cell           (fn [{{:keys [created-at]} :row-data}]
                          ;                   [uix/TimeAgo created-at])}
                          ;{:field-key      :started-at
                          ; :header-content "Started"
                          ; :cell           (fn [{{:keys [started-at]} :row-data}]
                          ;                   [uix/TimeAgo started-at])}
                          ;{:field-key      :cpu-capacity
                          ; :header-content "CPU capacity"}
                          ]
      ;:sort-config     {:db-path ::spec/stats-container-ordering}
      :default-columns   #{:Id
                           :RepoTags
                           :Size
                           :Created}
      :table-props       {:stackable true}
      ;:table-props       (merge style/single-line {:stackable true})
      :wrapper-div-class nil
      :cell-props        {:header {:single-line true}}
      :rows              @images}
     ::table-cols-edge-detail-coe-resource-docker-images]))

(defn ImagesPane
  []
  (r/with-let [images (subscribe [::subs/docker-images])]
    [ui/TabPane
     [DockerImagesTable]
     "Images 1 content"
     (str @images)
     ]))

(defn VolumesPane
  []
  [ui/TabPane
   "Volumes 1 content"
   ])

(defn Tab
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn []
      (let []
        [ui/Tab
         {:panes [{:menuItem "Images", :render #(r/as-element [ImagesPane])}
                  {:menuItem "Volumes", :render #(r/as-element [VolumesPane])}]}]))))