(ns sixsq.nuvla.ui.apps-component.views
  (:require
    [cljs.pprint :refer [cl-format]]
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.acl.views :as acl]
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
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [taoensso.timbre :as log]))


(defn input
  [id name value placeholder update-event value-spec fluid?]
  (let [local-validate? (r/atom false)
        form-valid?     (subscribe [::apps-subs/form-valid?])]
    (fn [id name value placeholder update-event value-spec fluid?]
      (let [input-name (str name "-" id)
            validate?  (or @local-validate? (not @form-valid?))]
        [ui/Input {:name          input-name
                   :placeholder   placeholder
                   :default-value value
                   :type          :text
                   :error         (when (and validate? (not (s/valid? value-spec value))) true)
                   :fluid         fluid?
                   :onMouseEnter  #(dispatch [::apps-events/active-input input-name])
                   :onMouseLeave  #(dispatch [::apps-events/active-input nil])
                   :on-change     (ui-callback/input-callback
                                    #(do (reset! local-validate? true)
                                         (dispatch [update-event id %])
                                         (dispatch [::main-events/changes-protection? true])
                                         (dispatch [::apps-events/validate-form])))}]))))


(defn trash
  [id remove-event]
  [ui/Icon {:name     "trash"
            :on-click #(do (dispatch [::main-events/changes-protection? true])
                           (dispatch [remove-event id])
                           (dispatch [::apps-events/validate-form]))
            :color    :red}])


(defn plus
  [add-event]
  [ui/Icon {:name     "add"
            :color    "green"
            :on-click #(do (dispatch [::main-events/changes-protection? true])
                           (dispatch [add-event (random-uuid) {}])
                           (dispatch [::apps-events/validate-form]))}])


(defn registry-url
  [image]
  (str/join ":" (-> image (str/split #":") drop-last)))


(defn docker-image-view
  [{:keys [::spec/image-name ::spec/registry ::spec/repository ::spec/tag] :as image}]
  [:span
   (when (not (empty? registry))
     [:span registry "/"])
   (when (not (empty? repository))
     [:span repository "/"])
   [:span image-name]
   (when (not (empty? tag))
     [:span ":" tag])])


(defn docker-image
  []
  (let [tr              (subscribe [::i18n-subs/tr])
        module          (subscribe [::apps-subs/module])
        image           (subscribe [::subs/image])
        is-new?         (subscribe [::apps-subs/is-new?])
        local-validate? (r/atom false)
        form-valid?     (subscribe [::apps-subs/form-valid?])]
    (fn []
      (let [editable? (apps-utils/editable? @module @is-new?)
            {:keys [::spec/image-name ::spec/registry ::spec/repository ::spec/tag]
             :or   {registry "" repository "" image "" tag ""}} @image
            label     (@tr [:module-docker-image-label])
            validate? (or @local-validate? (not @form-valid?))]
        [ui/TableRow
         [ui/TableCell {:collapsing true} (if editable? (apps-utils/mandatory-name label) label)]

         ; force react to regenerate the content of this cell with a random key
         ^{:key (:id @module "1")}
         [ui/TableCell
          (if editable?
            [:div
             [input "docker-registry" "docker-registry" registry
              (@tr [:module-docker-registry-placeholder]) ::events/update-docker-registry ::spec/registry]
             [:span "/"]
             [input "docker-repository" "docker-repository" repository
              (@tr [:module-docker-repository-placeholder]) ::events/update-docker-repository ::spec/repository]
             [:span "/"]
             [input "docker-image-name" "docker-image-name" image-name
              (@tr [:module-docker-image-placeholder]) ::events/update-docker-image-name ::spec/image-name]
             [:span ":"]
             [input "docker-tag" "docker-tag" tag
              (@tr [:module-docker-tag-placeholder]) ::events/update-docker-tag ::spec/tag]]
            (docker-image-view @image))]]))))


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
                                             (dispatch [::events/architecture %])))}]]
        [:span @arch])]]))


