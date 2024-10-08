(ns sixsq.nuvla.ui.common-components.plugins.module
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-sub subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.pages.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.utils :refer [name->href str-pathify]]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.tooltip :as tt]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(def change-event-module-version ::change-event-module-version)
(def change-event-env-variables ::change-event-env-variables)
(def change-event-files ::change-event-files)

(def change-event-registries-credentials ::change-event-registries-credentials)

(def module-env-vars-path [:content :environmental-variables])

(def module-files-path [:content :files])

(def module-private-registries-path [:content :private-registries])

(def module-registries-credentials-path [:content :registries-credentials])

(defn- base-path
  [db-path href]
  (conj db-path ::modules (some-> href (str/split "_") first)))

(defn- db-module-subpath
  [db-path href k]
  (conj (base-path db-path href) k))

(defn- versioned-path
  [db-path href]
  (conj db-path ::modules href))

(defn- db-module-versioned-subpath
  [db-path href k]
  (conj (versioned-path db-path href) k))

(defn- db-module-path
  [db-path href]
  (db-module-subpath db-path href ::module))

(defn- db-module-overwrite-path
  [db-path href]
  (db-module-versioned-subpath db-path href ::overwrite))

(defn- db-module-reset-to-path
  [db-path href]
  (db-module-versioned-subpath db-path href ::reset-to))

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

(defn db-module
  [db db-path href]
  (get-in db (db-module-path db-path href)))

(defn get-version-id
  [module-versions version]
  (some (fn [[idx {:keys [href]}]] (when (= version href) idx)) module-versions))

(defn- module-versions-indexed
  [module]
  (-> module :versions apps-utils/map-versions-index))

(defn db-selected-version
  [db db-path href]
  (let [module            (db-module db db-path href)
        module-content-id (-> module :content :id)
        versions-indexed  (module-versions-indexed module)]
    (get-version-id versions-indexed module-content-id)))

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

(defn- module-files
  [module]
  (get-in module module-files-path))

(defn- new-file-content-by-index
  [files index]
  (get-in files [index ::new-file-content]))

(defn- update-module-files
  [module f]
  (update-in module module-files-path f))

(defn- update-module-registries-credentials
  [module f]
  (update-in module module-registries-credentials-path f))

(defn- update-env-value-by-index
  [env-vars index value]
  (assoc-in env-vars [index ::new-value] value))

(defn- update-file-content-by-index
  [files index file-content]
  (assoc-in files [index ::new-file-content] file-content))

(defn- reset-file-content-by-index
  [files index]
  (update files index dissoc ::new-file-content))

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
  (mapv (fn [{env-name :name :as environment-variable}]
          (if-let [env-value (get env env-name)]
            (assoc environment-variable
              ::new-value env-value)
            environment-variable))
        environment-variables))

(defn- overwrite-files
  [files overridden-files]
  (mapv (fn [{:keys [file-name] :as file}]
          (if-let [file-content (get overridden-files file-name)]
            (assoc file
              ::new-file-content file-content)
            file))
        files))

(defn- set-db-loading-registries
  [db db-path href loading?]
  (assoc-in db (db-module-loading-registries-path db-path href) loading?))

(defn- init-reset-to
  [db db-path href module reset-to]
  (assoc-in db (db-module-reset-to-path db-path href)
            (-> reset-to
                (update :env #(merge (->> module :content :environmental-variables
                                          (map (juxt :name :value))
                                          (into {}))
                                     %))
                (update :files #(merge (->> module :content :files
                                            (map (juxt :file :file-content))
                                            (into {}))
                                       %)))))

(reg-event-fx
  ::set-module
  (fn [{db :db} [_ db-path href module on-success-event reset-to]]
    (let [overwrite-map               (get-in db (db-module-overwrite-path db-path href))
          update-env-vars             #(overwrite-env % (:env overwrite-map))
          overwrite-module-env        #(update-module-env-vars % update-env-vars)
          update-files                #(overwrite-files % (:files overwrite-map))
          overwrite-module-files      #(update-module-files % update-files)
          overwrite-module-regs-creds #(if-let [registries-credentials (:registries-credentials overwrite-map)]
                                         (update-module-registries-credentials % (constantly registries-credentials))
                                         %)]
      {:db (-> (-> module
                   overwrite-module-env
                   overwrite-module-files
                   overwrite-module-regs-creds
                   (set-db-module db db-path href))
               (init-reset-to db-path href module reset-to))
       :fx (cond-> [[:dispatch [::load-infra-registries db-path href]]]
                   on-success-event (conj [:dispatch on-success-event]))})))

