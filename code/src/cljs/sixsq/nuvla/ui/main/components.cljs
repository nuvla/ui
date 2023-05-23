(ns sixsq.nuvla.ui.main.components
  (:require ["react" :as react]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch dispatch-sync subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.events :as events]
            [sixsq.nuvla.ui.main.subs :as subs]
            [sixsq.nuvla.ui.utils.form-fields :as ff]
            [sixsq.nuvla.ui.utils.forms :as forms]
            [sixsq.nuvla.ui.utils.general :as utils-general]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(def ref (react/createRef))


(defn RefreshedIn
  [action-id]
  (let [tr           (subscribe [::i18n-subs/tr])
        next-refresh (subscribe [::subs/next-refresh action-id])]
    (fn []
      [ui/MenuItem {:disabled true
                    :style    {:margin-right "10px"
                               :color        "grey"}}
       [:span (@tr [:automatic-refresh-in]) " "
        (when @next-refresh
          [uix/CountDown @next-refresh]) "s"]])))


(defn RefreshButton
  [loading? on-click refresh-disabled?]
  (let [tr       (subscribe [::i18n-subs/tr])
        on-click (or on-click #())]
    [uix/MenuItem
     {:name     (@tr [:refresh])
      :icon     icons/i-arrow-rotate
      :loading? (boolean loading?)
      :on-click on-click
      :style    {:cursor "pointer"
                 :color  "black"}
      :disabled (boolean refresh-disabled?)}]))


(defn RefreshMenu
  [{:keys [action-id loading? on-refresh refresh-disabled?]}]
  [ui/MenuMenu {:position :right}
   (when action-id
     [RefreshedIn action-id])
   (when on-refresh
     [RefreshButton loading? on-refresh refresh-disabled?])])


(defn RefreshCompact
  [{:keys [action-id loading? on-refresh refresh-disabled?]}]
  [:span {:style {:display "inline-flex"}}
   (when action-id
     [RefreshedIn action-id])
   (when on-refresh
     [RefreshButton loading? on-refresh refresh-disabled?])])


(defn SearchInput
  [opts]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Input (merge {:placeholder (str (@tr [:search]) "...")
                      :icon        (when-not (:action opts) "search")}
                     opts)]))


(defn StickyBar [Menu]
  [ui/Sticky {:offset  -1
              :context ref
              :style   {:margin-top    11
                        :margin-bottom 10}}
   Menu])


(defn ResponsiveMenuBar
  [Children & [Refresh]]
  (let [mobile?            (subscribe [::subs/is-mobile-device?])
        n                  (if @mobile? 0 2)
        Children           (remove nil? Children)
        ChildrenFirstPart  (take n Children)
        ChildrenSecondPart (drop n Children)]
    [ui/Menu {:borderless true}
     (when (seq ChildrenFirstPart)
       ChildrenFirstPart)
     (when (seq ChildrenSecondPart)
       [ui/Dropdown {:item true
                     :icon "ellipsis horizontal"}
        [ui/DropdownMenu
         ChildrenSecondPart]])
     Refresh]))


(defn ErrorJobsMessage
  [_job-subs _set-active-tab-event _job-tab _on-click]
  (let [tr                (subscribe [::i18n-subs/tr])
        errors-dissmissed (r/atom #{})]
    (fn [job-subs set-active-tab-event job-tab on-click]
      (let [fn-filter   (fn [coll _action jobs]
                          (let [{:keys [id state] :as last-job} (first jobs)]
                            (if (and (= state "FAILED") (not (@errors-dissmissed id)))
                              (conj coll last-job)
                              coll)))
            failed-jobs (->> (subscribe [job-subs])
                             deref
                             :resources
                             (group-by :action)
                             (reduce-kv fn-filter [])
                             (sort-by :updated >))]
        [:<>
         (doall
           (for [{:keys [id action status-message]} failed-jobs]
             ^{:key id}
             [ui/Message {:error      true
                          :on-dismiss #(swap! errors-dissmissed conj id)}
              [ui/MessageHeader
               {:style    {:cursor "pointer"}
                :on-click (or on-click
                              #(dispatch [set-active-tab-event job-tab]))}
               (str (str/capitalize (@tr [:job])) " " action " " (@tr [:failed]))]
              [ui/MessageContent (last (str/split-lines (or status-message "")))]]))]))))


(defn StatisticState
  ([{:keys [value icons label clickable? positive-color set-state-selector-event state-selector-subs stacked?]
     :or   {positive-color "black"}}]
   (let [state-selector (subscribe [state-selector-subs])
         selected?      (or
                          (= label @state-selector)
                          (and (= label "TOTAL")
                               (nil? @state-selector)))
         color          (if (pos? value) positive-color "grey")
         icon-key       (str label "-" icons)]
     [ui/Statistic {:style    (when clickable? {:cursor "pointer"})
                    :color    color
                    :class    (when clickable? "slight-up")
                    :on-click #(when clickable?
                                 (dispatch [set-state-selector-event
                                            (if (= label "TOTAL") nil label)]))}
      (if stacked?
        [:<> [ui/IconGroup
              {:style {:margin-right "auto"
                       :margin-left  "auto"}
               :role  :button}
              (for [i icons]
                [icons/Icon {:key     icon-key
                             :size    (when (and clickable? selected?) "large")
                             :loading (and (pos? value) (= "spinner" i))
                             :style   {:margin-right 0}
                             :name    i}])]
         [ui/StatisticValue
          (or value "-")]
         [ui/StatisticLabel label]]
        [:<>
         [ui/StatisticValue
          (or value "-")
          "\u2002"
          [ui/IconGroup
           (for [i icons]
             [icons/Icon {:key     icon-key
                          :size    (when (and clickable? selected?) "large")
                          :loading (and (pos? value) (= "spinner" i))
                          :name    i}])]]
         [ui/StatisticLabel label]])])))


(defn ClickMeStaticPopup
  []
  (let [tr               (subscribe [::i18n-subs/tr])
        is-small-device? (subscribe [::subs/is-small-device?])]
    (when-not @is-small-device?
      [ui/Segment {:raised true :compact true}
       [:span [ui/Icon {:name "arrow left"}] (@tr [:statistics-select-info])]])))


(defn InfoPopup
  [message]
  [ui/Popup {:content message
             :trigger (r/as-element [icons/InfoIconFull])}])


(defn NotFoundPortal
  [subs message-header message-content]
  (let [tr         (subscribe [::i18n-subs/tr])
        not-found? (subscribe [subs])]
    [ui/Dimmer {:active   @not-found?
                :inverted true}
     [ui/Segment {:textAlign "center"
                  :raised    true
                  :style     {:top    "20%"
                              :zIndex 1000}}
      [ui/Message {:warning true
                   :icon    "warning circle"
                   :header  (@tr [message-header])
                   :content (@tr [message-content])}]]]))


(defn LoadingContent
  [content]
  (let [loading? (subscribe [::subs/loading?])]
    (if @loading?
      [ui/Loader {:active true :size "massive"
                  :style  {:position "fixed"
                           :top      "50%"
                           :left     "50%"}}]
      content)))


(defn DimmableContent
  [content]
  [ui/DimmerDimmable
   {:style {:overflow "visible"}}
   content])


(defn LoadingPage
  "This form-2 component wraps content with a LoadingContent component.
   dispatch-sync is used at initialisation of the component to ensure a clean display of the spinner
   right from the start, without any blinking from previous potential content.
   This component should be used for large sections of content, such as tabs or containers.
   The content passed as argument is responsible for setting main-spec/loading? to false once the data is loaded.
   An optional DimmerContent component can also wrap the content. In this case, the content argument must include a
   NotFoundPortal component and a content event must set the corresponding spec attribute to true."
  [{:keys [_dimmable?]} _content]
  (dispatch-sync [::events/set-loading? true])
  (fn [{:keys [dimmable?]} content]
    [LoadingContent
     (if dimmable?
       [DimmableContent content]
       content)]))


(defn Pencil
  [editing?]
  [ui/Icon {:name     "pencil"
            :on-click #(reset! editing? true)
            :style    {:cursor "pointer"}}])


(defn EditableInput
  "Input component that provides editing behaviour:
    - activate by clicking on pencil icon
    - saves on enter key or button click
    - cancel on escape key"
  [attribute element on-change-fn]
  (let [new-value     (r/atom (get element attribute))
        initial-value (r/atom @new-value)
        editing?      (r/atom false)
        close-fn      #(reset! editing? false)
        save-fn       #(do
                         (when (not= @new-value @initial-value)
                           (on-change-fn @new-value)
                           (reset! initial-value @new-value))
                         (close-fn))]
    (fn [_attribute _element _on-change-fn]
      [ui/TableCell
       (if @editing?
         [ui/Input {:default-value @new-value
                    :on-key-press  (partial forms/on-return-key
                                            save-fn)
                    :on-key-down   (partial forms/on-escape-key
                                            #(do (reset! new-value @initial-value)
                                                 (close-fn)))
                    :on-change     (ui-callback/input-callback #(reset! new-value %))
                    :focus         true
                    :fluid         true
                    :action        {:icon     "check"
                                    :on-click save-fn}}]
         [:<>
          @new-value
          ff/nbsp
          [Pencil editing?]])])))

(defn TagsDropdown
  [{:keys [tags]}]
  (let [tr            (subscribe [::i18n-subs/tr])
        value-options (r/atom tags)]
    (fn [{:keys [on-change-fn initial-options tag-color]}]
      (let [options (map (fn [v] {:key v :text v :value v})
                         (distinct (concat (or initial-options tags) @value-options)))]
        [ui/Dropdown {:selection        true
                      :placeholder      (@tr [:type-to-add-tags])
                      :default-value    tags
                      :name             "tag"
                      :fluid             true
                      :allowAdditions   true
                      :additionLabel    (str (@tr [:add-dropdown]) " ")
                      :search           true
                      :noResultsMessage (@tr [:type-to-add-new-tag])
                      :multiple         true
                      :on-change        (ui-callback/value (fn [v] (on-change-fn v)))
                      :on-add-item      (ui-callback/value
                                         (fn [value] (reset! value-options (conj @value-options value))))
                      :options          options
                      :renderLabel      (fn [label]
                                          (r/as-element
                                           [ui/Label {:icon    "tag"
                                                      :size    "mini"
                                                      :color   (or tag-color "teal")
                                                      :content (.-value label)
                                                      :style   {:margin-top 10
                                                                :max-height 150
                                                                :overflow   "auto"}}]))}]))))

(defn EditableTags
  "Editable tags component. Allows editing (add, remove) of tags if the element is editable by the user."
  [element _on-change-fn]
  (let [tr            (subscribe [::i18n-subs/tr])
        editing?      (r/atom false)
        uuid          (random-uuid)
        editable?     (or (utils-general/can-edit? element)
                          (-> element :acl nil?))]
    (fn [{:keys [tags] :as _element} on-change-fn]
      (if @editing?
        [:div {:style {:align-items "center"}}
         [TagsDropdown {:tags tags :on-change-fn on-change-fn}]
         [ui/Button {:icon     "check"
                     :on-click #(reset! editing? false)}]]
        [ui/LabelGroup {:size  "tiny"
                        :color "teal"
                        :style {:margin-top 10, :min-height 30 :max-height 150,
                                :overflow   "auto"}}
         (for [tag tags]
           ^{:key (str uuid "_" tag)}
           [ui/Popup
            {:trigger        (r/as-element [ui/Label [ui/Icon {:name "tag"}]
                                            (utils-general/truncate tag 20)])
             :content        tag
             :position       "bottom center"
             :on             "hover"
             :size           "tiny"
             :hide-on-scroll true}])
         (when-not (seq tags)
           (if editable?
             (@tr [:add-first-tag])
             [ui/Message (@tr [:no-items-to-show])]))
         ff/nbsp
         (when editable?
           [Pencil editing?])]))))
