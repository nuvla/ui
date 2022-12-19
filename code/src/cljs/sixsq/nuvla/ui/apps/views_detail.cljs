(ns sixsq.nuvla.ui.apps.views-detail
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch dispatch-sync subscribe]]
    [re-frame.db]
    [reagent.core :as r]
    [sixsq.nuvla.ui.acl.utils :as acl-utils]
    [sixsq.nuvla.ui.acl.views :as acl-views]
    [sixsq.nuvla.ui.apps-application.events :as apps-application-events]
    [sixsq.nuvla.ui.apps.events :as events]
    [sixsq.nuvla.ui.apps.spec :as spec]
    [sixsq.nuvla.ui.apps.subs :as subs]
    [sixsq.nuvla.ui.apps.utils :as utils]
    [sixsq.nuvla.ui.apps.utils-detail :as utils-detail]
    [sixsq.nuvla.ui.deployment-dialog.events :as deployment-dialog-events]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.intercom.events :as intercom-events]
    [sixsq.nuvla.ui.main.components :as components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.profile.subs :as profile-subs]
    [sixsq.nuvla.ui.session.subs :as session-subs]
    [sixsq.nuvla.ui.utils.collapsible-card :as cc]
    [sixsq.nuvla.ui.utils.form-fields :as ff]
    [sixsq.nuvla.ui.utils.forms :as utils-forms]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as utils-values]))


(def application-kubernetes-subtype "application_kubernetes")
(def docker-compose-subtype "application")

(def edit-cell-left-padding 24)

(defn LicenseTitle
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [:<>
     [uix/Icon {:name "drivers license"}]
     (str/capitalize (@tr [:license]))]))


(defn PricingTitle
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [:<>
     [uix/Icon {:name "euro"}]
     (str/capitalize (@tr [:pricing]))]))


(defn DeploymentsTitle
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [:<>
     [uix/Icon {:name "rocket"}]
     (str/capitalize (@tr [:deployments]))]))


(defn DetailsTitle
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [:<>
     [uix/Icon {:name "info"}]
     (str/capitalize (@tr [:details]))]))


(defn DockerTitle
  []
  (let [module-subtype (subscribe [::subs/module-subtype])
        tab-name       (if (= application-kubernetes-subtype @module-subtype) "Kubernetes" "Docker")]
    [:<>
     [uix/Icon {:name "docker"}]
     (str/capitalize tab-name)]))


(defn ConfigurationTitle
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [:<>
     [uix/Icon {:name "cog"}]
     (str/capitalize (@tr [:configuration]))]))


(defn TabMenuDetails
  []
  (let [tr     (subscribe [::i18n-subs/tr])
        error? (subscribe [::subs/details-validation-error?])]
    [:span {:style {:color (if (true? @error?) utils-forms/dark-red "black")}}
     [uix/Icon {:name "info"}]
     (str/capitalize (@tr [:details]))]))


(defn edit-button-disabled?
  [page-changed? form-valid?]
  (or (not page-changed?) (not form-valid?)))


(defn save-callback
  []
  (dispatch-sync [::events/set-validate-form? true])
  (dispatch-sync [::events/validate-form])
  (let [form-valid? (get @re-frame.db/app-db ::spec/form-valid?)
        {:keys [subtype]} (get @re-frame.db/app-db ::spec/module)
        new-subtype (:subtype @(subscribe [::main-subs/nav-query-params]))]
    (when form-valid?
      (dispatch [::events/set-validate-form? false])
      (if (= (or subtype new-subtype) "project")
        (dispatch [::events/open-save-modal])
        (dispatch [::events/open-save-modal])))))


(defn DeleteButton
  [module]
  (let [tr      (subscribe [::i18n-subs/tr])
        is-new? (subscribe [::subs/is-new?])
        {:keys [id name description]} module
        content (str (or name id) (when description " - ") (utils-values/markdown->summary description))]
    [uix/ModalDanger
     {:on-confirm  #(dispatch [::events/delete-module id])
      :trigger     (r/as-element [ui/MenuItem {:disabled @is-new?}
                                  [ui/Icon {:name "trash"}]
                                  (str/capitalize (@tr [:delete]))])
      :content     [:h3 content]
      :header      (@tr [:delete-module])
      :danger-msg  (@tr [:module-delete-warning])
      :button-text (@tr [:delete])}]))


(defn PublishButton
  [module]
  (let [tr      (subscribe [::i18n-subs/tr])
        is-new? (subscribe [::subs/is-new?])
        {:keys [id]} module]
    [uix/ModalFromButton
     {:on-confirm  #(dispatch [::events/publish id])
      :trigger     (r/as-element [ui/MenuItem {:disabled @is-new?}
                                  [ui/Icon {:name utils/publish-icon}]
                                  (str/capitalize (@tr [:publish]))])
      :content     [:p (@tr [:publish-confirmation-message])]
      :header      (@tr [:publish-module])
      :icon        utils/publish-icon
      :button-text (@tr [:publish])}]))


(defn UnPublishButton
  [module]
  (let [tr (subscribe [::i18n-subs/tr])
        {:keys [id]} module]
    [uix/ModalFromButton
     {:on-confirm  #(dispatch [::events/un-publish id])
      :trigger     (r/as-element [ui/MenuItem
                                  [ui/Icon {:name utils/un-publish-icon}]
                                  (str/capitalize (@tr [:un-publish]))])
      :content     [:p (@tr [:un-publish-confirmation-message])]
      :header      (@tr [:un-publish-module])
      :icon        utils/un-publish-icon
      :button-text (@tr [:un-publish])}]))


