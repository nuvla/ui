(ns sixsq.nuvla.ui.infrastructures.views
  (:require
    [cljs.pprint :refer [cl-format]]
    [re-frame.core :refer [dispatch dispatch-sync subscribe]]
    [sixsq.nuvla.ui.acl.views :as acl]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.infrastructures-detail.views :as infra-detail]
    [sixsq.nuvla.ui.infrastructures.events :as events]
    [sixsq.nuvla.ui.infrastructures.spec :as spec]
    [sixsq.nuvla.ui.infrastructures.subs :as subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.validation :as utils-validation]
    [sixsq.nuvla.ui.utils.values :as values]
    [taoensso.timbre :as timbre]
    [taoensso.timbre :as log]))


(defn ControlBar []
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Menu {:borderless true}
     [ui/MenuMenu {:position "left"}
      [uix/MenuItemWithIcon
       {:name      (@tr [:add])
        :icon-name "plus"
        :position  "right"
        :on-click  #(do
                      (dispatch-sync [::events/reset-service-group])
                      (dispatch-sync [::events/reset-infra-service])
                      (dispatch [::events/open-add-service-modal]))}]]
     [main-components/RefreshMenu
      {:on-refresh #(dispatch [::events/get-infra-service-groups])}]]))


(def service-icons
  {:swarm :docker
   :s3    :aws})


(defn ServiceCard
  [{:keys [id name description path subtype logo-url] :as service}]
  [ui/Card (when (general-utils/can-edit? service)
             {:on-click #(dispatch [::history-events/navigate
                                    (str "infrastructures/" (general-utils/id->uuid id))])})
   (when logo-url
     [ui/Image {:src   logo-url
                :style {:width      "auto"
                        :height     "100px"
                        :object-fit "contain"}}])
   [ui/CardContent
    [ui/CardHeader {:style {:word-wrap "break-word"}}
     [ui/Icon {:name ((keyword subtype) service-icons)}]
     (or name id)]
    [ui/CardMeta {:style {:word-wrap "break-word"}} path]
    [ui/CardDescription {:style {:overflow "hidden" :max-height "100px"}} description]]])


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
         [ControlBar]
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


(defn service-swarm
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        is-new?        (subscribe [::subs/is-new?])
        service        (subscribe [::subs/infra-service])
        validate-form? (subscribe [::subs/validate-form?])
        on-change      (fn [name-kw value]
                         (dispatch [::events/update-infra-service name-kw value])
                         (dispatch [::events/validate-swarm-service-form]))]
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
           [uix/TableRowField (@tr [:endpoint]), :placeholder "https://swarm-example.io::2376",
            :default-value endpoint, :spec ::spec/endpoint, :editable? editable?, :required? true,
            :on-change (partial on-change :endpoint), :validate-form? @validate-form?]]]]))))


(defn service-minio
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
  {"swarm" {:validation-event ::events/validate-swarm-service-form
            :modal-content    service-swarm}
   "s3"    {:validation-event ::events/validate-minio-service-form
            :modal-content    service-minio}})


(defn save-callback
  [form-validation-event]
  (dispatch-sync [::events/set-validate-form? true])
  (dispatch-sync [form-validation-event])
  (let [form-valid? (get @re-frame.db/app-db ::spec/form-valid?)]
    (when form-valid?
      (do
        (dispatch [::events/set-validate-form? false])
        (dispatch [::events/edit-infra-service])))))


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

           [ui/ModalHeader header]

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

       [ui/ModalHeader [ui/Icon {:name "add"}] (@tr [:add])]

       [ui/ModalContent {:scrolling false}

        [:div
         [:p (@tr [:register-swarm-note])]
         [ui/CardGroup {:centered true
                        :style    {:margin-bottom "10px"}}

          [ui/Card
           {:on-click #(do
                         (dispatch [::events/set-validate-form? false])
                         (dispatch [::events/form-valid])
                         (dispatch [::events/close-add-service-modal])
                         (dispatch [::events/open-service-modal
                                    (assoc @service :subtype "swarm") true]))}

           [ui/CardContent {:text-align :center}
            [ui/Header "Swarm"]
            [ui/Icon {:name "docker"
                      :size :massive}]
            [ui/Header (@tr [:register])]]]]]

        #_[:div
         [:p (@tr [:register-s3-note])]
         [ui/CardGroup {:centered true}

          [ui/Card
           {:on-click #(do
                         (dispatch [::events/set-validate-form? false])
                         (dispatch [::events/form-valid])
                         (dispatch [::events/close-add-service-modal])
                         (dispatch [::events/open-service-modal
                                    (assoc @service :subtype "s3") true]))}
           [ui/CardContent {:text-align :center}
            [ui/Header "MinIO"]
            [ui/Image {:src  "/ui/images/minio.png"
                       :size :tiny}]
            [ui/Header (@tr [:register])]]]]]]])))


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
