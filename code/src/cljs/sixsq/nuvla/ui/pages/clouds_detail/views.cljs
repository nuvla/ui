(ns sixsq.nuvla.ui.pages.clouds-detail.views
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.acl.views :as acl]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.components :as main-components]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.main.subs :as main-subs]
            [sixsq.nuvla.ui.pages.cimi-detail.views :as cimi-detail-views]
            [sixsq.nuvla.ui.pages.clouds-detail.events :as events]
            [sixsq.nuvla.ui.pages.clouds-detail.spec :as spec]
            [sixsq.nuvla.ui.pages.clouds-detail.subs :as subs]
            [sixsq.nuvla.ui.pages.clouds.utils :as utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.view-components :refer [OnlineStatusIcon]]))


(defn refresh
  [uuid]
  (dispatch [::events/get-infrastructure-service (str "infrastructure-service/" uuid)])
  (dispatch [::main-events/changes-protection? false]))


(def form-valid? (r/atom true))


(def validate-form? (r/atom false))


(defn validate
  [form-spec data callback]
  (let [valid? (s/valid? form-spec data)]
    (when-not valid? (s/explain form-spec data))
    (callback valid?)))


(defn CompatibilityLabel
  [infra-service]
  (let [{:keys [popup-txt label-txt label-icon label-color]
         :or   {label-color "blue"}
         } (cond
             (utils/swarm-manager? infra-service)
             {:popup-txt  "Swarm Manager"
              :label-txt  "Swarm"
              :label-icon icons/i-crown}

             (utils/swarm-worker? infra-service)
             {:popup-txt  "Swarm Worker"
              :label-txt  "Swarm"
              :label-icon icons/i-robot})]
    (when label-txt
      [ui/Popup
       {:size    "tiny"
        :content popup-txt
        :trigger (r/as-element
                   [ui/Label {:circular true
                              :color    label-color
                              :size     "tiny"
                              :basic    true
                              :style    {:float "right"}}
                    (when label-icon
                      [icons/Icon {:name label-icon}])
                    label-txt])}])))


(defn DeleteButton
  [infra-service]
  (let [tr      (subscribe [::i18n-subs/tr])
        {:keys [id name description]} @infra-service
        content (str (or name id) (when description " - ") description)]
    [uix/ModalDanger
     {:button-text (@tr [:delete])
      :on-confirm  #(dispatch [::events/delete])
      :danger-msg  (@tr [:infrastructure-delete-warning])
      :trigger     (r/as-element [ui/MenuItem
                                  [icons/TrashIconFull]
                                  (str/capitalize (@tr [:delete]))])
      :header      (@tr [:delete-infrastructure])
      :content     [:h3 content]}]))


(defn StopButton
  [infra-service]
  (let [tr      (subscribe [::i18n-subs/tr])
        {:keys [id name description]} @infra-service
        content (str (or name id) (when description " - ") description)]
    [uix/ModalDanger
     {:button-text (@tr [:stop])
      :on-confirm  #(dispatch [::events/operation "stop"])
      :danger-msg  (@tr [:infrastructure-stop-warning])
      :trigger     (r/as-element [ui/MenuItem
                                  [icons/StopIconFull]
                                  (@tr [:stop])])
      :header      (@tr [:stop-infrastructure])
      :content     [:h3 content]}]))


(defn StartButton
  [infra-service]
  (let [tr      (subscribe [::i18n-subs/tr])
        {:keys [id name description]} @infra-service
        content (str (or name id) (when description " - ") description)]
    [uix/ModalDanger
     {:button-text (@tr [:start])
      :on-confirm  #(dispatch [::events/operation "start"])
      :danger-msg  (@tr [:infrastructure-start-warning])
      :trigger     (r/as-element [ui/MenuItem
                                  [icons/RocketIcon]
                                  (@tr [:start])])
      :header      (@tr [:start-infrastructure])
      :content     [:h3 content]}]))


