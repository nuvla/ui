(ns sixsq.nuvla.ui.pages.credentials.views
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch dispatch-sync subscribe]]
            [re-frame.db]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.acl.views :as acl]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.plugins.nav-tab :as tab-plugin]
            [sixsq.nuvla.ui.common-components.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.pages.credentials.events :as events]
            [sixsq.nuvla.ui.pages.credentials.spec :as spec]
            [sixsq.nuvla.ui.pages.credentials.subs :as subs]
            [sixsq.nuvla.ui.pages.credentials.utils :as utils]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.general :as utils-general]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.common-components.plugins.table-refactor :as table-refactor :refer [TableController]]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
            [sixsq.nuvla.ui.utils.validation :as utils-validation]))

(defn row-infrastructure-services-selector
  [subtypes additional-filter _editable? _value-spec _on-change]
  (let [tr              (subscribe [::i18n-subs/tr])
        infra-services  (subscribe [::subs/infrastructure-services-available])
        credential      (subscribe [::subs/credential])
        local-validate? (r/atom false)
        validate-form?  (subscribe [::subs/validate-form?])]
    (dispatch [::events/fetch-infrastructure-services-available subtypes additional-filter])
    (fn [subtypes _additional-filter _editable? value-spec on-change]
      (let [value     (:parent @credential)
            subtype   (:subtype @credential)
            validate? (or @local-validate? @validate-form?)
            valid?    (s/valid? value-spec value)]
        [ui/TableRow
         [ui/TableCell {:collapsing true}
          (utils-general/mandatory-name (if (= utils/subtype-infrastructure-service-helm-repo subtype)
                                          "Helm Repository"
                                          (@tr [:infrastructure])))]
         [ui/TableCell {:error (and validate? (not valid?))}
          (if (pos-int? (count @infra-services))
            ^{:key value}
            [ui/Dropdown {:clearable   true
                          :selection   true
                          :fluid       true
                          :value       value
                          :placeholder (@tr [:credentials-select-related-infra])
                          :on-change   (ui-callback/callback
                                         :value #(do
                                                   (reset! local-validate? true)
                                                   (on-change %)))
                          :options     (map (fn [{id :id, infra-name :name}]
                                              {:key id, :value id, :text infra-name})
                                            @infra-services)}]
            [ui/Message {:content (str (str/capitalize (@tr [:no-infra-service-of-subtype]))
                                       " " subtypes ".")}])]]))))

(defn credential-coe
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        coe-subtypes   ["swarm" "kubernetes"]
        is-new?        (subscribe [::subs/is-new?])
        credential     (subscribe [::subs/credential])
        validate-form? (subscribe [::subs/validate-form?])
        on-change      (fn [name-kw value]
                         (dispatch [::events/update-credential name-kw value])
                         (dispatch [::events/validate-credential-form ::spec/coe-credential]))]
    (fn []
      (let [editable? (utils-general/editable? @credential @is-new?)
            {:keys [name description ca cert key]} @credential]

        [:<>

         [acl/AclButton {:default-value (:acl @credential)
                         :read-only     (not editable?)
                         :on-change     #(dispatch [::events/update-credential :acl %])}]

         [ui/Table style/definition
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
           [row-infrastructure-services-selector coe-subtypes nil editable? ::spec/parent
            (partial on-change :parent)]]]]))))

(defn credential-ssh
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        is-new?        (subscribe [::subs/is-new?])
        credential     (subscribe [::subs/credential])
        validate-form? (subscribe [::subs/validate-form?])
        on-change      (fn [name-kw value]
                         (dispatch [::events/update-credential name-kw value])
                         (dispatch [::events/validate-credential-form ::spec/ssh-credential]))]
    (fn []
      (let [editable? (utils-general/editable? @credential @is-new?)
            {:keys [name description public-key private-key]} @credential]

        [:<>

         [acl/AclButton {:default-value (:acl @credential)
                         :read-only     (not editable?)
                         :on-change     #(dispatch [::events/update-credential :acl %])}]

         [ui/Table style/definition
          [ui/TableBody
           [uix/TableRowField (@tr [:name]), :editable? editable?, :required? true,
            :validate-form? @validate-form?, :default-value name, :spec ::spec/name,
            :on-change (partial on-change :name)]
           [uix/TableRowField (@tr [:description]), :editable? editable?, :required? true,
            :default-value description, :spec ::spec/description, :validate-form? @validate-form?,
            :on-change (partial on-change :description)]
           [uix/TableRowField (r/as-element [ui/Popup
                                             {:position "right center"
                                              :content  (@tr [:public-key-info])
                                              :trigger  (r/as-element
                                                          [:span (@tr [:public-key])
                                                           [icons/InfoIconFull]])}]),
            :placeholder (@tr [:public-key]), :editable? editable?, :required? false,
            :default-value public-key, :spec ::spec/public-key-optional, :validate-form? @validate-form?,
            :type :textarea, :on-change (partial on-change :public-key)]
           [uix/TableRowField (@tr [:private-key]), :placeholder (@tr [:private-key]), :editable? editable?,
            :required? false, :default-value private-key, :spec ::spec/private-key-optional,
            :validate-form? @validate-form?, :type :textarea, :on-change (partial on-change :private-key)]]]]))))

