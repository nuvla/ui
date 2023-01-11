(ns sixsq.nuvla.ui.apps-component.views
  (:require [cljs.spec.alpha :as s]
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
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.utils.form-fields :as forms]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn registry-url
  [image]
  (str/join ":" (-> image (str/split #":") drop-last)))


(defn docker-image-view
  [{:keys [::spec/image-name ::spec/registry ::spec/repository ::spec/tag] :as _image}]
  [:span
   #_:clj-kondo/ignore
   (when (not (empty? registry))
     [:span registry "/"])
   #_:clj-kondo/ignore
   (when (not (empty? repository))
     [:span repository "/"])
   [:span image-name]
   #_:clj-kondo/ignore
   (when (not (empty? tag))
     [:span ":" tag])])


(defn docker-image
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        module    (subscribe [::apps-subs/module])
        image     (subscribe [::subs/image])
        editable? (subscribe [::apps-subs/editable?])]
    (fn []
      (let [{:keys [::spec/image-name ::spec/registry ::spec/repository ::spec/tag]
             :or   {registry "" repository "" image-name "" tag ""}} @image
            label (@tr [:module-docker-image-label])]
        [ui/TableRow
         [ui/TableCell {:collapsing true} (if @editable? (apps-utils/mandatory-name label) label)]

         ; force react to regenerate the content of this cell with a random key
         ^{:key (:id @module "1")}
         [ui/TableCell
          (if @editable?
            [:div
             [apps-views-detail/input "docker-registry" "docker-registry" registry
              "docker.io" ::events/update-docker-registry ::spec/registry]
             [:span " "]
             [apps-views-detail/input "docker-repository-image-name" "docker-repository-image-name"
              (str/join "/" (remove nil? [repository image-name]))
              (@tr [:module-docker-image-placeholder])
              ::events/update-docker-image ::spec/image-name]
             [:span ":"]
             [apps-views-detail/input "docker-tag" "docker-tag" tag
              (@tr [:module-docker-tag-placeholder]) ::events/update-docker-tag ::spec/tag]]
            (docker-image-view @image))]]))))


(defn architectures
  []
  (let [architectures   (subscribe [::subs/architectures])
        form-valid?     (subscribe [::apps-subs/form-valid?])
        editable?       (subscribe [::apps-subs/editable?])
        local-validate? (r/atom false)
        label           "architectures"]
    (fn []
      (let [validate? (or @local-validate? (not @form-valid?))]
        ^{:key @architectures}
        [ui/TableRow
         [ui/TableCell {:collapsing true
                        :style      {:padding-bottom 8}}
          (if @editable? (apps-utils/mandatory-name label) label)]
         [ui/TableCell
          (if @editable?
            [ui/Dropdown {:name          "architectures"
                          :multiple      true
                          :selection     true
                          :default-value @architectures
                          :options       (map (fn [arch] {:key arch, :value arch, :text arch})
                                              ["386", "amd64", "amd64p32", "arm", "armbe", "arm64",
                                               "arm64/v8", "arm64be", "arm/v5", "arm/v6", "arm/v7",
                                               "ppc", "ppc64", "ppc64le", "mips", "mipsle",
                                               "mips64", "mips64le", "mips64p32", "mips64p32le",
                                               "s390", "s390x", "sparc", "sparc64"])
                          :error         (and validate?
                                              (not (s/valid? ::spec/architectures @architectures)))
                          :on-change     (ui-callback/value
                                           #(do
                                              (reset! local-validate? true)
                                              (dispatch [::events/architectures %])
                                              (dispatch [::main-events/changes-protection? true])
                                              (dispatch [::apps-events/validate-form])))}]
            [:span (str/join ", " @architectures)])]]))))


(defn Details []
  [apps-views-detail/Details
   {:extras
    [^{:key "summary-docker-image"}
     [docker-image]
     ^{:key "summary-architectures"}
     [architectures]]}])


(defn single-port
  [port]
  (let [tr        (subscribe [::i18n-subs/tr])
        editable? (subscribe [::apps-subs/editable?])
        {:keys [id
                ::spec/published-port
                ::spec/target-port
                ::spec/protocol]} port]
    [ui/TableRow

     [ui/TableCell
      (if @editable?
        [apps-views-detail/input id "published-port" published-port
         (@tr [:module-ports-published-port-placeholder]) ::events/update-port-published
         ::spec/published-port]
        (when (number? published-port) [:span [:b published-port] " "]))]

     [ui/TableCell
      (if @editable?
        [apps-views-detail/input id "target-port" target-port "dest. - e.g. 22 or 22-23"
         ::events/update-port-target ::spec/target-port]
        [:span [:b target-port]])]

     [ui/TableCell
      (if @editable?
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
        #_:clj-kondo/ignore
        (when (and (not (empty? protocol)) (not= "tcp" protocol))
          [:b protocol]))]

     (when @editable?
       [ui/TableCell
        [apps-views-detail/trash id ::events/remove-port]])]))


(defn ports-section []
  (let [tr        (subscribe [::i18n-subs/tr])
        ports     (subscribe [::subs/ports])
        editable? (subscribe [::apps-subs/editable?])]
    (fn []
      [uix/Accordion
       [:<>
        [:div (@tr [:module-publish-port]) " "
         [:span forms/nbsp (forms/help-popup (@tr [:module-ports-help]))]]
        (if (empty? @ports)
          [ui/Message
           (str/capitalize (str (@tr [:no-ports]) "."))]
          [:div [ui/Table {:style {:margin-top 10}}
                 [ui/TableHeader
                  [ui/TableRow
                   [ui/TableHeaderCell {:content "Destination (External)"}]
                   [ui/TableHeaderCell {:content "Source (Internal)"}]
                   [ui/TableHeaderCell {:content "Protocol"}]
                   (when @editable?
                     [ui/TableHeaderCell {:content "Action"}])]]
                 [ui/TableBody
                  (for [[id port] @ports]
                    ^{:key (str "port_" id)}
                    [single-port port])]]])
        (when @editable?
          [:div {:style {:padding-top 10}}
           [apps-views-detail/plus ::events/add-port]])]
       :label (@tr [:module-ports])
       :count (count @ports)
       :default-open false])))


(defn single-mount [mount]
  (let [editable? (subscribe [::apps-subs/editable?])
        {:keys [id
                ::spec/mount-type
                ::spec/mount-source
                ::spec/mount-target
                ::spec/mount-read-only]} mount]

    [ui/TableRow

     [ui/TableCell
      (if @editable?
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
      (if @editable?
        ;id name value placeholder update-event value-spec
        [apps-views-detail/input id "vol-source" mount-source "source"
         ::events/update-mount-source ::spec/mount-source false]
        [:span "src=" [:b mount-source]])]

     [ui/TableCell
      (if @editable?
        [apps-views-detail/input id "vol-dest" mount-target "target"
         ::events/update-mount-target ::spec/mount-target false]
        [:span "dst=" [:b mount-target]])]

     [ui/TableCell
      (if @editable?
        [ui/Checkbox {:name      "read-only"
                      :checked   (if (nil? mount-read-only) false mount-read-only)
                      :on-change (ui-callback/checked
                                   #(do (dispatch [::main-events/changes-protection? true])
                                        (dispatch [::events/update-mount-read-only? id %])
                                        (dispatch [::apps-events/validate-form])))
                      :align     :middle}]
        (when mount-read-only [:b "readonly"]))]

     (when @editable?
       [ui/TableCell
        [apps-views-detail/trash id ::events/remove-mount]])]))


(defn mounts-section []
  (let [tr        (subscribe [::i18n-subs/tr])
        mounts    (subscribe [::subs/mounts])
        editable? (subscribe [::apps-subs/editable?])]
    (fn []
      [uix/Accordion
       [:<>
        [:div "Container volumes (i.e. mounts) "
         [:span forms/nbsp (forms/help-popup (@tr [:module-mount-help]))]]
        (if (empty? @mounts)
          [ui/Message
           (str/capitalize (str (@tr [:no-mounts]) "."))]
          [:div [ui/Table {:style {:margin-top 10}}
                 [ui/TableHeader
                  [ui/TableRow
                   [ui/TableHeaderCell {:content "Type"}]
                   [ui/TableHeaderCell {:content "Source"}]
                   [ui/TableHeaderCell {:content "Target"}]
                   [ui/TableHeaderCell {:content "Read only?"}]
                   (when @editable?
                     [ui/TableHeaderCell {:content "Action"}])]]
                 [ui/TableBody
                  (for [[id mount] @mounts]
                    ^{:key (str "mount_" id)}
                    [single-mount mount])]]])
        (when @editable?
          [:div {:style {:padding-top 10}}
           [apps-views-detail/plus ::events/add-mount]])]
       :label (@tr [:module-mounts])
       :count (count @mounts)
       :default-open false])))


(defn generate-ports-args
  [ports]
  (let [ports-args
        (for [[_id port] ports]
          (let [{:keys [::spec/published-port ::spec/target-port ::spec/protocol]} port]
            (str "-p " published-port ":" target-port (when
                                                        (and
                                                          (not= "tcp" protocol)
                                                          #_:clj-kondo/ignore
                                                          (not (empty? protocol)))
                                                        (str "/" protocol)))))]
    (str/join " " ports-args)))


(defn generate-mounts-args
  [mounts]
  (let [mounts-commands
        (for [[_id {:keys [::spec/mount-type ::spec/mount-source
                           ::spec/mount-target ::spec/mount-read-only]}] mounts]
          (str
            "--mount type=" mount-type
            ",src=" mount-source
            ",dst=" mount-target
            (when mount-read-only ",readonly")))]
    (str/join " " mounts-commands)))


(defn generate-image-arg
  [{:keys [::spec/registry ::spec/repository ::spec/image-name ::spec/tag]}]
  (str
    #_:clj-kondo/ignore
    (when (not (empty? registry))
      (str registry "/"))
    #_:clj-kondo/ignore
    (when (not (empty? repository))
      (str repository "/"))
    image-name
    #_:clj-kondo/ignore
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
                                              [uix/LinkIcon {:name "clipboard outline"}]])
                     :position "top center"}
           "copy to clipboard"]]
         [:p "Note: ensure you have a recent installation of docker."]]))))


