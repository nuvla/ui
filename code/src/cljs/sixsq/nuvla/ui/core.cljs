(ns sixsq.nuvla.ui.core
  (:require [cljs.spec.test.alpha :as ts]
            [clojure.string :as str]
            [form-validator.core :as fv]
            [goog.dom :as gdom]
            [re-frame.core :refer [clear-subscription-cache! dispatch dispatch-sync]]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [sixsq.nuvla.ui.app.view :refer [App]]
            [sixsq.nuvla.ui.common-components.i18n.events :as i18n-events]
            [sixsq.nuvla.ui.common-components.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.config :as config]
            [sixsq.nuvla.ui.db.events :as db-events]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.pages.about.events :as about-events]
            [sixsq.nuvla.ui.pages.cimi.events :as api-events]
            [sixsq.nuvla.ui.routing.router :refer [init-routes!]]
            [sixsq.nuvla.ui.session.events :as session-events]
            [taoensso.timbre :as log]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (ts/instrument)
    (log/info "development mode")))

(defn visibility-watcher []
  (let [callback #(dispatch [::main-events/visible (not (.-hidden js/document))])]
    (.addEventListener js/document "visibilitychange" callback)))

(defn screen-size-watcher []
  (let [callback #(dispatch [::main-events/set-device])]
    (callback)
    (.addEventListener js/window "resize" callback)))

(defn patch-kvlt
  "Patch kvlt is encoding IPv6 address in server name. This encoding is making Firefox browser failing all api requests.
  To reproduce the issue in browser console:
  new goog.Uri().setScheme('https').setDomain('[2a02:1210:5a0a:4200:5054:ff:fe7d:c137]').setPath('/api/cloud-entry-point').toString();
  Resulting in \"https://%5B2a02%3A1210%3A5a0a%3A4200%3A5054%3Aff%3Afe7d%3Ac137%5D/api/cloud-entry-point"
  []
  (let [req->url kvlt.platform.xhr/req->url
        fix-req->url (fn [{:keys [scheme server-name] :as opts}]
                       (if (some-> server-name (str/starts-with? "["))
                         (let [placeholder "server-name"
                               separator "://"
                               scheme-str (name (or scheme :http))]
                           (str/replace (req->url (assoc opts :server-name placeholder))
                                        (re-pattern (str "^" scheme-str separator placeholder))
                                        (str scheme-str separator server-name)))
                         (req->url opts)))]
    (set! kvlt.platform.xhr/req->url fix-req->url)))

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

(defn mount-root []
  (clear-subscription-cache!)
  (rdom/render [App] (gdom/getElement "app")))

(defn ^:export init []
  (init-routes!)
  (patch-kvlt)
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
  (dispatch-sync [::about-events/init-feature-flags])
  (visibility-watcher)
  (screen-size-watcher)
  (swap! fv/conf #(merge % {:atom r/atom}))
  (mount-root)
  (log/info "finished initialization"))