(defn MenuBar []
  (let [tr               (subscribe [::i18n-subs/tr])
        module           (subscribe [::subs/module])
        is-new?          (subscribe [::subs/is-new?])
        page-changed?    (subscribe [::main-subs/changes-protection?])
        form-valid?      (subscribe [::subs/form-valid?])
        editable?        (subscribe [::subs/editable?])
        module-id        (subscribe [::subs/module-id-version])
        is-project?      (subscribe [::subs/is-project?])
        is-app?          (subscribe [::subs/is-app?])
        can-copy?        (subscribe [::subs/can-copy?])
        paste-disabled?  (subscribe [::subs/paste-disabled?])
        launch-disabled? (subscribe [::subs/launch-disabled?])
        can-publish?     (subscribe [::subs/can-publish?])
        can-unpublish?   (subscribe [::subs/can-unpublish?])]
    (fn []
      [components/StickyBar
       [ui/Menu {:borderless true}
        (when @editable?
          [uix/MenuItem
           {:name     (@tr [:save])
            :icon     "save"
            :disabled (edit-button-disabled? @page-changed? @form-valid?)
            :on-click save-callback}])

        (when @is-app?
          [uix/MenuItem
           {:name     (@tr [:launch])
            :icon     "rocket"
            :disabled @launch-disabled?
            :on-click #(dispatch [::main-events/subscription-required-dispatch
                                  [::deployment-dialog-events/create-deployment
                                   @module-id :infra-services]])}])

        (when @is-project?
          [uix/MenuItem
           {:name     (@tr [:add])
            :icon     "add"
            :disabled @launch-disabled?
            :on-click #(dispatch [::events/open-add-modal])}])
        (when @can-copy?
          [ui/Popup
           {:trigger        (r/as-element
                              [ui/MenuItem
                               {:name     (@tr [:copy])
                                :icon     "copy"
                                :disabled @is-new?
                                :on-click #(dispatch [::events/copy])}])
            :content        (@tr [:module-copied])
            :on             "click"
            :position       "top center"
            :wide           true
            :hide-on-scroll true}])

        (when @is-project?
          [uix/MenuItem
           {:name     (@tr [:paste])
            :icon     "paste"
            :disabled @paste-disabled?
            :on-click #(dispatch [::events/open-paste-modal])}])

        (when (general-utils/can-delete? @module)
          [DeleteButton @module])

        (when @can-unpublish?
          [UnPublishButton @module])

        (when @can-publish?
          [PublishButton @module])

        [components/RefreshMenu
         {:refresh-disabled? @is-new?
          :on-refresh        #(dispatch [::events/refresh])}]]])))


(defn save-modal
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        visible?       (subscribe [::subs/save-modal-visible?])
        username       (subscribe [::session-subs/user])
        commit-message (subscribe [::subs/commit-message])
        need-commit?   (subscribe [::subs/module-content-updated?])
        subtype        (subscribe [::subs/module-subtype])]
    (fn []
      (let [commit-map {:author @username
                        :commit @commit-message}
            save-fn    #(do (dispatch [::events/edit-module (when @need-commit? commit-map)])
                            (dispatch [::events/close-save-modal])
                            (dispatch [::events/commit-message nil])
                            (dispatch [::intercom-events/set-event "Last save module" (time/timestamp)]))]
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(do (dispatch [::events/commit-message nil])
                                    (dispatch [::events/close-save-modal]))}

         [uix/ModalHeader {:header (str (@tr [:save]) " " (@tr [:component]))}]

         [ui/ModalContent
          (if (and @need-commit? (not= @subtype "project"))
            [ui/Input {:placeholder   (@tr [:commit-placeholder])
                       :fluid         true
                       :default-value @commit-message
                       :auto-focus    true
                       :focus         true
                       :on-change     (ui-callback/input-callback
                                        #(dispatch [::events/commit-message %]))
                       :on-key-press  (partial utils-forms/on-return-key save-fn)}]
            (@tr [:are-you-sure?]))]

         [ui/ModalActions
          [uix/Button {:text     (@tr [:save])
                       :positive true
                       :active   true
                       :on-click save-fn}]]]))))


(defn logo-url-modal
  []
  (let [local-url (r/atom "")
        tr        (subscribe [::i18n-subs/tr])
        visible?  (subscribe [::subs/logo-url-modal-visible?])
        module    (subscribe [::subs/module])]
    (fn []
      [ui/Modal {:open       @visible?
                 :close-icon true
                 :on-close   #(dispatch [::events/close-logo-url-modal])}

       [uix/ModalHeader {:header (@tr [:select-logo-url])}]

       [ui/ModalContent
        [ui/Input {:default-value (or (:logo-url @module) "")
                   :placeholder   (@tr [:logo-url-placeholder])
                   :fluid         true
                   :auto-focus    true
                   :on-change     (ui-callback/input-callback #(reset! local-url %))
                   :on-key-press  (partial utils-forms/on-return-key
                                           #(dispatch [::events/save-logo-url @local-url]))}]]

       [ui/ModalActions
        [uix/Button {:text     "Ok"
                     :positive true
                     :disabled (empty? @local-url)
                     :active   true
                     :on-click #(dispatch [::events/save-logo-url @local-url])}]]])))


(defn AddModal
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        visible? (subscribe [::subs/add-modal-visible?])
        nav-path (subscribe [::main-subs/nav-path])]
    (fn []
      (let [parent (utils/nav-path->module-path @nav-path)]
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-add-modal])}

         [uix/ModalHeader {:header (@tr [:add]) :icon "add"}]

         [ui/ModalContent {:scrolling false}
          [ui/CardGroup {:centered    true
                         :itemsPerRow 3}

           [ui/Card
            {:on-click #(do (dispatch [::events/close-add-modal])
                            (dispatch [::history-events/navigate
                                       (str/join
                                         "/" (remove str/blank?
                                                     ["apps" parent
                                                      "New Project?subtype=project"]))]))}
            [ui/CardContent {:text-align :center}
             [ui/Header "Project"]
             [ui/Icon {:name "folder"
                       :size :massive}]]]

           [ui/Card
            {:on-click (when parent
                         #(do
                            (dispatch [::events/close-add-modal])
                            (dispatch [::history-events/navigate
                                       (str/join
                                         "/" (remove str/blank?
                                                     ["apps" parent
                                                      "New Application?subtype=application"]))])))}
            [ui/CardContent {:text-align :center}
             [ui/Header (@tr [:application-docker])]
             [:div]
             [ui/IconGroup
              [ui/Icon {:name  "cubes"
                        :size  :massive
                        :color (when-not parent :grey)}]
              [:div [ui/Icon {:name  "docker"
                              :size  :huge
                              :color (when-not parent :grey)
                              :style {:padding-left "150px"}}]]]]]

           [ui/Card
            {:on-click (when parent
                         #(do
                            (dispatch [::events/close-add-modal])
                            (dispatch [::history-events/navigate
                                       (str/join
                                         "/" (remove str/blank?
                                                     ["apps" parent
                                                      "New Application?subtype=application_kubernetes"]))])))}
            [ui/CardContent {:text-align :center}
             [ui/Header (@tr [:application-kubernetes])]
             [:div]
             [ui/IconGroup {:size :massive}
              [ui/Icon {:name  "cubes"
                        :color (when-not parent :grey)}]
              [ui/Image {:src     (if parent "/ui/images/kubernetes.svg" "/ui/images/kubernetes-grey.svg")
                         :floated "right"
                         :style   {:width "50px"}}]]]]]]]))))


(defn paste-modal
  []
  (let [tr              (subscribe [::i18n-subs/tr])
        visible?        (subscribe [::subs/paste-modal-visible?])
        copy-module     (subscribe [::subs/copy-module])
        module-name     (:name @copy-module)
        new-module-name (r/atom module-name)
        form-valid?     (r/atom true)]
    (fn []
      (let [paste-fn #(do (dispatch [::events/close-paste-modal])
                          (dispatch [::events/commit-message nil])
                          (dispatch [::events/paste-module @new-module-name])
                          (reset! new-module-name module-name))]
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-paste-modal])}

         [uix/ModalHeader {:header (str (@tr [:paste-modal-header]))
                           :icon   "paste"}]

         [ui/ModalContent
          [:div (@tr [:paste-modal-content])]
          [:h5 {:style {:margin-top "10px"}} (:parent-path @copy-module) "/" (:name @copy-module)]
          [ui/Table {:compact    true
                     :definition true}
           [ui/TableBody

            [uix/TableRowField (@tr [:new-name]), :key "new-name", :editable? true,
             :spec ::spec/name, :validate-form? true, :required? true,
             :default-value @new-module-name,
             :on-change #(do (reset! new-module-name %)
                             (reset! form-valid? (s/valid? ::spec/name %)))]]]]

         [ui/ModalActions
          [utils-forms/validation-error-msg (@tr [:paste-modal-validation-error]) (not @form-valid?)]
          [uix/Button {:text     (@tr [:paste])
                       :positive true
                       :active   true
                       :disabled (not @form-valid?)
                       :on-click paste-fn}]]]))))


(defn VersionWarning
  []
  (let [tr                     (subscribe [::i18n-subs/tr])
        is-latest?             (subscribe [::subs/is-latest-version?])
        is-latest-published?   (subscribe [::subs/is-latest-published-version?])
        is-module-published?   (subscribe [::subs/is-module-published?])
        latest-published-index (subscribe [::subs/latest-published-index])]
    (fn []
      [:<>
       (when (and @is-module-published? (not @is-latest-published?))
         [ui/Message {:warning true}
          [ui/MessageHeader (@tr [:warning])]
          [ui/MessageContent (@tr [:warning-draft-version-1])
           [:a {:on-click #(dispatch [::events/get-module @latest-published-index])
                :style    {:cursor :pointer}} (@tr [:here])]
           (@tr [:warning-draft-version-2])]])
       (when (and (not @is-module-published?) (not @is-latest?))
         [ui/Message {:warning true}
          [ui/MessageHeader (@tr [:warning])]
          [ui/MessageContent (@tr [:warning-not-latest-version-1])
           [:a {:on-click #(dispatch [::events/get-module -1])
                :style    {:cursor :pointer}} (@tr [:here])]
           (@tr [:warning-not-latest-version-2])]])])))


(defn tuple-to-row [[v1 v2]]
  [ui/TableRow
   [ui/TableCell {:collapsing true} (name v1)]
   [ui/TableCell v2]])


(def module-summary-keys #{:created
                           :updated
                           :resource-url
                           :id
                           :path
                           :subtype})


(defn details-section
  []
  (let [module (subscribe [::subs/module])]
    (fn []
      (let [summary-info (-> (select-keys @module module-summary-keys)
                             (merge {:owners (->> @module :acl :owners (str/join ", "))
                                     :author (->> @module :content :author)}))
            rows         (map tuple-to-row summary-info)]
        [cc/metadata-simple rows]))))


(defn error-text [tr error]
  (if-let [{:keys [status reason]} (ex-data error)]
    (str (or (@tr [reason]) (name reason)) " (" status ")")
    (str error)))


(defn format-error
  [_error]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [error]
      (when (instance? js/Error error)
        [ui/Container
         [ui/Header {:as "h3", :icon true}
          [ui/Icon {:name "warning sign"}]
          (error-text tr error)]]))))


(defn Description
  [validation-event]
  (let [tr             (subscribe [::i18n-subs/tr])
        description    (subscribe [::subs/description])
        editable?      (subscribe [::subs/editable?])
        validate-form? (subscribe [::subs/validate-form?])
        default-value  @description]
    (fn []
      (let [valid?    (s/valid? ::spec/description @description)
            validate? @validate-form?]
        [uix/Accordion
         [:<>
          [ui/Grid {:centered true
                    :columns  2}
           [ui/GridColumn
            [:h4 "Markdown" [general-utils/mandatory-icon]]
            [ui/Segment
             [uix/EditorMarkdown
              default-value
              (fn [_editor _data value]
                (dispatch [::events/description value])
                (dispatch [::main-events/changes-protection? true])
                (dispatch [::events/validate-form]))
              @editable?]
             (when validate?
               (when validation-event
                 (dispatch [validation-event "description" (not valid?)]))
               (when (not valid?)
                 [:<>
                  [ui/Label {:pointing "above", :basic true, :color "red"}
                   (@tr [:description-cannot-be-empty])]]))]]
           [ui/GridColumn
            [:h4 "Preview"]
            [ui/Segment [ui/ReactMarkdown {:class ["markdown"]} @description]]]]]
         :label (str/capitalize (@tr [:description]))
         :default-open true]))))


(defn Tags
  []
  (let [module (subscribe [::subs/module])]
    [components/EditableTags
     @module
     #(do (dispatch [::main-events/changes-protection? true])
          (dispatch [::events/set-tags %]))]))


(defn Details
  [{:keys [_extras _validation-event]}]
  (let [tr               (subscribe [::i18n-subs/tr])
        default-logo-url (subscribe [::subs/default-logo-url])
        module-common    (subscribe [::subs/module-common])
        editable?        (subscribe [::subs/editable?])
        validate-form?   (subscribe [::subs/validate-form?])
        on-change        (fn [update-event-kw value]
                           (dispatch [update-event-kw value])
                           (dispatch [::main-events/changes-protection? true])
                           (dispatch [::events/validate-form]))]
    (fn [{:keys [extras validation-event]}]
      (let [{name     ::spec/name
             parent   ::spec/parent-path
             logo-url ::spec/logo-url
             subtype  ::spec/subtype
             :or      {name     ""
                       parent   ""
                       logo-url @default-logo-url
                       subtype  "project"}} @module-common]
        [:<>
         [:h2 [DetailsTitle]]
         [ui/Grid {:stackable true, :reversed :mobile}
          [ui/GridRow
           [ui/GridColumn {:width 13}
            [ui/Table {:compact    true
                       :definition true}
             [ui/TableBody
              [uix/TableRowField (@tr [:name]), :key (str parent "-name"), :editable? @editable?,
               :spec ::spec/name, :validate-form? @validate-form?, :required? true,
               :default-value name, :on-change (partial on-change ::events/name)
               :on-validation ::events/set-details-validation-error]
              (when (not-empty parent)
                (let [label (if (= "project" subtype) "parent project" "project")]
                  [ui/TableRow
                   [ui/TableCell {:collapsing true
                                  :style      {:padding-bottom 8}} label]
                   [ui/TableCell {:style {:padding-left (when @editable? edit-cell-left-padding)}} parent]]))
              [ui/TableRow
               [ui/TableCell (@tr [:tags])]
               [ui/TableCell [Tags]]]
              (for [x extras]
                x)]]
            [details-section]]
           [ui/GridColumn {:width 3 :floated "right"}
            [ui/Image {:src (or logo-url @default-logo-url)}]
            (when @editable?
              [ui/Button {:fluid    true
                          :on-click #(dispatch [::events/open-logo-url-modal])}
               (@tr [:module-change-logo])])]]]
         [Description validation-event]]))))


(defn input
  [_id _name _value _placeholder _update-event _value-spec _fluid?]
  (let [local-validate? (r/atom false)
        form-valid?     (subscribe [::subs/form-valid?])]
    (fn [id name value placeholder update-event value-spec fluid?]
      (let [input-name (str name "-" id)
            validate?  (or @local-validate? (not @form-valid?))]
        [ui/Input {:name          input-name
                   :placeholder   placeholder
                   :default-value value
                   :type          :text
                   :error         (and validate? (not (s/valid? value-spec value)))
                   :fluid         fluid?
                   :onMouseEnter  #(dispatch [::events/active-input input-name])
                   :onMouseLeave  #(dispatch [::events/active-input nil])
                   :on-change     (ui-callback/input-callback
                                    #(do (reset! local-validate? true)
                                         (dispatch [update-event id (when-not (str/blank? %) %)])
                                         (dispatch [::main-events/changes-protection? true])
                                         (dispatch [::events/validate-form])))}]))))


(defn trash
  [id remove-event]
  [ui/Icon {:name     "trash"
            :link     true
            :on-click #(do (dispatch [::main-events/changes-protection? true])
                           (dispatch [remove-event id])
                           (dispatch [::events/validate-form]))
            :color    :red}])


(defn plus
  [add-event]
  [ui/Icon {:name     "add"
            :link     true
            :color    "green"
            :on-click #(do (dispatch [::main-events/changes-protection? true])
                           (dispatch [add-event {}])
                           (dispatch [::events/validate-form]))}])


(defn SingleEnvVariable
  [env-variable]
  (let [tr              (subscribe [::i18n-subs/tr])
        validate-form?  (subscribe [::subs/validate-form?])
        editable?       (subscribe [::subs/editable?])
        local-validate? (r/atom false)
        {:keys [id
                ::spec/env-name
                ::spec/env-description
                ::spec/env-value
                ::spec/env-required]} env-variable]
    [ui/TableRow {:key id}
     (if @editable?
       [uix/TableRowCell {:key            (str "env-var-name-" id)
                          :placeholder    (@tr [:name])
                          :editable?      @editable?
                          :spec           ::spec/env-name
                          :validate-form? @validate-form?
                          :required?      true
                          :default-value  (or env-name "")
                          :on-change      #(do
                                             (reset! local-validate? true)
                                             (dispatch [::events/update-env-name id %])
                                             (dispatch [::main-events/changes-protection? true])
                                             (dispatch [::events/validate-form]))
                          :on-validation  ::apps-application-events/set-configuration-validation-error}]
       [ui/TableCell {:floated :left
                      :width   3}
        [:span env-name]])

     [ui/TableCell {:floated :left
                    :width   3}
      (if @editable?
        [input id "value" env-value (@tr [:value])
         ::events/update-env-value ::spec/env-value true]
        [:span env-value])]

     [ui/TableCell {:floated :left
                    :width   8}
      (if @editable?
        [input id "description" env-description (@tr [:description])
         ::events/update-env-description ::spec/env-description true]
        [:span env-description])]

     [ui/TableCell {:floated    :left
                    :text-align :center
                    :width      1}
      [ui/Checkbox {:name      (@tr [:required])
                    :checked   (or env-required false)
                    :disabled  (not @editable?)
                    :on-change (ui-callback/checked
                                 #(do (dispatch [::main-events/changes-protection? true])
                                      (dispatch [::events/update-env-required id %])
                                      (dispatch [::events/validate-form])))
                    :align     :middle}]]

     (when @editable?
       [ui/TableCell {:floated :right
                      :width   1
                      :align   :right
                      :style   {}}
        [trash id ::events/remove-env-variable]])]))