(defn summary []
  (let []
    [apps-views-detail/summary
     [
      ^{:key "summary-docker-image"}
      [docker-image]
      ^{:key "summary-architecture"}
      [architecture]]]))


(defn show-count
  [coll]
  [:span form-fields/nbsp [ui/Label {:circular true} (count coll)]])


(defn single-port
  [port editable? tr]
  (let [{:keys [id
                ::spec/published-port
                ::spec/target-port
                ::spec/protocol]} port]
    [ui/TableRow

     [ui/TableCell
      (if editable?
        [input id "published-port" published-port (@tr [:module-ports-published-port-placeholder])
         ::events/update-port-published ::spec/published-port]
        (when (number? published-port) [:span [:b published-port] " "]))]

     [ui/TableCell
      (if editable?
        [input id "target-port" target-port "dest. - e.g. 22 or 22-23"
         ::events/update-port-target ::spec/target-port]
        [:span [:b target-port]])]

     [ui/TableCell
      (if editable?
        [ui/Dropdown {:name      (str "protocol-" id)
                      :selection true
                      :value     (or protocol "tcp")
                      :options   [{:key "TCP", :value "tcp", :text "TCP"}
                                  {:key "UDP", :value "udp", :text "UDP"}
                                  {:key "SCTP", :value "sctp", :text "SCTP"}]
                      :on-change (ui-callback/value
                                   #(do (dispatch [::main-events/changes-protection? true])
                                        (dispatch [::events/update-port-protocol id %])
                                        (dispatch [::apps-events/validate-form])))}]
        (when (and (not (empty? protocol)) (not= "tcp" protocol))
          [:b protocol]))]

     (when editable?
       [ui/TableCell
        [trash id ::events/remove-port]])]))


(defn ports-section []
  (let [tr      (subscribe [::i18n-subs/tr])
        ports   (subscribe [::subs/ports])
        module  (subscribe [::apps-subs/module])
        is-new? (subscribe [::apps-subs/is-new?])]
    (fn []
      (let [editable? (apps-utils/editable? @module @is-new?)]
        [uix/Accordion
         [:<>
          [:div (@tr [:module-publish-port]) " "
           [:span forms/nbsp (forms/help-popup (@tr [:module-ports-help]))]]
          (if (empty? @ports)
            [ui/Message
             (str/capitalize (str (@tr [:no-ports]) "."))]
            [:div [ui/Table {:style {:margin-top 10}
                             :class :nuvla-ui-editable}
                   [ui/TableHeader
                    [ui/TableRow
                     [ui/TableHeaderCell {:content "Destination (External)"}]
                     [ui/TableHeaderCell {:content "Source (Internal)"}]
                     [ui/TableHeaderCell {:content "Protocol"}]
                     (when editable?
                       [ui/TableHeaderCell {:content "Action"}])]]
                   [ui/TableBody
                    (for [[id port] @ports]
                      ^{:key id}
                      [single-port port editable? tr])]]])
          (when editable?
            [:div {:style {:padding-top 10}}
             [plus ::events/add-port]])]
         :label (@tr [:module-ports])
         :count (count @ports)]))))


