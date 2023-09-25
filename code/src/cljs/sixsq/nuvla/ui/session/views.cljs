(ns sixsq.nuvla.ui.session.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.subs :as route-subs]
            [sixsq.nuvla.ui.routing.utils :refer [name->href]]
            [sixsq.nuvla.ui.session.events :as events]
            [sixsq.nuvla.ui.session.reset-password-views :as reset-password-views]
            [sixsq.nuvla.ui.session.set-password-views :as set-password-views]
            [sixsq.nuvla.ui.session.sign-in-views :as sign-in-views]
            [sixsq.nuvla.ui.session.sign-up-views :as sign-up-views]
            [sixsq.nuvla.ui.session.subs :as subs]
            [sixsq.nuvla.ui.session.utils :as utils]
            [sixsq.nuvla.ui.utils.form-fields :as ff]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn SwitchGroupMenuItem
  []
  (let [extended?    (r/atom false)
        search       (r/atom "")
        open         (r/atom false)
        cursor       (r/atom 0)
        on-click     #(dispatch [::events/switch-group % @extended?])
        options      (subscribe [::subs/switch-group-options])
        tr           (subscribe [::i18n-subs/tr])
        is-mobile?   (subscribe [::main-subs/is-mobile-device?])
        active-claim (subscribe [::subs/active-claim])
        id-menu      "nuvla-close-menu-item"]
    (fn []
      (let [visible-opts (filter
                           #(re-matches
                              (re-pattern
                                (str "(?i).*"
                                     (general-utils/regex-escape @search)
                                     ".*"))
                              (str (:text %) (:value %)))
                           @options)]
        (when (seq @options)
          [ui/Dropdown
           {:id            id-menu
            :className     "nuvla-close-menu-item"
            :item          true
            :on-click      #(do (reset! open true)
                                (reset! cursor 0))
            :close-on-blur false
            :on-blur       #(when (not= (.-id (.-target %1))
                                        id-menu)
                              (reset! open false))
            :on-key-down   #(case (.-key %)
                              "ArrowDown" (when (< @cursor (-> visible-opts count dec))
                                            (swap! cursor inc))
                              "ArrowUp" (when (> @cursor 0) (swap! cursor dec))
                              "Enter" (some-> visible-opts (nth @cursor) :value on-click)
                              nil)
            :open          @open
            :on-close      #(do
                              (reset! open false)
                              (reset! search ""))
            :icon          (r/as-element
                             [:<>
                              [icons/UserGroupIcon {:class "large"}]
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
                :on-key-down   #(when (= (.-key %) " ")
                                  (.stopPropagation %))
                :placeholder   (str (@tr [:search]) "...")
                :on-change     (ui-callback/input-callback
                                 #(do
                                    (reset! cursor 0)
                                    (reset! search %)))
                :on-click      #(.stopPropagation %)}]
              [ui/DropdownMenu {:scrolling true}
               (doall
                 (for [{:keys [i value text icon level] :as opt}
                       (->> visible-opts
                            (map-indexed #(assoc %2 :i %1))
                            (partition-by :root)
                            (interpose [:-])
                            (apply concat))]
                   (if (= opt :-)
                     ^{:key (random-uuid)}
                     [ui/DropdownDivider]
                     ^{:key value}
                     [ui/DropdownItem {:on-click #(on-click value)
                                       :selected (= i @cursor)}
                      [:span (str/join (repeat (* level 5) ff/nbsp))]
                      [ui/Icon {:class icon}]
                      (if (= @active-claim value)
                        [:b {:style {:color "#c10e12"}} text]
                        text)]
                     )))]
              [ui/DropdownDivider]
              [ui/DropdownItem
               {:text     (@tr [:show-subgroups-resources])
                :icon     (str (when @extended? "check ")
                               icons/i-square-outline)
                :on-click #(do (swap! extended? not)
                               (on-click @active-claim)
                               (.stopPropagation %))}]])])))))


