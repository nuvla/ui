(ns sixsq.nuvla.ui.deployment-dialog.views-env-variables
  (:require [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.credentials.events :as creds-events]
            [sixsq.nuvla.ui.credentials.subs :as creds-subs]
            [sixsq.nuvla.ui.deployment-dialog.events :as events]
            [sixsq.nuvla.ui.deployment-dialog.subs :as subs]
            [sixsq.nuvla.ui.deployment-dialog.utils :as utils]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.utils.form-fields :as ff]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn SummaryRow
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
  (let [tr (subscribe [::i18n-subs/tr])]
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
  [index {env-name        :name
          env-description :description
          env-value       :value
          env-required    :required}]
  (let [deployment     (subscribe [::subs/deployment])
        creds          (subscribe [::creds-subs/credentials])
        selected-creds (filter-creds env-name @creds)]
    [ui/FormField {:required env-required}
     [:label env-name ff/nbsp (ff/help-popup env-description)]
     (if selected-creds
       [CredentialsDropdown env-value index deployment selected-creds]
       [ui/Input
        {:type          "text"
         :name          env-name
         :default-value (or env-value "")
         :summary-page  false
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
          [AsFormInput i env-variable])
        @env-variables)]]))
