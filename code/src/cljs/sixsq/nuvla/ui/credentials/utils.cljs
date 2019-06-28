(ns sixsq.nuvla.ui.credentials.utils
  (:require [sixsq.nuvla.ui.credentials.spec :as spec]
            [taoensso.timbre :as log]))


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


(defn db->new-credential
  [db]
  (let [subtype (get-in db [::spec/credential :subtype])]
    (case subtype
      "infrastructure-service-swarm" (db->new-swarm-credential db)
      "infrastructure-service-minio" (db->new-minio-credential db))))


; TODO: has moved to utils/general
(defn can-operation? [operation data]
  (->> data :operations (map :rel) (some #{(name operation)}) nil? not))


(defn can-edit? [data]
  (can-operation? :edit data))


(defn editable?
  [module is-new?]
  (or is-new? (can-edit? module)))
