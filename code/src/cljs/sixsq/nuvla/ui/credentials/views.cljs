(ns sixsq.nuvla.ui.credentials.views
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch dispatch-sync subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.acl.views :as acl]
    [sixsq.nuvla.ui.credentials.events :as events]
    [sixsq.nuvla.ui.credentials.spec :as spec]
    [sixsq.nuvla.ui.credentials.subs :as subs]
    [sixsq.nuvla.ui.history.views :as history]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.accordion :as utils-accordion]
    [sixsq.nuvla.ui.utils.form-fields :as ff]
    [sixsq.nuvla.ui.utils.general :as utils-general]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.validation :as utils-validation]
    [taoensso.timbre :as timbre]
    [taoensso.timbre :as log]))


(defn in?
  "true if coll contains elm"
  [coll elm]
  (some #(= elm %) coll))


(defn row-with-label
  [key name-kw value editable? mandatory? value-spec type validation-event]
  (let [tr              (subscribe [::i18n-subs/tr])
        active-input    (subscribe [::subs/active-input])
        local-validate? (r/atom false)
        validate-form?  (subscribe [::subs/validate-form?])]
    (fn [key name-kw value editable? mandatory? value-spec type validation-event]
      (let [name-str      (name name-kw)
            name-label    (if (and editable? mandatory?) (utils-general/mandatory-name name-str) name-str)
            input-active? (= name-str @active-input)
            validate?     (or @local-validate? @validate-form?)
            valid?        (s/valid? value-spec value)]
        [ui/TableRow
         [ui/TableCell {:collapsing true}
          name-label]
         [ui/TableCell
          (if editable?
            ^{:key key}
            (if (in? [:input :password] type)
              [ui/Input {:default-value value
                         :placeholder   (@tr [name-kw])
                         :disabled      (not editable?)
                         :error         (and validate? (not valid?))
                         :fluid         true
                         :type          (if (= type :input) :text type)
                         :icon          (when input-active? :pencil)
                         :onMouseEnter  #(dispatch [::events/active-input name-str])
                         :onMouseLeave  #(dispatch [::events/active-input nil])
                         :on-change     (ui-callback/input-callback
                                          #(do
                                             (reset! local-validate? true)
                                             (dispatch [::events/update-credential name-kw %])
                                             (dispatch [validation-event])))}]
              ; Semantic UI's textarea styling requires to be wrapped in a form
              [ui/Form
               [ui/FormField {:class (when (and validate? (not valid?)) :error)}
                [ui/TextArea {:default-value value
                              :placeholder   (@tr [name-kw])
                              :disabled      (not editable?)
                              :icon          (when input-active? :pencil)
                              :onMouseEnter  #(dispatch [::events/active-input name-str])
                              :onMouseLeave  #(dispatch [::events/active-input nil])
                              :on-change     (ui-callback/input-callback
                                               #(do
                                                  (reset! local-validate? true)
                                                  (dispatch [::events/update-credential name-kw %])
                                                  (dispatch [validation-event])))}]]])
            [:span value])]]))))


(defn row-infrastructure-services-selector
  [subtype editable? value-spec validation-event]
  (let [tr                      (subscribe [::i18n-subs/tr])
        infrastructure-services (subscribe [::subs/infrastructure-services-available subtype])
        credential              (subscribe [::subs/credential])
        local-validate?         (r/atom false)
        validate-form?          (subscribe [::subs/validate-form?])]
    (dispatch [::events/fetch-infrastructure-services-available subtype])
    (fn [subtype editable? value-spec validation-event]
      (let [value     (:parent @credential)
            validate? (or @local-validate? @validate-form?)
            valid?    (s/valid? value-spec value)]
        [ui/TableRow
         [ui/TableCell {:collapsing true}
          (utils-general/mandatory-name (@tr [:infrastructure]))]
         [ui/TableCell {:error (and validate? (not valid?))}
          [ui/Form {:style {:max-height "100px"
                            :overflow-y "auto"}}
           (if (pos-int? (count @infrastructure-services))
             (for [{id :id, infra-name :name} @infrastructure-services]
               ^{:key (str id value)}
               [ui/FormField
                [ui/Radio {:label    (or infra-name id)
                           :checked  (= id value)
                           :disabled (not editable?)
                           :on-click (ui-callback/value
                                       #(do
                                          (reset! local-validate? true)
                                          (dispatch [::events/update-credential :parent id])
                                          (dispatch [validation-event])))}]
                ff/nbsp
                [history/icon-link (str "api/" id)]])
             [ui/Message {:content (str (str/capitalize (@tr [:no-infra-service-of-subtype])) " " subtype ".")}]
             )]]]))))


