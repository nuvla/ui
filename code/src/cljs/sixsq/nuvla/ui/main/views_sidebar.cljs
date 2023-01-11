(ns sixsq.nuvla.ui.main.views-sidebar
  (:require [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.events :as events]
            [sixsq.nuvla.ui.main.subs :as subs]
            [sixsq.nuvla.ui.routing.subs :as route-subs]
            [sixsq.nuvla.ui.routing.events :as history-events]
            [sixsq.nuvla.ui.routing.utils :refer [name->href]]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))

(def sidebar-width "10rem")

(defn item
  [label-kw url icon protected?]
  (let [tr           (subscribe [::i18n-subs/tr])
        is-user?     (subscribe [::session-subs/is-user?])
        active?      (subscribe [::route-subs/nav-url-active? url])
        auth-needed? (and protected? (not @is-user?))
        auth-url     (name->href :sign-in)]

    ^{:key (name label-kw)}
    [uix/MenuItem
     {:name                     (or (@tr [label-kw]) (name label-kw))
      :icon                     icon
      :style                    {:min-width  sidebar-width
                                 :overflow-x "hidden"}
      :active                   @active?
      :href                     (if auth-needed? auth-url url)
      :on-click                 (fn [event]
                                  (.preventDefault event)
                                  (dispatch (if auth-needed?
                                              [::history-events/navigate auth-url]
                                              [::events/navigate url])))
      :data-reitit-handle-click false}]))

(defn logo-item
  []
  (let [tr  (subscribe [::i18n-subs/tr])
        url (subscribe [::subs/config :nuvla-logo-url])]
    ^{:key "welcome"}
    [ui/MenuItem (cond-> {:aria-label (@tr [:welcome])
                          :style      {:overflow-x "hidden"
                                       :min-width  sidebar-width}}
                         @url (assoc :href @url))
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
     (for [{:keys [key label-kw icon protected? iframe-visble? hidden?]
            :or   {hidden? false}} pages-list]
       (when (and (or (not iframe?) iframe-visble?) (not hidden?))
         ^{:key key}
         [item label-kw (name->href key) icon protected?]))]))