(defn credential-gpg
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        is-new?        (subscribe [::subs/is-new?])
        credential     (subscribe [::subs/credential])
        validate-form? (subscribe [::subs/validate-form?])
        on-change      (fn [name-kw value]
                         (dispatch [::events/update-credential name-kw value])
                         (dispatch [::events/validate-credential-form ::spec/gpg-credential]))]
    (fn []
      (let [editable? (utils-general/editable? @credential @is-new?)
            {:keys [name description public-key private-key]} @credential]

        [:<>
         [acl/AclButton {:default-value (:acl @credential)
                         :read-only     (not editable?)
                         :on-change     #(dispatch [::events/update-credential :acl %])}]
         [ui/Table style/definition
          [ui/TableBody
           [uix/TableRowField (@tr [:name]), :editable? editable?, :required? true,
            :validate-form? @validate-form?, :default-value name, :spec ::spec/name,
            :on-change (partial on-change :name)]
           [uix/TableRowField (@tr [:description]), :editable? editable?, :required? true,
            :default-value description, :spec ::spec/description, :validate-form? @validate-form?,
            :on-change (partial on-change :description)]
           [uix/TableRowField (@tr [:public-key]), :placeholder (@tr [:public-key]), :editable? editable?,
            :required? true, :default-value public-key, :validate-form? @validate-form?,
            :spec ::spec/public-key, :type :textarea,
            :on-change (partial on-change :public-key)]
           [uix/TableRowField (@tr [:private-key]), :placeholder (@tr [:private-key]), :editable? editable?,
            :required? false, :default-value private-key, :validate-form? @validate-form?,
            :spec ::spec/private-key-optional, :type :textarea,
            :on-change (partial on-change :private-key)]]]]))))

(defn credential-object-store
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

         [ui/Table style/definition
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
           [uix/TableRowField "secret-key", :editable? editable?, :required? true, :type :password,
            :default-value secret-key, :spec ::spec/secret-key, :validate-form? @validate-form?,
            :on-change (partial on-change :secret-key)]
           [row-infrastructure-services-selector ["s3"] nil editable? ::spec/parent
            (partial on-change :parent)]]]]))))

(defn credential-registry
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        is-new?        (subscribe [::subs/is-new?])
        credential     (subscribe [::subs/credential])
        validate-form? (subscribe [::subs/validate-form?])
        on-change      (fn [name-kw value]
                         (dispatch [::events/update-credential name-kw value])
                         (dispatch [::events/validate-credential-form ::spec/registry-credential]))]
    (fn []
      (let [editable? (utils-general/editable? @credential @is-new?)
            {:keys [name description username password subtype]} @credential]

        [:<>
         [acl/AclButton {:default-value (:acl @credential)
                         :read-only     (not editable?)
                         :on-change     #(dispatch [::events/update-credential :acl %])}]

         [ui/Table style/definition
          [ui/TableBody
           [uix/TableRowField (@tr [:name]), :editable? editable?, :required? true,
            :default-value name, :spec ::spec/name, :on-change (partial on-change :name),
            :validate-form? @validate-form?]
           [uix/TableRowField (@tr [:description]), :editable? editable?, :required? true,
            :default-value description, :spec ::spec/description, :validate-form? @validate-form?,
            :on-change (partial on-change :description)]
           [uix/TableRowField "username", :editable? editable?, :required? true,
            :default-value username, :spec ::spec/username, :validate-form? @validate-form?,
            :on-change (partial on-change :username)]
           [uix/TableRowField "password", :editable? editable?, :required? true,
            :default-value password, :spec ::spec/password, :validate-form? @validate-form?,
            :type :password, :on-change (partial on-change :password)]
           [row-infrastructure-services-selector (if (= utils/subtype-infrastructure-service-helm-repo subtype)
                                                   ["helm-repo"]
                                                   ["registry"]) nil editable? ::spec/parent
            (partial on-change :parent)]]]]))))

