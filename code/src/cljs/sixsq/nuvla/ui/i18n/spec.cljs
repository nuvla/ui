(ns sixsq.nuvla.ui.i18n.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.i18n.utils :as utils]))

(s/def ::locale string?)

(s/def ::tr fn?)

(s/def ::theme-dictionary any?)

(s/def ::db (s/keys :req [::locale ::theme-dictionary ::tr]))

(def defaults {::locale           "en"
               ::theme-dictionary nil
               ::tr               (utils/create-tr-fn "en")})
