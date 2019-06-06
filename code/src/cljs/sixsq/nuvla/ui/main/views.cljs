(ns sixsq.nuvla.ui.main.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.about.views]
    [sixsq.nuvla.ui.apps-component.views]
    [sixsq.nuvla.ui.apps-project.views]
    [sixsq.nuvla.ui.apps-store.views]
    [sixsq.nuvla.ui.apps.events :as apps-events]
    [sixsq.nuvla.ui.apps.views]
    [sixsq.nuvla.ui.authn.views :as authn-views]
    [sixsq.nuvla.ui.cimi.subs :as api-subs]
    [sixsq.nuvla.ui.cimi.views]
    [sixsq.nuvla.ui.credentials.events :as credential-events]
    [sixsq.nuvla.ui.credentials.views]
    [sixsq.nuvla.ui.dashboard.views]
    [sixsq.nuvla.ui.data.views]
    [sixsq.nuvla.ui.docs.views]
    [sixsq.nuvla.ui.edge.views]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.history.utils :as history-utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.i18n.views :as i18n-views]
    [sixsq.nuvla.ui.infrastructures.events :as infra-service-events]
    [sixsq.nuvla.ui.infrastructures.views]
    [sixsq.nuvla.ui.main.events :as events]
    [sixsq.nuvla.ui.main.subs :as subs]
    [sixsq.nuvla.ui.main.views-sidebar :as sidebar]
    [sixsq.nuvla.ui.messages.views :as messages]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.profile.views]
    [sixsq.nuvla.ui.utils.general :as utils]
    [sixsq.nuvla.ui.utils.responsive :as responsive]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.welcome.views]
    [taoensso.timbre :as log]))


(defn crumb
  [index segment]
  (let [nav-path    (subscribe [::subs/nav-path])
        callback-fn #(dispatch [::history-events/navigate (history-utils/trim-path @nav-path index)])]
    ^{:key (str index "_" segment)}
    [ui/BreadcrumbSection
     [:a {:on-click callback-fn
          :style    {:cursor "pointer"}}
      (utils/truncate (str segment))]]))


(defn breadcrumbs-links []
  (let [nav-path (subscribe [::subs/nav-path])]
    (vec (concat [ui/Breadcrumb {:size :large}]
                 (->> @nav-path
                      (map crumb (range))
                      (interpose [ui/BreadcrumbDivider {:icon "chevron right"}]))))))


(defn breadcrumb-option
  [index segment]
  {:key   segment
   :value index
   :text  (utils/truncate segment 8)})


(defn breadcrumbs-dropdown []
  (let [path        (subscribe [::subs/nav-path])
        options     (map breadcrumb-option (range) @path)
        selected    (-> options last :value)
        callback-fn #(dispatch [::history-events/navigate (history-utils/trim-path @path %)])]
    [ui/Dropdown
     {:inline    true
      :value     selected
      :on-change (ui-callback/value callback-fn)
      :options   options}]))


(defn breadcrumbs []
  (let [device (subscribe [::subs/device])]
    (if (#{:mobile} @device)
      [breadcrumbs-dropdown]
      [breadcrumbs-links])))


(defn footer
  []
  [:footer.nuvla-ui-footer
   [:div.nuvla-ui-footer-left
    [:span "© 2019, SixSq Sàrl"]]
   [:div.nuvla-ui-footer-centre
    [:a {:on-click #(dispatch
                      [::history-events/navigate "about"])
         :style    {:cursor "pointer"}}
     [:span#release-version (str "v")]]]
   [:div.nuvla-ui-footer-right
    [i18n-views/locale-dropdown]]])


(defn ignore-changes-modal
  []
  (let [tr                (subscribe [::i18n-subs/tr])
        navigation-info   (subscribe [::subs/ignore-changes-modal])
        ignore-changes-fn #(dispatch [::events/ignore-changes false])]

    [ui/Modal {:open       (some? @navigation-info)
               :close-icon true
               :on-close   ignore-changes-fn}

     [ui/ModalHeader (str/capitalize (str (@tr [:ignore-changes?])))]

     [ui/ModalContent {:content (@tr [:ignore-changes-content])}]

     [ui/ModalActions
      [uix/Button {:text     (@tr [:ignore-changes]),
                   :positive true
                   :active   true
                   :on-click #(do (dispatch [::events/ignore-changes true])
                                  (dispatch [::apps-events/form-valid]))}]]]))


(defmulti BootstrapMessage identity)

(defmethod BootstrapMessage :no-swarm
  [_]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Message
     {:icon       "inbox"
      :info       true
      :on-dismiss #(dispatch [::events/set-bootsrap-message])
      :header     (@tr [:message-no-swarm])
      :content    (r/as-element [:p (@tr [:message-to-create-one])
                                 [:a
                                  {:style    {:cursor "pointer"}
                                   :on-click #(do (dispatch [::history-events/navigate "infrastructures"])
                                                  (dispatch [::infra-service-events/open-add-service-modal]))}
                                  (str " " (@tr [:click-here]))]])}]))


(defmethod BootstrapMessage :no-credential
  [_]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Message
     {:icon       "inbox"
      :info       true
      :on-dismiss #(dispatch [::events/set-bootsrap-message])
      :header     (@tr [:message-no-credential])
      :content    (r/as-element [:p (@tr [:message-to-create-one])
                                 [:a
                                  {:style    {:cursor "pointer"}
                                   :on-click #(do (dispatch [::history-events/navigate "credentials"])
                                                  (dispatch [::credential-events/open-add-credential-modal]))}
                                  (str " " (@tr [:click-here]))]])}]))