(defn credential-api-key
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        is-new?        (subscribe [::subs/is-new?])
        credential     (subscribe [::subs/credential])
        validate-form? (subscribe [::subs/validate-form?])
        on-change      (fn [name-kw value]
                         (dispatch [::events/update-credential name-kw value])
                         (dispatch [::events/validate-credential-form ::spec/api-key-credential]))]
    (fn []
      (let [editable? (utils-general/editable? @credential @is-new?)
            {:keys [name description]} @credential]

        [:<>
         [acl/AclButton {:default-value (:acl @credential)
                         :read-only     (not editable?)
                         :on-change     #(dispatch [::events/update-credential :acl %])}]

         [ui/Table style/definition
          [ui/TableBody
           [uix/TableRowField (@tr [:name]), :editable? editable?, :required? true,
            :default-value name, :spec ::spec/name, :on-change (partial on-change :name),
            :validate-form? @validate-form?]
           [uix/TableRowField (@tr [:description]), :editable? editable?, :required? true,
            :default-value description, :spec ::spec/description, :validate-form? @validate-form?,
            :on-change (partial on-change :description)]]]]))))

(defn credential-vpn
  []
  (let [tr                 (subscribe [::i18n-subs/tr])
        is-new?            (subscribe [::subs/is-new?])
        credential         (subscribe [::subs/credential])
        validate-form?     (subscribe [::subs/validate-form?])
        on-change          (fn [name-kw value]
                             (dispatch [::events/update-credential name-kw value])
                             (dispatch [::events/validate-credential-form ::spec/vpn-credential]))
        infra-services     (subscribe [::subs/infrastructure-services-available])
        user               (subscribe [::session-subs/user])
        update-description (atom true)]
    (fn []
      (let [editable?              (utils-general/editable? @credential @is-new?)
            infra-id               (:parent @credential)
            infra-service-selected (->> @infra-services
                                        (filter #(= (:id %) infra-id))
                                        first)
            infra-name-or-id       (or (:name infra-service-selected)
                                       (:id infra-service-selected))
            name-credential        (str infra-name-or-id " - " @user)
            description-credential (str infra-name-or-id " " (@tr [:credential-for]) " " @user)]
        (on-change :name name-credential)
        (when @update-description                           ; used for first load
          (on-change :description description-credential))
        [:<>
         [ui/Table style/definition
          [ui/TableBody
           [row-infrastructure-services-selector ["vpn"] "vpn-scope='customer'" editable?
            ::spec/parent #(do (on-change :parent %)
                               (on-change :description description-credential))]
           ^{:key (str "description-cred-" infra-id)}
           [uix/TableRowField (@tr [:description]), :editable? editable?, :required? true,
            :default-value description-credential, :spec ::spec/description,
            :validate-form? @validate-form?, :on-change #(do
                                                           (reset! update-description false)
                                                           (on-change :description %))]]]]))))

(defn VpnGeneratedCredential
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        generated-cred (subscribe [::subs/generated-credential-modal])
        cred           (subscribe [::subs/credential])
        infra-services (subscribe [::subs/infrastructure-services-available])]
    (fn []
      (let [infra  (some #(when (= (:parent @cred) (:id %)) %) @infra-services)
            config (utils/vpn-config (:vpn-ca-certificate infra)
                                     (:vpn-intermediate-ca infra)
                                     (:intermediate-ca @generated-cred)
                                     (:certificate @generated-cred)
                                     (:private-key @generated-cred)
                                     (:vpn-shared-key infra)
                                     (:vpn-common-name-prefix infra)
                                     (:vpn-endpoints infra))]
        [:<>

         [ui/Message {:warning true}
          [ui/MessageHeader (@tr [:warning])]
          [ui/MessageContent
           [:div
            (@tr [:credential-please-save-this-file])
            [:br]
            " "
            (@tr [:credential-go-to])
            " "
            [:a {:href "https://docs.nuvla.io/nuvla/user-guide/vpn", :target "_blank"} (@tr [:documentation])]
            " "
            (@tr [:credential-for-details-vpn])]]]

         [ui/CardGroup {:centered true}

          [ui/Card
           {:href     (str "data:text/plain;charset=utf-8," (js/encodeURIComponent config))
            :download (str "vpn client " (:name @cred) ".conf")
            :disabled (not config)}
           [ui/CardContent {:text-align :center}
            [ui/Header (@tr [:credential-save])]
            [icons/TextFileIcon {:size :massive}]]]]]))))

