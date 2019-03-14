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
    [sixsq.nuvla.ui.deployment-dialog.views :as deployment-dialog-views]))

(defn refresh-button
  []
  (let [tr            (subscribe [::i18n-subs/tr])
        page-changed? (subscribe [::subs/page-changed?])]
    (fn []
      [ui/MenuMenu {:position "right"}
       [uix/MenuItemWithIcon
        {:name      (@tr [:refresh])
         :icon-name "refresh"
         :loading?  false                                   ;; FIXME: Add loading flag for module.
         :disabled  @page-changed?
         ;:on-click  #(dispatch [::application-events/get-module])
         }]])))


(defn control-bar []
  (let [tr            (subscribe [::i18n-subs/tr])
        page-changed? (subscribe [::subs/page-changed?])]
    (fn []
      [ui/Menu {:borderless true}
       [uix/MenuItemWithIcon
        {:name      (@tr [:launch])
         :icon-name "rocket"
         :disabled  true
         ;:on-click #(dispatch [::deployment-dialog-events/create-deployment (:id @module) :credentials])
         }]
       [uix/MenuItemWithIcon
        {:name     (@tr [:save])
         ;:icon-name "add"
         :disabled (not @page-changed?)
         :on-click #(dispatch [::events/open-save-modal])}]
       [refresh-button]])))


(defn summary []
  (let [tr               (subscribe [::i18n-subs/tr])
        name             (subscribe [::subs/name])
        parent           (subscribe [::subs/parent])
        logo-url         (subscribe [::subs/logo-url])
        default-logo-url (subscribe [::subs/default-logo-url])]
    [ui/Grid {:style {:margin-bottom 5}}
     [ui/GridRow {:reversed :computer}
      [ui/GridColumn {:computer 2}
       [ui/Image {:src (or @logo-url @default-logo-url)}]
       [ui/Button {:fluid    true
                   :on-click #(dispatch [::events/open-logo-url-modal])}
        (@tr [:module-change-logo])]]
      [ui/GridColumn {:computer 14}
       ;[:div (pr-str @(subscribe [::subs/name]) @(subscribe [::subs/description]))]
       [ui/Input {:name        "name"
                  :value       @name
                  :placeholder (str/capitalize (@tr [:name]))
                  :fluid       true
                  :style       {:padding-bottom 5}
                  :on-change   (ui-callback/input-callback #(dispatch [::events/name %]))}]
       [ui/Input {:name        "parent"
                  :value       @parent
                  :placeholder (str/capitalize (@tr [:parent]))
                  :fluid       true
                  :style       {:padding-bottom 5}
                  :on-change   (ui-callback/input-callback #(dispatch [::events/parent %]))}]
       [ui/Input {:name        "description"
                  :placeholder (str/capitalize (@tr [:description]))
                  :fluid       true
                  :style       {:padding-bottom 5}
                  :on-change   (ui-callback/input-callback #(dispatch [::events/description %]))}]
       [:div
        [:div "Docker image"
         [:span forms/nbsp (forms/help-popup (@tr [:module-docker-name-help]))]]
        [ui/Input {:name        "docker-name"
                   :placeholder "e.g. ubuntu:18.11"
                   :fluid       true
                   :style       {:padding-bottom 5
                                 :padding-top    5}}]]
       ;[:div {:style {:padding-bottom 5}} " " (@tr [:module-force-pull-image?]) " "
       ; [ui/Checkbox {:name  "force-image-pull"
       ;               :align :middle}]
       ; [:span " "]
       ; [:span forms/nbsp (forms/help-popup "Force pull when...")]]
       ;[:div (@tr [:module-restart-policy]) " "
       ; [ui/Label
       ;  [ui/Dropdown {:name          "restart-policy"
       ;                :inline        true
       ;                :default-value :always
       ;                :on-change     (ui-callback/dropdown #(log/infof "Dropdown: %s" %))
       ;                :options       [{:key "Always", :value "always", :text "Always"}
       ;                                {:key "Never", :value "never", :text "Never"}]}]]
       ; [:span " "]
       ; [:span forms/nbsp (forms/help-popup (@tr [:module-restart-policy-help]))]]
       ]]]))

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
                 :value       source                        ;(or source "")
                 :on-change   (ui-callback/input-callback #(dispatch [::events/update-mapping-source id %]))}]
      [:span " : "]
      [ui/Input {:name        (str "destination-" id)
                 :placeholder "dest. - e.g. 22 or 22-23"
                 :value       destination                   ;(or destination "")
                 :on-change   (ui-callback/input-callback #(dispatch [::events/update-mapping-destination id %]))}]
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
                :on-click #(dispatch [::events/remove-port-mapping id])
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
                 ;[ui/GridRow {:key id}]
                 [single-port-mapping id mapping])]]
        [:div
         [ui/Icon {:name     "plus circle"
                   :on-click #(dispatch [::events/add-port-mapping (random-uuid) {}])}]]]])))


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
                 :on-change   (ui-callback/input-callback #(dispatch [::events/update-volume-source id %]))}]
      [ui/Input {:name        (str "destination-" id)
                 :placeholder "destination"
                 :value       destination
                 :on-change   (ui-callback/input-callback #(dispatch [::events/update-volume-destination id %]))}]
      [ui/Input {:name        (str "driver-" id)
                 :placeholder "driver"
                 :value       driver
                 :on-change   (ui-callback/input-callback #(dispatch [::events/update-volume-driver id %]))}]
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
                :on-click #(dispatch [::events/remove-volume id])
                :color    :red}]]]))


(defn volumes-section []
  (let [tr       (subscribe [::i18n-subs/tr])
        active?  (reagent/atom true)
        volumes  (subscribe [::subs/volumes])]
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
                 ;[ui/GridRow {:key id}]
                 [single-volume id volume])]]
        [:div
         [ui/Icon {:name     "plus circle"
                   :on-click #(dispatch [::events/add-volume (random-uuid) {}])}]]]])))


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

(defn save-action []
  (let [page-changed? (subscribe [::subs/page-changed?])]
    (fn []
      [ui/Button {:primary  true
                  :style    {:margin-top 10}
                  :disabled (not @(subscribe [::subs/page-changed?]))
                  :on-click #(dispatch [::events/open-save-modal])}
       "Save"])))

(defn sections []
  [:div
   [port-mappings-section]
   [:div {:style {:padding 5}}]
   [volumes-section]
   ;[:div {:style {:padding 5}}]
   ;   [runtime]
   ])

(defn logo-url-modal
  []
  (let [local-url (reagent/atom "")
        tr        (subscribe [::i18n-subs/tr])
        visible?  (subscribe [::subs/logo-url-modal-visible?])
        url       (subscribe [::subs/logo-url])]
    (fn []
      (let []
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-logo-url-modal])}

         [ui/ModalHeader (@tr [:select-logo-url])]

         [ui/ModalContent
          [ui/Input {:default-value (or @url "")
                     :placeholder   (@tr [:logo-url-placeholder])
                     :fluid         true
                     :auto-focus    true
                     :on-change     (ui-callback/input-callback #(reset! local-url %))
                     :on-key-press  (fn [e]
                                      (when (= 13 (.-charCode e))
                                        (dispatch [::events/save-logo-url @local-url])))}]]

         [ui/ModalActions
          [uix/Button {:text         "Ok"
                       :positive     true
                       :disabled     (empty? @local-url)
                       :active       true
                       :on-click     #(dispatch [::events/save-logo-url @local-url])
                       :on-key-press (fn [e]
                                       (if (= 13 (.-charCode e))
                                         (log/infof "Button ENTER")
                                         (log/infof "Button NOT ENTER")))}]]]))))

(defn save-modal
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        visible? (subscribe [::subs/save-modal-visible?])]
    (fn []
      (let []
        (log/infof "is true? %s" @visible?)
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-save-modal])}

         [ui/ModalHeader (str/capitalize (str (@tr [:save]) " " (@tr [:component])))]

         [ui/ModalContent
          [ui/Input {:placeholder  (@tr [:commit-placeholder])
                     :fluid        true
                     :auto-focus   true
                     :on-change    (ui-callback/input-callback #(dispatch [::events/commit-message %]))
                     :on-key-press (fn [e]
                                     (when (= 13 (.-charCode e))
                                       (dispatch [::events/close-save-modal])))}]]

         [ui/ModalActions
          [uix/Button {:text     (@tr [:save])
                       :positive true
                       :active   true
                       :on-click #(dispatch [::events/close-save-modal])}]]]))))

;(defn testing []
;  [ui/Grid
;   [ui/GridRow
;    [ui/GridColumn "LEFT"]
;    [ui/GridColumn "LEFT"]
;    [ui/GridColumn "LEFT"]
;    [ui/GridColumn {:floated :right
;                    } "RIGHT"]]
;   [ui/GridRow
;    [ui/GridColumn "LEFT"]
;    [ui/GridColumn "LEFT"]
;    [ui/GridColumn "LEFT"]
;    [ui/GridColumn {:floated :right
;                    } "RIGHT"]]
;   [ui/GridRow
;    [ui/GridColumn "LEFT"]
;    [ui/GridColumn "LEFT"]
;    [ui/GridColumn "LEFT"]
;    [ui/GridColumn {:floated :right
;                    } "RIGHT"]]
;   [ui/GridRow
;    [ui/GridColumn "LEFT"]
;    [ui/GridColumn "LEFT"]
;    [ui/GridColumn "LEFT"]
;    [ui/GridColumn {:floated :right
;                    } "RIGHT"]]
;   ])

(defn view-edit
  []
  (let [name   (subscribe [::subs/name])
        parent (subscribe [::subs/parent])]
    (fn []
      [ui/Container {:fluid true}
       [:h2 [ui/Icon {:name "th"}]
        @parent (when (not (empty? @parent)) "/") @name]
       [control-bar]
       [summary]
       [sections]
       [save-action]
       [logo-url-modal]
       [save-modal]
       [deployment-dialog-views/deploy-modal false]])))


(defn path->parent-path
  [path]
  (str/join "/" (rest path)))


(defmethod panel/render :module-component
  [path]
  (let [tr (subscribe [::i18n-subs/tr])]
    (dispatch [::events/name (str/capitalize (@tr [:new-component]))])
    (dispatch [::events/parent (path->parent-path @(subscribe [::main-subs/nav-path]))])
    [view-edit]))
