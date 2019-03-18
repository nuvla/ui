(ns sixsq.nuvla.ui.module-component.views
  (:require
    [reagent.core :as reagent]
    [cljs.pprint :refer [cl-format]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.cimi-api.utils :as cimi-api-utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.module-component.events :as events]
    [sixsq.nuvla.ui.module-component.subs :as subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.plot.plot :as plot]
    [sixsq.nuvla.ui.utils.collapsible-card :as cc]
    [sixsq.nuvla.ui.utils.resource-details :as details]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.form-fields :as forms]
    [sixsq.nuvla.ui.panel :as panel]
    [taoensso.timbre :as log]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.deployment-dialog.events :as deployment-dialog-events]
    [sixsq.nuvla.ui.application.subs :as application-subs]
    [sixsq.nuvla.ui.application.utils :as application-utils]
    [sixsq.nuvla.ui.application.views :as application-views]
    [sixsq.nuvla.ui.application.events :as application-events]))

(defn refresh-button
  []
  (let [tr            (subscribe [::i18n-subs/tr])
        page-changed? (subscribe [::application-subs/page-changed?])]
    (fn []
      [ui/MenuMenu {:position "right"}
       [uix/MenuItemWithIcon
        {:name      (@tr [:refresh])
         :icon-name "refresh"
         :loading?  false                                   ;; FIXME: Add loading flag for module.
         :disabled  @page-changed?
         :on-click  #(do (dispatch [::application-events/page-changed? false])
                         (dispatch [::application-events/get-module]))
         }]])))


(defn control-bar []
  (let [tr            (subscribe [::i18n-subs/tr])
        page-changed? (subscribe [::application-subs/page-changed?])
        module        (subscribe [::application-subs/module])]
    (fn []
      [ui/Menu {:borderless true}
       [uix/MenuItemWithIcon
        {:name      (@tr [:launch])
         :icon-name "rocket"
         :disabled  (if @page-changed? true false)
         :on-click  #(dispatch [::deployment-dialog-events/create-deployment (:id @module) :credentials])
         }]
       [uix/MenuItemWithIcon
        {:name      (@tr [:save])
         :icon-name "disk"
         :disabled  (not @page-changed?)
         :on-click  #(dispatch [::application-events/open-save-modal])}]
       [refresh-button]])))


(defn summary []
  (let [tr     (subscribe [::i18n-subs/tr])
        module (subscribe [::application-subs/module])]
    (fn []
      (let [content (:content @module)
            {docker-image :image
                          :or {docker-image ""}} content]
        [application-views/summary
         [:div
          [ui/Input {:name        "docker-image"
                     :label       "docker image"
                     :value       docker-image
                     :placeholder (@tr [:module-docker-image-placeholder])
                     :fluid       true
                     :style       {:padding-bottom 5}
                     :on-change   (ui-callback/input-callback #(do (dispatch [::application-events/page-changed? true])
                                                                   (dispatch [::application-events/docker-image %])))
                     }]]]))))



(defn toggle [v]
  (swap! v not))

(defn single-port-mapping [id mapping]
  (let [{source      :source
         destination :destination
         port-type   :port-type :or {source "" destination "" port-type "TCP"}} mapping]
    [ui/GridRow {:key id}
     [ui/GridColumn {:floated :left
                     :width   11}
      [ui/Input {:name        (str "source-" id)
                 :placeholder "source - e.g. 22 or 22-23"
                 :value       source
                 :on-change   (ui-callback/input-callback #(do (dispatch [::application-events/page-changed? true])
                                                               (dispatch [::events/update-mapping-source id %])))}]
      [:span " : "]
      [ui/Input {:name        (str "destination-" id)
                 :placeholder "dest. - e.g. 22 or 22-23"
                 :value       destination
                 :on-change   (ui-callback/input-callback #(do (dispatch [::application-events/page-changed? true])
                                                               (dispatch [::events/update-mapping-destination id %])))}]
      [:span " / "]
      [ui/Label
       [ui/Dropdown {:name    (str "port-type-" id)
                     :inline  true
                     :value   port-type
                     :options [{:key "TCP", :value "TCP", :text "TCP"}
                               {:key "UDP", :value "UDP", :text "UDP"}]
                     ;:on-change (events/dropdown ::events/toto 123)
                     }]]]
     [ui/GridColumn {:floated :right
                     :align   :right
                     :style   {}}
      [ui/Icon {:name     "trash"
                :on-click #(do (dispatch [::application-events/page-changed? true])
                               (dispatch [::events/remove-port-mapping id]))
                :color    :red}]]]))

