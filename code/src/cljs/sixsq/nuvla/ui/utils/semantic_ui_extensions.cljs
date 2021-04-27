(ns sixsq.nuvla.ui.utils.semantic-ui-extensions
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.accordion :as accordion-utils]
    [sixsq.nuvla.ui.utils.form-fields :as form-fields]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [taoensso.timbre :as log]
    [clojure.string :as str]))


(defn Icon
  [{:keys [name] :as opts}]
  [ui/Icon (cond-> opts
                   (or (str/starts-with? name "fad ")
                       (str/starts-with? name "fas ")) (-> (dissoc :name)
                                                           (assoc :className name)))])


(defn Button
  "This button requires a single options map that contains the :text key. The
   value of the :text key is used to define the button text as well as the
   accessibility label :aria-label. The button may not specify children."
  [{:keys [text icon] :as options}]
  (let [final-opts (-> options
                       (dissoc :text)
                       (dissoc :icon)
                       (assoc :aria-label text))]
    [ui/Button final-opts (when icon [ui/Icon {:name icon}]) text]))


(defn MenuItem
  "Provides a menu item that reuses the name for the :name property and as the
   MenuItem label. The optional icon-name specifies the icon to use. The
   loading? parameter specifies if the icon should be spinning."
  [{:keys [name icon loading?] :as options}]
  (let [final-opts (-> options
                       (dissoc :icon :loading?)
                       (assoc :aria-label name))]
    [ui/MenuItem final-opts
     (when icon
       [Icon (cond-> {:name icon}
                     (boolean? loading?) (assoc :loading loading?))])
     (str/capitalize name)]))


(defn MenuItemForSearch
  "Provides a standard menu item for a 'search' button. The :name property is
   used as the label and for the :aria-label value. If loading a refresh
   spinner is shown; the search icon otherwise."
  [{:keys [name loading?] :as options}]
  (let [final-opts (-> options
                       (dissoc :loading?)
                       (assoc :aria-label name))]
    [ui/MenuItem final-opts
     (if loading?
       [ui/Icon {:name "refresh", :loading loading?}]
       [ui/Icon {:name "search"}])
     name]))


(defn MenuItemSectionToggle
  "Provides a standard menu item that is intended to toggle the visibility of a
   section. There is no textual label."
  [{:keys [visible?] :as options}]
  (let [final-opts (-> options
                       (assoc :aria-label "toggle section visibility")
                       (dissoc :visible?))
        icon-name  (if visible? "chevron down" "chevron up")]
    [ui/MenuItem final-opts
     [ui/Icon {:name icon-name}]]))


(defn ModalHeader
  [{:keys [header icon]}]
  [ui/ModalHeader
   (when icon
     [ui/Icon {:name icon}])
   (str/capitalize header)])


(defn Pagination
  "Provide pagination element with more visible icons. Note: :totalitems is in lowercase not to
   interfere with React DOM attributes."
  [options]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Grid {:vertical-align "middle"
              :style          {:margin-top "20px"}}
     (when (:totalitems options)
       [ui/GridColumn {:floated "left", :width 3}
        [ui/Label {:size :medium}
         (str (@tr [:total]) ": " (:totalitems options))]])
     [ui/GridColumn {:floated "right", :width 13, :text-align "right"}
      [ui/Pagination
       (merge {:size      "tiny"
               :firstItem {:content (r/as-element [ui/Icon {:name "angle double left"}]) :icon true}
               :lastItem  {:content (r/as-element [ui/Icon {:name "angle double right"}]) :icon true}
               :prevItem  {:content (r/as-element [ui/Icon {:name "angle left"}]) :icon true}
               :nextItem  {:content (r/as-element [ui/Icon {:name "angle right"}]) :icon true}}
              options)]]]))


(defn EditorJson
  "A convenience function to setup the CodeMirror editor component for JSON."
  [text on-change-fn editable?]
  (let [default-text text]
    (fn [text on-change-fn editable?]
      [ui/CodeMirror {:value      text
                      :autoCursor true
                      :options    {:mode                "application/json"
                                   :line-numbers        true
                                   :match-brackets      true
                                   :auto-close-brackets true
                                   :style-active-line   true
                                   :fold-gutter         true
                                   :gutters             ["CodeMirror-linenumbers"]}
                      :on-change  (fn [editor data value]
                                    (reset! text value))}])))


(defn EditorYaml
  [text on-change-fn editable?]
  (fn [text on-change-fn editable?]
    [ui/CodeMirror {:value      text
                    :autoCursor true
                    :autofocus  true
                    :autoFocus  true
                    :auto-focus true
                    :options    {:mode              "text/x-yaml"
                                 :read-only         (not editable?)
                                 :line-numbers      true
                                 :style-active-line true}
                    :on-change  on-change-fn
                    :style      {:height "auto !important"}}]))


