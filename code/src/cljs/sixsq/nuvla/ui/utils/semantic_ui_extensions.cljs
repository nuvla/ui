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
            [sixsq.nuvla.ui.config :as config]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.utils.accordion :as accordion-utils]
            [sixsq.nuvla.ui.utils.form-fields :as form-fields]
            [sixsq.nuvla.ui.utils.general :as utils-general]
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
       [icons/Icon {:name "refresh", :loading loading?}]
       [icons/Icon {:name "search"}])
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


(defn ModalHeader
  [{:keys [header icon]}]
  [ui/ModalHeader
   (when icon
     [icons/Icon {:name icon}])
   (utils-general/capitalize-first-letter header)])


(defn Message
  [{:keys [icon content header type] :as _ops}]
  [ui/Message
   (cond-> {}
           type (assoc type true)
           icon (assoc :icon true))
   (when icon [icons/Icon {:name icon}])
   [ui/MessageContent
    [ui/MessageHeader
     (when header header)]
    (when content content)]])


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
         {:trigger (r/as-element
                     [ui/CopyToClipboard {:text value}
                      [:span
                       [icons/Icon {:style {:margin-left 10}
                                    :link  true
                                    :size  "large"
                                    :name  "clone"}]]])}
         [TR :click-to-copy]])]]]])

(defn EditorCode
  [{:keys [value] :as props}]
  [ui/CodeMirror
   (assoc props :value (or value ""))])

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
                     :fluid      true
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
           [:span form-fields/nbsp form-fields/nbsp [ui/Label {:circular true} count]])]]

       (when @active? [ui/AccordionContent {:active @active?}
                       content])])))


(defn PageHeader
  [icon title & {:keys [inline]}]
  [:h2 (when inline {:style {:display    :inline
                             :word-break :break-all}})
   [icons/Icon {:name icon}] " " title])


(defn SpanBlockJustified
  [text]
  [:span {:style {:display    :block
                  :text-align :justify}}
   text])


