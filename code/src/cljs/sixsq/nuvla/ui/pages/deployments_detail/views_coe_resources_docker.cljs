(ns sixsq.nuvla.ui.pages.deployments-detail.views-coe-resources-docker
  (:require [re-frame.core :refer [dispatch subscribe reg-event-fx reg-sub]]
            [sixsq.nuvla.ui.common-components.plugins.table-refactor :as table]
            [sixsq.nuvla.ui.pages.deployments-detail.views-coe-resources :as coe]
            [reagent.core :as r]
            [sixsq.nuvla.ui.pages.deployments-detail.subs :as subs]))

(def field-id {::table/field-key      :Id
               ::table/header-content "Id"
               ::table/field-cell     (table/CellOverflowTooltipAs :div.max-width-12ch.ellipsing)})
(def field-created {::table/field-key      :Created
                    ::table/header-content "Created"
                    ::table/field-cell     table/CellTimeAgo})
(def field-created-iso {::table/field-key      :Created
                        ::table/header-content "Created"
                        ::table/field-cell     table/CellTimeAgo})
(def field-created-at {::table/field-key      :CreatedAt
                       ::table/header-content "Created"
                       ::table/field-cell     table/CellTimeAgo})
(def field-labels {::table/field-key      :Labels
                   ::table/header-content "Labels"
                   ::table/no-sort?       true
                   ::table/field-cell     coe/CellKeyValueLabelGroup})
(def field-name {::table/field-key      :Name
                 ::table/header-content "Name"})
(def field-driver {::table/field-key      :Driver
                   ::table/header-content "Driver"})
(def field-raw {::table/field-key      :raw
                ::table/header-content "Raw"
                ::table/no-sort?       true
                ::table/field-cell     coe/CellJson})


