(ns sixsq.nuvla.ui.utils.semantic-ui-extensions
  (:require ["@codemirror/lang-json" :as lang-json]
            ["@codemirror/lang-markdown" :as lang-markdown]
            ["@codemirror/language" :as language]
            ["@codemirror/legacy-modes/mode/shell" :as shell]
            ["@codemirror/legacy-modes/mode/yaml" :as yaml]
            [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.config :as config]
            [sixsq.nuvla.ui.routing.subs :as routing-subs]
            [sixsq.nuvla.ui.utils.accordion :as accordion-utils]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


(defn TR
  [msg-or-key fn]
  (let [tr (subscribe [::i18n-subs/tr])]
    ((or fn identity)
     (cond
       (keyword? msg-or-key) (@tr [msg-or-key])
       (nil? msg-or-key) ""
       :else msg-or-key))))


(defn Button
  "This button requires a single options map that contains the :text key. The
   value of the :text key is used to define the button text as well as the
   accessibility label :aria-label. The button may not specify children."
  [{:keys [text icon content] :as options}]
  (let [cntn       (or text content)
        final-opts (-> options
                       (dissoc :text)
                       (assoc :content cntn)
                       (dissoc :icon)
                       (assoc :aria-label (when (string? cntn) cntn))
                       (assoc :icon (when icon (r/as-element [icons/Icon {:name icon}]))))]
    [ui/Button final-opts]))


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
       [icons/Icon (cond-> {:name icon}
                           (boolean? loading?) (assoc :loading loading?))])
     (when (string? name) (str/capitalize name))]))


(defn HighlightableMenuItem
  "Provides a menu item that reads from query params if it should be highlighted"
  [{:keys [query-key query-param-value] :or {query-key :highlight-menu-item} :as opts} & children]
  (let [highlighted? (subscribe [::routing-subs/has-query-param-value? query-key
                                 (some-> query-param-value name)])]
    (into [ui/MenuItem
           (cond-> (-> opts
                       (dissoc :query-key :query-param-value)
                       (assoc :data-highlight-key query-param-value))
                   @highlighted?
                   (general-utils/add-classes :primary-menu-item))]
          children)))


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
       [icons/ArrowRotateIcon {:loading true}]
       [icons/SearchIcon])
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
     [icons/Icon {:name icon-name}]]))

(defn HelpPopup
  [description & {:keys [on] :or {on "hover"}}]
  (when description
    [ui/Popup
     {:trigger        (r/as-element
                        [:span
                         [icons/QuestionCircleIcon
                          {:style {:margin-left 2}}]])
      :content        description
      :on             on
      :position       "top center"
      :hide-on-scroll true}]))


(defn ModalHeader
  [{:keys [header icon]}]
  [ui/ModalHeader
   (when icon
     [icons/Icon {:name icon}])
   (general-utils/capitalize-first-letter header)])

(defn Msg
  [{:keys [icon content header type size] :as _ops}]
  [ui/Message
   (cond-> {}
           size (assoc :size size)
           type (assoc type true)
           icon (assoc :icon true))
   (when icon [icons/Icon {:name icon}])
   [ui/MessageContent
    [ui/MessageHeader
     (when header header)]
    (when content content)]])

(defn MsgInfo
  [opts]
  [Msg (assoc opts
         :type :info
         :icon icons/i-info-full)])

(defn MsgWarn
  [opts]
  [Msg (assoc opts
         :type :warning
         :icon icons/i-warning)])

(defn MsgError
  [opts]
  [Msg (assoc opts
         :type :error
         :icon icons/i-warning)])

(defn MsgNoItemsToShow
  [message]
  [Msg {:content (or message [TR :no-items-to-show])}])

