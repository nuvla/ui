(ns sixsq.nuvla.ui.utils.bulk-edit-tags-modal
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-sub
                                   subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.i18n.spec :as i18n-spec]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.messages.events :as messages-events]
            [sixsq.nuvla.ui.plugins.table :as table-plugin]
            [sixsq.nuvla.ui.utils.events :as events]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))


(def modal-tags-set-id ::tags-set)
(def modal-tags-add-id ::tags-add)
(def modal-tags-remove-id ::tags-remove)
(def modal-tags-remove-all ::tags-remove-all)
(def tags-modal-ids [modal-tags-add-id modal-tags-set-id modal-tags-remove-id modal-tags-remove-all])
(def tags-modal-ids-set (set tags-modal-ids))

(reg-sub
  ::selected-count
  (fn [[_ db-path total-count-sub-key]]
    [(subscribe [::table-plugin/selected-set-sub db-path])
     (subscribe [::table-plugin/select-all?-sub db-path])
     (subscribe [total-count-sub-key])])
  (fn [[selected-set selected-all? total-count]]
    (if selected-all? total-count (count selected-set))))

(reg-sub
  ::bulk-modal-visible?
  (fn [[_ db-path]]
    (subscribe [::edit-mode db-path]))
  (fn [opened-modal]
    (boolean (tags-modal-ids-set opened-modal))))

(reg-sub
  ::edit-mode
  (fn [db [_ db-path]]
    (get-in db [::edit-mode db-path])))

(reg-event-fx
  ::open-modal
  (fn [{db :db} [_ {:keys [resource-key modal-id db-path on-open-modal-event]}]]
    (let [fx (when (and (tags-modal-ids-set modal-id)
                     (not (tags-modal-ids-set (get-in db [::edit-mode db-path]))))
               [[:dispatch [on-open-modal-event]]
                [:dispatch [::fetch-tags resource-key]]])]
      {:db (assoc-in db [::edit-mode db-path] modal-id)
       :fx (or fx [])})))

(reg-event-fx
  ::fetch-tags
  (fn [_ [_ resource-key]]
    {::cimi-api-fx/search
     [resource-key
      {:first        0
       :last        0
       :aggregation "terms:tags"}
      (fn [response]
        (dispatch [::set-tags
                   (->> response :aggregations :terms:tags :buckets
                     (map :key))
                   resource-key]))]}))

(reg-event-db
  ::set-tags
  (fn [db [_ tags resource-key]]
    (assoc-in db [::tags resource-key] tags)))

(reg-sub
  ::tags
  (fn [db [_ resource-key]]
    (-> db ::tags resource-key)))

(defn- translate-from-qkey
  [tr q-key]
  (when (keyword? q-key) (tr [(-> q-key name keyword)])))

