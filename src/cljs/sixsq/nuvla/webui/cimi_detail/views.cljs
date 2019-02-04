(ns sixsq.nuvla.webui.cimi-detail.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.webui.cimi-detail.events :as cimi-detail-events]
    [sixsq.nuvla.webui.cimi-detail.subs :as cimi-detail-subs]
    [sixsq.nuvla.webui.cimi.subs :as cimi-subs]
    [sixsq.nuvla.webui.docs.subs :as docs-subs]
    [sixsq.nuvla.webui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.webui.main.subs :as main-subs]
    [sixsq.nuvla.webui.utils.resource-details :as details]
    [sixsq.nuvla.webui.utils.semantic-ui :as ui]
    [sixsq.nuvla.webui.utils.semantic-ui-extensions :as uix]
    [taoensso.timbre :as log]))


(defn refresh-button
  []
  (let [tr (subscribe [::i18n-subs/tr])
        loading? (subscribe [::cimi-detail-subs/loading?])
        resource-id (subscribe [::cimi-detail-subs/resource-id])]
    (fn []
      [ui/MenuMenu {:position "right"}
       [uix/MenuItemWithIcon
        {:name      (@tr [:refresh])
         :icon-name "refresh"
         :loading?  @loading?
         :on-click  #(dispatch [::cimi-detail-events/get @resource-id])}]])))


(defn path->resource-id
  [path]
  (str/join "/" (rest path)))


(defn cimi-detail
  []
  (let [cep (subscribe [::cimi-subs/cloud-entry-point])
        path (subscribe [::main-subs/nav-path])
        loading? (subscribe [::cimi-detail-subs/loading?])
        cached-resource-id (subscribe [::cimi-detail-subs/resource-id])
        resource (subscribe [::cimi-detail-subs/resource])]
    (fn []
      (let [resource-id (path->resource-id @path)
            correct-resource? (= resource-id @cached-resource-id)
            resourceMetadata (subscribe [::docs-subs/document @resource])]

        ;; forces a refresh when the correct resource isn't cached
        (when-not correct-resource?
          (dispatch [::cimi-detail-events/get (path->resource-id @path)]))

        ;; render the (possibly empty) detail
        [details/resource-detail
         [refresh-button]
         (when (and (not @loading?) correct-resource?) @resource)
         (:baseURI @cep)
         @resourceMetadata]))))
