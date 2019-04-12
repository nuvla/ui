(ns sixsq.nuvla.ui.apps-component.views
  (:require
    [cljs.pprint :refer [cl-format]]
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as reagent]
    [sixsq.nuvla.ui.apps-component.events :as events]
    [sixsq.nuvla.ui.apps-component.spec :as spec]
    [sixsq.nuvla.ui.apps-component.subs :as subs]
    [sixsq.nuvla.ui.apps.events :as apps-events]
    [sixsq.nuvla.ui.apps.spec :as apps-spec]
    [sixsq.nuvla.ui.apps.subs :as apps-subs]
    [sixsq.nuvla.ui.apps.utils :as apps-utils]
    [sixsq.nuvla.ui.apps.views-detail :as apps-views-detail]
    [sixsq.nuvla.ui.deployment-dialog.views :as deployment-dialog-views]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.utils.form-fields :as forms]
    [sixsq.nuvla.ui.utils.form-fields :as form-fields]
    [sixsq.nuvla.ui.utils.general :as utils-general]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [taoensso.timbre :as log]))


(defn registry-url
  [image]
  (str/join ":" (-> image (str/split #":") drop-last)))


(defn get-image [module]
  (get-in module [:content :image]))


(defn docker-image-view
  [{:keys [image-name registry repository tag]}]
  [:span
   (when (not (empty? registry))
     [:span registry "/"])
   (when (not (empty? repository))
     [:span repository "/"])
   [:span image-name]
   (when (not (empty? tag))
     [:span ":" tag])
   ])

(defn docker-image
  []
  (let [tr           (subscribe [::i18n-subs/tr])
        active-input (subscribe [::apps-subs/active-input])
        module       (subscribe [::apps-subs/module])
        is-new?      (subscribe [::apps-subs/is-new?])
        validate?    (reagent/atom false)]
    (fn []
      (let [editable? (apps-utils/editable? @module @is-new?)
            image     (get-image @module)
            {:keys [image-name registry repository tag] :or {registry "" repository "" tag ""}} image
            label     (@tr [:module-docker-image-label])]
        [ui/TableRow
         [ui/TableCell {:collapsing true} (if editable? (apps-utils/mandatory-name label) label)]

         [ui/TableCell
          (if editable?
            [:div
             [ui/Input {:name          "docker-registry"
                        :default-value registry
                        :placeholder   (@tr [:module-docker-registry-placeholder])
                        :error         (when (and @validate? (not (s/valid? ::spec/registry registry))) true)
                        :on-change     (ui-callback/input-callback
                                         #(do (reset! validate? true)
                                              (dispatch [::main-events/changes-protection? true])
                                              (dispatch [::events/update-docker-registry %])))}]
             [:span "/"]
             [ui/Input {:name          "docker-repository"
                        :default-value repository
                        :placeholder   (@tr [:module-docker-repository-placeholder])
                        :error         (when (and @validate? (not (s/valid? ::spec/repository repository))) true)
                        :on-change     (ui-callback/input-callback
                                         #(do (reset! validate? true)
                                              (dispatch [::main-events/changes-protection? true])
                                              (dispatch [::events/update-docker-repository %])))}]
             [:span "/"]
             [ui/Input {:name          "docker-image-name"
                        :default-value image-name
                        :placeholder   (@tr [:module-docker-image-placeholder])
                        :error         (when (and @validate? (not (s/valid? ::spec/image-name image-name))) true)
                        :on-change     (ui-callback/input-callback
                                         #(do (reset! validate? true)
                                              (dispatch [::main-events/changes-protection? true])
                                              (dispatch [::events/update-docker-image-name %])))}]
             [:span ":"]
             [ui/Input {:name          "docker-tag"
                        :default-value tag
                        :placeholder   (@tr [:module-docker-tag-placeholder])
                        :error         (when (and @validate? (not (s/valid? ::spec/tag tag))) true)
                        :on-change     (ui-callback/input-callback
                                         #(do (reset! validate? true)
                                              (dispatch [::main-events/changes-protection? true])
                                              (dispatch [::events/update-docker-tag %])))}]]
            (docker-image-view image))
          ]]))))


