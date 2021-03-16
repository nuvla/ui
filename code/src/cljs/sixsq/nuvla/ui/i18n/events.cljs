(ns sixsq.nuvla.ui.i18n.events
  (:require
    [ajax.core :as ajax]
    [com.degel.re-frame.storage :as storage]
    [day8.re-frame.http-fx]
    [re-frame.core :refer [reg-event-db reg-event-fx inject-cofx]]
    [sixsq.nuvla.ui.i18n.utils :as utils]
    [sixsq.nuvla.ui.i18n.spec :as spec]
    [sixsq.nuvla.ui.main.spec :as main-spec]
    [taoensso.timbre :as log]))


(reg-event-fx
  ::set-locale
  (fn [{db :db} [_ locale-new]]
    (let [theme-dictionary (::spec/theme-dictionary db)
          locale-db        (::spec/locale db)
          locale           (or locale-new locale-db)]
      {:db          (cond-> db
                            locale-new (assoc ::spec/locale locale-new)
                            true (assoc ::spec/tr (utils/create-tr-fn locale theme-dictionary)))
       :storage/set {:session? false
                     :name     :nuvla.ui.locale
                     :value    locale}})))


(reg-event-fx
  ::get-locale-from-local-storage
  [(inject-cofx :storage/get {:name :nuvla.ui.locale})]
  (fn [{db :db locale :storage/get}]
    (when-not (empty? locale)
      {:db (assoc db ::spec/locale locale)})))


(reg-event-fx
  ::get-theme-dictionary-good
  (fn [{{:keys [locale] :as db} :db} [_ result]]
    (log/info "Theme dictionary loaded: " locale)
    {:db       (assoc db ::spec/theme-dictionary result)
     :dispatch [::set-locale locale]}))


(reg-event-db
  ::get-theme-dictionary-bad
  (fn [db [_ response]]
    (log/error "Failed to load theme dictionary: " response)
    db))


(reg-event-fx
  ::get-theme-dictionary
  (fn [{{:keys [::main-spec/theme-root ::main-spec/theme] :as db} :db} _]
    (if theme
      {:http-xhrio {:method          :get
                    :uri             (str theme-root "dictionary.json")
                    :timeout         8000
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::get-theme-dictionary-good]
                    :on-failure      [::get-theme-dictionary-bad]}}
      {})))
