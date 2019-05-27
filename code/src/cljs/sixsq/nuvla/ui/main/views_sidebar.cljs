(ns sixsq.nuvla.ui.main.views-sidebar
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.authn.subs :as authn-subs]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [taoensso.timbre :as log]))


(defn navigate
  "Fires a navigation event to the given URL. On small devices, this also
   forces the sidebar to close."
  [url]
  (let [device (subscribe [::main-subs/device])]
    (when (#{:mobile :tablet} @device)
      (dispatch [::main-events/close-sidebar]))
    (dispatch [::history-events/navigate url])))


(defn item
  [label-kw url icon]
  (let [tr       (subscribe [::i18n-subs/tr])
        nav-path (subscribe [::main-subs/nav-path])
        is-user? (subscribe [::authn-subs/is-user?])]

    ^{:key (name label-kw)}
    [uix/MenuItemWithIcon
     {:name      (@tr [label-kw])
      :icon-name icon
      :active    (= (first @nav-path) url)
      :on-click  (fn []
                   (when @is-user?
                     (navigate url)))}]))


(defn logo-item
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    ^{:key "welcome"}
    [ui/MenuItem {:aria-label (@tr [:welcome])
                  :on-click   #(navigate "welcome")}
     [ui/Image {:alt      "logo"
                :src      "/ui/images/nuvla-logo.png"
                :size     "tiny"
                :style    {:margin-top    "10px"
                           :margin-bottom "10px"}
                :centered true}]]))


(defn menu
  "Provides the sidebar menu for selecting major components/panels of the
   application."
  []
  (let [show?   (subscribe [::main-subs/sidebar-open?])
        iframe? (subscribe [::main-subs/iframe?])]
    [ui/Sidebar {:as        ui/MenuRaw
                 :className "medium thin"
                 :vertical  true
                 :inverted  true
                 :visible   @show?
                 :animation "uncover"}
     [:nav {:aria-label "sidebar"}
      [ui/Menu {:icon     "labeled"
                :size     "large"
                :vertical true
                :compact  true
                :inverted true}
       (when-not @iframe? [logo-item])
       (when-not @iframe? [item :dashboard "deployment" "dashboard"])
       (when-not @iframe? [item :apps "apps" "play"])
       [item :data "data" "database"]
       ;       [item :deployment "deployment" "cloud"]
       (when-not @iframe? [item :infra-service-short "infrastructures" "cloud"])
       (when-not @iframe? [item :credentials "credentials" "key"])
       (when-not @iframe? [item :edge "edge" "box"])
       (when-not @iframe? [item :api "api" "code"])]]]))
