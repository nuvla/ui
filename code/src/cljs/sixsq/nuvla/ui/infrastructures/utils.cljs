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


(def infra-service-subtype-exoscale "infrastructure-service-exoscale")
(def infra-service-subtype-google "infrastructure-service-google")
;; TODO: This needs to come from the server as configuration defaults.
;; Probably from template(s) or better a configuration resource?
(def cloud-params-defaults
  {infra-service-subtype-exoscale
   {:cloud-vm-size "Small"
    :cloud-vm-disk-size 50
    :cloud-region "CH-DK-2"
    :cloud-vm-image "Linux Ubuntu 18.04 LTS 64-bit"
    :cloud-security-group "docker_machine"
    :cloud-doc-link "https://exoscale.com"}
   "infrastructure-service-amazonec2"
   {:cloud-vm-size "t2.medium"
    :cloud-vm-disk-size 50
    :cloud-region "us-east-1"
    :cloud-vm-image "ami-927185ef"
    :cloud-doc-link "https://docs.aws.amazon.com/ec2"}
   "infrastructure-service-azure"
   {:cloud-vm-size "Standard_A2"
    :cloud-region "francecentral"
    :cloud-vm-image "canonical:UbuntuServer:16.04.0-LTS:latest"
    :cloud-doc-link "https://docs.microsoft.com/en-us/azure"}
   infra-service-subtype-google
   {:cloud-vm-size "e2-medium"
    :cloud-vm-disk-size 50
    :cloud-region "europe-west3-a"
    :cloud-project ""
    :cloud-vm-image "ubuntu-os-cloud/global/images/ubuntu-1804-bionic-v20200610"
    :cloud-doc-link "https://cloud.google.com/docs"}
   })

(def cloud-params-keys (->> cloud-params-defaults
                            (map #(vec (keys (val %))))
                            flatten
                            set
                            vec))


(defn cloud-param-default-value
  [mgmt-cred-subtype param-kw]
  (if mgmt-cred-subtype
    (param-kw (get cloud-params-defaults mgmt-cred-subtype))
    ""))


(defn db->new-service
  [db]
  (let [service-name          (get-in db [::spec/infra-service :name])
        description           (get-in db [::spec/infra-service :description])
        group-id              (get-in db [::spec/infra-service :parent])
        subtype               (get-in db [::spec/infra-service :subtype])
        management-credential (let [mc (get-in db [::spec/infra-service :management-credential])]
                                (if-not (str/blank? mc) mc))
        mgmt-cred-subtype     (->> (::spec/management-credentials-available db)
                                   (filter (comp #{management-credential} :id))
                                   first
                                   :subtype)
        template-type         (if management-credential "coe" "generic")
        endpoint              (if (= template-type "generic") (get-in db [::spec/infra-service :endpoint]))
        multiplicity          (when-not endpoint (get-in db [::spec/infra-service :multiplicity] 1))
        coe-manager-install   (when-not endpoint (get-in db [::spec/infra-service :coe-manager-install] false))
        ssh-keys              (when-not endpoint (get-in db [::spec/infra-service :ssh-keys]))
        cloud-vm-image        (get-in db [::spec/infra-service :cloud-vm-image] (cloud-param-default-value mgmt-cred-subtype :cloud-vm-image))
        cloud-vm-size         (get-in db [::spec/infra-service :cloud-vm-size] (cloud-param-default-value mgmt-cred-subtype :cloud-vm-size))
        cloud-vm-disk-size    (get-in db [::spec/infra-service :cloud-vm-disk-size] (cloud-param-default-value mgmt-cred-subtype :cloud-vm-disk-size))
        cloud-region          (get-in db [::spec/infra-service :cloud-region] (cloud-param-default-value mgmt-cred-subtype :cloud-region))
        cloud-project          (get-in db [::spec/infra-service :cloud-project] (cloud-param-default-value mgmt-cred-subtype :cloud-project))
        cloud-security-group  (get-in db [::spec/infra-service :cloud-security-group])
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
        (cond-> (and (= template-type "coe") (not-empty ssh-keys)) (assoc-in [:template :cluster-params :ssh-keys] ssh-keys))
        (cond-> (and (= template-type "coe") (not-empty cloud-vm-image)) (assoc-in [:template :cluster-params :cloud-vm-image] cloud-vm-image))
        (cond-> (and (= template-type "coe") (not-empty cloud-vm-size)) (assoc-in [:template :cluster-params :cloud-vm-size] cloud-vm-size))
        (cond-> (and (= template-type "coe") cloud-vm-disk-size) (assoc-in [:template :cluster-params :cloud-vm-disk-size] cloud-vm-disk-size))
        (cond-> (and (= template-type "coe") (not-empty cloud-region)) (assoc-in [:template :cluster-params :cloud-region] cloud-region))
        (cond-> (and (= template-type "coe") (not-empty cloud-project)) (assoc-in [:template :cluster-params :cloud-project] cloud-project))
        (cond-> (and (= template-type "coe") (not-empty cloud-security-group)) (assoc-in [:template :cluster-params :cloud-security-group] cloud-security-group))
        (cond-> (and (= template-type "coe") coe-manager-install) (assoc-in [:template :cluster-params :coe-manager-install] coe-manager-install))
        (cond-> (and (= template-type "coe") management-credential) (assoc-in [:template :management-credential] management-credential)))))