(reg-event-fx
  ::load-module
  (fn [{db :db} [_ db-path href overwrite on-success-event reset-to]]
    {:db               (-> db
                           (assoc-in (db-module-overwrite-path db-path href) overwrite))
     ::cimi-api-fx/get [href #(dispatch [::set-module db-path href % on-success-event reset-to])]}))

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
                  (general-utils/filter-eq-ids private-registries))
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
                  (general-utils/filter-eq-parent-vals private-registries))
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
          update-module-env-var #(update-module-env-vars % update-env-vars)
          module                (db-module db db-path href)
          versioned-href        (str href "_" (db-selected-version db db-path href))
          env-var-name          (get-in (module-env-vars module) [index :name])]
      {:db (-> db
               (assoc-in (conj (db-module-overwrite-path db-path versioned-href) :env env-var-name) new-value)
               (update-db-module db-path href update-module-env-var))
       :fx [(when change-event [:dispatch change-event])]})))

(reg-event-fx
  ::reset-env-value
  (fn [{db :db} [_ db-path href index]]
    #_(let [change-event          (get-in db (conj db-path change-event-env-variables))
            update-env-vars       #(reset-env-value-by-index % index)
            update-module-env-var #(update-module-env-vars % update-env-vars)]
        {:db (update-db-module db db-path href update-module-env-var)
         :fx [(when change-event [:dispatch change-event])]})
    (let [versioned-href         (str href "_" (db-selected-version db db-path href))
          module                 (db-module db db-path href)
          env-vars               (get-in module module-env-vars-path)
          env-var                (get env-vars index)
          reset-to               (get-in db (db-module-reset-to-path db-path versioned-href))
          reset-to-env-var-value (get-in reset-to [:env (:name env-var)])]
      {:fx [[:dispatch [::update-env db-path href index reset-to-env-var-value]]]})))

(reg-event-fx
  ::update-file-content
  (fn [{db :db} [_ db-path href index file-content]]
    (let [change-event       (get-in db (conj db-path change-event-files))
          update-files       #(update-file-content-by-index % index file-content)
          update-module-file #(update-module-files % update-files)
          module             (db-module db db-path href)
          versioned-href     (str href "_" (db-selected-version db db-path href))
          file-name          (get-in (module-files module) [index :file-name])]
      {:db (-> db
               (assoc-in (conj (db-module-overwrite-path db-path versioned-href) :files file-name) file-content)
               (update-db-module db-path href update-module-file))
       :fx [(when change-event [:dispatch change-event])]})))

(reg-event-fx
  ::update-file-override-and-reset
  (fn [{db :db} [_ db-path href index file-content]]
    (let [change-event       (get-in db (conj db-path change-event-files))
          update-files       #(reset-file-content-by-index % index)
          update-module-file #(update-module-files % update-files)
          module             (db-module db db-path href)
          versioned-href     (str href "_" (db-selected-version db db-path href))
          file-name          (get-in (module-files module) [index :file-name])]
      {:db (-> db
               (assoc-in (conj (db-module-overwrite-path db-path versioned-href) :files file-name) file-content)
               (update-db-module db-path href update-module-file))
       :fx [(when change-event [:dispatch change-event])]})))

(reg-event-fx
  ::reset-file-content
  (fn [{db :db} [_ db-path href index]]
    (let [versioned-href        (str href "_" (db-selected-version db db-path href))
          module                (db-module db db-path href)
          files                 (get-in module module-files-path)
          file                  (get files index)
          reset-to              (get-in db (db-module-reset-to-path db-path versioned-href))
          reset-to-file-content (get-in reset-to [:files (:file-name file)])]
      {:fx [[:dispatch [::update-file-content db-path href index reset-to-file-content]]]})))

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

(reg-sub
  ::module
  (fn [db [_ db-path href]]
    (db-module db db-path href)))

(reg-sub
  ::module-overwrite
  (fn [db [_ db-path href]]
    (let [versioned-href (str href "_" (db-selected-version db db-path href))]
      (get-in db (db-module-overwrite-path db-path versioned-href)))))

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
          change-event     (get-in db (conj db-path change-event-module-version))
          overwrites       (get-in db (db-module-overwrite-path db-path new-version-href))]
      {:db (assoc-in db (db-new-version-module-href-path db-path href) version-module-href)
       :fx [[:dispatch [::load-module db-path new-version-href
                        overwrites
                        (when change-event (conj change-event new-version-href))]]]})))

(defn latest-published-version
  [db db-path href]
  (let [module (db-module db db-path href)]
    (->> module
         :versions
         (filter :published)
         reverse
         (map :href)
         first)))

(defn latest-published-version-id
  [db db-path href]
  (let [module           (db-module db db-path href)
        version-id       (latest-published-version db db-path href)
        versions-indexed (module-versions-indexed module)]
    (get-version-id versions-indexed version-id)))