(defn UserMenuItem
  []
  (let [is-group?    (subscribe [::subs/is-group?])
        on-click     #(dispatch [::routing-events/navigate routes/profile])
        is-mobile?   (subscribe [::main-subs/is-mobile-device?])
        active-claim (subscribe [::subs/active-claim])]
    (fn []
      (let [name (subscribe [::subs/resolve-principal @active-claim])]
        [ui/MenuItem {:className "nuvla-close-menu-item"
                      :on-click  on-click}
         (if @is-group?
           [icons/UserGroupIcon {:class "large"}]
           [icons/UserLargeIcon {:class "large"}])
         (general-utils/truncate @name (if @is-mobile? 6 20))]))))


(defn LogoutMenuItem
  []
  (let [on-click   #(dispatch [::events/logout])
        is-mobile? (subscribe [::main-subs/is-mobile-device?])]
    (fn []
      [ui/MenuItem {:className "nuvla-close-menu-item"
                    :on-click  on-click}
       [icons/ArrowRightFromBracketIcon {:size "large"}]
       (when-not @is-mobile?
         [uix/TR :logout])])))


(defn SignUpMenuItem
  []
  (let [signup-template? (subscribe [::subs/user-template-exist?
                                     utils/user-tmpl-email-password])
        on-click         #(dispatch [::routing-events/navigate routes/sign-up])]
    (fn []
      (when @signup-template?
        [ui/MenuItem {:on-click on-click}
         [uix/TR :sign-up]]))))


(defn SignInButton
  []
  (let [on-click #(dispatch [::routing-events/navigate routes/sign-in])]
    (fn []
      [ui/Button {:primary  true
                  :on-click on-click
                  :style    {:margin "0.6rem 1.5rem 0.6rem 0.6rem"}}
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
         [icons/Icon {:class icon}]])]]))


(defn LeftPanel
  []
  (let [tr                   (subscribe [::i18n-subs/tr])
        first-path           (subscribe [::route-subs/nav-path-first])
        signup-template?     (subscribe [::subs/user-template-exist? utils/user-tmpl-email-password])
        eula                 (subscribe [::main-subs/config :eula])
        terms-and-conditions (subscribe [::main-subs/config :terms-and-conditions])]
    [:div {:class "nuvla-ui-session-left"
           :style {:padding          "4rem"
                   :background-color "#C10E12"}}
     [ui/Image {:alt      "logo"
                :src      "/ui/images/nuvla-logo.png"
                :size     "medium"
                :centered false}]
     [:br]

     [:h1 (@tr [:edge-platform-as-a-service])]
     [:br]

     [:p (@tr [:start-journey-to-the-edge])]

     [:div
      [uix/Button
       {:class                                             "white-button"
        (if (= @first-path "sign-in") :primary :secondary) true
        :text                                              (@tr [:sign-in])
        :inverted                                          true
        :active                                            (= @first-path "sign-in")
        :on-click                                          #(dispatch [::routing-events/navigate routes/sign-in])}]
      (when @signup-template?
        [:span
         [uix/Button
          {:class                                             "white-button"
           (if (= @first-path "sign-up") :primary :secondary) true
           :text                                              (@tr [:sign-up])
           :inverted                                          true
           :active                                            (= @first-path "sign-up")
           :on-click                                          #(dispatch [::routing-events/navigate routes/sign-up])}]
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
  (let [first-path (subscribe [::route-subs/nav-path-first])]
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
        query-params (subscribe [::route-subs/nav-query-params])
        tr           (subscribe [::i18n-subs/tr])]
    (when (and navigate? @session)
      (dispatch [::routing-events/navigate (or (some->
                                                 (:redirect @query-params)
                                                 js/decodeURIComponent)
                                               (name->href routes/home))]))
    (when-let [error (:error @query-params)]
      (dispatch [::events/set-error-message
                 (or (@tr [(keyword error)]) error)]))
    (when-let [message (:message @query-params)]
      (dispatch [::events/set-success-message
                 (or (@tr [(keyword message)]) message)]))
    [ui/Grid {:stackable true
              :columns   2
              :style     {:margin           0
                          :background-color "white"
                          :padding          0}}

     [ui/GridColumn {:class "login-left"}
      [LeftPanel]]
     [ui/GridColumn
      [RightPanel]]]))
