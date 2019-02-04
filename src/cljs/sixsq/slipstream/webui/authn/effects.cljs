(ns sixsq.slipstream.webui.authn.effects
  (:require [re-frame.core :refer [dispatch reg-fx]]
            [sixsq.slipstream.webui.utils.time :as time]))

(reg-fx
  ::automatic-logout-at-session-expiry
  (fn [[{:keys [expiry] :as session}]]
    (let [remaining-milliseconds (->> expiry (time/delta-milliseconds (time/now)) int)]
      (js/setTimeout #(do
                        (dispatch [:sixsq.slipstream.webui.authn.events/logout])
                        (dispatch [:sixsq.slipstream.webui.authn.events/open-modal :login])) remaining-milliseconds))))
