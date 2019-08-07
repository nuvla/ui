(ns sixsq.nuvla.ui.authn.utils
  (:require
    [cljs.spec.alpha :as s]))

(def ^:const user-tmpl-email-password "user-template/email-password")
(def ^:const user-tmpl-email-invitation "user-template/email-invitation")
(def ^:const session-tmpl-password "session-template/password")
(def ^:const session-tmpl-api-key "session-template/api-key")
(def ^:const session-tmpl-password-reset "session-template/password-reset")

(def ^:const internal-templates #{user-tmpl-email-password
                                  user-tmpl-email-invitation
                                  session-tmpl-password
                                  session-tmpl-api-key
                                  session-tmpl-password-reset})


(defn order-and-group
  "Sorts the methods by ID, then the order attribute, and then groups them on
   the group or method attribute."
  [methods]
  (->> methods
       (sort-by :id)
       (sort-by #(or (:order %) 0))
       (group-by #(or (:group %) (:method %)))))


(defn select-method-by-id
  [id methods]
  (->> methods
       (filter #(= id (:id %)))
       first))


(defn select-group-methods-by-id
  [id method-groups]
  (->> method-groups
       (filter #(-> % first (= id)))
       first
       second))


(defn grouped-authn-methods
  "Takes a set of raw downloaded templates, groups and orders them by the
   :group (or :method) value. Returns a vector of tuples [group-key
   authn-methods]."
  [templates]
  (->> templates vals (remove :hidden) order-and-group vec))


(defn error?
  [field-key form-spec form-data]
  (let [field-spec (some-> form-spec (name) (str "/" (name field-key)) (keyword))]
    (s/valid? field-spec (field-key form-data))))