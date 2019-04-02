(ns sixsq.nuvla.ui.apps-component.views
  (:require
    [cljs.pprint :refer [cl-format]]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as reagent]
    [sixsq.nuvla.ui.apps-component.events :as events]
    [sixsq.nuvla.ui.apps-component.subs :as subs]
    [sixsq.nuvla.ui.apps-component.spec :as spec]
    [sixsq.nuvla.ui.apps.events :as apps-events]
    [sixsq.nuvla.ui.apps.subs :as apps-subs]
    [sixsq.nuvla.ui.apps.views-detail :as apps-views-detail]
    [sixsq.nuvla.ui.deployment-dialog.views :as deployment-dialog-views]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.form-fields :as forms]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [clojure.string :as str]
    [taoensso.timbre :as log]
    [sixsq.nuvla.ui.apps.utils :as apps-utils]
    [cljs.spec.alpha :as s]))


(defn registry-url
  [image]
  (str/join ":" (-> image (str/split #":") drop-last)))


(defn get-image [module]
  (get-in module [:content :image] ""))


(defn docker-image
  []
  (let [tr           (subscribe [::i18n-subs/tr])
        active-input (subscribe [::apps-subs/active-input])
        module       (subscribe [::apps-subs/module])
        is-new?      (subscribe [::apps-subs/is-new?])
        validate?    (reagent/atom false)]
    (fn []
      (let [editable?     (apps-utils/editable? @module @is-new?)
            name          "docker-image"
            input-active? (= name @active-input)
            image         (get-image @module)
            valid?        (s/valid? ::spec/docker-image image)
            label         (@tr [:module-docker-image-label])]
        [ui/TableRow
         [ui/TableCell {:collapsing true} (if editable? (apps-utils/mandatory-name label) label)]
         [ui/TableCell
          (if editable?
            [ui/Input {:name         "docker-image"
                       :value        image
                       :placeholder  (@tr [:module-docker-image-placeholder])
                       :fluid        true
                       :error        (when (and @validate? (not valid?)) true)
                       :icon         (when input-active? :pencil)
                       :onMouseEnter #(dispatch [::apps-events/active-input name])
                       :onMouseLeave #(dispatch [::apps-events/active-input nil])
                       :on-change    (do
                                       (reset! validate? true)
                                       (ui-callback/input-callback #(do (dispatch [::apps-events/page-changed? true])
                                                                        (dispatch [::apps-events/docker-image %]))))}]
            [:div {:style {:padding-left 15}} image])
          [:a {:href   (str "http://hub.docker.com/_/" (registry-url image))
               :target "_blank"
               :style  {:padding-left 15}}
           "Access registry "
           [ui/Icon {:name  :external
                     :style {:padding-left   5
                             :padding-top    5
                             :padding-bottom 15}}]]]]))))


(defn summary []
  (let []
    (fn []
      (let []
        [apps-views-detail/summary
         [docker-image]]))))


(defn toggle [v]
  (swap! v not))


(defn input
  [id name value placeholder update-event value-spec]
  (let [active-input (subscribe [::apps-subs/active-input])
        validate?    (reagent/atom false)]
    (fn [id name value placeholder update-event value-spec]
      (let [input-name (str name "-" id)
            valid?     (s/valid? value-spec value)]
        [ui/Input {:name         input-name
                   :placeholder  placeholder
                   :value        value
                   :error        (when (and @validate? (not valid?)) true)
                   :onMouseEnter #(dispatch [::apps-events/active-input input-name])
                   :onMouseLeave #(dispatch [::apps-events/active-input nil])
                   :on-change    (ui-callback/input-callback #(do
                                                                (reset! validate? true)
                                                                (dispatch [::apps-events/page-changed? true])
                                                                (dispatch [update-event id %])))}]))))


(defn single-port-mapping [id mapping editable?]
  (let [{source      :source
         destination :destination
         port-type   :port-type} mapping]
    [ui/GridRow {:key id}
     [ui/GridColumn {:floated :left
                     :width   11}
      (if editable?
        [input id "port-source" source "source - e.g. 22 or 22-23"
         ::events/update-mapping-source ::spec/input-value]
        [:span [:b source]])
      [:span " : "]
      (if editable?
        [input id "port-dest" destination "dest. - e.g. 22 or 22-23"
         ::events/update-mapping-destination ::spec/input-value]
        [:span [:b destination]])
      (if editable?
        (do
          [:span " / "]
          [ui/Label
           [ui/Dropdown {:name      (str "port-type-" id)
                         :inline    true
                         :value     port-type
                         :options   [{:key "TCP", :value "TCP", :text "TCP"}
                                     {:key "UDP", :value "UDP", :text "UDP"}]
                         :on-change (ui-callback/value #(dispatch [::events/update-mapping-port-type id %]))
                         }]])
        (when (not= "xTCP" port-type)
          [:span " / " [:b port-type]]))]
     (when editable?
       [ui/GridColumn {:floated :right
                       :align   :right
                       :style   {}}
        [ui/Icon {:name     "trash"
                  :on-click #(do (dispatch [::apps-events/page-changed? true])
                                 (dispatch [::events/remove-port-mapping id]))
                  :color    :red}]])]))


(defn port-mappings-section []
  (let [tr       (subscribe [::i18n-subs/tr])
        active?  (reagent/atom true)
        mappings (subscribe [::subs/port-mappings])
        module   (subscribe [::apps-subs/module])
        is-new?  (subscribe [::apps-subs/is-new?])]
    (fn []
      (let [editable? (apps-utils/editable? @module @is-new?)]
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
                   [single-port-mapping id mapping editable?])]]
          (when editable?
            [:div
             [ui/Icon {:name     "plus circle"
                       :on-click #(do (dispatch [::apps-events/page-changed? true])
                                      (dispatch [::events/add-port-mapping (random-uuid) {}]))}]])]]))))


