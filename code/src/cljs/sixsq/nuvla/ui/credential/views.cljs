(ns sixsq.nuvla.ui.credential.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch dispatch-sync subscribe]]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.credential.events :as events]
    [sixsq.nuvla.ui.credential.subs :as subs]
    [sixsq.nuvla.ui.credential.spec :as spec]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.accordion :as utils-accordion]
    [sixsq.nuvla.ui.utils.validation :as utils-validation]
    [reagent.core :as reagent]
    [sixsq.nuvla.ui.utils.general :as utils-general]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [cljs.spec.alpha :as s]
    [taoensso.timbre :as log]
    [taoensso.timbre :as timbre]))


(defn in?
  "true if coll contains elm"
  [coll elm]
  (some #(= elm %) coll))


(defn row-with-label
  [key name-kw value editable? mandatory? value-spec type validation-event]
  (let [tr              (subscribe [::i18n-subs/tr])
        active-input    (subscribe [::subs/active-input])
        local-validate? (reagent/atom false)
        validate-form?  (subscribe [::subs/validate-form?])]
    (fn [key name-kw value editable? mandatory? value-spec type validation-event]
      (let [name-str      (name name-kw)
            name-label    (if (and editable? mandatory?) (utils-general/mandatory-name name-str) name-str)
            input-active? (= name-str @active-input)
            validate?     (or @local-validate? @validate-form?)
            valid?        (s/valid? value-spec value)]
        (s/explain value-spec value)
        (log/infof "local-validate?: %s validate-form?: %s" @local-validate? @validate-form?)
        [ui/TableRow
         [ui/TableCell {:collapsing true}
          name-label]
         [ui/TableCell
          (if editable?
            ^{:key key}
            (if (in? [:input :password] type)
              [ui/Input {:default-value value
                         :placeholder   (@tr [name-kw])
                         :disabled      (not editable?)
                         :error         (when (and validate? (not valid?)) true)
                         :fluid         true
                         :type          (if (= type :input) :text type)
                         :icon          (when input-active? :pencil)
                         :onMouseEnter  #(dispatch [::events/active-input name-str])
                         :onMouseLeave  #(dispatch [::events/active-input nil])
                         :on-change     (ui-callback/input-callback
                                          #(do
                                             (reset! local-validate? true)
                                             (dispatch [::main-events/changes-protection? true])
                                             (dispatch [validation-event])
                                             (dispatch [::events/update-credential name-kw %])))}]
              ; Semantic UI's textarea styling requires to be wrapped in a form
              [ui/Form
               [ui/FormField {:class (when (and validate? (not valid?)) :error)}
                [ui/TextArea {:default-value value
                              :placeholder   (@tr [name-kw])
                              :disabled      (not editable?)
                              :icon          (when input-active? :pencil)
                              :onMouseEnter  #(dispatch [::events/active-input name-str])
                              :onMouseLeave  #(dispatch [::events/active-input nil])
                              :on-change     (ui-callback/input-callback
                                               #(do
                                                  (reset! local-validate? true)
                                                  (dispatch [::main-events/changes-protection? true])
                                                  (dispatch [validation-event])
                                                  (dispatch [::events/update-credential name-kw %])))}]]])
            [:span value])]]))))


(defn credential-swarm
  []
  (let [is-new?    (subscribe [::subs/is-new?])
        credential (subscribe [::subs/credential])]
    (fn []
      (let [editable?             (utils-general/editable? @credential @is-new?)
            {:keys [name description ca cert key]} @credential
            form-validation-event ::events/validate-swarm-credential-form]
        [ui/Table (assoc style/definition :class :nuvla-ui-editable)
         [ui/TableBody
          [row-with-label "name" :name name editable? true
           ::spec/name :input form-validation-event]
          [row-with-label "description" :description description editable? true
           ::spec/description :input form-validation-event]
          [row-with-label "swarm-credential-ca" :ca ca editable? true
           ::spec/ca :textarea form-validation-event]
          [row-with-label "swarm-credential-cert" :cert cert editable? true
           ::spec/cert :textarea form-validation-event]
          [row-with-label "swarm-credential-key" :key key editable? true
           ::spec/key :textarea form-validation-event]]]))))


