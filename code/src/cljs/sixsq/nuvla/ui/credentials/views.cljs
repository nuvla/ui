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
    [sixsq.nuvla.ui.credentials.utils :as utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.session.subs :as session-subs]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.validation :as utils-validation]
    [taoensso.timbre :as timbre]))


(defn in?
  "true if coll contains elm"
  [coll elm]
  (some #(= elm %) coll))


(defn row-infrastructure-services-selector
  [subtype additional-filter editable? value-spec on-change]
  (let [tr              (subscribe [::i18n-subs/tr])
        infra-services  (subscribe [::subs/infrastructure-services-available])
        credential      (subscribe [::subs/credential])
        local-validate? (r/atom false)
        validate-form?  (subscribe [::subs/validate-form?])]
    (dispatch [::events/fetch-infrastructure-services-available subtype additional-filter])
    (fn [subtype additional-filter editable? value-spec on-change]
      (let [value     (:parent @credential)
            validate? (or @local-validate? @validate-form?)
            valid?    (s/valid? value-spec value)]
        [ui/TableRow
         [ui/TableCell {:collapsing true}
          (general-utils/mandatory-name (@tr [:infrastructure]))]
         [ui/TableCell {:error (and validate? (not valid?))}
          (if (pos-int? (count @infra-services))
            ^{:key value}
            [ui/Dropdown {:clearable   true
                          :selection   true
                          :fluid       true
                          :value       value
                          :placeholder "Select releated infrastructure service"
                          :on-change   (ui-callback/callback
                                         :value #(do
                                                   (reset! local-validate? true)
                                                   (on-change %)))
                          :options     (map (fn [{id :id, infra-name :name}]
                                              {:key id, :value id, :text infra-name})
                                            @infra-services)}]
            [ui/Message {:content (str (str/capitalize (@tr [:no-infra-service-of-subtype]))
                                       " " subtype ".")}])]]))))


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
      (let [editable? (general-utils/editable? @credential @is-new?)
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
           [row-infrastructure-services-selector ["swarm" "kubernetes"] nil editable? ::spec/parent
            (partial on-change :parent)]]]]))))


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
      (let [editable? (general-utils/editable? @credential @is-new?)
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
           [uix/TableRowField "secret-key", :editable? editable?, :required? true,
            :default-value secret-key, :spec ::spec/secret-key, :validate-form? @validate-form?,
            :on-change (partial on-change :secret-key)]
           [row-infrastructure-services-selector ["s3"] nil editable? ::spec/parent
            (partial on-change :parent)]]]]))))


(defn credential-registy
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        is-new?        (subscribe [::subs/is-new?])
        credential     (subscribe [::subs/credential])
        validate-form? (subscribe [::subs/validate-form?])
        on-change      (fn [name-kw value]
                         (dispatch [::events/update-credential name-kw value])
                         (dispatch [::events/validate-credential-form ::spec/registry-credential]))]
    (fn []
      (let [editable? (general-utils/editable? @credential @is-new?)
            {:keys [name description username password]} @credential]

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
            :on-change (partial on-change :password)]
           [row-infrastructure-services-selector ["registry"] nil editable? ::spec/parent
            (partial on-change :parent)]]]]))))


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
      (let [editable?              (general-utils/editable? @credential @is-new?)
            infra-id               (:parent @credential)
            infra-service-selected (->> @infra-services
                                        (filter #(= (:id %) infra-id))
                                        first)
            infra-name-or-id       (or (:name infra-service-selected)
                                       (:id infra-service-selected))
            name-credential        (str infra-name-or-id " - " @user)
            description-credential (str infra-name-or-id " credential for " @user)]
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
    :modal-content   credential-object-store},
   "infrastructure-service-vpn"
   {:validation-spec ::spec/vpn-credential
    :modal-content   credential-vpn}
   "infrastructure-service-registry"
   {:validation-spec ::spec/registry-credential
    :modal-content   credential-registy}
   })


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
            header          (str (if is-new? "New" "Update") " Credential")
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
          [:div {:style {:padding-bottom 20}} (@tr [:credentials-add-message])]
          [ui/CardGroup {:centered true}

           [ui/Card
            {:on-click #(do
                          (dispatch [::events/set-validate-form? false])
                          (dispatch [::events/form-valid])
                          (dispatch [::events/close-add-credential-modal])
                          (dispatch [::events/open-credential-modal
                                     {:subtype "infrastructure-service-swarm"} true]))}
            [ui/CardContent {:text-align :center}
             [ui/Header "Swarm / Kubernetes"]
             [ui/Icon {:name "docker"
                       :size :massive}]
             [ui/Image {:src   "/ui/images/kubernetes.svg"
                        :style {:max-width 112}}]]]

           [ui/Card
            {:on-click #(do
                          (dispatch [::events/set-validate-form? false])
                          (dispatch [::events/form-valid])
                          (dispatch [::events/close-add-credential-modal])
                          (dispatch [::events/open-credential-modal
                                     {:subtype "infrastructure-service-vpn"} true]))}
            [ui/CardContent {:text-align :center}
             [ui/Header "OpenVPN"]
             [ui/Image {:src   "/ui/images/openvpn.png"
                        :style {:max-width 112}}]]]]
          [uix/MoreAccordion
           [ui/CardGroup {:centered true}
            [ui/Card
             {:on-click #(do
                           (dispatch [::events/set-validate-form? false])
                           (dispatch [::events/form-valid])
                           (dispatch [::events/close-add-credential-modal])
                           (dispatch [::events/open-credential-modal
                                      {:subtype "infrastructure-service-registry"} true]))}
             [ui/CardContent {:text-align :center}
              [ui/Header "Docker registry"]
              [:div]
              [ui/IconGroup {:size "massive"}
               [ui/Icon {:name "docker"}]
               [ui/Icon {:name "database", :corner "bottom right"}]]]]

            [ui/Card
             {:on-click #(do
                           (dispatch [::events/set-validate-form? false])
                           (dispatch [::events/form-valid])
                           (dispatch [::events/close-add-credential-modal])
                           (dispatch [::events/open-credential-modal
                                      {:subtype "infrastructure-service-minio"} true]))}
             [ui/CardContent {:text-align :center}
              [ui/Header "Object Store"]
              [:div]
              [ui/Image {:src   "/ui/images/s3.png"
                         :style {:max-height 112}}]]]]]
          ]]))))


