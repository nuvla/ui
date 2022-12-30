(ns sixsq.nuvla.ui.core
  (:require [cljs.spec.test.alpha :as ts]
            [form-validator.core :as fv]
            [re-frame.core :refer [clear-subscription-cache! dispatch
                                   dispatch-sync]]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [sixsq.nuvla.ui.cimi.events :as api-events]
            [sixsq.nuvla.ui.config :as config]
            [sixsq.nuvla.ui.db.events :as db-events]
            [sixsq.nuvla.ui.history.events :as history-events]
            [sixsq.nuvla.ui.i18n.events :as i18n-events]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.main.views :as main-views]
            [sixsq.nuvla.ui.routing.router :refer [init-routes!]]
            [sixsq.nuvla.ui.session.events :as session-events]
            [taoensso.timbre :as log]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (ts/instrument)
    (log/info "development mode")))


(defn render-component-when-present
  ([tag comp & {:keys [initialization-fn]}]
   (when-let [container-element (.getElementById js/document tag)]
     (log/info "Rendering " tag)
     (when initialization-fn (initialization-fn))
     (rdom/render [comp] container-element))))


(defn mount-root []
  (clear-subscription-cache!)
  (render-component-when-present "app" main-views/app))


(defn visibility-watcher []
  (let [callback #(dispatch [::main-events/visible (not (.-hidden js/document))])]
    (.addEventListener js/document "visibilitychange" callback)))


(defn screen-size-watcher []
  (let [callback #(dispatch [::main-events/set-device])]
    (callback)
    (.addEventListener js/window "resize" callback)))


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
  (dispatch-sync [::pagination-plugin/init-paginations])
  (dispatch-sync [::i18n-events/set-locale])
  (dispatch-sync [::api-events/get-cloud-entry-point])
  (dispatch-sync [::main-events/get-ui-config])
  (dispatch-sync [::main-events/check-ui-version-polling])
  (dispatch-sync [::session-events/initialize])
  (dispatch-sync [::main-events/check-iframe])
  (visibility-watcher)
  (screen-size-watcher)
  (dispatch [::history-events/initialize @config/path-prefix])
  (swap! fv/conf #(merge % {:atom r/atom}))
  (init-routes!)
  (mount-root)
  (log/info "finished initialization"))
