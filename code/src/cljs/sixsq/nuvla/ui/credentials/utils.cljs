(ns sixsq.nuvla.ui.credentials.utils
  (:require [clojure.string :as str]
            [sixsq.nuvla.ui.credentials.spec :as spec]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.time :as time]))


(defn db->new-coe-credential
  [db]
  (let [name        (get-in db [::spec/credential :name])
        description (get-in db [::spec/credential :description])
        parent      (get-in db [::spec/credential :parent])

        ;; subtype of the credential has to match the subtype for the infra-service
        infra-subtype (->> (::spec/infrastructure-services-available db)
                           (filter #(= (:id %) parent))
                           first
                           :subtype)
        subtype (str "infrastructure-service-" infra-subtype)

        ca          (get-in db [::spec/credential :ca])
        cert        (get-in db [::spec/credential :cert])
        key         (get-in db [::spec/credential :key])
        acl         (get-in db [::spec/credential :acl])]
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
  [db]
  (let [name        (get-in db [::spec/credential :name])
        description (get-in db [::spec/credential :description])
        subtype     (get-in db [::spec/credential :subtype])
        public-key  (get-in db [::spec/credential :public-key])
        private-key (get-in db [::spec/credential :private-key])
        acl         (get-in db [::spec/credential :acl])]
    (-> {}
        (assoc :name name)
        (assoc :description description)
        (assoc-in [:template :href] (str "credential-template/" subtype))
        (cond-> private-key (assoc-in [:template :private-key] private-key))
        (cond-> public-key (assoc-in [:template :public-key] public-key))
        (cond-> acl (assoc-in [:template :acl] acl)))))


(defn db->new-minio-credential
  [db]
  (let [name                    (get-in db [::spec/credential :name])
        description             (get-in db [::spec/credential :description])
        subtype                 (get-in db [::spec/credential :subtype])
        access-key              (get-in db [::spec/credential :access-key])
        secret-key              (get-in db [::spec/credential :secret-key])
        infrastructure-services (get-in db [::spec/credential :parent] [])
        acl                     (get-in db [::spec/credential :acl])]
    (-> {}
        (assoc :name name)
        (assoc :description description)
        (assoc-in [:template :href] (str "credential-template/" subtype))
        (assoc-in [:template :parent] infrastructure-services)
        (assoc-in [:template :access-key] access-key)
        (assoc-in [:template :secret-key] secret-key)
        (cond-> acl (assoc-in [:template :acl] acl)))))


(defn db->new-registry-credential
  [db]
  (let [name                    (get-in db [::spec/credential :name])
        description             (get-in db [::spec/credential :description])
        subtype                 (get-in db [::spec/credential :subtype])
        username                (get-in db [::spec/credential :username])
        password                (get-in db [::spec/credential :password])
        infrastructure-services (get-in db [::spec/credential :parent] [])
        acl                     (get-in db [::spec/credential :acl])]
    (-> {}
        (assoc :name name)
        (assoc :description description)
        (assoc-in [:template :href] (str "credential-template/" subtype))
        (assoc-in [:template :parent] infrastructure-services)
        (assoc-in [:template :username] username)
        (assoc-in [:template :password] password)
        (cond-> acl (assoc-in [:template :acl] acl)))))


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


(defn db->new-exoscale-credential
  [db]
  (let [name        (get-in db [::spec/credential :name])
        description (get-in db [::spec/credential :description])
        subtype     (get-in db [::spec/credential :subtype])
        secret      (get-in db [::spec/credential :exoscale-api-secret-key])
        key         (get-in db [::spec/credential :exoscale-api-key])
        acl         (get-in db [::spec/credential :acl])]
    (-> {}
        (assoc :name name)
        (assoc :description description)
        (assoc-in [:template :href] (str "credential-template/store-" subtype))
        (assoc-in [:template :exoscale-api-key] key)
        (assoc-in [:template :exoscale-api-secret-key] secret)
        (cond-> acl (assoc-in [:template :acl] acl)))))


(defn db->new-amazonec2-credential
  [db]
  (let [name        (get-in db [::spec/credential :name])
        description (get-in db [::spec/credential :description])
        subtype     (get-in db [::spec/credential :subtype])
        key         (get-in db [::spec/credential :amazonec2-access-key])
        secret      (get-in db [::spec/credential :amazonec2-secret-key])
        acl         (get-in db [::spec/credential :acl])]
    (-> {}
        (assoc :name name)
        (assoc :description description)
        (assoc-in [:template :href] (str "credential-template/store-" subtype))
        (assoc-in [:template :amazonec2-access-key] key)
        (assoc-in [:template :amazonec2-secret-key] secret)
        (cond-> acl (assoc-in [:template :acl] acl)))))


(defn db->new-azure-credential
  [db]
  (let [name        (get-in db [::spec/credential :name])
        description (get-in db [::spec/credential :description])
        subtype     (get-in db [::spec/credential :subtype])
        subsciption (get-in db [::spec/credential :azure-subscription-id])
        id          (get-in db [::spec/credential :azure-client-id])
        secret      (get-in db [::spec/credential :azure-client-secret])
        acl         (get-in db [::spec/credential :acl])]
    (-> {}
        (assoc :name name)
        (assoc :description description)
        (assoc-in [:template :href] (str "credential-template/store-" subtype))
        (assoc-in [:template :azure-subscription-id] subsciption)
        (assoc-in [:template :azure-client-id] id)
        (assoc-in [:template :azure-client-secret] secret)
        (cond-> acl (assoc-in [:template :acl] acl)))))


(defn db->new-google-credential
  [db]
  (let [name        (get-in db [::spec/credential :name])
        description (get-in db [::spec/credential :description])
        subtype     (get-in db [::spec/credential :subtype])
        username    (get-in db [::spec/credential :google-username])
        id          (get-in db [::spec/credential :client-id])
        secret      (get-in db [::spec/credential :client-secret])
        token       (get-in db [::spec/credential :refresh-token])
        acl         (get-in db [::spec/credential :acl])]
    (-> {}
        (assoc :name name)
        (assoc :description description)
        (assoc-in [:template :href] (str "credential-template/store-" subtype))
        (assoc-in [:template :google-username] username)
        (assoc-in [:template :client-id] id)
        (assoc-in [:template :client-secret] secret)
        (assoc-in [:template :refresh-token] token)
        (cond-> acl (assoc-in [:template :acl] acl)))))


(defn db->new-credential
  [db]
  (let [subtype (get-in db [::spec/credential :subtype])]
    (case subtype
      "infrastructure-service-swarm" (db->new-coe-credential db)
      "infrastructure-service-kubernetes" (db->new-coe-credential db)
      "infrastructure-service-minio" (db->new-minio-credential db)
      "infrastructure-service-vpn" (db->new-vpn-credential db)
      "infrastructure-service-exoscale" (db->new-exoscale-credential db)
      "infrastructure-service-amazonec2" (db->new-amazonec2-credential db)
      "infrastructure-service-azure" (db->new-azure-credential db)
      "infrastructure-service-google" (db->new-google-credential db)
      "infrastructure-service-registry" (db->new-registry-credential db)
      "generate-ssh-key" (db->new-ssh-credential db))))


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



(defn credential-last-check-ago
  [{:keys [last-check] :as credential} locale]
  (some-> last-check time/parse-iso8601 (time/ago locale)))


(defn credential-status-valid
  [{:keys [status] :as credential}]
  (some-> status (= "VALID")))


(defn credential-is-outdated?
  [{:keys [last-check] :as credential} delta-minutes]
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


(defn credential-check-status
  [loading? invalid?]
  (cond
    loading? :loading
    invalid? :warning
    :else :ok))
