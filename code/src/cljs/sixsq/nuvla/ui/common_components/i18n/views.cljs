(ns sixsq.nuvla.ui.common-components.i18n.views
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.i18n.events :as i18n-events]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.i18n.utils :as utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))


(defn LocaleDropdownItem
  [{:keys [value text]}]
  (let [on-click #(dispatch [::i18n-events/set-locale value])]
    [ui/DropdownItem {:on-click on-click} text]))


(defn LocaleDropdown
  []
  (let [locale (subscribe [::i18n-subs/locale])
        Icon   [:span [icons/GlobeIcon] @locale]]
    [ui/Dropdown {:close-on-change true
                  :item            true
                  :icon            nil
                  :pointing        "top right"
                  :trigger         (r/as-element Icon)}
     [ui/DropdownMenu
      (for [{:keys [value] :as locale-choice} (utils/locale-choices)]
        ^{:key value}
        [LocaleDropdownItem locale-choice])]]))
