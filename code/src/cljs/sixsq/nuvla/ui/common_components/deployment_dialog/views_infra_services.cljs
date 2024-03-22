(ns sixsq.nuvla.ui.common-components.deployment-dialog.views-infra-services
  (:require [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.common-components.deployment-dialog.events :as events]
            [sixsq.nuvla.ui.common-components.deployment-dialog.subs :as subs]
            [sixsq.nuvla.ui.common-components.deployment-dialog.utils :as utils]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.pages.clouds-detail.views :as clouds-detail]
            [sixsq.nuvla.ui.utils.form-fields :as ff]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.style :as style]))

(defn summary-row
  []
  (let [tr                     (subscribe [::i18n-subs/tr])
        selected-infra-service (subscribe [::subs/selected-infra-service])
        selected-credential    (subscribe [::subs/selected-credential])
        completed?             (subscribe [::subs/infra-services-completed?])
        creds-completed?       (subscribe [::subs/credentials-completed?])
        on-click-fn            #(dispatch [::events/set-active-step :infra-services])]

    ^{:key "clouds"}
    [:<>
     (let [{:keys [id name description subtype]} @selected-infra-service]
       [ui/TableRow {:active   false
                     :on-click on-click-fn}
        [ui/TableCell {:collapsing true}
         (if @completed?
           [ui/Icon {:name "map marker alternate", :size "large"}]
           [icons/WarningIcon {:size "large", :color "red"}])]
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
           [icons/KeyIcon {:size "large"}]
           [icons/WarningIcon {:size "large", :color "red"}])]
        [ui/TableCell {:collapsing true} (@tr [:credentials])]
        [ui/TableCell [:div
                       [:span (or name id)]
                       [:br]
                       [:span description]]]])]))


(defn cred-item
  [{:keys [id name description] :as credential}]
  (let [selected-credential (subscribe [::subs/selected-credential])
        selected?           (= id (:id @selected-credential))]
    [ui/ListItem (cond-> {:active   selected?
                          :on-click #(dispatch [::events/set-selected-credential credential])})
     [ui/ListIcon {:vertical-align "middle"}
      [icons/KeyIcon {:color (if selected? "blue" "black")
                      :size  "large"}]]
     [ui/ListContent
      [ui/ListHeader (when selected? {:as :a}) (or name id)]
      (when description
        [ui/ListDescription description])]]))


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

(defn CompatibilityMessage
  [infra-service compatible?]
  (let [compatibility-msg (subscribe [::subs/app-infra-compatibility-msg infra-service])]
    (when @compatibility-msg
      [ui/Message {:size    "tiny"
                   :info    compatible?
                   :warning (not compatible?)}
       (if compatible? [icons/InfoIcon] [icons/WarningIcon])
       @compatibility-msg])))

(defn InfraServiceItem
  [{:keys [id name subtype description] :as infra-service}]
  (let [selected-infra (subscribe [::subs/selected-infra-service])
        active?        (= (:id @selected-infra) id)
        loading?       (subscribe [::subs/credentials-loading?])
        module         (subscribe [::subs/module])
        compatible?    (utils/infra-app-compatible? @module infra-service)]
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
        [:<>
         [icons/DockerIcon]
         [clouds-detail/CompatibilityLabel infra-service]])
      ff/nbsp
      (or name id)]
     [ui/AccordionContent {:active active?}
      description
      [ui/Segment (assoc style/basic :loading @loading?)
       [CompatibilityMessage infra-service compatible?]
       (when compatible?
         [creds-list])]]]))


(defmethod utils/step-content :infra-services
  [_step-id]
  (let [tr             (subscribe [::i18n-subs/tr])
        infra-services (subscribe [::subs/visible-infra-services])
        loading?       (subscribe [::subs/infra-services-loading?])]
    [ui/Segment (assoc style/basic :loading @loading?)
     (if (seq @infra-services)
       [ui/Accordion {:fluid true, :styled true}
        (doall
          (for [{:keys [id] :as infra-service} @infra-services]
            ^{:key id}
            [InfraServiceItem infra-service]))]
       [ui/Message {:error true} (@tr [:no-infra-services])])]))
