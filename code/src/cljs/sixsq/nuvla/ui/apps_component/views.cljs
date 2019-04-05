(ns sixsq.nuvla.ui.apps-component.views
  (:require
    [cljs.pprint :refer [cl-format]]
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as reagent]
    [sixsq.nuvla.ui.apps-component.events :as events]
    [sixsq.nuvla.ui.apps-component.spec :as spec]
    [sixsq.nuvla.ui.apps.spec :as apps-spec]
    [sixsq.nuvla.ui.apps-component.subs :as subs]
    [sixsq.nuvla.ui.apps.events :as apps-events]
    [sixsq.nuvla.ui.apps.subs :as apps-subs]
    [sixsq.nuvla.ui.apps.utils :as apps-utils]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.apps.views-detail :as apps-views-detail]
    [sixsq.nuvla.ui.deployment-dialog.views :as deployment-dialog-views]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.form-fields :as forms]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [taoensso.timbre :as log]
    [sixsq.nuvla.ui.utils.style :as style]))


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
                                       (ui-callback/input-callback
                                         #(do (dispatch [::main-events/changes-protection? true])
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


(defn architecture
  []
  (let [tr   (subscribe [::i18n-subs/tr])
        arch (subscribe [::subs/architecture])]
    [ui/TableRow
     [ui/TableCell {:collapsing true
                    :style      {:padding-bottom 8}} "architecture"]
     [ui/TableCell
      [ui/Label
       (log/infof "arch: %s" @arch)
       [ui/Dropdown {:name          (str "architecture")
                     :inline        true
                     :default-value @arch
                                    :options [{:key "x86", :value "x86", :text "x86"}
                                              {:key "ARM", :value "ARM", :text "ARM"}]
                                    :on-change (do (dispatch [::main-events/changes-protection? true])
                                                   (ui-callback/value #(dispatch [::events/architecture %])))
                     }]]]]))


(defn summary []
  (let []
    [apps-views-detail/summary
     [
      ^{:key "summary-docker-image"}
      [docker-image]
      ^{:key "summary-architecture"}
      [architecture]]
     ]))


(defn toggle [v]
  (swap! v not))


(defn input
  [id name value placeholder update-event value-spec]
  (let [active-input (subscribe [::apps-subs/active-input])
        validate?    (reagent/atom false)]
    (fn [id name value placeholder update-event value-spec]
      (let [input-name (str name "-" id)
            valid?     (s/valid? value-spec value)]
        [ui/Input {:name          input-name
                   :placeholder   placeholder
                   :default-value value
                   :error         (when (and @validate? (not valid?)) true)
                   :onMouseEnter  #(dispatch [::apps-events/active-input input-name])
                   :onMouseLeave  #(dispatch [::apps-events/active-input nil])
                   :on-change     (ui-callback/input-callback #(do
                                                                 (reset! validate? true)
                                                                 (dispatch [::main-events/changes-protection? true])
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
                  :on-click #(do (dispatch [::main-events/changes-protection? true])
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
          (@tr [:module-port-mapping])]
         [ui/AccordionContent {:active @active?}
          [:div (@tr [:module-publish-port]) " "
           [:span forms/nbsp (forms/help-popup (@tr [:module-port-mapping-help]))]]
          [:div [ui/Grid {:style {:margin-top    5
                                  :margin-bottom 5}}
                 (for [[id mapping] @mappings]
                   ^{:key id}
                   [single-port-mapping id mapping editable?])]]
          (when editable?
            [:div
             [ui/Icon {:name     "plus circle"
                       :on-click #(do (dispatch [::main-events/changes-protection? true])
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
         [ui/Dropdown {:name          (str "type-" id)
                       :default-value type
                       :selection     true
                       :options       [{:key "volume", :value "volume", :text "volume"}
                                       {:key "bind", :value "bind", :text "bind"}
                                       {:key "tmpfs", :value "tmpfs", :text "tmpfs"}]
                       :on-change     (ui-callback/value #(dispatch [::events/update-volume-type id %]))}]]
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
                  :on-click #(do (dispatch [::main-events/changes-protection? true])
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
                       :on-click #(do (dispatch [::main-events/changes-protection? true])
                                      (dispatch [::events/add-volume (random-uuid) {}]))}]])]]))))


(defn single-url
  [url-map]
  (let [tr (subscribe [::i18n-subs/tr])
        {:keys [id name url]} url-map]
    [ui/TableRow {:key id}
     (log/infof "url id: %s" id)
     [ui/TableCell {:floated :left
                     :width   2}
      [ui/Input {:name          (str "url-name-" id)
                 :placeholder   "name of this url"
                 :default-value name
                 :fluid         true
                 :on-change     (ui-callback/input-callback #(do (dispatch [::main-events/changes-protection? true])
                                                                 (dispatch [::events/update-url-name id %])))}]]
     [ui/TableCell {:floated :left
                     :width   13}
      [ui/Input {:name          (str "url-url-" id)
                 :placeholder   "url - e.g. http://${hostname}:${tcp.8888}/?token=${jupyter-token}"
                 :default-value url
                 :fluid         true
                 :on-change     (ui-callback/input-callback #(do (dispatch [::main-events/changes-protection? true])
                                                                 (dispatch [::events/update-url-url id %])))}]]
     [ui/TableCell {:floated :right
                     :width   1
                     :align   :right
                     :style   {}}
      [ui/Icon {:name     "trash"
                :on-click #(do (dispatch [::main-events/changes-protection? true])
                               (dispatch [::events/remove-url id]))
                :color    :red}]]]))


(defn urls-section []
  (let [tr      (subscribe [::i18n-subs/tr])
        active? (reagent/atom true)
        urls    (subscribe [::subs/urls])]
    (fn []
      [ui/Accordion {:fluid     true
                     :styled    true
                     :exclusive false}
       [ui/AccordionTitle {:active   @active?
                           :index    1
                           :on-click #(toggle active?)}
        [ui/Icon {:name (if @active? "dropdown" "caret right")}]
        (@tr [:urls])]

       [ui/AccordionContent {:active @active?}
        [:div (@tr [:urls])
         [:span forms/nbsp (forms/help-popup (@tr [:module-urls-help]))]]
        (if (empty? @urls)
          [ui/Message
           (str/capitalize (str (@tr [:no-output-parameters]) "."))]
          [:div [ui/Table {:style {:margin-top 10}
                           :class :nuvla-ui-editable}
                 [ui/TableHeader
                  [ui/TableRow
                   [ui/TableHeaderCell {:content "Name"}]
                   [ui/TableHeaderCell {:content "URL"}]
                   [ui/TableHeaderCell {:content "Action"}]]]
                 [ui/TableBody
                  (for [url-map (vals @urls)]
                    ^{:key (:id url-map)}
                    [single-url url-map])]]])
        [:div {:style {:padding-top 10}}
         [ui/Icon {:name     "plus circle"
                   :on-click #(do (dispatch [::main-events/changes-protection? true])
                                  (dispatch [::events/add-url (random-uuid) {}]))}]]]])))


(defn single-output-parameter [id param]
  (let [tr (subscribe [::i18n-subs/tr])
        {name        :name
         description :description} param]
    [ui/TableRow {:key id}
     [ui/TableCell {:floated :left
                     :width   2}
      [ui/Input {:name          (str "output-param-name-" id)
                 :placeholder   "output parameter name"
                 :default-value name
                 :fluid         true
                 :on-change     (ui-callback/input-callback
                                  #(do (dispatch [::main-events/changes-protection? true])
                                       (dispatch [::events/update-output-parameter-name id %])))}]]
     [ui/TableCell {:floated :left
                     :width   13}
      [ui/Input {:name          (str "url-url-" id)
                 :placeholder   "output parameter description"
                 :default-value description
                 :fluid         true
                 :on-change     (ui-callback/input-callback
                                  #(do (dispatch [::main-events/changes-protection? true])
                                       (dispatch [::events/update-output-parameter-description id %])))}]]
     [ui/TableCell {:floated :right
                     :width   1
                     :align   :right
                     :style   {}}
      [ui/Icon {:name     "trash"
                :on-click #(do (dispatch [::main-events/changes-protection? true])
                               (dispatch [::events/remove-output-parameter id]))
                :color    :red}]]]))


