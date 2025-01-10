(ns sixsq.nuvla.ui.components.edge-selector-scenes
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [sixsq.nuvla.ui.pages.edges.utils :as edges-utils]
            [sixsq.nuvla.ui.portfolio-utils :refer [defscene]]
            [sixsq.nuvla.ui.common-components.plugins.edge-selector :refer [EdgeSelectorController]]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(def edges
  [{:id         "nuvlabox/0"
    :name       "nb-0"
    :state      "COMMISSIONED"
    :created-by "user a"
    :coe-list   [{:coe-type edges-utils/coe-type-docker}]}
   {:id         "nuvlabox/1"
    :name       "nb-1"
    :state      "DECOMMISSIONED"
    :created-by "user a"
    :coe-list   [{:coe-type edges-utils/coe-type-swarm}]}
   {:id         "nuvlabox/2"
    :name       "nb-2"
    :state      "COMMISSIONED"
    :created-by "user a"
    :coe-list   [{:coe-type edges-utils/coe-type-kubernetes}]}
   {:id         "nuvlabox/3"
    :name       "nb-3"
    :state      "COMMISSIONED"
    :created-by "user b"
    :coe-list   [{:coe-type edges-utils/coe-type-docker}
                 {:coe-type edges-utils/coe-type-kubernetes}]}
   {:id         "nuvlabox/4"
    :name       "nb-4"
    :state      "ACTIVATED"
    :created-by "user b"
    :coe-list   [{:coe-type edges-utils/coe-type-swarm}]}
   {:id         "nuvlabox/5"
    :name       "nb-5"
    :state      "ACTIVATED"
    :created-by "user a"
    :coe-list   [{:coe-type edges-utils/coe-type-docker}]}
   {:id         "nuvlabox/6"
    :name       "nb-6"
    :state      "COMMISSIONED"
    :created-by "user b"
    :coe-list   [{:coe-type edges-utils/coe-type-kubernetes}]}
   {:id         "nuvlabox/7"
    :name       "nb-7"
    :state      "COMMISSIONED"
    :created-by "user c"
    :coe-list   [{:coe-type edges-utils/coe-type-docker}]}])

(defn set-filters-fn
  [!edges !coe-types]
  (fn [{text-filter :text}]
    (let [filtered-edges (filterv (fn [{:keys [name description state created-by coe-list]}]
                                    (and (or (nil? text-filter)
                                             (some #(str/includes? % text-filter)
                                                   (remove nil? [name description state created-by])))
                                         (or (nil? !coe-types)
                                             (some #(contains? @!coe-types %)
                                                   (map :coe-type coe-list)))))
                                  edges)]
      (reset! !edges filtered-edges))))

(defn SelectedCount
  [!selected]
  [:div {:data-testid "edges-count"} "Number of edges selected: " (count @!selected)])

(defscene basic-selector
  (r/with-let [!edges    (r/atom edges)
               !selected (r/atom #{})]
    [:div {:style {:width 800}}
     [:div {:style {:margin 10}}
      [EdgeSelectorController {:!edges         !edges
                               :set-filters-fn (set-filters-fn !edges nil)
                               :!selected      !selected}]]
     [SelectedCount !selected]]))

(defscene pagination
  (r/with-let [!edges      (r/atom edges)
               !coe-types  (r/atom #{edges-utils/coe-type-docker
                                     edges-utils/coe-type-swarm
                                     edges-utils/coe-type-kubernetes})
               !selected   (r/atom #{})
               !pagination (r/atom {:page-index 0
                                    :page-size  4})]
    [:div {:style {:width 800}}
     [:div {:style {:margin 10}}
      [EdgeSelectorController {:!edges              !edges
                               :set-filters-fn      (set-filters-fn !edges !coe-types)
                               :!selected           !selected
                               :!enable-pagination? (r/atom true)
                               :!page-sizes         (r/atom [4 7 10 20])
                               :!pagination         !pagination}]]
     [SelectedCount !selected]]))

(defn COETypeFilterParams
  [!coe-types]
  (let [docker? (contains? @!coe-types edges-utils/coe-type-docker)
        swarm?  (contains? @!coe-types edges-utils/coe-type-swarm)
        k8s?    (contains? @!coe-types edges-utils/coe-type-kubernetes)]
    [:div {:style {:margin-bottom "20px"
                   :padding       "4px"
                   :display       :flex
                   :gap           "10px"}}
     [ui/Checkbox {:data-testid "checkbox-docker"
                   :label       "Docker"
                   :style       {:position       :relative
                                 :vertical-align :middle}
                   :checked     docker?
                   :on-click    #(swap! !coe-types (if docker? disj conj)
                                        edges-utils/coe-type-docker)}]
     [ui/Checkbox {:data-testid "checkbox-swarm"
                   :label       "Docker Swarm"
                   :style       {:position       :relative
                                 :vertical-align :middle}
                   :checked     swarm?
                   :on-click    #(swap! !coe-types (if swarm? disj conj)
                                        edges-utils/coe-type-swarm)}]
     [ui/Checkbox {:data-testid "checkbox-kubernetes"
                   :label       "Kubernetes"
                   :style       {:position       :relative
                                 :vertical-align :middle}
                   :checked     k8s?
                   :on-click    #(swap! !coe-types (if k8s? disj conj)
                                        edges-utils/coe-type-kubernetes)}]]))

(defscene filter-by-coe-type
  (r/with-let [!edges     (r/atom edges)
               !coe-types (r/atom #{edges-utils/coe-type-docker
                                    edges-utils/coe-type-swarm
                                    edges-utils/coe-type-kubernetes})
               !selected  (r/atom #{})]
    [:div {:style {:width 800}}
     [:div {:style {:margin 10}}
      [COETypeFilterParams !coe-types]
      ^{:key (str @!coe-types)}
      [EdgeSelectorController {:!edges         !edges
                               :set-filters-fn (set-filters-fn !edges !coe-types)
                               :!selected      !selected}]]
     [SelectedCount !selected]]))