(defn MenuBar [uuid]
  (let [can-delete?    (subscribe [::subs/can-delete?])
        can-start?     (subscribe [::subs/can-start?])
        can-stop?      (subscribe [::subs/can-stop?])
        infra-service  (subscribe [::subs/infrastructure-service])]
    [main-components/StickyBar
     [ui/Menu {:borderless true}
      (when @can-delete?
        [DeleteButton infra-service])

      (when @can-stop?
        [StopButton infra-service])

      (when @can-start?
        [StartButton infra-service])

      [main-components/RefreshMenu
       {:on-refresh #(refresh uuid)}]]]))


(defn InfraService
  []
  (let [tr            (subscribe [::i18n-subs/tr])

        infra-service (subscribe [::subs/infrastructure-service])
        can-edit?     (subscribe [::subs/can-edit?])
        changes?      (subscribe [::main-subs/changes-protection?])
        on-change     (fn [key value]
                        (let [update-infra-service (assoc @infra-service key value)]
                          (dispatch [::events/set-infrastructure-service update-infra-service])
                          (dispatch [::main-events/changes-protection? true])
                          (when @validate-form?
                            (validate ::spec/infrastructure-service update-infra-service
                                      #(reset! form-valid? %)))))]
    (fn []
      (let [{id          :id
             name        :name
             description :description
             endpoint    :endpoint} @infra-service]
        [:<>
         ^{:key id}
         [acl/AclButton {:default-value (:acl @infra-service)
                         :read-only     (not @can-edit?)
                         :on-change     (partial on-change :acl)}]

         [ui/Message {:hidden @form-valid?
                      :error  true}
          [ui/MessageHeader (@tr [:validation-error])]
          [ui/MessageContent (@tr [:validation-error-message])]]

         [ui/Table {:compact    true
                    :definition true}
          [ui/TableBody

           [uix/TableRowField (@tr [:name]), :key (str id "-name"), :editable? @can-edit?,
            :spec ::spec/name, :validate-form? @validate-form?, :required? true,
            :default-value name, :on-change (partial on-change :name)]

           [uix/TableRowField (@tr [:description]), :key (str id "-description"),
            :editable? @can-edit?, :spec ::spec/description, :validate-form? @validate-form?,
            :required? true, :default-value description,
            :on-change (partial on-change :description)]

           [uix/TableRowField (@tr [:endpoint]), :key (str id "-subtype"),
            :editable? @can-edit?, :spec ::spec/endpoint, :validate-form? @validate-form?,
            :required? true, :default-value endpoint,
            :on-change (partial on-change :endpoint)]]]
         (when @can-edit?
           [uix/Button {:text     (@tr [:save])
                        :primary  true
                        :disabled (not @changes?)
                        :on-click (fn []
                                    (reset! validate-form? true)
                                    (validate ::spec/infrastructure-service @infra-service
                                              #(reset! form-valid? %))
                                    (when @form-valid?
                                      (dispatch [::events/edit-infrastructure-service])))}])]))))


(defn PageHeader
  []
  (let [infra-service (subscribe [::subs/infrastructure-service])]
    (fn []
      (let [{:keys [state name id]} @infra-service]
        [:div
         [:h2 {:style {:margin "0 0 0 0"}}
          [icons/CloudIcon]
          (or name id)]
         [:p {:style {:margin "0.5em 0 1em 0"}}
          [OnlineStatusIcon state]
          [CompatibilityLabel @infra-service]
          [:span {:style {:font-weight "bold"}}
           "State: "]
          state]]))))


(defn InfrastructureDetails
  [uuid]
  (let [infra-service (subscribe [::subs/infrastructure-service])]
    (refresh uuid)
    (fn [uuid]
      [ui/DimmerDimmable {:style {:overflow "visible"}}
       [main-components/NotFoundPortal
        ::subs/infra-service-not-found?
        :no-infra-service-message-header
        :no-infra-service-message-content]
       ^{:key uuid}
       [ui/Container {:fluid true}
        [PageHeader]
        [MenuBar uuid]
        [cimi-detail-views/detail-header @infra-service]
        ^{:key uuid}
        [InfraService]]])))
