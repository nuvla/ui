(ns sixsq.nuvla.ui.data.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.apps.utils :as application-utils]
    [sixsq.nuvla.ui.data.events :as events]
    [sixsq.nuvla.ui.data.subs :as subs]
    [sixsq.nuvla.ui.data.utils :as utils]
    [sixsq.nuvla.ui.data-set.views :as data-set-views]
    [sixsq.nuvla.ui.deployment-dialog.events :as deployment-dialog-events]
    [sixsq.nuvla.ui.deployment-dialog.subs :as deployment-dialog-subs]
    [sixsq.nuvla.ui.deployment-dialog.views :as deployment-dialog-views]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.general :as utils-general]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn refresh []
  (dispatch [::events/refresh]))


(defn ProcessButton
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        data-sets (subscribe [::subs/selected-data-set-ids])]
    (fn []
      [uix/MenuItem
       {:name     (@tr [:process])
        :disabled (not (seq @data-sets))
        :icon     "rocket"
        :position "left"
        :on-click #(dispatch [::main-events/subscription-required-dispatch
                              [::events/open-application-select-modal]])}])))


(defn MenuBar []
  (let [tr (subscribe [::i18n-subs/tr])]
    [:div
     [main-components/StickyBar
      [ui/Menu {:attached "top", :borderless true}
       [ProcessButton]
       [main-components/RefreshMenu
        {:on-refresh refresh}]]]
     [:div {:style {:padding "10px 0"}}
      [ui/Message {:info true}
       (@tr [:data-set-search-message])]
      [data-set-views/SearchHeader refresh ::events/set-full-text-search ::subs/full-text-search]]]))


(defn ApplicationListItem
  [{:keys [id name description subtype created] :as _application}]
  (let [selected-application-id (subscribe [::subs/selected-application-id])
        on-click-fn             #(dispatch [::events/set-selected-application-id id])]
    ^{:key id}
    [ui/ListItem {:active   (and @selected-application-id (= id @selected-application-id))
                  :on-click on-click-fn}
     [ui/ListIcon {:name (application-utils/subtype-icon subtype), :size "large"}]
     [ui/ListContent
      [ui/ListHeader (str (or name id) " (" (time/ago (time/parse-iso8601 created)) ")")]
      (or description "")]]))


(defn ApplicationList
  []
  (let [tr           (subscribe [::i18n-subs/tr])
        applications (subscribe [::subs/applications])
        loading?     (subscribe [::subs/loading-applications?])]
    [ui/Segment {:loading @loading?
                 :basic   true}
     (if (seq @applications)
       (vec (concat [ui/ListSA {:divided   true
                                :relaxed   true
                                :selection true}]
                    (mapv ApplicationListItem @applications)))
       [ui/Message {:error true} (@tr [:no-apps])])]))


(defn LaunchButton
  []
  (let [tr                     (subscribe [::i18n-subs/tr])
        visible?               (subscribe [::subs/application-select-visible?])
        selected-app-id        (subscribe [::subs/selected-application-id])

        data-step-active?      (subscribe [::deployment-dialog-subs/data-step-active?])

        deployment             (subscribe [::deployment-dialog-subs/deployment])
        data-completed?        (subscribe [::deployment-dialog-subs/data-completed?])
        credentials-completed? (subscribe [::deployment-dialog-subs/credentials-completed?])
        env-completed?         (subscribe [::deployment-dialog-subs/env-variables-completed?])
        hide-fn                #(do
                                  (dispatch [::events/close-application-select-modal])
                                  (dispatch [::events/delete-deployment]))
        configure-fn           (fn [id]
                                 (dispatch [::events/close-application-select-modal])
                                 (dispatch [::deployment-dialog-events/create-deployment id :data]))

        launch-fn              #(do
                                  (dispatch [::events/close-application-select-modal])
                                  (dispatch [::deployment-dialog-events/edit-deployment]))]

    (fn []
      (let [launch-disabled? (or (not @deployment)
                                 (and (not @data-completed?) @data-step-active?)
                                 (not @credentials-completed?)
                                 (not @env-completed?))]

        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   hide-fn}

         [uix/ModalHeader {:header (@tr [:select-application]) :icon "sitemap"}]

         [ui/ModalContent {:scrolling true}
          [ui/ModalDescription
           [ApplicationList]]]
         [ui/ModalActions
          [ui/Button {:disabled (nil? @selected-app-id)
                      :primary  true
                      :on-click #(configure-fn @selected-app-id)}
           [ui/Icon {:name "settings"}]
           (@tr [:configure])]
          [ui/Button {:disabled (nil? @selected-app-id)
                      :primary  true
                      :on-click #()}
           [ui/Icon {:name     "rocket"
                     :disabled launch-disabled?
                     :on-click launch-fn}]
           (@tr [:launch])]]]))))


