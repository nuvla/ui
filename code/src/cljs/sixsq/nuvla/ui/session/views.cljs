(ns sixsq.nuvla.ui.session.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.session.events :as events]
    [sixsq.nuvla.ui.session.reset-password-views :as reset-password-views]
    [sixsq.nuvla.ui.session.set-password-views :as set-password-views]
    [sixsq.nuvla.ui.session.sign-in-views :as sign-in-views]
    [sixsq.nuvla.ui.session.sign-up-views :as sign-up-views]
    [sixsq.nuvla.ui.session.subs :as subs]
    [sixsq.nuvla.ui.session.utils :as utils]
    [sixsq.nuvla.ui.utils.form-fields :as ff]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn SwitchGroupMenuItem
  []
  (let [extended?    (r/atom false)
        search       (r/atom "")
        open         (r/atom false)
        on-click     #(dispatch [::events/switch-group % @extended?])
        options      (subscribe [::subs/switch-group-options])
        tr           (subscribe [::i18n-subs/tr])
        is-mobile?   (subscribe [::main-subs/is-mobile-device?])
        active-claim (subscribe [::subs/active-claim])
        id-menu      "nuvla-close-menu-item"]
    (fn []
      (let [visible-opts (->> @options
                              (filter
                                #(re-matches
                                   (re-pattern
                                     (str "(?i).*"
                                          (general-utils/regex-escape @search)
                                          ".*"))
                                   (str (:text %) (:value %))))
                              doall)]
        (when (seq @options)
          [ui/Dropdown
           {:id            id-menu
            :className     "nuvla-close-menu-item"
            :item          true
            :on-click      #(reset! open true)
            :close-on-blur false
            :on-blur       #(when (not= (.-id (.-target %1))
                                        id-menu)
                              (reset! open false))
            :open          @open
            :on-close      #(do
                              (reset! open false)
                              (reset! search ""))
            :icon          (r/as-element
                             [:<>
                              [ui/IconGroup
                               [ui/Icon {:name "users" :size "large"}]
                               [ui/Icon {:name "refresh" :corner "top right"}]]
                              (when-not @is-mobile?
                                [uix/TR :switch-group])])}
           (when @open
             [ui/DropdownMenu
              [ui/Input
               {:icon          :search
                :icon-position :left
                :class-name    "search"
                :auto-complete :off
                :auto-focus    true
                :value         @search
                :placeholder   (@tr [:search])
                :on-change     (ui-callback/input-callback
                                 #(reset! search %))
                :on-click      #(.stopPropagation %)}]
              [ui/DropdownMenu {:scrolling true}
               (for [{:keys [value text icon level selected]} visible-opts]
                 ^{:key value}
                 [ui/DropdownItem {:on-click #(on-click value)
                                   :selected selected}
                  [:span (str/join (repeat (* level 5) ff/nbsp))]
                  [ui/Icon {:name icon}]
                  (if selected
                    [:b text]
                    text)])]
              [ui/DropdownDivider]
              [ui/DropdownItem
               {:text     "show subgroups resources"
                :icon     (str (when @extended? "check ")
                               "square outline")
                :on-click #(do (swap! extended? not)
                               (on-click @active-claim)
                               (.stopPropagation %))}]])])))))


(defn UserMenuItem
  []
  (let [user       (subscribe [::subs/user])
        is-group?  (subscribe [::subs/is-group?])
        on-click   #(dispatch [::history-events/navigate "profile"])
        is-mobile? (subscribe [::main-subs/is-mobile-device?])]
    (fn []
      [ui/MenuItem {:className "nuvla-close-menu-item"
                    :on-click  on-click}
       [ui/Icon {:name     (if @is-group? "group" "user")
                 :circular true}]
       (-> @user
           utils/remove-group-prefix
           (general-utils/truncate (if @is-mobile? 6 20)))])))


(defn LogoutMenuItem
  []
  (let [on-click   #(dispatch [::events/logout])
        is-mobile? (subscribe [::main-subs/is-mobile-device?])]
    (fn []
      [ui/MenuItem {:className "nuvla-close-menu-item"
                    :on-click  on-click}
       [ui/Icon {:name "sign-out"
                 :size "large"}]
       (when-not @is-mobile?
         [uix/TR :logout])])))


