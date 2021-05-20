(ns sixsq.nuvla.ui.deployment-dialog.views-env-variables
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.credentials.events :as creds-events]
    [sixsq.nuvla.ui.credentials.subs :as creds-subs]
    [sixsq.nuvla.ui.deployment-dialog.events :as events]
    [sixsq.nuvla.ui.deployment-dialog.subs :as subs]
    [sixsq.nuvla.ui.deployment-dialog.utils :as utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.form-fields :as ff]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [clojure.string :as str]))


(defn summary-row
  []
  (let [tr            (subscribe [::i18n-subs/tr])
        env-variables (subscribe [::subs/env-variables])
        completed?    (subscribe [::subs/env-variables-completed?])

        description   (str "Count: " (count @env-variables))
        on-click-fn   #(dispatch [::events/set-active-step :env-variables])]

    ^{:key "env-variables"}
    [ui/TableRow {:active   false
                  :on-click on-click-fn}
     [ui/TableCell {:collapsing true}
      (if @completed?
        [ui/Icon {:name "list alternate outline", :size "large"}]
        [ui/Icon {:name "warning sign", :size "large", :color "red"}])]
     [ui/TableCell {:collapsing true} (@tr [:env-variables])]
     [ui/TableCell [:div [:span description]]]]))


(defn CredentialsDropdown
  [_env-value _index _deployment _credentials]
  (let [tr   (subscribe [::i18n-subs/tr])]
    (fn [env-value index deployment credentials]
      [ui/Dropdown
       {:clearable   true
        :selection   true
        :fluid       true
        :value       env-value
        :placeholder (@tr [:credentials-select-related-infra])
        :on-change   (ui-callback/value
                       #(dispatch [::events/set-deployment (assoc-in
                                                            @deployment
                                                            [:module :content
                                                             :environmental-variables
                                                             index :value] %)]))
        :options     (map (fn [{id :id, name :name}]
                            {:key id, :value id, :text name})
                          credentials)}])))


(defn filter-s3-creds
  [creds]
  (filter #(when (= "infrastructure-service-minio" (:subtype %)) %) creds))


(defn as-form-input
  [index {env-name        :name
          env-description :description
          env-value       :value
          env-required    :required}]
  (let [deployment (subscribe [::subs/deployment])
        is-cred?   (str/starts-with? env-name "S3_CRED")
        creds      (when is-cred? (subscribe [::creds-subs/credentials]))
        s3-creds   (when creds (filter-s3-creds @creds))]
    [ui/FormField {:required env-required}
     [:label env-name ff/nbsp (ff/help-popup env-description)]
     (if s3-creds
       [CredentialsDropdown env-value index deployment s3-creds]
       [ui/Input
        {:type          "text"
         :name          env-name
         :default-value (or env-value "")
         :read-only     false
         :fluid         true
         :on-change     (ui-callback/input-callback
                          #(dispatch [::events/set-deployment (assoc-in
                                                                @deployment
                                                                [:module :content
                                                                 :environmental-variables
                                                                 index :value] %)]))}])]))


(defmethod utils/step-content :env-variables
  []
  (dispatch [::creds-events/get-credentials])
  (let [env-variables (subscribe [::subs/env-variables])]
    [ui/Segment
     [ui/Form
      (map-indexed
        (fn [i env-variable]
          ^{:key (str (:name env-variable) "_" i)}
          [as-form-input i env-variable])
        @env-variables)]]))
