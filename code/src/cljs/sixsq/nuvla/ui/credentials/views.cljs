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
    [sixsq.nuvla.ui.utils.general :as utils-general]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.validation :as utils-validation]
    [taoensso.timbre :as log]))


(defn in?
  "true if coll contains elm"
  [coll elm]
  (some #(= elm %) coll))


(defn row-infrastructure-services-selector
  [subtypes additional-filter editable? value-spec on-change]
  (let [tr              (subscribe [::i18n-subs/tr])
        infra-services  (subscribe [::subs/infrastructure-services-available])
        credential      (subscribe [::subs/credential])
        local-validate? (r/atom false)
        validate-form?  (subscribe [::subs/validate-form?])]
    (dispatch [::events/fetch-infrastructure-services-available subtypes additional-filter])
    (fn [subtypes additional-filter editable? value-spec on-change]
      (let [value     (:parent @credential)
            validate? (or @local-validate? @validate-form?)
            valid?    (s/valid? value-spec value)]
        [ui/TableRow
         [ui/TableCell {:collapsing true}
          (utils-general/mandatory-name (@tr [:infrastructure]))]
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
                                                          [:span "public key " [ui/Icon {:name "info circle"}]])}]),
            :placeholder (@tr [:public-key]), :editable? editable?, :required? false,
            :default-value public-key, :spec ::spec/public-key, :type :textarea,
            :on-change (partial on-change :public-key)]
           [uix/TableRowField "private key", :placeholder (@tr [:private-key]), :editable? editable?,
            :required? false, :default-value private-key, :spec ::spec/private-key, :type :textarea,
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
      (let [editable? (utils-general/editable? @credential @is-new?)
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
            :type :password, :on-change (partial on-change :password)]
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


(defn credential-exoscale
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        is-new?        (subscribe [::subs/is-new?])
        credential     (subscribe [::subs/credential])
        validate-form? (subscribe [::subs/validate-form?])
        on-change      (fn [name-kw value]
                         (dispatch [::events/update-credential name-kw value])
                         (dispatch [::events/validate-credential-form ::spec/exoscale-credential]))]
    (fn []
      (let [editable? (utils-general/editable? @credential @is-new?)
            {:keys [name description exoscale-api-key exoscale-api-secret-key]} @credential]
        [:<>
         [ui/Table style/definition
          [ui/TableBody
           [uix/TableRowField (@tr [:name]), :editable? editable?, :required? true,
            :default-value name, :spec ::spec/name, :validate-form? @validate-form?,
            :on-change (partial on-change :name)]
           [uix/TableRowField (@tr [:description]), :editable? editable?, :required? true,
            :default-value description, :spec ::spec/description, :validate-form? @validate-form?,
            :on-change (partial on-change :description)]
           [uix/TableRowField "api key", :placeholder "Exoscale API key", :editable? editable?, :required? true,
            :default-value exoscale-api-key, :spec ::spec/exoscale-api-key, :validate-form? @validate-form?,
            :on-change (partial on-change :exoscale-api-key)]
           [uix/TableRowField "api secret", :placeholder "Exoscale API secret", :editable? editable?, :required? true,
            :default-value exoscale-api-secret-key, :spec ::spec/exoscale-api-secret-key, :validate-form? @validate-form?,
            :on-change (partial on-change :exoscale-api-secret-key)]]]
         [:div {:style {:color "grey" :font-style "oblique"}} (@tr [:credential-cloud-follow-link])]
         [:a {:href   "https://community.exoscale.com/documentation/iam/quick-start/"
              :target "_blank"}
          (@tr [:nuvlabox-modal-more-info])]]))))


(defn credential-amazonec2
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        is-new?        (subscribe [::subs/is-new?])
        credential     (subscribe [::subs/credential])
        validate-form? (subscribe [::subs/validate-form?])
        on-change      (fn [name-kw value]
                         (dispatch [::events/update-credential name-kw value])
                         (dispatch [::events/validate-credential-form ::spec/amazonec2-credential]))]
    (fn []
      (let [editable? (utils-general/editable? @credential @is-new?)
            {:keys [name description amazonec2-access-key amazonec2-secret-key]} @credential]
        [:<>
         [ui/Table style/definition
          [ui/TableBody
           [uix/TableRowField (@tr [:name]), :editable? editable?, :required? true,
            :default-value name, :spec ::spec/name, :validate-form? @validate-form?,
            :on-change (partial on-change :name)]
           [uix/TableRowField (@tr [:description]), :editable? editable?, :required? true,
            :default-value description, :spec ::spec/description, :validate-form? @validate-form?,
            :on-change (partial on-change :description)]
           [uix/TableRowField "api key", :placeholder "AWS EC2 API key", :editable? editable?, :required? true,
            :default-value amazonec2-access-key, :spec ::spec/amazonec2-access-key, :validate-form? @validate-form?,
            :on-change (partial on-change :amazonec2-access-key)]
           [uix/TableRowField "api secret", :placeholder "AWS EC2 API secret", :editable? editable?, :required? true,
            :default-value amazonec2-secret-key, :spec ::spec/amazonec2-secret-key, :validate-form? @validate-form?,
            :on-change (partial on-change :amazonec2-secret-key)]]]
         [:div {:style {:color "grey" :font-style "oblique"}} (@tr [:credential-cloud-follow-link])]
         [:a {:href   "https://docs.aws.amazon.com/general/latest/gr/managing-aws-access-keys.html"
              :target "_blank"}
          (@tr [:nuvlabox-modal-more-info])]]))))


