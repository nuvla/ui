(ns sixsq.nuvla.ui.module-project.views
  (:require
    [reagent.core :as reagent]
    [cljs.pprint :refer [cl-format]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch dispatch-sync subscribe]]
    [sixsq.nuvla.ui.cimi-api.utils :as cimi-api-utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.module-project.events :as events]
    [sixsq.nuvla.ui.module-project.subs :as subs]
    [sixsq.nuvla.ui.plot.plot :as plot]
    [sixsq.nuvla.ui.utils.collapsible-card :as cc]
    [sixsq.nuvla.ui.utils.resource-details :as details]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.form-fields :as forms]
    [sixsq.nuvla.ui.panel :as panel]
    [taoensso.timbre :as log]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.deployment-dialog.views :as deployment-dialog-views]))

(defn refresh-button
  []
  (let [tr            (subscribe [::i18n-subs/tr])
        page-changed? (subscribe [::subs/page-changed?])]
    (fn []
      [ui/MenuMenu {:position "right"}
       [uix/MenuItemWithIcon
        {:name      (@tr [:refresh])
         :icon-name "refresh"
         :loading?  false                                   ;; FIXME: Add loading flag for module.
         :disabled  @page-changed?
         ;:on-click  #(dispatch [::application-events/get-module])
         }]])))


(defn control-bar []
  (let [tr            (subscribe [::i18n-subs/tr])
        page-changed? (subscribe [::subs/page-changed?])]
    (fn []
      [ui/Menu {:borderless true}
       [uix/MenuItemWithIcon
        {:name      (@tr [:launch])
         :icon-name "rocket"
         :disabled  true
         ;:on-click #(dispatch [::deployment-dialog-events/create-deployment (:id @module) :credentials])
         }]
       [uix/MenuItemWithIcon
        {:name     (@tr [:save])
         ;:icon-name "add"
         :disabled (not @page-changed?)
         ;:on-click nil                                      ;#(dispatch [::application-events/open-add-modal])
         }]
       [refresh-button]])))


(defn summary []
  (let [tr               (subscribe [::i18n-subs/tr])
        name             (subscribe [::subs/name])
        parent           (subscribe [::subs/parent])
        description      (subscribe [::subs/description])
        logo-url         (subscribe [::subs/logo-url])
        default-logo-url (subscribe [::subs/default-logo-url])]
    (fn []
      [ui/Grid {:style {:margin-bottom 5}}
       [ui/GridRow {:reversed :computer}
        [ui/GridColumn {:computer 2
                        :large-screen 2}
         [ui/Image {:src (or @logo-url @default-logo-url)}]
         [ui/Button {:fluid    true
                     :on-click #(dispatch [::events/open-logo-url-modal])}
          (@tr [:module-change-logo])]]
        [ui/GridColumn {:computer 14
                        :large-screen 14}
         ;[:div (pr-str @(subscribe [::subs/name]) @(subscribe [::subs/description]))]
         [ui/Input {:name        "name"
                    :value       @name
                    :placeholder (str/capitalize (@tr [:name]))
                    :fluid       true
                    :style       {:padding-bottom 5}
                    :on-change   (ui-callback/input-callback #(dispatch [::events/name %]))}]
         [ui/Input {:name        "parent"
                    :value       @parent
                    :placeholder (str/capitalize (@tr [:parent]))
                    :fluid       true
                    :style       {:padding-bottom 5}
                    :on-change   (ui-callback/input-callback #(dispatch [::events/parent %]))}]
         [ui/Input {:name        "description"
                    :value       @description
                    :placeholder (str/capitalize (@tr [:description]))
                    :fluid       true
                    :style       {:padding-bottom 5}
                    :on-change   (ui-callback/input-callback #(dispatch [::events/description %]))}]]]])))


(defn save-action []
  (let [page-changed? (subscribe [::subs/page-changed?])]
    (fn []
      [ui/Button {:primary  true
                  :style    {:margin-top 10}
                  :disabled (not @(subscribe [::subs/page-changed?]))}
       "Save"])))


(defn logo-url-modal
  []
  (let [local-url (reagent/atom "")
        tr        (subscribe [::i18n-subs/tr])
        visible?  (subscribe [::subs/logo-url-modal-visible?])
        url       (subscribe [::subs/logo-url])]
    (fn []
      [ui/Modal {:open       @visible?
                 :close-icon true
                 :on-close   #(dispatch [::events/close-logo-url-modal])}

       [ui/ModalHeader (@tr [:select-logo-url])]

       [ui/ModalContent
        [ui/Input {:default-value (or @url "")
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
                                       (log/infof "Button NOT ENTER")))}]]])))


(defn modules []
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn []
      [ui/Message {:warning true}
       [ui/Icon {:name "warning sign"}]
       (@tr [:no-children-modules])])))


(defn view-edit
  []
  (let [name (subscribe [::subs/name])
        parent (subscribe [::subs/parent])]
    (fn []
      [ui/Container {:fluid true}
       [:h2 [ui/Icon {:name "folder"}]
        @parent (when (not (empty? @parent)) "/") @name]
       [control-bar]
       [summary]
       [save-action]
       [modules]
       [logo-url-modal]])))


(defn path->parent-path
  [path]
  (str/join "/" (rest path)))


(defmethod panel/render :module-project
  [path]
  (let [tr (subscribe [::i18n-subs/tr])]
    (dispatch [::events/name (str/capitalize (@tr [:new-project]))])
    (dispatch [::events/parent (path->parent-path @(subscribe [::main-subs/nav-path]))])
    [view-edit]))
