(ns sixsq.nuvla.ui.components.resource-filter-scenes
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [sixsq.nuvla.ui.pages.edges.utils :as edges-utils]
            [sixsq.nuvla.ui.portfolio-utils :refer [defscene]]
            [sixsq.nuvla.ui.common-components.plugins.resource-filter
             :refer [ResourceFilterController]]))

(def edges
  [{:id         "nuvlabox/0"
    :name       "nb-0"
    :state      "COMMISSIONED"
    :online     true
    :created-by "user a"
    :coe-list   [{:coe-type edges-utils/coe-type-docker}]
    :tags       ["tag 1" "tag 2"]}
   {:id         "nuvlabox/1"
    :name       "nb-1"
    :state      "DECOMMISSIONED"
    :online     false
    :created-by "user a"
    :coe-list   [{:coe-type edges-utils/coe-type-swarm}]}
   {:id         "nuvlabox/2"
    :name       "nb-2"
    :state      "COMMISSIONED"
    :online     true
    :created-by "user a"
    :coe-list   [{:coe-type edges-utils/coe-type-kubernetes}]}
   {:id         "nuvlabox/3"
    :name       "nb-3"
    :state      "COMMISSIONED"
    :online     false
    :created-by "user b"
    :coe-list   [{:coe-type edges-utils/coe-type-docker}
                 {:coe-type edges-utils/coe-type-kubernetes}]}
   {:id         "nuvlabox/4"
    :name       "nb-4"
    :state      "ACTIVATED"
    :created-by "user b"
    :coe-list   [{:coe-type edges-utils/coe-type-swarm}]}
   {:id         "nuvlabox/5"
    :name       "nb-5"
    :state      "ACTIVATED"
    :created-by "user a"
    :coe-list   [{:coe-type edges-utils/coe-type-docker}]
    :tags       ["tag 3"]}
   {:id         "nuvlabox/6"
    :name       "nb-6"
    :state      "COMMISSIONED"
    :online     false
    :created-by "user b"
    :coe-list   [{:coe-type edges-utils/coe-type-kubernetes}]}
   {:id         "nuvlabox/7"
    :name       "nb-7"
    :state      "COMMISSIONED"
    :online     false
    :created-by "user c"
    :coe-list   [{:coe-type edges-utils/coe-type-docker}]
    :tags       ["tag 4" "tag 5"]}])

(defn set-filters-fn
  [!edges !coe-types]
  (fn [{text-filter :text}]
    (let [filtered-edges (filterv (fn [{:keys [name description state created-by coe-list]}]
                                    (and (or (nil? text-filter)
                                             (some #(str/includes? % text-filter)
                                                   (remove nil? [name description state created-by])))
                                         (or (nil? !coe-types)
                                             (some #(contains? @!coe-types %)
                                                   (map :coe-type coe-list)))))
                                  edges)]
      (reset! !edges filtered-edges))))

(defn SelectedCount
  [!selected]
  [:div {:data-testid "edges-count"} "Number of edges selected: " (count @!selected)])