(defn architecture
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        arch      (subscribe [::subs/architecture])
        module    (subscribe [::apps-subs/module])
        is-new?   (subscribe [::apps-subs/is-new?])
        editable? (apps-utils/editable? @module @is-new?)]
    [ui/TableRow
     [ui/TableCell {:collapsing true
                    :style      {:padding-bottom 8}} "architecture"]
     [ui/TableCell
      (if editable?
        [ui/Label
         [ui/Dropdown {:name          (str "architecture")
                       :inline        true
                       :default-value @arch
                       :options       [{:key "x86", :value "x86", :text "x86"}
                                       {:key "ARM", :value "ARM", :text "ARM"}]
                       :on-change     (ui-callback/value
                                        #(do (dispatch [::main-events/changes-protection? true])
                                             (dispatch [::events/architecture %])))
                       }]]
        [:span @arch])]]))


(defn summary []
  (let []
    [apps-views-detail/summary
     [
      ^{:key "summary-docker-image"}
      [docker-image]
      ^{:key "summary-architecture"}
      [architecture]]]))


(defn toggle [v]
  (swap! v not))


(defn show-count
  [coll]
  [:span form-fields/nbsp [ui/Label {:circular true} (count coll)]])


(defn input
  [id name value placeholder update-event value-spec]
  (let [active-input (subscribe [::apps-subs/active-input])
        validate?    (reagent/atom false)]
    (fn [id name value placeholder update-event value-spec]
      (let [input-name (str name "-" id)
            valid?     (s/valid? value-spec value)
            ]
        [ui/Input {:name          input-name
                   :placeholder   placeholder
                   :default-value value
                   :error         (when (and @validate? (not valid?)) true)
                   :onMouseEnter  #(dispatch [::apps-events/active-input input-name])
                   :onMouseLeave  #(dispatch [::apps-events/active-input nil])
                   :on-change     (ui-callback/input-callback
                                    #(do (reset! validate? true)
                                         (dispatch [::main-events/changes-protection? true])
                                         (dispatch [update-event id %])))}]))))


; TODO: too much duplication with input above
(defn input-int
  [id name value placeholder update-event value-spec]
  (let [active-input (subscribe [::apps-subs/active-input])
        validate?    (reagent/atom false)]
    (fn [id name value placeholder update-event value-spec]
      (let [input-name (str name "-" id)
            valid?     (s/valid? value-spec value)
            ]
        (s/explain value-spec 123)
        [ui/Input {:name          input-name
                   :placeholder   placeholder
                   :default-value value
                   :type          :number
                   :error         (when (and @validate? (not valid?)) true)
                   :onMouseEnter  #(dispatch [::apps-events/active-input input-name])
                   :onMouseLeave  #(dispatch [::apps-events/active-input nil])
                   :on-change     (ui-callback/input-callback
                                    #(do (reset! validate? true)
                                         (dispatch [::main-events/changes-protection? true])
                                         (dispatch [update-event id (utils-general/str->int %)])))}]))))


