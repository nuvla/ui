(ns sixsq.nuvla.ui.credentials.utils
  (:require [sixsq.nuvla.ui.credentials.spec :as spec]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [taoensso.timbre :as log]))


(defn get-query-params
  [full-text-search page elements-per-page]
  {:first  (inc (* (dec page) elements-per-page))
   :last   (* page elements-per-page)
   :filter (str "type=='COMPONENT'"
                (when (not-empty full-text-search) (str "and fulltext=='" full-text-search "*'")))})

(defn db->new-swarm-credential
  [db]
  (let [name                    (get-in db [::spec/credential :name])
        description             (get-in db [::spec/credential :description])
        type                    (get-in db [::spec/credential :type])
        ca                      (get-in db [::spec/credential :ca])
        cert                    (get-in db [::spec/credential :cert])
        key                     (get-in db [::spec/credential :key])
        infrastructure-services (get-in db [:infrastructure-services] [])]
    (-> {}
        (assoc :name name)
        (assoc :description description)
        (assoc-in [:template :href] (str "credential-template/" type))
        (assoc-in [:template :infrastructure-services] infrastructure-services)
        (assoc-in [:template :ca] ca)
        (assoc-in [:template :cert] cert)
        (assoc-in [:template :key] key))))


(defn db->new-minio-credential
  [db]
  (let [name                    (get-in db [::spec/credential :name])
        description             (get-in db [::spec/credential :description])
        type                    (get-in db [::spec/credential :type])
        access-key              (get-in db [::spec/credential :access-key])
        secret-key              (get-in db [::spec/credential :secret-key])
        infrastructure-services (get-in db [:infrastructure-services] [])]
    (-> {}
        (assoc :name name)
        (assoc :description description)
        (assoc-in [:template :href] (str "credential-template/" type))
        (assoc-in [:template :infrastructure-services] infrastructure-services)
        (assoc-in [:template :access-key] access-key)
        (assoc-in [:template :secret-key] secret-key))))


(defn db->new-credential
  [db]
  (let [type (get-in db [::spec/credential :type])]
    (case type
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


(defn mandatory-name
  [name]
  [:span name [:sup " " [ui/Icon {:name  :asterisk
                                  :size  :tiny
                                  :color :red}]]])
