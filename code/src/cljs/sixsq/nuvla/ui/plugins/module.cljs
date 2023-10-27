(ns sixsq.nuvla.ui.plugins.module
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-sub subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.utils :refer [name->href str-pathify]]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(def change-event-module-version ::change-event-module-version)
(def change-event-env-variables ::change-event-env-variables)

(def change-event-registries-credentials ::change-event-registries-credentials)

(def module-env-vars-path [:content :environmental-variables])

(def module-private-registries-path [:content :private-registries])

(def module-registries-credentials-path [:content :registries-credentials])

(defn- base-path
  [db-path href]
  (conj db-path ::modules (some-> href (str/split "_") first)))

(defn- db-module-subpath
  [db-path href k]
  (conj (base-path db-path href) k))

(defn- db-module-path
  [db-path href]
  (db-module-subpath db-path href ::module))

(defn- db-module-overwrite-path
  [db-path href]
  (db-module-subpath db-path href ::overwrite))

(defn- db-new-version-module-href-path
  [db-path href]
  (db-module-subpath db-path href ::new-version-module-href-path))

(defn- db-module-loading-registries-path
  [db-path href]
  (db-module-subpath db-path href ::loading?))

(defn- db-module-resolved-private-registries-path
  [db-path href]
  (db-module-subpath db-path href ::resolved-private-registries))

(defn- db-module-resolved-registries-creds-path
  [db-path href]
  (db-module-subpath db-path href ::resolved-registries-creds))

(defn- db-module-registries-credentials-path
  [db-path href]
  (db-module-subpath db-path href ::registries-credentials))


(defn db-module
  [db db-path href]
  (get-in db (db-module-path db-path href)))

(defn- update-db-module
  [db db-path href f]
  (update-in db (db-module-path db-path href) f))

(defn- set-db-module
  [module db db-path href]
  (assoc-in db (db-module-path db-path href) module))

(defn- module-env-vars
  [module]
  (get-in module module-env-vars-path))

(defn- env-vars-value-by-index
  [env-vars index]
  (get-in env-vars [index ::new-value]))

(defn- update-module-env-vars
  [module f]
  (update-in module module-env-vars-path f))

(defn- update-module-registries-credentials
  [module f]
  (update-in module module-registries-credentials-path f))

(defn- update-env-value-by-index
  [env-vars index value]
  (assoc-in env-vars [index ::new-value] value))

(defn- update-registry-credential-by-index
  [registries-credentials index value]
  (assoc registries-credentials index value))

(defn- module-private-registries
  [module]
  (get-in module module-private-registries-path))

(defn- module-registries-credentials
  [module]
  (get-in module module-registries-credentials-path))

(defn- overwrite-env
  [environment-variables env]
  (mapv (fn [{env-name :name env-value :value :as environment-variable}]
          (assoc environment-variable :value (get env env-name env-value)))
        environment-variables))

(defn- set-db-loading-registries
  [db db-path href loading?]
  (assoc-in db (db-module-loading-registries-path db-path href) loading?))

(reg-event-fx
  ::set-module
  (fn [{db :db} [_ db-path href module on-success-event]]
    (let [overwrite-map               (get-in db (db-module-overwrite-path db-path href))
          update-env-vars             #(overwrite-env % (:env overwrite-map))
          overwrite-module-env        #(update-module-env-vars % update-env-vars)
          overwrite-module-regs-creds #(if-let [registries-credentials (:registries-credentials overwrite-map)]
                                         (update-module-registries-credentials % (constantly registries-credentials))
                                         %)]
      {:db (-> module
               overwrite-module-env
               overwrite-module-regs-creds
               (set-db-module db db-path href))
       :fx (cond-> [[:dispatch [::load-infra-registries db-path href]]]
                   on-success-event (conj [:dispatch on-success-event]))})))

