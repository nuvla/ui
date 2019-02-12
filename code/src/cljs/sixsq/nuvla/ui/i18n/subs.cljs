(ns sixsq.nuvla.ui.i18n.subs
  (:require
    [re-frame.core :refer [reg-sub]]))


(reg-sub
  ::locale
  :sixsq.nuvla.ui.i18n.spec/locale)


(reg-sub
  ::tr
  :sixsq.nuvla.ui.i18n.spec/tr)
