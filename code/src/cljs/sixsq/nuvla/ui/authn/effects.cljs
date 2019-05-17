(ns sixsq.nuvla.ui.authn.effects
  (:require [re-frame.core :refer [dispatch reg-fx]]
            [sixsq.nuvla.ui.utils.time :as time]))


(def timeout-id (atom nil))


(reg-fx
  ::automatic-logout-at-session-expiry
  (fn [[{:keys [expiry] :as session}]]
    (let [logout-callback       (fn []
                                  (dispatch [:sixsq.nuvla.ui.authn.events/logout])
                                  (dispatch [:sixsq.nuvla.ui.authn.events/open-modal :login]))
          remaining-time-millis (->> expiry (time/delta-milliseconds (time/now)) int)]
      (if (pos-int? remaining-time-millis)
        (do (js/clearTimeout @timeout-id)
            (reset! timeout-id (js/setTimeout logout-callback remaining-time-millis))
            (dispatch [:sixsq.nuvla.ui.authn.events/close-modal-no-session]))
        (logout-callback)))))