(defn EnvVariablesSection []
  (let [tr            (subscribe [::i18n-subs/tr])
        env-variables (subscribe [::subs/env-variables])
        editable?     (subscribe [::subs/editable?])]
    (fn []
      [uix/Accordion
       [:<>
        [:div (@tr [:env-variables])
         [:span ff/nbsp (ff/help-popup (@tr [:module-env-variables-help]))]]
        (if (empty? @env-variables)
          [ui/Message
           (str/capitalize (str (@tr [:module-no-env-variables]) "."))]
          [:div [ui/Table {:style {:margin-top 10}}
                 [ui/TableHeader
                  [ui/TableRow
                   [ui/TableHeaderCell {:content (str/capitalize (@tr [:name]))}]
                   [ui/TableHeaderCell {:content (str/capitalize (@tr [:value]))}]
                   [ui/TableHeaderCell {:content (str/capitalize (@tr [:description]))}]
                   [ui/TableHeaderCell {:content (str/capitalize (@tr [:required]))}]
                   (when @editable?
                     [ui/TableHeaderCell {:content (str/capitalize (@tr [:action]))}])]]
                 [ui/TableBody
                  (for [[id env-variable] @env-variables]
                    ^{:key (str "env_" id)}
                    [SingleEnvVariable env-variable])]]])
        (when @editable?
          [:div {:style {:padding-top 10}}
           [plus ::events/add-env-variable]])]
       :label (@tr [:module-env-variables])
       :count (count @env-variables)
       :default-open true])))


