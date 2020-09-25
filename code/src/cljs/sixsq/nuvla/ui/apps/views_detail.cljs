(ns sixsq.nuvla.ui.apps.views-detail
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch dispatch-sync subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.apps.events :as events]
    [sixsq.nuvla.ui.apps.spec :as spec]
    [sixsq.nuvla.ui.apps.subs :as subs]
    [sixsq.nuvla.ui.apps.utils :as utils]
    [sixsq.nuvla.ui.apps.views-versions :as views-versions]
    [sixsq.nuvla.ui.deployment-dialog.events :as deployment-dialog-events]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.intercom.events :as intercom-events]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.session.subs :as session-subs]
    [sixsq.nuvla.ui.utils.collapsible-card :as cc]
    [sixsq.nuvla.ui.utils.form-fields :as ff]
    [sixsq.nuvla.ui.utils.forms :as forms]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.profile.events :as profile-events]
    [sixsq.nuvla.ui.profile.subs :as profile-subs]
    [sixsq.nuvla.ui.profile.views :as profile-views]))


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
      (do
        (dispatch [::events/set-validate-form? false])
        (if (= (or subtype new-subtype) "project")
          (dispatch [::events/edit-module nil])
          (dispatch [::events/open-save-modal]))))))


(defn DeleteButton
  [module]
  (let [tr      (subscribe [::i18n-subs/tr])
        {:keys [id name description]} module
        content (str (or name id) (when description " - ") description)]
    [uix/ModalDanger
     {:on-confirm  #(dispatch [::events/delete-module id])
      :trigger     (r/as-element [ui/MenuItem
                                  [ui/Icon {:name "trash"}]
                                  (@tr [:delete])])
      :content     [:h3 content]
      :header      (@tr [:delete-module])
      :danger-msg  (@tr [:module-delete-warning])
      :button-text (@tr [:delete])}]))


(defn MenuBar []
  (let [tr            (subscribe [::i18n-subs/tr])
        module        (subscribe [::subs/module])
        is-new?       (subscribe [::subs/is-new?])
        page-changed? (subscribe [::main-subs/changes-protection?])]
    (fn []
      (let [subtype          (:subtype @module)
            launchable?      (and subtype (not= "project" subtype))
            launch-disabled? (or @is-new? @page-changed?)
            add?             (= "project" (:subtype @module))
            add-disabled?    (or @is-new? @page-changed?)
            id               (:id @module)]

        [ui/Menu {:borderless true}

         (when launchable?
           [uix/MenuItemWithIcon
            {:name      (@tr [:launch])
             :icon-name "rocket"
             :disabled  launch-disabled?
             :on-click  #(dispatch [::main-events/subscription-required-dispatch
                                    [::deployment-dialog-events/create-deployment
                                     id :infra-services]])}])

         (when add?
           [uix/MenuItemWithIcon
            {:name      (@tr [:add])
             :icon-name "add"
             :disabled  add-disabled?
             :on-click  #(dispatch [::events/open-add-modal])}])

         (when (general-utils/can-delete? @module)
           [DeleteButton @module])

         [main-components/RefreshMenu
          {:refresh-disabled? @is-new?
           :on-refresh        #(let [get-module-fn (fn [] (dispatch [::events/get-module]))]
                                 (if @page-changed?
                                   (dispatch [::main-events/ignore-changes-modal get-module-fn])
                                   (get-module-fn)))}]]))))


(defn save-action []
  (let [page-changed? (subscribe [::main-subs/changes-protection?])
        tr            (subscribe [::i18n-subs/tr])
        form-valid?   (subscribe [::subs/form-valid?])
        editable?     (subscribe [::subs/editable?])]
    (fn []
      (when @editable?
        [ui/Button {:primary  true
                    :style    {:margin-top 10}
                    :disabled (edit-button-disabled? @page-changed? @form-valid?)
                    :icon     "save"
                    :content  (@tr [:save])
                    :on-click save-callback}]))))