(defn generated-credential-modal
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
        [ui/Modal {:open       (boolean @generated-cred)
                   :close-icon true
                   :on-close   #(dispatch [::events/set-generated-credential-modal nil])}

         [ui/ModalHeader "Generated credential"]

         [ui/ModalContent {:scrolling false}

          [ui/Message {:warning true}
           [ui/MessageHeader "Warning"]
           [ui/MessageContent
            [:div
             "Please save this file, since Nuvla will not save it (it's your secret!)."
             [:br]
             " Go to "
             [:a {:href "https://docs.nuvla.io/nuvla/vpn", :target "_blank"} "docs.nuvla.io"]
             " for details on how to configure your OpenVPN client."
             ]]]

          [ui/CardGroup {:centered true}

           [ui/Card
            {:href     (str "data:text/plain;charset=utf-8," (js/encodeURIComponent config))
             :download (str "vpn client " (:name @cred) ".conf")
             :disabled (not config)}
            [ui/CardContent {:text-align :center}
             [ui/Header "Save credential"]
             [ui/Icon {:name "file text"
                       :size :massive}]]]]
          ]]))))


(defn control-bar []
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Menu {:borderless true}
     [uix/MenuItemWithIcon
      {:name      (@tr [:add])
       :icon-name "add"
       :on-click  #(dispatch [::events/open-add-credential-modal])}]
     [main-components/RefreshMenu
      {:on-refresh #(dispatch [::events/get-credentials])}]]))


(defn DeleteButton
  [credential]
  (let [tr      (subscribe [::i18n-subs/tr])
        {:keys [id name description]} credential
        content (str (or name id) (when description " - ") description)]
    [uix/ModalDanger
     {:on-confirm  #(dispatch [::events/delete-credential id])
      :trigger     (r/as-element [ui/Icon {:name  "trash"
                                           :style {:cursor "pointer"}
                                           :color "red"}])
      :content     [:h3 content]
      :header      (@tr [:delete-credential])
      :danger-msg  (@tr [:credential-delete-warning])
      :button-text (@tr [:delete])}]))


;subtype name description
(defn single-credential
  [{:keys [subtype name description] :as credential}]
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
      [DeleteButton credential])

    (when (general-utils/can-edit? credential)
      [ui/Icon {:name     :cog
                :color    :blue
                :style    {:cursor :pointer}
                :on-click #(dispatch [::events/open-credential-modal credential false])}])]])


(defn credentials
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        credentials (subscribe [::subs/credentials])]
    (dispatch [::events/get-credentials])
    (fn []
      (let [infra-service-creds (filter #(in? infrastructure-service-subtypes (:subtype %))
                                        @credentials)]
        [ui/Container {:fluid true}
         [uix/PageHeader "key" (str/capitalize (@tr [:credentials])) :inline true]
         [uix/Accordion
          [:<>
           [:div (@tr [:credential-infra-service-section-sub-text])]
           [control-bar]
           (if (empty? infra-service-creds)
             [ui/Message
              (str/capitalize (str (@tr [:no-credentials]) "."))]
             [:div [ui/Table {:style {:margin-top 10}}
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
   [generated-credential-modal]])