(defn Tab []
  (r/with-let [docker-images     {::coe/!data            (subscribe [::subs/docker-images])
                                  ::coe/!columns         (r/atom [field-id
                                                                  {::table/field-key      :ParentId
                                                                   ::table/header-content "Parent Id"
                                                                   ::table/no-sort?       true}
                                                                  {::table/field-key      :RepoDigests
                                                                   ::table/header-content "Repo Digests"
                                                                   ::table/field-cell     coe/CellLabelGroup}
                                                                  {::table/field-key      :Size
                                                                   ::table/header-content "Size"
                                                                   ::table/field-cell     table/CellBytes}
                                                                  field-created
                                                                  {::table/field-key      :RepoTags
                                                                   ::table/header-content "Repo/Tags"
                                                                   ::table/field-cell     coe/CellLabelGroup}
                                                                  field-labels
                                                                  {::table/field-key      :Repository
                                                                   ::table/header-content "Repository"}
                                                                  {::table/field-key      :Tag
                                                                   ::table/header-content "Tag"}
                                                                  field-raw])
                                  ::coe/!default-columns (r/atom [:Id :Size :Created :RepoTags])
                                  ::coe/resource-type    "image"}
               docker-containers {::coe/!data            (subscribe [::subs/docker-containers])
                                  ::coe/!columns         (r/atom [field-id
                                                                  {::table/field-key      :Image
                                                                   ::table/header-content "Image"}
                                                                  field-created
                                                                  {::table/field-key      :Status
                                                                   ::table/header-content "Status"}
                                                                  {::table/field-key      :SizeRootFs
                                                                   ::table/header-content "Size RootFs"
                                                                   ::table/field-cell     table/CellBytes}
                                                                  field-labels
                                                                  {::table/field-key      :HostConfig
                                                                   ::table/header-content "Host config"
                                                                   ::table/no-sort?       true
                                                                   ::table/field-cell     coe/CellKeyValueLabelGroup}
                                                                  {::table/field-key      :Names
                                                                   ::table/header-content "Names"
                                                                   ::table/field-cell     coe/CellLabelGroup}
                                                                  {::table/field-key      :SizeRw
                                                                   ::table/header-content "Size RW"
                                                                   ::table/field-cell     table/CellBytes}
                                                                  {::table/field-key      :ImageID
                                                                   ::table/header-content "Image Id"}
                                                                  {::table/field-key      :Mounts
                                                                   ::table/header-content "Mounts"
                                                                   ::table/field-cell     coe/CellLabelGroup
                                                                   ::table/no-sort?       true}
                                                                  {::table/field-key      :Name
                                                                   ::table/header-content "Name"}
                                                                  {::table/field-key      :NetworkSettings
                                                                   ::table/header-content "Networks"
                                                                   ::table/field-cell     coe/CellLabelGroup
                                                                   ::table/no-sort?       true}
                                                                  {::table/field-key      :State
                                                                   ::table/header-content "State"}
                                                                  {::table/field-key      :Command
                                                                   ::table/header-content "Command"}
                                                                  {::table/field-key      :Ports
                                                                   ::table/header-content "Ports"
                                                                   ::table/no-sort?       true
                                                                   ::table/field-cell     coe/CellLabelGroup}
                                                                  field-raw])
                                  ::coe/!default-columns (r/atom [:Id :Name :Image :Status :Created :Ports])
                                  ::coe/resource-type    "container"}
               docker-volumes    {::coe/!data            (subscribe [::subs/docker-volumes])
                                  ::coe/!columns         (r/atom [{::table/field-key      :Name
                                                                   ::table/header-content "Name"
                                                                   ::table/field-cell     (table/CellOverflowTooltipAs :div.max-width-50ch.ellipsing)}
                                                                  field-driver
                                                                  {::table/field-key      :Scope
                                                                   ::table/header-content "Scope"}
                                                                  {::table/field-key      :Mountpoint
                                                                   ::table/header-content "Mount point"}
                                                                  field-created-at
                                                                  field-labels
                                                                  {::table/field-key      :Options
                                                                   ::table/header-content "Options"}
                                                                  field-raw])
                                  ::coe/row-id-fn        :Name
                                  ::coe/!default-columns (r/atom [:Name :CreatedAt :Driver])
                                  ::coe/resource-type    "volume"}
               docker-networks   {::coe/!data            (subscribe [::subs/docker-networks])
                                  ::coe/!columns         (r/atom [field-id
                                                                  field-name
                                                                  field-created-iso
                                                                  field-driver
                                                                  {::table/field-key      :Options
                                                                   ::table/header-content "Options"
                                                                   ::table/field-cell     coe/CellKeyValueLabelGroup}
                                                                  field-labels
                                                                  {::table/field-key      :ConfigFrom
                                                                   ::table/header-content "ConfigFrom"}
                                                                  {::table/field-key      :ConfigOnly
                                                                   ::table/header-content "ConfigOnly"}
                                                                  {::table/field-key      :IPAM
                                                                   ::table/header-content "IPAM"
                                                                   ::table/field-cell     coe/CellKeyValueLabelGroup}
                                                                  {::table/field-key      :Attachable
                                                                   ::table/header-content "Attachable"
                                                                   ::table/field-cell     coe/CellBoolean
                                                                   ::table/div-class      ["slideways-lr"]}
                                                                  {::table/field-key      :EnableIPv6
                                                                   ::table/header-content "IPv6"
                                                                   ::table/field-cell     coe/CellBoolean
                                                                   ::table/div-class      ["slideways-lr"]}
                                                                  {::table/field-key      :Ingress
                                                                   ::table/header-content "Ingress"
                                                                   ::table/field-cell     coe/CellBoolean
                                                                   ::table/div-class      ["slideways-lr"]}
                                                                  {::table/field-key      :Internal
                                                                   ::table/header-content "Internal"
                                                                   ::table/field-cell     coe/CellBoolean
                                                                   ::table/div-class      ["slideways-lr"]}
                                                                  {::table/field-key      :Scope
                                                                   ::table/header-content "Scope"}
                                                                  field-raw])
                                  ::coe/!default-columns (r/atom [:Id :Name :Created :Driver :Scope :Attachable :Internal :Ingress :EnableIPv6 :IPAM])
                                  ::coe/resource-type    "network"}]
    [coe/Tab
     [["Containers" ::docker-containers docker-containers]
      ["Images" ::docker-images docker-images]
      ["Volumes" ::docker-volumes docker-volumes]
      ["Networks" ::docker-networks docker-networks]]]))