(defn output-parameters-section []
  (let [tr                (subscribe [::i18n-subs/tr])
        active?           (reagent/atom true)
        output-parameters (subscribe [::subs/output-parameters])]
    (fn []
      [ui/Accordion {:fluid     true
                     :styled    true
                     :exclusive false}
       [ui/AccordionTitle {:active   @active?
                           :index    1
                           :on-click #(toggle active?)}
        [ui/Icon {:name (if @active? "dropdown" "caret right")}]
        (@tr [:module-output-parameters])]

       [ui/AccordionContent {:active @active?}
        ;[:div (pr-str "output-parameters: " @output-parameters)]
        ;[:div (pr-str "module: " (get-in @module [:content :output-parameters]))]
        [:div (@tr [:module-output-parameters])
         [:span forms/nbsp (forms/help-popup (@tr [:module-output-parameters-help]))]]
        (if (empty? @output-parameters)
          [ui/Message
           (str/capitalize (str (@tr [:no-output-parameters]) "."))]
          [:div [ui/Table {:style {:margin-top 10}
                           :class :nuvla-ui-editable}
                 [ui/TableHeader
                  [ui/TableRow
                   [ui/TableHeaderCell {:content "Name"}]
                   [ui/TableHeaderCell {:content "Description"}]
                   [ui/TableHeaderCell {:content "Action"}]]]
                 [ui/TableBody
                  (for [[id param] @output-parameters]
                    ^{:key id}
                    [single-output-parameter id param])]]])
        [:div {:style {:padding-top 10}}
         [ui/Icon {:name     "plus circle"
                   :on-click #(do (dispatch [::main-events/changes-protection? true])
                                  (dispatch [::events/add-output-parameter (random-uuid) {}]))}]]]])))


