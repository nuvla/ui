(ns
  sixsq.nuvla.ui.apps.utils
  (:require
    [clojure.string :as str]
    [sixsq.nuvla.ui.apps.spec :as spec]
    [sixsq.nuvla.ui.utils.general :as utils-general]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]))


(def publish-icon
  "check circle outline")


(def un-publish-icon
  "times circle outline")


(defn find-current-version
  [module-versions module-id]
  (when module-id
    (some
      (fn [[idx item]]
        (when (= (:href item) module-id) idx))
      module-versions)))


(defn extract-version
  "Return the index or nil if it is the most recent version"
  [module-id]
  (-> module-id (str/split #"/") last (str/split #"_") second))


(defn published?
  "Check if the module version is published"
  [module module-id]
  (let [versions (:versions module)
        index    (extract-version module-id)
        version  (if (nil? index) (dec (count versions)) (js/parseInt index))]
    (-> versions (nth version) :published true?)))


(defn filter-published-versions
  [map-versions]
  (filter #(true? (-> % second :published true?)) map-versions))


(defn compose-module-id
  [module-version]
  (str (-> module-version second :href) "_" (first module-version)))


(defn latest-published-index
  "Return the latest published index. This can be used to append to a module id to fetch a specific
  module version"
  [map-versions]
  (ffirst (filter-published-versions map-versions)))


(defn latest-published-version
  "Return the latest published version id"
  [map-versions]
  (-> map-versions filter-published-versions first second :href))


(defn latest-published-module-with-index
  "Return the latest published module id (with the version appended: <module-id>_<version-index>)."
  [module-id map-versions]
  (let [index (-> map-versions filter-published-versions ffirst)]
    (str module-id "_" index)))


(defn latest-published?
  "Check if the module version corresponds to the latest published version."
  [version-id map-versions]
  (let [lastest (latest-published-version map-versions)]
    (= (:href lastest) version-id)))


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
    "application_kubernetes" "cubes"
    "question circle"))


(defn meta-subtype-icon
  [subtype]
  (if (= "project" subtype)
    "folder open"
    (subtype-icon subtype)))


(defn contruct-path [parent name]
  (let [sanitized-name (utils-general/sanitize-name name)]
    (str/join "/"
              (remove str/blank?
                      [parent sanitized-name]))))


(defn sanitize-base
  [module]
  (let [path (contruct-path (:parent-path module) (:name module))]
    (if (nil? (:path module))
      (assoc module :path path)
      module)))                                             ;; ui forcing path immutability to not loose children


(defn env-variables->module
  [db]
  (into
    []
    (for [[_id m] (get-in db [::spec/module-common ::spec/env-variables])]
      (let [{:keys [::spec/env-name ::spec/env-description ::spec/env-value ::spec/env-required]
             :or   {env-required false}} m]
        (cond-> {:name     env-name
                 :required env-required}
                env-value (assoc :value env-value)
                env-description (assoc :description env-description))))))


(defn registries->module
  [db]
  (let [registries             (-> db (get-in [::spec/module-common ::spec/registries]) vals)
        private-registries     (map ::spec/registry-id registries)
        credentials-registries (map ::spec/registry-cred-id registries)]
    [private-registries credentials-registries]))


(defn urls->module
  [db]
  (into
    []
    (for [[_id u] (get-in db [::spec/module-common ::spec/urls])]
      [(::spec/url-name u) (::spec/url u)])))


(defn output-parameters->module
  [db]
  (into
    []
    (for [[_id op] (get-in db [::spec/module-common ::spec/output-parameters])]
      (let [{:keys [::spec/output-parameter-name ::spec/output-parameter-description]} op]
        (conj
          {:name output-parameter-name}
          {:description output-parameter-description})))))


(defn data-binding->module
  [db]
  (into
    []
    (for [[_id binding] (get-in db [::spec/module-common ::spec/data-types])]
      (let [{:keys [::spec/data-type]} binding]
        (conj data-type)))))


(defn db->module
  [module _commit-map db]
  (let [name              (get-in db [::spec/module-common ::spec/name])
        description       (get-in db [::spec/module-common ::spec/description])
        parent-path       (get-in db [::spec/module-common ::spec/parent-path])
        logo-url          (get-in db [::spec/module-common ::spec/logo-url])
        subtype           (get-in db [::spec/module-common ::spec/subtype])
        path              (get-in db [::spec/module-common ::spec/path])
        acl               (get-in db [::spec/module-common ::spec/acl])
        price             (get-in db [::spec/module-common ::spec/price])
        license           (get-in db [::spec/module-common ::spec/license])
        env-variables     (env-variables->module db)
        [private-registries
         registries-credentials] (registries->module db)
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
          (if (empty? private-registries)
            (update-in m [:content] dissoc :private-registries)
            (assoc-in m [:content :private-registries] private-registries))
          (if (empty? registries-credentials)
            (update-in m [:content] dissoc :registries-credentials)
            (assoc-in m [:content :registries-credentials] registries-credentials))
          (assoc-in m [:content :output-parameters] output-parameters)
          (assoc-in m [:data-accept-content-types] data-bindings)
          (cond-> m (:cent-amount-daily price) (assoc-in [:price] price))
          (cond-> m (:license-url license)
                  (assoc :license
                         (cond-> {:url  (:license-url license)
                                  :name (:license-name license)}
                                 (not (str/blank? (:license-description license)))
                                 (assoc :description (:license-description license)))))
          (sanitize-base m)
          (dissoc m :children))))


(defn env-variables->db
  [env-variables]
  (into
    (sorted-map)
    (for [[id {:keys [name description value required]}] (map-indexed vector env-variables)]
      [id {:id                    id
           ::spec/env-name        name
           ::spec/env-value       value
           ::spec/env-description description
           ::spec/env-required    (or required false)}])))


(defn registries->db
  [private-registries registries-credentials]
  (into
    (sorted-map)
    (for [[id infra-id] (map-indexed vector private-registries)]
      [id {:id                     id
           ::spec/registry-id      infra-id
           ::spec/registry-cred-id (nth registries-credentials id "")}])))


(defn urls->db
  [tuples]
  (into
    (sorted-map)
    (for [[id [name url]] (map-indexed vector tuples)]
      [id {:id             id
           ::spec/url-name name
           ::spec/url      url}])))


(defn output-parameters->db
  [params]
  (into
    (sorted-map)
    (for [[id {:keys [name description]}] (map-indexed vector params)]
      [id {:id                                 id
           ::spec/output-parameter-name        name
           ::spec/output-parameter-description description}])))


(defn data-types->db
  [dts]
  (into
    (sorted-map)
    (for [[id dt] (map-indexed vector dts)]
      [id {:id              id
           ::spec/data-type dt}])))


(defn module->db
  [db {:keys [name description parent-path content data-accept-content-types
              path logo-url subtype acl price license] :as _module}]
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
                (data-types->db data-accept-content-types))
      (assoc-in [::spec/module-common ::spec/registries]
                (registries->db (:private-registries content)
                                (:registries-credentials content)))
      (assoc-in [::spec/module-common ::spec/price]
                price)
      (assoc-in [::spec/module-common ::spec/license]
                (when (some? license)
                  {:license-name        (:name license)
                   :license-description (:description license)
                   :license-url         (:url license)}))))


(defn mandatory-name
  [name]
  [:span name [:sup " " [ui/Icon {:name  "asterisk"
                                  :size  :tiny
                                  :color :red}]]])

(defn sorted-map-new-idx
  [sorted-map-elemts]
  (or (some-> sorted-map-elemts last first inc) 0))


(defn map-versions-index
  "Create a list of tuples with [index version], where index starts at 0"
  [versions]
  (reverse (map-indexed vector versions)))


(defn module->groups
  [module]
  (let [owners  (-> module :acl :owners)
        vendors (filter #(str/starts-with? % "group/") owners)]
    vendors))


(defn module->users
  [module]
  (let [owners (-> module :acl :owners)
        users  (filter #(not (str/starts-with? % "group/")) owners)]
    users))


(defn is-vendor?
  [module]
  (let [vendors (module->groups module)]
    #_:clj-kondo/ignore
    (not (empty? vendors))))


(defn vendor-name
  [user]
  (drop 6 user))


(defn set-reset-error
  [db key error? error-spec]
  (let [errors  (error-spec db)
        is-set? (contains? errors key)
        reset?  (and (not error?) is-set?)
        set?    (and error? (not is-set?))]
    (cond-> db
            reset? (update error-spec #(disj % key))
            set? (update error-spec #(conj % key)))))
