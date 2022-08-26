(ns sixsq.nuvla.ui.plugins.module-version
  (:require [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [re-frame.core :refer [subscribe dispatch reg-event-fx reg-sub]]
            [sixsq.nuvla.ui.plugins.helpers :as helpers]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [cljs.spec.alpha :as s]
            [sixsq.nuvla.ui.apps.utils :as apps-utils]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
            [clojure.string :as str]
            [sixsq.nuvla.ui.utils.form-fields :as ff]))

(reg-event-fx
  ::load-module
  (fn [_ [_ db-path href]]
    (let [module-id (some-> href (str/split "_") first)]
      {::cimi-api-fx/get
       [href #(dispatch [::helpers/set (conj db-path ::modules)
                         module-id %])]})))

(defn get-version-id
  [module-versions version]
  (some (fn [[idx {:keys [href]}]] (when (= version href) idx)) module-versions))

(defn module-content-id->version-url
  [versions-indexed id module-content-id]
  (->> module-content-id
       (get-version-id versions-indexed)
       (str id "_")))

(defn selected-version
  [db db-path href]
  (let [module-content-id (get-in db (conj db-path ::modules href :content :id))
        versions-indexed  (subscribe [::module-versions-indexed db-path href])]
    (module-content-id->version-url @versions-indexed href module-content-id)))

(reg-sub
  ::module
  (fn [db [_ db-path href]]
    (get-in db (conj db-path ::modules href))))

(reg-sub
  ::module-versions-indexed
  (fn [[_ db-path href]]
    (subscribe [::module db-path href]))
  (fn [{:keys [versions]}]
    (apps-utils/map-versions-index versions)))

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



;(defn CredentialsDropdown
;  [_env-value _index _deployment _credentials]
;  (let [tr (subscribe [::i18n-subs/tr])]
;    (fn [env-value index deployment credentials]
;      [ui/Dropdown
;       {:clearable   true
;        :selection   true
;        :fluid       true
;        :value       env-value
;        :placeholder (@tr [:credentials-select-related-infra])
;        :on-change   (ui-callback/value
;                       #(dispatch [::events/set-deployment (assoc-in
;                                                             @deployment
;                                                             [:module :content
;                                                              :environmental-variables
;                                                              index :value] %)]))
;        :options     (map (fn [{id :id, name :name}]
;                            {:key id, :value id, :text name})
;                          credentials)}])))


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
  [db-path href
   index {env-name        :name
          env-description :description
          env-value       :value
          env-required    :required}]
  (let [
        ;deployment     (subscribe [::subs/deployment])
        ;creds          (subscribe [::creds-subs/credentials])
        ;selected-creds (filter-creds env-name @creds)
        ]
    [ui/FormField {:required env-required}
     [:label env-name ff/nbsp (ff/help-popup env-description)]
     ;(if selected-creds
     ;  [CredentialsDropdown env-value index deployment selected-creds]
     [ui/Input
      {:type      "text"
       :name      env-name
       :value     (or env-value "")
       :read-only false
       :fluid     true
       :on-change (ui-callback/input-callback
                    #(dispatch [::helpers/set (conj db-path ::modules href :content :environmental-variables index) :value %])
                    )}]
     ;)
     ]))


(defn EnvVariables
  [{:keys [db-path href] :as _opts}]
  (let [module        @(subscribe [::module db-path href])
        env-variables (get-in module [:content :environmental-variables])]
    [ui/Segment
     [ui/Form
      (map-indexed
        (fn [i env-variable]
          ^{:key (str (:name env-variable) "_" i)}
          [AsFormInput db-path href i env-variable])
        env-variables)]]))

(defn ModuleVersions
  [{:keys [db-path href] :as _opts}]
  (let [module           (subscribe [::module db-path href])
        versions-indexed (subscribe [::module-versions-indexed db-path href])
        options          (subscribe [::module-versions-options db-path href])]
    (fn [{:keys [db-path href] :as _opts}]
      (let [{:keys [id content]} @module]
        (when (nil? id)
          (dispatch [::load-module db-path href]))
        [:<>
         [ui/FormDropdown
          {:value     (:id content)
           :scrolling true
           :upward    false
           :selection true
           :on-change (ui-callback/value
                        #(dispatch [::load-module db-path
                                    (module-content-id->version-url
                                      @versions-indexed id %)]))
           :fluid     true
           :options   @options}]
         [:div (str content)]

         ]))))

(s/def ::href string?)

(s/fdef ModuleVersions
        :args (s/cat :opts (s/keys :req-un [::helpers/db-path
                                            ::href])))

