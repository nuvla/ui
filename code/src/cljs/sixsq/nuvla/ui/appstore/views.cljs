(ns sixsq.nuvla.ui.appstore.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
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
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn refresh-button
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn []
      [uix/MenuItemWithIcon
       {:name      (@tr [:refresh])
        :icon-name "refresh"
        :on-click  #(dispatch [::events/get-deployment-templates])}])))


(defn control-bar []
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Menu {:attached "top", :borderless true}
     [ui/MenuItem
      [ui/Input {:placeholder (@tr [:search])
                 :icon        "search"
                 :on-change   (ui-callback/input-callback #(dispatch [::events/set-full-text-search %]))}]]
     [ui/MenuMenu {:position "right"}
      [refresh-button]]]))


(defn format-deployment-template
  [{:keys [id name description module] :as deployment-template}]
  (let [tr (subscribe [::i18n-subs/tr])
        {:keys [type parentPath logoURL]} module]
    ^{:key id}
    [ui/Card
     (when logoURL
       [ui/Image {:src   logoURL
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


(defn deployment-templates-cards-group
  [deployment-templates-list]
  [ui/Segment style/basic
   (vec (concat [ui/CardGroup]
                (map (fn [deployment-template]
                       [format-deployment-template deployment-template])
                     deployment-templates-list)))])


(defn deployment-template-resources
  []
  (let [deployment-templates (subscribe [::subs/deployment-templates])
        elements-per-page (subscribe [::subs/elements-per-page])
        page (subscribe [::subs/page])]
    (fn []
      (let [total-pages (general-utils/total-pages (get @deployment-templates :count 0) @elements-per-page)]
        [ui/Container {:fluid true}
         [control-bar]
         [deployment-dialog-views/deploy-modal false]
         [deployment-templates-cards-group (get @deployment-templates :resources [])]
         (when (> total-pages 1)
           [uix/Pagination
            {:totalPages   total-pages
             :activePage   @page
             :onPageChange (ui-callback/callback :activePage #(dispatch [::events/set-page %]))}])]))))


(defmethod panel/render :appstore
  [path]
  (dispatch [::events/get-deployment-templates])
  [deployment-template-resources])