(defn single-mount [mount editable?]
  (let [tr (subscribe [::i18n-subs/tr])
        {:keys [id
                ::spec/mount-type
                ::spec/mount-source
                ::spec/mount-target
                ::spec/mount-read-only]} mount]

    [ui/TableRow

     [ui/TableCell
      (if editable?
        [ui/Dropdown {:name          (str "type-" id)
                      :default-value mount-type
                      :selection     true
                      :options       [{:key "volume", :value "volume", :text "volume"}
                                      {:key "bind", :value "bind", :text "bind"}
                                      ;{:key "tmpfs", :value "tmpfs", :text "tmpfs"}
                                      ]
                      :on-change     (ui-callback/value
                                       #(do (dispatch [::main-events/changes-protection? true])
                                            (dispatch [::events/update-mount-type id %])
                                            (dispatch [::apps-events/validate-form])))}]
        [:span "type=" [:b mount-type]])]

     [ui/TableCell
      (if editable?
        ;id name value placeholder update-event value-spec
        [input id "vol-source" mount-source "source"
         ::events/update-mount-source ::spec/mount-source false]
        [:span "src=" [:b mount-source]])]

     [ui/TableCell
      (if editable?
        [input id "vol-dest" mount-target "target"
         ::events/update-mount-target ::spec/mount-target false]
        [:span "dst=" [:b mount-target]])]

     [ui/TableCell
      (if editable?
        [ui/Checkbox {:name      "read-only"
                      :checked   (if (nil? mount-read-only) false mount-read-only)
                      :on-change (ui-callback/checked
                                   #(do (dispatch [::main-events/changes-protection? true])
                                        (dispatch [::events/update-mount-read-only? id %])
                                        (dispatch [::apps-events/validate-form])))
                      :align     :middle}]
        (when mount-read-only [:b "readonly"]))]

     (when editable?
       [ui/TableCell
        [trash id ::events/remove-mount]])]))


(defn mounts-section []
  (let [tr      (subscribe [::i18n-subs/tr])
        module  (subscribe [::apps-subs/module])
        mounts  (subscribe [::subs/mounts])
        is-new? (subscribe [::apps-subs/is-new?])]
    (fn []
      (let [editable? (apps-utils/editable? @module @is-new?)]
        [uix/Accordion
         [:<>
          [:div "Container volumes (i.e. mounts) "
           [:span forms/nbsp (forms/help-popup (@tr [:module-mount-help]))]]
          (if (empty? @mounts)
            [ui/Message
             (str/capitalize (str (@tr [:no-mounts]) "."))]
            [:div [ui/Table {:style {:margin-top 10}
                             :class :nuvla-ui-editable}
                   [ui/TableHeader
                    [ui/TableRow
                     [ui/TableHeaderCell {:content "Type"}]
                     [ui/TableHeaderCell {:content "Source"}]
                     [ui/TableHeaderCell {:content "Target"}]
                     [ui/TableHeaderCell {:content "Read only?"}]
                     (when editable?
                       [ui/TableHeaderCell {:content "Action"}])]]
                   [ui/TableBody
                    (for [[id mount] @mounts]
                      ^{:key id}
                      [single-mount mount editable?])]]])
          (when editable?
            [:div
             [plus ::events/add-mount]])]
         :label (@tr [:module-mounts])
         :count (count @mounts)]))))


(defn single-env-variable
  [env-variable editable?]
  (let [tr              (subscribe [::i18n-subs/tr])
        form-valid?     (subscribe [::apps-subs/form-valid?])
        local-validate? (r/atom false)]
    (let [{:keys [id
                  ::spec/env-name
                  ::spec/env-description
                  ::spec/env-value
                  ::spec/env-required]} env-variable
          validate? (or @local-validate? (not @form-valid?))]
      [ui/TableRow {:key id}
       [ui/TableCell {:floated :left
                      :width   3}
        (if editable?
          (let [input-name (str name "-" id)]
            [ui/Input {:name         (str name "-" id)
                       :placeholder  "name"
                       :type         :text
                       :value        (or env-name "")
                       :error        (when (and validate? (not (s/valid? ::spec/env-name env-name))) true)
                       :fluid        true
                       :onMouseEnter #(dispatch [::apps-events/active-input input-name])
                       :onMouseLeave #(dispatch [::apps-events/active-input nil])
                       :on-change    (ui-callback/input-callback
                                       #(do
                                          (reset! local-validate? true)
                                          (dispatch [::events/update-env-name id (str/upper-case %)])
                                          (dispatch [::main-events/changes-protection? true])
                                          (dispatch [::apps-events/validate-form])))}])

          [:span env-name])]

       [ui/TableCell {:floated :left
                      :width   3}
        (if editable?
          [input id "value" env-value "value"
           ::events/update-env-value ::spec/env-value true]
          [:span env-value])]

       [ui/TableCell {:floated :left
                      :width   8}
        (if editable?
          [input id "description" env-description "description"
           ::events/update-env-description ::spec/env-description true]
          [:span env-description])]

       [ui/TableCell {:floated    :left
                      :text-align :center
                      :width      1}
        [ui/Checkbox {:name      (@tr [:required])
                      :checked   (or env-required false)
                      :disabled  (not editable?)
                      :on-change (ui-callback/checked
                                   #(do (dispatch [::main-events/changes-protection? true])
                                        (dispatch [::events/update-env-required id %])
                                        (dispatch [::apps-events/validate-form])))
                      :align     :middle}]]

       (when editable?
         [ui/TableCell {:floated :right
                        :width   1
                        :align   :right
                        :style   {}}
          [trash id ::events/remove-env-variable]])])))


