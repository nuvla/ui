(ns sixsq.nuvla.ui.utils.spec
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as str]))


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


(def timestamp-regex #"^(-?(?:[1-9][0-9]*)?[0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])T(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])(\.[0-9]{1,3})?(Z)?$")

(defn timestamp? [s] (re-matches timestamp-regex s))

(s/def ::timestamp (s/and string? timestamp?))


(def ipv4-regex #"^((25[0-5]|(2[0-4]|1[0-9]|[1-9]|)[0-9])(\.(?!$)|$)){4}$")

(def private-ipv4-regex #"(^127\.)|(^10\.)|(^172\.1[6-9]\.)|(^172\.2[0-9]\.)|(^172\.3[0-1]\.)|(^192\.168\.)")

(defn ipv4? [s] (not (nil? (re-matches ipv4-regex s))))

(defn private-ipv4? [s] (and (ipv4? s) (not (nil? (re-find private-ipv4-regex s)))))


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
