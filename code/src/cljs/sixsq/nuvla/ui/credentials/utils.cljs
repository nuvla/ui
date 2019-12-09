(ns sixsq.nuvla.ui.credentials.utils
  (:require [clojure.string :as str]
            [sixsq.nuvla.ui.credentials.spec :as spec]))


(defn db->new-swarm-credential
  [db]
  (let [name        (get-in db [::spec/credential :name])
        description (get-in db [::spec/credential :description])
        subtype     (get-in db [::spec/credential :subtype])
        ca          (get-in db [::spec/credential :ca])
        cert        (get-in db [::spec/credential :cert])
        key         (get-in db [::spec/credential :key])
        parent      (get-in db [::spec/credential :parent] [])
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


(defn db->new-credential
  [db]
  (let [subtype (get-in db [::spec/credential :subtype])]
    (case subtype
      "infrastructure-service-swarm" (db->new-swarm-credential db)
      "infrastructure-service-minio" (db->new-minio-credential db)
      "infrastructure-service-vpn" (db->new-vpn-credential db))))


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
