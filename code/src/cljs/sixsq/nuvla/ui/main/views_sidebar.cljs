(ns sixsq.nuvla.ui.main.views-sidebar
  (:require [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.about.subs :as about-subs]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.events :as events]
            [sixsq.nuvla.ui.main.subs :as subs]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.subs :as route-subs]
            [sixsq.nuvla.ui.routing.utils :refer [name->href]]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))

(def sidebar-width "10rem")

(defn Item
  [{:keys [key label-kw icon protected? route-names iframe-visible? feature-flag-kw]}]
  (let [tr           (subscribe [::i18n-subs/tr])
        iframe?      (subscribe [::subs/iframe?])
        is-user?     (subscribe [::session-subs/is-user?])
        url          (name->href key)
        active?      (subscribe [::route-subs/nav-url-active? (or route-names key)])
        auth-needed? (and protected? (not @is-user?))
        auth-url     (name->href routes/sign-in)
        href         (if auth-needed? auth-url url)
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
                                    (dispatch (if auth-needed?
                                                [::routing-events/navigate auth-url]
                                                [::events/navigate url])))
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
  (let [show?      @(subscribe [::subs/sidebar-open?])
        iframe?    @(subscribe [::subs/iframe?])
        pages-list @(subscribe [::subs/pages-list])]
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
       [Item page])]))