(defn credential-azure
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        is-new?        (subscribe [::subs/is-new?])
        credential     (subscribe [::subs/credential])
        validate-form? (subscribe [::subs/validate-form?])
        on-change      (fn [name-kw value]
                         (dispatch [::events/update-credential name-kw value])
                         (dispatch [::events/validate-credential-form ::spec/azure-credential]))]
    (fn []
      (let [editable? (utils-general/editable? @credential @is-new?)
            {:keys [name description azure-subscription-id azure-client-id azure-client-secret]} @credential]
        [:<>
         [ui/Table style/definition
          [ui/TableBody
           [uix/TableRowField (@tr [:name]), :editable? editable?, :required? true,
            :default-value name, :spec ::spec/name, :validate-form? @validate-form?,
            :on-change (partial on-change :name)]
           [uix/TableRowField (@tr [:description]), :editable? editable?, :required? true,
            :default-value description, :spec ::spec/description, :validate-form? @validate-form?,
            :on-change (partial on-change :description)]
           [uix/TableRowField "subscription id", :placeholder "Azure Subscription ID", :editable? editable?, :required? true,
            :default-value azure-subscription-id, :spec ::spec/azure-subscription-id, :validate-form? @validate-form?,
            :on-change (partial on-change :azure-subscription-id)]
           [uix/TableRowField "client id", :placeholder "Azure Client ID", :editable? editable?, :required? true,
            :default-value azure-client-id, :spec ::spec/azure-client-id, :validate-form? @validate-form?,
            :on-change (partial on-change :azure-client-id)]
           [uix/TableRowField "client secret", :placeholder "Azure Client Secret", :editable? editable?, :required? true,
            :default-value azure-client-secret, :spec ::spec/azure-client-secret, :validate-form? @validate-form?,
            :on-change (partial on-change :azure-client-secret)]]]
         [:div {:style {:color "grey" :font-style "oblique"}} (@tr [:credential-cloud-follow-link])]
         [:a {:href   "https://www.inkoop.io/blog/how-to-get-azure-api-credentials"
              :target "_blank"}
          (@tr [:nuvlabox-modal-more-info])]]))))


(defn credential-google
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        is-new?        (subscribe [::subs/is-new?])
        credential     (subscribe [::subs/credential])
        validate-form? (subscribe [::subs/validate-form?])
        on-change      (fn [name-kw value]
                         (dispatch [::events/update-credential name-kw value])
                         (dispatch [::events/validate-credential-form ::spec/google-credential]))]
    (fn []
      (let [editable? (utils-general/editable? @credential @is-new?)
            {:keys [name description google-username client-id client-secret refresh-token]} @credential]
        [:<>
         [ui/Table style/definition
          [ui/TableBody
           [uix/TableRowField (@tr [:name]), :editable? editable?, :required? true,
            :default-value name, :spec ::spec/name, :validate-form? @validate-form?,
            :on-change (partial on-change :name)]
           [uix/TableRowField (@tr [:description]), :editable? editable?, :required? true,
            :default-value description, :spec ::spec/description, :validate-form? @validate-form?,
            :on-change (partial on-change :description)]
           [uix/TableRowField "username", :placeholder "Google Username", :editable? editable?, :required? true,
            :default-value google-username, :spec ::spec/google-username, :validate-form? @validate-form?,
            :on-change (partial on-change :google-username)]
           [uix/TableRowField "client id", :placeholder "Google Client ID", :editable? editable?, :required? true,
            :default-value client-id, :spec ::spec/client-id, :validate-form? @validate-form?,
            :on-change (partial on-change :client-id)]
           [uix/TableRowField "client secret", :placeholder "Google Client Secret", :editable? editable?, :required? true,
            :default-value client-secret, :spec ::spec/client-secret, :validate-form? @validate-form?,
            :on-change (partial on-change :client-secret)]
           [uix/TableRowField "refresh token", :placeholder "Google Refresh Token", :editable? editable?, :required? true,
            :default-value refresh-token, :spec ::spec/refresh-token, :validate-form? @validate-form?,
            :on-change (partial on-change :refresh-token)]
           ]]
         [:div {:style {:color "grey" :font-style "oblique"}} (@tr [:credential-cloud-follow-link])]
         [:a {:href   "https://cloud.google.com/docs/authentication/production"
              :target "_blank"}
          (@tr [:nuvlabox-modal-more-info])]]))))


