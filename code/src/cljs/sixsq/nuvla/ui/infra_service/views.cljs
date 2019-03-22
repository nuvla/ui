(ns sixsq.nuvla.ui.infra-service.views
  (:require
    [reagent.core :as reagent]
    [cljs.pprint :refer [cl-format]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.main.events :as main-events]
    [taoensso.timbre :as log]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.infra-service.events :as events]
    [sixsq.nuvla.ui.infra-service.subs :as subs]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.infra-service.events :as events]
    [taoensso.timbre :as timbre]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))


(defn refresh-button
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn []
      [uix/MenuItemWithIcon
       {:name      (@tr [:refresh])
        :icon-name "refresh"
        :position  "right"
        :on-click  #(dispatch [::events/get-services])}])))


(defn toggle [v]
  (swap! v not))


(defn services-search []
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Input {:placeholder (@tr [:search])
               :icon        "search"
               :on-change   (ui-callback/input-callback #(dispatch [::events/set-full-text-search %]))}]))


(defn control-bar []
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Menu {:secondary true}
     [ui/MenuMenu {:position "left"}
      [services-search]]
     [ui/MenuMenu {:position "right"}
      [refresh-button]]]))


;(defn show-service-sidebar []
;  (let [show-service-sidebar? (subscribe [::subs/show-service-sidebar?])]
;    [ui/Sidebar {:as        ui/MenuRaw
;                 ;                 :className "medium thin"
;                 :vertical  true
;                 :inverted  true
;                 :direction :right
;                 :visible   true                            ;@show-service-sidebar?
;                 :animation :overlay}
;     [:nav {:aria-label "sidebar"}
;      [:div "hello"]
;      ]]))


(def service-icons
  {:swarm :docker
   :s3    :aws})


(defn service-card
  [{:keys [id name description path type logo-url] :as service}]
  (let [tr (subscribe [::i18n-subs/tr])
        {:keys [type]} service
        ]
    ^{:key id}
    [ui/Card
     (when logo-url
       [ui/Image {:src   logo-url
                  :style {:width      "auto"
                          :height     "100px"
                          :object-fit "contain"}}])
     [ui/CardContent
      [ui/CardHeader {:style {:word-wrap "break-word"}}
       [ui/Icon {:name ((keyword type) service-icons)}]
       [ui/Label {:corner   true
                  :style    {:z-index 0
                             :cursor  :pointer}
                  :on-click #(dispatch [::events/show-service-sidebar? true])}
        [ui/Icon {:name  "info circle"
                  :style {:cursor :pointer}}]               ; use content to work around bug in icon in label for cursor
        ]
       (or name id)]
      [ui/CardMeta {:style {:word-wrap "break-word"}} path]
      [ui/CardDescription {:style {:overflow "hidden" :max-height "100px"}} description]]]))


(defn modules-cards-group
  [modules-list]
  [ui/Segment style/basic
   (vec (concat [ui/CardGroup {:centered true}]
                (map (fn [module]
                       [service-card module])
                     modules-list)))])


(defn infra-services
  []
  (let [services          (subscribe [::subs/services])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])
        active?           (reagent/atom true)]
    (fn []
      (let [total-services (get @services :count 0)
            total-pages    (general-utils/total-pages total-services @elements-per-page)]
        [ui/Accordion {:fluid     true
                       :styled    true
                       :exclusive false
                       }
         [ui/AccordionTitle {:active   @active?
                             :index    1
                             :on-click #(toggle active?)}
          [:h2
           [ui/Icon {:name (if @active? "dropdown" "caret right")}]
           "Infrastructure Services"]]
         [ui/AccordionContent {:active @active?}
          [control-bar]
          [modules-cards-group (get @services :resources [])]
          (when (> total-pages 1)
            [:div {:style {:padding-bottom 30}}
             [uix/Pagination
              {:totalitems   total-services
               :totalPages   total-pages
               :activePage   @page
               :onPageChange (ui-callback/callback :activePage #(dispatch [::events/set-page %]))}]]
            )]]))))


(defmethod panel/render :infra-service
  [path]
  (timbre/set-level! :info)
  (dispatch [::events/get-services])
  [infra-services])