(defn env-variables-section []
  (let [tr            (subscribe [::i18n-subs/tr])
        module        (subscribe [::apps-subs/module])
        env-variables (subscribe [::subs/env-variables])
        is-new?       (subscribe [::apps-subs/is-new?])]
    (fn []
      (let [editable? (apps-utils/editable? @module @is-new?)]
        [uix/Accordion
         [:<>
          [:div "Container Environmental variables (i.e. env) "
           [:span forms/nbsp (forms/help-popup (@tr [:module-env-variables-help]))]]
          (if (empty? @env-variables)
            [ui/Message
             (str/capitalize (str (@tr [:module-no-env-variables]) "."))]
            [:div [ui/Table {:style {:margin-top 10}
                             :class :nuvla-ui-editable}
                   [ui/TableHeader
                    [ui/TableRow
                     [ui/TableHeaderCell {:content "Name"}]
                     [ui/TableHeaderCell {:content "Value"}]
                     [ui/TableHeaderCell {:content "Description"}]
                     [ui/TableHeaderCell {:content "Required"}]
                     (when editable?
                       [ui/TableHeaderCell {:content "Action"}])]]
                   [ui/TableBody
                    (for [[id env-variable] @env-variables]
                      ^{:key id}
                      [single-env-variable env-variable editable?])]]])
          (when editable?
            [:div {:style {:padding-top 10}}
             [plus ::events/add-env-variable]])]
         :label (@tr [:module-env-variables])
         :count (count @env-variables)]))))


(defn single-url
  [url-map editable?]
  (let [tr (subscribe [::i18n-subs/tr])
        {:keys [id ::spec/url-name ::spec/url]} url-map]
    [ui/TableRow {:key id}
     [ui/TableCell {:floated :left
                    :width   2}
      (if editable?
        [input id (str "url-name-" id) url-name
         "name of this url" ::events/update-url-name ::spec/url-name false]
        [:span url-name])]
     [ui/TableCell {:floated :left
                    :width   13}
      (if editable?
        [input id (str "url-url-" id) url
         "url - e.g. http://${hostname}:${tcp.8888}/?token=${jupyter-token}" ::events/update-url-url ::spec/url true]
        [:span url])]
     (when editable?
       [ui/TableCell {:floated :right
                      :width   1
                      :align   :right
                      :style   {}}
        [trash id ::events/remove-url]])]))


(defn urls-section []
  (let [tr      (subscribe [::i18n-subs/tr])
        urls    (subscribe [::subs/urls])
        module  (subscribe [::apps-subs/module])
        is-new? (subscribe [::apps-subs/is-new?])]
    (fn []
      (let [editable? (apps-utils/editable? @module @is-new?)]
        [uix/Accordion
         [:<>
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
                     (when editable?
                       [ui/TableHeaderCell {:content "Action"}])]]
                   [ui/TableBody
                    (for [[id url-map] @urls]
                      ^{:key id}
                      [single-url url-map editable?])]]])
          (when editable?
            [:div {:style {:padding-top 10}}
             [plus ::events/add-url]])]
         :label (@tr [:urls])
         :count (count @urls)
         :default-open false]))))