(defn save-callback
  [form-validation-spec]
  (dispatch-sync [::events/set-validate-form? true])
  (dispatch-sync [::events/validate-credential-form form-validation-spec])
  (let [form-valid? (get @re-frame.db/app-db ::spec/form-valid?)]
    (when form-valid?
      (do
        (dispatch [::events/set-validate-form? false])
        (dispatch [::events/edit-credential])))))


(def infrastructure-service-csp-validation-map
  {"infrastructure-service-exoscale"
   {:validation-spec ::spec/exoscale-credential
    :modal-content   credential-exoscale}
   "infrastructure-service-amazonec2"
   {:validation-spec ::spec/amazonec2-credential
    :modal-content   credential-amazonec2}
   "infrastructure-service-azure"
   {:validation-spec ::spec/azure-credential
    :modal-content   credential-azure}
   "infrastructure-service-google"
   {:validation-spec ::spec/google-credential
    :modal-content   credential-google}})


(def infrastructure-service-csp-subtypes
  (keys infrastructure-service-csp-validation-map))


(def infrastructure-service-storage-validation-map
  {"infrastructure-service-minio"
   {:validation-spec ::spec/minio-credential
    :modal-content   credential-object-store}})


(def infrastructure-service-storage-subtypes
  (keys infrastructure-service-storage-validation-map))


(def infrastructure-service-coe-validation-map
  {"infrastructure-service-swarm"
   {:validation-spec ::spec/coe-credential
    :modal-content   credential-coe},
   "infrastructure-service-kubernetes"
   {:validation-spec ::spec/coe-credential
    :modal-content   credential-coe}})


(def coe-subtypes
  (keys infrastructure-service-coe-validation-map))


(def infrastructure-service-registry-validation-map
  {"infrastructure-service-registry"
   {:validation-spec ::spec/registry-credential
    :modal-content   credential-registy}})


(def registry-service-subtypes
  (keys infrastructure-service-registry-validation-map))


(def api-key-validation-map
  {"api-key"
   {:validation-spec ::spec/registry-credential
    :modal-content   credential-registy}})


(def api-key-subtypes
  (keys api-key-validation-map))


(def infrastructure-service-access-keys-validation-map
  {"infrastructure-service-vpn"
   {:validation-spec ::spec/vpn-credential
    :modal-content   credential-vpn}
   "ssh-key"
   {:validation-spec ::spec/ssh-credential
    :modal-content   credential-ssh}
   "generate-ssh-key"
   {:validation-spec ::spec/ssh-credential
    :modal-content   credential-ssh}
   "hashed-password"
   {:validation-spec ::spec/hashed-password
    :modal-content   credential-ssh}})


(def access-keys-subtypes
  (keys infrastructure-service-access-keys-validation-map))


(def infrastructure-service-validation-map
  (merge infrastructure-service-csp-validation-map
         infrastructure-service-storage-validation-map
         infrastructure-service-coe-validation-map
         infrastructure-service-access-keys-validation-map
         infrastructure-service-registry-validation-map
         api-key-validation-map))


(def tab-indices
  {:coe-services      0
   :cloud-services    1
   :access-services   2
   :storage-services  3
   :registry-services 4
   :api-keys          5})


