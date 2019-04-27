(ns sixsq.nuvla.ui.apps.views-detail
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch dispatch-sync subscribe]]
    [reagent.core :as reagent]
    [sixsq.nuvla.ui.apps.events :as events]
    [sixsq.nuvla.ui.apps.spec :as spec]
    [sixsq.nuvla.ui.apps.subs :as subs]
    [sixsq.nuvla.ui.apps.utils :as utils]
    [sixsq.nuvla.ui.apps.views-versions :as views-versions]
    [sixsq.nuvla.ui.authn.subs :as authn-subs]
    [sixsq.nuvla.ui.cimi.subs :as api-subs]
    [sixsq.nuvla.ui.deployment-dialog.events :as deployment-dialog-events]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.utils.collapsible-card :as cc]
    [sixsq.nuvla.ui.utils.forms :as forms]
    [sixsq.nuvla.ui.utils.resource-details :as resource-details]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [taoensso.timbre :as log]))


(defn refresh-button
  []
  (let [tr            (subscribe [::i18n-subs/tr])
        page-changed? (subscribe [::main-subs/changes-protection?])
        is-new?       (subscribe [::subs/is-new?])]
    (fn []
      [ui/MenuMenu {:position "right"}
       [uix/MenuItemWithIcon
        {:name      (@tr [:refresh])
         :icon-name "refresh"
         :disabled  @is-new?
         :loading?  false                                   ;; FIXME: Add loading flag for module.
         :on-click  #(let [get-module-fn (fn [] (dispatch [::events/get-module]))]
                       (if @page-changed?
                         (dispatch [::main-events/ignore-changes-modal get-module-fn])
                         (get-module-fn)))}]])))


(defn edit-button-disabled?
  [page-changed? form-valid?]
  (or (not page-changed?) (not form-valid?)))


(defn save-callback
  []
  (dispatch-sync [::events/set-validate-form? true])
  (dispatch-sync [::events/validate-form])
  (let [form-valid? (get @re-frame.db/app-db ::spec/form-valid?)]
    (when form-valid?
      (do
        (dispatch [::events/set-validate-form? false])
        (dispatch [::events/is-new? false])
        (dispatch [::events/open-save-modal])))))


(defn control-bar []
  (let [tr            (subscribe [::i18n-subs/tr])
        module        (subscribe [::subs/module])
        is-new?       (subscribe [::subs/is-new?])
        cep           (subscribe [::api-subs/cloud-entry-point])
        form-valid?   (subscribe [::subs/form-valid?])
        page-changed? (subscribe [::main-subs/changes-protection?])]
    (fn []
      (let [launchable?      (not= "PROJECT" (:type @module))
            launch-disabled? (or @is-new? @page-changed?)
            add?             (= "PROJECT" (:type @module))
            add-disabled?    (or @is-new? @page-changed?)
            editable?        (utils/editable? @module @is-new?)]
        (vec (concat [ui/Menu {:borderless true}]

                     (resource-details/format-operations nil @module (:base-uri @cep) nil)

                     [(when launchable?
                        [uix/MenuItemWithIcon
                         {:name      (@tr [:launch])
                          :icon-name "rocket"
                          :disabled  launch-disabled?
                          :on-click  #(dispatch [::deployment-dialog-events/create-deployment (:id @module) :credentials])}])

                      (when add?
                        [uix/MenuItemWithIcon
                         {:name      (@tr [:add])
                          :icon-name "add"
                          :disabled  add-disabled?
                          :on-click  #(dispatch [::events/open-add-modal])}])

                      (when editable?
                        [uix/MenuItemWithIcon
                         {:name      (@tr [:save])
                          :icon-name "save"
                          :disabled  (edit-button-disabled? @page-changed? @form-valid?)
                          :on-click  save-callback}])
                      [refresh-button]]))))))


(defn save-action []
  (let [page-changed? (subscribe [::main-subs/changes-protection?])
        tr            (subscribe [::i18n-subs/tr])
        module        (subscribe [::subs/module])
        form-valid?   (subscribe [::subs/form-valid?])
        is-new?       (subscribe [::subs/is-new?])]
    (fn []
      (let [editable? (utils/editable? @module @is-new?)]
        (when editable?
          [ui/Button {:primary  true
                      :style    {:margin-top 10}
                      :disabled (edit-button-disabled? @page-changed? @form-valid?)
                      :icon     "save"
                      :content  (@tr [:save])
                      :on-click save-callback}])))))


(defn save-modal
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        visible?       (subscribe [::subs/save-modal-visible?])
        username       (subscribe [::authn-subs/user])
        commit-message (subscribe [::subs/commit-message])]
    (fn []
      (let [commit-map {:author @username
                        :commit @commit-message}]
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(do (dispatch [::events/commit-message nil])
                                    (dispatch [::events/close-save-modal]))}

         [ui/ModalHeader (str/capitalize (str (@tr [:save]) " " (@tr [:component])))]

         [ui/ModalContent
          [ui/Input {:placeholder   (@tr [:commit-placeholder])
                     :fluid         true
                     :default-value @commit-message
                     :auto-focus    true
                     :focus         true
                     :on-change     (ui-callback/input-callback #(dispatch [::events/commit-message %]))
                     :on-key-press  (partial forms/on-return-key #(do (dispatch [::events/edit-module commit-map])
                                                                      (dispatch [::events/close-save-modal])
                                                                      (dispatch [::events/commit-message nil])))}]]

         [ui/ModalActions
          [uix/Button {:text     (@tr [:save])
                       :positive true
                       :active   true
                       :on-click #(do (dispatch [::events/edit-module commit-map])
                                      (dispatch [::events/commit-message nil])
                                      (dispatch [::events/close-save-modal]))}]]]))))


(defn logo-url-modal
  []
  (let [local-url (reagent/atom "")
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
                     :on-key-press  (partial forms/on-return-key #(dispatch [::events/save-logo-url @local-url]))}]]

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
      (let [parent  (utils/nav-path->module-path @nav-path)]
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-add-modal])}

         [ui/ModalHeader [ui/Icon {:name "add"}] (@tr [:add])]

         [ui/ModalContent {:scrolling false}
          [ui/CardGroup {:centered true}

           [ui/Card {:on-click #(do (dispatch [::events/close-add-modal])
                                    (dispatch [::history-events/navigate
                                               (str/join "/"
                                                         (remove str/blank?
                                                                 ["apps" parent "New Project?type=project"]))]))}
            [ui/CardContent {:text-align :center}
             [ui/Header "Project"]
             [ui/Icon {:name "folder"
                       :size :massive}]]]

           [ui/Card {:on-click (when parent
                                 #(do
                                    (dispatch [::events/close-add-modal])
                                    (dispatch [::history-events/navigate
                                               (str/join "/"
                                                         (remove str/blank?
                                                                 ["apps" parent "New Component?type=component"]))])))}
            [ui/CardContent {:text-align :center}
             [ui/Header "Component"]
             [:div]
             [ui/Icon {:name  "th"
                       :size  :massive
                       :color (when-not parent :grey)}]]]]]
         [ui/ModalActions]]))))


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
                           :id})


