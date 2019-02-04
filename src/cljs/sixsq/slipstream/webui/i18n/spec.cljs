(ns sixsq.slipstream.webui.i18n.spec
  (:require
    [clojure.spec.alpha :as s]
    [sixsq.slipstream.webui.i18n.utils :as utils]))

(s/def ::locale string?)

(s/def ::tr fn?)

(s/def ::db (s/keys :req [::locale ::tr]))

(def defaults {::locale "en"
               ::tr     (utils/create-tr-fn "en")})
