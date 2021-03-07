(ns sixsq.nuvla.ui.i18n.events
  (:require
    [ajax.core :as ajax]
    [day8.re-frame.http-fx]
    [re-frame.core :refer [reg-event-db reg-event-fx]]
    [sixsq.nuvla.ui.i18n.utils :as utils]
    [sixsq.nuvla.ui.i18n.spec :as spec]
    [sixsq.nuvla.ui.main.spec :as main-spec]
    [taoensso.timbre :as log]))


(reg-event-db
  ::set-locale
  (fn [db [_ locale]]
    (let [theme-dictionary (::spec/theme-dictionary db nil)]
      (-> db
          (assoc :sixsq.nuvla.ui.i18n.spec/locale locale)
          (assoc :sixsq.nuvla.ui.i18n.spec/tr (utils/create-tr-fn locale theme-dictionary))))))


(reg-event-fx
  ::get-theme-dictionary-good
  (fn [{db :db} [_ {:keys [stripe] :as result}]]
    (log/info "Theme dictionary loaded")
    (let [local (::spec/locale db)]
      {:db       (assoc db ::spec/theme-dictionary result)
       :dispatch [::set-locale local]})))


(reg-event-db
  ::get-theme-dictionary-bad
  (fn [db [_ response]]
    (log/error "Failed to load theme dictionary: " response)
    db))


(reg-event-fx
  ::get-theme-dictionary
  (fn [{{:keys [::main-spec/theme] :as db} :db} _]
    {:http-xhrio {:method          :get
                  :uri             (str theme "dictionary.json")
                  :timeout         8000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::get-theme-dictionary-good]
                  :on-failure      [::get-theme-dictionary-bad]}}))
