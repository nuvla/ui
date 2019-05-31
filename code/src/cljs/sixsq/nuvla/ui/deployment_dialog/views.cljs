(ns sixsq.nuvla.ui.deployment-dialog.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.deployment-dialog.events :as events]
    [sixsq.nuvla.ui.deployment-dialog.spec :as spec]
    [sixsq.nuvla.ui.deployment-dialog.subs :as subs]
    [sixsq.nuvla.ui.deployment-dialog.views-credentials :as credentials-step]
    [sixsq.nuvla.ui.deployment-dialog.views-data :as data-step]
    [sixsq.nuvla.ui.deployment-dialog.views-env-variables :as env-variables-step]
    [sixsq.nuvla.ui.deployment-dialog.views-summary :as summary-step]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))


(defn deployment-step-state
  [{:keys [step-id completed? icon] :as step-state}]
  (let [tr                       (subscribe [::i18n-subs/tr])
        active-step              (subscribe [::subs/active-step])
        credentials-completed?   (subscribe [::subs/credentials-completed?])
        env-variables-completed? (subscribe [::subs/env-variables-completed?])
        data-completed?          (subscribe [::subs/data-completed?])]
    [ui/Step {:link      true
              :on-click  #(dispatch [::events/set-active-step step-id])
              :completed (case step-id
                           :data @data-completed?
                           :credentials @credentials-completed?
                           :environmental-variables @env-variables-completed?
                           completed?)
              :active    (= step-id @active-step)}
     [ui/Icon {:name icon}]
     [ui/StepContent
      [ui/StepTitle (@tr [step-id])]]]))


(defn step-content
  [active-step]
  (case active-step
    :data [data-step/content]
    :credentials [credentials-step/content]
    :environmental-variables [env-variables-step/content]
    :summary [summary-step/content]
    nil))


(defn deploy-modal
  [show-data?]
  (let [tr                       (subscribe [::i18n-subs/tr])
        visible?                 (subscribe [::subs/deploy-modal-visible?])
        deployment               (subscribe [::subs/deployment])
        loading?                 (subscribe [::subs/loading-deployment?])
        active-step              (subscribe [::subs/active-step])
        data-step-active?        (subscribe [::subs/data-step-active?])
        step-states              (subscribe [::subs/step-states])

        data-completed?          (subscribe [::subs/data-completed?])
        credentials-completed?   (subscribe [::subs/credentials-completed?])
        env-variables-completed? (subscribe [::subs/env-variables-completed?])]
    (fn [show-data?]
      (let [ready?           (and (not @loading?) @deployment)
            module-name      (-> @deployment :module :name)
            hide-fn          #(dispatch [::events/close-deploy-modal])
            submit-fn        #(dispatch [::events/edit-deployment])
            visible-steps    (if show-data? spec/steps (rest spec/steps))

            launch-disabled? (or (not @deployment)
                                 (and (not @data-completed?) @data-step-active?)
                                 (not @credentials-completed?)
                                 (not @env-variables-completed?))]

        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   hide-fn}

         [ui/ModalHeader
          [ui/Icon {:name "rocket", :size "large"}]
          (if ready?
            (str "\u00a0" module-name)
            "\u2026")]

         [ui/ModalContent {:scrolling true}
          [ui/ModalDescription

           (vec (concat [ui/StepGroup {:size "mini", :fluid true}]
                        (->> visible-steps
                             (map #(get @step-states %))
                             (mapv deployment-step-state))))

           [ui/Segment {:loading (not ready?)
                        :basic   true
                        :style   {:padding 0
                                  :height  "25em"}}
            (if ready?
              [step-content @active-step])]]]

         [ui/ModalActions
          [ui/Button {:primary  true
                      :disabled launch-disabled?
                      :on-click submit-fn}
           [ui/Icon {:name     "rocket"
                     :disabled launch-disabled?}]
           (@tr [:launch])]]]))))
