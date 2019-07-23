(ns sixsq.nuvla.ui.utils.spec
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [taoensso.timbre :as log]))


(defn nonblank-string [s]
  (let [str-s (str s)]
    (when-not (str/blank? str-s)
      str-s)))


(defn acceptable-password?
  [password]
  (and (string? password)
       (>= (count password) 8)
       (re-matches #"^.*[A-Z].*$" password)
       (re-matches #"^.*[a-z].*$" password)
       (re-matches #"^.*[0-9].*$" password)
       (re-matches #"^.*[^A-Za-z0-9].*$" password)))


(s/def ::password acceptable-password?)


(s/def ::password-repeat ::password)


(def email-regex #"^[a-zA-Z0-9.!#$%&'*+\/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$")


(defn email? [s] (re-matches email-regex s))


(s/def ::email (s/and string? email?))


(s/def ::username nonblank-string)


(s/def ::key nonblank-string)


(s/def ::secret nonblank-string)


(defn filename? [s] (re-matches #"^[\w-\.]+$" s))


(s/def ::filename (s/and string? filename?))


(defn resolvable?
  [spec]
  (try
    (do (s/describe spec) true)
    (catch :default e
      false)))