(defn ModalActionButton
  [{:keys [show?] :as _opts}]
  (let [tr               (subscribe [::i18n-subs/tr])
        show?            (or show? (r/atom false))
        validation-error (r/atom nil)]
    (fn [{:keys [menu-item-label button-confirm-label icon title-text Content
                 on-confirm on-cancel scrolling? validate-fn ValidationError Trigger
                 ExtraButton]
          :or   {scrolling?      false
                 on-confirm      #()
                 on-cancel       #()
                 validate-fn     identity
                 ValidationError (fn [error]
                                   [MsgError {:header  "A validation error happened"
                                              :content error}])}}]
      (let [reset-fn  #(do (reset! show? false)
                           (reset! validation-error nil))
            action-fn #(let [result (validate-fn)]
                         (if (instance? js/Error result)
                           (reset! validation-error (str result))
                           (do (on-confirm result)
                               (reset-fn))))
            cancel-fn (fn []
                        (reset-fn)
                        (on-cancel))]
        [ui/Modal
         {:open       (boolean @show?)
          :close-icon true
          :on-click   #(.stopPropagation %)
          :on-close   cancel-fn
          :trigger    (r/as-element
                        (or Trigger
                            [ui/MenuItem {:aria-label menu-item-label
                                          :name       menu-item-label
                                          :on-click   #(reset! show? true)}
                             (when icon
                               [icons/Icon {:name icon}])
                             (str/capitalize menu-item-label)]))}
         [ModalHeader {:header title-text}]
         [ui/ModalContent {:scrolling (boolean scrolling?)}
          [:<>
           (when @validation-error [ValidationError @validation-error])
           Content]]
         [ui/ModalActions
          (when ExtraButton ExtraButton)
          [Button
           {:text     (@tr [:cancel])
            :on-click cancel-fn}]
          [Button
           {:text     button-confirm-label
            :primary  true
            :on-click action-fn}]]]))))

(defn CopyToClipboard
  [{:keys [content value popup-text on-hover?] :as _opts}]
  [ui/CopyToClipboard {:text value}
   [:span
    [ui/Popup
     {:content (r/as-element [:p [TR (or popup-text :copy-to-clipboard)]])
      :trigger (r/as-element
                 [:span (cond-> {:style {:cursor :pointer}}
                                on-hover? (assoc :class ["show-on-hover-value"]))
                  (or content value)
                  general-utils/nbsp
                  [ui/Icon
                   {:class [(when on-hover? "hide")]
                    :name  "clone outline"
                    :color "blue"
                    :style {:color "black"}}]])
      :size    "tiny"}]]])


(defn CopyToClipboardDownload
  [{:keys [name value copy download filename]
    :or {filename "file.txt"
         copy     true
         download false} :as _ops}]
  [ui/Segment {:inverted true
               :color    "grey"}
   [ui/Grid {:stackable true}
    [ui/GridRow {:columns 2}
     [ui/GridColumn {:width 14
                     :style {:text-overflow :ellipsis
                             :overflow      :hidden
                             :white-space   :nowrap}}
      [:b (str name ": \u000B")]
      value]
     [ui/GridColumn {:width      2
                     :text-align "right"}
      (when download
        [ui/Popup
         {:trigger (r/as-element
                     [:a {:style    {:color "white"}
                          :href     (str "data:text/plain;charset=utf-8,"
                                         (js/encodeURIComponent value))
                          :target   "_blank"
                          :download filename}
                      [icons/Icon {:link true
                                   :size "large"
                                   :name "download"}]])}
         [TR :click-to-download]])
      (when copy
        [ui/Popup
         {:trigger  (r/as-element
                      [:span [ui/CopyToClipboard {:text value}
                              [:span [icons/Icon
                                      {:style {:margin-left 10}
                                       :link  true
                                       :size  "large"
                                       :name  "clone"}]]]])
          :position "top center"}
         [TR :click-to-copy]])]]]])