(defn category-icon
  [category]
  (case category
    "PROJECT" "folder"
    "APPLICATION" "sitemap"
    "IMAGE" "file"
    "COMPONENT" "microchip"
    "question circle"))


(defn details-section
  []
  (let [module (subscribe [::subs/module])]
    (fn []
      (let [summary-info (-> (select-keys @module module-summary-keys)
                             (merge (select-keys @module #{:path :type})
                                    {:owners (->> @module :acl :owners (str/join ", "))}
                                    {:author (->> @module :content :author)}))
            icon         (-> @module :type category-icon)
            rows         (map tuple-to-row summary-info)
            name         (:name @module)
            description  (:name @module)
            acl          (:acl @module)]
        [cc/metadata-simple
         {:title       name
          :description (:startTime summary-info)
          :icon        icon
          :subtitle    description
          :acl         acl}
         rows]))))


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


(defn toggle [v]
  (swap! v not))


(defn summary-row
  [key name-kw value on-change-event editable? mandatory? value-spec]
  (let [tr              (subscribe [::i18n-subs/tr])
        active-input    (subscribe [::subs/active-input])
        local-validate? (reagent/atom false)
        validate-form?     (subscribe [::subs/validate-form?])]
    (fn [key name-kw value on-change-event editable? mandatory? value-spec]
      (let [name-str      (name name-kw)
            name-label    (if (and editable? mandatory?) (utils/mandatory-name name-str) name-str)
            input-active? (= name-str @active-input)
            validate?     (or @local-validate? @validate-form?)
            valid?        (s/valid? value-spec value)]
        [ui/TableRow
         [ui/TableCell {:collapsing true}
          name-label]
         [ui/TableCell
          (if editable?
            ^{:key key}
            [ui/Input {:default-value value
                       :placeholder   (str/capitalize (@tr [name-kw]))
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
                                           (dispatch [::events/validate-form])
                                           (dispatch [on-change-event %])))}]
            [:span value])]]))))


(defn summary
  [extras]
  (let [tr               (subscribe [::i18n-subs/tr])
        default-logo-url (subscribe [::subs/default-logo-url])
        is-new?          (subscribe [::subs/is-new?])
        module           (subscribe [::subs/module])
        module-common    (subscribe [::subs/module-common])]
    (fn [extras]
      (let [editable? (utils/editable? @module @is-new?)
            {name        ::spec/name
             parent      ::spec/parent-path
             description ::spec/description
             logo-url    ::spec/logo-url
             type        ::spec/type
             path        ::spec/path
             :or         {name        ""
                          parent      ""
                          description ""
                          logo-url    @default-logo-url
                          type        "project"
                          path        nil}} @module-common]
        [ui/Grid {:style {:margin-bottom 5}}
         [ui/GridRow {:reversed :computer}
          [ui/GridColumn {:computer     2
                          :large-screen 2}
           [ui/Image {:src (or logo-url @default-logo-url)}]
           (when editable?
             [ui/Button {:fluid    true
                         :on-click #(dispatch [::events/open-logo-url-modal])}
              (@tr [:module-change-logo])])]
          [ui/GridColumn {:computer     14
                          :large-screen 14}
           [ui/Table (assoc style/definition :class :nuvla-ui-editable)
            [ui/TableBody
             [summary-row (str parent "-name")
              :name name ::events/name editable? true ::spec/name]
             [summary-row (str parent "-description")
              :description description ::events/description editable? true ::spec/description]
             (when (not-empty parent)
               (let [label (if (= "PROJECT" type) "parent project" "project")]
                 [ui/TableRow
                  [ui/TableCell {:collapsing true
                                 :style      {:padding-bottom 8}} label]
                  [ui/TableCell {:style {:padding-left (when editable? 24)}} parent]]))
             (for [x extras]
               x)]]

           (when (not @is-new?)
             [details-section])
           [views-versions/versions]]]]))))
