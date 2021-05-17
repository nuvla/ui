(ns sixsq.nuvla.ui.infrastructures-detail.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.utils.spec :as spec-utils]))


(s/def ::loading? boolean?)

(s/def ::name spec-utils/nonblank-string)

(s/def ::parent spec-utils/nonblank-string)

(s/def ::subtype spec-utils/nonblank-string)

(s/def ::description spec-utils/nonblank-string)

(s/def ::endpoint spec-utils/nonblank-string)

(s/def ::infra-service-not-found? boolean?)

(s/def ::infrastructure-service (s/nilable (s/keys :req-un [::name
                                                            ::parent
                                                            ::subtype
                                                            ::endpoint]
                                                   :opt-un [::description])))


(s/def ::db (s/keys :req [::loading?
                          ::infrastructure-service
                          ::infra-service-not-found?]))


(def defaults {::loading?                 true
               ::infrastructure-service   nil
               ::infra-service-not-found? false})