(defn SingleUrl
  [url-map]
  (let [tr              (subscribe [::i18n-subs/tr])
        validate-form?  (subscribe [::subs/validate-form?])
        editable?       (subscribe [::subs/editable?])
        local-validate? (r/atom false)
        {:keys [id ::spec/url-name ::spec/url]} url-map]
    [ui/TableRow {:key id}
     (if @editable?
       [uix/TableRowCell {:key            (str "url-name-" id)
                          :placeholder    (@tr [:name-of-url])
                          :editable?      @editable?
                          :spec           ::spec/url-name
                          :validate-form? @validate-form?
                          :required?      true
                          :default-value  (or url-name "")
                          :width          3
                          :on-change      #(do
                                             (reset! local-validate? true)
                                             (dispatch [::events/update-url-name id %])
                                             (dispatch [::main-events/changes-protection? true])
                                             (dispatch [::events/validate-form]))
                          :on-validation  ::apps-application-events/set-configuration-validation-error}]
       [ui/TableCell {:floated :left, :width 3} url-name])
     (if @editable?
       [uix/TableRowCell {:key            (str "url-url-" id)
                          :placeholder    "url - e.g. http://${hostname}:${tcp.8888}/?token=${jupyter-token}"
                          :editable?      @editable?
                          :spec           ::spec/url
                          :validate-form? @validate-form?
                          :required?      true
                          :default-value  (or url "")
                          :on-change      #(do
                                             (reset! local-validate? true)
                                             (dispatch [::events/update-url-url id %])
                                             (dispatch [::main-events/changes-protection? true])
                                             (dispatch [::events/validate-form]))
                          :on-validation  ::apps-application-events/set-configuration-validation-error}]
       [ui/TableCell {:floated :left, :width 9} url])
     (when @editable?
       [ui/TableCell {:floated :right
                      :width   1
                      :align   :right
                      :style   {}}
        [trash id ::events/remove-url]])]))


