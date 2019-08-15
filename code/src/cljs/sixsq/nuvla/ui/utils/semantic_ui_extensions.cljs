(ns sixsq.nuvla.ui.utils.semantic-ui-extensions
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.accordion :as accordion-utils]
    [sixsq.nuvla.ui.utils.form-fields :as form-fields]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [taoensso.timbre :as log]))


(defn Button
  "This button requires a single options map that contains the :text key. The
   value of the :text key is used to define the button text as well as the
   accessibility label :aria-label. The button may not specify children."
  [{:keys [text] :as options}]
  (let [final-opts (-> options
                       (dissoc :text)
                       (assoc :aria-label text))]
    [ui/Button final-opts text]))


(defn MenuItemWithIcon
  "Provides a menu item that reuses the name for the :name property and as the
   MenuItem label. The optional icon-name specifies the icon to use. The
   loading? parameter specifies if the icon should be spinning."
  [{:keys [name icon-name loading?] :as options}]
  (let [final-opts (-> options
                       (dissoc :icon-name :loading?)
                       (assoc :aria-label name))]
    [ui/MenuItem final-opts
     (when icon-name
       [ui/Icon (cond-> {:name icon-name}
                        (boolean? loading?) (assoc :loading loading?))])
     name]))


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


(defn MenuItemForFilter
  "Provides a standard menu item for the filter button that toggles the
   visibility of a filter panel. The :name property is used as the label and
   for the :aria-label value."
  [{:keys [name visible?] :as options}]
  (let [final-opts (-> options
                       (dissoc :visible?)
                       (assoc :aria-label name))]
    [ui/MenuMenu {:position "right"}
     [ui/MenuItem final-opts
      [ui/IconGroup
       [ui/Icon {:name "filter"}]
       [ui/Icon {:name   (if visible? "chevron down" "chevron right")
                 :corner true}]]
      name]]))


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


(defn Pagination
  "Provide pagination element with more visible icons. Note: :totalitems is in lowercase not to
   interfere with React DOM attributes."
  [options]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Grid {:vertical-align "middle"}
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
  [text]
  (let [default-text @text]
    (fn [text]
      [ui/CodeMirror {:value     default-text
                      :options   {:mode                "application/json"
                                  :line-numbers        true
                                  :match-brackets      true
                                  :auto-close-brackets true
                                  :style-active-line   true
                                  :fold-gutter         true
                                  :gutters             ["CodeMirror-foldgutter"]}
                      :on-change (fn [editor data value]
                                   (reset! text value))}])))


(defn Accordion
  [content & {:keys [label count icon default-open title-size] :or {default-open true
                                                                    title-size   :h3}}]
  (let [active? (r/atom default-open)]
    (fn [content & {:keys [label count icon default-open title-size] :or {default-open true
                                                                          title-size   :h3}}]
      [ui/Accordion {:fluid     true
                     :styled    true
                     :style     {:margin-top    "10px"
                                 :margin-bottom "10px"}
                     :exclusive false}

       [ui/AccordionTitle {:active   @active?
                           :index    1
                           :on-click #(accordion-utils/toggle active?)}
        [title-size
         [ui/Icon {:name (if @active? "dropdown" "caret right")}]

         (when icon
           [ui/Icon {:name icon}])

         label

         (when count
           [:span form-fields/nbsp form-fields/nbsp [ui/Label {:circular true} count]])]]

       [ui/AccordionContent {:active @active?}
        content]])))


(defn PageHeader
  [icon title & {:keys [inline]}]
  [:h2 (when inline {:style {:display    :inline
                             :word-break :break-all}})
   [ui/Icon {:name icon}] " " title])


(defn SpanBlockJustified
  [text]
  [:span {:style {:display    :block
                  :text-align :justify}}
   text])


(defn TableRowField
  [name & {:keys [key placeholder default-value spec on-change
                  required? editable? validate-form? type]}]
  (let [local-validate? (r/atom false)
        active-input?   (r/atom false)]
    (fn [name & {:keys [key placeholder default-value spec on-change required?
                        editable? validate-form? type]
                 :or   {editable? true, spec any?, type :input}}]
      (let [name-label  (cond-> name
                                (and editable? required?) (general-utils/mandatory-name))
            validate?   (boolean (or @local-validate? validate-form?))
            error?      (and validate? (not (s/valid? spec default-value)))
            common-opts {:default-value default-value
                         :placeholder   (or placeholder name)
                         :onMouseEnter  #(reset! active-input? true)
                         :onMouseLeave  #(reset! active-input? false)
                         :on-change     (ui-callback/input-callback
                                          #(let [text (when-not (str/blank? %) %)]
                                             (reset! local-validate? true)
                                             (on-change text)))}]
        [ui/TableRow
         [ui/TableCell {:collapsing true} name-label]
         ^{:key (or key name)}
         [ui/TableCell
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
