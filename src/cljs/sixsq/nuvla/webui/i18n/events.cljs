(ns sixsq.nuvla.webui.i18n.events
  (:require
    [re-frame.core :refer [reg-event-db]]
    [sixsq.nuvla.webui.i18n.utils :as utils]))


(reg-event-db
  ::set-locale
  (fn [db [_ locale]]
    (-> db
        (assoc :sixsq.nuvla.webui.i18n.spec/locale locale)
        (assoc :sixsq.nuvla.webui.i18n.spec/tr (utils/create-tr-fn locale)))))
