(ns sixsq.slipstream.webui.i18n.subs
  (:require
    [re-frame.core :refer [reg-sub]]))


(reg-sub
  ::locale
  :sixsq.slipstream.webui.i18n.spec/locale)


(reg-sub
  ::tr
  :sixsq.slipstream.webui.i18n.spec/tr)
