(ns sixsq.nuvla.ui.infra-service.utils
  (:require [clojure.string :as str]
            [sixsq.nuvla.ui.infra-service.spec :as spec]
            [taoensso.timbre :as log]))

(defn get-query-params
  [full-text-search page elements-per-page]
  (let [full-text-search (str "fulltext=='" full-text-search "*'")]
    (cond-> {:first (inc (* (dec page) elements-per-page))
             :last  (* page elements-per-page)}
            (not (str/blank? filter)) (assoc :filter full-text-search))))


(defn db->new-service-group
  [db]
  (let [name        (get-in db [::spec/service :name])
        description (get-in db [::spec/service :description])
        ;documentation (get-in db [::spec/service :documentation] "")
        ]
    (let [g (as-> {} g
                  (assoc g :name name)
                  (assoc g :description description)
                  ;(assoc g :documentation documentation)
                  )]
      (log/infof "g: %s" g)
      g)))


(defn db->new-swarm-service
  [db]
  (let [name        (get-in db [::spec/service :name])
        description (get-in db [::spec/service :description])
        group-id    (get-in db [::spec/service :parent])
        endpoint    (get-in db [::spec/service :endpoint])]
    (log/infof "->service type %s" type)
    (log/infof "->service desc. %s" description)
    (log/infof "->service parent %s" group-id)
    (let [s (as-> {} s
                  (assoc-in s [:template :href] (str "infrastructure-service-template/generic")) ;TODO: no type?
                  (assoc-in s [:template :name] name)
                  (assoc-in s [:template :description] description)
                  (assoc-in s [:template :parent] group-id)
                  (assoc-in s [:template :type] "swarm")
                  (assoc-in s [:template :endpoint] endpoint)
                  (assoc-in s [:template :state] "STARTED") ;TODO: should this not be driven by the real state?
                  )]
      (log/infof "s: %s" s)
      s)))