(defn EditorMarkdown
  "A convenience function to setup the CodeMirror editor component for Markdown."
  [text on-change-fn editable?]
  (fn [text on-change-fn editable?]
    [ui/CodeMirror {:value      text
                    :autoCursor true
                    :autofocus  true
                    :autoFocus  true
                    :auto-focus true
                    :options    {:mode                "text/x-markdown"
                                 :read-only           (not editable?)
                                 :lineWrapping        true
                                 :match-brackets      true
                                 :auto-close-brackets true
                                 :style-active-line   true
                                 :fold-gutter         true
                                 :gutters             ["CodeMirror-foldgutter"]}
                    :on-change  on-change-fn}]))


(defn Accordion
  [content & {:keys [id label count icon default-open title-size on-open on-close !control-open? styled?]
              :or   {default-open true, title-size :h3, on-open #(), on-close #(), styled? true}}]
  (let [active? (or !control-open? (r/atom default-open))]
    (fn [content & {:keys [id label count icon default-open title-size]
                    :or   {default-open true, title-size :h3, on-open #(), on-close #()}}]
      [ui/Accordion {:id        id
                     :fluid     true
                     :styled    styled?
                     :style     {:margin-top    "10px"
                                 :margin-bottom "10px"}
                     :exclusive false}

       [ui/AccordionTitle {:active   @active?
                           :index    1
                           :style    {:display "inline-block"
                                      :width   "100%"}
                           :on-click #(do
                                        (accordion-utils/toggle active?)
                                        (if @active? (on-open) (on-close)))}
        [title-size
         [ui/Icon {:name (if @active? "dropdown" "caret right")}]

         (when icon
           [:<> [Icon {:name icon}] " "])

         label

         (when count
           [:span form-fields/nbsp form-fields/nbsp [ui/Label {:circular true} count]])]]

       (when @active? [ui/AccordionContent {:active @active?}
                       content])])))


(defn MoreAccordion
  [content]
  (let [tr    (subscribe [::i18n-subs/tr])
        more? (r/atom false)]
    (fn [content]
      [ui/Accordion
       [ui/AccordionTitle {:on-click #(swap! more? not)
                           :active   @more?}
        [ui/Icon {:name "dropdown"}]
        (@tr [:more])]
       [ui/AccordionContent {:active @more?}
        content]])))


(defn PageHeader
  [icon title & {:keys [inline]}]
  [:h2 (when inline {:style {:display    :inline
                             :word-break :break-all}})
   [Icon {:name icon}] " " title])


(defn SpanBlockJustified
  [text]
  [:span {:style {:display    :block
                  :text-align :justify}}
   text])


(defn TableRowField
  [name & {:keys [key placeholder default-value spec on-change on-validation
                  required? editable? validate-form? type input-help-msg]}]
  (let [local-validate? (r/atom false)
        active-input?   (r/atom false)]
    (fn [name & {:keys [key placeholder default-value spec on-change on-validation
                        required? editable? validate-form? type input-help-msg]
                 :or   {editable? true, spec any?, type :input}}]
      (let [name-label  (cond-> name
                                (and editable? required?) (general-utils/mandatory-name))
            validate?   (boolean (or @local-validate? validate-form?))
            error?      (and validate? (not (s/valid? spec default-value)))
            common-opts {:default-value default-value
                         :placeholder   (or placeholder name)
                         :onMouseEnter  #(reset! active-input? true)
                         :onMouseLeave  #(reset! active-input? false)
                         :auto-complete "nope"
                         :on-change     (ui-callback/input-callback
                                          #(let [text (when-not (str/blank? %) %)]
                                             (reset! local-validate? true)
                                             (on-change text)))}]
        (when on-validation
          (dispatch [on-validation key error?]))
        [ui/TableRow
         [ui/TableCell {:collapsing true} name-label]
         ^{:key (or key name)}
         [ui/TableCell
          input-help-msg
          (if editable?
            (if (#{:input :password} type)
              [ui/Input (assoc common-opts
                          :error error?
                          :fluid true
                          :type type
                          :auto-complete "nope"
                          :icon (when @active-input? :pencil))]
              [ui/Form
               [ui/FormField {:error error?}
                [:div {:className "ui input icon"}
                 [ui/TextArea common-opts]
                 (when @active-input? [ui/Icon {:name "pencil"}])]]])
            [SpanBlockJustified default-value])]]))))


(defn LinkIcon
  [{:keys [name on-click]}]
  [:a [ui/Icon {:name name, :link true, :on-click on-click}]])


(defn ModalDanger
  [{:keys [button-text on-confirm danger-msg header content trigger open on-close modal-action
           control-confirmed?]}]
  (let [tr         (subscribe [::i18n-subs/tr])
        confirmed? (or control-confirmed? (r/atom (nil? danger-msg)))
        clicked?   (r/atom false)]
    (fn [{:keys [button-text on-confirm danger-msg header content trigger open on-close modal-action]}]
      [ui/Modal (cond->
                  {:on-click   (fn [event]
                                 (.stopPropagation event)
                                 (.preventDefault event))
                   :close-icon true
                   :trigger    trigger
                   :on-close   (fn [& args]
                                 (when on-close
                                   (apply on-close args))
                                 (reset! confirmed? (nil? danger-msg))
                                 (reset! clicked? false))}
                  (some? open) (assoc :open open))

       (when header [ui/ModalHeader (str/capitalize header)])

       [ui/ModalContent {:scrolling false}
        (when content content)
        (when danger-msg
          [ui/Message {:error true}
           [ui/MessageHeader {:style {:margin-bottom 10}} (@tr [:danger-action-cannot-be-undone])]
           [ui/MessageContent [ui/Checkbox {:label     danger-msg
                                            :checked   @confirmed?
                                            :fitted    true
                                            :on-change #(swap! confirmed? not)}]]])]

       [ui/ModalActions
        (when modal-action modal-action)
        [Button {:text     (str/lower-case button-text)
                 :negative true
                 :disabled (or (not @confirmed?) @clicked?)
                 :loading  @clicked?
                 :active   true
                 :on-click #(do (reset! clicked? true)
                                (on-confirm))}]]])))


(defn ModalFromButton
  "Defines a standard modal, triggered by a button."
  [{:keys [button-text on-confirm header icon content trigger open on-close modal-action]}]
  (let [tr       (subscribe [::i18n-subs/tr])
        clicked? (r/atom false)]
    (fn [{:keys [button-text on-confirm header icon content trigger open on-close modal-action]}]
      [ui/Modal (cond->
                  {:on-click   (fn [event]
                                 (.stopPropagation event)
                                 (.preventDefault event))
                   :close-icon true
                   :trigger    trigger
                   :on-close   (fn [& args]
                                 (when on-close
                                   (apply on-close args))
                                 (reset! clicked? false))}
                  (some? open) (assoc :open open))

       (when header [ui/ModalHeader (when icon [Icon {:name icon}]) (str/capitalize header)])

       [ui/ModalContent {:scrolling false}
        (when content content)]

       [ui/ModalActions
        (when modal-action modal-action)
        [Button {:text     (str/lower-case button-text)
                 :primary  true
                 :loading  @clicked?
                 :active   true
                 :icon     icon
                 :on-click #(do (reset! clicked? true)
                                (on-confirm))}]]])))


(defn TimeAgo
  [time-str]
  (let [locale        (subscribe [::i18n-subs/locale])
        fn-update-ago #(time/parse-ago % @locale)
        refresh       (r/atom 0)]
    (js/setInterval #(swap! refresh inc) 5000)
    (fn [time-str]
      ^{:key (str time-str @refresh)}
      [:span (fn-update-ago time-str)])))


(defn CountDown
  [futur-time]
  (let [refresh (r/atom 0)]
    (js/setInterval #(swap! refresh inc) 1000)
    (fn [futur-moment]
      (let [delta-seconds (/ (time/delta-milliseconds (time/now) futur-moment) 1000)]
        ^{:key (str futur-moment @refresh)}
        [:span
         (if (neg? delta-seconds) 0 (js/Math.round delta-seconds))]))))


(defn WarningMsgNoElements
  [message]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [message]
      [ui/Message {:warning true}
       [ui/Icon {:name "warning sign"}]
       (or message (@tr [:no-items-to-show]))])))


(defn Tags
  [{:keys [tags]}]
  (let [uuid (random-uuid)]
    (fn [{:keys [tags]}]
      [ui/LabelGroup {:size  "tiny"
                      :color "teal"
                      :style {:margin-top 10, :max-height 150, :overflow "auto"}}
       (for [tag tags]
         ^{:key (str uuid "_" tag)}
         [ui/Label {:style {:max-width     "15ch"
                            :overflow      "hidden"
                            :text-overflow "ellipsis"
                            :white-space   "nowrap"}}
          [ui/Icon {:name "tag"}] tag])])))


(defn Card
  [{:keys [header description meta image on-click href
           button tags content corner-button state loading? on-select selected?]}]
  [ui/Card (when on-click
             (cond-> {:on-click (fn [event]
                                  (on-click event)
                                  (.preventDefault event))}
                     href (assoc :href href)))
   (when on-select
     [:div {:style {:position "absolute"
                    :top      "-7px"
                    :left     "-7px"}}
      [ui/Checkbox {:style    {:z-index 1}
                    :checked  selected?
                    :on-click #(do
                                 (on-select (not selected?))
                                 (.preventDefault %)
                                 (.stopPropagation %))}]])

   (when image
     [ui/Image
      {:src      image
       :bordered true
       :style    {:width      "auto"
                  :height     "200px"
                  :object-fit "cover"}}])

   (when corner-button corner-button)

   [ui/CardContent

    (when header [ui/CardHeader
                  {:style {:word-wrap "break-word"}}
                  header])

    (when meta [ui/CardMeta meta])

    (when state
      [ui/Segment {:basic  true
                   :padded false
                   :style  {:padding    0
                            :text-align "right"
                            :margin     "0px 5px 10px"}}
       [:div
        [:p {:style {:color "initial"}} state]
        [ui/Loader {:active        loading?
                    :indeterminate true}]]])

    (when description
      [ui/CardDescription
       {:style {:overflow "hidden" :max-height "100px"}}
       description])

    (when content content)

    (when (seq tags)
      [Tags {:tags tags}])]

   (when button button)])
