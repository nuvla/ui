(ns sixsq.nuvla.ui.deployment-dialog.views-infra-services
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.credentials.subs :as creds-subs]
    [sixsq.nuvla.ui.deployment-dialog.events :as events]
    [sixsq.nuvla.ui.deployment-dialog.subs :as subs]
    [sixsq.nuvla.ui.deployment-dialog.utils :as utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.form-fields :as ff]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]))

(defn summary-row
  []
  (let [tr                     (subscribe [::i18n-subs/tr])
        selected-infra-service (subscribe [::subs/selected-infra-service])
        selected-credential    (subscribe [::subs/selected-credential])
        completed?             (subscribe [::subs/infra-services-completed?])
        creds-completed?       (subscribe [::subs/credentials-completed?])
        on-click-fn            #(dispatch [::events/set-active-step :infra-services])]

    ^{:key "infra-services"}
    [:<>
     (let [{:keys [id name description subtype]} @selected-infra-service]
       [ui/TableRow {:active   false
                     :on-click on-click-fn}
        [ui/TableCell {:collapsing true}
         (if @completed?
           [ui/Icon {:name "cloud", :size "large"}]
           [ui/Icon {:name "warning sign", :size "large", :color "red"}])]
        [ui/TableCell {:collapsing true} (@tr [:infra-services])]
        [ui/TableCell [:div
                       [:span (or name id)]
                       [:br]
                       [:span description]
                       [:br]
                       [:span subtype]]]])
     (let [{:keys [id name description]} @selected-credential]
       [ui/TableRow {:active   false
                     :on-click on-click-fn}
        [ui/TableCell {:collapsing true}
         (if @creds-completed?
           [ui/Icon {:name "key", :size "large"}]
           [ui/Icon {:name "warning sign", :size "large", :color "red"}])]
        [ui/TableCell {:collapsing true} (@tr [:credentials])]
        [ui/TableCell [:div
                       [:span (or name id)]
                       [:br]
                       [:span description]]]])]))


(defn cred-item
  [{:keys [id name description] :as credential}]
  (let [tr                  (subscribe [::i18n-subs/tr])
        selected-credential (subscribe [::subs/selected-credential])
        status              (subscribe [::creds-subs/credential-check-status id])
        cred-valid?         (subscribe [::creds-subs/credential-check-status-valid? id])
        last-check          (subscribe [::creds-subs/credential-check-last-check id])
        selected?           (= id (:id @selected-credential))]
    [ui/ListItem (cond-> {:active selected?
                          :on-click #(dispatch [::events/set-selected-credential credential])})
     [ui/ListIcon {:vertical-align "middle"}
      [ui/IconGroup {:size "big"}
       [ui/Icon {:name "key"}]
       (when (some? @status)
         [ui/Icon {:corner true
                   :name   (if @cred-valid? "thumbs up" "thumbs down")
                   :color  (if @cred-valid? "green" "red")}])]]
     [ui/ListContent
      [ui/ListHeader (or name id)]
      (when description
        [ui/ListDescription description])
      [ui/ListDescription
       (@tr [:last-check])
       [:span
        (if @last-check
          [uix/TimeAgo @last-check]
          (@tr [:not-available]))]]]]))


(defn creds-list
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        credentials (subscribe [::subs/credentials])]
    (if (seq @credentials)
      [ui/ListSA {:divided   true
                  :relaxed   true
                  :selection true}
       (doall
         (for [{:keys [id] :as credential} @credentials]
           ^{:key id}
           [cred-item credential]))]
      [ui/Message {:error true} (@tr [:no-credentials])])))


(defn item
  [{:keys [id name subtype description] :as infra-service}]
  (let [selected-infra (subscribe [::subs/selected-infra-service])
        active?        (= (:id @selected-infra) id)
        loading?       (subscribe [::subs/credentials-loading?])]
    [:<>
     [ui/AccordionTitle {:active   active?
                         :on-click #(dispatch [::events/set-selected-infra-service
                                               infra-service])}
      [ui/Icon {:name "dropdown"}]
      (if (= subtype "kubernetes")
        [ui/Image {:src   "/ui/images/kubernetes.svg"
                   :style {:overflow       "hidden"
                           :display        "inline-block"
                           :height         28
                           :margin-right   4
                           :padding-bottom 7}}]
        [ui/Icon {:name "docker"}])
      ff/nbsp
      (or name id)]
     [ui/AccordionContent {:active active?}
      description
      [ui/Segment (assoc style/basic :loading @loading?)
       [creds-list]]]]))


(defmethod utils/step-content :infra-services
  [step-id]
  (let [tr             (subscribe [::i18n-subs/tr])
        infra-services (subscribe [::subs/visible-infra-services])
        loading?       (subscribe [::subs/infra-services-loading?])]
    [ui/Segment (assoc style/basic :loading @loading?)
     (if (seq @infra-services)
       [ui/Accordion {:fluid true, :styled true}
        (doall
          (for [{:keys [id] :as infra-service} @infra-services]
            ^{:key id}
            [item infra-service]))]
       [ui/Message {:error true} (@tr [:no-infra-services])])]))