(defn UrlsSection []
  (let [tr        (subscribe [::i18n-subs/tr])
        urls      (subscribe [::subs/urls])
        editable? (subscribe [::subs/editable?])]
    (fn []
      [uix/Accordion
       [:<>
        [:div (@tr [:urls])
         [:span ff/nbsp (ff/help-popup (@tr [:module-urls-help]))]]
        (if (empty? @urls)
          [ui/Message
           (str/capitalize (str (@tr [:no-urls]) "."))]
          [:div [ui/Table {:style {:margin-top 10}}
                 [ui/TableHeader
                  [ui/TableRow
                   [ui/TableHeaderCell {:content (str/capitalize (@tr [:name]))}]
                   [ui/TableHeaderCell {:content "URL"}]
                   (when @editable?
                     [ui/TableHeaderCell {:content (str/capitalize (@tr [:action]))}])]]
                 [ui/TableBody
                  (for [[id url-map] @urls]
                    ^{:key (str "url_" id)}
                    [SingleUrl url-map])]]])
        (when @editable?
          [:div {:style {:padding-top 10}}
           [plus ::events/add-url]])]
       :label (@tr [:urls])
       :count (count @urls)
       :default-open true])))


(defn SingleOutputParameter [param]
  (let [tr              (subscribe [::i18n-subs/tr])
        validate-form?  (subscribe [::subs/validate-form?])
        editable?       (subscribe [::subs/editable?])
        local-validate? (r/atom false)
        {:keys [id ::spec/output-parameter-name ::spec/output-parameter-description]} param]
    [ui/TableRow {:key id}
     (if @editable?
       [uix/TableRowCell {:key            (str "output-parameter-name-" id)
                          :placeholder    (@tr [:name])
                          :editable?      @editable?
                          :spec           ::spec/output-parameter-name
                          :validate-form? @validate-form?
                          :required?      true
                          :default-value  (or output-parameter-name "")
                          :width          3
                          :on-change      #(do
                                             (reset! local-validate? true)
                                             (dispatch [::events/update-output-parameter-name id %])
                                             (dispatch [::main-events/changes-protection? true])
                                             (dispatch [::events/validate-form]))
                          :on-validation  ::apps-application-events/set-configuration-validation-error}]
       [ui/TableCell {:floated :left
                      :width   3}
        [:span output-parameter-name]])
     (if @editable?
       [uix/TableRowCell {:key            (str "output-param-description-" id)
                          :placeholder    (@tr [:description])
                          :editable?      @editable?
                          :spec           ::spec/output-parameter-description
                          :validate-form? @validate-form?
                          :required?      true
                          :default-value  (or output-parameter-description "")
                          :on-change      #(do
                                             (reset! local-validate? true)
                                             (dispatch [::events/update-output-parameter-description id %])
                                             (dispatch [::main-events/changes-protection? true])
                                             (dispatch [::events/validate-form]))
                          :on-validation  ::apps-application-events/set-configuration-validation-error}]
       [ui/TableCell {:floated :left
                      :width   12}
        [:span output-parameter-description]])
     (when @editable?
       [ui/TableCell {:floated :right
                      :width   1
                      :align   :right}
        [trash id ::events/remove-output-parameter]])]))


