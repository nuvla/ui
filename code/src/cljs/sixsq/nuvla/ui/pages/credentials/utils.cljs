(ns sixsq.nuvla.ui.pages.credentials.utils
  (:require [clojure.string :as str]
            [sixsq.nuvla.ui.pages.credentials.spec :as spec]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.time :as time]))

(def ^:const subtype-infrastructure-service-swarm "infrastructure-service-swarm")
(def ^:const subtype-infrastructure-service-kubernetes "infrastructure-service-kubernetes")
(def ^:const subtype-infrastructure-service-vpn "infrastructure-service-vpn")
(def ^:const subtype-gpg-key "gpg-key")
(def ^:const subtype-ssh-key "ssh-key")
(def ^:const subtype-infrastructure-service-minio "infrastructure-service-minio")
(def ^:const subtype-generate-ssh-key "generate-ssh-key")
(def ^:const subtype-generate-api-key "generate-api-key")
(def ^:const subtype-infrastructure-service-registry "infrastructure-service-registry")
(def ^:const subtype-api-key "api-key")
(def ^:const subtype-infrastructure-service-helm-repo "infrastructure-service-helm-repo")

(defn tab->subtypes
  [active-tab]
  (case active-tab
    :coe-services [subtype-infrastructure-service-swarm subtype-infrastructure-service-kubernetes]
    :access-services [subtype-infrastructure-service-vpn subtype-gpg-key subtype-ssh-key subtype-generate-ssh-key]
    :storage-services [subtype-infrastructure-service-minio]
    :registry-services [subtype-infrastructure-service-registry]
    :api-keys [subtype-generate-api-key subtype-api-key]
    :helm-repositories [subtype-infrastructure-service-helm-repo]
    ["unknown"]))

(defn tab->section-sub-text
  [active-tab]
  (case active-tab
    :coe-services :credential-coe-service-section-sub-text
    :access-services :credential-ssh-keys-section-sub-text
    :storage-services :credential-storage-service-section-sub-text
    :registry-services :credential-registry-service-section-sub-text
    :api-keys :api-keys-section-sub-text
    :helm-repositories :helm-repo-section-sub-text
    :unknown-active-tab))

(defn subtype->info
  [subtype]
  (case subtype
    subtype-infrastructure-service-minio {:tab-key :storage-services, :icon icons/i-hard-drive, :name "S3/Minio"}
    subtype-infrastructure-service-swarm {:tab-key :coe-services, :icon icons/i-docker, :name "Docker Swarm"}
    subtype-infrastructure-service-kubernetes {:tab-key :coe-services, :icon icons/i-docker, :name "Kubernetes"}
    subtype-infrastructure-service-registry {:tab-key :registry-services, :icon icons/i-docker, :name "Docker Registry"}
    subtype-infrastructure-service-vpn {:tab-key :access-services, :icon icons/i-key, :name "VPN"}
    subtype-infrastructure-service-helm-repo {:tab-key :helm-repositories, :icon icons/i-key, :name "Helm Repository"}
    subtype-gpg-key {:tab-key :access-services, :icon icons/i-key, :name "GPG keys"}
    subtype-api-key {:tab-key :api-keys, :icon icons/i-key, :name "API keys"}
    subtype-generate-api-key {:tab-key :api-keys, :icon icons/i-key, :name "API keys"}
    subtype-ssh-key {:tab-key :access-services, :icon icons/i-key, :name "SSH keys"}
    subtype-generate-ssh-key {:tab-key :access-services, :icon icons/i-key, :name "SSH keys"}
    {}))