(defn single-output-parameter [param editable?]
  (let [tr (subscribe [::i18n-subs/tr])
        {:keys [id ::spec/output-parameter-name ::spec/output-parameter-description]} param]
    [ui/TableRow {:key id}
     [ui/TableCell {:floated :left
                    :width   2}
      (if editable?
        [input id (str "output-param-name-" id) output-parameter-name
         "output parameter name" ::events/update-output-parameter-name
         ::spec/output-parameter-name false]
        [:span output-parameter-name])]
     [ui/TableCell {:floated :left
                    :width   13}
      (if editable?
        [input id (str "output-param-description-" id) output-parameter-description
         "output parameter description" ::events/update-output-parameter-description
         ::spec/output-parameter-description true]
        [:span output-parameter-description])]
     (when editable?
       [ui/TableCell {:floated :right
                      :width   1
                      :align   :right}
        [trash id ::events/remove-output-parameter]])]))


(defn output-parameters-section []
  (let [tr                (subscribe [::i18n-subs/tr])
        output-parameters (subscribe [::subs/output-parameters])
        module            (subscribe [::apps-subs/module])
        is-new?           (subscribe [::apps-subs/is-new?])]
    (fn []
      (let [editable? (apps-utils/editable? @module @is-new?)]
        [uix/Accordion

         [:<>
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
                     (when editable?
                       [ui/TableHeaderCell {:content "Action"}])]]
                   [ui/TableBody
                    (for [[id param] @output-parameters]
                      ^{:key id}
                      [single-output-parameter param editable?])]]])
          (when editable?
            [:div {:style {:padding-top 10}}
             [plus ::events/add-output-parameter]])]
         :label (@tr [:module-output-parameters])
         :count (count @output-parameters)
         :default-open false]))))


(def data-type-options (atom [{:key "application/x-hdr", :value "application/x-hdr", :text "application/x-hdr"}
                              {:key "application/x-clk", :value "application/x-clk", :text "application/x-clk"}
                              {:key "text/plain", :value "text/plain", :text "text/plain"}]))


(defn add-data-type-options
  [option]
  (swap! data-type-options conj {:key option :value option :text option}))


(defn single-data-type [dt editable?]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [dt editable?]
      (let [{:keys [id ::spec/data-type]} dt]
        [ui/GridRow {:key id}
         [ui/GridColumn {:floated :left
                         :width   2}
          (if editable?
            [ui/Label
             [ui/Dropdown {:name           (str "data-type-" id)
                           :default-value  (or data-type "text/plain")
                           :allowAdditions true
                           :selection      true
                           :additionLabel  "Additional data type: "
                           :search         true
                           :options        @data-type-options
                           :on-add-item    #(add-data-type-options (-> % .-target .-value))
                           :on-change      (ui-callback/value
                                             #(do (dispatch [::main-events/changes-protection? true])
                                                  (dispatch [::events/update-data-type id %])
                                                  (dispatch [::apps-events/validate-form])))}]]
            [:span [:b data-type]])]
         (when editable?
           [ui/GridColumn {:floated :right
                           :width   1
                           :align   :right
                           :style   {}}
            [trash id ::events/remove-data-type]])]))))


(defn data-types-section []
  (let [tr         (subscribe [::i18n-subs/tr])
        data-types (subscribe [::subs/data-types])
        module     (subscribe [::apps-subs/module])
        is-new?    (subscribe [::apps-subs/is-new?])]
    (fn []
      (let [editable? (apps-utils/editable? @module @is-new?)]
        [uix/Accordion
         [:<>
          [:div (@tr [:data-type])
           [:span forms/nbsp (forms/help-popup (@tr [:module-data-type-help]))]]
          (if (empty? @data-types)
            [ui/Message
             (str/capitalize (str (@tr [:no-datasets]) "."))]
            [:div [ui/Grid {:style {:margin-top    5
                                    :margin-bottom 5}}
                   (for [[id dt] @data-types]
                     ^{:key id}
                     [single-data-type dt editable?])]])
          (when editable?
            [:div
             [plus ::events/add-data-type]])]]
        :label (@tr [:data-binding])
        :count (count @data-types)
        :default-open false))))