(defn OutputParametersSection []
  (let [tr                (subscribe [::i18n-subs/tr])
        output-parameters (subscribe [::subs/output-parameters])
        editable?         (subscribe [::subs/editable?])]
    (fn []
      [uix/Accordion

       [:<>
        [:div (@tr [:module-output-parameters])
         [:span ff/nbsp (ff/help-popup (@tr [:module-output-parameters-help]))]]
        (if (empty? @output-parameters)
          [ui/Message
           (str/capitalize (str (@tr [:no-output-parameters]) "."))]
          [:div [ui/Table {:style {:margin-top 10}}
                 [ui/TableHeader
                  [ui/TableRow
                   [ui/TableHeaderCell {:content (str/capitalize (@tr [:name]))}]
                   [ui/TableHeaderCell {:content (str/capitalize (@tr [:description]))}]
                   (when @editable?
                     [ui/TableHeaderCell {:content (str/capitalize (@tr [:action]))}])]]
                 [ui/TableBody
                  (for [[id param] @output-parameters]
                    ^{:key (str "out-param_" id)}
                    [SingleOutputParameter param])]]])
        (when @editable?
          [:div {:style {:padding-top 10}}
           [plus ::events/add-output-parameter]])]
       :label (@tr [:module-output-parameters])
       :count (count @output-parameters)
       :default-open true])))


(defn add-data-type-options
  [option]
  (swap! utils-detail/data-type-options conj {:key option :value option :text option}))


(defn SingleDataType
  [_dt]
  (let [tr        (subscribe [::i18n-subs/tr])
        editable? (subscribe [::subs/editable?])]
    (fn [dt]
      (let [{:keys [id ::spec/data-type]} dt]
        [ui/GridRow {:key id}
         [ui/GridColumn {:floated :left
                         :width   2}
          (if @editable?
            [ui/Label
             [ui/Dropdown {:name           (str "data-type-" id)
                           :default-value  (or data-type "text/plain")
                           :allowAdditions true
                           :selection      true
                           :additionLabel  (str (@tr [:add-dropdown]) " ")
                           :search         true
                           :options        @utils-detail/data-type-options
                           :on-add-item    (ui-callback/value #(add-data-type-options %))
                           :on-change      (ui-callback/value
                                             #(do
                                                (dispatch [::main-events/changes-protection? true])
                                                (dispatch [::events/update-data-type id %])
                                                (dispatch [::events/validate-form])))}]]
            [:span [:b data-type]])]
         (when @editable?
           [ui/GridColumn {:floated :right
                           :width   1
                           :align   :right
                           :style   {}}
            [trash id ::events/remove-data-type]])]))))


(defn DataTypesSection []
  (let [tr         (subscribe [::i18n-subs/tr])
        data-types (subscribe [::subs/data-types])
        editable?  (subscribe [::subs/editable?])]
    (fn []
      [uix/Accordion
       [:<>
        [:div (@tr [:module-data-type])
         [:span ff/nbsp (ff/help-popup (@tr [:module-data-type-help]))]]
        (if (empty? @data-types)
          [ui/Message
           (str/capitalize (str (@tr [:no-databindings]) "."))]
          [:div [ui/Grid {:style {:margin-top    5
                                  :margin-bottom 5}}
                 (for [[id dt] @data-types]
                   ^{:key (str "data-type_" id)}
                   [SingleDataType dt])]])
        (when @editable?
          [:div
           [plus ::events/add-data-type]])]
       :label (@tr [:data-binding])
       :count (count @data-types)
       :default-open true])))


