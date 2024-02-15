(ns sixsq.nuvla.ui.pages.about.subs
  (:require [re-frame.core :refer [reg-sub]]
            [sixsq.nuvla.ui.pages.about.spec :as spec]
            [sixsq.nuvla.ui.pages.about.utils :as utils]))

(reg-sub
  ::enabled-feature-flags
  :-> ::spec/enabled-feature-flags)

(reg-sub
  ::feature-flag-enabled?
  :<- [::enabled-feature-flags]
  (fn [enabled-feature-flags [_ k]]
    (utils/feature-flag-enabled? enabled-feature-flags k)))