(defn MessageKeyGenerated
  [header-key]
  [uix/Msg
   {:header  [uix/TR header-key]
    :content [uix/TR :warning-secret-displayed-once]
    :icon    icons/i-circle-check
    :type    :success}])

(defn ApiKeyGeneratedCredential
  []
  (let [generated-cred (subscribe [::subs/generated-credential-modal])]
    (fn []
      [:<>
       [MessageKeyGenerated :api-key-generated]
       [uix/CopyToClipboardDownload {:name  "Key"
                                     :value (:resource-id @generated-cred)}]
       [uix/CopyToClipboardDownload {:name  "Secret"
                                     :value (:secret-key @generated-cred)}]])))

(defn SshGeneratedCredential
  []
  (let [generated-cred (subscribe [::subs/generated-credential-modal])]
    (fn []
      (let [{:keys [private-key public-key]} @generated-cred]
        [:<>
         [MessageKeyGenerated :ssh-key-generated]
         [uix/CopyToClipboardDownload {:name     "Public-key"
                                       :value    public-key
                                       :download true
                                       :filename "ssh_public.key"}]
         [uix/CopyToClipboardDownload {:name     "Private-key"
                                       :value    private-key
                                       :download true
                                       :filename "ssh_private.key"}]]))))

(defn save-callback
  [form-validation-spec]
  (dispatch-sync [::events/set-validate-form? true])
  (dispatch-sync [::events/validate-credential-form form-validation-spec])
  (let [form-valid? (get @re-frame.db/app-db ::spec/form-valid?)]
    (when form-valid?
      (dispatch [::events/set-validate-form? false])
      (dispatch [::events/edit-credential]))))

(def infrastructure-service-storage-validation-map
  {utils/subtype-infrastructure-service-minio
   {:validation-spec ::spec/minio-credential
    :modal-content   credential-object-store}})

(def infrastructure-service-coe-validation-map
  {utils/subtype-infrastructure-service-swarm
   {:validation-spec ::spec/coe-credential
    :modal-content   credential-coe},
   utils/subtype-infrastructure-service-kubernetes
   {:validation-spec ::spec/coe-credential
    :modal-content   credential-coe}})

(def infrastructure-service-registry-validation-map
  {utils/subtype-infrastructure-service-registry
   {:validation-spec ::spec/registry-credential
    :modal-content   credential-registry}})

(def infrastructure-service-helm-repository-validation-map
  {utils/subtype-infrastructure-service-helm-repo
   {:validation-spec ::spec/registry-credential
    :modal-content   credential-registry}})

(def api-key-validation-map
  {utils/subtype-generate-api-key
   {:validation-spec ::spec/api-key-credential
    :modal-content   credential-api-key
    :modal-generated ApiKeyGeneratedCredential}
   "api-key"
   {:validation-spec ::spec/api-key-credential
    :modal-content   credential-api-key}})

(def infrastructure-service-access-keys-validation-map
  {utils/subtype-infrastructure-service-vpn
   {:validation-spec ::spec/vpn-credential
    :modal-content   credential-vpn
    :modal-generated VpnGeneratedCredential}
   utils/subtype-gpg-key
   {:validation-spec ::spec/gpg-credential
    :modal-content   credential-gpg}
   utils/subtype-ssh-key
   {:validation-spec ::spec/ssh-credential
    :modal-content   credential-ssh}
   utils/subtype-generate-ssh-key
   {:validation-spec ::spec/ssh-credential
    :modal-content   credential-ssh
    :modal-generated SshGeneratedCredential}})

(def infrastructure-service-validation-map
  (merge infrastructure-service-storage-validation-map
         infrastructure-service-coe-validation-map
         infrastructure-service-access-keys-validation-map
         infrastructure-service-registry-validation-map
         api-key-validation-map
         infrastructure-service-helm-repository-validation-map))