(defn single-volume [id volume editable?]
  (let [tr (subscribe [::i18n-subs/tr])
        {type        :type
         source      :source
         destination :destination
         driver      :driver
         read-only?  :read-only?} volume]
    [ui/GridRow {:key id}
     [ui/GridColumn {:floated :left
                     :width   15}
      (if editable?
        [ui/Label
         [ui/Dropdown {:name      (str "type-" id)
                       :value     type
                       :selection true
                       :options   [{:key "volume", :value "volume", :text "volume"}
                                   {:key "bind", :value "bind", :text "bind"}
                                   {:key "tmpfs", :value "tmpfs", :text "tmpfs"}]
                       :on-change (ui-callback/value #(dispatch [::events/update-volume-type id %]))}]]
        [:span "type=" [:b type]])
      [:span " , "]
      (if editable?
        [input id "vol-source" source "source" ::events/update-volume-source ::spec/input-value]
        [:span "src=" [:b source]])
      [:span " , "]
      (if editable?
        [input id "vol-dest" destination "destination" ::events/update-volume-destination ::spec/input-value]
        [:span "dst=" [:b destination]])
      [:span " , "]
      (if editable?
        [input id "vol-driver" driver "driver" ::events/update-volume-driver ::spec/input-value]
        [:span "volume-driver=" [:b driver]])
      (if editable?
        (do
          [:span " , "]
          [:span " " (@tr [:module-volume-read-only?]) " "
           [ui/Checkbox {:name      "read-only"
                         :checked   read-only?
                         :on-change (ui-callback/checked
                                      #(dispatch [::events/update-volume-read-only? id %]))
                         :align     :middle}]
           ])
        (when read-only? (do [:span " , " [:b "readonly"]])))]
     (when editable?
       [ui/GridColumn {:floated :right
                       :width   1
                       :align   :right
                       :style   {}}
        [ui/Icon {:name     "trash"
                  :on-click #(do (dispatch [::apps-events/page-changed? true])
                                 (dispatch [::events/remove-volume id]))
                  :color    :red}]])]))


(defn volumes-section []
  (let [tr      (subscribe [::i18n-subs/tr])
        active? (reagent/atom true)
        module  (subscribe [::apps-subs/module])
        volumes (subscribe [::subs/volumes])
        is-new? (subscribe [::apps-subs/is-new?])]
    (fn []
      (let [editable? (apps-utils/editable? @module @is-new?)]
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
                   [single-volume id volume editable?])]]
          (when editable?
            [:div
             [ui/Icon {:name     "plus circle"
                       :on-click #(do (dispatch [::apps-events/page-changed? true])
                                      (dispatch [::events/add-volume (random-uuid) {}]))}]])]]))))


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


(defn generate-ports-args
  [ports]
  (let [ports-commands
        (for [{:keys [source destination port-type]} ports]
          (conj (str "-p " source ":" destination (when (not= "TCP" port-type) (str "/" port-type)))))]
    (str/join " " ports-commands)))


(defn generate-volumes-args
  [volumes]
  (let [volumes-commands
        (for [{:keys [type source destination driver read-only?]} volumes]
          (conj (str
                  "--mount type=" type
                  ",src=" source
                  ",dst=" destination
                  ",volume-driver=" driver
                  (when read-only? ",readonly"))))]
    (str/join " " volumes-commands)))


(defn test-command
  []
  (let [tr           (subscribe [::i18n-subs/tr])
        mappings     (subscribe [::subs/port-mappings])
        mapping-args (generate-ports-args (vals @mappings))
        volumes      (subscribe [::subs/volumes])
        volumes-args (generate-volumes-args (vals @volumes))
        image        (get-image @(subscribe [::apps-subs/module]))
        command      (str "docker service create " mapping-args " " volumes-args " " image)]
    [ui/Message {:info true}
     [ui/MessageHeader (@tr [:module-docker-command-message])]
     [:p command " "
      [ui/Popup {:trigger  (reagent/as-element [ui/CopyToClipboard {:text command}
                                                [:a [ui/Icon {:name "clipboard outline"}]]])
                 :position "top center"}
       "copy to clipboard"]]]))


(defn view-edit
  []
  (let [module (subscribe [::apps-subs/module])]
    (fn []
      (let [name   (:name @module)
            parent (:parent-path @module)]
        [ui/Container {:fluid true}
         [:h2 [ui/Icon {:name "th"}]
          parent (when (not-empty parent) "/") name]
         [apps-views-detail/control-bar ::spec/module-component]
         [summary]
         [port-mappings-section]
         [:div {:style {:padding-top 10}}]
         [volumes-section]
         [test-command]
         [apps-views-detail/save-action ::spec/module-component]
         [deployment-dialog-views/deploy-modal]
         ]))))
