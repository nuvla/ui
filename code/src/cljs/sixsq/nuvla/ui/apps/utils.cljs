(ns
  sixsq.nuvla.ui.apps.utils
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [subscribe]]
    [sixsq.nuvla.ui.apps.spec :as spec]
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
    "component" "grid layout"
    "application" "cubes"
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
      module)))                                             ;; ui forcing path immutability to not loose children


(defn env-variables->module
  [db]
  (into []
        (for [[id m] (get-in db [::spec/module-common ::spec/env-variables])]
          (let [{:keys [::spec/env-name ::spec/env-description ::spec/env-value ::spec/env-required]
                 :or   {env-required false}} m]
            (cond-> {:name     env-name
                     :required env-required}
                    env-value (assoc :value env-value)
                    env-description (assoc :description env-description))))))


(defn urls->module
  [db]
  (into []
        (for [[id u] (get-in db [::spec/module-common ::spec/urls])]
          (do
            [(::spec/url-name u) (::spec/url u)]))))


(defn output-parameters->module
  [db]
  (into []
        (for [[id op] (get-in db [::spec/module-common ::spec/output-parameters])]
          (let [{:keys [::spec/output-parameter-name ::spec/output-parameter-description]} op]
            (conj
              {:name output-parameter-name}
              {:description output-parameter-description})))))


(defn data-binding->module
  [db]
  (into []
        (for [[id binding] (get-in db [::spec/module-common ::spec/data-types])]
          (let [{:keys [::spec/data-type]} binding]
            (conj
              data-type)))))


(defn db->module
  [module commit-map db]
  (let [name              (get-in db [::spec/module-common ::spec/name])
        description       (get-in db [::spec/module-common ::spec/description])
        parent-path       (get-in db [::spec/module-common ::spec/parent-path])
        logo-url          (get-in db [::spec/module-common ::spec/logo-url])
        subtype           (get-in db [::spec/module-common ::spec/subtype])
        path              (get-in db [::spec/module-common ::spec/path])
        acl               (get-in db [::spec/module-common ::spec/acl])
        env-variables     (env-variables->module db)
        urls              (urls->module db)
        output-parameters (output-parameters->module db)
        data-bindings     (data-binding->module db)]
    (as-> module m
          (assoc-in m [:name] name)
          (assoc-in m [:description] description)
          (assoc-in m [:parent-path] parent-path)
          (assoc-in m [:logo-url] logo-url)
          (assoc-in m [:subtype] subtype)
          (assoc-in m [:path] path)
          (cond-> m acl (assoc-in [:acl] acl))
          (if (empty? env-variables)
            (update-in m [:content] dissoc :environmental-variables)
            (assoc-in m [:content :environmental-variables] env-variables))
          (if (empty? urls)
            (update-in m [:content] dissoc :urls)
            (assoc-in m [:content :urls] urls))
          (assoc-in m [:content :output-parameters] output-parameters)
          (assoc-in m [:data-accept-content-types] data-bindings)
          (sanitize-base m)
          (dissoc m :children))))


(defn env-variables->db
  [env-variables]
  (into {}
        (for [{:keys [name description value required]} env-variables]
          (let [id (random-uuid)]
            {id {:id                    id
                 ::spec/env-name        name
                 ::spec/env-value       value
                 ::spec/env-description description
                 ::spec/env-required    (or required false)}}))))


(defn urls->db
  [tuples]
  (into {}
        (for [[name url] tuples]
          (let [id (random-uuid)]
            {id {:id             id
                 ::spec/url-name name
                 ::spec/url      url}}))))


(defn output-parameters->db
  [params]
  (into {}
        (for [{:keys [name description]} params]
          (let [id (random-uuid)]
            {id {:id                                 id
                 ::spec/output-parameter-name        name
                 ::spec/output-parameter-description description}}))))


(defn data-types->db
  [dts]
  (into {}
        (for [dt dts]
          (let [id (random-uuid)]
            {id {:id              id
                 ::spec/data-type dt}}))))


(defn module->db
  [db {:keys [name description parent-path content data-accept-content-types
              path logo-url subtype acl] :as module}]
  (-> db
      (assoc-in [::spec/module-common ::spec/name] name)
      (assoc-in [::spec/module-common ::spec/description] description)
      (assoc-in [::spec/module-common ::spec/parent-path] parent-path)
      (assoc-in [::spec/module-common ::spec/path] path)
      (assoc-in [::spec/module-common ::spec/logo-url] logo-url)
      (assoc-in [::spec/module-common ::spec/subtype] subtype)
      (assoc-in [::spec/module-common ::spec/acl] acl)
      (assoc-in [::spec/module-common ::spec/env-variables]
                (env-variables->db (:environmental-variables content)))
      (assoc-in [::spec/module-common ::spec/urls] (urls->db (:urls content)))
      (assoc-in [::spec/module-common ::spec/output-parameters]
                (output-parameters->db (:output-parameters content)))
      (assoc-in [::spec/module-common ::spec/data-types]
                (data-types->db data-accept-content-types))))


(defn mandatory-name
  [name]
  [:span name [:sup " " [ui/Icon {:name  "asterisk"
                                  :size  :tiny
                                  :color :red}]]])
