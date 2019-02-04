(ns sixsq.slipstream.webui.cimi-api.effects
  "Provides effects that use the CIMI client to interact asynchronously with
   the server."
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [re-frame.core :refer [dispatch reg-fx]]
    [sixsq.slipstream.client.api.authn :as authn]
    [sixsq.slipstream.client.api.cimi :as cimi]
    [sixsq.slipstream.client.api.metrics :as metrics]
    [sixsq.slipstream.webui.cimi-api.utils :as cimi-api-utils]))


(reg-fx
  ::cloud-entry-point
  (fn [[client callback]]
    (go
      (callback (<! (cimi/cloud-entry-point client))))))


(reg-fx
  ::get
  (fn [[client resource-id callback]]
    (go
      (callback (<! (cimi/get client resource-id))))))


(reg-fx
  ::search
  (fn [[client resource-type params callback]]
    (go
      (callback (<! (cimi/search client resource-type (cimi-api-utils/sanitize-params params)))))))


(reg-fx
  ::delete
  (fn [[client resource-id callback]]
    (go
      ;; FIXME: Using 2-arg form doesn't work with advanced optimization. Why?
      (callback (<! (cimi/delete client resource-id {}))))))


(reg-fx
  ::edit
  (fn [[client resource-id data callback]]
    (go
      (<! (cimi/edit client resource-id data))

      ;; This is done to get a fully updated resource.  If the return
      ;; value of edit is used, then, for example, the operations are
      ;; not updated.
      (callback (<! (cimi/get client resource-id))))))

(reg-fx
  ::add
  (fn [[client resource-type data callback]]
    (go
      (callback (<! (cimi/add client resource-type data))))))


(reg-fx
  ::operation
  (fn [[client resource-id operation callback]]
    (go
      (callback (<! (cimi/operation client resource-id operation))))))


(reg-fx
  ::logout
  (fn [[client callback]]
    (go
      (callback (<! (authn/logout client))))))


(reg-fx
  ::login
  (fn [[client creds callback]]
    (go
      (let [resp (<! (authn/login client creds))
            session (<! (cimi-api-utils/get-current-session client))]
        (callback resp session)))))


(reg-fx
  ::session
  (fn [[client callback]]
    (go
      (callback (<! (cimi-api-utils/get-current-session client))))))


(reg-fx
  ::current-user-params
  (fn [[client username callback]]
    (go
      (when (and client username)
        (let [filter (str "acl/owner/principal='" username "'")]
          (callback (-> (<! (cimi/search client
                                         :userParam
                                         (cimi-api-utils/sanitize-params {:$filter filter})))
                        :userParam
                        first)))))))


(reg-fx
  ::metrics
  (fn [[client callback]]
    (go
      (callback (<! (metrics/get-metrics client {}))))))


