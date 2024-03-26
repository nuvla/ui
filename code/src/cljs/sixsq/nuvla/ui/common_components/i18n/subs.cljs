(ns sixsq.nuvla.ui.common-components.i18n.subs
  (:require [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.common-components.i18n.spec :as spec]))

(reg-sub
  ::locale
  :-> ::spec/locale)

(reg-sub
  ::tr
  :-> ::spec/tr)