(defn save-modal
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        visible?       (subscribe [::subs/save-modal-visible?])
        username       (subscribe [::session-subs/user])
        commit-message (subscribe [::subs/commit-message])
        need-commit?   (subscribe [::subs/module-content-updated?])]
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

         [ui/ModalHeader (str/capitalize (str (@tr [:save]) " " (@tr [:component])))]

         [ui/ModalContent
          (if @need-commit?
            [ui/Input {:placeholder   (@tr [:commit-placeholder])
                       :fluid         true
                       :default-value @commit-message
                       :auto-focus    true
                       :focus         true
                       :on-change     (ui-callback/input-callback
                                        #(dispatch [::events/commit-message %]))
                       :on-key-press  (partial forms/on-return-key save-fn)}]
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
      (let []
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-logo-url-modal])}

         [ui/ModalHeader (@tr [:select-logo-url])]

         [ui/ModalContent
          [ui/Input {:default-value (or (:logo-url @module) "")
                     :placeholder   (@tr [:logo-url-placeholder])
                     :fluid         true
                     :auto-focus    true
                     :on-change     (ui-callback/input-callback #(reset! local-url %))
                     :on-key-press  (partial forms/on-return-key
                                             #(dispatch [::events/save-logo-url @local-url]))}]]

         [ui/ModalActions
          [uix/Button {:text     "Ok"
                       :positive true
                       :disabled (empty? @local-url)
                       :active   true
                       :on-click #(dispatch [::events/save-logo-url @local-url])}]]]))))


(defn add-modal
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        visible? (subscribe [::subs/add-modal-visible?])
        nav-path (subscribe [::main-subs/nav-path])]
    (fn []
      (let [parent (utils/nav-path->module-path @nav-path)]
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-add-modal])}

         [ui/ModalHeader [ui/Icon {:name "add"}] (@tr [:add])]

         [ui/ModalContent {:scrolling false}
          [ui/CardGroup {:centered true}

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
                                                      "New Component?subtype=component"]))])))}
            [ui/CardContent {:text-align :center}
             [ui/Header "Component"]
             [:div]
             [ui/Icon {:name  "grid layout"
                       :size  :massive
                       :color (when-not parent :grey)}]]]

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
             [ui/Header "Application"]
             [:div]
             [ui/Icon {:name  "cubes"
                       :size  :massive
                       :color (when-not parent :grey)}]]]
           ]]]))))


(defn version-warning []
  (let [version-warning? (subscribe [::subs/version-warning?])]
    (fn []
      (let []
        [ui/Message {:hidden  (not @version-warning?)
                     :warning true}
         [ui/MessageHeader "Warning!"]
         [ui/MessageContent "This is not the latest version. Click or tap "
          [:a {:on-click #(dispatch [::events/get-module])
               :style    {:cursor :pointer}} "here"]
          " to load the latest."]]))))


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
  [error]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [error]
      (when (instance? js/Error error)
        [ui/Container
         [ui/Header {:as "h3", :icon true}
          [ui/Icon {:name "warning sign"}]
          (error-text tr error)]]))))


