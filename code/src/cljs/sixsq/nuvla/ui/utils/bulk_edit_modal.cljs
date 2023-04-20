(ns sixsq.nuvla.ui.utils.bulk-edit-modal
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-sub subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.edges.events :as events]
            [sixsq.nuvla.ui.edges.spec :as spec]
            [sixsq.nuvla.ui.edges.subs :as subs]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.plugins.table :as table-plugin]))

(def modal-tags-set-id ::tags-set)
(def modal-tags-add-id ::tags-add)
(def modal-tags-remove-id ::tags-remove)
(def modal-tags-remove-all ::tags-remove-all)
(def tags-modal-ids [modal-tags-add-id modal-tags-set-id modal-tags-remove-id modal-tags-remove-all])

(reg-sub
  ::selected-count
  (fn [_ [_ db-path]]
    [(subscribe [::table-plugin/selected-set-sub db-path])
     (subscribe [::table-plugin/select-all?-sub db-path])
     (subscribe [::nuvlaboxes-count])])
  (fn [[selected-set selected-all? total-count]]
    (if selected-all? total-count (count selected-set))))

(defn- get-name-as-keyword
  [tr q-key]
  (tr [(-> q-key name keyword)]))

(defn- TagsEditModeRadio
  [edit-mode opened-modal change-mode]
  (let [tr               (subscribe [::i18n-subs/tr])
        active?          (= opened-modal edit-mode)
        font-weight      (if active? 700 400)
        setting-tags?    (= modal-tags-set-id edit-mode)]
    [ui/Radio {:style     {:font-weight font-weight}
               :label     (str (get-name-as-keyword @tr edit-mode)
                               (when setting-tags? (str " (" (@tr [:tags-overwrite]) "!)")))
               :checked   active?
               :on-change #(change-mode edit-mode)}]))

(defn- ButtonAskingForConfirmation
  [_form-tags close-fn]
  (let [tr               (subscribe [::i18n-subs/tr])
        selected-count   (subscribe [::subs/selected-count ::spec/select])
        edit-mode        (subscribe [::subs/opened-modal])
        mode             (r/atom :idle)
        edit-mode->color {modal-tags-add-id     :green
                          modal-tags-remove-all :red
                          modal-tags-remove-id  :red
                          modal-tags-set-id     :red}]
    (fn [form-tags _close-fn]
      (let [text      (get-name-as-keyword @tr @edit-mode)
            call-back (fn [] (close-fn))
            update-fn (fn []
                        (dispatch [::events/update-tags
                                   @edit-mode
                                   {:tags         form-tags
                                    :call-back-fn call-back
                                    :text text}]))
            disabled? (or (= @selected-count 0)
                          (and  (not= spec/modal-tags-remove-all @edit-mode)
                                (= 0 (count form-tags))))]
        (if (= :idle @mode)
          [:div
           [:span (str text "?")]
           [ui/Button {
                      ;;  :icon
                       :color    (edit-mode->color @edit-mode)
                       :disabled disabled?
                       :active   true
                       :style    {:margin-left "2rem"}
                       :on-click (fn [] (reset! mode :confirming))}
            [uix/Icon {:style {:margin 0}
                       :name "fa-check"}]]]
          [:div
           [:span "Are you sure? " ]
           [uix/Button {:text     (str "Yes, " text)
                        :disabled disabled?
                        :color    (edit-mode->color @edit-mode)
                        :on-click update-fn}]
           [ui/Button {:on-click (fn [] (reset! mode :idle))}
            [ui/Icon {:style {:margin 0}
                      :name "fa-xmark"}]]])))))

(defn BulkEditModal
  [db-path]
  (let [tr               (subscribe [::i18n-subs/tr])
        selected-count   (subscribe [::subs/selected-count ::spec/select])
        opened-modal     (subscribe [::subs/opened-modal])
        open?            (subscribe [::subs/bulk-modal-visible?])
        used-tags        (subscribe [::subs/edges-tags])
        view-only-edges  (subscribe [::subs/edges-without-edit-rights])
        form-tags        (r/atom [])
        mode->tag-color  (zipmap spec/tags-modal-ids [:teal :teal :red :red])]
    (fn []
      (let [close-fn     (fn []
                           (dispatch [::events/open-modal nil])
                           (reset! form-tags []))
            change-mode  (fn [edit-mode]
                           (when (= spec/modal-tags-remove-all edit-mode)
                             (reset! form-tags []))
                           (dispatch [::events/open-modal edit-mode]))
            not-editable (:count @view-only-edges)]
        [ui/Modal {:open       @open?
                   :close-icon true
                   :on-close   close-fn}
         [uix/ModalHeader {:header (@tr [:bulk-update-tags])}]
         [ui/ModalContent
          [ui/Form
           [:div {:style {:display :flex
                          :gap     "1.5rem"}}
            (doall (for [edit-mode spec/tags-modal-ids]
                     ^{:key edit-mode}
                     [TagsEditModeRadio edit-mode @opened-modal change-mode]))]
           [:div {:style {:margin-top "1.5rem"}}
            (when-not (= spec/modal-tags-remove-all @opened-modal)
              [components/TagsDropdown {:initial-options @used-tags
                                        :on-change-fn    (fn [tags] (reset! form-tags tags))
                                        :tag-color       (mode->tag-color @opened-modal)}])]]]
         [ui/ModalActions
          {:style {:display         :flex
                   :align-items     :center
                   :justify-content :space-between
                   :text-align      :left}}
          [:div
           {:style {:line-height "1.2rem"}}
           [:div (str (str/capitalize (@tr [:tags-bulk-you-have-selected]))
                      " "
                      @selected-count
                      " "
                      (@tr [(if (= @selected-count 0) :edge :edges)])
                      ". ")]
           (when (<= 1 not-editable @selected-count)
             [:<>
              [:div
               (str not-editable " " (@tr [(if (= not-editable 1) :edge :edges)]) " " (@tr [:tags-not-updated-no-rights]))]
              [:div [:a {:style {:cursor :pointer}
                         :target :_blank
                         :on-click
                         (fn []
                           (dispatch
                            [::events/store-filter-and-open-in-new-tab
                             (str/join " or "
                                       (map #(str "id='" % "'")
                                            (->> @view-only-edges :resources (map :id))))]))}
                     (str "Open " (if (= not-editable 1) "it" "them") " in a new tab")]]])]
          [ButtonAskingForConfirmation @form-tags close-fn]]]))))