(ns sixsq.nuvla.ui.i18n.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.i18n.spec :as spec]
    [sixsq.nuvla.ui.i18n.utils :as utils]))


(reg-sub
  ::locale
  (fn [db]
    (::spec/locale db)))


(reg-sub
  ::tr
  (fn [db]
    (::spec/tr db)))
