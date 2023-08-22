(ns sixsq.nuvla.ui.i18n.events
  (:require     #_:clj-kondo/ignore
    [com.degel.re-frame.storage :as storage]
    [re-frame.core :refer [inject-cofx reg-event-fx]]
    [sixsq.nuvla.ui.i18n.spec :as spec]
    [sixsq.nuvla.ui.i18n.utils :as utils]))

(def local-storage-key "nuvla.ui.locale")


(reg-event-fx
  ::set-locale
  [(inject-cofx :storage/get {:name local-storage-key})]
  (fn [{{locale-db ::spec/locale :as db} :db
        locale-storage                   :storage/get} [_ locale-arg]]
    (let [locale (or locale-arg locale-storage locale-db)]
      {:db          (assoc db ::spec/locale locale
                              ::spec/tr (utils/create-tr-fn locale))
       :storage/set {:session? false
                     :name     local-storage-key
                     :value    locale}})))
