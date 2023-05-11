(ns sixsq.nuvla.ui.utils.bulk-edit-modal
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-sub
                                   subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.edges.events :as events]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.plugins.table :as table-plugin]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))


(def modal-tags-set-id ::tags-set)
(def modal-tags-add-id ::tags-add)
(def modal-tags-remove-id ::tags-remove)
(def modal-tags-remove-all ::tags-remove-all)
(def tags-modal-ids [modal-tags-add-id modal-tags-set-id modal-tags-remove-id modal-tags-remove-all])

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
    (tap> opened-modal)
    (boolean ((set tags-modal-ids) opened-modal))))

(reg-sub
  ::edit-mode
  (fn [db [_ db-path]]
    (get-in db [::edit-mode db-path])))

(reg-event-fx
  ::open-modal
  (fn [{db :db} [_ {:keys [resource-key modal-id db-path on-open-modal-event]}]]
    (let [fx [(when (and ((set tags-modal-ids) modal-id)
                     (not ((set tags-modal-ids) (get-in db [::edit-mode db-path]))))
               (when on-open-modal-event [:dispatch [on-open-modal-event]])
                [:dispatch [::fetch-tags resource-key]])]]
      {:db (assoc-in db [::edit-mode db-path] modal-id)
       :fx fx})))

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
  (tr [(-> q-key name keyword)]))

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
  [_form-tags close-fn db-path update-event total-count-sub-key]
  (let [edit-mode->color     {modal-tags-add-id     :green
                              modal-tags-remove-all :red
                              modal-tags-remove-id  :red
                              modal-tags-set-id     :red}
        edit-mode->operation {modal-tags-add-id     "add-tags"
                              modal-tags-remove-all "set-tags"
                              modal-tags-set-id     "set-tags"
                              modal-tags-remove-id  "remove-tags"}
        tr                   (subscribe [::i18n-subs/tr])
        selected-count       (subscribe [::selected-count db-path total-count-sub-key])
        edit-mode            (subscribe [::edit-mode db-path])
        mode                 (r/atom :idle)]
    (fn [form-tags _close-fn]
      (let [text         (translate-from-qkey @tr @edit-mode)
            updated-tags (if (= modal-tags-remove-all edit-mode)
                           []
                           form-tags)
            update-fn    (fn []
                           (dispatch [update-event
                                      {:updated-tags updated-tags
                                       :operation    (edit-mode->operation @edit-mode)
                                       :call-back-fn close-fn
                                       :text         text}]))
            disabled? (or (= @selected-count 0)
                        (and  (not= modal-tags-remove-all @edit-mode)
                          (= 0 (count form-tags))))]
        (if (= :idle @mode)
          [:div
           [:span (str text "?")]
           [ui/Button {:color    (edit-mode->color @edit-mode)
                       :disabled disabled?
                       :active   true
                       :style    {:margin-left "2rem"}
                       :on-click (fn [] (reset! mode :confirming))}
            [uix/Icon {:style {:margin 0}
                       :name "fa-check"}]]]
          [:div
           [:span "Are you sure? "]
           [uix/Button {:text     (str "Yes, " text)
                        :disabled disabled?
                        :color    (edit-mode->color @edit-mode)
                        :on-click update-fn}]
           [ui/Button {:on-click (fn [] (reset! mode :idle))}
            [ui/Icon {:style {:margin 0}
                      :name "fa-xmark"}]]])))))

;; Needs
;; - available tags -> through resource-key, fetched from here
;; - selected resources -> sub-key
;; - not editable selected resources -> event plus sub-key
;; - open-modal -> db-path

(s/def ::db-path (s/* keyword?))
(s/def ::resource-key keyword?)
(s/def ::no-edit-rights-sub-key keyword?)
(s/def ::total-count-sub-key keyword?)


(defn BulkEditTagsModal
  [{:keys [resource-key no-edit-rights-sub-key
           update-event db-path total-count-sub-key]} {:keys [singular plural]}]
  (let [tr               (subscribe [::i18n-subs/tr])
        selected-count   (subscribe [::selected-count db-path total-count-sub-key])
        opened-modal     (subscribe [::edit-mode db-path])
        open?            (subscribe [::bulk-modal-visible? db-path])
        used-tags        (subscribe [::tags resource-key])
        view-only-avlbl? (keyword? no-edit-rights-sub-key)
        view-only-items  (when view-only-avlbl?
                           (subscribe [no-edit-rights-sub-key]))
        form-tags        (r/atom [])
        mode->tag-color  (zipmap tags-modal-ids [:teal :teal :red :red])]
    (fn []
      (let [close-fn     (fn []
                           (dispatch [::open-modal {:db-path db-path}])
                           (reset! form-tags []))
            change-mode  (fn [modal-id]
                           (when (= modal-tags-remove-all modal-id)
                             (reset! form-tags []))
                           (dispatch [::open-modal {:db-path  db-path
                                                    :modal-id modal-id}]))
            not-editable (when view-only-avlbl? (:count @view-only-items))]
        [ui/Modal {:open       @open?
                   :close-icon true
                   :on-close   close-fn}
         [uix/ModalHeader {:header (@tr [:bulk-update-tags])}]
         [ui/ModalContent
          [ui/Form
           [:div {:style {:display :flex
                          :gap     "1.5rem"}}
            (doall (for [edit-mode tags-modal-ids]
                     ^{:key edit-mode}
                     [TagsEditModeRadio edit-mode @opened-modal change-mode]))]
           [:div {:style {:margin-top "1.5rem"}}
            (when-not (= modal-tags-remove-all @opened-modal)
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
                   (@tr [(if (= @selected-count 0) singular plural)])
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
                     (str "Open " (if (= not-editable 1) "it" "them") " in a new tab")]]])]
          [ButtonAskingForConfirmation @form-tags close-fn db-path update-event total-count-sub-key]]]))))

(defn create-bulk-edit-modal
  [{:keys [db-path on-open-modal-event resource-key] :as opts}]
  {:trigger-config {:icon (fn [] [ui/Icon {:className "fal fa-tags"}])
                   :name "Edit Tags"
                   :event (fn []
                            (dispatch
                              [::open-modal
                               {:modal-id            modal-tags-add-id
                                :db-path             db-path
                                :on-open-modal-event on-open-modal-event
                                :resource-key        resource-key}]))}
   :modal         (fn [] [BulkEditTagsModal opts])})

(s/fdef BulkEditTagsModal :args (s/cat :opts (s/keys
                                               :req-un [::db-path
                                                        ::resource-key
                                                        ::update-event]
                                               :opt-un [::no-edit-rights-sub-key])))

(s/fdef create-bulk-edit-modal :args (s/cat :opts (s/keys
                                               :req-un [::db-path
                                                        ::resource-key
                                                        ::update-event]
                                               :opt-un [::no-edit-rights-sub-key])))
