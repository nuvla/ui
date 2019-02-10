(ns sixsq.nuvla.ui.authn.effects
  (:require [re-frame.core :refer [dispatch reg-fx]]
            [sixsq.nuvla.ui.utils.time :as time]))

(reg-fx
  ::automatic-logout-at-session-expiry
  (fn [[{:keys [expiry] :as session}]]
    (let [remaining-milliseconds (->> expiry (time/delta-milliseconds (time/now)) int)]
      (js/setTimeout #(do
                        (dispatch [:sixsq.nuvla.ui.authn.events/logout])
                        (dispatch [:sixsq.nuvla.ui.authn.events/open-modal :login])) remaining-milliseconds))))
