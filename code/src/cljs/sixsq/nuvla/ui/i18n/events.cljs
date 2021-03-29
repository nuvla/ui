(ns sixsq.nuvla.ui.i18n.events
  (:require
    [re-frame.core :refer [reg-event-db reg-event-fx inject-cofx]]
    [com.degel.re-frame.storage :as storage]
    [sixsq.nuvla.ui.i18n.spec :as spec]))


(def local-storage-key "nuvla.ui.locale")


(reg-event-fx
  ::set-locale
  (fn [{db :db} [_ locale-new]]
    (let [locale-db (::spec/locale db)
          locale    (or locale-new locale-db)]
      {:db          (cond-> db
                            locale-new (assoc ::spec/locale locale-new))
       :storage/set {:session? false
                     :name     local-storage-key
                     :value    locale}})))


(reg-event-fx
  ::get-locale-from-local-storage
  [(inject-cofx :storage/get {:name local-storage-key})]
  (fn [{db :db locale :storage/get}]
    (when-not (empty? locale)
      {:db (assoc db ::spec/locale locale)})))