(defn CredentialModal
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        visible?    (subscribe [::subs/credential-modal-visible?])
        form-valid? (subscribe [::subs/form-valid?])
        credential  (subscribe [::subs/credential])
        is-new?     (subscribe [::subs/is-new?])]
    (fn []
      (let [subtype         (:subtype @credential "")
            tab-key         (:tab-key (utils/subtype->info subtype))
            icon            (:icon (utils/subtype->info subtype))
            name            (:name (utils/subtype->info subtype))
            header          (str (str/capitalize (str (if @is-new?
                                                        (@tr [:new])
                                                        (@tr [:update]))))
                                 " " name " " (@tr [:credential]))
            validation-item (get infrastructure-service-validation-map subtype)
            validation-spec (:validation-spec validation-item)
            modal-content   (:modal-content validation-item)]
        (if (empty? subtype)
          [:div]
          [ui/Modal {:open       @visible?
                     :close-icon true
                     :on-close   #(do (dispatch [::events/close-credential-modal]))}

           [uix/ModalHeader {:header header :icon icon}]

           [ui/ModalContent {:scrolling false}
            [utils-validation/validation-error-message ::subs/form-valid?]
            [modal-content]]
           [ui/ModalActions
            [uix/Button
             {:text     (if (true? @is-new?) (@tr [:create]) (@tr [:save]))
              :positive true
              :disabled (when-not @form-valid? true)
              :active   true
              :on-click #(do
                           (save-callback validation-spec)
                           (dispatch [::tab-plugin/change-tab
                                      {:db-path [::spec/tab] :tab-key tab-key}]))}]]])))))

