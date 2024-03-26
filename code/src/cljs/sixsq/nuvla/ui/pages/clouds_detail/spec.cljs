(ns sixsq.nuvla.ui.pages.clouds-detail.spec
  (:require [clojure.spec.alpha :as s]
            [sixsq.nuvla.ui.utils.spec :as spec-utils]))


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


(def defaults {::infrastructure-service   nil
               ::infra-service-not-found? false})