(defn summary
  [extras]
  (let [tr               (subscribe [::i18n-subs/tr])
        default-logo-url (subscribe [::subs/default-logo-url])
        is-new?          (subscribe [::subs/is-new?])
        module-common    (subscribe [::subs/module-common])
        editable?        (subscribe [::subs/editable?])
        validate-form?   (subscribe [::subs/validate-form?])
        on-change        (fn [update-event-kw value]
                           (dispatch [update-event-kw value])
                           (dispatch [::main-events/changes-protection? true])
                           (dispatch [::events/validate-form]))]
    (fn [extras]
      (let [{name        ::spec/name
             parent      ::spec/parent-path
             description ::spec/description
             logo-url    ::spec/logo-url
             subtype     ::spec/subtype
             path        ::spec/path
             :or         {name        ""
                          parent      ""
                          description ""
                          logo-url    @default-logo-url
                          subtype     "project"
                          path        nil}} @module-common]
        [ui/Grid {:stackable true, :reversed :mobile}
         [ui/GridColumn {:width 13}
          [ui/Table {:compact    true
                     :definition true}
           [ui/TableBody

            [uix/TableRowField (@tr [:name]), :key (str parent "-name"), :editable? @editable?,
             :spec ::spec/name, :validate-form? @validate-form?, :required? true,
             :default-value name, :on-change (partial on-change ::events/name)]

            [uix/TableRowField (@tr [:description]), :key (str parent "-description"),
             :editable? @editable?, :spec ::spec/description, :validate-form? @validate-form?,
             :required? true, :default-value description,
             :on-change (partial on-change ::events/description)]

            (when (not-empty parent)
              (let [label (if (= "project" subtype) "parent project" "project")]
                [ui/TableRow
                 [ui/TableCell {:collapsing true
                                :style      {:padding-bottom 8}} label]
                 [ui/TableCell {:style {:padding-left (when @editable? 24)}} parent]]))
            (for [x extras]
              x)]]

          (when (not @is-new?)
            [details-section])
          [views-versions/versions]]
         [ui/GridColumn {:width 3 :floated "right"}
          [ui/Image {:src (or logo-url @default-logo-url)}]
          (when @editable?
            [ui/Button {:fluid    true
                        :on-click #(dispatch [::events/open-logo-url-modal])}
             (@tr [:module-change-logo])])]]))))


(defn input
  [id name value placeholder update-event value-spec fluid?]
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


(defn single-env-variable
  [env-variable]
  (let [tr              (subscribe [::i18n-subs/tr])
        form-valid?     (subscribe [::subs/form-valid?])
        editable?       (subscribe [::subs/editable?])
        local-validate? (r/atom false)
        {:keys [id
                ::spec/env-name
                ::spec/env-description
                ::spec/env-value
                ::spec/env-required]} env-variable
        validate?       (or @local-validate? (not @form-valid?))]
    [ui/TableRow {:key id}
     [ui/TableCell {:floated :left
                    :width   3}
      (if @editable?
        (let [input-name (str name "-" id)]
          [ui/Input {:name          (str name "-" id)
                     :placeholder   (@tr [:name])
                     :type          :text
                     :default-value (or env-name "")
                     :error         (when (and validate?
                                               (not (s/valid? ::spec/env-name env-name))) true)
                     :fluid         true
                     :onMouseEnter  #(dispatch [::events/active-input input-name])
                     :onMouseLeave  #(dispatch [::events/active-input nil])
                     :on-change     (ui-callback/input-callback
                                      #(do
                                         (reset! local-validate? true)
                                         (dispatch [::events/update-env-name id %])
                                         (dispatch [::main-events/changes-protection? true])
                                         (dispatch [::events/validate-form])))}])

        [:span env-name])]

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


(defn env-variables-section []
  (let [tr            (subscribe [::i18n-subs/tr])
        env-variables (subscribe [::subs/env-variables])
        editable?     (subscribe [::subs/editable?])]
    (fn []
      [uix/Accordion
       [:<>
        [:div (str/capitalize (@tr [:env-variables]))
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
                    [single-env-variable env-variable])]]])
        (when @editable?
          [:div {:style {:padding-top 10}}
           [plus ::events/add-env-variable]])]
       :label (@tr [:module-env-variables])
       :count (count @env-variables)])))


(defn single-url
  [url-map]
  (let [editable? (subscribe [::subs/editable?])
        {:keys [id ::spec/url-name ::spec/url]} url-map]
    [ui/TableRow {:key id}
     [ui/TableCell {:floated :left
                    :width   2}
      (if @editable?
        [input id (str "url-name-" id) url-name
         "name of this url" ::events/update-url-name ::spec/url-name false]
        [:span url-name])]
     [ui/TableCell {:floated :left
                    :width   13}
      (if @editable?
        [input id (str "url-url-" id) url
         "url - e.g. http://${hostname}:${tcp.8888}/?token=${jupyter-token}"
         ::events/update-url-url ::spec/url true]
        [:span url])]
     (when @editable?
       [ui/TableCell {:floated :right
                      :width   1
                      :align   :right
                      :style   {}}
        [trash id ::events/remove-url]])]))


(defn urls-section []
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
                    [single-url url-map])]]])
        (when @editable?
          [:div {:style {:padding-top 10}}
           [plus ::events/add-url]])]
       :label (@tr [:urls])
       :count (count @urls)
       :default-open false])))


(defn single-output-parameter [param]
  (let [tr        (subscribe [::i18n-subs/tr])
        editable? (subscribe [::subs/editable?])
        {:keys [id ::spec/output-parameter-name ::spec/output-parameter-description]} param]
    [ui/TableRow {:key id}
     [ui/TableCell {:floated :left
                    :width   2}
      (if @editable?
        [input id (str "output-param-name-" id) output-parameter-name
         (@tr [:name]) ::events/update-output-parameter-name
         ::spec/output-parameter-name false]
        [:span output-parameter-name])]
     [ui/TableCell {:floated :left
                    :width   13}
      (if @editable?
        [input id (str "output-param-description-" id)
         output-parameter-description (@tr [:description])
         ::events/update-output-parameter-description
         ::spec/output-parameter-description true]
        [:span output-parameter-description])]
     (when @editable?
       [ui/TableCell {:floated :right
                      :width   1
                      :align   :right}
        [trash id ::events/remove-output-parameter]])]))