(defn port-mappings-section []
  (let [tr       (subscribe [::i18n-subs/tr])
        active?  (reagent/atom true)
        mappings (subscribe [::subs/port-mappings])]
    (fn []
      [ui/Accordion {:fluid     true
                     :styled    true
                     :exclusive false}
       [ui/AccordionTitle {:active   @active?
                           :index    1
                           :on-click #(toggle active?)}
        [ui/Icon {:name (if @active? "dropdown" "caret right")}]
        "Port Mappings"]

       [ui/AccordionContent {:active @active?}
        [:div "Publish ports "
         [:span forms/nbsp (forms/help-popup (@tr [:module-port-mapping-help]))]]
        [:div [ui/Grid {:style {:margin-top    5
                                :margin-bottom 5}}
               (for [[id mapping] @mappings]
                 ^{:key id}
                 [single-port-mapping id mapping])]]
        [:div
         [ui/Icon {:name     "plus circle"
                   :on-click #(do (dispatch [::application-events/page-changed? true])
                                  (dispatch [::events/add-port-mapping (random-uuid) {}]))}]]]])))


(defn single-volume [id volume]
  (let [tr (subscribe [::i18n-subs/tr])
        {type        :type
         source      :source
         destination :destination
         driver      :driver
         read-only?  :read-only? :or {type        "volume"
                                      source      ""
                                      destination ""
                                      driver      "local"
                                      read-only?  false}} volume]
    [ui/GridRow {:key id}
     [ui/GridColumn {:floated :left
                     :width   15}
      [ui/Label
       [ui/Dropdown {:name           (str "type-" id)
                     ;:inline  true
                     :default-value  "volume"
                     ;:value          (or type "volume")
                     :allowAdditions true
                     :selection      true
                     :additionLabel  "Custom type: "
                     :search         true
                     :options        [{:key "volume", :value "volume", :text "volume"}
                                      {:key "bind", :value "bind", :text "bind"}
                                      {:key "tmpfs", :value "tmpfs", :text "tmpfs"}]
                     ;:on-change (events/dropdown ::events/toto 123)
                     }]]
      [:span " , "]
      [ui/Input {:name        (str "source-" id)
                 :placeholder "source"
                 :value       source
                 :on-change   (ui-callback/input-callback #(do (dispatch [::application-events/page-changed? true])
                                                               (dispatch [::events/update-volume-source id %])))}]
      [ui/Input {:name        (str "destination-" id)
                 :placeholder "destination"
                 :value       destination
                 :on-change   (ui-callback/input-callback #(do (dispatch [::application-events/page-changed? true])
                                                               (dispatch [::events/update-volume-destination id %])))}]
      [ui/Input {:name        (str "driver-" id)
                 :placeholder "driver"
                 :value       driver
                 :on-change   (ui-callback/input-callback #(do (dispatch [::application-events/page-changed? true])
                                                               (dispatch [::events/update-volume-driver id %])))}]
      [:span " " (@tr [:module-volume-read-only?]) " "
       [ui/Checkbox {:name    "read-only"
                     :checked (if (nil? read-only?) false read-only?)
                     ;:on-click #(dispatch [::events/remove-volume id]) TODO!!
                     :align   :middle}]
       ]]
     [ui/GridColumn {:floated :right
                     :width   1
                     :align   :right
                     :style   {}}
      [ui/Icon {:name     "trash"
                :on-click #(do (dispatch [::application-events/page-changed? true])
                               (dispatch [::events/remove-volume id]))
                :color    :red}]]]))


