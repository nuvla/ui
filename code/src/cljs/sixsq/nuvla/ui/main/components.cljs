(ns sixsq.nuvla.ui.main.components
  (:require
    ["react" :as react]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [clojure.string :as str]
    [reagent.core :as r]))

(def ref (react/createRef))


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


(defn StickyBar [Menu]
  [ui/Sticky {:offset  -1
              :context ref
              :style   {:margin-top    11
                        :margin-bottom 10}}
   Menu])


(defn ErrorJobsMessage
  [resource-subs job-subs set-active-tab-index-event job-tab-index]
  (let [errors-dissmissed (r/atom #{})]
    (fn [resource-subscription]
      (let [{:keys [state]} @(subscribe [resource-subscription])
            jobs            (subscribe [job-subs])
            last-failed-job (some #(when (= (:state %) "FAILED") %) (:resources @jobs))
            action          (:action last-failed-job)
            id              (:id last-failed-job)
            last-line       (last (str/split-lines (get last-failed-job :status-message "")))]
        (when (and
                (not (@errors-dissmissed id))
                ;(= state "ERROR")
                (some? last-failed-job))
          [ui/Message {:error      true
                       :on-dismiss #(swap! errors-dissmissed conj id)}
           [ui/MessageHeader
            {:style    {:cursor "pointer"}
             :on-click #(dispatch [set-active-tab-index-event job-tab-index])}
            (str "Job " action " failed")]
           [ui/MessageContent last-line]])))))