(reg-sub
  ::latest-published-version
  (fn [db [_ db-path href]]
    (latest-published-version db db-path href)))

(defn is-behind-latest-published-version?
  [db db-path href]
  (let [version-id                  (db-selected-version db db-path href)
        latest-published-version-id (latest-published-version-id db db-path href)]
    (some->> latest-published-version-id (< version-id))))

(reg-sub
  ::is-behind-latest-published-version?
  (fn [db [_ db-path href]]
    (is-behind-latest-published-version? db db-path href)))

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
  (keep (fn [{:keys [::new-value :name]}]
          (when (some? new-value)
            {:name  name
             :value new-value})
          ) env-vars))

(defn db-changed-env-vars
  [db db-path href]
  (changed-env-vars (db-module-env-vars db db-path href)))

(defn- db-module-files
  [db db-path href]
  (module-files (db-module db db-path href)))

(defn- changed-files
  [files]
  (keep (fn [{:keys [::new-file-content :file-name]}]
          (when (some? new-file-content)
            {:file-name    file-name
             :file-content new-file-content})
          ) files))

(defn db-changed-files
  [db db-path href]
  (changed-files (db-module-files db db-path href)))

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
  ::module-overridden-env-var?
  (fn [db [_ db-path href index]]
    (let [module                 (db-module db db-path href)
          env-vars               (get-in module module-env-vars-path)
          env-var                (get env-vars index)
          versioned-href         (str href "_" (db-selected-version db db-path href))
          env-var-value          (env-vars-value-by-index (db-module-env-vars db db-path versioned-href) index)
          reset-to               (get-in db (db-module-reset-to-path db-path versioned-href))
          reset-to-env-var-value (get-in reset-to [:env (:name env-var)])]
      (and (some? env-var-value)
           (not= reset-to-env-var-value env-var-value)))))

(reg-sub
  ::module-env-vars-in-error
  (fn [db [_ db-path href]]
    (db-module-env-vars-in-error db db-path href)))

(reg-sub
  ::module-new-file-content
  (fn [db [_ db-path href index]]
    (new-file-content-by-index (db-module-files db db-path href) index)))

