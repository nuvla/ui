(ns sixsq.nuvla.ui.pages.clouds.views
  (:require [re-frame.core :refer [dispatch dispatch-sync subscribe]]
            [re-frame.db]
            [sixsq.nuvla.ui.common-components.acl.views :as acl]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.intercom.events :as intercom-events]
            [sixsq.nuvla.ui.common-components.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.pages.clouds-detail.views :as clouds-detail]
            [sixsq.nuvla.ui.pages.clouds.events :as events]
            [sixsq.nuvla.ui.pages.clouds.spec :as spec]
            [sixsq.nuvla.ui.pages.clouds.subs :as subs]
            [sixsq.nuvla.ui.pages.edges-detail.views :as edges-detail]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.utils :refer [name->href]]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.validation :as utils-validation]
            [sixsq.nuvla.ui.utils.values :as values]
            [sixsq.nuvla.ui.utils.view-components :refer [OnlineStatusIcon]]
            [taoensso.timbre :as timbre]))

(defn MenuBar []
  (let [tr (subscribe [::i18n-subs/tr])]
    [components/StickyBar
     [ui/Menu {:borderless true}
      [ui/MenuMenu {:position "left"}
       [uix/MenuItem
        {:name     (@tr [:add])
         :icon     icons/i-plus-large
         :position "right"
         :on-click #(do
                      (dispatch-sync [::events/reset-service-group])
                      (dispatch-sync [::events/reset-infra-service])
                      (dispatch [::events/open-add-service-modal]))}]]
      [components/RefreshMenu
       {:on-refresh #(dispatch [::events/get-infra-service-groups])}]]]))

(defn ServiceCard
  [{:keys [id name description path subtype logo-url state]
    :as   infra-service}]
  (let [href (name->href routes/clouds-details {:uuid (general-utils/id->uuid id)})]
    [uix/Card
     {:href        href
      :image       logo-url
      :header      [:<>
                    [:div {:style {:float "right"}}
                     [OnlineStatusIcon state]]
                    [edges-detail/ServiceIcon subtype]
                    (or name id)]
      :meta        path
      :description description
      :content     [clouds-detail/CompatibilityLabel infra-service]}]))

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
      [icons/InfoIconFull {:style {:cursor :pointer}}]
      ; use content to work around bug in icon in label for cursor
      ]
     [ui/CardContent
      [ui/CardDescription
       [values/AsLink id :label name]]
      [ui/CardHeader {:style {:word-wrap "break-word"}}]
      (if @services
        (for [{service-id :id :as service} @services]
          ^{:key service-id}
          [ServiceCard service])
        [ui/CardMeta "Empty cloud group"])]]))

(defn ServiceGroups
  [isgs]
  [ui/CardGroup {:centered true}
   (for [{:keys [id name]} (:resources isgs)]
     ^{:key id}
     [ServiceGroupCard id name])])

(defn InfraServices
  []
  (let [isgs (subscribe [::subs/infra-service-groups])]
    (fn []
      (let [infra-group-count (get @isgs :count 0)]
        [:<>
         [MenuBar]
         (when (pos-int? infra-group-count)
           [:<>
            [ServiceGroups @isgs]
            [pagination-plugin/Pagination
             {:db-path      [::spec/pagination]
              :total-items  infra-group-count
              :change-event [::events/get-infra-service-groups]}]])]))))

(defn service-coe
  []
  (let [tr                   (subscribe [::i18n-subs/tr])
        is-new?              (subscribe [::subs/is-new?])
        service              (subscribe [::subs/infra-service])
        validate-form?       (subscribe [::subs/validate-form?])
        subtype              (:subtype @service "swarm")
        on-change            (fn [name-kw value]
                               (dispatch [::events/update-infra-service name-kw value])
                               (dispatch [::events/validate-coe-service-form]))]
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
           [uix/TableRowField (@tr [:endpoint]), :placeholder (str "https://" subtype "-example.io:2376"),
            :default-value endpoint, :spec ::spec/endpoint, :editable? editable?,
            :required? true, :on-change (partial on-change :endpoint), :validate-form? @validate-form?]]]]))))

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
            [uix/Button {:text     (if @is-new? (@tr [:create]) (@tr [:update]))
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

       [uix/ModalHeader {:header (@tr [:add]) :icon icons/i-plus-full}]

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
           [icons/DockerIcon {:size "massive"}]]]

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
            [icons/DockerIcon]
            [icons/DbIconFull {:corner "bottom right"}]]]]

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

(defn Infrastructures
  []
  (dispatch [::events/get-infra-service-groups])
  [components/LoadingPage {}
   [:<>
    [InfraServices]
    [ServiceModal]
    [AddServiceModal]]])

(defn clouds-view
  [{path :path}]
  (timbre/set-min-level! :info)
  (let [[_ uuid] path
        n        (count path)
        root     [Infrastructures]
        children (case n
                   1 root
                   2 [clouds-detail/InfrastructureDetails uuid]
                   root)]
    [ui/Segment style/basic children]))