(defn subtype->info
  [subtype]
  (case subtype
    "infrastructure-service-minio" {:tab-index (:storage-services tab-indices), :icon "disk", :name "S3/Minio"}
    "infrastructure-service-swarm" {:tab-index (:coe-services tab-indices), :icon "docker", :name "Docker Swarm"}
    "infrastructure-service-kubernetes" {:tab-index (:coe-services tab-indices), :icon "docker", :name "Kubernetes"}
    "infrastructure-service-registry"
    {:tab-index (:registry-services tab-indices), :icon "docker", :name "Docker Registry"}
    "infrastructure-service-azure" {:tab-index (:cloud-services tab-indices), :icon "cloud", :name "Microsoft Azure"}
    "infrastructure-service-google" {:tab-index (:cloud-services tab-indices), :icon "cloud", :name "Google Compute"}
    "infrastructure-service-amazonec2" {:tab-index (:cloud-services tab-indices), :icon "cloud", :name "AWS EC2"}
    "infrastructure-service-exoscale" {:tab-index (:cloud-services tab-indices), :icon "cloud", :name "Exoscale"}
    "infrastructure-service-vpn" {:tab-index (:access-services tab-indices), :icon "key", :name "VPN"}
    "ssh-key" {:tab-index (:access-services tab-indices), :icon "key", :name "SSH keys"}
    "api-key" {:tab-index (:api-keys tab-indices), :icon "key", :name "API keys"}
    "generate-ssh-key" {:tab-index (:access-services tab-indices), :icon "key", :name "SSH keys"}
    {:tab-index 0, :icon "cloud", :name ""}))


(defn extract-metrics
  [terms subtypes]
  (apply + (-> terms (select-keys (map keyword subtypes)) vals)))

(defn StatisticStates
  ([] [StatisticStates true])
  ([clickable?]
   (let [tr         (subscribe [::i18n-subs/tr])
         summary    (subscribe [::subs/credentials-summary])
         open-popup (r/atom true)]
     ;(time/sleep 5000 #(reset! open-popup false))
     (fn [clickable?]
       (let [terms      (utils-general/aggregate-to-map (get-in @summary [:aggregations :terms:subtype :buckets]))
             coe        (extract-metrics terms coe-subtypes)
             csp        (extract-metrics terms infrastructure-service-csp-subtypes)
             access-key (extract-metrics terms access-keys-subtypes)
             storage    (extract-metrics terms infrastructure-service-storage-subtypes)
             registry   (extract-metrics terms registry-service-subtypes)
             api-key    (extract-metrics terms api-key-subtypes)
             total      (:count @summary)
             ]
         [:div {:style {:margin     "10px auto 10px auto"
                        :text-align "center"
                        :width      "100%"}}
          [ui/StatisticGroup (merge {:widths (if clickable? nil 5) :size "tiny"}
                                    {:style {:margin-right "0px"
                                             :display      "block"}})
           [main-components/StatisticState total ["key"] "TOTAL" clickable?
            ::events/set-state-selector ::subs/state-selector]
           [main-components/StatisticState coe ["docker"] "DOCKER/K8S" clickable?
            ::events/set-state-selector ::subs/state-selector]
           [main-components/StatisticState csp ["cloud"] "CLOUDS" clickable?
            ::events/set-state-selector ::subs/state-selector]
           [main-components/StatisticState access-key ["key"] "REMOTE ACCESS" clickable?
            ::events/set-state-selector ::subs/state-selector]
           [main-components/StatisticState storage ["disk"] "STORAGE" clickable?
            ::events/set-state-selector ::subs/state-selector]
           [main-components/StatisticState registry ["docker"] "REGISTRY" clickable?
            ::events/set-state-selector ::subs/state-selector]
           (if clickable?
             [ui/Popup {:trigger  (r/as-element
                                    [main-components/StatisticState api-key ["key"] "API KEYS" clickable?
                                     ::events/set-state-selector ::subs/state-selector])
                        :open     @open-popup
                        :position "right center"
                        :offset   [0 20]}
              [ui/PopupContent
               [:span [ui/Icon {:name "arrow left"}] (@tr [:statistics-select-info])]]]
             [main-components/StatisticState api-key ["key"] "API KEYS" clickable?
              ::events/set-state-selector ::subs/state-selector])
           ]])))))


(defn credential-modal
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        visible?    (subscribe [::subs/credential-modal-visible?])
        form-valid? (subscribe [::subs/form-valid?])
        credential  (subscribe [::subs/credential])
        is-new?     (subscribe [::subs/is-new?])]
    (fn []
      (let [subtype          (:subtype @credential "")
            active-tab-index (:tab-index (subtype->info subtype))
            icon             (:icon (subtype->info subtype))
            name             (:name (subtype->info subtype))
            header           (str (str/capitalize (str (if @is-new?
                                                         (@tr [:new])
                                                         (@tr [:update]))))
                                  " " name " " (@tr [:credential]))
            validation-item  (get infrastructure-service-validation-map subtype)
            validation-spec  (:validation-spec validation-item)
            modal-content    (:modal-content validation-item)]
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
            [uix/Button {:text     (if (true? @is-new?) (@tr [:create]) (@tr [:save]))
                         :positive true
                         :disabled (when-not @form-valid? true)
                         :active   true
                         :on-click #(do (save-callback validation-spec)
                                        (dispatch [::events/set-active-tab-index
                                                   active-tab-index]))}]]])))))


