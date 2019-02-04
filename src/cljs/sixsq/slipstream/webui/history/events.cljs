(ns sixsq.slipstream.webui.history.events
  (:require
    [re-frame.core :refer [reg-event-fx trim-v]]
    [sixsq.slipstream.webui.history.effects :as fx]
    [taoensso.timbre :as log]))


(reg-event-fx
  ::initialize
  (fn [_ [_ path-prefix]]
    (log/info "setting history path-prefix to " path-prefix)
    {::fx/initialize [path-prefix]}))


(reg-event-fx
  ::navigate
  (fn [_ [_ relative-url]]
    (log/info "triggering navigate effect " relative-url)
    {::fx/navigate [relative-url]}))


(reg-event-fx
  ::navigate-js-location
  (fn [_ [_ url]]
    (log/info "triggering navigate js location effect " url)
    {::fx/navigate-js-location [url]}))