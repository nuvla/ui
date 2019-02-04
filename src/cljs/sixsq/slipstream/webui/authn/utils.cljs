(ns sixsq.slipstream.webui.authn.utils
  (:require
    [clojure.string :as str]))


(defn has-role? [session role]
  (some-> session :roles (str/split #"\s+") set (contains? role)))


(defn order-and-group
  "Sorts the methods by ID, then the order attribute, and then groups them on
   the group or method attribute."
  [methods]
  (->> methods
       (sort-by :id)
       (sort-by #(or (:order %) 0))
       (group-by #(or (:group %) (:method %)))))


(defn keep-visible-params
  "Keeps the form parameters that should be shown to the user. It removes all
   readOnly parameters along with :name and :description."
  [[k {:keys [readOnly]}]]
  (and (not= :name k)
       (not= :description k)
       (not= :group k)
       (not= :redirectURI k)
       (not readOnly)))


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
