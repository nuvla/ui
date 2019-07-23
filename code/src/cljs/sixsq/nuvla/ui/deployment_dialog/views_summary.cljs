(ns sixsq.nuvla.ui.deployment-dialog.views-summary
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.deployment-dialog.subs :as subs]
    [sixsq.nuvla.ui.deployment-dialog.views-credentials :as credentials-step]
    [sixsq.nuvla.ui.deployment-dialog.views-data :as data-step]
    [sixsq.nuvla.ui.deployment-dialog.views-env-variables :as env-variables-step]
    [sixsq.nuvla.ui.deployment-dialog.views-files :as files-step]
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


(defn content
  []
  (let [data-step-active? (subscribe [::subs/data-step-active?])
        is-application?   (subscribe [::subs/is-application?])]
    [ui/Table
     [ui/TableBody
      [application-row]
      (when @data-step-active?
        [data-step/summary-row])
      [credentials-step/summary-row]
      (when @is-application? [files-step/summary-row])
      [env-variables-step/summary-row]]]))
