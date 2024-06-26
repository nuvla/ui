(ns sixsq.nuvla.ui.common-components.deployment-dialog.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.deployment-dialog.events :as events]
            [sixsq.nuvla.ui.common-components.deployment-dialog.subs :as subs]
            [sixsq.nuvla.ui.common-components.deployment-dialog.utils :as utils]
            [sixsq.nuvla.ui.common-components.deployment-dialog.views-data]
            [sixsq.nuvla.ui.common-components.deployment-dialog.views-env-variables]
            [sixsq.nuvla.ui.common-components.deployment-dialog.views-files]
            [sixsq.nuvla.ui.common-components.deployment-dialog.views-infra-services]
            [sixsq.nuvla.ui.common-components.deployment-dialog.views-license]
            [sixsq.nuvla.ui.common-components.deployment-dialog.views-module-version]
            [sixsq.nuvla.ui.common-components.deployment-dialog.views-price]
            [sixsq.nuvla.ui.common-components.deployment-dialog.views-registries]
            [sixsq.nuvla.ui.common-components.deployment-dialog.views-summary]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]))


(defmulti StepIcon :step-id)

(defn step-icon
  [{:keys [icon] :as _step-state}]
  [ui/Icon {:name icon}])

(defmethod StepIcon :default
  [step-state]
  [step-icon step-state])

(defmethod StepIcon :module-version
  [step-state]
  (let [tr                   (subscribe [::i18n-subs/tr])
        is-latest?           (subscribe [::subs/is-latest-version?])
        is-latest-published? (subscribe [::subs/is-latest-published-version?])
        is-module-published? (subscribe [::subs/is-module-published?])
        completed?           (subscribe [::subs/version-completed?])
        is-ok?               (if @is-module-published? @is-latest-published? @is-latest?)]
    (if @completed?
      [ui/Popup {:trigger  (r/as-element
                             [ui/Icon {:class (if is-ok? icons/i-check-full icons/i-info-full)
                                       :color (if is-ok? "green" "blue")}])
                 :content  (@tr [:new-version-exist])
                 :wide     "very"
                 :disabled is-ok?
                 :position "top center"}]
      [step-icon step-state])))


(defn deployment-step-state
  [{:keys [step-id step-title] :as step-state}]
  (let [tr         (subscribe [::i18n-subs/tr])
        active?    (subscribe [::subs/step-active? step-id])
        completed? (subscribe [::subs/step-completed? step-id])]
    [ui/Step {:link      true
              :on-click  #(dispatch [::events/set-active-step step-id])
              :completed @completed?
              :active    @active?}
     [StepIcon step-state]
     [ui/StepContent
      [ui/StepTitle {:style {:width "10ch"}} (or (@tr [step-title])
                                                 (str/capitalize (@tr [step-id]))) " "]]]))


(defn step-content-segment
  [active-step]
  (let [style (cond-> style/autoscroll-y
                (= active-step :module-version)
                (update-in [:style] dissoc :overflow-y))]
    [ui/Segment style
     ^{:key active-step}
     [utils/step-content active-step]]))


(defn deploy-modal
  [_show-data?]
  (let [open?            (subscribe [::subs/deploy-modal-visible?])
        deployment       (subscribe [::subs/deployment])
        loading?         (subscribe [::subs/loading-deployment?])
        active-step      (subscribe [::subs/active-step])
        step-states      (subscribe [::subs/step-states])
        error            (subscribe [::subs/error-message])
        visible-steps    (subscribe [::subs/visible-steps])
        header-text      (subscribe [::subs/modal-header-text])
        button-icon      (subscribe [::subs/modal-action-button-icon])
        button-text      (subscribe [::subs/modal-action-button-text])
        button-color     (subscribe [::subs/modal-action-button-color])
        button-disabled? (subscribe [::subs/modal-action-button-disabled?])
        operation        (subscribe [::subs/modal-operation])
        submit-loading?  (subscribe [::subs/submit-loading?])
        tr               (subscribe [::i18n-subs/tr])]
    (fn [show-data?]
      (let [hide-fn   #(do
                         (when (= (:state @deployment) "CREATED")
                           (dispatch [::events/delete-deployment (:id @deployment)]))
                         (dispatch [::events/reset]))
            submit-fn #(dispatch [::events/edit-deployment @operation])]
        [ui/Modal (cond-> {:open       @open?
                           :close-icon true
                           :on-close   hide-fn}
                          show-data? (assoc :size "large"))

         [uix/ModalHeader {:header @header-text :icon @button-icon}]
         [ui/ModalContent
          [ui/Message {:info true
                       :size "small"}
           [:p (@tr [:create-deployment-group-with-app-message])  [:br]
            [:a {:style {:cursor "pointer"
                         :font-weight "bold"}
                 :on-click #(dispatch [::events/create-deployment-group-from-modal @deployment ])}
             (@tr [:check-it-out])]]]
          (when @error
            [ui/Message {:error true}
             [ui/MessageHeader (:title @error)]
             (:content @error)])
          [ui/ModalDescription
           [ui/Grid {:columns 2, :stackable true}
            [ui/GridColumn {:width 4}
             [ui/StepGroup {:size "mini", :fluid true, :vertical true}
              (doall
                (for [step-id @visible-steps]
                  ^{:key step-id}
                  [deployment-step-state (get @step-states step-id)]))]]
            [ui/GridColumn {:width 12}
             [ui/Segment {:loading @loading?
                          :basic   true
                          :style   {:padding 0
                                    :height  "25em"}}
              (when-not @loading?
                [step-content-segment @active-step])]]]]]
         [ui/ModalActions
          [ui/Button {:color    @button-color
                      :disabled @button-disabled?
                      :loading  @submit-loading?
                      :on-click submit-fn}
           [ui/Icon {:class @button-icon}]
           @button-text]]]))))