(defn credential-minio
  []
  (let [is-new?    (subscribe [::subs/is-new?])
        credential (subscribe [::subs/credential])]
    (fn []
      (let [editable?             (utils-general/editable? @credential @is-new?)
            {:keys [name description access-key secret-key]} @credential
            form-validation-event ::events/validate-minio-credential-form]
        [ui/Table (assoc style/definition :class :nuvla-ui-editable)
         [ui/TableBody
          [row-with-label "name" :name name editable? true
           ::spec/name :input form-validation-event]
          [row-with-label "description" :description description editable? true
           ::spec/description :input form-validation-event]
          [row-with-label "access-key" :access-key access-key editable? true
           ::spec/access-key :input form-validation-event]
          [row-with-label "secret-key" :secret-key secret-key editable? true
           ::spec/secret-key :password form-validation-event]]]))))


(defn credential-store-azure
  []
  (let [is-new?    (subscribe [::subs/is-new?])
        credential (subscribe [::subs/credential])]
    (fn []
      (let [editable?             (utils-general/editable? @credential @is-new?)
            {:keys [name description access-key secret-key]} @credential
            form-validation-event ::events/validate-minio-credential-form]
        [ui/Table (assoc style/definition :class :nuvla-ui-editable)
         [ui/TableBody]]))))


(defn credential-store-amazonec2
  []
  (let [is-new?    (subscribe [::subs/is-new?])
        credential (subscribe [::subs/credential])]
    (fn []
      (let [editable?             (utils-general/editable? @credential @is-new?)
            {:keys [name description access-key secret-key]} @credential
            form-validation-event ::events/validate-minio-credential-form]
        [ui/Table (assoc style/definition :class :nuvla-ui-editable)
         [ui/TableBody]]))))


(defn credential-store-exoscale
  []
  (let [is-new?    (subscribe [::subs/is-new?])
        credential (subscribe [::subs/credential])]
    (fn []
      (let [editable?             (utils-general/editable? @credential @is-new?)
            {:keys [name description access-key secret-key]} @credential
            form-validation-event ::events/validate-minio-credential-form]
        [ui/Table (assoc style/definition :class :nuvla-ui-editable)
         [ui/TableBody]]))))


(defn save-callback
  [form-validation-event]
  (dispatch-sync [::events/set-validate-form? true])
  (dispatch-sync [form-validation-event])
  (let [form-valid? (get @re-frame.db/app-db ::spec/form-valid?)]
    (log/infof "form valid? %s" form-valid?)
    (when form-valid?
      (do
        (dispatch [::events/set-validate-form? false])
        (dispatch [::events/edit-credential])))))


(def infrastructure-service-validation-map
  {"infrastructure-service-swarm"          {:validation-event ::events/validate-swarm-credential-form
                                            :modal-content    credential-swarm}
   "infrastructure-service-minio"          {:validation-event ::events/validate-minio-credential-form
                                            :modal-content    credential-minio}
   "infrastructure-service-azure"          {:validation-event ::events/validate-store-azure-form
                                            :modal-content    credential-store-azure}
   "infrastructure-service-amazonec2"      {:validation-event ::events/validate-store-amazonec2-form
                                            :modal-content    credential-store-amazonec2}
   "infrastructure-service-exoscale"       {:validation-event ::events/validate-store-exoscale-form
                                            :modal-content    credential-store-exoscale}
   "cloud-infrastructure-service-exoscale" {:validation-event ::events/validate-swarm-credential-form
                                            :modal-content    credential-swarm
                                            }
   "cloud-infrastructure-service-azure"    {:validation-event ::events/validate-minio-credential-form
                                            :modal-content    credential-minio}
   ;""
   ;{:validation-event ::events/validate-swarm-credential-form
   ; :modal-content    credential-swarm}
   })


(def infrastructure-service-types
  (keys infrastructure-service-validation-map))


(def cloud-types
  ["cloud-infrastructure-service-exoscale"
   "cloud-infrastructure-service-azure"])


