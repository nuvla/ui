(ns sixsq.nuvla.ui.session.utils
  (:require [clojure.string :as str]))

(def ^:const user-tmpl-email-password "user-template/email-password")
(def ^:const session-tmpl-password "session-template/password")
(def ^:const session-tmpl-password-reset "session-template/password-reset")

(defn is-group?
  [active-claim]
  (str/starts-with? (or active-claim "") "group/"))

(defn remove-group-prefix
  [account]
  (str/replace-first account #"^group/" ""))

(defn get-user-id
  [session]
  (:user session))

(defn get-identifier
  [session]
  (:identifier session))

(defn get-active-claim
  [session]
  (or (:active-claim session)
      (get-user-id session)))

(defn get-roles
  [session]
  (set (some-> session :roles (str/split #"\s+"))))

(defn resolve-user
  [current-user-id identifier peers user-id]
  (if (= user-id current-user-id)
    identifier
    (get peers user-id user-id)))