(defn single-port [port editable? tr]
  (let [{:keys [id
                published-port
                target-port
                protocol]} port]
    [ui/GridRow {:key id}
     [ui/GridColumn {:floated :left
                     :width   11}
      (if editable?
        [input-int id "published-port" published-port (@tr [:module-ports-published-port-placeholder])
         ::events/update-port-published ::spec/published-port]
        (when (number? published-port) [:span [:b published-port] " "]))
      [:span ": "]
      (if editable?
        [input-int id "target-port" target-port "dest. - e.g. 22 or 22-23"
         ::events/update-port-target ::spec/target-port]
        [:span [:b target-port]])
      (if editable?
        (do
          [:span " / "]
          [ui/Label
           [ui/Dropdown {:name      (str "protocol-" id)
                         :inline    true
                         :value     (or protocol "tcp")
                         :options   [{:key "TCP", :value "tcp", :text "TCP"}
                                     {:key "UDP", :value "udp", :text "UDP"}
                                     {:key "SCTP", :value "sctp", :text "SCTP"}]
                         :on-change (ui-callback/value
                                      #(do (dispatch [::main-events/changes-protection? true])
                                           (dispatch [::events/update-port-protocol id %])))
                         }]])
        (when (and (not (empty? protocol)) (not= "tcp" protocol))
          [:span " / " [:b protocol]]))]
     (when editable?
       [ui/GridColumn {:floated :right
                       :align   :right
                       :style   {}}
        [ui/Icon {:name     "trash"
                  :on-click #(do (dispatch [::main-events/changes-protection? true])
                                 (dispatch [::events/remove-port id]))
                  :color    :red}]])]))


(defn ports-section []
  (let [tr      (subscribe [::i18n-subs/tr])
        active? (reagent/atom true)
        ports   (subscribe [::subs/ports])
        module  (subscribe [::apps-subs/module])
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
          (@tr [:module-ports]) (show-count @ports)]
         [ui/AccordionContent {:active @active?}
          [:div (@tr [:module-publish-port]) " "
           [:span forms/nbsp (forms/help-popup (@tr [:module-ports-help]))]]
          (if (empty? @ports)
            [ui/Message
             (str/capitalize (str (@tr [:no-ports]) "."))]
            [:div [ui/Grid {:style {:margin-top    5
                                    :margin-bottom 5}}
                   (for [[id port] @ports]
                     ^{:key id}
                     [single-port port editable? tr])
                   ]])
          (when editable?
            [:div
             [ui/Icon {:name     "plus circle"
                       :on-click #(do (dispatch [::main-events/changes-protection? true])
                                      (dispatch [::events/add-port (random-uuid) {}]))}]])]]))))


(defn single-mount [mount editable?]
  (let [tr (subscribe [::i18n-subs/tr])
        {:keys [id
                mount-type
                source
                target
                read-only]} mount]
    [ui/GridRow {:key id}
     [ui/GridColumn {:floated :left
                     :width   15}
      (if editable?
        [ui/Label
         [ui/Dropdown {:name          (str "type-" id)
                       :default-value mount-type
                       :selection     true
                       :options       [{:key "volume", :value "volume", :text "volume"}
                                       {:key "bind", :value "bind", :text "bind"}
                                       ;{:key "tmpfs", :value "tmpfs", :text "tmpfs"}
                                       ]
                       :on-change     (ui-callback/value
                                        #(do (dispatch [::main-events/changes-protection? true])
                                             (dispatch [::events/update-mount-type id %])))}]]
        [:span "type=" [:b mount-type]])
      [:span " , "]
      (if editable?
        [input id "vol-source" source "source" ::events/update-mount-source ::spec/input-value]
        [:span "src=" [:b source]])
      [:span " , "]
      (if editable?
        [input id "vol-dest" target "target" ::events/update-mount-target ::spec/input-value]
        [:span "dst=" [:b target]])
      (if editable?
        (do
          [:span " , "]
          [:span " " (@tr [:module-mount-read-only?]) " "
           [ui/Checkbox {:name            "read-only"
                         :default-checked (if (nil? read-only) false read-only)
                         :on-change       (ui-callback/checked
                                            #(do (dispatch [::main-events/changes-protection? true])
                                                 (dispatch [::events/update-mount-read-only? id %])))
                         :align           :middle}]
           ])
        (when read-only (do [:span " , " [:b "readonly"]])))]
     (when editable?
       [ui/GridColumn {:floated :right
                       :width   1
                       :align   :right
                       :style   {}}
        [ui/Icon {:name     "trash"
                  :on-click #(do (dispatch [::main-events/changes-protection? true])
                                 (dispatch [::events/remove-mount id]))
                  :color    :red}]])]))