(defn credential-modal
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        visible?    (subscribe [::subs/credential-modal-visible?])
        form-valid? (subscribe [::subs/form-valid?])
        credential  (subscribe [::subs/credential])
        is-new?     (subscribe [::subs/is-new?])]
    (fn []
      (let [type             (:type @credential "")
            header           (str (if is-new? "New" "Update") " Credential: " type)
            validation-item  (get infrastructure-service-validation-map type)
            validation-event (:validation-event validation-item)
            modal-content    (:modal-content validation-item)]
        (log/infof "infrastructure-service-validation-map: %s" (keys infrastructure-service-validation-map))
        (log/infof "type: %s" type)
        (log/infof "type nil?: %s" (nil? type))
        (log/infof "validation-item: %s" validation-item)
        (log/infof "validation-event: %s" validation-event)
        (if (empty? type)
          [:div]
          [ui/Modal {:open       @visible?
                     :close-icon true
                     :on-close   #(dispatch [::events/close-credential-modal])}

           [ui/ModalHeader header]

           [ui/ModalContent {:scrolling false}
            [utils-validation/validation-error-message ::subs/form-valid?]
            [modal-content]]
           [ui/ModalActions
            [uix/Button {:text     (if (true? @is-new?) (@tr [:create]) (@tr [:save]))
                         :positive true
                         :disabled (when-not @form-valid? true)
                         :active   true
                         :on-click #(save-callback validation-event)}]]])))))


(defn add-credential-modal
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        visible? (subscribe [::subs/add-credential-modal-visible?])]
    (fn []
      (let []
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-add-credential-modal])}

         [ui/ModalHeader [ui/Icon {:name "add"}] (@tr [:add])]

         [ui/ModalContent {:scrolling false}
          [:div {:style {:padding-bottom 20}} "Choose the credential type you want to add."]
          [ui/CardGroup {:centered true}

           [ui/Card {:on-click #(do
                                  (dispatch [::events/set-validate-form? false])
                                  (dispatch [::events/form-valid])
                                  (dispatch [::events/close-add-credential-modal])
                                  (dispatch [::events/open-credential-modal {:type "infrastructure-service-swarm"}
                                             true]))}
            [ui/CardContent {:text-align :center}
             [ui/Header "Swarm"]
             [ui/Icon {:name "docker"
                       :size :massive}]]]

           [ui/Card {:on-click #(do
                                  (dispatch [::events/set-validate-form? false])
                                  (dispatch [::events/form-valid])
                                  (dispatch [::events/close-add-credential-modal])
                                  (dispatch [::events/open-credential-modal {:type "infrastructure-service-minio"}
                                             true]))}
            [ui/CardContent {:text-align :center}
             [ui/Header "MinIO"]
             [:div]
             [ui/Image {:src  "/ui/images/minio.png"
                        :size :tiny}]]]
           ;[ui/Card {:on-click #(do
           ;                       (dispatch [::events/set-validate-form? false])
           ;                       (dispatch [::events/form-valid])
           ;                       (dispatch [::events/close-add-credential-modal])
           ;                       (dispatch [::events/open-credential-modal {:type "store-infrastructure-service-exoscale"}
           ;                                  true]))}
           ; [ui/CardContent {:text-align :center}
           ;  [ui/Header "Exoscale Store (S3)"]
           ;  [:div]
           ;  [ui/Image {:src  "/ui/images/exoscale.png"
           ;             :size :large}]]]
           ;[ui/Card {:on-click #(do
           ;                       (dispatch [::events/set-validate-form? false])
           ;                       (dispatch [::events/form-valid])
           ;                       (dispatch [::events/close-add-credential-modal])
           ;                       (dispatch [::events/open-credential-modal {:type "store-infrastructure-service-azure"}
           ;                                  true]))}
           ; [ui/CardContent {:text-align :center}
           ;  [ui/Header "Azure Store (S3)"]
           ;  [:div]
           ;  [ui/Image {:src  "/ui/images/azure.png"
           ;             :size :small}]]]
           ;[ui/Card {:on-click #(do
           ;                       (dispatch [::events/set-validate-form? false])
           ;                       (dispatch [::events/form-valid])
           ;                       (dispatch [::events/close-add-credential-modal])
           ;                       (dispatch [::events/open-credential-modal {:type "store-infrastructure-service-amazonec2"}
           ;                                  true]))}
           ; [ui/CardContent {:text-align :center}
           ;  [ui/Header "Amazon S3"]
           ;  [:div]
           ;  [ui/Image {:src  "/ui/images/aws.png"
           ;             :size :small}]]]
           ]]
         [ui/ModalActions]]))))


