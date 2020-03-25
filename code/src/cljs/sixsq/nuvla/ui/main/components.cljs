(ns sixsq.nuvla.ui.main.components
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))


(defn RefreshedIn
  [action-id]
  (let [tr           (subscribe [::i18n-subs/tr])
        next-refresh (subscribe [::subs/next-refresh action-id])]
    (fn []
      [ui/MenuItem {:disabled true}
       [:span (@tr [:automatic-refresh-in]) " "
        (when @next-refresh
          [uix/CountDown @next-refresh]) "s"]])))


(defn RefreshButton
  [loading? on-click refresh-disabled?]
  (let [tr       (subscribe [::i18n-subs/tr])
        on-click (or on-click #())]
    [uix/MenuItemWithIcon
     {:name      (@tr [:refresh])
      :icon-name "refresh"
      :loading?  (boolean loading?)
      :on-click  on-click
      :disabled  (boolean refresh-disabled?)}]))


(defn RefreshMenu
  [{:keys [action-id loading? on-refresh refresh-disabled?]}]
  [ui/MenuMenu {:position :right}
   (when action-id
     [RefreshedIn action-id])
   (when on-refresh
     [RefreshButton loading? on-refresh refresh-disabled?])])


(defn SearchInput
  [opts]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Input (merge {:placeholder (@tr [:search])
                      :icon        "search"}
                     opts)]))