(defn mounts-section []
  (let [tr      (subscribe [::i18n-subs/tr])
        active? (reagent/atom true)
        module  (subscribe [::apps-subs/module])
        mounts  (subscribe [::subs/mounts])
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
          (@tr [:module-mounts]) (show-count @mounts)]

         [ui/AccordionContent {:active @active?}
          [:div "Container volumes (i.e. mounts) "
           [:span forms/nbsp (forms/help-popup (@tr [:module-mount-help]))]]
          (if (empty? @mounts)
            [ui/Message
             (str/capitalize (str (@tr [:no-mounts]) "."))]
            [:div [ui/Grid {:style {:margin-top    5
                                    :margin-bottom 5}}
                   (for [[id mount] @mounts]
                     ^{:key id}
                     [single-mount mount editable?])]])
          (when editable?
            [:div
             [ui/Icon {:name     "plus circle"
                       :on-click #(do (dispatch [::main-events/changes-protection? true])
                                      (dispatch [::events/add-mount (random-uuid) {}]))}]])]]))))


(defn single-url
  [url-map]
  (let [tr (subscribe [::i18n-subs/tr])
        {:keys [id name url]} url-map]
    [ui/TableRow {:key id}
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
        active? (reagent/atom false)
        urls    (subscribe [::subs/urls])]
    (fn []
      [ui/Accordion {:fluid     true
                     :styled    true
                     :exclusive false}
       [ui/AccordionTitle {:active   @active?
                           :index    1
                           :on-click #(toggle active?)}
        [ui/Icon {:name (if @active? "dropdown" "caret right")}]
        (@tr [:urls]) (show-count @urls)]

       [ui/AccordionContent {:active @active?}
        [:div (@tr [:urls])
         [:span forms/nbsp (forms/help-popup (@tr [:module-urls-help]))]]
        (if (empty? @urls)
          [ui/Message
           (str/capitalize (str (@tr [:no-urls]) "."))]
          [:div [ui/Table {:style {:margin-top 10}
                           :class :nuvla-ui-editable}
                 [ui/TableHeader
                  [ui/TableRow
                   [ui/TableHeaderCell {:content "Name"}]
                   [ui/TableHeaderCell {:content "URL"}]
                   [ui/TableHeaderCell {:content "Action"}]]]
                 [ui/TableBody
                  (for [[id url-map] @urls]
                    ^{:key id}
                    [single-url url-map])]]])
        [:div {:style {:padding-top 10}}
         [ui/Icon {:name     "plus circle"
                   :on-click #(do (dispatch [::main-events/changes-protection? true])
                                  (dispatch [::events/add-url (random-uuid) {}]))}]]]])))


(defn single-output-parameter [param]
  (let [tr (subscribe [::i18n-subs/tr])
        {:keys [id name description]} param]
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
                    :align   :right}
      [ui/Icon {:name     "trash"
                :on-click #(do (dispatch [::main-events/changes-protection? true])
                               (dispatch [::events/remove-output-parameter id]))
                :color    :red}]]]))


(defn output-parameters-section []
  (let [tr                (subscribe [::i18n-subs/tr])
        active?           (reagent/atom false)
        output-parameters (subscribe [::subs/output-parameters])]
    (fn []
      [ui/Accordion {:fluid     true
                     :styled    true
                     :exclusive false}
       [ui/AccordionTitle {:active   @active?
                           :index    1
                           :on-click #(toggle active?)}
        [ui/Icon {:name (if @active? "dropdown" "caret right")}]
        (@tr [:module-output-parameters]) (show-count @output-parameters)]

       [ui/AccordionContent {:active @active?}
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
                    [single-output-parameter param])]]])
        [:div {:style {:padding-top 10}}
         [ui/Icon {:name     "plus circle"
                   :on-click #(do (dispatch [::main-events/changes-protection? true])
                                  (dispatch [::events/add-output-parameter (random-uuid) {}]))}]]]])))


