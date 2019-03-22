(ns sixsq.nuvla.ui.apps-component.views
  (:require
    [cljs.pprint :refer [cl-format]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as reagent]
    [sixsq.nuvla.ui.apps-component.events :as events]
    [sixsq.nuvla.ui.apps-component.subs :as subs]
    [sixsq.nuvla.ui.apps.events :as apps-events]
    [sixsq.nuvla.ui.apps.subs :as apps-subs]
    [sixsq.nuvla.ui.apps.utils :as apps-utils]
    [sixsq.nuvla.ui.apps.views-detail :as apps-views-detail]
    [sixsq.nuvla.ui.deployment-dialog.views :as deployment-dialog-views]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.utils.form-fields :as forms]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [taoensso.timbre :as log]))

(defn refresh-button
  []
  (let [tr            (subscribe [::i18n-subs/tr])
        page-changed? (subscribe [::apps-subs/page-changed?])]
    (fn []
      [ui/MenuMenu {:position "right"}
       [uix/MenuItemWithIcon
        {:name      (@tr [:refresh])
         :icon-name "refresh"
         :loading?  false                                   ;; FIXME: Add loading flag for module.
         :disabled  @page-changed?
         :on-click  #(do (dispatch [::apps-events/page-changed? false])
                         (dispatch [::apps-events/get-module]))
         }]])))


(defn summary []
  (let [tr     (subscribe [::i18n-subs/tr])
        module (subscribe [::apps-subs/module])]
    (fn []
      (let [content (:content @module)
            {docker-image :image
                          :or {docker-image ""}} content]
        [apps-views-detail/summary
         [:div
          [ui/Input {:name        "docker-image"
                     :label       "docker image"
                     :value       docker-image
                     :placeholder (@tr [:module-docker-image-placeholder])
                     :fluid       true
                     :style       {:padding-bottom 5}
                     :on-change   (ui-callback/input-callback #(do (dispatch [::apps-events/page-changed? true])
                                                                   (dispatch [::apps-events/docker-image %])))
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
                 :on-change   (ui-callback/input-callback #(do (dispatch [::apps-events/page-changed? true])
                                                               (dispatch [::events/update-mapping-source id %])))}]
      [:span " : "]
      [ui/Input {:name        (str "destination-" id)
                 :placeholder "dest. - e.g. 22 or 22-23"
                 :value       destination
                 :on-change   (ui-callback/input-callback #(do (dispatch [::apps-events/page-changed? true])
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
                :on-click #(do (dispatch [::apps-events/page-changed? true])
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
                   :on-click #(do (dispatch [::apps-events/page-changed? true])
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
                 :on-change   (ui-callback/input-callback #(do (dispatch [::apps-events/page-changed? true])
                                                               (dispatch [::events/update-volume-source id %])))}]
      [ui/Input {:name        (str "destination-" id)
                 :placeholder "destination"
                 :value       destination
                 :on-change   (ui-callback/input-callback #(do (dispatch [::apps-events/page-changed? true])
                                                               (dispatch [::events/update-volume-destination id %])))}]
      [ui/Input {:name        (str "driver-" id)
                 :placeholder "driver"
                 :value       driver
                 :on-change   (ui-callback/input-callback #(do (dispatch [::apps-events/page-changed? true])
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
                :on-click #(do (dispatch [::apps-events/page-changed? true])
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
                   :on-click #(do (dispatch [::apps-events/page-changed? true])
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
  (let [module (subscribe [::apps-subs/module])]
    (fn []
      (let [name       (:name @module)
            parent     (:parent-path @module)]
        (when (empty? @module)
          (let [new-parent (apps-utils/nav-path->parent-path @(subscribe [::main-subs/nav-path]))
                new-name   (apps-utils/nav-path->module-name @(subscribe [::main-subs/nav-path]))]
            (dispatch [::apps-events/name new-name])
            (dispatch [::apps-events/parent new-parent])))
        [ui/Container {:fluid true}
         [:h2 [ui/Icon {:name "th"}]
          parent (when (not-empty parent) "/") name]
         [apps-views-detail/control-bar]
         [summary]
         [port-mappings-section]
         [:div {:style {:padding-top 10}}]
         [volumes-section]
         [apps-views-detail/save-action]
         [apps-views-detail/add-modal]
         [apps-views-detail/save-modal]
         [apps-views-detail/logo-url-modal]
         [deployment-dialog-views/deploy-modal]
         ]))))
