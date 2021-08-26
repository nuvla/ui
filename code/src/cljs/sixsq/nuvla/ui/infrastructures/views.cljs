(ns sixsq.nuvla.ui.infrastructures.views
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch dispatch-sync subscribe]]
    [re-frame.db]
    [reagent.core :as r]
    [sixsq.nuvla.ui.acl.views :as acl]
    [sixsq.nuvla.ui.credentials.views :as cred-views]
    [sixsq.nuvla.ui.edge-detail.views :as edge-detail]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.infrastructures-detail.views :as infra-detail]
    [sixsq.nuvla.ui.infrastructures.events :as events]
    [sixsq.nuvla.ui.infrastructures.spec :as spec]
    [sixsq.nuvla.ui.infrastructures.subs :as subs]
    [sixsq.nuvla.ui.infrastructures.utils :as utils]
    [sixsq.nuvla.ui.intercom.events :as intercom-events]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.form-fields :as ff]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.validation :as utils-validation]
    [sixsq.nuvla.ui.utils.values :as values]
    [taoensso.timbre :as timbre]))


(defn MenuBar []
  (let [tr (subscribe [::i18n-subs/tr])]
    [main-components/StickyBar
     [ui/Menu {:borderless true}
      [ui/MenuMenu {:position "left"}
       [uix/MenuItem
        {:name     (@tr [:add])
         :icon     "plus"
         :position "right"
         :on-click #(do
                      (dispatch-sync [::events/reset-service-group])
                      (dispatch-sync [::events/reset-infra-service])
                      (dispatch [::events/open-add-service-modal]))}]]
      [main-components/RefreshMenu
       {:on-refresh #(dispatch [::events/get-infra-service-groups])}]]]))


(def service-icons
  {:swarm      "docker"
   :s3         "/ui/images/s3.png"
   :kubernetes "/ui/images/kubernetes.svg"
   :registry   "database"})


(defn ServiceCard
  [{:keys [id name description path subtype logo-url swarm-enabled online] :as _service}]
  (let [icon-or-image (get service-icons (keyword subtype) "question circle")
        status        (cond
                        (true? online) true
                        (false? online) false
                        :else nil)
        href          (str "infrastructures/" (general-utils/id->uuid id))]
    [uix/Card
     {:on-click    #(dispatch [::history-events/navigate href])
      :href        href
      :image       logo-url
      :header      [:<>
                    [:div {:style {:float "right"}}
                     [edge-detail/OnlineStatusIcon status]]
                    (if (str/starts-with? icon-or-image "/")
                      [ui/Image {:src   icon-or-image
                                 :style {:overflow       "hidden"
                                         :display        "inline-block"
                                         :height         28
                                         :margin-right   4
                                         :padding-bottom 7
                                         }}]
                      [ui/Icon {:name icon-or-image}])
                    (or name id)]
      :meta        path
      :description description
      :content     (when (true? swarm-enabled)
                     [ui/Label {:image    true
                                :color    "blue"
                                :circular true
                                :basic    true
                                :style    {:left   "0"
                                           :margin "0.7em 0 0 0"}}
                      [ui/Image {:bordered true}
                       [ui/Icon {:name icon-or-image}]]
                      "Swarm enabled"])}]))


(defn ServiceGroupCard
  [id name]
  (let [services (subscribe [::subs/services-in-group id])]
    [ui/Card
     [ui/Label {:corner   true
                :style    {:z-index 0
                           :cursor  :pointer}
                :size     "mini"
                :on-click #(do
                             (dispatch-sync [::events/set-service-group id services])
                             (dispatch-sync [::events/reset-infra-service])
                             (dispatch-sync [::events/update-infra-service :parent id])
                             (dispatch [::events/open-add-service-modal]))}
      [ui/Icon {:name "plus", :style {:cursor :pointer}}]
      ; use content to work around bug in icon in label for cursor
      ]
     [ui/CardContent
      [ui/CardDescription
       [values/as-link id :label name]]
      [ui/CardHeader {:style {:word-wrap "break-word"}}]
      (if @services
        (for [{service-id :id :as service} @services]
          ^{:key service-id}
          [ServiceCard service])
        [ui/CardMeta "Empty infrastructure group"])]]))


(defn ServiceGroups
  [isgs]
  [ui/CardGroup {:centered true}
   (for [{:keys [id name]} (:resources isgs)]
     ^{:key id}
     [ServiceGroupCard id name])])


(defn InfraServices
  []
  (let [tr                (subscribe [::i18n-subs/tr])
        isgs              (subscribe [::subs/infra-service-groups])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])]
    (fn []
      (let [infra-group-count (get @isgs :count 0)
            total-pages       (general-utils/total-pages infra-group-count @elements-per-page)]
        [:<>
         [uix/PageHeader "cloud" (@tr [:infra-services])]
         [MenuBar]
         (when (pos-int? infra-group-count)
           [:<>
            [ServiceGroups @isgs]
            [uix/Pagination
             {:totalitems   infra-group-count
              :totalPages   total-pages
              :activePage   @page
              :onPageChange (ui-callback/callback
                              :activePage #(dispatch [::events/set-page %]))}]])]))))


(defn in?
  "true if coll contains elm"
  [coll elm]
  (some #(= elm %) coll))


(defn cloud-params-default-by-cred-id
  [db cred-id]
  (get utils/cloud-params-defaults (utils/mgmt-cred-subtype-by-id db cred-id)))

(defn row-csp-credential-selector
  [subtypes additional-filter _disabled? _value-spec _on-change]
  (let [tr              (subscribe [::i18n-subs/tr])
        mgmt-creds      (subscribe [::subs/management-credentials-available])
        service         (subscribe [::subs/infra-service])
        local-validate? (r/atom false)
        validate-form?  (subscribe [::subs/validate-form?])]
    (dispatch [::events/fetch-coe-management-credentials-available subtypes additional-filter])
    (fn [_subtypes _additional-filter disabled? value-spec on-change]
      (let [value           (:management-credential @service)
            validate?       (or @local-validate? @validate-form?)
            valid?          (s/valid? value-spec value)
            local-on-change (fn [cred-id]
                              (if (str/blank? cred-id)
                                (do (dispatch-sync [::events/clear-infra-service-cloud-params])
                                    (on-change cred-id))
                                (do
                                  (dispatch-sync [::events/clear-infra-service-cloud-params])
                                  (dispatch-sync [::events/update-infra-service-map
                                                  (cloud-params-default-by-cred-id @re-frame.db/app-db cred-id)])
                                  (on-change cred-id))))]
        [ui/TableRow
         [ui/TableCell {:collapsing false} (@tr [:credentials-cloud-short])]
         [ui/TableCell {:error (and validate? (not valid?))}
          (if (pos-int? (count @mgmt-creds))
            ^{:key value}
            [ui/Dropdown {:clearable   true
                          :selection   true
                          :disabled    disabled?
                          :fluid       true
                          :value       value
                          :placeholder (@tr [:credentials-cloud-select])
                          :on-change   (ui-callback/callback
                                         :value #(do
                                                   (reset! local-validate? true)
                                                   (local-on-change %)))
                          :options     (map (fn [{id :id, infra-name :name}]
                                              {:key id, :value id, :text infra-name})
                                            @mgmt-creds)}]
            [ui/Message {:content (@tr [:credentials-cloud-not-found])}])]]))))


(defn ssh-keys-selector
  [_disabled?]
  (let [ssh-keys         (subscribe [::subs/ssh-keys])
        ssh-keys-options (subscribe [::subs/ssh-keys-options])
        form-valid?      (subscribe [::subs/form-valid?])
        local-validate?  (r/atom false)]
    (dispatch [::events/get-ssh-keys-infra])
    (fn [disabled?]
      (let [validate? (or @local-validate? (not @form-valid?))]
        ^{:key @ssh-keys}
        [ui/Table style/definition
         [ui/TableBody
          [ui/TableRow
           [ui/TableCell {:collapsing true
                          :style      {:padding-bottom 8}} "ssh keys"]
           [ui/TableCell
            [ui/Dropdown
             {:multiple      true
              :clearable     false
              :selection     true
              :disabled      disabled?
              :default-value @ssh-keys
              :options       @ssh-keys-options
              :error         (and validate?
                                  (not (s/valid? ::spec/ssh-keys @ssh-keys)))
              :on-change     (ui-callback/value
                               #(do
                                  (reset! local-validate? true)
                                  (dispatch [::events/ssh-keys %])
                                  (dispatch [::events/validate-coe-service-form])))}]
            [:span (str/join ", " @ssh-keys)]]]]]))))


(defn cloud-help-popup
  [text cred-subtype]
  [:span ff/nbsp
   (ff/help-popup (r/as-element
                    [:span [:p text]
                     (when cred-subtype
                       [:a {:href (utils/cloud-param-default-value cred-subtype :cloud-doc-link)
                            :target "_blank"} "See this link."])])
                  :on (if cred-subtype "focus" "hover"))])


(defn service-coe
  []
  (let [tr                   (subscribe [::i18n-subs/tr])
        is-new?              (subscribe [::subs/is-new?])
        service              (subscribe [::subs/infra-service])
        validate-form?       (subscribe [::subs/validate-form?])
        subtype              (:subtype @service "swarm")
        default-multiplicity 1
        multiplicity         (r/atom default-multiplicity)
        on-change            (fn [name-kw value]
                               (let [value (if (some #{name-kw} [:multiplicity :cloud-vm-disk-size]) (int value) value)]
                                 (dispatch [::events/update-infra-service name-kw value])
                                 (dispatch [::events/validate-coe-service-form])))]
    (fn []
      (let [editable?            (general-utils/editable? @service @is-new?)
            mgmt-cred-set?       (subscribe [::subs/mgmt-creds-set?])
            mgmt-cred-subtype    (subscribe [::subs/mgmt-cred-subtype])
            {:keys [name description endpoint]} @service
            cloud-project        (or (:cloud-project @service) (utils/cloud-param-default-value @mgmt-cred-subtype :cloud-project))
            cloud-region         (or (:cloud-region @service) (utils/cloud-param-default-value @mgmt-cred-subtype :cloud-region))
            cloud-vm-size        (or (:cloud-vm-size @service) (utils/cloud-param-default-value @mgmt-cred-subtype :cloud-vm-size))
            cloud-vm-image       (or (:cloud-vm-image @service) (utils/cloud-param-default-value @mgmt-cred-subtype :cloud-vm-image))
            cloud-security-group (or (:cloud-security-group @service) (utils/cloud-param-default-value @mgmt-cred-subtype :cloud-security-group))
            cloud-vm-disk-size   (if-not @mgmt-cred-set? "" (utils/calc-disk-size (:cloud-vm-disk-size @service) (utils/cloud-param-default-value @mgmt-cred-subtype :cloud-vm-disk-size)))]
        [:<>

         [acl/AclButton {:default-value (:acl @service)
                         :read-only     (not editable?)
                         :on-change     #(dispatch [::events/update-infra-service :acl %])}]

         [ui/Table style/definition
          [ui/TableBody
           [uix/TableRowField (@tr [:name]), :editable? editable?, :required? true,
            :default-value name, :spec ::spec/name, :on-change (partial on-change :name),
            :validate-form? @validate-form?]
           [uix/TableRowField (@tr [:description]), :editable? editable?, :required? true,
            :default-value description, :spec ::spec/description,
            :on-change (partial on-change :description), :validate-form? @validate-form?]]]

         [ui/Divider]

         [:div {:style {:color "grey" :font-style "oblique"}}
          (general-utils/format (@tr [:infra-service-give-endpoint-fmt]) (str/capitalize subtype))]

         [ui/Table style/definition
          [ui/TableBody
           [uix/TableRowField (@tr [:endpoint]), :placeholder (str "https://" subtype "-example.io:2376"),
            :default-value endpoint, :spec ::spec/endpoint, :editable? (str/blank? (:management-credential @service)),
            :required? false, :on-change (partial on-change :endpoint), :validate-form? @validate-form?]]]

         [ui/Divider {:horizontal true :as "h4"} "or"]

         [:div {:style {:color "grey" :font-style "oblique"}}
          (general-utils/format (@tr [:coe-provision-fmt]) (str/capitalize subtype))]

         [ui/Table style/definition
          [ui/TableBody
           [row-csp-credential-selector cred-views/infrastructure-service-csp-subtypes nil
            (boolean (:endpoint @service)) (if (:endpoint @service) any? ::spec/management-credential)
            (partial on-change :management-credential)]]]
         [ui/Container {:style {:margin "5px" :display "inline-block"}}
          [ui/Input {:label       (@tr [:coe-cluster-size])
                     :placeholder default-multiplicity
                     :disabled    (not @mgmt-cred-set?)
                     :value       @multiplicity
                     :size        "mini"
                     :type        "number"
                     :on-change   (ui-callback/input-callback
                                    #(do
                                       (cond
                                         (number? (general-utils/str->int %)) (reset! multiplicity (general-utils/str->int %))
                                         (empty? %) (reset! multiplicity 1))
                                       (on-change :multiplicity @multiplicity)))
                     :step        1
                     :min         1}]]

         [:span
          [ui/Checkbox {:key             "coe-manager-install"
                        :label           (if (= subtype "swarm")
                                           (@tr [:coe-install-manager-portainer])
                                           (@tr [:coe-install-manager-rancher]))
                        :disabled        (not @mgmt-cred-set?)
                        :default-checked false
                        :style           {:margin "1em"}
                        :on-change       (ui-callback/checked
                                           #(on-change :coe-manager-install %))}]
          (if (= subtype "swarm")
            [:a {:href "https://portainer.io" :target "_blank"} "https://portainer.io"]
            [:a {:href "https://rancher.io" :target "_blank"} "https://rancher.io"])]

         ^{:key "ssh-keys-selector"}
         [ssh-keys-selector (not @mgmt-cred-set?)]

         [ui/Table style/definition
          [ui/TableBody

           [uix/TableRowField [:div "VM Size" (cloud-help-popup "Cloud specific VM size definition." @mgmt-cred-subtype)],
            :placeholder "", :editable? @mgmt-cred-set?, :required? false,
            :default-value cloud-vm-size, :spec ::spec/cloud-vm-size, :on-change (partial on-change :cloud-vm-size),
            :validate-form? @validate-form?]

           (when (or (nil? @mgmt-cred-subtype) (get-in utils/cloud-params-defaults [@mgmt-cred-subtype :cloud-vm-disk-size]))
             [uix/TableRowField [:div "VM Disk Size (GB)" (cloud-help-popup "Cloud specific VM disk size definition." @mgmt-cred-subtype)],
              :placeholder "", :editable? @mgmt-cred-set?, :required? false, :default-value cloud-vm-disk-size,
              :spec ::spec/cloud-vm-disk-size, :on-change (partial on-change :cloud-vm-disk-size), :validate-form? @validate-form?])

           (when (= utils/infra-service-subtype-google @mgmt-cred-subtype)
             [uix/TableRowField [:div "Project ID" (cloud-help-popup "GCP Project ID." @mgmt-cred-subtype)],
              :placeholder "", :editable? @mgmt-cred-set?, :required? true, :default-value cloud-project,
              :spec ::spec/cloud-project, :on-change (partial on-change :cloud-project), :validate-form? @validate-form?])

           [uix/TableRowField [:div "Region" (cloud-help-popup "Cloud specific region." @mgmt-cred-subtype)],
            :placeholder "", :editable? @mgmt-cred-set?, :required? false,
            :default-value cloud-region, :spec ::spec/cloud-region, :on-change (partial on-change :cloud-region),
            :validate-form? @validate-form?]

           [uix/TableRowField [:div "Image" (cloud-help-popup "Cloud specific image." @mgmt-cred-subtype)],
            :placeholder "", :editable? @mgmt-cred-set?, :required? false, :default-value cloud-vm-image,
            :spec ::spec/cloud-vm-image, :on-change (partial on-change :cloud-vm-image), :validate-form? @validate-form?]

           (when (= utils/infra-service-subtype-exoscale @mgmt-cred-subtype)
             [uix/TableRowField [:div "Security Group" (cloud-help-popup "Cloud specific security group." @mgmt-cred-subtype)],
              :editable? @mgmt-cred-set?, :required? true, :placeholder "", :default-value cloud-security-group,
              :spec ::spec/cloud-security-group, :on-change (partial on-change :cloud-security-group),
              :validate-form? @validate-form?])]]]))))


(defn service-registry
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        is-new?        (subscribe [::subs/is-new?])
        service        (subscribe [::subs/infra-service])
        validate-form? (subscribe [::subs/validate-form?])
        on-change      (fn [name-kw value]
                         (dispatch [::events/update-infra-service name-kw value])
                         (dispatch [::events/validate-registry-service-form]))]
    (fn []
      (let [editable? (general-utils/editable? @service @is-new?)
            {:keys [name description endpoint]} @service]
        [:<>

         [acl/AclButton {:default-value (:acl @service)
                         :read-only     (not editable?)
                         :on-change     #(dispatch [::events/update-infra-service :acl %])}]

         [ui/Table style/definition
          [ui/TableBody
           [uix/TableRowField (@tr [:name]), :editable? editable?, :required? true,
            :default-value name, :spec ::spec/name, :on-change (partial on-change :name),
            :validate-form? @validate-form?]
           [uix/TableRowField (@tr [:description]), :editable? editable?, :required? true,
            :default-value description, :spec ::spec/description,
            :on-change (partial on-change :description), :validate-form? @validate-form?]
           [uix/TableRowField (@tr [:endpoint]), :placeholder "https://registry.hub.docker.com",
            :default-value endpoint, :spec ::spec/endpoint, :editable? editable?, :required? true,
            :on-change (partial on-change :endpoint), :validate-form? @validate-form?]]]]))))


(defn service-object-store
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        is-new?        (subscribe [::subs/is-new?])
        service        (subscribe [::subs/infra-service])
        validate-form? (subscribe [::subs/validate-form?])
        on-change      (fn [name-kw value]
                         (dispatch [::events/update-infra-service name-kw value])
                         (dispatch [::events/validate-minio-service-form]))]
    (fn []
      (let [editable? (general-utils/editable? @service @is-new?)
            {:keys [name description endpoint]} @service]
        [:<>
         [acl/AclButton {:default-value (:acl @service)
                         :read-only     (not editable?)
                         :on-change     #(dispatch [::events/update-infra-service :acl %])}]

         [ui/Table style/definition
          [ui/TableBody
           [uix/TableRowField (@tr [:name]), :editable? editable?, :required? true,
            :default-value name, :spec ::spec/name, :on-change (partial on-change :name),
            :validate-form? @validate-form?]
           [uix/TableRowField (@tr [:description]), :editable? editable?, :required? true,
            :default-value description, :spec ::spec/description, :validate-form? @validate-form?,
            :on-change (partial on-change :description)]
           [uix/TableRowField (@tr [:endpoint]), :placeholder "http://minio-example.io:9000",
            :default-value endpoint, :spec ::spec/endpoint, :editable? editable?, :required? true,
            :on-change (partial on-change :endpoint), :validate-form? @validate-form?]]]]))))


(def infrastructure-service-validation-map
  {"swarm"      {:validation-event ::events/validate-coe-service-form
                 :modal-content    service-coe}
   "s3"         {:validation-event ::events/validate-minio-service-form
                 :modal-content    service-object-store}
   "kubernetes" {:validation-event ::events/validate-coe-service-form
                 :modal-content    service-coe}
   "registry"   {:validation-event ::events/validate-registry-service-form
                 :modal-content    service-registry}})


(defn save-callback
  [form-validation-event]
  (dispatch-sync [::events/set-validate-form? true])
  (dispatch-sync [form-validation-event])
  (let [form-valid? (get @re-frame.db/app-db ::spec/form-valid?)]
    (when form-valid?
      (dispatch [::events/set-validate-form? false])
      (dispatch [::events/edit-infra-service])
      (dispatch [::intercom-events/set-event "Last create Infrastructure Service" (time/timestamp)]))))


(defn ServiceModal
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        visible?    (subscribe [::subs/service-modal-visible?])
        form-valid? (subscribe [::subs/form-valid?])
        service     (subscribe [::subs/infra-service])
        is-new?     (subscribe [::subs/is-new?])]
    (fn []
      (let [subtype          (:subtype @service "")
            name             (:name @service subtype)
            header           (str (if (true? @is-new?) (@tr [:new]) (@tr [:update])) " " name)
            validation-item  (get infrastructure-service-validation-map subtype)
            validation-event (:validation-event validation-item)
            modal-content    (:modal-content validation-item)]

        (if (empty? subtype)
          [:div]
          [ui/Modal {:open       @visible?
                     :close-icon true
                     :on-close   #(dispatch [::events/close-service-modal])}

           [uix/ModalHeader {:header header}]

           [ui/ModalContent {:scrolling false}
            [utils-validation/validation-error-message ::subs/form-valid?]
            [modal-content]]
           [ui/ModalActions
            [uix/Button {:text     (if (true? @is-new?) (@tr [:create]) (@tr [:update]))
                         :positive true
                         :disabled (when-not @form-valid? true)
                         :active   true
                         :on-click #(save-callback validation-event)}]]])))))


(defn AddServiceModal
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        visible? (subscribe [::subs/add-service-modal-visible?])
        service  (subscribe [::subs/infra-service])]
    (fn []
      [ui/Modal {:open       @visible?
                 :close-icon true
                 :on-close   #(dispatch [::events/close-add-service-modal])}

       [uix/ModalHeader {:header (@tr [:add]) :icon "add"}]

       [ui/ModalContent {:scrolling false}
        [:div {:style {:padding-bottom 20}} (@tr [:register-swarm-note])]
        [ui/CardGroup {:centered true}

         [ui/Card
          {:on-click #(do
                        (dispatch [::events/set-validate-form? false])
                        (dispatch [::events/form-valid])
                        (dispatch [::events/close-add-service-modal])
                        (dispatch [::events/open-service-modal
                                   (assoc @service :subtype "swarm") true]))}

          [ui/CardContent {:text-align :center}
           [ui/Header "Docker Swarm"]
           [ui/Icon {:name "docker"
                     :size "massive"}]]]

         [ui/Card
          {:on-click #(do
                        (dispatch [::events/set-validate-form? false])
                        (dispatch [::events/form-valid])
                        (dispatch [::events/close-add-service-modal])
                        (dispatch [::events/open-service-modal
                                   (assoc @service :subtype "kubernetes") true]))}
          [ui/CardContent {:text-align :center}
           [ui/Header "Kubernetes"]
           [ui/Image {:src   "/ui/images/kubernetes.svg"
                      :style {:max-width 112}}]]]

         [ui/Card
          {:on-click #(do
                        (dispatch [::events/set-validate-form? false])
                        (dispatch [::events/form-valid])
                        (dispatch [::events/close-add-service-modal])
                        (dispatch [::events/open-service-modal
                                   (assoc @service :subtype "registry") true]))}
          [ui/CardContent {:text-align :center}
           [ui/Header "Docker Registry"]
           [ui/IconGroup {:size "massive"}
            [ui/Icon {:name "docker"}]
            [ui/Icon {:name "database", :corner "bottom right"}]]]]

         [ui/Card
          {:on-click #(do
                        (dispatch [::events/set-validate-form? false])
                        (dispatch [::events/form-valid])
                        (dispatch [::events/close-add-service-modal])
                        (dispatch [::events/open-service-modal
                                   (assoc @service :subtype "s3") true]))}
          [ui/CardContent {:text-align :center}
           [ui/Header "Object Store"]
           [ui/Image {:src   "/ui/images/s3.png"
                      :style {:max-width 112}}]]]]]])))


(defmethod panel/render :infrastructures
  [path]
  (timbre/set-level! :info)
  (dispatch [::events/get-infra-service-groups])
  (let [[_ uuid] path
        n        (count path)
        root     [:<>
                  [InfraServices]
                  [ServiceModal]
                  [AddServiceModal]]
        children (case n
                   1 root
                   2 [infra-detail/InfrastructureDetails uuid]
                   root)]
    [ui/Segment style/basic children]))