(defn output-parameters-section []
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
                    [single-output-parameter param])]]])
        (when @editable?
          [:div {:style {:padding-top 10}}
           [plus ::events/add-output-parameter]])]
       :label (@tr [:module-output-parameters])
       :count (count @output-parameters)
       :default-open false])))


(def data-type-options
  (atom [{:key "application/x-hdr", :value "application/x-hdr", :text "application/x-hdr"}
         {:key "application/x-clk", :value "application/x-clk", :text "application/x-clk"}
         {:key "text/plain", :value "text/plain", :text "text/plain"}]))


(defn add-data-type-options
  [option]
  (swap! data-type-options conj {:key option :value option :text option}))


(defn single-data-type [dt]
  (let [editable? (subscribe [::subs/editable?])]
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
                           :additionLabel  "Additional data type: "
                           :search         true
                           :options        @data-type-options
                           :on-add-item    #(add-data-type-options (-> % .-target .-value))
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


(defn data-types-section []
  (let [tr         (subscribe [::i18n-subs/tr])
        data-types (subscribe [::subs/data-types])
        editable?  (subscribe [::subs/editable?])]
    (fn []
      [uix/Accordion
       [:<>
        [:div (@tr [:data-type])
         [:span ff/nbsp (ff/help-popup (@tr [:module-data-type-help]))]]
        (if (empty? @data-types)
          [ui/Message
           (str/capitalize (str (@tr [:no-datasets]) "."))]
          [:div [ui/Grid {:style {:margin-top    5
                                  :margin-bottom 5}}
                 (for [[id dt] @data-types]
                   ^{:key (str "data-type_" id)}
                   [single-data-type dt])]])
        (when @editable?
          [:div
           [plus ::events/add-data-type]])]
       :label (@tr [:data-binding])
       :count (count @data-types)
       :default-open false])))


(defn single-registry
  [{:keys [id ::spec/registry-id ::spec/registry-cred-id] :as registry}]
  (let [editable?               (subscribe [::subs/editable?])
        form-valid?             (subscribe [::subs/form-valid?])
        validate?               (not @form-valid?)
        registries-options      (subscribe [::subs/private-registries-options])
        registries-cred-options (subscribe [::subs/registries-credentials-options registry-id])]
    [ui/TableRow {:error (and validate?
                              (not (s/valid? ::spec/single-registry registry)))}
     [ui/TableCell {:floated :left}
      [ui/Dropdown
       {:selection     true
        :default-value registry-id
        :disabled      (not @editable?)
        :options       @registries-options
        :on-change     (ui-callback/value
                         #(do
                            (dispatch [::main-events/changes-protection? true])
                            (dispatch [::events/update-registry-id id %])
                            (dispatch [::events/validate-form])))}]]
     [ui/TableCell {:floated :left}
      [ui/Dropdown
       {:selection     true
        :default-value registry-cred-id
        :disabled      (not @editable?)
        :options       @registries-cred-options
        :on-change     (ui-callback/value
                         #(do
                            (dispatch [::main-events/changes-protection? true])
                            (dispatch [::events/update-registry-cred-id id %])
                            (dispatch [::events/validate-form])))}]]
     (when @editable?
       [ui/TableCell {:floated :right
                      :align   :right}
        [trash id ::events/remove-registry]])]))


(defn registries-section []
  (let [tr         (subscribe [::i18n-subs/tr])
        editable?  (subscribe [::subs/editable?])
        registries (subscribe [::subs/registries])
        subtype    (subscribe [::subs/module-subtype])]
    (dispatch [::events/get-registries-infra])
    (dispatch [::events/get-registries-credentials])
    (fn []
      [uix/Accordion
       [:<>
        [:div "Private registries"
         [:span ff/nbsp (ff/help-popup "Private registries help")]]
        (if (empty? @registries)
          [ui/Message
           "No private registries used"]
          [:div
           [ui/Table {:style {:margin-top 10}}
            [ui/TableHeader
             [ui/TableRow
              [ui/TableHeaderCell {:content (r/as-element [utils/mandatory-name "Private registry"])}]
              [ui/TableHeaderCell {:content "Credential"}]
              (when @editable?
                [ui/TableHeaderCell {:content (str/capitalize (@tr [:action]))}])]]
            [ui/TableBody
             (for [[id registry] @registries]
               ^{:key (str "registry-" id)}
               [single-registry registry])]]])
        (when (and @editable? (or (not= @subtype "component")
                                  (and (= @subtype "component")
                                       (zero? (count @registries)))))
          [:div {:style {:padding-top 10}}
           [plus ::events/add-registry]])]
       :label "Private registries"
       :count (count @registries)])))


