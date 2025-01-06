(ns sixsq.nuvla.ui.components.module-selector-scenes
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [sixsq.nuvla.ui.pages.apps.apps-applications-sets.spec :as apps-sets-spec]
            [sixsq.nuvla.ui.pages.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.portfolio-utils :refer [defscene]]
            [sixsq.nuvla.ui.common-components.plugins.module-selector-refactor :refer [ModuleSelectorController]]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(def modules
  [{:id          "application/0"
    :name        "app 0"
    :category    :app-store
    :subtype     apps-utils/subtype-application
    :description "This is application 0"}
   {:id          "application/1"
    :name        "app 1"
    :category    :my-apps
    :subtype     apps-utils/subtype-application
    :description "This is application 1"}
   {:id          "application/2"
    :name        "app 2"
    :category    :my-apps
    :subtype     apps-utils/subtype-application-k8s
    :description "This is application 2"}
   {:id          "application/3"
    :name        "app 3"
    :category    :my-apps
    :subtype     apps-utils/subtype-application-helm
    :description "This is application 3"}
   {:id          "application/4"
    :name        "app 4"
    :category    :app-store
    :subtype     apps-utils/subtype-application-k8s
    :description "This is application 4"}])

(defn set-filters-fn
  [!modules !subtypes]
  (fn [{category-filter :category text-filter :text}]
    (let [filtered-modules (filterv (fn [{:keys [name category subtype description]}]
                                      (and (or (nil? category-filter)
                                               (= :all-apps category-filter)
                                               (= category-filter category))
                                           (or (nil? text-filter)
                                               (or (str/includes? name text-filter)
                                                   (str/includes? description text-filter)))
                                           (or (nil? !subtypes)
                                               (contains? @!subtypes subtype))))
                                    modules)]
      (reset! !modules filtered-modules))))

(defn SelectedCount
  [!selected]
  [:div {:data-testid "modules-count"} "Number of modules selected: " (count @!selected)])

(defscene basic-selector
  (r/with-let [!modules  (r/atom modules)
               !selected (r/atom #{})]
    [:div {:style {:width 400}}
     [:div {:style {:margin 10}}
      [ModuleSelectorController {:!modules       !modules
                                 :set-filters-fn (set-filters-fn !modules nil)
                                 :!selected      !selected}]]
     [SelectedCount !selected]]))

(defscene pagination
  (r/with-let [!modules    (r/atom modules)
               !subtypes   (r/atom #{apps-utils/subtype-application
                                     apps-utils/subtype-application-k8s
                                     apps-utils/subtype-application-helm})
               !selected   (r/atom #{})
               !pagination (r/atom {:page-index 0
                                    :page-size  4})]
    [:div {:style {:width 800}}
     [:div {:style {:margin 10}}
      [ModuleSelectorController {:!modules            !modules
                                 :set-filters-fn      (set-filters-fn !modules !subtypes)
                                 :!selected           !selected
                                 :!enable-pagination? (r/atom true)
                                 :!page-sizes         (r/atom [4 7 10 20])
                                 :!pagination         !pagination}]]
     [SelectedCount !selected]]))

(defn SubtypeFilterParams
  [!subtypes]
  (let [compose? (contains? @!subtypes apps-utils/subtype-application)
        k8s?     (contains? @!subtypes apps-utils/subtype-application-k8s)
        helm?    (contains? @!subtypes apps-utils/subtype-application-helm)]
    [:div {:style {:margin-bottom "20px"
                   :padding       "4px"
                   :display       :flex
                   :gap           "10px"}}
     [ui/Checkbox {:data-testid "checkbox-docker-compose"
                   :label       "Docker Compose"
                   :style       {:position       :relative
                                 :vertical-align :middle}
                   :checked     compose?
                   :on-click    #(swap! !subtypes (if compose? disj conj)
                                        apps-utils/subtype-application)}]
     [ui/Checkbox {:data-testid "checkbox-kubernetes"
                   :label       "Kubernetes"
                   :style       {:position       :relative
                                 :vertical-align :middle}
                   :checked     k8s?
                   :on-click    #(swap! !subtypes (if k8s? disj conj)
                                        apps-utils/subtype-application-k8s)}]
     [ui/Checkbox {:data-testid "checkbox-helm"
                   :label       "Helm"
                   :style       {:position       :relative
                                 :vertical-align :middle}
                   :checked     helm?
                   :on-click    #(swap! !subtypes (if helm? disj conj)
                                        apps-utils/subtype-application-helm)}]]))

(defscene filter-by-subtype
  (r/with-let [!modules    (r/atom modules)
               !subtypes   (r/atom #{apps-utils/subtype-application
                                     apps-utils/subtype-application-k8s
                                     apps-utils/subtype-application-helm})
               !selected   (r/atom #{})]
    [:div {:style {:width 800}}
     [:div {:style {:margin 10}}
      [SubtypeFilterParams !subtypes]
      ^{:key (str @!subtypes)}
      [ModuleSelectorController {:!modules            !modules
                                 :set-filters-fn      (set-filters-fn !modules !subtypes)
                                 :!selected           !selected}]]
     [SelectedCount !selected]]))

