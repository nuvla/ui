(ns sixsq.nuvla.ui.apps.views-detail
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as reagent]
    [sixsq.nuvla.ui.apps.events :as events]
    [sixsq.nuvla.ui.apps.subs :as subs]
    [sixsq.nuvla.ui.apps.utils :as utils]
    [sixsq.nuvla.ui.apps.views-versions :as views-versions]
    [sixsq.nuvla.ui.authn.subs :as authn-subs]
    [sixsq.nuvla.ui.cimi.subs :as api-subs]
    [sixsq.nuvla.ui.deployment-dialog.events :as deployment-dialog-events]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.utils.collapsible-card :as cc]
    [sixsq.nuvla.ui.utils.forms :as forms]
    [sixsq.nuvla.ui.utils.resource-details :as resource-details]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn refresh-button
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn []
      [ui/MenuMenu {:position "right"}
       [uix/MenuItemWithIcon
        {:name      (@tr [:refresh])
         :icon-name "refresh"
         :loading?  false                                   ;; FIXME: Add loading flag for module.
         :on-click  #(dispatch [::events/get-module])}]])))


(defn control-bar []
  (let [tr (subscribe [::i18n-subs/tr])
        module (subscribe [::subs/module])
        cep (subscribe [::api-subs/cloud-entry-point])
        page-changed? (subscribe [::subs/page-changed?])]
    (let [add-disabled? (not= "PROJECT" (:type @module))
          deploy-disabled? (= "PROJECT" (:type @module))]
      (vec (concat [ui/Menu {:borderless true}]

                   (resource-details/format-operations nil @module (:base-uri @cep) nil)

                   [
                    [uix/MenuItemWithIcon
                     {:name      (@tr [:launch])
                      :icon-name "rocket"
                      :disabled  deploy-disabled?
                      :on-click  #(dispatch [::deployment-dialog-events/create-deployment (:id @module) :credentials])}]

                    (when (not add-disabled?)
                      [uix/MenuItemWithIcon
                       {:name      (@tr [:add])
                        :icon-name "add"
                        :disabled  add-disabled?
                        :on-click  #(dispatch [::events/open-add-modal])}])

                    [uix/MenuItemWithIcon
                     {:name      (@tr [:save])
                      :icon-name "save"
                      :disabled  (not @page-changed?)
                      :on-click  #(dispatch [::events/open-save-modal])}]

                    [refresh-button]])))))


(defn save-action []
  (let [page-changed? (subscribe [::subs/page-changed?])
        tr (subscribe [::i18n-subs/tr])]
    (fn []
      [ui/Button {:primary  true
                  :style    {:margin-top 10}
                  :disabled (not @page-changed?)
                  :icon     "save"
                  :content  (@tr [:save])
                  :on-click #(dispatch [::events/open-save-modal])}])))


(defn save-modal
  []
  (let [tr (subscribe [::i18n-subs/tr])
        visible? (subscribe [::subs/save-modal-visible?])
        username (subscribe [::authn-subs/user])
        commit-message (subscribe [::subs/commit-message])]
    (fn []
      (let [commit-map {:author @username
                        :commit @commit-message}]
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-save-modal])}

         [ui/ModalHeader (str/capitalize (str (@tr [:save]) " " (@tr [:component])))]

         [ui/ModalContent
          [ui/Input {:placeholder  (@tr [:commit-placeholder])
                     :fluid        true
                     :auto-focus   true
                     :on-change    (ui-callback/input-callback #(dispatch [::events/commit-message %]))
                     :on-key-press (partial forms/on-return-key #(do (dispatch [::events/edit-module commit-map])
                                                                     (dispatch [::events/close-save-modal])))}]]

         [ui/ModalActions
          [uix/Button {:text     (@tr [:save])
                       :positive true
                       :active   true
                       :on-click #(do (dispatch [::events/edit-module commit-map])
                                      (dispatch [::events/close-save-modal])
                                      )}]]]))))


(defn logo-url-modal
  []
  (let [local-url (reagent/atom "")
        tr (subscribe [::i18n-subs/tr])
        visible? (subscribe [::subs/logo-url-modal-visible?])
        module (subscribe [::subs/module])]
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
                       :on-click #(dispatch [::events/save-logo-url @local-url])}]]
         ]))))


(defn add-modal
  []
  (let [tr (subscribe [::i18n-subs/tr])
        visible? (subscribe [::subs/add-modal-visible?])
        nav-path (subscribe [::main-subs/nav-path])]
    (fn []
      (let [parent (utils/nav-path->module-path @nav-path)
            hide-fn #(dispatch [::events/close-add-modal])]
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   hide-fn}

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
                       :size :massive
                       }]]]

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
                       :color (when-not parent :grey)
                       }]]]]]
         [ui/ModalActions]]))))


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
                                    {:owners (->> @module :acl :owners (str/join ", "))}))
            icon (-> @module :type category-icon)
            rows (map tuple-to-row summary-info)
            name (:name @module)
            description (:name @module)
            acl (:acl @module)]
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
  [name-kw value on-change-event]
  (let [tr (subscribe [::i18n-subs/tr])
        name-str (name name-kw)]
    [ui/TableRow
     [ui/TableCell {:collapsing true
                    :style      {:padding-bottom 8}} name-str]
     [ui/TableCell
      [ui/Input {:name        name-str
                 :value       value
                 :transparent true
                 :placeholder (str/capitalize (@tr [name-kw]))
                 :fluid       true
                 :on-change   (ui-callback/input-callback #(do (dispatch [::events/page-changed? true])
                                                               (dispatch [on-change-event %])))
                 }]]]))


(defn summary
  [extras]
  (let [tr (subscribe [::i18n-subs/tr])
        default-logo-url (subscribe [::subs/default-logo-url])
        is-new? (subscribe [::subs/is-new?])]
    (fn [extras]
      (let [module (subscribe [::subs/module])
            {name        :name
             parent      :parent-path
             description :description
             logo-url    :logo-url
             type        :type
             path        :path
             :or         {name        ""
                          parent      ""
                          description ""
                          logo-url    @default-logo-url
                          type        "project"
                          path        nil}} @module]
        [ui/Grid {:style {:margin-bottom 5}}
         [ui/GridRow {:reversed :computer}
          [ui/GridColumn {:computer     2
                          :large-screen 2}
           [ui/Image {:src (or logo-url @default-logo-url)}]
           [ui/Button {:fluid    true
                       :on-click #(dispatch [::events/open-logo-url-modal])}
            (@tr [:module-change-logo])]]
          [ui/GridColumn {:computer     14
                          :large-screen 14}
           [ui/Table (update-in style/definition [:style :max-width] (constantly "100%"))
            [ui/TableBody
             [summary-row :name name ::events/name]
             [summary-row :description description ::events/description]
             (when (not-empty parent)
               (let [label (if (= "PROJECT" type) "parent project" "project")]
                 [ui/TableRow
                  [ui/TableCell {:collapsing true
                                 :style      {:padding-bottom 8}} label]
                  [ui/TableCell parent]]))
             extras]]
           (when (not @is-new?)
             [details-section])
           [views-versions/versions]
           ]]]))))
