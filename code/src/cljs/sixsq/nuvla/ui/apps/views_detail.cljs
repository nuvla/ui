(ns sixsq.nuvla.ui.apps.views-detail
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as reagent]
    [sixsq.nuvla.ui.apps.events :as events]
    [sixsq.nuvla.ui.apps.subs :as subs]
    [sixsq.nuvla.ui.apps.utils :as utils]
    [sixsq.nuvla.ui.cimi.subs :as api-subs]
    [sixsq.nuvla.ui.deployment-dialog.events :as deployment-dialog-events]
    [sixsq.nuvla.ui.history.views :as history]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.collapsible-card :as cc]
    [sixsq.nuvla.ui.utils.resource-details :as resource-details]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [taoensso.timbre :as log]
    [taoensso.timbre :as timbre]
    [cemerick.url :as url]))


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
  (let [tr            (subscribe [::i18n-subs/tr])
        module        (subscribe [::subs/module])
        cep           (subscribe [::api-subs/cloud-entry-point])
        page-changed? (subscribe [::subs/page-changed?])]
    (let [add-disabled?    (not= "PROJECT" (:type @module))
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


(defn form-input-callback
  [path]
  (ui-callback/value #(dispatch [::events/update-add-data path %])))


(defn kw->icon-name
  [kw]
  (-> kw name str/upper-case utils/category-icon))


(defn pane
  [tr kw element]
  {:menuItem {:key     (name kw)
              :icon    (kw->icon-name kw)
              :content (@tr [kw])}
   :render   (fn [] (reagent/as-element [element]))})


(defn index->kw
  [index]
  (case index
    0 :project
    1 :image
    2 :component
    3 :application
    :project))


(defn save-action []
  (let [page-changed? (subscribe [::subs/page-changed?])
        tr            (subscribe [::i18n-subs/tr])]
    (fn []
      [ui/Button {:primary  true
                  :style    {:margin-top 10}
                  :disabled (not @page-changed?)
                  :icon     "save"
                  :content  (@tr [:save])
                  :on-click #(dispatch [::events/open-save-modal])}])))


(defn save-modal
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        visible? (subscribe [::subs/save-modal-visible?])]
    (fn []
      (let []
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   #(dispatch [::events/close-save-modal])}

         [ui/ModalHeader (str/capitalize (str (@tr [:save]) " " (@tr [:component])))]

         [ui/ModalContent
          [ui/Input {:placeholder  (@tr [:commit-placeholder])
                     :fluid        true
                     :auto-focus   true
                     :on-change    (ui-callback/input-callback #(dispatch [::events/commit-message %]))
                     :on-key-press (fn [e]
                                     (when (= 13 (.-charCode e))
                                       (do (dispatch [::events/edit-module])
                                           (dispatch [::events/close-save-modal])
                                           )))}]]

         [ui/ModalActions
          [uix/Button {:text     (@tr [:save])
                       :positive true
                       :active   true
                       :on-click #(do (dispatch [::events/edit-module])
                                      (dispatch [::events/close-save-modal])
                                      )}]]]))))


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
                     :on-key-press  (fn [e]
                                      (when (= 13 (.-charCode e))
                                        (dispatch [::events/save-logo-url @local-url])))}]]

         [ui/ModalActions
          [uix/Button {:text         "Ok"
                       :positive     true
                       :disabled     (empty? @local-url)
                       :active       true
                       :on-click     #(dispatch [::events/save-logo-url @local-url])
                       :on-key-press (fn [e]
                                       (if (= 13 (.-charCode e))
                                         (log/infof "Button ENTER")
                                         (log/infof "Button NOT ENTER")))}]]]))))


(defn add-modal
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        visible? (subscribe [::subs/add-modal-visible?])
        nav-path (subscribe [::main-subs/nav-path])]
    (fn []
      (let [parent  (utils/nav-path->module-path @nav-path)
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
   [ui/TableCell {:collapsing true} (str v1)]
   [ui/TableCell (str v2)]])


(defn preprocess-metadata
  [{:keys [name path description logo-url type acl] :as module-meta}]
  {:title       name
   :subtitle    path
   :description description
   :logo        logo-url
   :icon        (utils/meta-category-icon type)
   :acl         acl})


(defn metadata-rows
  [module-meta]
  (->> (dissoc module-meta :versions :children :acl :operations)
       (map (juxt (comp name first) (comp str second)))
       (map tuple-to-row)))


(defn format-meta
  [module-meta]
  (let [metadata (preprocess-metadata module-meta)
        rows     (metadata-rows module-meta)]
    [cc/metadata metadata rows]))


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


(defn sanitize-name [name]
  (str/lower-case
    (str/replace
      (str/trim
        (str/join "" (re-seq #"[a-zA-Z0-9\ ]" name)))
      " " "-")))


(defn contruct-path [parent name]
  (log/infof "san: %s '%s'" parent name)
  (let [sanitized-name (sanitize-name name)]
    (log/infof "san: '%s'" sanitized-name)
    (str/join "/"
              (remove str/blank?
                      [parent sanitized-name]))))


(defn summary
  [extras]
  (let [tr               (subscribe [::i18n-subs/tr])
        default-logo-url (subscribe [::subs/default-logo-url])
        is-new?          (subscribe [::subs/is-new?])]
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
           ;[:div (pr-str @(subscribe [::subs/module]))]
           [ui/Input {:name        "name"
                      :label       "name"
                      :value       name
                      :placeholder (str/capitalize (@tr [:name]))
                      :fluid       true
                      :style       {:padding-bottom 5}
                      :on-change   (ui-callback/input-callback #(do (dispatch [::events/page-changed? true])
                                                                    (dispatch [::events/name %])))
                      }]
           (when (not-empty parent)
             [ui/Input {:name        "parent"
                        :label       (if (= "PROJECT" type) "parent project" "project")
                        :value       (or parent "")         ;should not be needed, but is!!?
                        :placeholder (str/capitalize (@tr [:parent]))
                        :disabled    true
                        :fluid       true
                        :style       {:padding-bottom 5}}])
           [ui/Input {:name        "description"
                      :label       "description"
                      :value       description
                      :placeholder (str/capitalize (@tr [:description]))
                      :fluid       true
                      :style       {:padding-bottom 5}
                      :on-change   (ui-callback/input-callback #(do (dispatch [::events/page-changed? true])
                                                                    (dispatch [::events/description %])))}]
           [ui/Input {:name     "path"
                      :label    "path"
                      :value    (or path (contruct-path parent name))
                      :disabled true
                      :fluid    true
                      :style    {:padding-bottom 5}}]
           extras
           ]]]))))
