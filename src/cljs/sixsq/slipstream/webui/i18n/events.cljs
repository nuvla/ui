(ns sixsq.slipstream.webui.i18n.events
  (:require
    [re-frame.core :refer [reg-event-db]]
    [sixsq.slipstream.webui.i18n.utils :as utils]))


(reg-event-db
  ::set-locale
  (fn [db [_ locale]]
    (-> db
        (assoc :sixsq.slipstream.webui.i18n.spec/locale locale)
        (assoc :sixsq.slipstream.webui.i18n.spec/tr (utils/create-tr-fn locale)))))
