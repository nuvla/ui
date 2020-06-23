(ns sixsq.nuvla.ui.infrastructures-detail.views
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.acl.views :as acl]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.infrastructures-detail.events :as events]
    [sixsq.nuvla.ui.infrastructures-detail.spec :as spec]
    [sixsq.nuvla.ui.infrastructures-detail.subs :as subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.utils.resource-details :as resource-details]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))


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
                                  [ui/Icon {:name "trash"}]
                                  (@tr [:delete])])
      :header      (@tr [:delete-infrastructure])
      :content     [:h3 content]}]))


(defn MenuBar [uuid]
  (let [can-delete?   (subscribe [::subs/can-delete?])
        infra-service (subscribe [::subs/infrastructure-service])
        loading?      (subscribe [::subs/loading?])]
    [ui/Menu {:borderless true}
     (when @can-delete?
       [DeleteButton infra-service])

     [main-components/RefreshMenu
      {:loading?   @loading?
       :on-refresh #(refresh uuid)}]]))


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
            :on-change (partial on-change :endpoint)]
           ;; FIXME: refactor.
           (if (= (:subtype @infra-service) "swarm")
             [uix/TableRowField "COE manager", :key (str id "-subtype"),
              :editable? false, :spec ::spec/endpoint, :validate-form? @validate-form?,
              :required? true, :default-value [:a {:href   (str "http:" (second (str/split endpoint #":")) ":9000")
                                                   :target "_blank"}
                                               (str "http:" (second (str/split endpoint #":")) ":9000")]])]]
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


(defn InfrastructureDetails
  [uuid]
  (let [infra-service (subscribe [::subs/infrastructure-service])]
    (refresh uuid)
    (fn [uuid]
      ^{:key uuid}
      [ui/Container {:fluid true}
       [MenuBar uuid]
       [resource-details/detail-header @infra-service]
       ^{:key uuid}
       [InfraService]])))
