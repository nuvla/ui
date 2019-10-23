(ns sixsq.nuvla.ui.infrastructures.utils
  (:require [clojure.string :as str]
            [sixsq.nuvla.ui.infrastructures.spec :as spec]))

(defn get-query-params
  [page elements-per-page]
  {:first   (inc (* (dec page) elements-per-page))
   :last    (* page elements-per-page)
   :select  "id, name"
   :orderby "created:desc"})


(defn db->new-service-group
  [db]
  (let [name        (get-in db [::spec/infra-service :name])
        description (get-in db [::spec/infra-service :description])]
    {:name        name
     :description description}))


(defn db->new-service
  [db]
  (let [service-name    (get-in db [::spec/infra-service :name])
        description     (get-in db [::spec/infra-service :description])
        group-id        (get-in db [::spec/infra-service :parent])
        endpoint        (get-in db [::spec/infra-service :endpoint])
        service-subtype (get-in db [::spec/infra-service :subtype])
        acl             (get-in db [::spec/infra-service :acl])]
    (-> {}
        (assoc-in [:template :href] (str "infrastructure-service-template/generic"))
        (assoc-in [:template :name] service-name)
        (assoc-in [:template :description] description)
        (assoc-in [:template :parent] group-id)
        (assoc-in [:template :subtype] service-subtype)
        (assoc-in [:template :endpoint] endpoint)
        (cond-> acl (assoc-in [:template :acl] acl))
        )))