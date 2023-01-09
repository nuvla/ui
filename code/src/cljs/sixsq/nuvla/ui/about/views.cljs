(ns sixsq.nuvla.ui.about.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [subscribe]]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]))

(defn about
  [_path]
  (let [tr (subscribe [::i18n-subs/tr])]
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
      [ui/ListItem [:a {:href   "https://docs.nuvla.io"
                        :target "_blank"}
                    (str/capitalize (@tr [:documentation]))]]
      [ui/ListItem [:a {:href   "https://docs.nuvla.io/nuvla/api"
                        :target "_blank"}
                    (@tr [:api-doc])]]
      [ui/ListItem [:a {:href   "https://docs.nuvla.io/whoami"
                        :target "_blank"} (@tr [:personae-desc])]]
      [ui/ListItem [:a {:href   "https://github.com/nuvla/deployment/blob/master/CHANGELOG.md"
                        :target "_blank"}
                    (str/capitalize (@tr [:release-notes]))]]
      [ui/ListItem [:a {:href   "https://github.com/nuvla"
                        :target "_blank"}
                    (str/capitalize (@tr [:source-code-on]))
                    " GitHub"]]
      [ui/ListItem (str/capitalize (@tr [:core-license]))
       ": "
       [:a {:href   "https://www.apache.org/licenses/LICENSE-2.0.html"
            :target "_blank"}
        "Apache 2.0"]]]
     [:div
      [ui/Image {:centered true
                 :src      "/ui/images/logo_swiss_made_software.png"}]]]))
