(ns sixsq.nuvla.ui.infra-service.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.utils.spec :as utils-spec]))

(s/def ::services any?)

(s/def ::service any?)

(s/def ::full-text-search (s/nilable string?))

(s/def ::page int?)

(s/def ::elements-per-page int?)

(s/def ::service-modal-visible? boolean?)

(s/def ::add-service-modal-visible? boolean?)

(s/def ::is-new? boolean?)

; General

(s/def ::name utils-spec/nonblank-string)

(s/def ::description utils-spec/nonblank-string)

(s/def ::db (s/keys :req [::services
                          ::full-text-search
                          ::page
                          ::elements-per-page]))

(def defaults {::services                   {}
               ::service-modal-visible?     false
               ::add-service-modal-visible? false
               ::is-new?                    false
               ::full-text-search           nil
               ::page                       1
               ::elements-per-page          8})
