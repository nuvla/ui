(ns sixsq.nuvla.ui.deployment-dialog.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.credentials.components :as creds-comp]
    [sixsq.nuvla.ui.credentials.subs :as creds-subs]
    [sixsq.nuvla.ui.credentials.utils :as creds-utils]
    [sixsq.nuvla.ui.deployment-dialog.events :as events]
    [sixsq.nuvla.ui.deployment-dialog.subs :as subs]
    [sixsq.nuvla.ui.deployment-dialog.utils :as utils]
    [sixsq.nuvla.ui.deployment-dialog.views-data]
    [sixsq.nuvla.ui.deployment-dialog.views-env-variables]
    [sixsq.nuvla.ui.deployment-dialog.views-files]
    [sixsq.nuvla.ui.deployment-dialog.views-infra-services]
    [sixsq.nuvla.ui.deployment-dialog.views-license]
    [sixsq.nuvla.ui.deployment-dialog.views-module-version]
    [sixsq.nuvla.ui.deployment-dialog.views-price]
    [sixsq.nuvla.ui.deployment-dialog.views-registries]
    [sixsq.nuvla.ui.deployment-dialog.views-summary]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]))


(defmulti StepIcon :step-id)

(defn step-icon
  [{:keys [icon] :as step-state}]
  [ui/Icon {:name icon}])

(defmethod StepIcon :default
  [step-state]
  [step-icon step-state])


(defmethod StepIcon :infra-services
  [{:keys [step-id] :as step-state}]
  (let [cred-id                (subscribe [::subs/selected-credential-id])
        credential-loading?    (subscribe [::creds-subs/credential-check-loading? @cred-id])
        credential-valid?    (subscribe [::creds-subs/credential-check-status-valid? @cred-id])
        check-status           (creds-utils/credential-check-status
                                 @credential-loading? (not @credential-valid?))
        credentials-completed? (subscribe [::subs/credentials-completed?])]
    (dispatch [::events/set-launch-status step-id check-status])
    (or (when (and @credentials-completed? (not= :ok check-status))
          [creds-comp/CredentialCheckPopup @cred-id])
        [step-icon step-state])))


(defmethod StepIcon :module-version
  [step-state]
  (let [tr         (subscribe [::i18n-subs/tr])
        is-latest? (subscribe [::subs/is-latest-version?])
        completed? (subscribe [::subs/version-completed?])]
    (if @completed?
      [ui/Popup {:trigger  (r/as-element
                             [ui/Icon {:name  (if @is-latest? "check" "info circle")
                                       :color (if @is-latest? "green" "blue")}])
                 :content  (@tr [:new-version-exist])
                 :wide     "very"
                 :disabled @is-latest?
                 :position "top center"}]
      [step-icon step-state])))


(defmethod StepIcon :registries
  [{:keys [step-id] :as step-state}]
  (let [registries-completed? (subscribe [::subs/registries-completed?])
        registries-creds      (subscribe [::subs/registries-creds])]
    (or
      (when @registries-completed?
        (let
          [selected-reg-creds (->> (vals @registries-creds)
                                   (map (fn [{:keys [cred-id preselected?]}]
                                          (when-not preselected? cred-id)))
                                   (remove nil?))
           creds-reg-status   (map
                                (fn [cred-reg-id]
                                  (let [loading? (subscribe [::creds-subs/credential-check-loading?
                                                             cred-reg-id])
                                        invalid? (subscribe
                                                   [::creds-subs/credential-check-status-invalid?
                                                    cred-reg-id])]
                                    [cred-reg-id
                                     (creds-utils/credential-check-status @loading? @invalid?)]))
                                selected-reg-creds)
           focused-cred-reg   (or (some (fn [[_ status :as c]] (when (= status :warning) c))
                                        creds-reg-status)
                                  (some (fn [[_ status :as c]] (when (= status :loading) c))
                                        creds-reg-status)
                                  (first creds-reg-status))

           [cred-reg reg-cred-status] focused-cred-reg]

          (dispatch [::events/set-launch-status step-id (or reg-cred-status :ok)])
          (when (and cred-reg (not= :ok reg-cred-status))
            [creds-comp/CredentialCheckPopup cred-reg])))
      [step-icon step-state]
      )))


(defn deployment-step-state
  [{:keys [step-id] :as step-state}]
  (let [tr         (subscribe [::i18n-subs/tr])
        active?    (subscribe [::subs/step-active? step-id])
        completed? (subscribe [::subs/step-completed? step-id])]
    [ui/Step {:link      true
              :on-click  #(dispatch [::events/set-active-step step-id])
              :completed @completed?
              :active    @active?}
     [StepIcon step-state]
     [ui/StepContent
      [ui/StepTitle {:style {:width "10ch"}} (str/capitalize (@tr [step-id])) " "]]]))


(defn step-content-segment
  [active-step]
  [ui/Segment style/autoscroll-y
   ^{:key active-step}
   [utils/step-content active-step]])


(defn deploy-modal
  [show-data?]
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
        exec-mode        (subscribe [::subs/execution-mode])]
    (fn [show-data?]
      (let [hide-fn   #(do
                         (when (= (:state @deployment) "CREATED")
                           (dispatch [::events/delete-deployment (:id @deployment)]))
                         (dispatch [::events/reset]))
            submit-fn #(dispatch [::events/edit-deployment @operation @exec-mode])]
        [ui/Modal (cond-> {:open       @open?
                           :close-icon true
                           :on-close   hide-fn}
                          show-data? (assoc :size "large"))

         [uix/ModalHeader {:header @header-text :icon @button-icon}]
         [ui/ModalContent
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
                [step-content-segment @active-step])]
             ]]]]
         [ui/ModalActions
          [ui/Button {:color    @button-color
                      :disabled @button-disabled?
                      :on-click submit-fn}
           [ui/Icon {:name @button-icon}]
           @button-text]]]))))