(defn EditorCode
  [{:keys [value] :as props}]
  [ui/CodeMirror
   (assoc props :value (or value "")
                ;; forceParsing upto 999999 and within 100ms timeout
                :on-create-editor #(language/forceParsing % 999999 100))])

(defn EditorJson
  [props]
  [EditorCode
   (assoc props
     :extensions [(lang-json/json)])])

(defn EditorYaml
  [props]
  [EditorCode
   (assoc props
     :extensions [(.define language/StreamLanguage yaml/yaml)])])

(defn EditorShell
  [props]
  ; autocompletion disabled because doesn't play well with modal
  [EditorCode
   (assoc props
     :basic-setup {:autocompletion false}
     :extensions [(.define language/StreamLanguage shell/shell)])])

(defn EditorMarkdown
  [props]
  [EditorCode
   (assoc props
     :basic-setup {:line-numbers false}
     :extensions [(lang-markdown/markdown)])])


(defn Accordion
  [_content & {:keys [_id _label _count default-open _title-size _on-open _on-close !control-open? _icon _styled?]
               :or   {default-open true}}]
  (let [active? (or !control-open? (r/atom default-open))]
    (fn [content & {:keys [id label count _default-open title-size on-open on-close _!control-open? icon styled?
                           title-class]
                    :or   {title-size :h3, on-open #(), on-close #(), styled? true}}]
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
         {:class title-class}

         [icons/Icon {:name (if @active? icons/i-angle-down icons/i-angle-right)}]

         (when icon
           [:<> [icons/Icon {:name icon}] " "])

         label

         (when count
           [:span " " [ui/Label {:circular true} count]])]]

       (when @active? [ui/AccordionContent {:active @active?}
                       content])])))


(defn PageHeader
  [icon title & {:keys [inline color]}]
  [:h2 (when inline {:style {:display    :inline
                             :word-break :break-all}})
   [icons/Icon {:name  icon
                :color color}] " " title])


(defn SpanBlockJustified
  [text]
  [:span {:style {:display    :block
                  :text-align :justify}}
   text])


(defn TableRowCell
  [{:keys [_key _placeholder _default-value _spec _width _on-change _on-validation _style
           _editable? _validate-form? _type _input-help-msg]}]
  (let [local-validate? (r/atom false)
        active-input?   (r/atom false)
        show            (r/atom false)]
    (fn [{:keys [key placeholder default-value spec width on-change on-validation show-pencil?
                 editable? validate-form? type input-help-msg style options input-extra-options]
          :or   {editable?    true, spec any?, type :input
                 show-pencil? true}}]
      (let [validate?       (boolean (or @local-validate? validate-form?))
            error?          (and validate? (not (s/valid? spec default-value)))
            input-cbk       (ui-callback/input-callback
                              #(let [text (when-not (str/blank? %) %)]
                                 (reset! local-validate? true)
                                 (on-change text)))
            multiselect-cbk (ui-callback/value
                              #(let [values (when-not (empty? %) %)]
                                 (reset! local-validate? true)
                                 (on-change values)))
            common-opts     (merge {:default-value default-value
                                    :placeholder   placeholder
                                    :onMouseEnter  #(reset! active-input? true)
                                    :onMouseLeave  #(reset! active-input? false)
                                    :auto-complete "nope"}
                                   input-extra-options)
            icon            (cond
                              (= :password type) [icons/Icon {:name     (if @show "eye slash" :eye)
                                                              :link     true
                                                              :on-click #(swap! show not)}]
                              (and show-pencil? @active-input?) [icons/PencilIcon])]
        (when on-validation
          (dispatch [on-validation key error?]))

        ^{:key (or key name)}
        [ui/TableCell (merge (when width {:width width})
                             (when style {:style style}))
         input-help-msg
         (if editable?
           (cond
             (#{:input :password} type)
             [ui/Input (assoc common-opts
                         :error error?
                         :fluid true
                         :type (if @show :input type)
                         :auto-complete "nope"
                         :icon (r/as-element icon)
                         :on-change input-cbk)]

             (= :dropdown type)
             [ui/Dropdown (merge common-opts
                                 {:selection   true
                                  :multiple    true
                                  :compact     true
                                  :placeholder " "
                                  :search      true
                                  :options     options
                                  :on-change   multiselect-cbk})]

             :else
             [ui/Form
              [ui/FormField {:error error?}
               [:div {:className "ui input icon"}
                [ui/TextArea (assoc common-opts :on-change input-cbk)]
                (when @active-input? [icons/Icon {:name icons/i-pencil}])]]])
           [SpanBlockJustified (if (coll? default-value)
                                 (str/join ", " default-value)
                                 default-value)])]))))


(defn FieldLabel
  [{:keys [name required? help-popup]} & children]
  (into [:label name
         (when required?
           [:sup
            [icons/AsteriskIcon
             {:color "red"
              :size  "tiny"
              :style {:margin 0}}]])
         (when help-popup
           help-popup)]
        children))

(defn TableRowField
  [_name & {:keys [_key _placeholder _default-value _spec _on-change _on-validation _style
                   _required? _editable? _validate-form? _type _help-popup]}]
  (let [local-validate? (r/atom false)]
    (fn [name & {:keys [key _placeholder default-value spec _on-change on-validation
                        required? editable? validate-form? _type help-popup _style]
                 :or   {editable? true, spec any?}
                 :as   options}]
      (let [validate? (boolean (or @local-validate? validate-form?))
            error?    (and validate? (not (s/valid? spec default-value)))]
        (when on-validation
          (dispatch [on-validation key error?]))
        [ui/TableRow
         [ui/TableCell {:collapsing true}
          [FieldLabel {:name       name
                       :required?  (and editable? required?)
                       :help-popup help-popup}]]
         ^{:key (or key name)}
         [TableRowCell options]]))))

(defn LinkIcon
  [{:keys [name on-click color class aria-label]}]
  [:a {:class class :aria-label aria-label} [icons/Icon {:name name, :link true, :on-click on-click :color color}]])

(defn link-on-click
  [href event]
  (when-not (.-metaKey event)                               ;;cmd key not pressed
    (dispatch [:sixsq.nuvla.ui.routing.events/navigate href])
    (.preventDefault event)
    (.stopPropagation event)))


(defn Link
  "Renders a link that will navigate to the given href when clicked. The href
   value will also be used as the label, unless an explicit label is provided."
  [href & [label new-tab :as x]]
  [:a (cond-> {:href     (str @config/path-prefix "/" href)
               :style    {:overflow      "hidden",
                          :text-overflow "ellipsis",
                          :max-width     "20ch"}
               :target   "_blank"
               :on-click (partial link-on-click href)}
              new-tab (dissoc :on-click))
   (or label href)])

(defn ButtonAskingForConfirmation
  [_]
  (let [mode (r/atom :idle)
        tr   (subscribe [::i18n-subs/tr])]
    (fn [{:keys [color text update-event disabled? action-aria-label button-fn icon]}]
      (let [confirm-prefix (str (str/capitalize (@tr [:yes])) ": ")]
        (if (= :idle @mode)
          [Button {:text       (str text "?")
                   :aria-label action-aria-label
                   :color      (or color :red)
                   :disabled   disabled?
                   :active     true
                   :icon       (or icon icons/i-check-full)
                   :style      {:margin-left "2rem"}
                   :on-click   #(reset! mode :confirming)}]
          [:div
           [:span (str (@tr [:are-you-sure?]) " ")]
           (if button-fn
             [button-fn confirm-prefix]
             [Button {:text     (str confirm-prefix text)
                      :disabled disabled?
                      :color    #(or color :red)
                      :on-click #(if (fn? update-event)
                                   (update-event)
                                   (dispatch update-event))}])
           [ui/Button {:on-click #(reset! mode :idle)}
            [icons/XMarkIcon {:style {:margin 0}}]]])))))

(defn ModalDanger
  [{:keys [_button-text _on-confirm danger-msg _danger-msg-header _header _content _trigger _open _on-close _modal-action
           control-confirmed? header-class with-confirm-step? _all-confirmed?]}]
  (let [confirmed? (or control-confirmed? (r/atom (nil? danger-msg)))
        clicked?   (r/atom false)]
    (fn [{:keys [button-text on-confirm danger-msg danger-msg-header header content trigger open on-close modal-action all-confirmed?]}]
      (let [button (fn [added-text]
                     [Button {:text     (str added-text button-text)
                              :negative true
                              :disabled (or (false? all-confirmed?) (not @confirmed?) @clicked?)
                              :loading  @clicked?
                              :active   true
                              :on-click #(do (reset! clicked? true)
                                             (on-confirm))}])]
        [ui/Modal (cond->
                    {:on-click   (fn [event]
                                   (.stopPropagation event)
                                   (.preventDefault event))
                     :close-icon true
                     :trigger    trigger
                     :on-close   (fn [& args]
                                   (when on-close
                                     (apply on-close args)))
                     :on-unmount (fn []
                                   (when-not control-confirmed?
                                     (reset! confirmed? (nil? danger-msg)))
                                   (reset! clicked? false))}
                    (some? open) (assoc :open open))

         (when header [ui/ModalHeader {:class header-class}
                       (if (string? header)
                         (str/capitalize header)
                         header)])

         [ui/ModalContent {:scrolling false}
          (when content content)
          (when danger-msg
            [ui/Message {:error true}
             [ui/MessageHeader {:style {:margin-bottom 10}}
              (or danger-msg-header [TR :danger-action-cannot-be-undone])]
             [ui/MessageContent [ui/Checkbox {:label     danger-msg
                                              :checked   @confirmed?
                                              :fitted    true
                                              :on-change #(swap! confirmed? not)}]]])]

         [ui/ModalActions
          (when modal-action modal-action)
          (if with-confirm-step?
            [ButtonAskingForConfirmation {:button-fn         button :text button-text
                                          :action-aria-label button-text
                                          :disabled?         (or (false? all-confirmed?) (not @confirmed?) @clicked?)}]
            [button])]]))))


(defn ModalFromButton
  "Defines a standard modal, triggered by a button."
  [{:keys [_button-text _on-confirm _header _icon _content _trigger _open _on-close _modal-action]}]
  (let [clicked? (r/atom false)]
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

       (when header [ui/ModalHeader (when icon [icons/Icon {:name icon}]) (str/capitalize header)])

       [ui/ModalContent {:scrolling false}
        (when content content)]

       (when (or button-text modal-action)
         [ui/ModalActions
          (when modal-action modal-action)
          [Button {:text     (str/lower-case button-text)
                   :primary  true
                   :loading  @clicked?
                   :active   true
                   :icon     icon
                   :on-click #(do (reset! clicked? true)
                                  (on-confirm))}]])])))

(defn RerenderOnRecomputeChange
  [{:keys [Component recompute-fn delay data]
    :or   {delay 5000}}]
  (r/with-let
    [data!           (atom data)
     recomputed-data (r/atom (recompute-fn @data!))
     interval-id     (js/setInterval #(let [result (recompute-fn @data!)]
                                        (when (not= result @recomputed-data)
                                          (reset! recomputed-data result))) delay)]
    (reset! data! data)
    [Component @recomputed-data]
    (finally
      (js/clearInterval interval-id))))


(defn TimeAgo
  [time-str]
  (r/with-let [locale (subscribe [::i18n-subs/locale])
               C      (fn [[t ago]]
                        (when t
                          [ui/Popup {:trigger           (r/as-element [:span ago])
                                     :size              "small"
                                     :mouse-enter-delay 700
                                     :content           t}]))
               f      #(when % [% (time/parse-ago % @locale)])]
    [RerenderOnRecomputeChange
     {:Component    C
      :recompute-fn f
      :data         time-str}]))

(defn CountDown
  [futur-moment]
  (r/with-let [C (fn [d] [:span d])
               f #(when-let [d (some->> % (time/delta-milliseconds (time/now)))]
                    (if (neg? d) 0 (int (/ d 1000))))]
    [RerenderOnRecomputeChange
     {:Component    C
      :recompute-fn f
      :data         futur-moment
      :delay        1000}]))

(defn Tags
  [tags]
  [ui/LabelGroup {:size  "tiny"
                  :color "teal"
                  :style {:margin-top 10, :max-height 150, :overflow "auto"}}
   (for [tag tags]
     ^{:key (random-uuid)}
     [ui/Popup
      {:trigger        (r/as-element [ui/Label [icons/Icon {:name "tag"}]
                                      (general-utils/truncate tag 20)])
       :content        tag
       :position       "bottom center"
       :on             "hover"
       :size           "tiny"
       :hide-on-scroll true}])])


(defn Card
  "Wrapper around Semantic UI's Card.
   Small warning: If `:href` with valid app URL and `:on-click` handler dispatching ::events/navigate
   are provided, app navigates 2 times. Not dangerous, but unexpected for the user
   who would have to click back 2 times to get to previous page.
   "
  [{:keys [header description meta image on-click href target button tags content
           corner-button state left-state loading? on-select selected? extra class]}]
  [ui/Card (-> {:href      href
                :className class
                :target    target}
               (merge (when on-click {:on-click (fn [event]
                                                  (on-click event)
                                                  (.preventDefault event))})))
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
       :class    :card-image-centered
       :style    {:width      "auto"
                  :height     "200px"
                  :object-fit "cover"
                  :background "white"}}])

   (when corner-button corner-button)

   [ui/CardContent

    (when header [ui/CardHeader
                  {:style {:word-wrap "break-word"}}
                  header])

    (when meta [ui/CardMeta meta])

    (when (or state left-state)
      [ui/Grid {:style   {:color          "initial"
                          :padding-bottom 5
                          :padding-top    5}
                :columns 2}
       [ui/GridColumn left-state]
       [ui/GridColumn {:text-align :right}
        [ui/Loader {:active        loading?
                    :indeterminate true}]
        state]])

    (when description
      [ui/CardDescription
       {:style {:overflow "hidden" :max-height "250px"}}
       description])

    (when content content)

    (when (seq tags)
      [Tags tags])]

   (when button button)

   (when extra [ui/CardContent {:extra true}
                extra])])


(defn TokenSubmitter
  [_opts]
  (let [token (r/atom nil)]
    (fn [{:keys [character-count
                 on-submit
                 on-change
                 regex-characters]
          :or   {character-count  6
                 regex-characters #"\d+"
                 on-submit        #()
                 on-change        #()}
          :as   _opts}]
      [ui/Input
       {:icon          "key"
        :icon-position "left"
        :auto-focus    "on"
        :auto-complete "one-time-code"
        :value         (or @token "")
        :on-change     (ui-callback/input-callback
                         #(do
                            (on-change %1)
                            (reset! token
                                    (-> (re-find regex-characters %1)
                                        (or "")
                                        (subs 0 character-count)))
                            (when (= (count @token) character-count)
                              (on-submit @token)
                              (reset! token nil))))}])))

(defn label-group-overflow-detector
  [Component]
  (r/with-let [ref        (atom nil)
               overflow?  (r/atom false)
               show-more? (r/atom false)]
    (r/create-class
      {:display-name        "LabelGroupOverflow"
       :reagent-render      (fn [args]
                              [:div
                               [:div {:ref   #(reset! ref %)
                                      :style {:overflow-y :hidden
                                              :overflow-x :auto
                                              :max-height (if @show-more? nil "15ch")}}
                                [Component args]]
                               (when @overflow?
                                 [:div {:style {:display         :flex
                                                :justify-content :center}}
                                  [ui/Button {:style    {:margin-top    "0.5em"
                                                         :margin-bottom "0.5em"}
                                              :basic    true
                                              :on-click #(swap! show-more? not)
                                              :size     :mini} (if @show-more? "▲" "▼")]])])
       :component-did-mount #(reset! overflow? (general-utils/overflowed? @ref))})))