(defn price-section []
  (let [tr        (subscribe [::i18n-subs/tr])
        editable? (subscribe [::subs/editable?])
        price     (subscribe [::subs/price])]
    (fn []
      (let [amount (:cent-amount-daily @price)]
        (when (or @editable? (some? @price))
          [uix/Accordion
           [:<>
            [:div (str/capitalize (@tr [:price]))
             [:span ff/nbsp (ff/help-popup (@tr [:define-price]))]]
            (if @editable?
              [ui/Input {:labelPosition "right", :type "text"
                         :placeholder   (str/capitalize (@tr [:amount]))
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
              [ui/Label (str amount "ct€/" (@tr [:day]))]
              )
            [:p (@tr [:price-per-month])
             [:b (str
                   (if (pos-int? amount)
                     (general-utils/format "%.2f" (* amount 0.3))
                     "...")
                   "€/" (str/capitalize (@tr [:month])))]]]
           :label (str/capitalize (@tr [:price]))
           :count (if (>= amount 100)
                    (str (float (/ amount 100)) "€/" (@tr [:day]))
                    (str amount "ct€/" (@tr [:day])))
           :default-open false])))))


(defn license-section []
  (let [tr             (subscribe [::i18n-subs/tr])
        editable?      (subscribe [::subs/editable?])
        validate-form? (subscribe [::subs/validate-form?])
        license        (subscribe [::subs/module-license])
        on-change      (fn [update-event-kw value]
                         (dispatch [update-event-kw value])
                         (dispatch [::main-events/changes-protection? true])
                         (dispatch [::events/validate-form]))
        sixsq-license  {:license-name        "SixSq"
                        :license-description "SixSq license"
                        :license-url         "https://sixsq.com/license"}] ;; FIXME
    (fn []
      (let [sixsq-selected? (= (:license-name @license)
                               (:license-name sixsq-license))
            is-editable?    (and @editable? (not sixsq-selected?))]
        (when (or @editable? (some? @license))
          [uix/Accordion
           [ui/Form
            (when @editable?
              [:<>
               [ui/FormField [:b (@tr [:select-license])]]
               [ui/FormRadio {:label     (@tr [:sixsq-license])
                              :checked   sixsq-selected?
                              :on-change #(do
                                            (on-change ::events/license-name
                                                       (:license-name sixsq-license))
                                            (on-change ::events/license-description
                                                       (:license-description sixsq-license))
                                            (on-change ::events/license-url
                                                       (:license-url sixsq-license)))}]
               [ui/FormRadio {:label     (@tr [:custom-license])
                              :checked   (and (some? (:license-name @license))
                                              (not= (:license-name @license)
                                                    (:license-name sixsq-license)))
                              :on-change #(do (on-change ::events/license-name "")
                                              (on-change ::events/license-description "")
                                              (on-change ::events/license-url ""))}]])
            [ui/FormField
             [ui/Table {:compact true, :definition true}
              [ui/TableBody
               [uix/TableRowField (@tr [:name]), :key "license-name", :editable? is-editable?,
                :spec ::spec/license-name, :validate-form? @validate-form?,
                :required? true, :default-value (:license-name @license),
                :on-change (partial on-change ::events/license-name)]
               [uix/TableRowField (@tr [:description]), :key "license-description",
                :editable? is-editable?, :spec ::spec/license-description, :validate-form? @validate-form?,
                :required? false, :default-value (:license-description @license),
                :on-change (partial on-change ::events/license-description)]
               [uix/TableRowField (@tr [:url]), :key "license-url",
                :editable? is-editable?, :spec ::spec/license-url, :validate-form? @validate-form?,
                :required? true, :default-value (:license-url @license),
                :on-change (partial on-change ::events/license-url)]]]]]
           :label (str/capitalize (@tr [:license]))
           :count (:license-name @license)
           :default-open false])))))
