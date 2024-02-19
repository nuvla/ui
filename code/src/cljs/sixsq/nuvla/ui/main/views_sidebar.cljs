(ns sixsq.nuvla.ui.main.views-sidebar
  (:require [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.pages.about.subs :as about-subs]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.events :as events]
            [sixsq.nuvla.ui.main.subs :as subs]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.subs :as route-subs]
            [sixsq.nuvla.ui.routing.utils :refer [name->href]]
            [sixsq.nuvla.ui.pages.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))

(def sidebar-width "10rem")

(defn Item
  [{:keys [key label-kw icon route-names iframe-visible? feature-flag-kw]}]
  (let [tr       (subscribe [::i18n-subs/tr])
        iframe?  (subscribe [::subs/iframe?])
        href     (name->href key)
        active?  (subscribe [::route-subs/nav-url-active? (or route-names key) href])
        visible? (and (or (not @iframe?) iframe-visible?)
                      (or (nil? feature-flag-kw)
                          @(subscribe [::about-subs/feature-flag-enabled?
                                       feature-flag-kw])))]
    (when visible?
      [uix/MenuItem
       {:name                     (or (@tr [label-kw]) (name label-kw))
        :icon                     icon
        :class                    (str "nuvla-" (name key))
        :style                    {:min-width  sidebar-width
                                   :overflow-x "hidden"}
        :active                   @active?
        :href                     href
        :on-click                 (fn [event]
                                    (.preventDefault event)
                                    (dispatch [::events/navigate href]))
        :data-reitit-handle-click false}])))

(defn logo-item
  []
  (let [tr  (subscribe [::i18n-subs/tr])
        url (subscribe [::subs/config :nuvla-logo-url])]
    ^{:key "welcome"}
    [ui/MenuItem (cond-> {:aria-label (@tr [:welcome])
                          :style      {:overflow-x "hidden"
                                       :min-width  sidebar-width
                                       :padding    "0.5rem 0.5rem 0.2rem 0.5rem"}}
                         @url (assoc :href @url))
     [ui/Image {:alt      "logo"
                :src      "/ui/images/nuvla-logo.png"
                :size     "tiny"
                :style    {:margin-top    "10px"
                           :margin-bottom "10px"}
                :centered true}]]))

(defn Menu
  "Provides the sidebar menu for selecting major components/panels of the
   application."
  []
  (let [show?            @(subscribe [::subs/sidebar-open?])
        iframe?          @(subscribe [::subs/iframe?])
        is-small-device? @(subscribe [::subs/is-small-device?])
        pages-list       @(subscribe [::subs/pages-list])]
    [:<>
     [ui/Dimmer {:active   (and is-small-device? show?)
                 :inverted true
                 :style    {:z-index 999}
                 :on-click #(dispatch [::events/close-sidebar])}]
     [ui/Menu {:id         "nuvla-ui-sidebar"
               :style      {:transition "0.5s"
                            :width      (if show? sidebar-width "0")}
               :vertical   true
               :icon       "labeled"
               :borderless true
               :inverted   true
               :fixed      "left"}
      (when-not iframe? [logo-item])
      (for [page pages-list]
        ^{:key (:key page)}
        [Item page])]]))