(reg-sub
  ::module-overridden-file-content?
  (fn [db [_ db-path href index]]
    (let [file-content          (new-file-content-by-index (db-module-files db db-path href) index)
          module                (db-module db db-path href)
          files                 (get-in module module-files-path)
          file                  (get files index)
          versioned-href        (str href "_" (db-selected-version db db-path href))
          reset-to              (get-in db (db-module-reset-to-path db-path versioned-href))
          reset-to-file-content (get-in reset-to [:files (:file-name file)])]
      (and (some? file-content)
           (not= reset-to-file-content file-content)))))

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
  [env-name _value _error? _on-change]
  (let [tr      (subscribe [::i18n-subs/tr])
        options (r/atom [])
        subtype (get cred-env-var-map env-name)]
    (dispatch [::get-credentials-opts subtype #(reset! options %)])
    (fn [_env-name value error? on-change]
      [ui/Dropdown
       {:clearable   true
        :selection   true
        :fluid       true
        :value       value
        :error       error?
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
      [EnvCredential env-name value error? on-change]
      ^{:key (str href "-" i)}
      [ui/FormInput
       {:type      "text"
        :name      env-name
        :value     value
        :read-only read-only?
        :error     error?
        :fluid     true
        :on-change (ui-callback/input-callback on-change)}])))

(defn AsFormInput
  [db-path href read-only? error?
   i {env-name        :name
      env-description :description
      env-required    :required :as env-variable}
   show-required?]
  (let [tr              (subscribe [::i18n-subs/tr])
        overridden?     @(subscribe [::module-overridden-env-var? db-path href i])
        reset-env-value (fn [event]
                          (dispatch [::reset-env-value db-path href i])
                          (.preventDefault event)
                          (.stopPropagation event))]
    [ui/FormField
     [uix/FieldLabel {:name       env-name
                      :required?  (if show-required? env-required false)
                      :help-popup [uix/HelpPopup env-description]}
      (when overridden?
        [:div {:style {:float :right}}
         [tt/WithTooltip
          [icons/EditIcon]
          (@tr [:overridden-value])]
         (when-not read-only?
           [tt/WithTooltip
            [:a {:href     "#"
                 :on-click reset-env-value}
             [icons/UndoIcon {:style {:color "black"}}]]
            (@tr [:overridden-value-revert-to-default])])])]
     [EnvVarInput db-path href read-only? error? i env-variable]]))

(defn EnvVariables
  [{:keys [db-path href change-event read-only? highlight-errors? show-required?]
    :or   {read-only?     false
           show-required? true}
    :as   _opts}]
  (dispatch [::helpers/set db-path change-event-env-variables change-event])
  (let [tr            @(subscribe [::i18n-subs/tr])
        module        @(subscribe [::module db-path href])
        env-variables (module-env-vars module)
        vars-in-error (if highlight-errors?
                        @(subscribe [::module-env-vars-in-error db-path href])
                        #{})]
    (if (seq env-variables)
      [ui/Form
       (map-indexed
         (fn [i env-variable]
           (let [var-in-error (boolean (vars-in-error (:name env-variable)))]
             ^{:key (str (:name env-variable) "_" i)}
             [AsFormInput db-path href read-only? var-in-error
              i env-variable show-required?]))
         env-variables)]
      [ui/Message (tr [:module-no-env-variables])])))

(defn SingleFile
  [{:keys [db-path href read-only?]
    :or   {read-only? false}
    :as   _opts} _idx _file]
  (fn [_opts index {:keys [file-name file-content]}]
    (let [tr                    @(subscribe [::i18n-subs/tr])
          reset-file-content    (fn [event]
                                  (dispatch [::reset-file-content db-path href index])
                                  (.preventDefault event)
                                  (.stopPropagation event))
          updated-file-content  @(subscribe [::module-new-file-content db-path href index])
          overridden?           @(subscribe [::module-overridden-file-content? db-path href index])
          module-overrides      @(subscribe [::module-overwrite db-path href])
          file-content-override (get-in module-overrides [:files file-name])
          merged-file-content   (or updated-file-content file-content-override file-content "")
          on-change             #(dispatch [::update-file-content db-path href index %])]
      [ui/TableRow {:key index, :vertical-align "top"}
       [ui/TableCell {:floated :left
                      :width   3}
        [:span file-name]]
       [ui/TableCell {:floated :left
                      :width   12}
        [ui/Form
         [ui/TextArea {:rows      10
                       :read-only read-only?
                       :value     merged-file-content
                       :on-change (ui-callback/value on-change)}]]]
       [ui/TableCell {:floated :right
                      :width   1
                      :align   :right}
        (when overridden?
          [:div {:style {:float :right}}

           [tt/WithTooltip
            [icons/EditIcon]
            (tr [:overridden-value])]
           (when-not read-only?
             [tt/WithTooltip
              [:a {:href     "#"
                   :on-click reset-file-content}
               [icons/UndoIcon {:style {:color "black"}}]]
              (tr [:overridden-value-revert-to-default])])])]])))

(defn Files
  [{:keys [db-path href read-only? change-event]
    :as   opts}]
  (dispatch [::helpers/set db-path change-event-files change-event])
  (let [tr     @(subscribe [::i18n-subs/tr])
        module @(subscribe [::module db-path href])
        files  (module-files module)]
    (if (empty? files)
      [ui/Message
       (str/capitalize (str (tr [:no-files]) "."))]
      [:div [ui/Table {:style {:margin-top 10}}
             [ui/TableHeader
              [ui/TableRow
               [ui/TableHeaderCell {:content (str/capitalize (tr [:filename]))}]
               [ui/TableHeaderCell {:content (str/capitalize (tr [:content]))}]
               (when-not read-only?
                 [ui/TableHeaderCell {:content (str/capitalize (tr [:action]))}])]]
             [ui/TableBody
              (for [[idx file] (map-indexed vector files)]
                ^{:key (str "file_" idx)}
                [SingleFile opts idx file])]]])))

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
  (let [tr                 (subscribe [::i18n-subs/tr])
        module             @(subscribe [::module db-path href])
        private-registries (module-private-registries module)
        loading?           @(subscribe [::registries-loading? db-path href])]
    (dispatch [::helpers/set db-path change-event-registries-credentials change-event])
    (if (seq private-registries)
      [ui/Form {:loading loading?}
       (for [[i private-registry] (map-indexed vector private-registries)]
         ^{:key (str href "-" private-registry)}
         [DropdownContainerRegistry opts i private-registry])]
      [ui/Message (@tr [:no-container-regs-defined])])))

(defn LinkToAppView
  [{:keys [path version-id target]} children]
  (let [href (str-pathify (name->href routes/apps)
                          (str path "?version=" version-id))]
    [:a {:href     href
         :target   (or target "_blank")
         :on-click (partial uix/link-on-click href)}
     children]))

(defn LinkToApp
  [{:keys [db-path href children target]
    :as   _opts}]
  (let [{:keys [path content]} @(subscribe [::module db-path href])
        versions-indexed (subscribe [::module-versions-indexed db-path href])
        version-id       (get-version-id @versions-indexed (:id content))]
    [LinkToAppView {:path path :version-id version-id :target target}
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
