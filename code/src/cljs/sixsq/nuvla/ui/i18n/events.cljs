(ns sixsq.nuvla.ui.i18n.events
  (:require
    [re-frame.core :refer [reg-event-db]]
    [sixsq.nuvla.ui.i18n.utils :as utils]))


(reg-event-db
  ::set-locale
  (fn [db [_ locale]]
    (-> db
        (assoc :sixsq.nuvla.ui.i18n.spec/locale locale)
        (assoc :sixsq.nuvla.ui.i18n.spec/tr (utils/create-tr-fn locale)))))
