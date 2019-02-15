(ns sixsq.nuvla.ui.welcome.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.authn.subs :as authn-subs]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]))


(defn card [name-kw desc-kw icon target-resource]
  (let [tr (subscribe [::i18n-subs/tr])
        is-user? (subscribe [::authn-subs/is-user?])]
    [ui/Card
     [ui/CardContent {:as "h1"}
      [ui/CardHeader [ui/Header {:as "h1"}
                      [ui/Icon {:name icon}]]]
      [ui/CardDescription (@tr [desc-kw])]]
     [ui/Button {:fluid    true
                 :primary  true
                 :disabled (not @is-user?)
                 :on-click #(dispatch [::history-events/navigate target-resource])}
      (str/capitalize (@tr [name-kw]))]]))


(defmethod panel/render :welcome
  [path]
  (let [tr (subscribe [::i18n-subs/tr])
        iframe? (subscribe [::main-subs/iframe?])]
    [ui/Container {:textAlign "center"
                   :fluid     true
                   :class     "nuvla-ui-welcome-background"}

     [ui/Header {:as "h1"}
      (str/capitalize (@tr [:welcome]))]

     [ui/HeaderSubheader {:as "h3"}
      (@tr [:welcome-detail])]

     [ui/CardGroup {:centered true}
      (when-not @iframe? [card :appstore :welcome-appstore-desc "play" "appstore"])
      (when-not @iframe? [card :application :welcome-application-desc "sitemap" "application"])
      [card :data :welcome-data-desc "database" "data"]
      [card :deployment :welcome-deployment-desc "cloud" "deployment"]
      (when-not @iframe? [card :nuvlabox-ctrl :welcome-nuvlabox-desc "desktop" "nuvlabox"])
      (when-not @iframe? [card :api :welcome-api-desc "code" "api"])
      ]]))
