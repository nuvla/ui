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
    [sixsq.nuvla.ui.main.components :as main-components]
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


(defn row-infrastructure-services-selector
  [subtype additional-filter editable? value-spec on-change]
  (let [tr                      (subscribe [::i18n-subs/tr])
        infrastructure-services (subscribe [::subs/infrastructure-services-available subtype])
        credential              (subscribe [::subs/credential])
        local-validate?         (r/atom false)
        validate-form?          (subscribe [::subs/validate-form?])]
    (dispatch [::events/fetch-infrastructure-services-available subtype additional-filter])
    (fn [subtype additional-filter editable? value-spec on-change]
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
                                          (on-change id)))}]
                ff/nbsp
                [history/icon-link (str "api/" id)]])
             [ui/Message {:content (str (str/capitalize (@tr [:no-infra-service-of-subtype]))
                                        " " subtype ".")}])]]]))))


(defn credential-swarm
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        is-new?        (subscribe [::subs/is-new?])
        credential     (subscribe [::subs/credential])
        validate-form? (subscribe [::subs/validate-form?])
        on-change      (fn [name-kw value]
                         (dispatch [::events/update-credential name-kw value])
                         (dispatch [::events/validate-credential-form ::spec/swarm-credential]))]
    (fn []
      (let [editable? (utils-general/editable? @credential @is-new?)
            {:keys [name description ca cert key]} @credential]

        [:<>

         [acl/AclButton {:default-value (:acl @credential)
                         :read-only     (not editable?)
                         :on-change     #(dispatch [::events/update-credential :acl %])}]

         [ui/Table (assoc style/definition :class :nuvla-ui-editable)
          [ui/TableBody
           [uix/TableRowField (@tr [:name]), :editable? editable?, :required? true,
            :validate-form? @validate-form?, :default-value name, :spec ::spec/name,
            :on-change (partial on-change :name)]
           [uix/TableRowField (@tr [:description]), :editable? editable?, :required? true,
            :default-value description, :spec ::spec/description, :validate-form? @validate-form?,
            :on-change (partial on-change :description)]
           [uix/TableRowField "ca", :placeholder (@tr [:ca]), :editable? editable?, :required? true,
            :default-value ca, :spec ::spec/ca, :type :textarea, :validate-form? @validate-form?,
            :on-change (partial on-change :ca)]
           [uix/TableRowField "cert", :placeholder (@tr [:cert]), :editable? editable?,
            :required? true, :default-value cert, :spec ::spec/cert, :type :textarea,
            :on-change (partial on-change :cert), :validate-form? @validate-form?]
           [uix/TableRowField "key", :placeholder (@tr [:key]), :editable? editable?,
            :required? true, :default-value key, :spec ::spec/key, :type :textarea,
            :on-change (partial on-change :key), :validate-form? @validate-form?]
           [row-infrastructure-services-selector "swarm" nil editable? ::spec/parent
            (partial on-change :parent)]]]]))))


(defn credential-minio
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        is-new?        (subscribe [::subs/is-new?])
        credential     (subscribe [::subs/credential])
        validate-form? (subscribe [::subs/validate-form?])
        on-change      (fn [name-kw value]
                         (dispatch [::events/update-credential name-kw value])
                         (dispatch [::events/validate-credential-form ::spec/minio-credential]))]
    (fn []
      (let [editable? (utils-general/editable? @credential @is-new?)
            {:keys [name description access-key secret-key]} @credential]

        [:<>
         [acl/AclButton {:default-value (:acl @credential)
                         :read-only     (not editable?)
                         :on-change     #(dispatch [::events/update-credential :acl %])}]

         [ui/Table (assoc style/definition :class :nuvla-ui-editable)
          [ui/TableBody
           [uix/TableRowField (@tr [:name]), :editable? editable?, :required? true,
            :default-value name, :spec ::spec/name, :on-change (partial on-change :name),
            :validate-form? @validate-form?]
           [uix/TableRowField (@tr [:description]), :editable? editable?, :required? true,
            :default-value description, :spec ::spec/description, :validate-form? @validate-form?,
            :on-change (partial on-change :description)]
           [uix/TableRowField "access-key", :editable? editable?, :required? true,
            :default-value access-key, :spec ::spec/access-key, :validate-form? @validate-form?,
            :on-change (partial on-change :access-key)]
           [uix/TableRowField "secret-key", :editable? editable?, :required? true,
            :default-value secret-key, :spec ::spec/secret-key, :validate-form? @validate-form?,
            :on-change (partial on-change :secret-key)]
           [row-infrastructure-services-selector "s3" nil editable? ::spec/parent
            (partial on-change :parent)]]]]))))