(defn ApplicationSelectModal
  []
  (let [tr                     (subscribe [::i18n-subs/tr])
        visible?               (subscribe [::subs/application-select-visible?])
        selected-app-id        (subscribe [::subs/selected-application-id])

        data-step-active?      (subscribe [::deployment-dialog-subs/data-step-active?])

        deployment             (subscribe [::deployment-dialog-subs/deployment])
        data-completed?        (subscribe [::deployment-dialog-subs/data-completed?])
        credentials-completed? (subscribe [::deployment-dialog-subs/credentials-completed?])
        env-completed?         (subscribe [::deployment-dialog-subs/env-variables-completed?])
        hide-fn                #(do
                                  (dispatch [::events/close-application-select-modal])
                                  (dispatch [::events/delete-deployment]))
        configure-fn           (fn [id]
                                 (dispatch [::events/close-application-select-modal])
                                 (dispatch [::deployment-dialog-events/create-deployment id :data]))

        launch-fn              #(do
                                  (dispatch [::events/close-application-select-modal])
                                  (dispatch [::deployment-dialog-events/edit-deployment]))]
    (fn []
      (let [launch-disabled? (or (not @deployment)
                                 (and (not @data-completed?) @data-step-active?)
                                 (not @credentials-completed?)
                                 (not @env-completed?))]

        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   hide-fn}

         [uix/ModalHeader {:header (@tr [:select-application]) :icon "sitemap"}]

         [ui/ModalContent {:scrolling true}
          [ui/ModalDescription
           [ApplicationList]]]
         [ui/ModalActions
          [ui/Button {:disabled (nil? @selected-app-id)
                      :primary  true
                      :on-click #(configure-fn @selected-app-id)}
           [ui/Icon {:name "settings"}]
           (@tr [:configure])]
          [ui/Button {:disabled (or (nil? @selected-app-id)
                                    launch-disabled?)
                      :primary  true
                      :on-click launch-fn}
           [ui/Icon {:name "rocket"}]
           (@tr [:launch])]]]))))


(defn DataSetCard
  [{:keys [id name description] :as _data-set}]
  (let [tr        (subscribe [::i18n-subs/tr])
        counts    (subscribe [::subs/counts])
        sizes     (subscribe [::subs/sizes])
        data-sets (subscribe [::subs/selected-data-set-ids])
        count     (get @counts id "...")
        size      (get @sizes id "...")
        selected? (boolean (@data-sets id))]
    ^{:key id}
    [uix/Card
     {:header      name
      :description description
      :extra       [:<>
                    [ui/Label
                     [ui/Icon {:name "file"}]
                     [:span (str count " " (@tr [:objects]))]]
                    [ui/Label
                     [ui/Icon {:name "expand arrows alternate"}]
                     [:span (utils/format-bytes size)]]]
      :on-select   #(dispatch [::events/toggle-data-set-id id])
      :selected?   selected?
      :on-click    (fn [event]
                     (dispatch [::history-events/navigate (utils/data-record-href id)])
                     (.preventDefault event))}]))


(defn QueriesCardsGroup
  []
  (let [tr                (subscribe [::i18n-subs/tr])
        data-sets         (subscribe [::subs/data-sets])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])
        loading?          (subscribe [::subs/loading?])
        total             (subscribe [::subs/total])]
    (fn []
      (let [total-elements @total
            total-pages    (utils-general/total-pages total-elements @elements-per-page)]
        (if @loading?
          [ui/Loader {:active true
                      :inline "centered"}]
          [:<>
           [:div style/center-items
            (if (not (seq @data-sets))
              [ui/Message {:warning true}
               [ui/Icon {:name "warning sign"}]
               (@tr [:no-datasets])]
              (vec (concat [ui/CardGroup {:centered    true
                                          :itemsPerRow 4}]
                           (map (fn [data-set]
                                  [DataSetCard data-set])
                                (vals @data-sets)))))]
           [uix/Pagination {:totalitems   total-elements
                            :totalPages   total-pages
                            :activePage   @page
                            :onPageChange (ui-callback/callback
                                            :activePage
                                            #(dispatch [::events/set-page %]))}]])))))


(defn MainActionButton
  []
  (let [tr        (subscribe [::i18n-subs/tr])
        data-sets (subscribe [::subs/selected-data-set-ids])]
    [ui/ButtonGroup {:primary true
                     :style   {:padding-top 10}}
     [ui/Button
      {:content  (@tr [:process])
       :disabled (not (seq @data-sets))
       :icon     "rocket"
       :on-click #(dispatch [::main-events/subscription-required-dispatch
                             [::events/open-application-select-modal]])}]]))


(defn Data
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Segment style/basic
     [uix/PageHeader "database" (@tr [:data-processing])]
     [MenuBar]
     [ApplicationSelectModal]
     [deployment-dialog-views/deploy-modal true]
     [QueriesCardsGroup]
     [MainActionButton]]))


(defmethod panel/render :data
  [path]
  (refresh)
  (let [[_ uuid] path
        n (count path)]
    (case n
      2 [data-set-views/DataSet uuid]
      [Data])))
