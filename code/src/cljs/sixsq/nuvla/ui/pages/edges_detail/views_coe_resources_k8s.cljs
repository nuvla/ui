(ns sixsq.nuvla.ui.pages.edges-detail.views-coe-resources-k8s
  (:require [re-frame.core :refer [dispatch subscribe reg-event-fx reg-sub]]
            [sixsq.nuvla.ui.common-components.plugins.table-refactor :as table]
            [sixsq.nuvla.ui.pages.edges-detail.views-coe-resources :as coe]
            [reagent.core :as r]
            [sixsq.nuvla.ui.pages.edges-detail.subs :as subs]))

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

(defn Tab []
  (r/with-let [k8s-images     {::coe/!data            (subscribe [::subs/k8s-images])
                               ::coe/!columns         (r/atom [{::table/field-key      :names
                                                                ::table/header-content "Names"
                                                                ::table/field-cell     coe/LabelGroup}
                                                               {::table/field-key      :size_bytes
                                                                ::table/header-content "Size"
                                                                ::table/field-cell     table/CellBytes}])
                               ::coe/!default-columns (r/atom [:names :size_bytes])
                               ::coe/resource-type    "image"
                               ::coe/row-id-fn        :names}
               k8s-namespaces {::coe/!data            (subscribe [::subs/k8s-namespaces-clean])
                               ::coe/!columns         (r/atom [field-uid
                                                               field-name
                                                               field-creation-timestamp
                                                               field-resource-version])
                               ::coe/!default-columns (r/atom [:name :creation_timestamp :resource_version])
                               ::coe/resource-type    "namespace"
                               ::coe/row-id-fn        :uid}
               k8s-pods       {::coe/!data            (subscribe [::subs/k8s-pods-clean])
                               ::coe/!columns         (r/atom [field-uid
                                                               field-name
                                                               field-creation-timestamp
                                                               field-resource-version
                                                               field-namespace
                                                               {::table/field-key      :phase
                                                                ::table/header-content "Status"}])
                               ::coe/!default-columns (r/atom [:name :creation_timestamp :resource_version :namespace :phase])
                               ::coe/resource-type    "pod"
                               ::coe/row-id-fn        :uid}
               k8s-nodes      {::coe/!data            (subscribe [::subs/k8s-nodes-clean])
                               ::coe/!columns         (r/atom [field-uid
                                                               field-name
                                                               field-creation-timestamp
                                                               field-resource-version
                                                               {::table/field-key      :node_info
                                                                ::table/header-content "NodeInfo"
                                                                ::table/field-cell     coe/KeyValueLabelGroup}])
                               ::coe/!default-columns (r/atom [:name :creation_timestamp :resource_version :node_info])
                               ::coe/resource-type    "node"
                               ::coe/row-id-fn        :uid}
               k8s-configmaps {::coe/!data            (subscribe [::subs/k8s-configmaps-clean])
                               ::coe/!columns         (r/atom [field-uid
                                                               field-name
                                                               field-creation-timestamp
                                                               field-resource-version
                                                               field-namespace])
                               ::coe/!default-columns (r/atom [:name :creation_timestamp :resource_version :namespace])
                               ::coe/resource-type    "configmap"
                               ::coe/row-id-fn        :uid}
               k8s-secrets    {::coe/!data            (subscribe [::subs/k8s-secrets-clean])
                               ::coe/!columns         (r/atom [field-uid
                                                               field-name
                                                               field-creation-timestamp
                                                               field-resource-version
                                                               field-namespace
                                                               {::table/field-key      :type
                                                                ::table/header-content "Type"}])
                               ::coe/!default-columns (r/atom [:name :creation_timestamp :resource_version :namespace :type])
                               ::coe/resource-type    "secret"
                               ::coe/row-id-fn        :uid}]
    [coe/Tab
     [["Secrets" ::k8s-secrets]
      ["Config maps" ::k8s-configmaps]
      ["Nodes" ::k8s-nodes]
      ["Pods" ::k8s-pods]
      ["Namespaces" ::k8s-namespaces]
      ["Images" ::k8s-images]]
     {::k8s-images     k8s-images
      ::k8s-namespaces k8s-namespaces
      ::k8s-pods       k8s-pods
      ::k8s-nodes      k8s-nodes
      ::k8s-configmaps k8s-configmaps
      ::k8s-secrets    k8s-secrets}]))
