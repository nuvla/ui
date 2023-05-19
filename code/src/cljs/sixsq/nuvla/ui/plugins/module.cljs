(ns sixsq.nuvla.ui.plugins.module
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-sub subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.utils.form-fields :as ff]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
            [sixsq.nuvla.ui.utils.values :as values]))


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


(defn- db-module
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
  (fn [{db :db} [_ db-path href module]]
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
       :fx [[:dispatch [::load-infra-registries db-path href]]]})))

(reg-event-fx
  ::load-module
  (fn [{db :db} [_ db-path href overwrite]]
    {:db               (assoc-in db (db-module-overwrite-path db-path href) overwrite)
     ::cimi-api-fx/get [href #(dispatch [::set-module db-path href %])]}))

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


;#_(reg-event-fx
;  ::get-infra-registries
;  (fn [{:keys [db]} [_ registry-ids reg-creds-ids]]
;    {:db                  (assoc db ::spec/infra-registries-loading? true)
;     ::cimi-api-fx/search [:infrastructure-service
;                           {:filter  (general-utils/join-and
;                                       "subtype='registry'"
;                                       (apply general-utils/join-or
;                                              (map #(str "id='" % "'") registry-ids))),
;                            :select  "id, name, description"
;                            :orderby "name:asc,id:asc"}
;                           #(dispatch [::set-infra-registries registry-ids reg-creds-ids %])]}))
(reg-event-fx
  ::change-version
  (fn [{db :db} [_ db-path href]]
    (let [change-event (get-in db (conj db-path change-event-module-version))]
      {:fx [[:dispatch [::load-module db-path href]]
            (when change-event
              [:dispatch change-event])]})))

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

(defn- get-version-id
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


(defn- db-module-env-vars
  [db db-path href]
  (-> (db-module db db-path href)
      module-env-vars))

(defn- db-module-registries-credentials
  [db db-path href]
  (-> (db-module db db-path href)
      module-registries-credentials))

(defn- changed-env-vars
  [env-vars]
  (keep (fn [{:keys [::new-value :value :name]}]
          (when (some-> new-value (not= value))
            {:name  name
             :value new-value})
          ) env-vars))

(defn db-changed-env-vars
  [db db-path href]
  (-> (db-module-env-vars db db-path href)
      changed-env-vars))

#_(defn db-license-accepted?
    [db db-path href]
    (let [license (get-in db (conj db-path ::modules href :license))]
      (or (nil? license)
          (get license ::accepted? false))))

#_(defn db-price-accepted?
    [db db-path href]
    (let [price (get-in db (conj db-path ::modules href :price))]
      (or (nil? price)
          (get price ::accepted? false))))

#_(defn db-coupon
    [db db-path href]
    (let [coupon (get-in db (conj db-path ::modules href :price ::coupon))]
      (when-not (str/blank? coupon)
        coupon)))

(reg-sub
  ::module-versions-indexed
  (fn [[_ db-path href]]
    (subscribe [::module db-path href]))
  module-versions-indexed)

(reg-sub
  ::module-env-value
  (fn [db [_ db-path href index]]
    (-> (db-module-env-vars db db-path href)
        (env-vars-value-by-index index))))

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
    (map (fn [[idx {:keys [href commit published]}]]
           {:key   idx
            :value href
            :text  (str "v" idx " | " (general-utils/truncate commit 70)
                        (when (true? published) (str " | " (tr [:published]))))
            :icon  (when published apps-utils/publish-icon)})
         versions-indexed)))

#_(def cred-env-var-map
    {"S3_CRED"  "infrastructure-service-minio"
     "GPG_CRED" "gpg-key"})

#_(defn is-cred-env-var?
    [env-var-name]
    (contains? (set (keys cred-env-var-map)) env-var-name))

#_(defn filter-creds
    [env-name creds]
    (when (is-cred-env-var? env-name)
      (filter #(when (= (get cred-env-var-map env-name) (:subtype %)) %) creds)))

(defn AsFormInput
  [db-path href read-only?
   index {env-name        :name
          env-description :description
          env-value       :value
          env-required    :required}]
  (let [updated-env-value @(subscribe [::module-env-value db-path href index])]
    [ui/FormField {:required env-required}
     [:label env-name ff/nbsp (ff/help-popup env-description)]
     [ui/Input
      {:type          "text"
       :name          env-name
       :default-value (or updated-env-value env-value "")
       :read-only     read-only?
       :fluid         true
       :on-change     (ui-callback/input-callback
                        #(dispatch [::update-env db-path href index %]))}]]))

(defn EnvVariables
  [{:keys [db-path href change-event read-only?]
    :or   {read-only? false}
    :as   _opts}]
  (dispatch [::helpers/set db-path change-event-env-variables change-event])
  (let [tr            @(subscribe [::i18n-subs/tr])
        module        @(subscribe [::module db-path href])
        env-variables (module-env-vars module)]
    (if (seq env-variables)
      [ui/Form
       (map-indexed
         (fn [i env-variable]
           ^{:key (str (:name env-variable) "_" i)}
           [AsFormInput db-path href read-only? i env-variable])
         env-variables)]
      [ui/Message (tr [:module-no-env-variables])])))

(defn DropdownContainerRegistry
  [db-path href i private-registry]
  (let [tr             @(subscribe [::i18n-subs/tr])
        {:keys [id name description]} @(subscribe [::resolved-private-registry db-path href private-registry])
        options        @(subscribe [::private-registry-creds-options db-path href private-registry])
        value          @(subscribe [::selected-registry-credential db-path href i])
        preselected?   (and (some? value)
                            (zero? (count options)))
        registry-label (r/as-element
                         [:label (or name id) ff/nbsp
                          (when description (ff/help-popup description))])
        ;registry       (subscribe [::subs/infra-registry private-registry-id])
        ;registry-name  (or (:name @registry) private-registry-id)
        ;creds-options  (subscribe [::subs/infra-registries-creds-by-parent-options
        ;                           private-registry-id])
        ;registry-descr (:description @registry)
        ;{:keys [cred-id preselected?]} info
        ]
    (if preselected?
      [ui/FormInput
       {:disabled      true
        :label         registry-label
        :default-value (tr [:preselected])}]
      [ui/FormDropdown
       (cond->
         {:required    true
          :label       registry-label
          :selection   true
          :value       value
          ;:default-value cred-id
          :error       (when-not (seq options)
                         (tr [:no-available-creds-registry]))
          :placeholder (tr [:select-credential])
          :options     options
          :on-change   (ui-callback/value
                         #(dispatch [::update-registry-credential db-path href i %]))
          }
         #_(empty? @creds-options) #_(assoc :error (@tr [:no-available-creds-registry])))])
    ;(if (and preselected?
    ;         (not (some #(= cred-id (:value %)) @creds-options)))
    ;  [ui/FormInput
    ;   {:disabled      true
    ;    :label         (r/as-element [:label registry-name ff/nbsp
    ;                                  (when registry-descr (ff/help-popup registry-descr))])
    ;    :default-value (@tr [:preselected])}]
    ;  [ui/FormDropdown
    ;   (cond->
    ;     {:required      true
    ;      :label         (r/as-element [:label registry-name ff/nbsp
    ;                                    (when registry-descr (ff/help-popup registry-descr))
    ;                                    (when cred-id
    ;                                      [:span " "
    ;                                       [creds-comp/CredentialCheckPopup cred-id]])])
    ;      :selection     true
    ;      :default-value cred-id
    ;      :placeholder   (@tr [:select-credential])
    ;      :options       @creds-options
    ;      :on-change     (ui-callback/value
    ;                       #(dispatch [::events/set-credential-registry private-registry-id %]))}
    ;     (empty? @creds-options) (assoc :error (@tr [:no-available-creds-registry])))]
    ;  )
    ))

(defn RegistriesCredentials
  [{:keys [db-path href change-event read-only?]
    :or   {read-only? false}
    :as   _opts}]
  (let [module             @(subscribe [::module db-path href])
        private-registries (module-private-registries module)
        loading?           @(subscribe [::registries-loading? db-path href])]
    (dispatch [::helpers/set db-path change-event-registries-credentials change-event])
    ; load infra reg, help name resolution
    ; load cred reg group by parent needed for options
    ; look at module preselected and create
    ; for each infra reg, when only one cred select it
    ; deployment-set creds-registries
    ; dropdown value preselected or choice to user
    (if (seq private-registries)
      #_[ui/Form
         (map-indexed
           (fn [i env-variable]
             ^{:key (str (:name env-variable) "_" i)}
             [AsFormInput db-path href read-only? i env-variable])
           env-variables)]
      #_[:div (str private-registries)]
      [ui/Form {:loading loading?}
       (for [[i private-registry] (map-indexed vector private-registries)]
         ^{:key (str href "-" private-registry)}
         [DropdownContainerRegistry db-path href i private-registry])]
      [ui/Message "No container registries defined"])

    #_[ui/Form {:loading @loading?}
       (for [[private-registry-id info] @private-registries]
         ^{:key private-registry-id}
         [DropdownContainerRegistry private-registry-id info])]))

#_(defn AcceptLicense
    [{:keys [db-path href] :as _opts}]
    (let [tr     @(subscribe [::i18n-subs/tr])
          module @(subscribe [::module db-path href])
          {:keys [name description url] :as license} (:license module)]
      (if license
        [ui/Container
         [ui/Header {:as      :h4
                     :icon    "book"
                     :content (tr [:eula-full])}]
         [:h4 [:b (str (str/capitalize (tr [:eula])) ": ")
               [:u [:a {:href url :target "_blank"} name]]]]
         (when description
           [:p [:i description]])
         [ui/Checkbox {:label     (tr [:accept-eula])
                       :checked   (get license ::accepted? false)
                       :on-change (ui-callback/checked
                                    #(dispatch [::helpers/set (conj db-path ::modules href :license)
                                                ::accepted? %]))}]]
        [ui/Message (tr [:eula-not-defined])])))

#_(defn AcceptPrice
    [{:keys [db-path href] :as _opts}]
    (let [tr           @(subscribe [::i18n-subs/tr])
          module       @(subscribe [::module db-path href])
          price        (:price module)
          format-price #(if (>= (:cent-amount-daily %) 100)
                          (str (float (/ (:cent-amount-daily %) 100)) "€/" (tr [:day]))
                          (str (:cent-amount-daily %) "ct€/" (tr [:day])))]
      (if price
        [:<>
         [ui/Segment
          [:p
           (str (if (:follow-customer-trial price)
                  (tr [:trial-deployment-follow])
                  (tr [:trial-deployment]))
                (tr [:deployment-will-cost]))

           [:b (format-price price)]]
          [ui/Checkbox {:label     (tr [:accept-costs])
                        :checked   (get price ::accepted? false)
                        :on-change (ui-callback/checked
                                     #(dispatch [::helpers/set (conj db-path ::modules href :price)
                                                 ::accepted? %]))}]]
         ^{:key href}
         [ui/Input
          {:label         (tr [:coupon])
           :placeholder   (tr [:code])
           :default-value (get price ::coupon "")
           :on-change     (ui-callback/input-callback
                            #(dispatch [::helpers/set (conj db-path ::modules href :price)
                                        ::coupon %]))}]]
        [ui/Message (tr [:free-app])])))

(defn ModuleNameIcon
  [{:keys [db-path href children show-version?]
    :as   _opts}]
  (let [{:keys [id path name subtype content]} @(subscribe [::module db-path href])
        versions-indexed (subscribe [::module-versions-indexed db-path href])
        version-id       (get-version-id @versions-indexed (:id content))
        label            (cond-> (or name id)
                                 show-version? (str " v" version-id))
        href             (str path "?version=" version-id)]
    [ui/ListItem
     [apps-utils/SubtypeDockerK8sListIcon subtype]
     [ui/ListContent
      [values/AsLink href :label label :page "apps"]
      children]]))

(defn ModuleVersions
  [{:keys [db-path href change-event read-only?]
    :or   {read-only? false}
    :as   _opts}]
  (let [module           (subscribe [::module db-path href])
        versions-indexed (subscribe [::module-versions-indexed db-path href])
        options          (subscribe [::module-versions-options db-path href])]
    (dispatch [::helpers/set db-path change-event-module-version change-event])
    (let [{:keys [id content]} @module]
      [ui/FormDropdown
       {:value     (:id content)
        :scrolling true
        :upward    false
        :selection true
        :on-change (ui-callback/value
                     #(dispatch [::change-version db-path
                                 (str id "_" (get-version-id @versions-indexed %))]))
        :fluid     true
        :options   @options
        :disabled  read-only?}])))

(s/def ::href string?)

(s/fdef ModuleVersions
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path
                                            ::href]
                                   :opt-un [::helpers/read-only?
                                            ::helpers/change-event])))
