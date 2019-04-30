(ns sixsq.nuvla.ui.credential.utils
  (:require [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.credential.spec :as spec]
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
    (log/infof "->credential type %s" type)
    (log/infof "->credential desc. %s" description)
    (let [c (as-> {} c
                  (dissoc c)
                  (assoc c :name name)
                  (assoc c :description description)
                  (assoc-in c [:template :href] (str "credential-template/" type))
                  (assoc-in c [:template :infrastructure-services] infrastructure-services)
                  (assoc-in c [:template :ca] ca)
                  (assoc-in c [:template :cert] cert)
                  (assoc-in c [:template :key] key))]
      (log/infof "c: %s" c)
      c)))


(defn db->new-minio-credential
  [db]
  (let [name                    (get-in db [::spec/credential :name])
        description             (get-in db [::spec/credential :description])
        type                    (get-in db [::spec/credential :type])
        access-key              (get-in db [::spec/credential :access-key])
        secret-key              (get-in db [::spec/credential :secret-key])
        infrastructure-services (get-in db [:infrastructure-services] [])]
    (let [c (as-> {} c
                  (dissoc c)
                  (assoc c :name name)
                  (assoc c :description description)
                  (assoc-in c [:template :href] (str "credential-template/" type))
                  (assoc-in c [:template :infrastructure-services] infrastructure-services)
                  (assoc-in c [:template :access-key] access-key)
                  (assoc-in c [:template :secret-key] secret-key))]
      (log/infof "c: %s" c)
      c)))


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