(defn generate-ports-args
  [ports]
  (let [ports-args
        (for [[id port] ports]
          (let [{:keys [::spec/published-port ::spec/target-port ::spec/protocol]} port]
            (str "-p " published-port ":" target-port (when
                                                        (and
                                                          (not= "tcp" protocol)
                                                          (not (empty? protocol)))
                                                        (str "/" protocol)))))]
    (str/join " " ports-args)))


(defn generate-mounts-args
  [mounts]
  (let [mounts-commands
        (for [[id {:keys [::spec/mount-type ::spec/source ::spec/target ::spec/read-only]}] mounts]
          (conj (str
                  "--mount type=" mount-type
                  ",src=" source
                  ",dst=" target
                  (when read-only ",readonly"))))]
    (str/join " " mounts-commands)))


(defn generate-image-arg
  [{:keys [::spec/registry ::spec/repository ::spec/image-name ::spec/tag]}]
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
  (let [tr       (subscribe [::i18n-subs/tr])
        mappings (subscribe [::subs/ports])
        mounts   (subscribe [::subs/mounts])
        image    (subscribe [::subs/image])]
    (fn []
      (let [mapping-args (generate-ports-args @mappings)
            mounts-args  (generate-mounts-args @mounts)
            image-arg    (generate-image-arg @image)
            command      (str "docker service create " mapping-args " " mounts-args " " image-arg)]
        [ui/Message {:info true}
         [ui/MessageHeader (@tr [:module-docker-command-message])]
         [:p {:style {:padding-top 8}
              :class "nuvla-command"} [:b "$ " command " "]
          [ui/Popup {:trigger  (r/as-element [ui/CopyToClipboard {:text command}
                                              [:a [ui/Icon {:name "clipboard outline"}]]])
                     :position "top center"}
           "copy to clipboard"]]
         [:p "Note: ensure you have a recent installation of docker."]]))))


(defn clear-module
  []
  (dispatch [::events/clear-module]))


(defn view-edit
  []
  (let [module-common (subscribe [::apps-subs/module-common])
        module        (subscribe [::apps-subs/module])
        is-new?       (subscribe [::apps-subs/is-new?])
        acl-visible?  (r/atom false)]
    (fn []
      (let [name      (get @module-common ::apps-spec/name)
            parent    (get @module-common ::apps-spec/parent-path)
            editable? (apps-utils/editable? @module @is-new?)]
        (dispatch [::apps-events/set-form-spec ::spec/module-component])
        (dispatch [::apps-events/set-module-subtype :component])
        [ui/Container {:fluid true}
         [:h2 [ui/Icon {:name "grid layout"}]
          parent (when (not-empty parent) "/") name
          [acl/AclButton {:acl      (get @module-common ::apps-spec/acl)
                          :on-click #(swap! acl-visible? not)}]]
         [apps-views-detail/control-bar]
         (when @acl-visible?
           [:<>
            [acl/AclWidget {:acl       (get @module-common ::apps-spec/acl)
                            :on-change #(do (dispatch [::apps-events/acl %])
                                            (dispatch [::main-events/changes-protection? true]))
                            :read-only (not editable?)}]
            [:div {:style {:padding-top 10}}]])
         [summary]
         [ports-section]
         [env-variables-section]
         [mounts-section]
         [urls-section]
         [output-parameters-section]
         [data-types-section]
         [test-command]
         [apps-views-detail/save-action]
         [apps-views-detail/add-modal]
         [apps-views-detail/save-modal]
         [apps-views-detail/logo-url-modal]
         [deployment-dialog-views/deploy-modal]]))))