(defn- TagsEditModeRadio
  [edit-mode opened-modal change-mode]
  (let [tr               (subscribe [::i18n-subs/tr])
        active?          (= opened-modal edit-mode)
        font-weight      (if active? 700 400)
        setting-tags?    (= modal-tags-set-id edit-mode)]
    [ui/Radio {:style     {:font-weight font-weight}
               :label     (str (translate-from-qkey @tr edit-mode)
                            (when setting-tags? (str " (" (@tr [:tags-overwrite]) "!)")))
               :checked   active?
               :on-change #(change-mode edit-mode)}]))

(defn- ButtonAskingForConfirmation
  [_]
  (let [mode (r/atom :idle)
        tr   (subscribe [::i18n-subs/tr])]
    (fn [{:keys [color text update-event disabled? action-aria-label]}]
      (if (= :idle @mode)
        [:div
         [:span (str text "?")]
         [ui/Button {:aria-label action-aria-label
                     :color    color
                     :disabled disabled?
                     :active   true
                     :style    {:margin-left "2rem"}
                     :on-click (fn [] (reset! mode :confirming))}
          [icons/CheckIcon {:style {:margin 0}}]]]
        [:div
         [:span (str (@tr [:are-you-sure?]) " ")]
         [uix/Button {:text     (str (str/capitalize (@tr [:yes])) ", " text)
                      :disabled disabled?
                      :color    color
                      :on-click (fn [] (dispatch update-event))}]
         [ui/Button {:on-click (fn [] (reset! mode :idle))}
          [icons/XMarkIcon {:style {:margin 0}}]]]))))

;; Needs
;; - available tags -> through resource-key, fetched from here
;; - selected resources -> sub-key
;; - not editable selected resources -> event plus sub-key
;; - open-modal -> db-path

(s/def ::db-path (s/* keyword?))
(s/def ::resource-key keyword?)
(s/def ::no-edit-rights-sub-key keyword?)
(s/def ::total-count-sub-key keyword?)
(s/def ::on-open-modal-event keyword?)
(s/def ::singular string?)
(s/def ::plural string?)
(s/def ::filter-fn fn?)
(s/def ::refetch-event keyword?)

(reg-event-fx
  ::update-tags
  (fn [{{:keys [::i18n-spec/tr] :as db} :db} [_ {:keys [resource-key updated-tags call-back-fn refetch-event
                           text operation filter-fn
                           db-path singular plural]}]]
    {::cimi-api-fx/operation-bulk [resource-key
                                   (fn [result]
                                     (let [updated     (-> result :updated)
                                           success-msg (str updated " " (if (= 1 updated) singular plural) " " (tr [:updated-with-operation]) ": " text)]
                                       (dispatch [::messages-events/add
                                                  {:header  (tr [:bulk-edit-successful])
                                                   :content success-msg
                                                   :type    :success}])
                                       (dispatch [::table-plugin/set-bulk-edit-success-message
                                                  success-msg
                                                  db-path])
                                       (dispatch [::table-plugin/reset-bulk-edit-selection db-path])
                                       (dispatch [refetch-event])
                                       (when (fn? call-back-fn) (call-back-fn (-> result :updated)))))
                                   operation
                                   (filter-fn db)
                                   {:doc {:tags updated-tags}}]}))

(defn BulkEditTagsModal
  [{:keys [resource-key no-edit-rights-sub-key refetch-event
           db-path total-count-sub-key singular plural filter-fn]}]
  (let [tr                   (subscribe [::i18n-subs/tr])
        selected-count       (subscribe [::selected-count db-path total-count-sub-key])
        edit-mode            (subscribe [::edit-mode db-path])
        open?                (subscribe [::bulk-modal-visible? db-path])
        used-tags            (subscribe [::tags resource-key])
        view-only-avlbl?     (keyword? no-edit-rights-sub-key)
        view-only-items      (when view-only-avlbl?
                               (subscribe [no-edit-rights-sub-key]))
        form-tags            (r/atom [])
        mode->tag-color      (zipmap tags-modal-ids [:teal :teal :red :red])
        edit-mode->color     {modal-tags-add-id     :green
                              modal-tags-remove-all :red
                              modal-tags-remove-id  :red
                              modal-tags-set-id     :red}
        edit-mode->operation {modal-tags-add-id     "add-tags"
                              modal-tags-remove-all "set-tags"
                              modal-tags-set-id     "set-tags"
                              modal-tags-remove-id  "remove-tags"}]
    (fn []
      (let [action-text  (translate-from-qkey @tr @edit-mode)
            color        (edit-mode->color @edit-mode)
            close-fn     (fn []
                           (dispatch [::open-modal {:db-path db-path}])
                           (reset! form-tags []))
            change-mode  (fn [modal-id]
                           (when (= modal-tags-remove-all modal-id)
                             (reset! form-tags []))
                           (dispatch [::open-modal {:db-path  db-path
                                                    :modal-id modal-id}]))
            not-editable (when view-only-avlbl? (:count @view-only-items))
            updated-tags (if (= modal-tags-remove-all @edit-mode)
                           []
                           @form-tags)
            update-event [::update-tags {:resource-key resource-key
                                         :refetch-event refetch-event
                                         :updated-tags updated-tags
                                         :operation    (edit-mode->operation @edit-mode)
                                         :call-back-fn close-fn
                                         :text         action-text
                                         :filter-fn     filter-fn
                                         :db-path      db-path
                                         :plural       plural
                                         :singular     singular}]
            disabled? (or (= @selected-count 0)
                        (and  (not= modal-tags-remove-all @edit-mode)
                          (= 0 (count @form-tags))))]
        [ui/Modal {:open       @open?
                   :close-icon true
                   :on-close   close-fn}
         [uix/ModalHeader {:header (@tr [:bulk-update-tags])}]
         [ui/ModalContent
          [ui/Form
           [:div {:style {:display :flex
                          :gap     "1.5rem"}}
            (doall (for [mode tags-modal-ids]
                     ^{:key mode}
                     [TagsEditModeRadio mode @edit-mode change-mode]))]
           [:div {:style {:margin-top "1.5rem"}}
            (when-not (= modal-tags-remove-all @edit-mode)
              [components/TagsDropdown {:initial-options @used-tags
                                        :on-change-fn    (fn [tags] (reset! form-tags tags))
                                        :tag-color       (mode->tag-color @edit-mode)}])]]]
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
                   (@tr [(if (= @selected-count 1) singular plural)])
                   ". ")]
           (when (and view-only-avlbl? (<= 1 not-editable @selected-count))
             [:<>
              [:div
               (str not-editable " " (@tr [(if (= not-editable 1) singular plural)]) " " (@tr [:tags-not-updated-no-rights]))]
              [:div [:a {:style {:cursor :pointer}
                         :target :_blank
                         :on-click
                         (fn []
                           (dispatch
                            [::events/store-filter-and-open-in-new-tab
                             (str/join " or "
                                       (map #(str "id='" % "'")
                                            (->> @view-only-items :resources (map :id))))]))}
                     (@tr [(if (= not-editable 1) :open-it-in-new-tab :open-them-in-new-tab)])]]])]
          [ButtonAskingForConfirmation {:disabled? disabled? :db-path db-path
                                       :update-event update-event :total-count-sub-key total-count-sub-key
                                       :text action-text :color color :action-aria-label "edit tags"}]]]))))

(defn create-bulk-edit-modal
  [{:keys [db-path on-open-modal-event resource-key] :as opts}]
  {:trigger-config {:icon (fn [] [icons/TagIcon])
                   :name "Edit Tags"
                   :event (fn []
                            (dispatch
                              [::open-modal
                               {:modal-id            modal-tags-add-id
                                :db-path             db-path
                                :on-open-modal-event on-open-modal-event
                                :resource-key        resource-key}]))}
   :modal         (fn [] [BulkEditTagsModal opts])})

(s/def ::bulk-edit-modal-interface
  (s/cat :opts (s/keys
                 :req-un [::db-path
                          ::resource-key
                          ::total-count-sub-key
                          ::filter-fn
                          ::refetch-event]
                 :opt-un [::no-edit-rights-sub-key
                          ::on-open-modal-event
                          ::singular
                          ::plural])))

(s/fdef BulkEditTagsModal :args
  ::bulk-edit-modal-interface)

(s/fdef create-bulk-edit-modal :args
  ::bulk-edit-modal-interface)