(def default-metadata-attributes
  {"acl/delete"                     {:indexed true, :description "unique identifier for a principal", :editable true, :name "acl/delete", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "item"}
   "acl/edit-acl"                   {:indexed true, :description "unique identifier for a principal", :editable true, :name "acl/edit-acl", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "item"}
   "acl/edit-data"                  {:indexed true, :description "unique identifier for a principal", :editable true, :name "acl/edit-data", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "item"}
   "acl/edit-meta"                  {:indexed true, :description "unique identifier for a principal", :editable true, :name "acl/edit-meta", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "item"}
   "acl/manage"                     {:indexed true, :description "unique identifier for a principal", :editable true, :name "acl/manage", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "item"}
   "acl/owners"                     {:indexed true, :description "unique identifier for a principal", :editable true, :name "acl/owners", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "item"}
   "acl/view-acl"                   {:indexed true, :description "unique identifier for a principal", :editable true, :name "acl/view-acl", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "item"}
   "acl/view-data"                  {:indexed true, :description "unique identifier for a principal", :editable true, :name "acl/view-data", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "item"}
   "acl/view-meta"                  {:indexed true, :description "unique identifier for a principal", :editable true, :name "acl/view-meta", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "item"}
   "capabilities"                   {:indexed true, :description "string containing something other than only whitespace", :editable true, :name "capabilities", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "item"}
   "cloud-password"                 {:indexed true, :description "password for cloud infrastructure", :editable true, :name "cloud-password", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "cloud password", :order 18}
   "coe-list/coe-type"              {:indexed true, :description "coe type", :editable true, :name "coe-list/coe-type", :section "data", :sensitive false, :server-managed false, :value-scope {:values ["docker" "swarm" "kubernetes"]}, :type "string", :hidden false, :display-name "coe-type"}
   "coe-list/id"                    {:indexed true, :description "unique resource identifier", :editable false, :name "coe-list/id", :fulltext true, :section "meta", :sensitive false, :server-managed true, :type "resource-id", :hidden false, :display-name "identifier", :order 0}
   "comment"                        {:indexed true, :description "comment about the NuvlaBox", :editable true, :name "comment", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "comment", :order 27}
   "created"                        {:indexed true, :description "creation timestamp (UTC) for resource", :editable false, :name "created", :section "meta", :sensitive false, :server-managed true, :type "date-time", :hidden false, :display-name "created", :order 4}
   "created-by"                     {:indexed true, :description "user id who created the resource", :editable false, :name "created-by", :section "meta", :sensitive false, :server-managed true, :type "string", :hidden false, :display-name "created-by", :order 11}
   "credential-api-key"             {:indexed true, :description "identifier of the associated credential api key resource", :editable true, :name "credential-api-key", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "NuvlaBox credential api key", :order 44}
   "description"                    {:indexed true, :description "human-readable description of resource", :editable true, :name "description", :fulltext true, :section "meta", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "description", :order 7}
   "firmware-version"               {:indexed true, :description "NuvlaBox software firmware version", :editable true, :name "firmware-version", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "firmware version", :order 25}
   "form-factor"                    {:indexed true, :description "hardware form factor", :editable true, :name "form-factor", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "form factor", :order 23}
   "hardware-type"                  {:indexed true, :description "hardware type of the NuvlaBox", :editable true, :name "hardware-type", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "hardware type", :order 26}
   "heartbeat-interval"             {:indexed true, :description "hearthbeat interval in seconds", :editable true, :name "heartbeat-interval", :section "data", :sensitive false, :server-managed false, :type "integer", :hidden false, :display-name "hearthbeat interval", :order 38}
   "host-level-management-api-key"  {:indexed true, :description "when host level management is enabled, it points to the credential api key being used for that purpose", :editable true, :name "host-level-management-api-key", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "NuvlaBox credential api key for host level management operations", :order 45}
   "hw-revision-code"               {:indexed true, :description "hardware revision code", :editable true, :name "hw-revision-code", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "hardware revision code", :order 29}
   "id"                             {:indexed true, :description "unique resource identifier", :editable false, :name "id", :fulltext true, :section "meta", :sensitive false, :server-managed true, :type "resource-id", :hidden false, :display-name "identifier", :order 0}
   "inferred-location"              {:indexed true, :description "location [longitude, latitude, altitude] - dynamically inferred by the NuvlaBox", :editable true, :name "inferred-location", :section "data", :sensitive false, :server-managed true, :type "geo-point", :hidden false, :display-name "inferred-location", :order 36}
   "infrastructure-service-group"   {:indexed true, :description "identifier of the associated infrastructure-service-group resource", :editable true, :name "infrastructure-service-group", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "NuvlaBox infrastructure service group", :order 43}
   "internal-data-gateway-endpoint" {:indexed true, :description "the endpoint users should connect to, from within the NuvlaBox, to subscribe to the data gateway", :editable true, :name "internal-data-gateway-endpoint", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "nuvlabox data gateway endpoint", :order 32}
   "lan-cidr"                       {:indexed true, :description "network range for local area network", :editable true, :name "lan-cidr", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "LAN CIDR", :order 12}
   "location"                       {:indexed true, :description "location [longitude, latitude, altitude] associated with the data", :editable true, :name "location", :section "data", :sensitive false, :server-managed false, :type "geo-point", :hidden false, :display-name "location", :order 20}
   "login-password"                 {:indexed true, :description "password to log into NuvlaBox", :editable true, :name "login-password", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "login password", :order 17}
   "login-username"                 {:indexed true, :description "username to log into NuvlaBox", :editable true, :name "login-username", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "login username", :order 16}
   "manufacturer-serial-number"     {:indexed true, :description "hardware manufacturer serial number", :editable true, :name "manufacturer-serial-number", :section "data", :sensitive false, :server-managed false, :type "string", :hidden false, :display-name "manufacturer serial number", :order 24}
   "monitored"                      {:indexed true, :description "flag to indicate whether machine should be monitored", :editable true, :name "monitored", :section "data", :sensitive false, :server-managed false, :type "boolean", :hidden false, :display-name "monitored", :order 30}})

(defscene basic
  (r/with-let [!filter-query                 (r/atom nil)
               !resource-metadata-attributes (r/atom default-metadata-attributes)]
    [:div {:style {:width 800}}
     [:div {:style {:margin 10}}
      [ResourceFilterController {:resource-name                    "dummy"
                                 :!filter-query                    !filter-query
                                 :!resource-metadata-attributes    !resource-metadata-attributes
                                 :show-clear-button-outside-modal? true}]
      [:div {:data-testid "filter-query"
             :style       {:margin 10}}
       "Filter query: " @!filter-query]]]))

(defscene modal
  (r/with-let [!filter-query                 (r/atom nil)
               !resource-metadata-attributes (r/atom default-metadata-attributes)]
    [:div {:style {:width 800}}
     [:div {:style {:margin 10}}
      [ResourceFilterController {:resource-name                    "dummy"
                                 :!filter-query                    !filter-query
                                 :!resource-metadata-attributes    !resource-metadata-attributes
                                 :show-clear-button-outside-modal? true
                                 :modal?                           true}]
      [:div {:data-testid "filter-query"
             :style       {:margin 10}}
       "Filter query: " @!filter-query]]]))