(defn credential-swarm
  []
  (let [is-new?    (subscribe [::subs/is-new?])
        credential (subscribe [::subs/credential])]
    (fn []
      (let [editable?             (utils-general/editable? @credential @is-new?)
            {:keys [name description ca cert key]} @credential
            form-validation-event ::events/validate-swarm-credential-form]

        [:<>

         [acl/AclButton {:acl       (:acl @credential)
                         :read-only (not editable?)
                         :on-change #(dispatch [::events/update-credential :acl %])}]

         [ui/Table (assoc style/definition :class :nuvla-ui-editable)
          [ui/TableBody
           [row-with-label "name" :name name editable? true
            ::spec/name :input form-validation-event]
           [row-with-label "description" :description description editable? true
            ::spec/description :input form-validation-event]
           [row-with-label "swarm-credential-ca" :ca ca editable? true
            ::spec/ca :textarea form-validation-event]
           [row-with-label "swarm-credential-cert" :cert cert editable? true
            ::spec/cert :textarea form-validation-event]
           [row-with-label "swarm-credential-key" :key key editable? true
            ::spec/key :textarea form-validation-event]
           [row-infrastructure-services-selector "swarm" editable? ::spec/parent form-validation-event]]]]))))


(defn credential-minio
  []
  (let [is-new?    (subscribe [::subs/is-new?])
        credential (subscribe [::subs/credential])]
    (fn []
      (let [editable?             (utils-general/editable? @credential @is-new?)
            {:keys [name description access-key secret-key]} @credential
            form-validation-event ::events/validate-minio-credential-form]

        [:<>
         [acl/AclButton {:acl       (:acl @credential)
                         :read-only (not editable?)
                         :on-change #(dispatch [::events/update-credential :acl %])}]

         [ui/Table (assoc style/definition :class :nuvla-ui-editable)
          [ui/TableBody
           [row-with-label "name" :name name editable? true
            ::spec/name :input form-validation-event]
           [row-with-label "description" :description description editable? true
            ::spec/description :input form-validation-event]
           [row-with-label "access-key" :access-key access-key editable? true
            ::spec/access-key :input form-validation-event]
           [row-with-label "secret-key" :secret-key secret-key editable? true
            ::spec/secret-key :password form-validation-event]
           [row-infrastructure-services-selector "s3" editable? ::spec/parent form-validation-event]]]]))))


(defn credential-store-azure
  []
  (let [is-new?    (subscribe [::subs/is-new?])
        credential (subscribe [::subs/credential])]
    (fn []
      (let [editable?             (utils-general/editable? @credential @is-new?)
            {:keys [name description access-key secret-key]} @credential
            form-validation-event ::events/validate-minio-credential-form]
        [ui/Table (assoc style/definition :class :nuvla-ui-editable)
         [ui/TableBody]]))))


(defn credential-store-amazonec2
  []
  (let [is-new?    (subscribe [::subs/is-new?])
        credential (subscribe [::subs/credential])]
    (fn []
      (let [editable?             (utils-general/editable? @credential @is-new?)
            {:keys [name description access-key secret-key]} @credential
            form-validation-event ::events/validate-minio-credential-form]
        [ui/Table (assoc style/definition :class :nuvla-ui-editable)
         [ui/TableBody]]))))


(defn credential-store-exoscale
  []
  (let [is-new?    (subscribe [::subs/is-new?])
        credential (subscribe [::subs/credential])]
    (fn []
      (let [editable?             (utils-general/editable? @credential @is-new?)
            {:keys [name description access-key secret-key]} @credential
            form-validation-event ::events/validate-minio-credential-form]
        [ui/Table (assoc style/definition :class :nuvla-ui-editable)
         [ui/TableBody]]))))


(defn save-callback
  [form-validation-event]
  (dispatch-sync [::events/set-validate-form? true])
  (dispatch-sync [form-validation-event])
  (let [form-valid? (get @re-frame.db/app-db ::spec/form-valid?)]
    (when form-valid?
      (do
        (dispatch [::events/set-validate-form? false])
        (dispatch [::events/edit-credential])))))


(def infrastructure-service-validation-map
  {"infrastructure-service-swarm"          {:validation-event ::events/validate-swarm-credential-form
                                            :modal-content    credential-swarm}
   "infrastructure-service-minio"          {:validation-event ::events/validate-minio-credential-form
                                            :modal-content    credential-minio}
   "infrastructure-service-azure"          {:validation-event ::events/validate-store-azure-form
                                            :modal-content    credential-store-azure}
   "infrastructure-service-amazonec2"      {:validation-event ::events/validate-store-amazonec2-form
                                            :modal-content    credential-store-amazonec2}
   "infrastructure-service-exoscale"       {:validation-event ::events/validate-store-exoscale-form
                                            :modal-content    credential-store-exoscale}
   "cloud-infrastructure-service-exoscale" {:validation-event ::events/validate-swarm-credential-form
                                            :modal-content    credential-swarm
                                            }
   "cloud-infrastructure-service-azure"    {:validation-event ::events/validate-minio-credential-form
                                            :modal-content    credential-minio}
   ;""
   ;{:validation-event ::events/validate-swarm-credential-form
   ; :modal-content    credential-swarm}
   })


(def infrastructure-service-subtypes
  (keys infrastructure-service-validation-map))


(def cloud-subtypes
  ["cloud-infrastructure-service-exoscale"
   "cloud-infrastructure-service-azure"])


(defn credential-modal
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        visible?    (subscribe [::subs/credential-modal-visible?])
        form-valid? (subscribe [::subs/form-valid?])
        credential  (subscribe [::subs/credential])
        is-new?     (subscribe [::subs/is-new?])]
    (fn []
      (let [subtype          (:subtype @credential "")
            header           (str (if is-new? "New" "Update") " Credential: " subtype)
            validation-item  (get infrastructure-service-validation-map subtype)
            validation-event (:validation-event validation-item)
            modal-content    (:modal-content validation-item)]
        (if (empty? subtype)
          [:div]
          [ui/Modal {:open       @visible?
                     :close-icon true
                     :on-close   #(dispatch [::events/close-credential-modal])}

           [ui/ModalHeader header]

           [ui/ModalContent {:scrolling false}
            [utils-validation/validation-error-message ::subs/form-valid?]
            [modal-content]]
           [ui/ModalActions
            [uix/Button {:text     (if (true? @is-new?) (@tr [:create]) (@tr [:save]))
                         :positive true
                         :disabled (when-not @form-valid? true)
                         :active   true
                         :on-click #(save-callback validation-event)}]]])))))


(defn add-credential-modal
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        visible? (subscribe [::subs/add-credential-modal-visible?])]
    (fn []
      (let []
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-add-credential-modal])}

         [ui/ModalHeader [ui/Icon {:name "add"}] (@tr [:add])]

         [ui/ModalContent {:scrolling false}
          [:div {:style {:padding-bottom 20}} "Choose the credential subtype you want to add."]
          [ui/CardGroup {:centered true}

           [ui/Card {:on-click #(do
                                  (dispatch [::events/set-validate-form? false])
                                  (dispatch [::events/form-valid])
                                  (dispatch [::events/close-add-credential-modal])
                                  (dispatch [::events/open-credential-modal {:subtype "infrastructure-service-swarm"}
                                             true]))}
            [ui/CardContent {:text-align :center}
             [ui/Header "Swarm"]
             [ui/Icon {:name "docker"
                       :size :massive}]]]

           [ui/Card {:on-click #(do
                                  (dispatch [::events/set-validate-form? false])
                                  (dispatch [::events/form-valid])
                                  (dispatch [::events/close-add-credential-modal])
                                  (dispatch [::events/open-credential-modal {:subtype "infrastructure-service-minio"}
                                             true]))}
            [ui/CardContent {:text-align :center}
             [ui/Header "MinIO"]
             [:div]
             [ui/Image {:src  "/ui/images/minio.png"
                        :size :tiny}]]]]]]))))


(defn refresh-button
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn []
      [uix/MenuItemWithIcon
       {:name      (@tr [:refresh])
        :icon-name "refresh"
        :position  "right"
        :on-click  #(dispatch [::events/get-credentials])}])))


(defn control-bar-projects []
  (let [tr (subscribe [::i18n-subs/tr])]
    (vec (concat [ui/Menu {:borderless true}
                  [uix/MenuItemWithIcon
                   {:name      (@tr [:add])
                    :icon-name "add"
                    :on-click  #(dispatch [::events/open-add-credential-modal])}]
                  [refresh-button]]))))


(defn delete-confirmation-modal
  []
  (let [tr         (subscribe [::i18n-subs/tr])
        visible?   (subscribe [::subs/delete-confirmation-modal-visible?])
        credential (subscribe [::subs/credential])
        confirmed? (r/atom false)]
    (fn []
      (let [id   (:id @credential)
            name (:name @credential)]
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-delete-confirmation-modal])}

         [ui/ModalHeader (str "Delete Credential?")]

         [ui/ModalContent {:scrolling false}
          [:h3 name " (" (:description @credential) ")"]
          [:div "(" id ")"]
          [ui/Message {:error true}
           [ui/MessageHeader "Danger - this cannot be undone!"]
           [ui/MessageContent
            [:p]
            [ui/Checkbox {:name      "confirm-deletion"
                          :label     (@tr [:credential-delete-warning])
                          :checked   @confirmed?
                          :fitted    true
                          :on-change #(reset! confirmed? (not @confirmed?))}]]]]

         [ui/ModalActions
          [uix/Button {:text     (@tr [:delete])
                       :positive true
                       :disabled (when-not @confirmed? true)
                       :active   true
                       :on-click #(do
                                    (reset! confirmed? false)
                                    (dispatch [::events/delete-credential id])
                                    (dispatch [::events/close-delete-confirmation-modal]))}]]]))))


;subtype name description
(defn single-credential
  [{:keys [id subtype name description] :as credential}]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/TableRow                                            ;{:key id}
     [ui/TableCell {:floated :left
                    :width   2}
      [:span name]]
     [ui/TableCell {:floated :left
                    :width   9}
      [:span description]]
     [ui/TableCell {:floated :left
                    :width   4}
      [:span subtype]]
     [ui/TableCell {:floated :right
                    :width   1
                    :align   :right
                    :style   {}}

      (when (general-utils/can-delete? credential)
        [utils-accordion/trash id ::events/open-delete-confirmation-modal nil credential])

      (when (general-utils/can-edit? credential)
        [ui/Icon {:name     :cog
                  :color    :blue
                  :style    {:cursor :pointer}
                  :on-click #(dispatch [::events/open-credential-modal credential false])}])]]))


(defn credentials
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        credentials (subscribe [::subs/credentials])]
    (fn []
      (let [infra-service-creds (filter #(in? infrastructure-service-subtypes (:subtype %)) @credentials)
            ;cloud-creds         (filter #(in? cloud-subtypes (:subtype %)) @credentials)
            ]
        (dispatch [::events/get-credentials])
        [ui/Container {:fluid true}
         [:h2 [ui/Icon {:name "key"}]
          " "
          (str/capitalize (@tr [:credentials]))]

         [uix/Accordion
          [:<>
           [:div (@tr [:credential-infra-service-section-sub-text])]
           [control-bar-projects]
           (if (empty? infra-service-creds)
             [ui/Message
              (str/capitalize (str (@tr [:no-credentials]) "."))]
             [:div [ui/Table {:style {:margin-top 10}
                              :class :nuvla-ui-editable}
                    [ui/TableHeader
                     [ui/TableRow
                      [ui/TableHeaderCell {:content "Name"}]
                      [ui/TableHeaderCell {:content "Description"}]
                      [ui/TableHeaderCell {:content "Type"}]
                      [ui/TableHeaderCell {:content "Actions"}]]]
                    [ui/TableBody
                     (for [credential infra-service-creds]
                       ^{:key (:id credential)}
                       [single-credential credential])]]])]
          :label (@tr [:credential-infra-service-section])
          :count (count infra-service-creds)]]))))


(defmethod panel/render :credentials
  [path]
  (timbre/set-level! :info)
  [:div
   [credentials]
   [add-credential-modal]
   [credential-modal]
   [delete-confirmation-modal]])
