(ns sixsq.nuvla.ui.main.views-sidebar
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.authn.events :as authn-events]
    [sixsq.nuvla.ui.authn.subs :as authn-subs]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.events :as events]
    [sixsq.nuvla.ui.main.subs :as subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [taoensso.timbre :as log]))


(defn navigate
  "Fires a navigation event to the given URL. On small devices, this also
   forces the sidebar to close."
  [url]
  (let [device (subscribe [::subs/device])]
    (when (#{:mobile :tablet} @device)
      (dispatch [::events/close-sidebar]))
    (dispatch [::history-events/navigate url])))


(defn item
  [label-kw url icon protected?]
  (let [tr       (subscribe [::i18n-subs/tr])
        is-user? (subscribe [::authn-subs/is-user?])
        active?  (subscribe [::subs/nav-url-active? url])]

    ^{:key (name label-kw)}
    [uix/MenuItemWithIcon
     {:name      (@tr [label-kw])
      :icon-name icon
      :style     {:min-width  "15rem"
                  :overflow-x "hidden"}
      :active    @active?
      :on-click  (if (and protected? (not @is-user?))
                   #(dispatch [::authn-events/open-modal :login])
                   #(navigate url))}]))


(defn logo-item
  []
  (let [tr           (subscribe [::i18n-subs/tr])
        welcome-page (subscribe [::subs/page-info "welcome"])]
    ^{:key "welcome"}
    [ui/MenuItem {:aria-label (@tr [:welcome])
                  :style      {:overflow-x "hidden"
                               :min-width  "15rem"}
                  :on-click   #(navigate (:url @welcome-page))}
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
  (let [show?      (subscribe [::subs/sidebar-open?])
        iframe?    (subscribe [::subs/iframe?])
        pages-list (subscribe [::subs/pages-list])]
    [ui/Menu {:id         "nuvla-ui-sidebar"
              :style      {:transition "0.5s"
                           :width      (if @show? "15rem" "0")}
              :vertical   true
              :borderless true
              :inverted   true
              :fixed      "left"}
     (when-not @iframe? [logo-item])
     (doall
       (for [{:keys [url label-kw icon protected? iframe-visble?]} @pages-list]
         (when (or (not @iframe?) iframe-visble?)
           ^{:key url}
           [item label-kw url icon protected?])))]))
