(ns sixsq.nuvla.ui.about.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]

    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]))


(defmethod panel/render :about
  [path]
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
      [ui/ListItem [:a {:href "https://sixsq.com/nuvla"} (str/capitalize (@tr [:product-info]))]]
      [ui/ListItem [:a {:href "https://ssdocs.sixsq.com/en/latest/tutorials/index.html"}
                    (str/capitalize (@tr [:tutorials]))]]
      [ui/ListItem [:a {:on-click #(dispatch
                                     [::history-events/navigate "documentation"])
                        :style    {:cursor "pointer"}}
                    (@tr [:api-doc])]]
      [ui/ListItem [:a {:href "https://sixsq.com/personae"} (@tr [:personae-desc])]]
      [ui/ListItem [:a {:href "https://ssdocs.sixsq.com/en/latest/release_notes/index.html"}
                    (str/capitalize (@tr [:release-notes]))]]
      [ui/ListItem [:a {:href "https://github.com/nuvla"}
                    (str/capitalize (@tr [:source-code-on]))
                    " GitHub"]]
      [ui/ListItem (str/capitalize (@tr [:core-license]))
       ": "
       [:a {:href "https://www.apache.org/licenses/LICENSE-2.0.html"}
        "Apache 2.0"]]]
     [:div
      [ui/Image {:centered true
                 :src      "/ui/images/logo_swiss_made_software.png"}]]]))
