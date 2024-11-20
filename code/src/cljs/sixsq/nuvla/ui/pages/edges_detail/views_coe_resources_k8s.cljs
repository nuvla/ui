(ns sixsq.nuvla.ui.pages.edges-detail.views-coe-resources-k8s
  (:require [re-frame.core :refer [dispatch subscribe reg-event-fx reg-sub]]
            [sixsq.nuvla.ui.common-components.plugins.table-refactor :as table]
            [sixsq.nuvla.ui.pages.edges-detail.views-coe-resources :as coe]
            [reagent.core :as r]
            [sixsq.nuvla.ui.pages.edges-detail.subs :as subs]
            [sixsq.nuvla.ui.utils.time :as time]))

(def field-uid {::table/field-key      :uid
                ::table/header-content "UID"})
(def field-creation-timestamp {::table/field-key      :creation_timestamp
                               ::table/header-content "Creation timestamp"
                               ::table/field-cell     table/CellTimeAgo})
(def field-resource-version {::table/field-key      :resource_version
                             ::table/header-content "Resource version"})
(def field-namespace {::table/field-key      :namespace
                      ::table/header-content "Namespace"})
(def field-name {::table/field-key      :name
                 ::table/header-content "Name"})
(def field-raw {::table/field-key      :raw
                ::table/header-content "Raw"
                ::table/no-sort?       true
                ::table/field-cell     coe/CellJson})

(defn CellTimeAgoExtraFormat
  [cell-data row column]
  [table/CellTimeAgo
   (some-> cell-data time/parse-json time/time->utc-str) row column])