(defn volumes-section []
  (let [tr      (subscribe [::i18n-subs/tr])
        active? (reagent/atom true)
        volumes (subscribe [::subs/volumes])]
    (fn []
      [ui/Accordion {:fluid     true
                     :styled    true
                     :exclusive false}
       [ui/AccordionTitle {:active   @active?
                           :index    1
                           :on-click #(toggle active?)}
        [ui/Icon {:name (if @active? "dropdown" "caret right")}]
        "Volumes"]

       [ui/AccordionContent {:active @active?}
        [:div "Container volumes (i.e. mounts) "
         [:span forms/nbsp (forms/help-popup (@tr [:module-volume-help]))]]
        [:div [ui/Grid {:style {:margin-top    5
                                :margin-bottom 5}}
               (for [[id volume] @volumes]
                 ^{:key id}
                 [single-volume id volume])]]
        [:div
         [ui/Icon {:name     "plus circle"
                   :on-click #(do (dispatch [::application-events/page-changed? true])
                                  (dispatch [::events/add-volume (random-uuid) {}]))}]]]])))


(defn runtime []
  (let [active? (reagent/atom true)]
    (fn []
      [ui/Accordion {:fluid     true
                     :styled    true
                     :exclusive false}
       [ui/AccordionTitle {:active   @active?
                           :index    1
                           :on-click #(toggle active?)}
        [ui/Icon {:name (if @active? "dropdown" "caret right")}]
        "Runtime"]
       [ui/AccordionContent {:active @active?}
        [ui/Segment
         [:div "Command " [ui/Icon {:name "question circle"}]]
         [:div.ui.middle.aligned.divided.list
          [:div.item
           [ui/Input {:name        "runtime-command"
                      :placeholder "e.g. echo \"hello world\""
                      :fluid       true}]]]]
        [ui/Segment
         [:div "Environment variables " [ui/Icon {:name "question circle"}]]
         [:div.ui.middle.aligned.divided.list
          [:div.item
           [ui/Input {:name        "envar-name"
                      :placeholder "e.g. VAR"}]
           [:span " = "]
           [ui/Input {:name        "envar-value"
                      :placeholder "e.g. VALUE"}]
           [:div.right.floated.content
            [ui/Icon {:name "trash"}]]]
          [ui/Icon {:name "plus circle"}]]]
        [ui/Segment
         [:div "Secrets " [ui/Icon {:name "question circle"}]]
         [:div.ui.middle.aligned.divided.list
          [:div.item
           [ui/Input {:name        "secret-name"
                      :placeholder "Secret name"}]
           [:span " = "]
           [ui/Input {:name        "secret-value"
                      :placeholder "Secret value"
                      :type        :password
                      :icon        :eye}]
           [ui/Popup {:trigger  (reagent/as-element [ui/CopyToClipboard {:text "ssh-password"}
                                                     [:a [ui/Icon {:name "clipboard outline"}]]])
                      :position "top center"}
            "copy to clipboard"]
           [:div.right.floated.content
            [ui/Icon {:name "trash"}]]]
          [ui/Icon {:name "plus circle"}]]]]])))


(defn view-edit
  []
  (let [module (subscribe [::application-subs/module])]
    (fn []
      (let [new-parent (application-utils/nav-path->parent-path @(subscribe [::main-subs/nav-path]))
            new-name   (application-utils/nav-path->module-name @(subscribe [::main-subs/nav-path]))
            name       (:name @module)
            parent     (:parent-path @module)]
        (when (nil? @module)
          (dispatch [::application-events/name new-name])
          (dispatch [::application-events/parent new-parent]))
        [ui/Container {:fluid true}
         [:h2 [ui/Icon {:name "th"}]
          parent (when (not-empty parent) "/") name]
         [application-views/control-bar]
         [summary]
         [port-mappings-section]
         [:div {:style {:padding-top 10}}]
         [volumes-section]
         [application-views/save-action]
         [application-views/add-modal]
         [application-views/save-modal]
         [application-views/logo-url-modal]
         ]))))
