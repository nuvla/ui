(ns sixsq.nuvla.ui.deployment-dialog.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.credentials.components :as creds-comp]
    [sixsq.nuvla.ui.credentials.subs :as creds-subs]
    [sixsq.nuvla.ui.deployment-dialog.events :as events]
    [sixsq.nuvla.ui.deployment-dialog.subs :as subs]
    [sixsq.nuvla.ui.deployment-dialog.views-data :as data-step]
    [sixsq.nuvla.ui.deployment-dialog.views-env-variables :as env-variables-step]
    [sixsq.nuvla.ui.deployment-dialog.views-files :as files-step]
    [sixsq.nuvla.ui.deployment-dialog.views-infra-services :as infra-services-step]
    [sixsq.nuvla.ui.deployment-dialog.views-registries :as registries-step]
    [sixsq.nuvla.ui.deployment-dialog.views-summary :as summary-step]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.style :as style]))


(defn deployment-step-state
  [{:keys [step-id completed? icon] :as step-state}]
  (let [tr                       (subscribe [::i18n-subs/tr])
        active-step              (subscribe [::subs/active-step])
        credentials-completed?   (subscribe [::subs/credentials-completed?])
        env-variables-completed? (subscribe [::subs/env-variables-completed?])
        data-completed?          (subscribe [::subs/data-completed?])
        registries-completed?    (subscribe [::subs/registries-completed?])
        cred-id                  (subscribe [::subs/selected-credential-id])
        infra-registries-creds   (subscribe [::subs/infra-registries-creds])]
    [ui/Step {:link      true
              :on-click  #(dispatch [::events/set-active-step step-id])
              :completed (case step-id
                           :data @data-completed?
                           :infra-services @credentials-completed?
                           :env-variables @env-variables-completed?
                           :registries @registries-completed?
                           completed?)
              :active    (= step-id @active-step)}
     [ui/Icon {:name icon}]
     [ui/StepContent
      [ui/StepTitle (@tr [step-id]) " "
       (case step-id
         :infra-services
         (when @credentials-completed?
           [creds-comp/CredentialCheckPopup @cred-id])
         :registries
         (when @registries-completed?
           (let [registries-creds (map first (vals @infra-registries-creds))
                 focused-cred-reg (or
                                    (some
                                      (fn [{cred-reg-id :id}]
                                        (when (or
                                                @(subscribe
                                                   [::creds-subs/credential-check-loading?
                                                    cred-reg-id])
                                                @(subscribe
                                                   [::creds-subs/credential-check-status-invalid?
                                                    cred-reg-id]))
                                          cred-reg-id)) registries-creds)
                                    (:id (first registries-creds)))]

             [creds-comp/CredentialCheckPopup focused-cred-reg]))
         nil)]]]))


(defn step-content
  [active-step]
  [ui/Segment style/autoscroll-y
   (case active-step
     :data [data-step/content]
     :infra-services [infra-services-step/content]
     :registries [registries-step/content]
     :env-variables [env-variables-step/content]
     :files [files-step/content]
     :summary [summary-step/content]
     nil)])


(defn deploy-modal
  [show-data?]
  (let [tr                 (subscribe [::i18n-subs/tr])
        visible?           (subscribe [::subs/deploy-modal-visible?])
        deployment         (subscribe [::subs/deployment])
        private-registries (subscribe [::subs/private-registries])
        ready?             (subscribe [::subs/ready?])
        launch-disabled?   (subscribe [::subs/launch-disabled?])
        active-step        (subscribe [::subs/active-step])
        step-states        (subscribe [::subs/step-states])]
    (fn [show-data?]
      (let [module         (:module @deployment)
            module-name    (:name module)
            module-subtype (:subtype module)
            hide-fn        #(dispatch [::events/close-deploy-modal])
            submit-fn      #(dispatch [::events/edit-deployment])

            steps          [(when show-data? :data)
                            :infra-services
                            (when (some? @private-registries) :registries)
                            :env-variables
                            (when (= module-subtype "application") :files)
                            :summary]
            visible-steps  (remove nil? steps)]
        [ui/Modal (cond-> {:open       @visible?
                           :close-icon true
                           :on-close   hide-fn}
                          show-data? (assoc :size "large"))

         [ui/ModalHeader
          [ui/Icon {:name "rocket", :size "large"}]
          (if @ready?
            (str "\u00a0" module-name)
            "\u2026")]

         [ui/ModalContent
          [ui/ModalDescription

           [ui/StepGroup {:size "mini", :fluid true}
            (doall
              (for [step-id visible-steps]
                ^{:key step-id}
                [deployment-step-state (get @step-states step-id)]))]

           [ui/Segment {:loading (not ready?)
                        :basic   true
                        :style   {:padding 0
                                  :height  "25em"}}
            (when @ready?
              [step-content @active-step])]]]

         [ui/ModalActions

          [ui/Button {:primary  true
                      :disabled @launch-disabled?
                      :on-click submit-fn}
           [ui/Icon {:name     "rocket"
                     :disabled @launch-disabled?}]
           (@tr [:launch])]]]))))
