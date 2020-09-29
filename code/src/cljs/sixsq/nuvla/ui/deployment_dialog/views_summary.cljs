(ns sixsq.nuvla.ui.deployment-dialog.views-summary
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.deployment-dialog.subs :as subs]
    [sixsq.nuvla.ui.deployment-dialog.views-data :as data-step]
    [sixsq.nuvla.ui.deployment-dialog.views-env-variables :as env-variables-step]
    [sixsq.nuvla.ui.deployment-dialog.views-files :as files-step]
    [sixsq.nuvla.ui.deployment-dialog.views-infra-services :as infra-services-step]
    [sixsq.nuvla.ui.deployment-dialog.views-registries :as registries-step]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]))


(defn application-row
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        deployment  (subscribe [::subs/deployment])

        {:keys [name module]} @deployment
        header      (or name (-> module :path (str/split #"/") last))
        description (:path module)]

    ^{:key "application"}
    [ui/TableRow
     [ui/TableCell {:collapsing true}
      [ui/Icon {:name "sitemap", :size "large"}]]
     [ui/TableCell {:collapsing true} (@tr [:application])]
     [ui/TableCell [:div
                    [:span header]
                    [:br]
                    (when description [:span description])]]]))


(defn images-dct-row
  []
  (let [tr (subscribe [::i18n-subs/tr])
        {:keys [error dct]} @(subscribe [::subs/check-dct])]

    ^{:key "images-dct-row"}
    [ui/TableRow
     [ui/TableCell {:collapsing true}
      [ui/Icon {:name "image", :size "large"}]]
     [ui/TableCell {:collapsing true} (@tr [:images-trust])]
     [ui/TableCell
      (cond
        dct (for [[image-dct-name image-dct-trust] (into (sorted-map) dct)]
              ^{:key (str "image-dct-name-" image-dct-name)}
              [:<>
               [:span (when image-dct-trust
                        {:style {:color "green"}})
                (subs (str image-dct-name) 1) " " (when image-dct-trust
                                                    [ui/Icon {:className "fas fa-medal"
                                                              :color     "green"}])]
               [:br]])
        error [:p {:style {:color "red"}} error]
        :else [ui/Icon {:name "circle notch" :loading true}])]]))


(defn content
  []
  (let [data-step-active? (subscribe [::subs/data-step-active?])
        is-application?   (subscribe [::subs/is-application?])
        registries-creds  (subscribe [::subs/registries-creds])]
    [ui/Table
     [ui/TableBody {:style {:cursor "pointer"}}
      [application-row]
      (when @data-step-active?
        [data-step/summary-row])
      [infra-services-step/summary-row]
      (when @is-application? [files-step/summary-row])
      [env-variables-step/summary-row]
      (when (seq @registries-creds) [registries-step/summary-row])
      [images-dct-row]
      ]]))
