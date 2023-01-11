(ns sixsq.nuvla.ui.i18n.subs
  (:require [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.i18n.spec :as spec]))

(reg-sub
  ::locale
  :-> ::spec/locale)

(reg-sub
  ::tr
  :-> ::spec/tr)
