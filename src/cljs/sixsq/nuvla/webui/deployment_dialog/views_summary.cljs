(ns sixsq.nuvla.webui.deployment-dialog.views-summary
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.webui.deployment-dialog.subs :as subs]
    [sixsq.nuvla.webui.deployment-dialog.views-credentials :as credentials-step]
    [sixsq.nuvla.webui.deployment-dialog.views-data :as data-step]
    [sixsq.nuvla.webui.deployment-dialog.views-parameters :as parameters-step]
    [sixsq.nuvla.webui.deployment-dialog.views-size :as size-step]
    [sixsq.nuvla.webui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.webui.utils.semantic-ui :as ui]))


(defn application-row
  []
  (let [tr (subscribe [::i18n-subs/tr])
        deployment (subscribe [::subs/deployment])

        {:keys [name module]} @deployment
        header (or name (-> module :path (str/split #"/") last))
        description (:path module)]

    ^{:key "application"}
    [ui/TableRow
     [ui/TableCell {:collapsing true}
      [ui/Icon {:name "sitemap", :size "large", :vertical-align "middle"}]]
     [ui/TableCell {:collapsing true} (@tr [:application])]
     [ui/TableCell [:div
                    [:span header]
                    [:br]
                    (when description [:span description])]]]))


(defn content
  []
  (let [data-step-active? (subscribe [::subs/data-step-active?])]
    [ui/Table
     [ui/TableBody
      [application-row]
      (when @data-step-active?
        [data-step/summary-row])
      [credentials-step/summary-row]
      [size-step/summary-row]
      [parameters-step/summary-row]
      ]]))
