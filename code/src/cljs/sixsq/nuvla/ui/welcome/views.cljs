(ns sixsq.nuvla.ui.welcome.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.authn.events :as authn-events]
    [sixsq.nuvla.ui.authn.subs :as authn-subs]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [taoensso.timbre :as log]))


(defn card [name-kw desc-kw icon target-resource]
  (let [tr       (subscribe [::i18n-subs/tr])
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
  (let [tr           (subscribe [::i18n-subs/tr])
        iframe?      (subscribe [::main-subs/iframe?])
        query-params (subscribe [::main-subs/nav-query-params])]
    (when @query-params
      (when (contains? @query-params :reset-password)
        (dispatch [::authn-events/set-form-id "session-template/password-reset"])
        (dispatch [::authn-events/open-modal :reset-password]))
      (dispatch [::history-events/navigate (str (first path) "/")]))
    [ui/Container {:textAlign "center"
                   :fluid     true
                   :class     "nuvla-ui-welcome-background"}

     [ui/Header {:as "h1"}
      (str/capitalize (@tr [:welcome]))]

     [ui/HeaderSubheader {:as "h3"}
      (@tr [:welcome-detail])]

     [ui/CardGroup {:centered true}
      (when-not @iframe? [card :application :welcome-application-desc "play" "apps"])
      [card :data :welcome-data-desc "database" "data"]
      [card :deployment :welcome-deployment-desc "cloud" "deployment"]
      (when-not @iframe? [card :nuvlabox-ctrl :welcome-nuvlabox-desc "desktop" "nuvlabox"])
      (when-not @iframe? [card :api :welcome-api-desc "code" "api"])
      ]]))