(defn credential-vpn
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        is-new?        (subscribe [::subs/is-new?])
        credential     (subscribe [::subs/credential])
        validate-form? (subscribe [::subs/validate-form?])
        on-change      (fn [name-kw value]
                         (dispatch [::events/update-credential name-kw value])
                         (dispatch [::events/validate-credential-form ::spec/vpn-credential]))]
    (fn []
      (let [editable? (utils-general/editable? @credential @is-new?)
            {:keys [name description access-key secret-key]} @credential]

        [:<>

         [ui/Table (assoc style/definition :class :nuvla-ui-editable)
          [ui/TableBody
           [uix/TableRowField (@tr [:name]), :editable? editable?, :required? true,
            :default-value name, :spec ::spec/name, :on-change (partial on-change :name),
            :validate-form? @validate-form?]
           [uix/TableRowField (@tr [:description]), :editable? editable?, :required? true,
            :default-value description, :spec ::spec/description, :validate-form? @validate-form?,
            :on-change (partial on-change :description)]
           [row-infrastructure-services-selector "vpn" "vpn-scope='customer'" editable?
            ::spec/parent (partial on-change :parent)]]]]))))


(defn save-callback
  [form-validation-spec]
  (dispatch-sync [::events/set-validate-form? true])
  (dispatch-sync [::events/validate-credential-form form-validation-spec])
  (let [form-valid? (get @re-frame.db/app-db ::spec/form-valid?)]
    (when form-valid?
      (do
        (dispatch [::events/set-validate-form? false])
        (dispatch [::events/edit-credential])))))


(def infrastructure-service-validation-map
  {"infrastructure-service-swarm"
   {:validation-spec ::spec/swarm-credential
    :modal-content   credential-swarm},
   "infrastructure-service-minio"
   {:validation-spec ::spec/minio-credential
    :modal-content   credential-minio},
   "infrastructure-service-vpn"
   {:validation-spec ::spec/vpn-credential
    :modal-content   credential-vpn}})


(def infrastructure-service-subtypes
  (keys infrastructure-service-validation-map))


(defn credential-modal
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        visible?    (subscribe [::subs/credential-modal-visible?])
        form-valid? (subscribe [::subs/form-valid?])
        credential  (subscribe [::subs/credential])
        is-new?     (subscribe [::subs/is-new?])]
    (fn []
      (let [subtype         (:subtype @credential "")
            header          (str (if is-new? "New" "Update") " Credential: " subtype)
            validation-item (get infrastructure-service-validation-map subtype)
            validation-spec (:validation-spec validation-item)
            modal-content   (:modal-content validation-item)]
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
                         :on-click #(save-callback validation-spec)}]]])))))


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

           [ui/Card
            {:on-click #(do
                          (dispatch [::events/set-validate-form? false])
                          (dispatch [::events/form-valid])
                          (dispatch [::events/close-add-credential-modal])
                          (dispatch [::events/open-credential-modal
                                     {:subtype "infrastructure-service-swarm"} true]))}
            [ui/CardContent {:text-align :center}
             [ui/Header "Swarm"]
             [ui/Icon {:name "docker"
                       :size :massive}]]]

           [ui/Card
            {:on-click #(do
                          (dispatch [::events/set-validate-form? false])
                          (dispatch [::events/form-valid])
                          (dispatch [::events/close-add-credential-modal])
                          (dispatch [::events/open-credential-modal
                                     {:subtype "infrastructure-service-minio"} true]))}
            [ui/CardContent {:text-align :center}
             [ui/Header "MinIO"]
             [:div]
             [ui/Image {:src  "/ui/images/minio.png"
                        :size :tiny}]]]

           [ui/Card
            {:on-click #(do
                          (dispatch [::events/set-validate-form? false])
                          (dispatch [::events/form-valid])
                          (dispatch [::events/close-add-credential-modal])
                          (dispatch [::events/open-credential-modal
                                     {:subtype "infrastructure-service-vpn"} true]))}
            [ui/CardContent {:text-align :center}
             [ui/Header "OpenVPN"]
             [:div]
             [ui/Image {:src  "/ui/images/openvpn.png"
                        :size "small"}]]]
           ]]]))))


(defn control-bar []
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Menu {:borderless true}
     [uix/MenuItemWithIcon
      {:name      (@tr [:add])
       :icon-name "add"
       :on-click  #(dispatch [::events/open-add-credential-modal])}]
     [main-components/RefreshMenu
      {:on-refresh #(dispatch [::events/get-credentials])}]]))


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
  [ui/TableRow
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
                :on-click #(dispatch [::events/open-credential-modal credential false])}])]])


(defn credentials
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        credentials (subscribe [::subs/credentials])]
    (fn []
      (let [infra-service-creds (filter #(in? infrastructure-service-subtypes (:subtype %))
                                        @credentials)]
        (dispatch [::events/get-credentials])
        [ui/Container {:fluid true}
         [uix/PageHeader "key" (str/capitalize (@tr [:credentials])) :inline true]
         [uix/Accordion
          [:<>
           [:div (@tr [:credential-infra-service-section-sub-text])]
           [control-bar]
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
