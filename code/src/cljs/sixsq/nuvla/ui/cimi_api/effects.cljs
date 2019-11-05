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
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.defines :as defines]              ;; used namespace even if grey
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.response :as response]
    [taoensso.timbre :as log]))


(def NUVLA_URL (delay (if (str/blank? defines/HOST_URL) (utils/host-url) defines/HOST_URL)))


(def CLIENT (delay (async-client/instance (str @NUVLA_URL "/api/cloud-entry-point"))))


(defn get-current-session
  []
  (go
    (let [session-collection (<! (api/search @CLIENT :session))]
      (when-not (instance? js/Error session-collection)
        (-> session-collection :resources first)))))


(defn default-error-message
  [response error-msg]
  (dispatch [::messages-events/add
             (let [{:keys [status message]} (response/parse-ex-info response)]
               {:header  (cond-> error-msg
                                 status (str " (" status ")"))
                :content message
                :type    :error})]))

(defn default-add-on-error
  [resource-type response]
  (default-error-message response (str "failure adding " (name resource-type))))


(defn default-get-on-error
  [resource-id response]
  (default-error-message response (str "error getting " resource-id)))


(defn default-delete-on-error
  [resource-id response]
  (default-error-message response (str "error deleting " resource-id)))


(defn api-call-error-check
  [api-call on-success on-error]
  (go
    (let [response (<! (api-call))]
      (if (instance? js/Error response)
        (on-error response)
        (on-success response)))))


(reg-fx
  ::cloud-entry-point
  (fn [[callback]]
    (go
      (callback (<! (api/cloud-entry-point @CLIENT))))))


(reg-fx
  ::get
  (fn [[resource-id on-success & {:keys [on-error]}]]
    (when resource-id
      (let [api-call #(api/get @CLIENT resource-id)
            on-error (or on-error (partial default-get-on-error resource-id))]
        (api-call-error-check api-call on-success on-error)))))


(reg-fx
  ::search
  (fn [[resource-type params callback]]
    (when resource-type
      (go
        (callback (<! (api/search @CLIENT resource-type (general-utils/prepare-params params))))))))


(reg-fx
  ::delete
  (fn [[resource-id on-success & {:keys [on-error]}]]
    (when resource-id
      ;; FIXME: Using 2-arg form doesn't work with advanced optimization. Why?
      (let [api-call #(api/delete @CLIENT resource-id {})
            on-error (or on-error (partial default-delete-on-error resource-id))]
        (api-call-error-check api-call on-success on-error)))))


(reg-fx
  ::delete-bulk
  (fn [[resource-type on-success filter & {:keys [on-error]}]]
    (when resource-type
      (let [api-call #(api/delete-bulk @CLIENT resource-type {:filter filter})
            on-error (or on-error (partial default-delete-on-error resource-type))]
        (api-call-error-check api-call on-success on-error)))))


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
  (fn [[resource-type data on-success & {:keys [on-error]}]]
    (when resource-type
      (let [api-call #(api/add @CLIENT resource-type data)
            on-error (or on-error (partial default-add-on-error resource-type))]
        (api-call-error-check api-call on-success on-error)))))


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
