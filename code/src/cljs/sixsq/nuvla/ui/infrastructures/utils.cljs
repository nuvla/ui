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
  (let [service-name          (get-in db [::spec/infra-service :name])
        description           (get-in db [::spec/infra-service :description])
        group-id              (get-in db [::spec/infra-service :parent])
        subtype               (get-in db [::spec/infra-service :subtype])
        management-credential (let [mc (get-in db [::spec/infra-service :management-credential])]
                                (if-not (str/blank? mc) mc))
        coe-manager-install   (get-in db [::spec/infra-service :coe-manager-install])
        template-type         (if management-credential "coe" "generic")
        endpoint              (if (= template-type "generic") (get-in db [::spec/infra-service :endpoint]))
        multiplicity          (when-not endpoint (get-in db [::spec/infra-service :multiplicity] 1))
        acl                   (get-in db [::spec/infra-service :acl])]
    (-> {}
        (assoc-in [:template :href] (str "infrastructure-service-template/" template-type))
        (assoc-in [:template :name] service-name)
        (assoc-in [:template :description] description)
        (assoc-in [:template :parent] group-id)
        (assoc-in [:template :subtype] subtype)
        (cond-> acl (assoc-in [:template :acl] acl))
        (cond-> endpoint (assoc-in [:template :endpoint] endpoint))
        (cond-> (and (= template-type "coe") multiplicity) (assoc-in [:template :cluster-params :multiplicity] multiplicity))
        (cond-> (and (= template-type "coe") coe-manager-install) (assoc-in [:template :cluster-params :coe-manager-install] coe-manager-install))
        (cond-> (and (= template-type "coe") management-credential) (assoc-in [:template :management-credential] management-credential)))))