(defn single-registry
  [{:keys [id ::spec/registry-id ::spec/registry-cred-id] :as registry}]
  (let [editable?               (subscribe [::subs/editable?])
        form-valid?             (subscribe [::subs/form-valid?])
        validate?               (not @form-valid?)
        registries-options      (subscribe [::subs/private-registries-options])
        registries-cred-options (subscribe [::subs/registries-credentials-options registry-id])]
    [ui/TableRow {:error (and validate?
                              (not (s/valid? ::spec/single-registry registry)))}
     [ui/TableCell {:width 7}
      [ui/Dropdown
       {:selection     true
        :fluid         true
        :default-value registry-id
        :disabled      (not @editable?)
        :options       @registries-options
        :on-change     (ui-callback/value
                         #(do
                            (dispatch [::main-events/changes-protection? true])
                            (dispatch [::events/update-registry-id id %])
                            (dispatch [::events/validate-form])))}]]
     [ui/TableCell {:width 7}
      [ui/Dropdown
       {:selection     true
        :fluid         true
        :default-value registry-cred-id
        :disabled      (not @editable?)
        :options       @registries-cred-options
        :on-change     (ui-callback/value
                         #(do
                            (dispatch [::main-events/changes-protection? true])
                            (dispatch [::events/update-registry-cred-id id %])
                            (dispatch [::events/validate-form])))}]]
     [ui/TableCell {:width 2}
      (when @editable?
        [trash id ::events/remove-registry])]
     ]))


(defn registries-section []
  (let [tr         (subscribe [::i18n-subs/tr])
        editable?  (subscribe [::subs/editable?])
        registries (subscribe [::subs/registries])
        subtype    (subscribe [::subs/module-subtype])]
    (dispatch [::events/get-registries-infra])
    (dispatch [::events/get-registries-credentials])
    (fn []
      (let [no-of-registries (count @registries)]
        [uix/Accordion
         [:<>
          [:div (@tr [:private-registries])
           [:span ff/nbsp (ff/help-popup (@tr [:private-registries-help]))]]
          (if (empty? @registries)
            [ui/Message
             (@tr [:private-registries-not-used])]
            [:div
             [ui/Table {:style {:margin-top 10}}
              [ui/TableHeader
               [ui/TableRow
                [ui/TableHeaderCell
                 {:content (r/as-element [utils/mandatory-name (@tr [:private-registries])])}]
                [ui/TableHeaderCell {:content (str/capitalize (@tr [:credential]))}]
                [ui/TableHeaderCell {:content (str/capitalize (@tr [:action]))}]]]
              [ui/TableBody
               (for [[id registry] @registries]
                 ^{:key (str "registry-" id)}
                 [single-registry registry])]]])
          (when (and @editable? (or (not= @subtype "component")
                                    (and (= @subtype "component")
                                         (zero? no-of-registries))))
            [:div {:style {:padding-top 10}}
             [plus ::events/add-registry]])]
         :label (@tr [:private-registries])
         :count no-of-registries
         :default-open (pos? no-of-registries)]))))


(defn Pricing
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        editable? (subscribe [::subs/editable?])
        price     (subscribe [::subs/price])]
    (fn []
      (let [amount       (:cent-amount-daily @price)
            follow-trial (boolean (:follow-customer-trial @price))]
        [:<>
         (when (and @editable? (nil? amount))
           [ui/Message {:info true}
            (@tr [:define-price])])
         [ui/Input {:labelPosition "right", :type "text"
                    :placeholder   (str/capitalize (@tr [:amount]))
                    :disabled      (not @editable?)
                    :error         (not (s/valid? ::spec/cent-amount-daily amount))}
          [:input {:type          "number"
                   :step          1
                   :min           1
                   :default-value amount
                   :on-change     (ui-callback/input-callback
                                    #(do
                                       (dispatch [::events/cent-amount-daily
                                                  (when-not (str/blank? %)
                                                    (js/parseInt %))])
                                       (dispatch [::main-events/changes-protection? true])
                                       (dispatch [::events/validate-form])))}]
          [ui/Label "ct€/" (@tr [:day])]]
         [:p {:style {:padding-top 10}}
          (@tr [:price-per-month])
          [:b (str
                (if (pos-int? amount)
                  (general-utils/format "%.2f" (* amount 0.3))
                  "...")
                "€/" (str/capitalize (@tr [:month])))]]
         (when @editable?
           [:span
            [ui/Checkbox {:label          (@tr [:follow-customer-trial])
                          :defaultChecked follow-trial
                          :toggle         true
                          :on-change      (ui-callback/checked
                                            #(do
                                               (dispatch [::events/follow-customer-trial %1])
                                               (dispatch [::main-events/changes-protection? true])
                                               (dispatch [::events/validate-form])))}]
            " "
            [ui/Popup {:content (@tr [:follow-customer-trial-help])
                       :trigger (r/as-element [ui/Icon {:name "info circle"}])}]])
         ]))))


(defn price-section
  []
  (let [editable? (subscribe [::subs/editable?])
        price     (subscribe [::subs/price])
        vendor    (subscribe [::profile-subs/vendor])]
    (fn []
      (when (or (and @editable? @vendor) (some? @price))
        [Pricing]))))


(defn licenses->dropdown
  [licenses]
  (map
    (fn [license]
      (let [license-name (:license-name license)]
        {:key license-name, :text license-name, :value license-name}))
    licenses))