(reg-event-fx
  ::load-module
  (fn [{db :db} [_ db-path href overwrite on-success-event]]
    {:db               (assoc-in db (db-module-overwrite-path db-path href) overwrite)
     ::cimi-api-fx/get [href #(dispatch [::set-module db-path href % on-success-event])]}))

(reg-event-db
  ::set-resolved-private-registries
  (fn [db [_ db-path href private-registries]]
    (->> private-registries
         (map (juxt :id identity))
         (into {})
         (assoc-in db (db-module-resolved-private-registries-path db-path href)))))

(reg-event-fx
  ::resolve-private-registries
  (fn [_ [_ db-path href private-registries]]
    {::cimi-api-fx/search
     [:infrastructure-service
      {:filter  (general-utils/join-and
                  "subtype='registry'"
                  (apply general-utils/join-or
                         (map #(str "id='" % "'") private-registries))),
       :select  "id, name, description"
       :orderby "name:asc,id:asc"
       :last    10000}
      #(dispatch [::set-resolved-private-registries db-path href (:resources %)])]}))

(reg-event-db
  ::set-resolved-registries-creds
  (fn [db [_ db-path href registries-creds]]
    (->> registries-creds
         (group-by :parent)
         (assoc-in db (db-module-resolved-registries-creds-path db-path href)))))

(reg-event-fx
  ::resolve-registries-creds
  (fn [{db :db} [_ db-path href private-registries]]
    {:db (set-db-loading-registries db db-path href false)
     ::cimi-api-fx/search
     [:credential
      {:filter  (general-utils/join-and
                  "subtype='infrastructure-service-registry'"
                  (apply general-utils/join-or
                         (map #(str "parent='" % "'")
                              private-registries)))
       :select  "id, parent, name, description, last-check, status, subtype"
       :orderby "name:asc,id:asc"
       :last    10000}
      #(dispatch [::set-resolved-registries-creds db-path href (:resources %)])]}))

(reg-event-fx
  ::load-infra-registries
  (fn [{db :db} [_ db-path href]]
    (when-let [private-registries (-> (db-module db db-path href)
                                      module-private-registries
                                      seq)]
      {:db (set-db-loading-registries db db-path href true)
       :fx [[:dispatch [::resolve-private-registries db-path href private-registries]]
            [:dispatch [::resolve-registries-creds db-path href private-registries]]]})))


(reg-event-fx
  ::update-env
  (fn [{db :db} [_ db-path href index new-value]]
    (let [change-event          (get-in db (conj db-path change-event-env-variables))
          update-env-vars       #(update-env-value-by-index % index new-value)
          update-module-env-var #(update-module-env-vars % update-env-vars)]
      {:db (update-db-module db db-path href update-module-env-var)
       :fx [(when change-event [:dispatch change-event])]})))

(reg-event-fx
  ::update-registry-credential
  (fn [{db :db} [_ db-path href index new-value]]
    (let [change-event           (get-in db (conj db-path change-event-registries-credentials))
          private-registries-len (-> (db-module db db-path href)
                                     module-private-registries
                                     count)
          update-regs-creds      #(let [regs-creds (or % (take private-registries-len (repeat "")))]
                                    (update-registry-credential-by-index regs-creds index new-value))
          update-module-reg-cred #(update-module-registries-credentials % update-regs-creds)]
      {:db (update-db-module db db-path href update-module-reg-cred)
       :fx [(when change-event [:dispatch change-event])]})))

(defn get-version-id
  [module-versions version]
  (some (fn [[idx {:keys [href]}]] (when (= version href) idx)) module-versions))

(defn- module-versions-indexed
  [module]
  (-> module :versions apps-utils/map-versions-index))

(reg-sub
  ::module
  (fn [db [_ db-path href]]
    (db-module db db-path href)))

(defn- db-selected-version
  [db db-path href]
  (let [module            (db-module db db-path href)
        module-content-id (-> module :content :id)
        versions-indexed  (module-versions-indexed module)]
    (get-version-id versions-indexed module-content-id)))

(reg-sub
  ::new-version-module-href
  (fn [db [_ db-path href]]
    (get-in db (db-new-version-module-href-path db-path href))))

(defn- db-version-href
  [db db-path href version-module-href]
  (let [module           (db-module db db-path href)
        versions-indexed (module-versions-indexed module)]
    (str (:id module) "_" (get-version-id versions-indexed version-module-href))))

(reg-event-fx
  ::change-version
  (fn [{db :db} [_ db-path href version-module-href]]
    (let [new-version-href (db-version-href db db-path href version-module-href)
          change-event     (get-in db (conj db-path change-event-module-version))]
      {:db (assoc-in db (db-new-version-module-href-path db-path href) version-module-href)
       :fx [[:dispatch [::load-module db-path new-version-href]]
            (when change-event
              [:dispatch (conj change-event new-version-href)])]})))

(defn db-new-version
  [db db-path href]
  (let [module                  (db-module db db-path href)
        versions-indexed        (module-versions-indexed module)
        new-version-module-href (get-in db (db-new-version-module-href-path db-path href))]
    (when-not (str/blank? new-version-module-href)
      (get-version-id versions-indexed new-version-module-href))))

(defn- db-module-env-vars
  [db db-path href]
  (module-env-vars (db-module db db-path href)))

(defn db-module-registries-credentials
  [db db-path href]
  (module-registries-credentials (db-module db db-path href)))

(defn- changed-env-vars
  [env-vars]
  (keep (fn [{:keys [::new-value :value :name]}]
          (when (some? new-value)
            {:name  name
             :value new-value})
          ) env-vars))

(defn db-changed-env-vars
  [db db-path href]
  (changed-env-vars (db-module-env-vars db db-path href)))

(reg-sub
  ::module-versions-indexed
  (fn [[_ db-path href]]
    (subscribe [::module db-path href]))
  module-versions-indexed)

(reg-sub
  ::module-env-value
  (fn [db [_ db-path href index]]
    (env-vars-value-by-index (db-module-env-vars db db-path href) index)))

(defn db-module-env-vars-in-error
  [db db-path href]
  (let [module   (db-module db db-path href)
        env-vars (module-env-vars module)]
    (->> env-vars
         (keep (fn [{:keys [name value required] ::keys [new-value]}]
                 (when (and required (if (some? new-value)
                                       (str/blank? new-value)
                                       (str/blank? value)))
                   name)))
         (into #{}))))

(reg-sub
  ::module-env-vars-in-error
  (fn [db [_ db-path href]]
    (db-module-env-vars-in-error db db-path href)))

(reg-sub
  ::registries-loading?
  (fn [db [_ db-path href]]
    (get-in db (db-module-loading-registries-path db-path href) false)))

(reg-sub
  ::resolved-private-registry
  (fn [db [_ db-path href private-registry-id]]
    (-> db
        (get-in (db-module-resolved-private-registries-path db-path href))
        (get private-registry-id))))

(reg-sub
  ::selected-registry-credential
  (fn [db [_ db-path href i]]
    (-> db
        (db-module db-path href)
        module-registries-credentials
        (nth i nil))))

(reg-sub
  ::private-registry-creds-options
  (fn [db [_ db-path href private-registry-id]]
    (-> db
        (get-in (db-module-resolved-registries-creds-path db-path href))
        (get private-registry-id)
        (->> (map (fn [{:keys [id name]}]
                    {:key id :text (or name id) :value id}))))))

(reg-sub
  ::module-versions-options
  (fn [[_ db-path href]]
    [(subscribe [::module-versions-indexed db-path href])
     (subscribe [::i18n-subs/tr])])
  (fn [[versions-indexed tr]]
    (apps-utils/versions-options versions-indexed tr)))

(def cred-env-var-map
  {"S3_CRED"  "infrastructure-service-minio"
   "GPG_CRED" "gpg-key"})

(defn is-cred-env-var?
  [env-var-name]
  (contains? (set (keys cred-env-var-map)) env-var-name))

(defn filter-creds
  [env-name creds]
  (when (is-cred-env-var? env-name)
    (filter #(when (= (get cred-env-var-map env-name) (:subtype %)) %) creds)))

(reg-event-fx
  ::get-credentials-opts
  (fn [_ [_ subtype setter]]
    {::cimi-api-fx/search [:credential
                           {:filter  (str "subtype='" subtype "'")
                            :orderby "name:asc, id:asc"
                            :select  "id, name"
                            :last    10000}
                           #(setter (map (fn [{id :id, name :name}]
                                           {:key id, :value id, :text name})
                                         (:resources %)))]}))

(defn EnvCredential
  [env-name _value _on-change]
  (let [tr      (subscribe [::i18n-subs/tr])
        options (r/atom [])
        subtype (get cred-env-var-map env-name)]
    (dispatch [::get-credentials-opts subtype #(reset! options %)])
    (fn [_env-name value on-change]
      [ui/Dropdown
       {:clearable   true
        :selection   true
        :fluid       true
        :value       value
        :placeholder (@tr [:select-credential])
        :on-change   (ui-callback/value on-change)
        :options     @options}])))

(defn EnvVarInput
  [db-path href read-only? error? i {env-name  :name
                                     env-value :value}]
  (let [updated-env-value @(subscribe [::module-env-value db-path href i])
        value             (or updated-env-value env-value "")
        on-change         #(dispatch [::update-env db-path href i %])]
    (if (is-cred-env-var? env-name)
      [EnvCredential env-name value on-change]
      [ui/FormInput
       {:type          "text"
        :name          env-name
        :default-value value
        :read-only     read-only?
        :error         error?
        :fluid         true
        :on-change     (ui-callback/input-callback on-change)}])))

(defn AsFormInput
  [db-path href read-only? error?
   i {env-name        :name
      env-description :description
      env-required    :required :as env-variable}]
  [ui/FormField
   [uix/FieldLabel {:name       env-name
                    :required?  env-required
                    :help-popup [uix/HelpPopup env-description]}]
   [EnvVarInput db-path href read-only? error? i env-variable]])

(defn EnvVariables
  [{:keys [db-path href change-event read-only?]
    :or   {read-only? false}
    :as   _opts}]
  (dispatch [::helpers/set db-path change-event-env-variables change-event])
  (let [tr            @(subscribe [::i18n-subs/tr])
        module        @(subscribe [::module db-path href])
        env-variables (module-env-vars module)
        vars-in-error @(subscribe [::module-env-vars-in-error db-path href])]
    (if (seq env-variables)
      [ui/Form
       (map-indexed
         (fn [i env-variable]
           (let [var-in-error (boolean (vars-in-error (:name env-variable)))]
             ^{:key (str (:name env-variable) "_" i)}
             [AsFormInput db-path href read-only? var-in-error i env-variable]))
         env-variables)]
      [ui/Message (tr [:module-no-env-variables])])))

(defn DropdownContainerRegistry
  [{:keys [db-path href read-only? required?]
    :or   {read-only? false
           required?  true}
    :as   _opts} i private-registry]
  (let [tr             @(subscribe [::i18n-subs/tr])
        {:keys [id name description]} @(subscribe [::resolved-private-registry db-path href private-registry])
        options        @(subscribe [::private-registry-creds-options db-path href private-registry])
        value          @(subscribe [::selected-registry-credential db-path href i])
        preselected?   (and (some? value)
                            (zero? (count options)))
        registry-label (r/as-element
                         [uix/FieldLabel {:name       (or name id)
                                          :required?  required?
                                          :help-popup (when description [uix/HelpPopup description])}])
        disabled?      (or preselected? read-only?)
        placeholder    (if preselected?
                         (tr [:preselected])
                         (tr [:select-credential]))]
    [ui/FormDropdown
     {:label       registry-label
      :selection   true
      :value       value
      :disabled    disabled?
      :error       (when-not (seq options)
                     (tr [:no-available-creds-registry]))
      :placeholder placeholder
      :options     options
      :on-change   (ui-callback/value
                     #(dispatch [::update-registry-credential db-path href i %]))}]))

(defn RegistriesCredentials
  [{:keys [db-path href change-event]
    :as   opts}]
  (let [module             @(subscribe [::module db-path href])
        private-registries (module-private-registries module)
        loading?           @(subscribe [::registries-loading? db-path href])]
    (dispatch [::helpers/set db-path change-event-registries-credentials change-event])
    (if (seq private-registries)
      [ui/Form {:loading loading?}
       (for [[i private-registry] (map-indexed vector private-registries)]
         ^{:key (str href "-" private-registry)}
         [DropdownContainerRegistry opts i private-registry])]
      [ui/Message "No container registries defined"])))

(defn LinkToApp
  [{:keys [db-path href children target]
    :as   _opts}]
  (let [{:keys [path content]} @(subscribe [::module db-path href])
        versions-indexed (subscribe [::module-versions-indexed db-path href])
        version-id       (get-version-id @versions-indexed (:id content))]
    [:a {:href   (str-pathify (name->href routes/apps)
                              (str path "?version=" version-id))
         :target (or target "_blank")}
     children]))

(defn ModuleNameIcon
  [{:keys [db-path href children show-version?]
    :as   _opts}]
  (let [{:keys [id name subtype content]} @(subscribe [::module db-path href])
        versions-indexed (subscribe [::module-versions-indexed db-path href])
        version-id       (get-version-id @versions-indexed (:id content))
        label            (cond-> (or name id)
                                 show-version? (str " v" version-id))]
    [ui/ListItem
     [apps-utils/SubtypeDockerK8sListIcon subtype]
     [ui/ListContent
      [LinkToApp {:db-path  db-path
                  :href     href
                  :children label}]
      children]]))

(defn ModuleVersions
  [{:keys [db-path href change-event read-only?]
    :or   {read-only? false}
    :as   _opts}]
  (let [module                  (subscribe [::module db-path href])
        new-version-module-href (subscribe [::new-version-module-href db-path href])
        options                 (subscribe [::module-versions-options db-path href])]
    (dispatch [::helpers/set db-path change-event-module-version change-event])
    (let [{:keys [content]} @module
          value (or @new-version-module-href (:id content))]
      [ui/FormDropdown
       {:value     value
        :scrolling true
        :upward    false
        :selection true
        :on-change (ui-callback/value
                     #(dispatch [::change-version db-path href %]))
        :fluid     true
        :options   @options
        :disabled  read-only?}])))

(s/def ::href string?)

(s/fdef ModuleVersions
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path
                                            ::href]
                                   :opt-un [::helpers/read-only?
                                            ::helpers/change-event])))
