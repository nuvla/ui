(ns sixsq.nuvla.ui.cimi-detail.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.cimi-detail.events :as events]
    [sixsq.nuvla.ui.cimi-detail.subs :as subs]
    [sixsq.nuvla.ui.cimi.subs :as cimi-subs]
    [sixsq.nuvla.ui.docs.subs :as docs-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.utils.resource-details :as details]
    [taoensso.timbre :as log]
    [sixsq.nuvla.ui.acl.views :as acl-views]
    [sixsq.nuvla.ui.utils.general :as general-utils]))


(defn path->resource-id
  [path]
  (str/join "/" (rest path)))


(defn cimi-detail
  []
  (let [cep                (subscribe [::cimi-subs/cloud-entry-point])
        path               (subscribe [::main-subs/nav-path])
        loading?           (subscribe [::subs/loading?])
        cached-resource-id (subscribe [::subs/resource-id])
        resource           (subscribe [::subs/resource])]
    (fn []
      (let [resource-id       (path->resource-id @path)
            correct-resource? (= resource-id @cached-resource-id)
            {:keys [acl] :as resource-value} @resource
            resource-metadata (subscribe [::docs-subs/document resource-value])]

        ;; forces a refresh when the correct resource isn't cached
        (when-not correct-resource?
          (dispatch [::events/get (path->resource-id @path)]))

        ;; render the (possibly empty) detail
        [:<>
         (when acl
           [acl-views/AclButton {:default-value acl
                                 :read-only     (not (general-utils/can-edit? resource-value))
                                 :on-change     #(dispatch [::events/edit
                                                            resource-id
                                                            (assoc resource-value :acl %)])}])
         [details/resource-detail
          [main-components/RefreshMenu
           {:on-refresh #(dispatch [::events/get resource-id])
            :loading?   @loading?}]
          (when (and (not @loading?) correct-resource?) @resource)
          (:base-uri @cep)
          @resource-metadata]]))))
