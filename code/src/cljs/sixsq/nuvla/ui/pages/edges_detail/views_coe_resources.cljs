(ns sixsq.nuvla.ui.pages.edges-detail.views-coe-resources
  (:require [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.plugins.nav-tab :as tab-plugin]
            [sixsq.nuvla.ui.pages.data.utils :as data-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.common-components.plugins.table :as table-plugin]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.pages.edges-detail.spec :as spec]
            [reagent.core :as r]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.pages.edges-detail.subs :as subs]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]))


(defn- DockerImagesTable []
  (let [images     (subscribe [::subs/docker-images])
        cell-bytes (fn [{cell-data :cell-data}]
                     (data-utils/format-bytes cell-data))]

    [table-plugin/TableColsEditable
     {:columns           [
                          {:field-key      :Id
                           :header-content "Id"}
                          {:field-key      :Size
                           :header-content "Size"
                           :cell           cell-bytes}
                          {:field-key      :Created
                           :header-content "Created"
                           :cell           (fn [{{:keys [Created]} :row-data}]
                                             [uix/TimeAgo (some-> Created
                                                                  time/parse-unix
                                                                  time/time->utc-str)])}
                          {:field-key      :RepoTags
                           :header-content "Tags"
                           :cell           (fn [{:keys [cell-data row-data]}]
                                             [ui/LabelGroup {:color "blue"}
                                              (for [tag cell-data]
                                                ^{:key (str (:Id row-data) tag)}
                                                [ui/Label tag])])}
                          {:field-key      :Labels
                           :header-content "Labels"
                           :cell           (fn [{:keys [cell-data row-data]}]
                                             (r/with-let [max-n 4
                                                          show-more? (r/atom false)]
                                               [:div
                                                [ui/LabelGroup
                                                 (for [[label-k label-v] (cond->> cell-data
                                                                                  (not @show-more?) (take max-n))]
                                                   ^{:key (str (:Id row-data) label-k)}
                                                   [ui/Label {:content label-k :detail label-v}])]
                                                (when (> (count cell-data) max-n)
                                                  [ui/Button {:basic    true
                                                              :on-click #(swap! show-more? not)
                                                              :size     :mini} (if @show-more? "Show less" "Show more")])]))}
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
      :default-columns   #{
                           :Id
                           :RepoTags
                           :Size :Created
                           }
      :table-props       {:stackable true}
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