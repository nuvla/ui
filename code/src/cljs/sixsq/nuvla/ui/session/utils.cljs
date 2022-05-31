(ns sixsq.nuvla.ui.session.utils
  (:require
    [clojure.string :as str]))

(def ^:const user-tmpl-email-password "user-template/email-password")
(def ^:const session-tmpl-password "session-template/password")
(def ^:const session-tmpl-password-reset "session-template/password-reset")

(defn is-group?
  [active-claim]
  (str/starts-with? active-claim "group/"))

(defn remove-group-prefix
  [account]
  (str/replace-first account #"^group/" ""))
