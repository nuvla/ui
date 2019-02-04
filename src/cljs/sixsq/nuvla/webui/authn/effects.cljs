(ns sixsq.nuvla.webui.authn.effects
  (:require [re-frame.core :refer [dispatch reg-fx]]
            [sixsq.nuvla.webui.utils.time :as time]))

(reg-fx
  ::automatic-logout-at-session-expiry
  (fn [[{:keys [expiry] :as session}]]
    (let [remaining-milliseconds (->> expiry (time/delta-milliseconds (time/now)) int)]
      (js/setTimeout #(do
                        (dispatch [:sixsq.nuvla.webui.authn.events/logout])
                        (dispatch [:sixsq.nuvla.webui.authn.events/open-modal :login])) remaining-milliseconds))))
