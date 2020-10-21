(ns sixsq.nuvla.ui.deployment-dialog.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.credentials.components :as creds-comp]
    [sixsq.nuvla.ui.credentials.subs :as creds-subs]
    [sixsq.nuvla.ui.credentials.utils :as creds-utils]
    [sixsq.nuvla.ui.deployment-dialog.events :as events]
    [sixsq.nuvla.ui.deployment-dialog.subs :as subs]
    [sixsq.nuvla.ui.deployment-dialog.views-data :as data-step]
    [sixsq.nuvla.ui.deployment-dialog.views-env-variables :as env-variables-step]
    [sixsq.nuvla.ui.deployment-dialog.views-files :as files-step]
    [sixsq.nuvla.ui.deployment-dialog.views-infra-services :as infra-services-step]
    [sixsq.nuvla.ui.deployment-dialog.views-license :as license-step]
    [sixsq.nuvla.ui.deployment-dialog.views-price :as price-step]
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
        registries-creds         (subscribe [::subs/registries-creds])
        license-completed?       (subscribe [::subs/license-completed?])
        price-completed?         (subscribe [::subs/price-completed?])
        credential-loading?      (subscribe [::creds-subs/credential-check-loading? @cred-id])
        credential-invalid?      (subscribe [::creds-subs/credential-check-status-invalid? @cred-id])
        registries-status        (subscribe [::subs/launch-status-registries :registries])]

    [ui/Step {:link      true
              :on-click  #(dispatch [::events/set-active-step step-id])
              :completed (case step-id
                           :data @data-completed?
                           :infra-services (and @credentials-completed? (not @credential-loading?) (not @credential-invalid?))
                           :env-variables @env-variables-completed?
                           :registries (and @registries-completed? (= :ok @registries-status))
                           :license @license-completed?
                           :pricing @price-completed?
                           completed?)
              :active    (= step-id @active-step)}
     (or
       (case step-id
         :infra-services (do
                           (dispatch [::events/set-launch-status
                                      step-id
                                      (creds-utils/credential-check-status @credential-loading? @credential-invalid?)])
                           (when @credentials-completed? [creds-comp/CredentialCheckPopup @cred-id]))

         :registries (when @registries-completed?
                       (let [selected-reg-creds (->> (vals @registries-creds)
                                                   (map (fn [{:keys [cred-id preselected?]}]
                                                          (when-not preselected? cred-id)))
                                                   (remove nil?))
                           creds-reg-status (map
                                              (fn [cred-reg-id]
                                                (let [loading? (subscribe [::creds-subs/credential-check-loading? cred-reg-id])
                                                      invalid? (subscribe [::creds-subs/credential-check-status-invalid? cred-reg-id])]
                                                  [cred-reg-id (creds-utils/credential-check-status @loading? @invalid?)])) selected-reg-creds)
                           focused-cred-reg (or (some (fn [[_ status :as c]] (when (= status :warning) c)) creds-reg-status)
                                                (some (fn [[_ status :as c]] (when (= status :loading) c)) creds-reg-status)
                                                (first creds-reg-status))

                           [cred-reg reg-cred-status] focused-cred-reg]

                       (dispatch [::events/set-launch-status step-id (or reg-cred-status :ok)])
                       (when cred-reg [creds-comp/CredentialCheckPopup cred-reg])))
         nil)
       [ui/Icon {:name icon}])


     [ui/StepContent
      [ui/StepTitle {:style {:width "10ch"}} (@tr [step-id]) " "
       ]]]))


(defn step-content
  [active-step]
  [ui/Segment style/autoscroll-y
   (case active-step
     :data [data-step/content]
     :infra-services [infra-services-step/content]
     :registries [registries-step/content]
     :env-variables [env-variables-step/content]
     :files [files-step/content]
     :license [license-step/content]
     :pricing [price-step/content]
     :summary [summary-step/content]
     nil)])

(defn need-force-launch?
  []
  (let [launch-disabled?       (subscribe [::subs/launch-disabled?])
        cred-id                (subscribe [::subs/selected-credential-id])
        credentials-completed? (subscribe [::subs/credentials-completed?])
        credential-invalid?    (subscribe [::creds-subs/credential-check-status-invalid? @cred-id])]
    (boolean (or @launch-disabled? (and @credentials-completed? (not @credential-invalid?))))))

(defn deploy-modal
  [show-data?]
  (let [tr               (subscribe [::i18n-subs/tr])
        visible?         (subscribe [::subs/deploy-modal-visible?])
        deployment       (subscribe [::subs/deployment])
        registries-creds (subscribe [::subs/registries-creds])
        env-variables    (subscribe [::subs/env-variables])
        ready?           (subscribe [::subs/ready?])
        launch-disabled? (subscribe [::subs/launch-disabled?])
        launch-status    (subscribe [::subs/launch-status])
        active-step      (subscribe [::subs/active-step])
        step-states      (subscribe [::subs/step-states])
        license          (subscribe [::subs/license])
        price            (subscribe [::subs/price])
        files            (subscribe [::subs/files])
        error            (subscribe [::subs/error-message])]
    (fn [show-data?]
      (let [module         (:module @deployment)
            module-name    (:name module)
            module-subtype (:subtype module)
            hide-fn        #(do
                              (when (= (:state @deployment) "CREATED")
                                (dispatch [::events/delete-deployment (:id @deployment)]))
                              (dispatch [::events/reset]))
            submit-fn      #(dispatch [::events/edit-deployment])

            steps          [(when show-data? :data)
                            :infra-services
                            (when (seq @registries-creds) :registries)
                            (when (seq @env-variables) :env-variables)
                            (when (and (= module-subtype "application")
                                       (seq @files)) :files)
                            (when @license :license)
                            (when @price :pricing)
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
          (when @error
            [ui/Message {:error true}
             [ui/MessageHeader (:title @error)]
             (:content @error)])
          [ui/ModalDescription
           [ui/Grid {:columns 2, :stackable true}
            [ui/GridColumn {:width 4}
             [ui/StepGroup {:size "mini", :fluid true, :vertical true}
              (doall
                (for [step-id visible-steps]
                  ^{:key step-id}
                  [deployment-step-state (get @step-states step-id)]))]]
            [ui/GridColumn {:width 12}
             [ui/Segment {:loading (not ready?)
                          :basic   true
                          :style   {:padding 0
                                    :height  "25em"}}
              (when @ready?
                [step-content @active-step])]
             ]]]]
         [ui/ModalActions
          [ui/Button {:color    (if (= @launch-status :warning) "yellow" "blue")
                      :disabled @launch-disabled?
                      :on-click submit-fn}
           [ui/Icon {:name     "rocket"
                     :disabled @launch-disabled?}]
           (@tr (if (not= @launch-status :ok)
                  [:launch-force]
                  [:launch]))
           ]]]))))