(defn WelcomeMessage
  []
  (let [tr              (subscribe [::i18n-subs/tr])
        welcome-message (subscribe [::subs/welcome-message])]
    (fn []
      (when @welcome-message
        (js/setTimeout #(dispatch [::events/set-welcome-message nil]) 10000) ;; hide after 20s

        [ui/Container {:text-align :center}
         [ui/Message
          {:success true
           :content (@tr [@welcome-message])}]
         [:br]]))))


(defn contents
  []
  (let [resource-path     (subscribe [::subs/nav-path])
        bootstrap-message (subscribe [::subs/bootstrap-message])]
    (fn []
      [ui/Container {:as         "main"
                     :class-name "nuvla-ui-content"
                     :fluid      true}

       [WelcomeMessage]

       (when @bootstrap-message
         [BootstrapMessage @bootstrap-message])
       (panel/render @resource-path)])))


(defn header
  []
  [:header
   [ui/Menu {:className  "nuvla-ui-header"
             :borderless true}

    [ui/MenuItem {:aria-label "toggle sidebar"
                  :link       true
                  :on-click   #(dispatch [::events/toggle-sidebar])}
     [ui/Icon {:name "bars"}]]

    [ui/MenuItem [breadcrumbs]]

    [ui/MenuMenu {:position "right"}
     [messages/bell-menu]
     [ui/MenuItem {:fitted true}
      [authn-views/authn-menu]]]]

   [messages/alert-slider]
   [messages/alert-modal]])


(defn app []
  (fn []
    (let [show?   (subscribe [::subs/sidebar-open?])
          cep     (subscribe [::api-subs/cloud-entry-point])
          iframe? (subscribe [::subs/iframe?])]

      (if @cep
        [ui/Responsive {:as            "div"
                        :fire-on-mount true
                        :on-update     (responsive/callback #(dispatch [::events/set-device %]))}
         [ui/SidebarPushable {:as    ui/SegmentRaw
                              :basic true}
          [sidebar/menu]
          [ui/SidebarPusher
           [ui/Container (cond-> {:id "nuvla-ui-main" :fluid true}
                                 @show? (assoc :className "sidebar-visible"))
            [header]
            [contents]
            [ignore-changes-modal]
            (when-not @iframe? [footer])]]]]
        [ui/Container [ui/Loader {:active true :size "massive"}]]))))