(defn LicenseSection []
  (let [tr             (subscribe [::i18n-subs/tr])
        editable?      (subscribe [::subs/editable?])
        validate-form? (subscribe [::subs/validate-form?])
        license        (subscribe [::subs/module-license])
        on-change      (fn [update-event-kw value]
                         (dispatch [update-event-kw value])
                         (dispatch [::main-events/changes-protection? true])
                         (dispatch [::events/validate-form]))
        licenses       (subscribe [::main-subs/config :licenses])
        options        (licenses->dropdown @licenses)
        is-custom?     (r/atom false)
        {:keys [subtype]} @(subscribe [::subs/module])]
    (fn []
      (let [is-editable? (and @editable? @is-custom?)
            {:keys [license-name]} @license]
        [:<>
         (when (not= "component" subtype)
           [:h2 [LicenseTitle]])
         (if (or @editable? (some? @license))
           [ui/Form
            (when @editable?
              [:<>
               [ui/Message {:info true}
                "Choose a license to protect yourself and your users/customers. This is mandatory for published and paying apps."]
               [:div [:p {:style {:padding-bottom 10}} [:b "Choose a known open source license"]]]
               [ui/Dropdown {:options     options
                             :placeholder "Select a license"
                             :search      true
                             :value       (if @is-custom? "" license-name)
                             :selection   true
                             :on-change   (ui-callback/value
                                            (fn [value]
                                              (dispatch [::main-events/changes-protection? true])
                                              (dispatch [::events/set-license value @licenses])
                                              (reset! is-custom? false)))}]
               [:div [:p {:style {:padding "10px 0"}} [:b "Or provide a custom license"]]]
               [ui/Checkbox {:label     (@tr [:custom-license])
                             :checked   @is-custom?
                             :on-change (ui-callback/value
                                          (fn [_]
                                            (dispatch [::main-events/changes-protection? true])
                                            (reset! is-custom? (not @is-custom?))
                                            ))}]
               [ui/Message {:info true}
                (@tr [:license-generator-details])
                ": "
                [:a {:href "https://www.eulatemplate.com/eula-generator/"} (@tr [:license-generator])]]])
            [ui/Table {:compact true, :definition true}
             [ui/TableBody
              [uix/TableRowField (@tr [:name]), :key "license-name", :editable? is-editable?,
               :spec (if @is-custom? ::spec/license-name any?), :validate-form? @validate-form?,
               :required? true, :default-value (:license-name @license),
               :on-change (partial on-change ::events/license-name)
               :on-validation ::apps-application-events/set-license-validation-error]
              [uix/TableRowField (@tr [:description]), :key "license-description",
               :editable? is-editable?, :spec ::spec/license-description, :validate-form? @validate-form?,
               :required? false, :default-value (:license-description @license),
               :on-change (partial on-change ::events/license-description)]
              [uix/TableRowField (@tr [:url]), :key "license-url",
               :editable? is-editable?, :spec (if @is-custom? ::spec/license-url any?), :validate-form? @validate-form?,
               :required? true, :default-value (:license-url @license),
               :on-change (partial on-change ::events/license-url)
               :on-validation ::apps-application-events/set-license-validation-error]]]]
           [ui/Message {:info true}
            (@tr [:license-not-defined])])]))))


(defn AuthorVendorRow
  [label user]
  (let [resolved-user (subscribe [::session-subs/resolve-user user])]
    [ui/TableRow
     [ui/TableCell (str/capitalize label)]
     [ui/TableCell @resolved-user]]))


(defn AuthorVendor
  "Check if the module belongs to a group. If so, search amongst the group. Note that vendor here is a group,
  not a Stripe vendor."
  []
  (let [tr         (subscribe [::i18n-subs/tr])
        module     (subscribe [::subs/module])
        groups     (subscribe [::session-subs/groups])
        is-vendor? (utils/is-vendor? @module)]
    (if is-vendor?
      (let [groups-from-module (utils/module->groups @module)
            group-id           (first groups-from-module)
            group-definition   (acl-utils/find-group group-id @groups)
            vendor             (if group-definition
                                 (second group-definition)
                                 group-id)]
        [AuthorVendorRow (@tr [:vendor]) vendor])
      (let [users-from-module (utils/module->users @module)
            user              (first users-from-module)]
        [AuthorVendorRow (@tr [:author]) user]))))


(defn OverviewDescription
  []
  (let [tr          (subscribe [::i18n-subs/tr])
        editable?   (subscribe [::subs/editable?])
        description (subscribe [::subs/description])
        device      (subscribe [::main-subs/device])
        {:keys [logo-url]} @(subscribe [::subs/module])]
    [ui/Segment {:secondary true
                 :color     "purple"
                 :raised    true
                 :padded    true}
     [ui/Grid {:columns   1
               :stackable true}
      [ui/GridRow {:columns 2}
       [ui/GridColumn {:floated "left"}
        [:h4 (str/capitalize (@tr [:description]))]]
       (when @editable?
         [ui/GridColumn {:style {:text-align "right"}}
          [ui/Button {:icon     "pencil"
                      :compact  true
                      :on-click #(dispatch [::events/set-active-tab :details])}]])]
      [ui/GridRow
       [ui/GridColumn {:textAlign "center"
                       :only      "mobile"}
        [ui/Image {:src   (or logo-url "")
                   :style {:max-width "100%"}}]]
       [ui/GridColumn {:class ["markdown"]}
        [ui/Image {:floated "right"
                   :src     (or logo-url "")
                   :hidden  (when (= :mobile @device) true)
                   :style   {:max-width "25%"}}]
        [ui/ReactMarkdown @description]]]]]))


(defn ShareTitle
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [:<>
     [uix/Icon {:name "users"}]
     (str/capitalize (@tr [:share]))]))


(defn TabAcls
  [e can-edit? edit-event]
  (let [default-value (:acl @e)
        acl           (or default-value
                          (when-let [user-id (and can-edit?
                                                  @(subscribe [::session-subs/active-claim]))]
                            {:owners [user-id]}))
        ui-acl        (r/atom (when acl (acl-utils/acl->ui-acl-format acl)))]
    {:menuItem {:content (r/as-element [ShareTitle])
                :key     :share}
     :pane     {:content (r/as-element
                           [:<>
                            [:h2 [ShareTitle]]
                            ^{:key (:updated @e)}
                            [acl-views/AclWidget {:default-value default-value
                                                  :read-only     (not can-edit?)
                                                  :on-change     edit-event}
                             ui-acl]])
                :key     :share-pane}}))