(def data-type-options (atom [{:key "application/x-hdr", :value "application/x-hdr", :text "application/x-hdr"}
                              {:key "application/x-clk", :value "application/x-clk", :text "application/x-clk"}
                              {:key "text/plain", :value "text/plain", :text "text/plain"}]))


(defn add-data-type-options
  [option]
  (swap! data-type-options conj {:key option :value option :text option}))


(defn single-data-type [dt]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [dt]
      (let [{:keys [id data-type]} dt]
        [ui/GridRow {:key id}
         [ui/GridColumn {:floated :left
                         :width   2}
          [ui/Label
           [ui/Dropdown {:name           (str "data-type-" id)
                         :default-value  (or data-type "\"text/plain\"")
                         :allowAdditions true
                         :selection      true
                         :additionLabel  "Additional data type: "
                         :search         true
                         :options        @data-type-options
                         :on-add-item    #(add-data-type-options (-> % .-target .-value))
                         :on-change      (ui-callback/value
                                           #(do (dispatch [::main-events/changes-protection? true])
                                                (dispatch [::events/update-data-type id %])))
                         }]]]
         [ui/GridColumn {:floated :right
                         :width   1
                         :align   :right
                         :style   {}}
          [ui/Icon {:name     "trash"
                    :on-click #(do (dispatch [::main-events/changes-protection? true])
                                   (dispatch [::events/remove-data-type id]))
                    :color    :red}]]]))))


(defn data-types-section []
  (let [tr         (subscribe [::i18n-subs/tr])
        active?    (reagent/atom false)
        data-types (subscribe [::subs/data-types])]
    (fn []
      [ui/Accordion {:fluid     true
                     :styled    true
                     :exclusive false}
       [ui/AccordionTitle {:active   @active?
                           :index    1
                           :on-click #(toggle active?)}
        [ui/Icon {:name (if @active? "dropdown" "caret right")}]
        (@tr [:data-binding]) (show-count @data-types)]

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
  (let [ports-args
        (for [[id port] ports]
          (let [{:keys [published-port target-port protocol]} port]
            (str "-p " published-port ":" target-port (when
                                                        (and
                                                          (not= "tcp" protocol)
                                                          (not (empty? protocol)))
                                                        (str "/" protocol)))))]
    (str/join " " ports-args)))


(defn generate-mounts-args
  [mounts]
  (let [mounts-commands
        (for [[id {:keys [mount-type source target read-only]}] mounts]
          (conj (str
                  "--mount type=" mount-type
                  ",src=" source
                  ",dst=" target
                  (when read-only ",readonly"))))]
    (str/join " " mounts-commands)))


(defn generate-image-arg
  [{:keys [registry repository image-name tag]}]
  (str
    (when (not (empty? registry))
      (str registry "/"))
    (when (not (empty? repository))
      (str repository "/"))
    image-name
    (when (not (empty? tag))
      (str ":" tag))))


(defn test-command
  []
  (let [tr           (subscribe [::i18n-subs/tr])
        mappings     (subscribe [::subs/ports])
        mapping-args (generate-ports-args @mappings)
        mounts       (subscribe [::subs/mounts])
        mounts-args  (generate-mounts-args @mounts)
        image        (get-image @(subscribe [::apps-subs/module]))
        image-arg    (generate-image-arg image)
        command      (str "docker service create " mapping-args " " mounts-args " " image-arg)]
    [ui/Message {:info true}
     [ui/MessageHeader (@tr [:module-docker-command-message])]
     [:p {:style {:padding-top 8}
          :class "nuvla-command"} [:b "$ " command " "]
      [ui/Popup {:trigger  (reagent/as-element [ui/CopyToClipboard {:text command}
                                                [:a [ui/Icon {:name "clipboard outline"}]]])
                 :position "top center"}
       "copy to clipboard"]]
     [:p "Note: ensure you have a recent installation of docker."]]))


(defn clear-module
  []
  (dispatch [::events/clear-module]))


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
         [ports-section]
         [:div {:style {:padding-top 10}}]
         [mounts-section]
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
         [deployment-dialog-views/deploy-modal]]))))
