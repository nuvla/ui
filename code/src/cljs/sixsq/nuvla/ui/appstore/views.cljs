(ns sixsq.nuvla.ui.appstore.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.application.views :as application-views]
    [sixsq.nuvla.ui.application.events :as application-events]
    [sixsq.nuvla.ui.appstore.events :as events]
    [sixsq.nuvla.ui.appstore.subs :as subs]
    [sixsq.nuvla.ui.deployment-detail.utils :as deployment-detail-utils]
    [sixsq.nuvla.ui.deployment-dialog.views :as deployment-dialog-views]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [reagent.core :as reagent]))


(defn refresh-button
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn []
      [uix/MenuItemWithIcon
       {:name      (@tr [:refresh])
        :icon-name "refresh"
        :on-click  #(dispatch [::events/get-modules])}])))


(defn format-deployment-template
  [{:keys [id name description module] :as deployment-template}]
  (let [tr (subscribe [::i18n-subs/tr])
        {:keys [type parentPath logo-url]} module]
    ^{:key id}
    [ui/Card
     (when logo-url
       [ui/Image {:src   logo-url
                  :style {:width      "auto"
                          :height     "100px"
                          :object-fit "contain"}}])
     [ui/CardContent
      [ui/CardHeader {:style {:word-wrap "break-word"}}
       [ui/Icon {:name (deployment-detail-utils/category-icon type)}]
       (or name id)]
      [ui/CardMeta {:style {:word-wrap "break-word"}} parentPath]
      [ui/CardDescription {:style {:overflow "hidden" :max-height "100px"}} description]]
     [ui/Button {:fluid    true
                 :primary  true
                 :on-click #(dispatch [::events/create-deployment id "credentials"])}
      (@tr [:launch])]]))


(defn modules-cards-group
  [modules-list]
  [ui/Segment style/basic
   (vec (concat [ui/CardGroup]
                (map (fn [deployment-template]
                       [format-deployment-template deployment-template])
                     modules-list)))])


(defn toggle [v]
  (swap! v not))


(defn appstore-search []
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Input {:placeholder (@tr [:search])
               :icon        "search"
               :on-change   (ui-callback/input-callback #(dispatch [::events/set-full-text-search %]))}]))


(defn control-bar []
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Menu {:secondary true}
     [ui/MenuMenu {:position "left"}
      [appstore-search]]
     [ui/MenuMenu {:position "right"}
      [refresh-button]]]))


(defn appstore
  []
  (let [modules           (subscribe [::subs/modules])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])
        active? (reagent/atom true)]
    (fn []
      (let [total-pages (general-utils/total-pages (get @modules :count 0) @elements-per-page)]
        [ui/Container {:fluid true}
         [deployment-dialog-views/deploy-modal false]
         [ui/Accordion {:fluid     true
                        :styled    true
                        :exclusive false
                        }
          [ui/AccordionTitle {:active   @active?
                              :index    1
                              :on-click #(toggle active?)}
           [:h2
            [ui/Icon {:name (if @active? "dropdown" "caret right")}]
            "App Store"]]
          [ui/AccordionContent {:active @active?}
           [control-bar]
           [modules-cards-group (get @modules :resources [])]
           (when (> total-pages 1)
             [uix/Pagination
              {:totalPages   total-pages
               :activePage   @page
               :onPageChange (ui-callback/callback :activePage #(dispatch [::events/set-page %]))}])]]]))))

(defn apps
  []
  [ui/Container {:fluid true}
   [:div {:style {:margin-top 10}}]
   [appstore]
   [:div {:style {:margin-top 10}}]
   [application-views/module-resource]])

(defmethod panel/render :appstore
  [path]
  (dispatch [::events/get-modules])
  (dispatch [::application-events/get-module])
  [apps])