(defn add-credential-modal
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        visible?  (subscribe [::subs/add-credential-modal-visible?])
        is-group? (subscribe [::session-subs/active-claim-is-group?])]
    (fn []
      (let []
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-add-credential-modal])}

         [uix/ModalHeader {:header (@tr [:add]) :icon "add"}]

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
                                     {:subtype "infrastructure-service-swarm"} true]))}
            [ui/CardContent {:text-align :center}
             [ui/Header "Swarm / Kubernetes"]
             [ui/Icon {:name "docker"
                       :size :massive}]
             [ui/Image {:src   "/ui/images/kubernetes.svg"
                        :style {:max-width 112}}]]]

           [ui/Card
            (when (not @is-group?)
              {:on-click #(do
                            (dispatch [::events/set-validate-form? false])
                            (dispatch [::events/form-valid])
                            (dispatch [::events/close-add-credential-modal])
                            (dispatch [::events/open-credential-modal
                                       {:subtype "infrastructure-service-vpn"} true]))})
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
                        :style {:max-height 112}}]]]

           [ui/Card
            {:on-click #(do
                          (dispatch [::events/set-validate-form? false])
                          (dispatch [::events/form-valid])
                          (dispatch [::events/close-add-credential-modal])
                          (dispatch [::events/open-credential-modal
                                     {:subtype "generate-ssh-key"} true]))}
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
                                     {:subtype "infrastructure-service-exoscale"} true]))}
            [ui/CardContent {:text-align :center}
             [ui/Header "Cloud Exoscale"]
             [ui/Image {:src   "/ui/images/exoscale.png"
                        :style {:max-width 112}}]]]

           [ui/Card
            {:on-click #(do
                          (dispatch [::events/set-validate-form? false])
                          (dispatch [::events/form-valid])
                          (dispatch [::events/close-add-credential-modal])
                          (dispatch [::events/open-credential-modal
                                     {:subtype "infrastructure-service-amazonec2"} true]))}
            [ui/CardContent {:text-align :center}
             [ui/Header "Cloud Amazon"]
             [ui/Image {:src   "/ui/images/aws.png"
                        :style {:max-width 112}}]]]

           [ui/Card
            {:on-click #(do
                          (dispatch [::events/set-validate-form? false])
                          (dispatch [::events/form-valid])
                          (dispatch [::events/close-add-credential-modal])
                          (dispatch [::events/open-credential-modal
                                     {:subtype "infrastructure-service-azure"} true]))}
            [ui/CardContent {:text-align :center}
             [ui/Header "Cloud Azure"]
             [ui/Image {:src   "/ui/images/azure.png"
                        :style {:max-width 112}}]]]

           [ui/Card
            {:on-click #(do
                          (dispatch [::events/set-validate-form? false])
                          (dispatch [::events/form-valid])
                          (dispatch [::events/close-add-credential-modal])
                          (dispatch [::events/open-credential-modal
                                     {:subtype "infrastructure-service-google"} true]))}
            [ui/CardContent {:text-align :center}
             [ui/Header "Cloud Google"]
             [ui/Image {:src   "/ui/images/gce.png"
                        :style {:max-width 112}}]]]]]]))))


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

         [uix/ModalHeader {:header (@tr [:credential-generate])}]

         [ui/ModalContent {:scrolling false}

          [ui/Message {:warning true}
           [ui/MessageHeader (@tr [:warning])]
           [ui/MessageContent
            [:div
             (@tr [:credential-please-save-this-file])
             [:br]
             " "
             (@tr [:credential-go-to])
             " "
             [:a {:href "https://docs.nuvla.io/nuvla/vpn", :target "_blank"} (@tr [:documentation])]
             " "
             (@tr [:credential-for-details-vpn])]]]

          [ui/CardGroup {:centered true}

           [ui/Card
            {:href     (str "data:text/plain;charset=utf-8," (js/encodeURIComponent config))
             :download (str "vpn client " (:name @cred) ".conf")
             :disabled (not config)}
            [ui/CardContent {:text-align :center}
             [ui/Header (@tr [:credential-save])]
             [ui/Icon {:name "file text"
                       :size :massive}]]]]]]))))


