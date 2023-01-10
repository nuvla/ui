(ns sixsq.nuvla.ui.messages.views
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.cimi-detail.events :as cimi-detail-events]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.messages.events :as events]
            [sixsq.nuvla.ui.messages.subs :as subs]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.time :as time]))


(defn type->icon-name
  [type]
  (case type
    :error "warning circle"
    :info "info circle"
    :success "check circle"
    :notif "bullhorn"
    "warning circle"))


(defn type->message-type
  [type]
  (case type
    :error {:error true}
    :info {:info true}
    :success {:success true}
    {:info true}))


(defn message-detail-modal
  [icon-name header content visible? f]
  [ui/Modal
   {:close-icon true
    :open       @visible?
    :on-close   #(do
                   (reset! visible? false)
                   (when f (f)))}
   [uix/ModalHeader {:header header :icon icon-name}]
   (when content
     [ui/ModalContent {:scrolling true}
      [:pre content]])])


(defn alert-slider
  []
  (let [tr            (subscribe [::i18n-subs/tr])
        alert-message (subscribe [::subs/alert-message])
        alert-display (subscribe [::subs/alert-display])]
    (fn []
      (when-let [{:keys [type header]} @alert-message]
        (let [icon-name  (type->icon-name type)
              open?      (boolean (and @alert-message (= :slider @alert-display)))
              transition (clj->js {:animation "slide left"
                                   :duration  500})
              top-right  {:position "fixed", :top "30px", :right "5px", :zIndex 1000}]
          [ui/TransitionablePortal {:transition transition, :open open?}
           [ui/Message (merge (type->message-type type)
                              {:style      top-right
                               :on-dismiss #(dispatch [::events/hide])})
            [ui/MessageHeader [ui/Icon {:name icon-name}] header "\u2001\u00a0"]
            [:a {:on-click #(dispatch [::events/open-modal])} (@tr [:more-info])]]])))))


(defn alert-modal
  []
  (let [tr            (subscribe [::i18n-subs/tr])
        alert-message (subscribe [::subs/alert-message])
        alert-display (subscribe [::subs/alert-display])]
    (when-let [{:keys [type header content data]} @alert-message]
      (let [icon-name (type->icon-name type)
            uuid      (:uuid @alert-message)
            is-notif? (= type :notif)
            visible?  (= :modal @alert-display)
            hide-fn   #(dispatch [::events/hide])
            remove-fn #(dispatch [::events/remove uuid])]
        [ui/Modal {:open       visible?
                   :close-icon true
                   :on-close   hide-fn}

         [uix/ModalHeader {:header header :icon icon-name}]

         [ui/ModalContent {:scrolling true}
          (when content [:pre content])]

         [ui/ModalActions
          [uix/Button {:text (@tr [:close]), :on-click hide-fn}]
          (when-not is-notif?
            [uix/Button {:text (@tr [:clear]), :negative true, :on-click remove-fn}])
          (when is-notif?
            (let [{callback-id     :callback
                   notification-id :id} data]
              [:<>
               (when (general-utils/can-operation? "defer" data)
                 [uix/Button
                  {:secondary true
                   :text      "defer"
                   :on-click  #(do (dispatch
                                     [::cimi-detail-events/operation notification-id "defer"])
                                   (remove-fn))}])
               (when (:callback data)
                 [uix/Button
                  {:primary  true
                   :text     "apply"
                   :on-click #(do
                                (dispatch [::cimi-detail-events/operation callback-id "execute"])
                                (remove-fn))}])]))
          ]]))))


(defn feed-item
  [locale {:keys [type header timestamp] :as message}]
  (let [icon-name       (type->icon-name type)
        message-options (type->message-type type)]
    [ui/ListItem {:on-click #(dispatch [::events/show message])}
     [ui/Message message-options
      [ui/MessageHeader
       [ui/Icon {:name icon-name}]
       header]
      [ui/MessageContent
       (time/ago timestamp locale)]]]))


(defn message-feed
  []
  (let [locale   (subscribe [::i18n-subs/locale])
        messages (subscribe [::subs/messages])]
    (when (seq @messages)
      [ui/ListSA {:selection true
                  :style     {:height     "100%"
                              :max-height "40ex"
                              :overflow-y "auto"}}
       (doall
         (for [{:keys [uuid] :as message} @messages]
           ^{:key uuid}
           [feed-item @locale message]))])))


(defn bell-menu
  "Provides a messages menu icon that will bring up the list of recent
   messages. If there are no messages, the item will be disabled. If there are
   messages, then a label will show the number of them."
  []
  (let [tr              (subscribe [::i18n-subs/tr])
        is-user?        (subscribe [::session-subs/is-user?])
        messages        (subscribe [::subs/messages])
        popup-open?     (subscribe [::subs/popup-open?])
        show-clear-all? (subscribe [::subs/show-clear-all?])]
    (when @is-user?
      (let [n         (count @messages)
            disabled? (zero? n)]
        [ui/Popup {:flowing  true
                   :on       "click"
                   :position "bottom right"
                   :open     (boolean @popup-open?)
                   :on-open  #(dispatch [::events/open-popup])
                   :on-close #(dispatch [::events/close-popup])
                   :trigger  (r/as-element
                               [ui/MenuItem {:className "nuvla-close-menu-item"
                                             :disabled  disabled?}
                                n
                                [ui/Icon {:name "fa-light fa-bell large"
                                          :size "large"}]])}
         [ui/PopupHeader (@tr [:notifications])]
         [ui/PopupContent [ui/Divider]]
         [ui/PopupContent [message-feed]]
         (when @show-clear-all?
           [ui/PopupContent
            [ui/Divider]
            [uix/Button {:text     (@tr [:clear-all])
                         :fluid    true
                         :negative true
                         :compact  true
                         :on-click #(dispatch [::events/clear-all])}]])]))))
