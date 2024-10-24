(ns sixsq.nuvla.ui.common-components.deployment-dialog.views-infra-services
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.common-components.deployment-dialog.events :as events]
            [sixsq.nuvla.ui.common-components.deployment-dialog.subs :as subs]
            [sixsq.nuvla.ui.common-components.deployment-dialog.utils :as utils]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.pages.clouds-detail.views :as clouds-detail]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(defn UnmetRequirements
  [{:keys [architecture cpu ram disk] :as _unmet-requirements}]
  (let [tr @(subscribe [::i18n-subs/tr])]
    [:ul {:style {:margin 0, :padding-left "20px"}}
     (when architecture [:li (tr [:edge-architecture-not-supported] [(str/join ", " (:supported architecture))
                                                                     (:edge-architecture architecture)])])
     (when cpu [:li (tr [:edge-does-not-meet-min-cpu-requirements] [(:min cpu) (:available cpu)])])
     (when ram [:li (tr [:edge-does-not-meet-min-ram-requirements] [(:min ram) (:available ram)])])
     (when disk [:li (tr [:edge-does-not-meet-min-disk-requirements] [(:min disk) (:available disk)])])]))

(defn summary-row
  []
  (let [tr                          (subscribe [::i18n-subs/tr])
        selected-infra-service      (subscribe [::subs/selected-infra-service])
        minimum-requirements        (subscribe [::subs/minimum-requirements])
        min-requirements-met?       (subscribe [::subs/app-infra-requirements-met? @selected-infra-service])
        unmet-requirements          (subscribe [::subs/app-infra-unmet-requirements @selected-infra-service])
        unmet-requirements-accepted (subscribe [::subs/unmet-requirements-accepted])
        selected-credential         (subscribe [::subs/selected-credential])
        completed?                  (subscribe [::subs/infra-services-completed?])
        creds-completed?            (subscribe [::subs/credentials-completed?])
        on-click-fn                 #(dispatch [::events/set-active-step :infra-services])]

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
     (when (and @completed? (seq @minimum-requirements))
       [ui/TableRow {:active   false
                     :on-click on-click-fn}
        [ui/TableCell {:collapsing true}
         (if (or @min-requirements-met? @unmet-requirements-accepted)
           [ui/Icon {:name "server", :size "large"}]
           [icons/WarningIcon {:size "large", :color "red"}])]
        [ui/TableCell {:collapsing true} (@tr [:minimum-requirements])]
        [ui/TableCell [:div {:style {:display :flex, :flex-direction :row}}
                       (if @min-requirements-met?
                         (@tr [:edge-meets-app-minimum-requirements])
                         [:div
                          [UnmetRequirements @unmet-requirements]
                          (when @unmet-requirements-accepted
                            [:<>
                             [:br]
                             (@tr [:accepted-to-deploy-anyway])])])]]])
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

(defn RequirementsMessage
  [min-requirements-met? unmet-requirements]
  (let [tr                          @(subscribe [::i18n-subs/tr])
        unmet-requirements-accepted (subscribe [::subs/unmet-requirements-accepted])]
    [ui/Message {:size    "tiny"
                 :info    min-requirements-met?
                 :warning (not min-requirements-met?)}
     [:div {:style {:display :flex, :flex-direction :row}}
      (if min-requirements-met? [icons/InfoIcon] [icons/WarningIcon])
      (if min-requirements-met?
        (tr [:edge-meets-app-minimum-requirements])
        [:div
         [UnmetRequirements unmet-requirements]
         [ui/Checkbox {:style     {:margin-top "10px"}
                       :label     (tr [:deploy-anyway])
                       :checked   @unmet-requirements-accepted
                       :on-change (ui-callback/checked #(dispatch [::events/accept-unmet-requirements %]))}]])]]))

(defn InfraServiceItem
  [{:keys [id name subtype description] :as infra-service}]
  (let [selected-infra        (subscribe [::subs/selected-infra-service])
        active?               (= (:id @selected-infra) id)
        loading?              (subscribe [::subs/credentials-loading?])
        module                (subscribe [::subs/module])
        compatible?           (utils/infra-app-compatible? @module infra-service)
        architectures         (subscribe [::subs/architectures])
        min-requirements      (subscribe [::subs/minimum-requirements])
        edge-architecture     (subscribe [::subs/edge-architecture infra-service])
        edge-resources        (subscribe [::subs/edge-resources infra-service])
        min-requirements-met? (utils/infra-app-min-requirements-met? @architectures @min-requirements @edge-architecture @edge-resources)
        unmet-requirements    (utils/infra-app-unmet-requirements @architectures @min-requirements @edge-architecture @edge-resources)]
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
      general-utils/nbsp
      (or name id)]
     [ui/AccordionContent {:active active?}
      description
      [ui/Segment (assoc style/basic :loading @loading?)
       [CompatibilityMessage infra-service compatible?]
       (when (seq @min-requirements)
         [RequirementsMessage min-requirements-met? unmet-requirements])
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