(defn get-cred-count
  [summary tab]
  (let [subtypes-set (set (tab->subtypes tab))]
    (->> (get-in summary [:aggregations :terms:subtype :buckets])
         (keep #(when (subtypes-set (:key %)) (:doc_count %)))
         (reduce + 0))))

(defn db->new-coe-credential
  [db]
  (let [name          (get-in db [::spec/credential :name])
        description   (get-in db [::spec/credential :description])
        parent        (get-in db [::spec/credential :parent])

        ;; subtype of the credential has to match the subtype for the infra-service
        infra-subtype (->> (::spec/infrastructure-services-available db)
                           (filter #(= (:id %) parent))
                           first
                           :subtype)
        subtype       (str "infrastructure-service-" infra-subtype)

        ca            (get-in db [::spec/credential :ca])
        cert          (get-in db [::spec/credential :cert])
        key           (get-in db [::spec/credential :key])
        acl           (get-in db [::spec/credential :acl])]
    (-> {}
        (assoc :name name)
        (assoc :description description)
        (assoc-in [:template :href] (str "credential-template/" subtype))
        (assoc-in [:template :parent] parent)
        (assoc-in [:template :ca] ca)
        (assoc-in [:template :cert] cert)
        (assoc-in [:template :key] key)
        (cond-> acl (assoc-in [:template :acl] acl)))))

(defn db->new-ssh-credential
  [{{:keys [name description subtype public-key private-key acl]} ::spec/credential}]
  (cond-> {:name        name
           :description description
           :template    {:href (str "credential-template/" subtype)}}
          private-key (assoc-in [:template :private-key] private-key)
          public-key (assoc-in [:template :public-key] public-key)
          acl (assoc-in [:template :acl] acl)))

(defn db->new-minio-credential
  [{{:keys [name description subtype access-key secret-key parent acl]} ::spec/credential}]
  (cond-> {:name        name
           :description description
           :template    {:href       (str "credential-template/" subtype)
                         :access-key access-key
                         :secret-key secret-key
                         :parent     (or parent [])}}
          acl (assoc-in [:template :acl] acl)))

(defn db->new-registry-credential
  [{{:keys [name description subtype username password acl parent]} ::spec/credential}]
  (cond-> {:name        name
           :description description
           :template    {:href     (str "credential-template/" subtype)
                         :parent   (or parent [])
                         :username username
                         :password password}}
          acl (assoc-in [:template :acl] acl)))

(defn db->new-helm-repo-credential
  [{{:keys [name description subtype username password acl parent]} ::spec/credential}]
  (cond-> {:name        name
           :description description
           :template    {:href     (str "credential-template/" subtype)
                         :parent   (or parent [])
                         :username username
                         :password password}}
          acl (assoc-in [:template :acl] acl)))

(defn db->new-vpn-credential
  [db]
  (let [name                    (get-in db [::spec/credential :name])
        description             (get-in db [::spec/credential :description])
        infrastructure-services (get-in db [::spec/credential :parent] [])]
    (-> {}
        (assoc :name name)
        (assoc :description description)
        (assoc-in [:template :href] "credential-template/create-credential-vpn-customer")
        (assoc-in [:template :parent] infrastructure-services))))

(defn db->new-api-key
  [db]
  (let [name        (get-in db [::spec/credential :name])
        description (get-in db [::spec/credential :description])
        subtype     (get-in db [::spec/credential :subtype])
        acl         (get-in db [::spec/credential :acl])]
    (-> {}
        (assoc :name name)
        (assoc :description description)
        (assoc-in [:template :href] (str "credential-template/" subtype))
        (cond-> acl (assoc-in [:template :acl] acl)))))

(defn db->new-credential
  [db]
  (let [subtype (get-in db [::spec/credential :subtype])]
    (case subtype
      subtype-infrastructure-service-swarm (db->new-coe-credential db)
      subtype-infrastructure-service-kubernetes (db->new-coe-credential db)
      subtype-infrastructure-service-minio (db->new-minio-credential db)
      subtype-infrastructure-service-vpn (db->new-vpn-credential db)
      subtype-infrastructure-service-registry (db->new-registry-credential db)
      subtype-infrastructure-service-helm-repo (db->new-helm-repo-credential db)
      subtype-generate-ssh-key (db->new-ssh-credential db)
      subtype-generate-api-key (db->new-api-key db)
      subtype-gpg-key (db->new-ssh-credential db))))

(defn vpn-config
  [infra-ca-cert infra-vpn-intermediate-ca cred-vpn-intermediate-ca cred-certificate
   cred-private-key infra-shared-key infra-common-name-prefix infra-vpn-endpoints]
  (when
    (and infra-ca-cert infra-vpn-intermediate-ca cred-vpn-intermediate-ca cred-certificate
         cred-private-key infra-shared-key infra-common-name-prefix infra-vpn-endpoints)
    (str "client\n\ndev vpn\ndev-type tun\n\n\nnobind\n\n# Certificate Configuration\n\n# CA certificate\n<ca>\n"
         infra-ca-cert
         "\n"
         (str/join "\n" infra-vpn-intermediate-ca)
         "\n"
         (str/join "\n" cred-vpn-intermediate-ca)
         "\n</ca>\n\n# Client Certificate\n<cert>\n"
         cred-certificate
         "\n</cert>\n\n# Client Key\n<key>\n"
         cred-private-key
         "\n</key>\n\n# Shared key\n<tls-crypt>\n"
         infra-shared-key
         "\n</tls-crypt>\n\nremote-cert-tls server\n\nverify-x509-name \""
         infra-common-name-prefix
         "\" name-prefix\n\n#script-security 2\n"
         "#tls-verify \"/etc/openvpn/cmd/tls-verify dnQualifier Server\"\n\nauth-nocache\n\n"
         "ping 60\nping-restart 120\n# ping-exit 300\ncompress lz4\n\n"
         (str/join
           "\n\n"
           (map
             #(str "<connection>\nremote "
                   (:endpoint %) " " (:port %) " " (:protocol %)
                   "\n</connection>") infra-vpn-endpoints))
         "\n\n#verb 4\n")))

(defn credential-status-valid
  [{:keys [status] :as _credential}]
  (some-> status (= "VALID")))

(defn credential-is-outdated?
  [{:keys [last-check] :as _credential} delta-minutes]
  (boolean
    (or (nil? last-check)
        (-> last-check
            time/parse-iso8601
            time/delta-minutes
            (> delta-minutes)))))

(defn credential-can-op-check?
  [credential]
  (general-utils/can-operation? "check" credential))

(defn credential-need-check?
  [credential delta-minutes]
  (boolean
    (and
      credential
      (credential-can-op-check? credential)
      (or (not (credential-status-valid credential))
          (credential-is-outdated? credential delta-minutes)))))

(defn show-generated-cred-modal?
  [{{:keys [href private-key]} :template :as _new-credential}]
  (or
    (contains?
      #{"credential-template/create-credential-vpn-customer"
        "credential-template/generate-api-key"}
      href)
    (and (= href "credential-template/generate-ssh-key")
         (nil? private-key))))