(defn TableRowCell
  [{:keys [_key _placeholder _default-value _spec _width _on-change _on-validation
           _editable? _validate-form? _type _input-help-msg]}]
  (let [local-validate? (r/atom false)
        active-input?   (r/atom false)
        show            (r/atom false)]
    (fn [{:keys [key placeholder default-value spec width on-change on-validation
                 editable? validate-form? type input-help-msg]
          :or   {editable? true, spec any?, type :input}}]
      (let [validate?   (boolean (or @local-validate? validate-form?))
            error?      (and validate? (not (s/valid? spec default-value)))
            common-opts {:default-value default-value
                         :placeholder   placeholder
                         :onMouseEnter  #(reset! active-input? true)
                         :onMouseLeave  #(reset! active-input? false)
                         :auto-complete "nope"
                         :on-change     (ui-callback/input-callback
                                          #(let [text (when-not (str/blank? %) %)]
                                             (reset! local-validate? true)
                                             (on-change text)))}
            icon        (cond
                          (= :password type) [icons/Icon {:name     (if @show "eye slash" :eye)
                                                          :link     true
                                                          :on-click #(swap! show not)}]
                          @active-input? [icons/PencilIcon])]
        (when on-validation
          (dispatch [on-validation key error?]))

        ^{:key (or key name)}
        [ui/TableCell (when width {:width width})
         input-help-msg
         (if editable?
           (if (#{:input :password} type)
             [ui/Input (assoc common-opts
                         :error error?
                         :fluid true
                         :type (if @show :input type)
                         :auto-complete "nope"
                         :icon (r/as-element icon))]
             [ui/Form
              [ui/FormField {:error error?}
               [:div {:className "ui input icon"}
                [ui/TextArea common-opts]
                (when @active-input? [icons/Icon {:name icons/i-pencil}])]]])
           [SpanBlockJustified default-value])]))))


(defn TableRowField
  [_name & {:keys [_key _placeholder _default-value _spec _on-change _on-validation
                   _required? _editable? _validate-form? _type _input-help-msg]}]
  (let [local-validate? (r/atom false)]
    (fn [name & {:keys [key _placeholder default-value spec _on-change on-validation
                        required? editable? validate-form? _type _input-help-msg]
                 :or   {editable? true, spec any?}
                 :as   options}]
      (let [name-label (cond-> name
                               (and editable? required?) (utils-general/mandatory-name))
            validate?  (boolean (or @local-validate? validate-form?))
            error?     (and validate? (not (s/valid? spec default-value)))]
        (when on-validation
          (dispatch [on-validation key error?]))
        [ui/TableRow
         [ui/TableCell {:collapsing true} name-label]
         ^{:key (or key name)}
         [TableRowCell options]]))))


(defn LinkIcon
  [{:keys [name on-click]}]
  [:a [icons/Icon {:name name, :link true, :on-click on-click}]])


(defn Link
  "Renders a link that will navigate to the given href when clicked. The href
   value will also be used as the label, unless an explicit label is provided."
  [href & [label]]
  [:a {:href     (str @config/path-prefix "/" href)
       :style    {:overflow      "hidden",
                  :text-overflow "ellipsis",
                  :max-width     "20ch"}
       :target   "_blank"
       :on-click (fn [event]
                   (when-not (.-metaKey event)              ;;cmd key not pressed
                     (dispatch [:sixsq.nuvla.ui.routing.events/navigate href])
                     (.preventDefault event)
                     (.stopPropagation event)))}
   (or label href)])

(defn ButtonAskingForConfirmation
  [_]
  (let [mode (r/atom :idle)
        tr   (subscribe [::i18n-subs/tr])]
    (fn [{:keys [color text update-event disabled? action-aria-label button-fn]}]
      (let [confirm-prefix (str (str/capitalize (@tr [:yes])) ": ")]
        (if (= :idle @mode)
          [:div
           [:span (str text "?")]
           [ui/Button {:aria-label action-aria-label
                       :color    (or color :red)
                       :disabled disabled?
                       :active   true
                       :style    {:margin-left "2rem"}
                       :on-click (fn [] (reset! mode :confirming))}
            [icons/CheckIconFull {:style {:margin 0}}]]]
          [:div
           [:span (str (@tr [:are-you-sure?]) " ")]
           (if button-fn
             [button-fn confirm-prefix]
             [Button {:text     (str confirm-prefix text)
                      :disabled disabled?
                      :color    (or color :red)
                      :on-click (fn [] (if (fn? update-event)
                                         (update-event)
                                         (dispatch update-event)))}])
           [ui/Button {:on-click (fn [] (reset! mode :idle))}
            [icons/XMarkIcon {:style {:margin 0}}]]])))))

(defn ModalDanger
  [{:keys [_button-text _on-confirm danger-msg _header _content _trigger _open _on-close _modal-action
           control-confirmed? header-class with-confirm-step?]}]
  (let [confirmed? (or control-confirmed? (r/atom (nil? danger-msg)))
        clicked?   (r/atom false)]
    (fn [{:keys [button-text on-confirm danger-msg header content trigger open on-close modal-action]}]
      (let [button (fn [added-text]
                     [Button {:text     (str added-text button-text)
                              :negative true
                              :disabled (or (not @confirmed?) @clicked?)
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
                                    (apply on-close args))
                                  (reset! confirmed? (nil? danger-msg))
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
             [ui/MessageHeader {:style {:margin-bottom 10}} [TR :danger-action-cannot-be-undone]]
             [ui/MessageContent [ui/Checkbox {:label     danger-msg
                                              :checked   @confirmed?
                                              :fitted    true
                                              :on-change #(swap! confirmed? not)}]]])]

         [ui/ModalActions
          (when modal-action modal-action)
          (if with-confirm-step?
            [ButtonAskingForConfirmation {:button-fn button :text button-text
                                          :action-aria-label button-text
                                          :disabled? (or (not @confirmed?) @clicked?)}]
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


(defn TimeAgo
  [_time-str]
  (let [locale        (subscribe [::i18n-subs/locale])
        fn-update-ago #(time/parse-ago % @locale)
        refresh       (r/atom 0)]
    (js/setInterval #(swap! refresh inc) 5000)
    (fn [time-str]
      ^{:key (str time-str @refresh)}
      [:span (fn-update-ago time-str)])))


(defn CountDown
  [_futur-moment]
  (let [refresh (r/atom 0)]
    (js/setInterval #(swap! refresh inc) 1000)
    (fn [futur-moment]
      (let [delta-seconds (/ (time/delta-milliseconds (time/now) futur-moment) 1000)]
        ^{:key (str futur-moment @refresh)}
        [:span
         (if (neg? delta-seconds) 0 (js/Math.round delta-seconds))]))))


(defn WarningMsgNoElements
  [_message]
  (fn [message]
    [ui/Message {:info true}
     (or message [TR :no-items-to-show])]))


(defn Tags
  [tags]
  [ui/LabelGroup {:size  "tiny"
                  :color "teal"
                  :style {:margin-top 10, :max-height 150, :overflow "auto"}}
   (for [tag tags]
     ^{:key (random-uuid)}
     [ui/Popup
      {:trigger        (r/as-element [ui/Label [icons/Icon {:name "tag"}]
                                      (utils-general/truncate tag 20)])
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
  [{:keys [header description meta image on-click href button tags content
           corner-button state left-state loading? on-select selected? extra]}]
  [ui/Card (-> {:href href}
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
                          :padding-bottom 5}
                :columns 2}
       [ui/GridColumn left-state]
       [ui/GridColumn {:text-align :right}
        [ui/Loader {:active        loading?
                    :indeterminate true}]
        state]])

    (when description
      [ui/CardDescription
       {:style {:overflow "hidden" :max-height "100px"}}
       description])

    (when content content)

    (when (seq tags)
      [Tags tags])]

   (when button button)

   (when extra [ui/CardContent {:extra true}
                extra])])


(defn TokenSubmiter
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
        :auto-complete "off"
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
