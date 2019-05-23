(ns
  sixsq.nuvla.ui.apps.utils
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [subscribe]]
    [sixsq.nuvla.ui.apps.spec :as apps-spec]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [taoensso.timbre :as log]))


(defn nav-path->module-path
  [nav-path]
  (some->> nav-path rest seq (str/join "/")))


(defn nav-path->parent-path
  [nav-path]
  (some->> nav-path rest drop-last seq (str/join "/")))


(defn nav-path->module-name
  [nav-path]
  (some->> nav-path rest last))


(defn subtype-icon
  [subtype]
  (case subtype
    "project" "folder"
    "component" "microchip"
    "question circle"))


(defn meta-subtype-icon
  [subtype]
  (if (= "project" subtype)
    "folder open"
    (subtype-icon subtype)))


;; Sanitize before serialization to server

(defn sanitize-name [name]
  (str/lower-case
    (str/replace
      (str/trim
        (str/join "" (re-seq #"[a-zA-Z0-9\ ]" name)))
      " " "-")))


(defn contruct-path [parent name]
  (let [sanitized-name (sanitize-name name)]
    (str/join "/"
              (remove str/blank?
                      [parent sanitized-name]))))


(defn
  sanitize-base
  [module]
  (let [path (contruct-path (:parent-path module) (:name module))]
    (if (nil? (:path module))
      (assoc module :path path)
      module)))


(defn db->module
  [module commit-map db]
  (let [{:keys [author commit]} commit-map
        name        (get-in db [::apps-spec/module-common ::apps-spec/name])
        description (get-in db [::apps-spec/module-common ::apps-spec/description])
        parent-path (get-in db [::apps-spec/module-common ::apps-spec/parent-path])
        logo-url    (get-in db [::apps-spec/module-common ::apps-spec/logo-url])
        subtype     (get-in db [::apps-spec/module-common ::apps-spec/subtype])
        path        (get-in db [::apps-spec/module-common ::apps-spec/path])
        acl         (get-in db [::apps-spec/module-common ::apps-spec/acl])]
    (as-> module m
          (assoc-in m [:name] name)
          (assoc-in m [:description] description)
          (assoc-in m [:parent-path] parent-path)
          (assoc-in m [:logo-url] logo-url)
          (assoc-in m [:subtype] subtype)
          (assoc-in m [:path] path)
          (assoc-in m [:acl] acl)
          (sanitize-base m)
          (dissoc m :children))))


(defn module->db
  [db module]
  (-> db
      (assoc-in [::apps-spec/module-common ::apps-spec/name] (get-in module [:name]))
      (assoc-in [::apps-spec/module-common ::apps-spec/description] (get-in module [:description]))
      (assoc-in [::apps-spec/module-common ::apps-spec/parent-path] (get-in module [:parent-path]))
      (assoc-in [::apps-spec/module-common ::apps-spec/path] (get-in module [:path]))
      (assoc-in [::apps-spec/module-common ::apps-spec/logo-url] (get-in module [:logo-url]))
      (assoc-in [::apps-spec/module-common ::apps-spec/subtype] (get-in module [:subtype]))
      (assoc-in [::apps-spec/module-common ::apps-spec/acl] (get-in module [:acl]))))


; TODO: has moved to utils/general
(defn can-operation? [operation data]
  (->> data :operations (map :rel) (some #{(name operation)}) nil? not))


(defn can-edit? [data]
  (can-operation? :edit data))


(defn can-delete? [data]
  (can-operation? :delete data))


(defn editable?
  [module is-new?]
  (or is-new? (can-edit? module)))


(defn mandatory-name
  [name]
  [:span name [:sup " " [ui/Icon {:name  "asterisk"
                                  :size  :tiny
                                  :color :red}]]])
