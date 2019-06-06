(ns sixsq.nuvla.ui.cimi-api.effects
  "Provides effects that use the CIMI client to interact asynchronously with
   the server."
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-fx]]
    [sixsq.nuvla.client.api :as api]
    [sixsq.nuvla.client.async :as async-client]
    [sixsq.nuvla.client.authn :as authn]
    [sixsq.nuvla.ui.history.utils :as utils]
    [sixsq.nuvla.ui.utils.defines :as defines]
    [sixsq.nuvla.ui.utils.general :as general-utils]))


(def NUVLA_URL (delay (if (str/blank? defines/HOST_URL) (utils/host-url) defines/HOST_URL)))


(def CLIENT (delay (async-client/instance (str @NUVLA_URL "/api/cloud-entry-point"))))


(defn get-current-session
  []
  (go
    (let [session-collection (<! (api/search @CLIENT :session))]
      (when-not (instance? js/Error session-collection)
        (-> session-collection :resources first)))))


(reg-fx
  ::cloud-entry-point
  (fn [[callback]]
    (go
      (callback (<! (api/cloud-entry-point @CLIENT))))))


(reg-fx
  ::get
  (fn [[resource-id callback]]
    (when resource-id
      (go
        (callback (<! (api/get @CLIENT resource-id)))))))


(reg-fx
  ::search
  (fn [[resource-type params callback]]
    (when resource-type
      (go
        (callback (<! (api/search @CLIENT resource-type (general-utils/prepare-params params))))))))


(reg-fx
  ::delete
  (fn [[resource-id callback]]
    (when resource-id
      (go
        ;; FIXME: Using 2-arg form doesn't work with advanced optimization. Why?
        (callback (<! (api/delete @CLIENT resource-id {})))))))


(reg-fx
  ::edit
  (fn [[resource-id data callback]]
    (when resource-id
      (go
        (let [response (<! (api/edit @CLIENT resource-id data))]
          (if (instance? js/Error response)
            (callback response)
            ;; This is done to get a fully updated resource.  If the return
            ;; value of edit is used, then, for example, the operations are
            ;; not updated.
            (callback (<! (api/get @CLIENT resource-id)))))))))

(reg-fx
  ::add
  (fn [[resource-type data callback]]
    (when resource-type
      (go
        (callback (<! (api/add @CLIENT resource-type data)))))))


(reg-fx
  ::operation
  (fn [[resource-id operation callback data]]
    (when resource-id
      (go
        (callback (<! (api/operation @CLIENT resource-id operation data)))))))


(reg-fx
  ::logout
  (fn [[callback]]
    (go
      (callback (<! (authn/logout @CLIENT))))))


(reg-fx
  ::login
  (fn [[creds callback]]
    (go
      (let [resp    (<! (authn/login @CLIENT creds))
            session (<! (get-current-session))]
        (callback resp session)))))


(reg-fx
  ::session
  (fn [[callback]]
    (go
      (callback (<! (get-current-session))))))