(defn AddCredentialModal
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        visible?  (subscribe [::subs/add-credential-modal-visible?])
        is-group? (subscribe [::session-subs/is-group?])]
    (fn []
      [ui/Modal {:open       @visible?
                 :close-icon true
                 :on-close   #(dispatch [::events/close-add-credential-modal])}

       [uix/ModalHeader {:header (@tr [:add]) :icon icons/i-plus-full}]

       [ui/ModalContent {:scrolling false}
        [:div {:style {:padding-bottom 20}} (@tr [:credential-choose-type])]
        [ui/CardGroup {:centered true}

         [ui/Card
          {:on-click #(do
                        (dispatch [::events/set-validate-form? false])
                        (dispatch [::events/form-valid])
                        (dispatch [::events/close-add-credential-modal])
                        (dispatch [::events/open-credential-modal
                                   ;; FIXME: this is wrong, as it predefines the subtype of credential.
                                   ;; However, the subtype of cred depends on the subtype of COE IS.
                                   ;; We will overwrite this in utils/db->new-coe-credential depending
                                   ;; on the subtype of the COE IS user selected.
                                   ;; Not having subtype at this stage at all, doesn't render the modal.
                                   ;; Setting it to something other than infrastructure-service-swarm or
                                   ;; infrastructure-service-kubernetes doesn't work either. So, this is
                                   ;; a temporary default until COE IS is selected and submit button is
                                   ;; pressed.
                                   {:subtype utils/subtype-infrastructure-service-swarm} true]))}
          [ui/CardContent {:text-align :center}
           [ui/Header "Swarm / Kubernetes"]
           [icons/DockerIcon {:size :massive}]
           [ui/Image {:src   "/ui/images/kubernetes.svg"
                      :style {:max-width 112}}]]]

         [ui/Card
          (when-not @is-group?
            {:on-click #(do
                          (dispatch [::events/set-validate-form? false])
                          (dispatch [::events/form-valid])
                          (dispatch [::events/close-add-credential-modal])
                          (dispatch [::events/open-credential-modal
                                     {:subtype utils/subtype-infrastructure-service-vpn} true]))})
          [ui/CardContent {:text-align :center}
           [ui/Header "OpenVPN"]
           [ui/Image {:src   "/ui/images/openvpn.png"
                      :style {:max-width 112}}]
           (when @is-group?
             [:<>
              [:br]
              [:i (@tr [:credential-vpn-group-warning])]])]]

         [ui/Card
          {:on-click #(do
                        (dispatch [::events/set-validate-form? false])
                        (dispatch [::events/form-valid])
                        (dispatch [::events/close-add-credential-modal])
                        (dispatch [::events/open-credential-modal
                                   {:subtype utils/subtype-infrastructure-service-registry} true]))}
          [ui/CardContent {:text-align :center}
           [ui/Header "Docker registry"]
           [:div]
           [ui/IconGroup {:size "massive"}
            [icons/DockerIcon]
            [icons/DbIconFull {:corner "bottom right"}]]]]

         [ui/Card
          {:on-click #(do
                        (dispatch [::events/set-validate-form? false])
                        (dispatch [::events/form-valid])
                        (dispatch [::events/close-add-credential-modal])
                        (dispatch [::events/open-credential-modal
                                   {:subtype utils/subtype-infrastructure-service-helm-repo} true]))}
          [ui/CardContent {:text-align :center}
           [ui/Header "Helm Repository"]
           [ui/Image {:src   "/ui/images/helm.svg"
                      :style {:max-width 112}}]]]

         [ui/Card
          {:on-click #(do
                        (dispatch [::events/set-validate-form? false])
                        (dispatch [::events/form-valid])
                        (dispatch [::events/close-add-credential-modal])
                        (dispatch [::events/open-credential-modal
                                   {:subtype utils/subtype-infrastructure-service-minio} true]))}
          [ui/CardContent {:text-align :center}
           [ui/Header "Object Store"]
           [:div]
           [ui/Image {:src   "/ui/images/s3.png"
                      :style {:max-height 112}}]]]

         [ui/Card
          {:on-click #(do
                        (dispatch [::events/set-validate-form? false])
                        (dispatch [::events/form-valid])
                        (dispatch [::events/close-add-credential-modal])
                        (dispatch [::events/open-credential-modal
                                   {:subtype utils/subtype-generate-ssh-key} true]))}
          [ui/CardContent {:text-align :center}
           [ui/Header "SSH Keypair"]
           [:div]
           [ui/Image {:src   "/ui/images/ssh.png"
                      :style {:max-height 112}}]]]

         [ui/Card
          {:on-click #(do
                        (dispatch [::events/set-validate-form? false])
                        (dispatch [::events/form-valid])
                        (dispatch [::events/close-add-credential-modal])
                        (dispatch [::events/open-credential-modal
                                   {:subtype utils/subtype-gpg-key} true]))}
          [ui/CardContent {:text-align :center}
           [ui/Header "GPG Keypair"]
           [:div]
           [ui/Image {:src   "/ui/images/gpg.png"
                      :style {:max-height 112}}]]]

         [ui/Card
          {:on-click #(do
                        (dispatch [::events/set-validate-form? false])
                        (dispatch [::events/form-valid])
                        (dispatch [::events/close-add-credential-modal])
                        (dispatch [::events/open-credential-modal
                                   {:subtype utils/subtype-generate-api-key} true]))}
          [ui/CardContent {:text-align :center}
           [ui/Header "Api-Key"]
           [:div]
           [ui/Image {:src   "/ui/images/nuvla_logo_red_on_transparent_1000px.png"
                      :style {:max-width 120}}]]]]]])))

(defn GeneratedCredentialModal
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        generated-cred (subscribe [::subs/generated-credential-modal])
        cred           (subscribe [::subs/credential])]
    (fn []
      (let [subtype       (:subtype @cred "")
            item          (get infrastructure-service-validation-map subtype)
            modal-content (:modal-generated item)]
        [ui/Modal {:open       (boolean @generated-cred)
                   :close-icon true
                   :on-close   #(dispatch [::events/set-generated-credential-modal nil])}

         [uix/ModalHeader {:header (@tr [:credential-generate])}]

         [ui/ModalContent {:scrolling false}
          (when modal-content
            [modal-content])]]))))

