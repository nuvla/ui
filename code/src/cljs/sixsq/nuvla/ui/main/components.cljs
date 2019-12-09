(ns sixsq.nuvla.ui.main.components
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn RefreshedIn
  [action-id]
  (let [tr         (subscribe [::i18n-subs/tr])
        refresh-in (subscribe [::subs/refresh-in action-id])]
    (fn []
      [ui/MenuItem {:disabled true}
       (str (@tr [:automatic-refresh-in]) " " (/ @refresh-in 1000) "s")])))


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
  [callback]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Input {:placeholder (@tr [:search])
               :icon        "search"
               :on-change   (ui-callback/input-callback callback)}]))