(defn refresh-button
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn []
      [uix/MenuItemWithIcon
       {:name      (@tr [:refresh])
        :icon-name "refresh"
        :position  "right"
        :on-click  #(dispatch [::events/get-credentials])}])))


(defn control-bar-projects []
  (let [tr (subscribe [::i18n-subs/tr])]
    (vec (concat [ui/Menu {:borderless true}
                  [uix/MenuItemWithIcon
                   {:name      (@tr [:add])
                    :icon-name "add"
                    :on-click  #(dispatch [::events/open-add-credential-modal])}]
                  [refresh-button]]))))


(defn delete-confirmation-modal
  []
  (let [tr         (subscribe [::i18n-subs/tr])
        visible?   (subscribe [::subs/delete-confirmation-modal-visible?])
        credential (subscribe [::subs/credential])
        confirmed? (reagent/atom false)]
    (fn []
      (let [id (:id @credential)
            name (:name @credential)]
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-delete-confirmation-modal])}

         [ui/ModalHeader (str "Delete Credential?")]

         [ui/ModalContent {:scrolling false}
          [:h3 name " (" (:description @credential) ")"]
          [:div "(" id ")"]
          [ui/Message {:error true}
           [ui/MessageHeader "Danger - this cannot be undone!"]
           [ui/MessageContent
            [:p]
            [ui/Checkbox {:name      "confirm-deletion"
                          :checked   @confirmed?
                          :fitted    true
                          :on-change #(reset! confirmed? (not @confirmed?))}]
            " "
            (@tr [:credential-delete-warning])]]]

         [ui/ModalActions
          [uix/Button {:text     (@tr [:delete])
                       :positive true
                       :disabled (when-not @confirmed? true)
                       :active   true
                       :on-click #(do
                                    (reset! confirmed? false)
                                    (dispatch [::events/delete-credential id])
                                    (dispatch [::events/close-delete-confirmation-modal]))}]]]))))


;type name description
(defn single-credential
  [{:keys [id type name description] :as credential}]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/TableRow                                            ;{:key id}
     [ui/TableCell {:floated :left
                    :width   2}
      [:span name]]
     [ui/TableCell {:floated :left
                    :width   9}
      [:span description]]
     [ui/TableCell {:floated :left
                    :width   4}
      [:span type]]
     [ui/TableCell {:floated :right
                    :width   1
                    :align   :right
                    :style   {}}
      [utils-accordion/trash id ::events/open-delete-confirmation-modal nil credential]
      [ui/Icon {:name     :cog
                :color    :blue
                :style    {:cursor :pointer}
                :on-click #(dispatch [::events/open-credential-modal credential false])}]]]))


(defn credentials
  []
  (let [tr                    (subscribe [::i18n-subs/tr])
        credentials           (subscribe [::subs/credentials])
        infra-service-active? (reagent/atom true)
        cloud-active?         (reagent/atom false)]
    (fn []
      (let [infra-service-creds (filter #(in? infrastructure-service-types (:type %)) @credentials)
            cloud-creds         (filter #(in? cloud-types (:type %)) @credentials)]
        (dispatch [::events/get-credentials])
        [ui/Container {:fluid true}
         [:h2 [ui/Icon {:name "key"}]
          " "
          (str/capitalize (@tr [:credentials]))]

         [ui/Accordion {:fluid     true
                        :styled    true
                        :exclusive false}
          [ui/AccordionTitle {:active   @infra-service-active?
                              :index    1
                              :on-click #(utils-accordion/toggle infra-service-active?)}
           [:h3
            [ui/Icon {:name (if @infra-service-active? "dropdown" "caret right")}]
            (@tr [:credential-infra-service-section])
            (utils-accordion/show-count infra-service-creds)]]

          [ui/AccordionContent {:active @infra-service-active?}
           [:div (@tr [:credential-infra-service-section-sub-text])]
           [control-bar-projects]
           (if (empty? infra-service-creds)
             [ui/Message
              (str/capitalize (str (@tr [:no-credentials]) "."))]
             [:div [ui/Table {:style {:margin-top 10}
                              :class :nuvla-ui-editable}
                    [ui/TableHeader
                     [ui/TableRow
                      [ui/TableHeaderCell {:content "Name"}]
                      [ui/TableHeaderCell {:content "Description"}]
                      [ui/TableHeaderCell {:content "Type"}]
                      [ui/TableHeaderCell {:content "Actions"}]]]
                    [ui/TableBody
                     (for [credential infra-service-creds]
                       ^{:key (:id credential)}
                       [single-credential credential])]]])]]

         [ui/Accordion {:fluid     true
                        :styled    true
                        :exclusive false
                        :style     {:margin-top 10}}
          [ui/AccordionTitle {:active   @cloud-active?
                              :index    1
                              :on-click #(utils-accordion/toggle cloud-active?)}
           [:h3
            [ui/Icon {:name (if @cloud-active? "dropdown" "caret right")}]
            (@tr [:credential-cloud-section])
            (utils-accordion/show-count cloud-creds)]]

          [ui/AccordionContent {:active @cloud-active?}
           [ui/Message
            (str/capitalize (str (@tr [:coming-soon])))]]]]))))


(defmethod panel/render :credential
  [path]
  (timbre/set-level! :info)
  [:div
   [credentials]
   [add-credential-modal]
   [credential-modal]
   [delete-confirmation-modal]])