(defn SignUpMenuItem
  []
  (let [signup-template? (subscribe [::subs/user-template-exist?
                                     utils/user-tmpl-email-password])
        on-click         #(dispatch [::history-events/navigate "sign-up"])]
    (fn []
      (when @signup-template?
        [ui/MenuItem {:on-click on-click}
         [ui/Icon {:name "signup"}]
         [uix/TR :sign-up]]))))


(defn SignInButton
  []
  (let [on-click #(dispatch [::history-events/navigate "sign-in"])]
    (fn []
      [ui/Button {:primary  true
                  :on-click on-click}
       [ui/Icon {:name "sign in"}]
       [uix/TR :login]])))


(defn AuthnMenu
  []
  (let [logged-in? (subscribe [::subs/logged-in?])]
    (fn []
      (if @logged-in?
        [:<>
         [SwitchGroupMenuItem]
         [UserMenuItem]
         [LogoutMenuItem]]
        [:<>
         [SignUpMenuItem]
         [SignInButton]]))))


(defn follow-us
  []
  (let [tr           (subscribe [::i18n-subs/tr])
        linkedin     (subscribe [::main-subs/config :linkedin])
        twitter      (subscribe [::main-subs/config :twitter])
        facebook     (subscribe [::main-subs/config :facebook])
        youtube      (subscribe [::main-subs/config :youtube])
        social-media (remove #(nil? (second %))
                             [["linkedin" @linkedin]
                              ["twitter" @twitter]
                              ["facebook" @facebook]
                              ["youtube" @youtube]])]
    [:<>
     (when (seq social-media)
       (@tr [:follow-us-on]))
     [:span
      (for [[icon url] social-media]
        [:a {:key    url
             :href   url
             :target "_blank"
             :style  {:color "white"}}
         [ui/Icon {:name icon}]])]]))


(defn LeftPanel
  []
  (let [tr                   (subscribe [::i18n-subs/tr])
        first-path           (subscribe [::main-subs/nav-path-first])
        signup-template?     (subscribe [::subs/user-template-exist? utils/user-tmpl-email-password])
        eula                 (subscribe [::main-subs/config :eula])
        terms-and-conditions (subscribe [::main-subs/config :terms-and-conditions])]
    [:div {:class "nuvla-ui-session-left"}
     [ui/Image {:alt      "logo"
                :src      "/ui/images/nuvla-logo.png"
                :size     "medium"
                :centered false}]
     [:br]

     [:div {:style {:line-height "normal"
                    :font-size   "2em"}}
      (@tr [:edge-platform-as-a-service])]
     [:br]

     [:p {:style {:font-size "1.4em"}} (@tr [:start-journey-to-the-edge])]

     [:br] [:br]
     [:div
      [uix/Button
       {:text     (@tr [:sign-in])
        :inverted true
        :active   (= @first-path "sign-in")
        :on-click #(dispatch [::history-events/navigate "sign-in"])}]
      (when @signup-template?
        [:span
         [uix/Button
          {:text     (@tr [:sign-up])
           :inverted true
           :active   (= @first-path "sign-up")
           :on-click #(dispatch [::history-events/navigate "sign-up"])}]
         [:br]
         [:br]
         (when @terms-and-conditions
           [:a {:href   @terms-and-conditions
                :target "_blank"
                :style  {:margin-top 20 :color "white" :font-style "italic"}}
            (@tr [:terms-and-conditions])])
         (when (and @terms-and-conditions @eula) " and ")
         (when @eula
           [:a {:href   @eula
                :target "_blank"
                :style  {:margin-top 20 :color "white" :font-style "italic"}}
            (@tr [:terms-end-user-license-agreement])])])]
     [:br]
     [:a {:href   "https://docs.nuvla.io"
          :target "_blank"
          :style  {:color "white"}}
      [:p {:style {:font-size "1.2em" :text-decoration "underline"}}
       (@tr [:getting-started-docs])]]
     [:div {:style {:margin-top  20
                    :line-height "normal"}}
      (@tr [:keep-data-control])]

     [:div {:style {:position "absolute"
                    :bottom   40}}
      [follow-us]]]))


(defn RightPanel
  []
  (let [first-path (subscribe [::main-subs/nav-path-first])]
    (case @first-path
      "sign-in" [sign-in-views/Form]
      "sign-up" [sign-up-views/Form]
      "reset-password" [reset-password-views/Form]
      "set-password" [set-password-views/Form]
      "sign-in-token" [sign-in-views/FormTokenValidation]
      [sign-in-views/Form])))


(defn SessionPage
  [navigate?]
  (let [session      (subscribe [::subs/session])
        query-params (subscribe [::main-subs/nav-query-params])
        tr           (subscribe [::i18n-subs/tr])]
    (when (and navigate? @session)
      (dispatch [::history-events/navigate (or (:redirect @query-params)
                                               "welcome")]))
    (when-let [error (:error @query-params)]
      (dispatch [::events/set-error-message
                 (or (@tr [(keyword error)]) error)]))
    (when-let [message (:message @query-params)]
      (dispatch [::events/set-success-message
                 (or (@tr [(keyword message)]) message)]))
    [ui/Grid {:stackable true
              :columns   2
              :style     {:margin           0
                          :background-color "white"}}

     [ui/GridColumn {:style {:background-image    "url(/ui/images/session.png)"
                             :background-size     "cover"
                             :background-position "left"
                             :background-repeat   "no-repeat"
                             :color               "white"
                             :min-height          "100vh"}}
      [LeftPanel]]
     [ui/GridColumn
      [RightPanel]]]))
