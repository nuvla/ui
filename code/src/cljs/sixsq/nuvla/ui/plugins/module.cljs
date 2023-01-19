(ns sixsq.nuvla.ui.plugins.module
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-fx reg-sub subscribe]]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.utils.form-fields :as ff]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn- overwrite-env
  [environment-variables env]
  (mapv (fn [{env-name :name env-value :value :as environment-variable}]
          (assoc environment-variable :value (get env env-name env-value)))
        environment-variables))

(defn- overwrite-module
  [module {:keys [env] :as _overwrite}]
  (cond-> module
          (seq env) (update-in [:content :environmental-variables] overwrite-env env)))

(reg-event-fx
  ::load-module
  (fn [{db :db} [_ db-path href overwrite]]
    (let [path-overwrite (conj db-path ::overwrite)
          overwrite-map  (or overwrite (get-in db path-overwrite))
          module-id      (some-> href (str/split "_") first)]
      (cond-> {::cimi-api-fx/get
               [href #(dispatch [::helpers/set (conj db-path ::modules)
                                 module-id (if overwrite-map
                                             (overwrite-module % overwrite-map)
                                             %)])]}
              overwrite (assoc :db (assoc-in db path-overwrite overwrite))))))

(reg-event-fx
  ::change-version
  (fn [{db :db} [_ db-path href]]
    (let [change-event (get-in db (conj db-path ::change-event))]
      {:fx [[:dispatch [::load-module db-path href]]
            (when change-event
              [:dispatch change-event])]})))

(reg-event-fx
  ::update-env
  (fn [{db :db} [_ db-path href index new-value]]
    (let [change-event (get-in db (conj db-path ::change-event))]
      {:db (assoc-in db (conj db-path
                              ::modules href :content :environmental-variables
                              index ::new-value) new-value)
       :fx [(when change-event [:dispatch change-event])]})))

(defn get-version-id
  [module-versions version]
  (some (fn [[idx {:keys [href]}]] (when (= version href) idx)) module-versions))

(defn- module-db
  [db db-path href]
  (get-in db (conj db-path ::modules href)))

(defn- module-versions-indexed
  [module]
  (-> module :versions apps-utils/map-versions-index))

(reg-sub
  ::module
  (fn [db [_ db-path href]]
    (module-db db db-path href)))

(defn db-selected-version
  [db db-path href]
  (let [module-content-id (get-in db (conj db-path ::modules href :content :id))
        versions-indexed  (-> db
                              (module-db db-path href)
                              module-versions-indexed)]
    (get-version-id versions-indexed module-content-id)))


(defn db-environment-variables
  [db db-path href]
  (get-in db (conj db-path ::modules href :content :environmental-variables)))

(defn db-license-accepted?
  [db db-path href]
  (let [license (get-in db (conj db-path ::modules href :license))]
    (or (nil? license)
        (get license ::accepted? false))))

(defn db-price-accepted?
  [db db-path href]
  (let [price (get-in db (conj db-path ::modules href :price))]
    (or (nil? price)
        (get price ::accepted? false))))

(defn db-coupon
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
    (get-in db (conj db-path ::modules href :content :environmental-variables index ::new-value))))

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
      {:type      "text"
       :name      env-name
       :value     (or updated-env-value env-value "")
       :read-only read-only?
       :fluid     true
       :on-change (ui-callback/input-callback
                    #(dispatch [::update-env db-path href index %]))}]]))

(defn EnvVariables
  [{:keys [db-path href change-event read-only?]
    :or   {read-only? false}
    :as   _opts}]
  (let [module        @(subscribe [::module db-path href])
        env-variables (get-in module [:content :environmental-variables])]
    (dispatch [::helpers/set db-path ::change-event change-event])
    (if (seq env-variables)
      [ui/Form
       (map-indexed
         (fn [i env-variable]
           ^{:key (str (:name env-variable) "_" i)}
           [AsFormInput db-path href read-only? i env-variable])
         env-variables)]
      [ui/Message "No environment variables defined"])))

(defn AcceptLicense
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

(defn AcceptPrice
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

(defn ModuleVersions
  [{:keys [db-path href change-event read-only?]
    :or   {read-only? false}
    :as   _opts}]
  (let [module           (subscribe [::module db-path href])
        versions-indexed (subscribe [::module-versions-indexed db-path href])
        options          (subscribe [::module-versions-options db-path href])]
    (dispatch [::helpers/set db-path ::change-event change-event])
    (let [{:keys [id content]} @module]
      [ui/FormDropdown
       {:value     (:id content)
        :scrolling true
        :upward    false
        :selection true
        :on-change #(dispatch [::change-version db-path
                               (str id "_" (get-version-id @versions-indexed %))])
        :fluid     true
        :options   @options
        :disabled  read-only?}])))

(s/def ::href string?)

(s/fdef ModuleVersions
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path
                                            ::href]
                                   :opt-un [::helpers/read-only?])))