(defonce data-type-options (atom [{:key "application/x-hdr", :value "application/x-hdr", :text "application/x-hdr"}
                                  {:key "application/x-clk", :value "application/x-clk", :text "application/x-clk"}
                                  {:key "text/plain", :value "text/plain", :text "text/plain"}]))


(defn add-data-type-options
  [option]
  (swap! data-type-options conj {:key option :value option :text option}))


(defn single-data-type [{:keys [id data-type]} dt-map]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/GridRow {:key id}
     [ui/GridColumn {:floated :left
                     :width   2}
      [ui/Label
       [ui/Dropdown {:name           (str "data-type-" id)
                     :default-value  data-type
                     :allowAdditions true
                     :selection      true
                     :additionLabel  "Additional data type: "
                     :search         true
                     :options        @data-type-options
                     :on-add-item    #(add-data-type-options (-> % .-target .-value))
                     :on-change      (do (dispatch [::main-events/changes-protection? true])
                                         (ui-callback/value #(dispatch [::events/update-data-type id %])))
                     }]]]
     [ui/GridColumn {:floated :right
                     :width   1
                     :align   :right
                     :style   {}}
      [ui/Icon {:name     "trash"
                :on-click #(do (dispatch [::main-events/changes-protection? true])
                               (dispatch [::events/remove-data-type id]))
                :color    :red}]]]))


(defn data-types-section []
  (let [tr         (subscribe [::i18n-subs/tr])
        active?    (reagent/atom true)
        data-types (subscribe [::subs/data-types])]
    (fn []
      [ui/Accordion {:fluid     true
                     :styled    true
                     :exclusive false}
       [ui/AccordionTitle {:active   @active?
                           :index    1
                           :on-click #(toggle active?)}
        [ui/Icon {:name (if @active? "dropdown" "caret right")}]
        (@tr [:data-binding])]

       [ui/AccordionContent {:active @active?}
        [:div (@tr [:data-type])
         [:span forms/nbsp (forms/help-popup (@tr [:module-data-type-help]))]]
        (if (empty? @data-types)
          [ui/Message
           (str/capitalize (str (@tr [:no-datasets]) "."))]
          [:div [ui/Grid {:style {:margin-top    5
                                  :margin-bottom 5}}
                 (for [[id dt] @data-types]
                   ^{:key id}
                   [single-data-type dt])]])
        [:div
         [ui/Icon {:name     "plus circle"
                   :on-click #(do (dispatch [::main-events/changes-protection? true])
                                  (dispatch [::events/add-data-type (random-uuid) {}]))}]]]])))


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
         [:div {:style {:padding-top 10}}]
         [urls-section]
         [:div {:style {:padding-top 10}}]
         [output-parameters-section]
         [:div {:style {:padding-top 10}}]
         [data-types-section]
         [:div {:style {:padding-top 10}}]
         [test-command]
         [apps-views-detail/save-action ::spec/module-component]
         [apps-views-detail/add-modal]
         [apps-views-detail/save-modal]
         [apps-views-detail/logo-url-modal]
         [deployment-dialog-views/deploy-modal]
         ]))))
