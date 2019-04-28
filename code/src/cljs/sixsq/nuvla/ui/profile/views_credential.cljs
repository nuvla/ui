(ns sixsq.nuvla.ui.profile.views-credential
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch dispatch-sync subscribe]]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.profile.events :as events]
    [sixsq.nuvla.ui.profile.subs :as subs]
    [sixsq.nuvla.ui.profile.spec :as spec]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.accordion :as utils-accordion]
    [sixsq.nuvla.ui.utils.validation :as utils-validation]
    [reagent.core :as reagent]
    [sixsq.nuvla.ui.utils.form-fields :as forms]
    [sixsq.nuvla.ui.utils.general :as utils-general]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [cljs.spec.alpha :as s]
    [taoensso.timbre :as log]))


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
            (if (= type :input)
              [ui/Input {:default-value value
                         :placeholder   (@tr [name-kw])
                         :disabled      (not editable?)
                         :error         (when (and validate? (not valid?)) true)
                         :fluid         true
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
  (let [is-new?         (subscribe [::subs/is-new?])
        credential      (subscribe [::subs/credential])]
    (fn []
      (let [editable? (utils-general/editable? @credential @is-new?)
            {:keys [name description ca cert key]} @credential]
        (log/infof "credential-swarm: %s %s %s" name description @credential)

        [ui/Table (assoc style/definition :class :nuvla-ui-editable)
         [ui/TableBody
          [row-with-label "name" :name name editable? true
           ::spec/name :input ::events/validate-swarm-credential-form]
          [row-with-label "description" :description description editable? true
           ::spec/description :input ::events/validate-swarm-credential-form]
          [row-with-label "swarm-credential-ca" :ca ca editable? true
           ::spec/ca :textarea ::events/validate-swarm-credential-form]
          [row-with-label "swarm-credential-cert" :cert cert editable? true
           ::spec/cert :textarea ::events/validate-swarm-credential-form]
          [row-with-label "swarm-credential-key" :key key editable? true
           ::spec/key :textarea ::events/validate-swarm-credential-form]]]))))


(defn save-callback
  []
  (dispatch-sync [::events/set-validate-form? true])
  (dispatch-sync [::events/validate-swarm-credential-form])
  (let [form-valid? (get @re-frame.db/app-db ::spec/form-valid?)]
    (log/infof "form valid? %s" form-valid?)
    (when form-valid?
      (do
        (dispatch [::events/set-validate-form? false])
        (dispatch [::events/edit-credential])))))


(defn credential-modal
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        visible?    (subscribe [::subs/credential-modal-visible?])
        form-valid? (subscribe [::subs/form-valid?])
        credential  (subscribe [::subs/credential])
        is-new?     (subscribe [::subs/is-new?])]
    (fn []
      (let [type   (:type @credential "")
            header (str (if is-new? "New" "Update") " Credential: " type)]
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-credential-modal])}

         [ui/ModalHeader header]

         [ui/ModalContent {:scrolling false}
          [utils-validation/validation-error-message ::subs/form-valid?]
          (case type
            "infrastructure-service-swarm" [credential-swarm]
            [credential-swarm])]
         [ui/ModalActions
          (log/infof "form-valid? %s" @form-valid?)
          [uix/Button {:text     (if (true? @is-new?) (@tr [:create]) (@tr [:save]))
                       :positive true
                       :disabled (when-not @form-valid? true)
                       :active   true
                       :on-click #(save-callback)}]]]))))


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
                                  (dispatch [::events/close-add-credential-modal])
                                  (dispatch [::events/open-credential-modal {:name        ""
                                                                             :description ""
                                                                             :type        "infrastructure-service-swarm"
                                                                             :ca          ""
                                                                             :cert        ""
                                                                             :key         ""}
                                             true]))}
            [ui/CardContent {:text-align :center}
             [ui/Header "Swarm"]
             [ui/Icon {:name "docker"
                       :size :massive}]]]

           [ui/Card {:on-click #(do
                                  (dispatch [::events/close-add-credential-modal]))}
            [ui/CardContent {:text-align :center}
             [ui/Header "MinIO"]
             [:div]
             [ui/Image {:src  "data:image/png;base64,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"
                        :size :small}]]]
           [ui/Card {:on-click #(do
                                  (dispatch [::events/close-add-credential-modal]))}
            [ui/CardContent {:text-align :center}
             [ui/Header "Kubernetes"]
             [:div]
             [ui/Image {:src  "https://raw.githubusercontent.com/kubernetes/kubernetes/master/logo/logo.png"
                        :size :small}]]]]]
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
      [utils-accordion/trash id ::events/delete-credential nil]
      [ui/Icon {:name     :cog
                :color    :blue
                :style    {:cursor :pointer}
                :on-click #(dispatch [::events/open-credential-modal credential false])}]]]))


(defn credentials
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        credentials (subscribe [::subs/credentials])
        active?     (reagent/atom true)]
    (fn []
      (let []
        (dispatch [::events/get-credentials])
        [ui/Accordion {:fluid     true
                       :styled    true
                       :exclusive false}
         [ui/AccordionTitle {:active   @active?
                             :index    1
                             :on-click #(utils-accordion/toggle active?)}
          [ui/Icon {:name (if @active? "dropdown" "caret right")}]
          (@tr [:credentials]) (utils-accordion/show-count @credentials)]

         [ui/AccordionContent {:active @active?}
          [:div "Credentials"
           [:span forms/nbsp (forms/help-popup (@tr [:credentials-help]))]]
          [control-bar-projects]
          (if (empty? @credentials)
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
                    (for [credential @credentials]
                      ^{:key (:id credential)}
                      [single-credential credential])]]])]]))))