(defn Tab []
  (r/with-let [k8s-images                 {::coe/!data            (subscribe [::subs/k8s-images])
                                           ::coe/!columns         (r/atom [{::table/field-key      :names
                                                                            ::table/header-content "Names"
                                                                            ::table/field-cell     coe/CellLabelGroup}
                                                                           {::table/field-key      :size_bytes
                                                                            ::table/header-content "Size"
                                                                            ::table/field-cell     table/CellBytes}
                                                                           field-raw])
                                           ::coe/!default-columns (r/atom [:names :size_bytes :raw])
                                           ::coe/resource-type    "image"
                                           ::coe/row-id-fn        :names}
               k8s-namespaces             {::coe/!data            (subscribe [::subs/k8s-namespaces])
                                           ::coe/!columns         (r/atom [field-uid
                                                                           field-name
                                                                           field-creation-timestamp
                                                                           field-resource-version
                                                                           field-raw])
                                           ::coe/!default-columns (r/atom [:name :creation_timestamp :raw])
                                           ::coe/resource-type    "namespace"
                                           ::coe/row-id-fn        :uid}
               k8s-pods                   {::coe/!data            (subscribe [::subs/k8s-pods])
                                           ::coe/!columns         (r/atom [field-uid
                                                                           field-name
                                                                           field-creation-timestamp
                                                                           field-resource-version
                                                                           field-namespace
                                                                           {::table/field-key      :phase
                                                                            ::table/header-content "Status"}
                                                                           field-raw])
                                           ::coe/!default-columns (r/atom [:name :creation_timestamp :namespace :phase :raw])
                                           ::coe/resource-type    "pod"
                                           ::coe/row-id-fn        :uid}
               k8s-nodes                  {::coe/!data            (subscribe [::subs/k8s-nodes])
                                           ::coe/!columns         (r/atom [field-uid
                                                                           field-name
                                                                           field-creation-timestamp
                                                                           field-resource-version
                                                                           {::table/field-key      :node_info
                                                                            ::table/header-content "NodeInfo"
                                                                            ::table/field-cell     coe/CellKeyValueLabelGroup}
                                                                           field-raw])
                                           ::coe/!default-columns (r/atom [:name :creation_timestamp :node_info :raw])
                                           ::coe/resource-type    "node"
                                           ::coe/row-id-fn        :uid}
               k8s-configmaps             {::coe/!data            (subscribe [::subs/k8s-configmaps])
                                           ::coe/!columns         (r/atom [field-uid
                                                                           field-name
                                                                           field-creation-timestamp
                                                                           field-resource-version
                                                                           field-namespace
                                                                           field-raw])
                                           ::coe/!default-columns (r/atom [:name :creation_timestamp :namespace :raw])
                                           ::coe/resource-type    "configmap"
                                           ::coe/row-id-fn        :uid}
               k8s-secrets                {::coe/!data            (subscribe [::subs/k8s-secrets])
                                           ::coe/!columns         (r/atom [field-uid
                                                                           field-name
                                                                           field-creation-timestamp
                                                                           field-resource-version
                                                                           field-namespace
                                                                           {::table/field-key      :type
                                                                            ::table/header-content "Type"}
                                                                           field-raw])
                                           ::coe/!default-columns (r/atom [:name :creation_timestamp :namespace :type :raw])
                                           ::coe/resource-type    "secret"
                                           ::coe/row-id-fn        :uid}
               k8s-statefulsets           {::coe/!data            (subscribe [::subs/k8s-statefulsets])
                                           ::coe/!columns         (r/atom [field-uid
                                                                           field-name
                                                                           field-creation-timestamp
                                                                           field-resource-version
                                                                           field-namespace
                                                                           field-raw])
                                           ::coe/!default-columns (r/atom [:name :creation_timestamp :namespace :raw])
                                           ::coe/resource-type    "statefulset"
                                           ::coe/row-id-fn        :uid}
               k8s-persistentvolumes      {::coe/!data            (subscribe [::subs/k8s-persistentvolumes])
                                           ::coe/!columns         (r/atom [field-uid
                                                                           field-name
                                                                           field-creation-timestamp
                                                                           field-resource-version
                                                                           field-namespace
                                                                           field-raw])
                                           ::coe/!default-columns (r/atom [:name :creation_timestamp :namespace :raw])
                                           ::coe/resource-type    "persistentvolume"
                                           ::coe/row-id-fn        :uid}
               k8s-persistentvolumeclaims {::coe/!data            (subscribe [::subs/k8s-persistentvolumeclaims])
                                           ::coe/!columns         (r/atom [field-uid
                                                                           field-name
                                                                           field-creation-timestamp
                                                                           field-resource-version
                                                                           field-namespace
                                                                           field-raw])
                                           ::coe/!default-columns (r/atom [:name :creation_timestamp :namespace :raw])
                                           ::coe/resource-type    "persistentvolumeclaim"
                                           ::coe/row-id-fn        :uid}
               k8s-daemonsets             {::coe/!data            (subscribe [::subs/k8s-daemonsets])
                                           ::coe/!columns         (r/atom [field-uid
                                                                           field-name
                                                                           field-creation-timestamp
                                                                           field-resource-version
                                                                           field-namespace
                                                                           field-raw])
                                           ::coe/!default-columns (r/atom [:name :creation_timestamp :namespace :raw])
                                           ::coe/resource-type    "daemonset"
                                           ::coe/row-id-fn        :uid}
               k8s-deployments            {::coe/!data            (subscribe [::subs/k8s-deployments])
                                           ::coe/!columns         (r/atom [field-uid
                                                                           field-name
                                                                           field-creation-timestamp
                                                                           field-resource-version
                                                                           field-namespace
                                                                           field-raw])
                                           ::coe/!default-columns (r/atom [:name :creation_timestamp :namespace :raw])
                                           ::coe/resource-type    "deployment"
                                           ::coe/row-id-fn        :uid}
               k8s-jobs                   {::coe/!data            (subscribe [::subs/k8s-jobs])
                                           ::coe/!columns         (r/atom [field-uid
                                                                           field-name
                                                                           field-creation-timestamp
                                                                           field-resource-version
                                                                           field-namespace
                                                                           field-raw])
                                           ::coe/!default-columns (r/atom [:name :creation_timestamp :namespace :raw])
                                           ::coe/resource-type    "job"
                                           ::coe/row-id-fn        :uid}
               k8s-ingresses              {::coe/!data            (subscribe [::subs/k8s-ingresses])
                                           ::coe/!columns         (r/atom [field-uid
                                                                           field-name
                                                                           field-creation-timestamp
                                                                           field-resource-version
                                                                           field-namespace
                                                                           field-raw])
                                           ::coe/!default-columns (r/atom [:name :creation_timestamp :namespace :raw])
                                           ::coe/resource-type    "ingresse"
                                           ::coe/row-id-fn        :uid}
               k8s-cronjobs               {::coe/!data            (subscribe [::subs/k8s-cronjobs])
                                           ::coe/!columns         (r/atom [field-uid
                                                                           field-name
                                                                           field-creation-timestamp
                                                                           field-resource-version
                                                                           field-namespace
                                                                           field-raw])
                                           ::coe/!default-columns (r/atom [:name :creation_timestamp :namespace :raw])
                                           ::coe/resource-type    "cronjob"
                                           ::coe/row-id-fn        :uid}
               k8s-services               {::coe/!data            (subscribe [::subs/k8s-services])
                                           ::coe/!columns         (r/atom [field-uid
                                                                           field-name
                                                                           field-creation-timestamp
                                                                           field-resource-version
                                                                           field-namespace
                                                                           field-raw])
                                           ::coe/!default-columns (r/atom [:name :creation_timestamp :namespace :raw])
                                           ::coe/resource-type    "service"
                                           ::coe/row-id-fn        :uid}
               k8s-helmreleases           {::coe/!data            (subscribe [::subs/k8s-helmreleases])
                                           ::coe/!columns         (r/atom [field-name
                                                                           {::table/field-key      :chart
                                                                            ::table/header-content "Chart"}
                                                                           {::table/field-key      :app_version
                                                                            ::table/header-content "App version"}
                                                                           {::table/field-key      :revision
                                                                            ::table/header-content "Revision"}
                                                                           {::table/field-key      :status
                                                                            ::table/header-content "Status"}
                                                                           {::table/field-key      :updated
                                                                            ::table/header-content "Updated"
                                                                            ::table/field-cell     CellTimeAgoExtraFormat}
                                                                           field-namespace])
                                           ::coe/!default-columns (r/atom [:name :chart :app_version :status :updated :namespace])
                                           ::coe/resource-type    "helm"
                                           ::coe/row-id-fn        :name}]
    [coe/Tab
     [["Namespaces" ::k8s-namespaces k8s-namespaces]
      ["Images" ::k8s-images k8s-images]
      ["Nodes" ::k8s-nodes k8s-nodes]
      ["Pods" ::k8s-pods k8s-pods]
      ["Secrets" ::k8s-secrets k8s-secrets]
      ["Config maps" ::k8s-configmaps k8s-configmaps]
      ["Deployments" ::k8s-deployments k8s-deployments]
      ["Stateful sets" ::k8s-statefulsets k8s-statefulsets]
      ["Daemon sets" ::k8s-daemonsets k8s-daemonsets]
      ["Services" ::k8s-services k8s-services]
      ["Jobs" ::k8s-jobs k8s-jobs]
      ["Cronjobs" ::k8s-cronjobs k8s-cronjobs]
      ["Persistent volumes" ::k8s-persistentvolumes k8s-persistentvolumes]
      ["Persistent volume claims" ::k8s-persistentvolumeclaims k8s-persistentvolumeclaims]
      ["Ingresses" ::k8s-ingresses k8s-ingresses]
      ["Helm releases" ::k8s-helmreleases k8s-helmreleases]]]))
