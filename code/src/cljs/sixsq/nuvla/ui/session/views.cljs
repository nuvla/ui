(ns sixsq.nuvla.ui.session.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.authn.subs :as authn-subs]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.spec :as us]
    [cljs.spec.alpha :as s]
    [reagent.core :as r]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [form-validator.core :as fv]))

;; VALIDATION SPEC
(s/def ::username us/nonblank-string)
(s/def ::password us/nonblank-string)

(s/def ::session-template-email-password
  (s/keys :req-un [::username
                   ::password]))


(defn Sign-in-form
  []
  (swap! fv/conf #(merge % {:atom r/atom}))
  (let [spec->msg {::username "Should not be empty."
                   ::password "Should not be empty."}
        form-conf {:names->value {:username ""
                                  :password ""}
                   :form-spec    ::session-template-email-password}
        form      (fv/init-form form-conf)]
    (fn []
      [:div {:style {:margin-left "10%"
                     :margin-top  "30%"}}
       [:span {:style {:font-size "1.4em"}} "Login to " [:b "Account"]]
       [ui/Form {:style {:margin-top 30
                         :max-width  "70%"}}
        [ui/FormInput {:name      :username
                       :label     "Username"
                       :on-change (partial fv/event->names->value! form)
                       :on-blur   (partial fv/event->show-message form)
                       :error     (fv/?show-message form :username spec->msg)}]
        [ui/FormInput {:name      :password
                       :label     "Password"
                       :type      "password"
                       :on-change (partial fv/event->names->value! form)
                       :on-blur   (partial fv/event->show-message form)
                       :error     (fv/?show-message form :password spec->msg)}]
        [ui/FormField
         [:a {:href ""} "Forgot your password?"]]
        [ui/Button {:primary  true
                    :floated  "right"
                    :on-click #(when (fv/validate-form-and-show? form)
                                 (js/alert "Well done"))
                    :style    {:border-radius 0}} "Sign in"]]

       [:div {:style {:margin-top 70
                      :color      "grey"}} "or use your github account "
        [ui/Button {:style    {:margin-left 10}
                    :circular true
                    :basic    true
                    :class    "icon"}
         [ui/Icon {:name "github"
                   :size "large"}]]]])))



(defn SessionPage
  [Right-Panel]
  (let [session (subscribe [::authn-subs/session])]
    (when @session
      (dispatch [::history-events/navigate "welcome"]))
    [ui/Grid {:stackable true
              :columns   2
              :reversed  "mobile"
              :style     {:margin           0
                          :background-color "white"}}
     [ui/GridColumn {:style {:background-image    "url(/ui/images/volumlight.png)"
                             :background-size     "cover"
                             :background-position "left"
                             :background-repeat   "no-repeat"
                             :color               "white"
                             :min-height          "100vh"}}
      [:div {:style {:padding "75px"}}
       #_[:div {:style {:font-size "2em"}}
          "Welcome to"]
       [:div {:style {:font-size   "6em"
                      :line-height "normal"}}
        "Nuvla"]
       [:br]

       [:div {:style {:margin-top  40
                      :line-height "normal"
                      :font-size   "2em"}}
        "Start immediately deploying apps containers in one button click."]
       [:br]

       [:b {:style {:font-size "1.4em"}} "Start jouney with us"]

       [:br] [:br]
       [ui/Button {:style {:border-radius 0}
                   :size  "large" :inverted true} "Sign up"]
       [:div {:style {:margin-top  20
                      :line-height "normal"}}
        (str "Provide a secured edge to cloud (and back) management platform "
             "that enabled near-data AI for connected world use cases.")]

       [:div {:style {:position "absolute"
                      :bottom   40}}
        "Follow us on "
        [:span
         [ui/Icon {:name "facebook"}]
         [ui/Icon {:name "twitter"}]
         [ui/Icon {:name "youtube"}]]]

       ]
      ]
     [ui/GridColumn
      [Right-Panel]
      ]
     ]))


(defn Sign-in
  []
  [SessionPage Sign-in-form])