(defn MenuBar []
  (let [tr (subscribe [::i18n-subs/tr])]
    [main-components/StickyBar
     [ui/Menu {:borderless true}
      [uix/MenuItem
       {:name     (@tr [:add])
        :icon     "add"
        :on-click #(dispatch [::events/open-add-credential-modal])}]
      [main-components/RefreshMenu
       {:on-refresh #(dispatch [::events/get-credentials])}]]]))


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

    (when (utils-general/can-delete? credential)
      [DeleteButton credential])

    (when (utils-general/can-edit? credential)
      [ui/Icon {:name     :cog
                :color    :blue
                :style    {:cursor :pointer}
                :on-click #(dispatch [::events/open-credential-modal credential false])}])]])


(defn credentials-pane
  [section-sub-text credentials]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/TabPane
     [:div (@tr [section-sub-text])]
     (if (empty? credentials)
       [ui/Message
        (str/capitalize (str (@tr [:no-credentials]) "."))]
       [:div [ui/Table {:style {:margin-top 10}}
              [ui/TableHeader
               [ui/TableRow
                [ui/TableHeaderCell {:content (str/capitalize (@tr [:name]))}]
                [ui/TableHeaderCell {:content (str/capitalize (@tr [:description]))}]
                [ui/TableHeaderCell {:content (str/capitalize (@tr [:type]))}]
                [ui/TableHeaderCell {:content (str/capitalize (@tr [:actions]))}]]]
              [ui/TableBody
               (for [credential credentials]
                 ^{:key (:id credential)}
                 [single-credential credential])]]])]))


(defn credential
  [credentials section-name section-sub-text icon]
  (let [tr               (subscribe [::i18n-subs/tr])
        credential-count (count credentials)]
    {:menuItem {:content (r/as-element [:span (@tr [section-name])
                                        (when (> credential-count 0) [ui/Label {:circular true
                                                                                :size     "mini"
                                                                                :attached "top right"}
                                                                      credential-count])])
                :key     section-name
                :icon    icon}
     :render   (fn [] (r/as-element [credentials-pane section-sub-text credentials]))}))


(defn credentials
  []
  (let [credentials            (subscribe [::subs/credentials])
        coe-service-creds      (filter #(in? coe-subtypes (:subtype %))
                                       @credentials)
        cloud-service-creds    (filter #(in? infrastructure-service-csp-subtypes (:subtype %))
                                       @credentials)
        access-key-creds       (filter #(in? access-keys-subtypes (:subtype %))
                                       @credentials)
        storage-service-creds  (filter #(in? infrastructure-service-storage-subtypes (:subtype %))
                                       @credentials)
        register-service-creds (filter #(in? registry-service-subtypes (:subtype %))
                                       @credentials)
        api-key-creds          (filter #(in? api-key-subtypes (:subtype %))
                                       @credentials)]
    [(credential coe-service-creds :coe-services :credential-coe-service-section-sub-text "docker")
     (credential cloud-service-creds :cloud-services :credential-cloud-service-section-sub-text "cloud")
     (credential access-key-creds :access-keys :credential-ssh-keys-section-sub-text "key")
     (credential storage-service-creds :storage-services :credential-storage-service-section-sub-text "disk")
     (credential register-service-creds :registry-services :credential-registry-service-section-sub-text "docker")
     (credential api-key-creds :api-keys :api-keys-section-sub-text "key")]))


(defn TabsCredentials
  []
  (dispatch [::events/get-credentials])
  (fn []
    (let [active-index (subscribe [::subs/active-tab-index])]
      [ui/Tab
       {:menu        {:secondary true
                      :pointing  true
                      :style     {:display        "flex"
                                  :flex-direction "row"
                                  :flex-wrap      "wrap"}}
        :panes       (credentials)
        :activeIndex @active-index
        :onTabChange (fn [_ data]
                       (let [active-index (. data -activeIndex)]
                         (dispatch [::events/set-active-tab-index active-index])))}])))


(defmethod panel/render :credentials
  [path]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Segment style/basic
     [uix/PageHeader "key" (@tr [:credentials])]
     [MenuBar]
     [TabsCredentials]
     [add-credential-modal]
     [credential-modal]
     [generated-credential-modal]]))
