(ns sixsq.nuvla.ui.utils.semantic-ui-extensions
  (:require
    [re-frame.core :refer [subscribe]]
    [reagent.core :as reagent]
    [reagent.core :as r]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.accordion :as accordion-utils]
    [sixsq.nuvla.ui.utils.form-fields :as form-fields]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]))


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
  "Provide pagination element with more visible icons. Note: :totalitems is in lowercase not to interfere with
   React DOM attributes."
  [options]
  (let [tr (subscribe [::i18n-subs/tr])]
    [:div
     (when (:totalitems options)
       [ui/Label {:style {:float      :left
                          :margin-top 10}
                  :size  :medium}
        (str (@tr [:total]) ": " (:totalitems options))])
     [ui/Pagination
      (merge {:firstItem {:content (reagent/as-element [ui/Icon {:name "angle double left"}]) :icon true}
              :lastItem  {:content (reagent/as-element [ui/Icon {:name "angle double right"}]) :icon true}
              :prevItem  {:content (reagent/as-element [ui/Icon {:name "angle left"}]) :icon true}
              :nextItem  {:content (reagent/as-element [ui/Icon {:name "angle right"}]) :icon true}}
             (merge {:floated :right, :size "tiny"} options))]]))


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