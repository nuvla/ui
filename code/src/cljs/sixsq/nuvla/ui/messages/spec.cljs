(ns sixsq.nuvla.ui.messages.spec
  (:require-macros [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require [clojure.spec.alpha :as s]))


(s/def :message/header string?)

(s/def :message/content string?)

(s/def :message/type #{:success :info :error :notif})

(s/def :message/timestamp any?)

(s/def :message/uuid string?)

(s/def :message/data any?)

(s/def ::message (only-keys :req-un [:message/header
                                     :message/content
                                     :message/type
                                     :message/timestamp
                                     :message/uuid]
                            :opt-un [:message/data]))

(s/def ::messages (s/coll-of ::message))

(s/def ::alert-message (s/nilable ::message))

(s/def ::alert-display #{:none :slider :modal})

(s/def ::popup-open? boolean?)

(def defaults {::messages      []
               ::alert-message nil
               ::alert-display :none
               ::popup-open?   false})
