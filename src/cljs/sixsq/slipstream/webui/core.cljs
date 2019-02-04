(ns sixsq.slipstream.webui.core
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [clear-subscription-cache! dispatch dispatch-sync]]
    [reagent.core :as r]
    [sixsq.slipstream.webui.authn.events :as authn-events]
    [sixsq.slipstream.webui.authn.views :as authn-views]
    [sixsq.slipstream.webui.cimi.events :as api-events]
    [sixsq.slipstream.webui.client.events :as client-events]
    [sixsq.slipstream.webui.config :as config]
    [sixsq.slipstream.webui.dashboard.views :as dashboard-views]
    [sixsq.slipstream.webui.db.events :as db-events]
    [sixsq.slipstream.webui.deployment-detail.views :as deployment-detail-views]
    [sixsq.slipstream.webui.dnd.utils :as dnd-utils]
    [sixsq.slipstream.webui.history.events :as history-events]
    [sixsq.slipstream.webui.history.utils :as utils]
    [sixsq.slipstream.webui.main.events :as main-events]
    [sixsq.slipstream.webui.main.views :as main-views]
    [sixsq.slipstream.webui.routes :as routes]
    [sixsq.slipstream.webui.usage.views :as usage-views]
    [sixsq.slipstream.webui.utils.defines :as defines]
    [taoensso.timbre :as log]))

;;
;; determine the host url
;;
(def SLIPSTREAM_URL (delay (if-not (str/blank? defines/HOST_URL) defines/HOST_URL (utils/host-url))))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (log/info "development mode")))


(defn render-component-when-present
  ([tag comp & {:keys [initialization-fn]}]
   (when-let [container-element (.getElementById js/document tag)]
     (log/info "Rendering " tag)
     (when initialization-fn (initialization-fn))
     (r/render [comp] container-element))))


(defn mount-root []
  (clear-subscription-cache!)
  (render-component-when-present "app" main-views/app)
  (render-component-when-present "modal-login" authn-views/modal-login
                                 :initialization-fn #(do (dispatch-sync [::authn-events/server-redirect-uri "/login"])
                                                         (dispatch-sync [::authn-events/redirect-uri "/dashboard"])))
  (render-component-when-present "modal-signup" authn-views/modal-signup
                                 :initialization-fn #(do (dispatch-sync [::authn-events/server-redirect-uri "/login"])
                                                         (dispatch-sync [::authn-events/redirect-uri "/dashboard"])))
  (render-component-when-present "modal-reset-password" authn-views/modal-reset-password
                                 :initialization-fn #(do (dispatch-sync [::authn-events/server-redirect-uri "/login"])
                                                         (dispatch-sync [::authn-events/redirect-uri "/dashboard"])))
  (render-component-when-present "dashboard-tab" dashboard-views/vms-deployments)
  (render-component-when-present "usage" usage-views/usage)
  (render-component-when-present "deployment-detail-reports" deployment-detail-views/reports-list))


(defn visibility-watcher []
  (let [callback #(dispatch [::main-events/visible (not (.-hidden js/document))])]
    (.addEventListener js/document "visibilitychange" callback)))


(defn patch-process
  "patch for npm markdown module that calls into the process object for the
   current working directory"
  []
  (when-not (exists? js/process)
    (log/info "creating js/process global variable")
    (set! js/process (clj->js {})))

  (when-not (.-env js/process)
    (log/info "creating js/process.env map")
    (aset js/process "env" (clj->js {})))

  (when-not (.-cwd js/process)
    (log/info "creating js/process.cwd function")
    (aset js/process "cwd" (constantly "/"))))


(defn ^:export init []
  (patch-process)
  (dev-setup)
  (dispatch-sync [::db-events/initialize-db])
  (dispatch-sync [::client-events/initialize @SLIPSTREAM_URL])
  (dispatch-sync [::api-events/get-cloud-entry-point])
  (dispatch-sync [::authn-events/initialize])
  (dispatch-sync [::main-events/check-iframe])
  (visibility-watcher)
  (dnd-utils/disable-browser-dnd)
  (routes/routes)
  (dispatch [::history-events/initialize @config/path-prefix])
  (mount-root)
  (log/info "finished initialization"))
