(ns sixsq.nuvla.ui.apps-project.views
  (:require
    [cljs.pprint :refer [cl-format]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch dispatch-sync subscribe]]
    [reagent.core :as reagent]
    [sixsq.nuvla.ui.apps-project.spec :as spec]
    [sixsq.nuvla.ui.apps.events :as apps-events]
    [sixsq.nuvla.ui.apps.spec :as apps-spec]
    [sixsq.nuvla.ui.apps.subs :as apps-subs]
    [sixsq.nuvla.ui.apps.utils :as apps-utils]
    [sixsq.nuvla.ui.apps.views-detail :as apps-views-detail]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.history.utils :as history-utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.style :as style]
    [taoensso.timbre :as log]))

(defn summary []
  [apps-views-detail/summary])


(defn format-module
  [{:keys [type name path description] :as module}]
  (when module
    (let [on-click #(dispatch [::history-events/navigate (str "apps/" path)])
          icon-name  (apps-utils/category-icon type)]
      [ui/ListItem {:on-click on-click}
       [ui/ListIcon {:name           icon-name
                     :size           "large"
                     :vertical-align "middle"}]
       [ui/ListContent
        [ui/ListHeader [:a {:on-click on-click} name]]
        [ui/ListDescription [:span description]]]])))


(defn format-module-children
  [module-children]
  (when (pos? (count module-children))
    [ui/Segment style/basic
     (vec (concat [ui/ListSA {:divided   true
                              :relaxed   true
                              :selection true}]
                  (map (fn [{:keys [id] :as module}]
                         ^{:key id}
                         [format-module module]) module-children)))]))


(defn toggle [v]
  (swap! v not))


(defn modules-view []
  (let [tr      (subscribe [::i18n-subs/tr])
        module  (subscribe [::apps-subs/module])
        active? (reagent/atom true)]
    (fn []
      (let [children (:children @module)]
        (if (empty? children)
          [ui/Message {:warning true}
           [ui/Icon {:name "warning sign"}]
           (@tr [:no-children-modules])]

          [ui/Accordion {:fluid     true
                         :styled    true
                         :exclusive false
                         }
           [ui/AccordionTitle {:active   @active?
                               :index    1
                               :on-click #(toggle active?)}
            [:h2
             [ui/Icon {:name (if @active? "dropdown" "caret right")}]
             "Sub-modules"]]
           [ui/AccordionContent {:active @active?}
            [format-module-children children]]])))))


(defn
  clear-module
  [])


(defn view-edit
  []
  (let [module-common (subscribe [::apps-subs/module-common])]
    (fn []
      (let [name   (get @module-common ::apps-spec/name)
            parent (get @module-common ::apps-spec/parent-path)]
        (dispatch [::apps-events/set-form-spec ::spec/module-project])
        (dispatch [::apps-events/set-module-type :project])
        [ui/Container {:fluid true}
         [:h2 [ui/Icon {:name "folder"}]
          parent (when (not-empty parent) "/") name]
         [apps-views-detail/control-bar]
         [summary]
         [apps-views-detail/save-action]
         [:div {:style {:padding-top 10}}]
         [modules-view]
         [apps-views-detail/save-modal]
         [apps-views-detail/logo-url-modal]]))))