(defn MenuBar []
  (let [tr (subscribe [::i18n-subs/tr])]
    [components/StickyBar
     [ui/Menu {:borderless true}
      [uix/MenuItem
       {:name     (@tr [:add])
        :icon     icons/i-plus-large
        :on-click #(dispatch [::events/open-add-credential-modal])}]
      [components/RefreshMenu
       {:on-refresh #(dispatch [::events/refresh])}]]]))

(defn DeleteButton
  [credential]
  (let [tr      (subscribe [::i18n-subs/tr])
        {:keys [id name description]} credential
        content (str (or name id) (when description " - ") description)]
    [uix/ModalDanger
     {:on-confirm  #(dispatch [::events/delete-credential id])
      :trigger     (r/as-element
                     [ui/Icon {:class icons/i-trash-full
                               :style {:cursor "pointer"}
                               :color "red"}])
      :content     [:h3 content]
      :header      (@tr [:delete-credential])
      :danger-msg  (@tr [:credential-delete-warning])
      :button-text (@tr [:delete])}]))

(defn Pagination
  []
  (let [credentials @(subscribe [::subs/credentials])]
    [pagination-plugin/Pagination {:db-path      [::spec/pagination]
                                   :total-items  (get credentials :count 0)
                                   :change-event [::events/refresh]}]))

(defn CellAction
  [_cell-data credential _column]
  [:<>
   (when (utils-general/can-delete? credential)
     [DeleteButton credential])

   (when (utils-general/can-edit? credential)
     [icons/GearIcon {:color    :blue
                      :style    {:cursor :pointer}
                      :on-click #(dispatch [::events/open-credential-modal credential false])}])])

(defn CredentialsPane
  [tab-key]
  (let [tr                   @(subscribe [::i18n-subs/tr])
        credentials          @(subscribe [::subs/credentials])
        section-sub-text-key (utils/tab->section-sub-text tab-key)
        !resources           (subscribe [::subs/credentials-resources])]
    [ui/TabPane
     [:div (when section-sub-text-key (tr [section-sub-text-key]))]
     (if (empty? credentials)
       [ui/Message (str/capitalize (str (tr [:no-credentials]) "."))]
       [:div
        [TableController {:!columns               (r/atom [{::table-refactor/field-key      :name
                                                            ::table-refactor/header-content (str/capitalize (tr [:name]))
                                                            ::table-refactor/no-delete      true}
                                                           {::table-refactor/field-key      :description
                                                            ::table-refactor/header-content (str/capitalize (tr [:description]))
                                                            ::table-refactor/no-delete      true}
                                                           {::table-refactor/field-key      :subtype
                                                            ::table-refactor/header-content (str/capitalize (tr [:type]))}
                                                           {::table-refactor/field-key      :actions
                                                            ::table-refactor/header-content (str/capitalize (tr [:actions]))
                                                            ::table-refactor/no-delete      true
                                                            ::table-refactor/collapsing     true
                                                            ::table-refactor/field-cell     CellAction}
                                                           ])
                          :!default-columns       (r/atom [:name :description :subtype :actions])
                          :!current-columns       (subscribe [::subs/table-current-cols])
                          :set-current-columns-fn #(dispatch [::events/set-table-current-cols %])
                          :!data                  !resources
                          :!enable-global-filter? (r/atom false)
                          :!enable-sorting?       (r/atom false)}]
        [Pagination]])]))

(defn CredentialMenuItem
  [tab-key]
  (let [tr         (subscribe [::i18n-subs/tr])
        summary    (subscribe [::subs/credentials-summary])
        cred-count (utils/get-cred-count @summary tab-key)]
    [:span (@tr [tab-key])
     (when (pos? cred-count)
       [ui/Label {:circular true
                  :size     "mini"
                  :attached "top right"}
        cred-count])]))

(defn credential
  [tab-key]
  {:menuItem {:content (r/as-element [CredentialMenuItem tab-key])
              :key     tab-key
              :icon    (:icon (utils/subtype->info tab-key))}
   :render   #(r/as-element [CredentialsPane tab-key])})

(defn panes
  []
  [(credential :coe-services)
   (credential :access-services)
   (credential :storage-services)
   (credential :registry-services)
   (credential :api-keys)
   (credential :helm-repositories)])

(defn TabsCredentials
  []
  (dispatch [::events/refresh])
  (fn []
    [components/LoadingPage {}
     [tab-plugin/Tab
      {:db-path      [::spec/tab]
       :change-event [::pagination-plugin/change-page [::spec/pagination] 1]
       :menu         {:secondary true
                      :pointing  true
                      :style     {:display        "flex"
                                  :flex-direction "row"
                                  :flex-wrap      "wrap"}}
       :panes        (panes)}]]))

(defn credentials-view
  [_path]
  [ui/Segment style/basic
   [MenuBar]
   [TabsCredentials]
   [AddCredentialModal]
   [CredentialModal]
   [GeneratedCredentialModal]])
