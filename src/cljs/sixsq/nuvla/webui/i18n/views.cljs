(ns sixsq.nuvla.webui.i18n.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.webui.i18n.events :as i18n-events]
    [sixsq.nuvla.webui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.webui.i18n.utils :as utils]
    [sixsq.nuvla.webui.utils.semantic-ui :as ui]))


(defn locale-dropdown-item
  [{:keys [value text]}]
  (let [on-click #(dispatch [::i18n-events/set-locale value])]
    [ui/DropdownItem {:on-click on-click} text]))


(defn locale-dropdown
  []
  (let [locale (subscribe [::i18n-subs/locale])]
    (fn []
      [ui/Dropdown {:close-on-change true
                    :upward          true
                    :item            true
                    :icon            nil
                    :pointing        "top right"
                    :trigger         (r/as-element [:span [ui/Icon {:name "globe"}] @locale])}
       (vec (concat [ui/DropdownMenu]
                    (map locale-dropdown-item (utils/locale-choices))))])))
