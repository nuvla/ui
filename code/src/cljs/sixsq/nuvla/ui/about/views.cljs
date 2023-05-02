(ns sixsq.nuvla.ui.about.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.about.events :as about-events]
            [sixsq.nuvla.ui.about.subs :as subs]
            [sixsq.nuvla.ui.about.utils :as utils]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(defn FeatureFlag
  [{:keys [k label]}]
  [ui/FormCheckbox
   {:label     label
    :toggle    true
    :checked   @(subscribe [::subs/feature-flag-enabled? k])
    :on-change (ui-callback/checked
                 #(dispatch [::about-events/set-feature-flag k %]))}])

(defn ExperimentalFeatures
  []
  (let [tr        @(subscribe [::i18n-subs/tr])
        icon-code "\uD83D\uDEA7"]
    [ui/Modal
     {:trigger    (r/as-element
                    [:a (str (tr [:experimental-features]) " " icon-code)])
      :close-icon true}
     [ui/Header {:icon true}
      [:<> [ui/Icon icon-code]
       (tr [:experimental-features])]
      [ui/HeaderSubheader
       (tr [:experimental-features-warn])]]
     [ui/ModalContent
      [ui/Form
       (for [feature-flag utils/feature-flags]
         ^{:key (:k feature-flag)}
         [FeatureFlag feature-flag])]]]))

(defn About
  [_path]
  (let [tr    (subscribe [::i18n-subs/tr])
        terms (subscribe [::main-subs/config :terms-and-conditions])]
    [ui/Container
     [ui/Header {:as        "h1"
                 :textAlign "center"}
      (str/capitalize (@tr [:about]))]
     [:div
      [ui/Image {:centered true
                 :size     :large
                 :src      "/ui/images/nuvla_logo_red_on_transparent_1000px.png"}]]
     [ui/Header {:as        "h3"
                 :textAlign "center"}
      (@tr [:about-subtitle])]
     [:div
      [ui/Image {:centered true
                 :size     :small
                 :src      "/ui/images/by_sixsq_mark_red_on_transparent_128px.png"}]]
     [ui/ListSA
      [ui/ListItem (@tr [:version-number]) ": " [:span#release-version "v"]]
      [ui/ListItem [:a {:href   "https://sixsq.com/nuvla"
                        :target "_blank"}
                    (str/capitalize (@tr [:product-info]))]]
      (when @terms
        [ui/ListItem [:a {:href   @terms
                          :target "_blank"}
                      (str/capitalize (@tr [:terms-and-conditions]))]])
      [ui/ListItem [:a {:href   "https://docs.nuvla.io"
                        :target "_blank"}
                    (str/capitalize (@tr [:documentation]))]]
      [ui/ListItem [:a {:href   "https://docs.nuvla.io/nuvla/api"
                        :target "_blank"}
                    (@tr [:api-doc])]]
      [ui/ListItem [:a {:href   "https://github.com/nuvla/nuvla#latest-releases-and-artifacts"
                        :target "_blank"}
                    (str/capitalize (@tr [:software-versions]))]]
      [ui/ListItem [:a {:href   "https://github.com/nuvla"
                        :target "_blank"}
                    (str/capitalize (@tr [:source-code-on]))
                    " GitHub"]]
      [ui/ListItem [ExperimentalFeatures]]
      [ui/ListItem (str/capitalize (@tr [:core-license]))
       ": "
       [:a {:href   "https://www.apache.org/licenses/LICENSE-2.0.html"
            :target "_blank"}
        "Apache 2.0"]]]
     [:div
      [ui/Image {:centered true
                 :src      "/ui/images/logo_swiss_made_software.png"}]]]))