(defn clear-module
  []
  (dispatch [::events/clear-module]))


(defn view-edit
  []
  (let [tr            (subscribe [::i18n-subs/tr])
        module-common (subscribe [::apps-subs/module-common])
        editable?     (subscribe [::apps-subs/editable?])
        stripe        (subscribe [::main-subs/stripe])]
    (fn []
      (let [name   (get @module-common ::apps-spec/name)
            parent (get @module-common ::apps-spec/parent-path)]
        (dispatch [::apps-events/set-form-spec ::spec/module-component])
        [ui/Container {:fluid true}
         [acl/AclButton {:default-value (get @module-common ::apps-spec/acl)
                         :on-change     #(do (dispatch [::apps-events/acl %])
                                             (dispatch [::main-events/changes-protection? true]))
                         :read-only     (not @editable?)}]
         [uix/PageHeader "grid layout" (str parent (when (not-empty parent) "/") name) :inline true]
         [apps-views-detail/MenuBar]
         [Details]
         [apps-views-detail/registries-section]
         (when @stripe
           [uix/Accordion
            [apps-views-detail/price-section]
            :label (str/capitalize (@tr [:pricing]))
            :default-open false])
         [uix/Accordion
          [apps-views-detail/LicenseSection]
          :label (@tr [:eula-full])
          :default-open false]
         [ports-section]
         [apps-views-detail/EnvVariablesSection]
         [mounts-section]
         [apps-views-detail/UrlsSection]
         [apps-views-detail/OutputParametersSection]
         [apps-views-detail/DataTypesSection]
         [test-command]
         [apps-views-detail/save-modal]
         [apps-views-detail/logo-url-modal]
         [deployment-dialog-views/deploy-modal]]))))
