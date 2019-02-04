(ns sixsq.slipstream.webui.legacy-application.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as reagent]
    [sixsq.slipstream.webui.i18n.subs :as i18n-subs]
    [sixsq.slipstream.webui.legacy-application.events :as application-events]
    [sixsq.slipstream.webui.legacy-application.subs :as application-subs]
    [sixsq.slipstream.webui.main.events :as main-events]
    [sixsq.slipstream.webui.panel :as panel]
    [sixsq.slipstream.webui.utils.collapsible-card :as cc]
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]
    [sixsq.slipstream.webui.utils.style :as style]
    [sixsq.slipstream.webui.utils.ui-callback :as ui-callback]))


(defn category-icon
  [category]
  (case category
    "Project" "folder"
    "Deployment" "sitemap"
    "Image" "microchip"
    "question circle"))


(defn format-module [{:keys [category name version description] :as module}]
  (when module
    (let [on-click #(dispatch [::main-events/push-breadcrumb name])
          icon-name (category-icon category)]
      [ui/ListItem
       [ui/ListIcon {:name           icon-name
                     :size           "large"
                     :vertical-align "middle"}]
       [ui/ListContent
        [ui/ListHeader [:a {:on-click on-click} (str name " (" version ")")]]
        [ui/ListDescription [:span description]]]])))


(defn tuple-to-row [[v1 v2]]
  [ui/TableRow
   [ui/TableCell {:description true} (str v1)]
   [ui/TableCell (str v2)]])


(defn group-table-sui
  [group-data]
  (let [data (sort-by first group-data)]
    [ui/Table style/definition
     (vec (concat [ui/TableBody]
                  (map tuple-to-row (map (juxt (comp name first) (comp str second)) data))))]))


(defn more-or-less
  [state-atom]
  (let [tr (subscribe [::i18n-subs/tr])
        more? state-atom]
    (fn [state-atom]
      (let [label (if @more? (@tr [:less]) (@tr [:more]))
            icon-name (if @more? "caret down" "caret right")]
        [:a {:style    {:cursor "pointer"}
             :on-click #(reset! more? (not @more?))} [ui/Icon {:name icon-name}] label]))))


(defn format-meta
  [{:keys [shortName version description logoLink category] :as module-meta}]
  (let [more? (reagent/atom false)]
    (fn [{:keys [shortName version description logoLink category] :as module-meta}]
      (let [data (sort-by first module-meta)]
        (when (pos? (count data))
          [ui/Card {:fluid true}
           [ui/CardContent
            (when logoLink
              [ui/Image {:floated "right"
                         :size    :tiny
                         :src     logoLink}])
            [ui/CardHeader
             [ui/Icon {:name (category-icon category)}]
             (str " " shortName " (" version ")")]
            (when description
              [ui/CardMeta
               [:p description]])
            [ui/CardDescription
             [more-or-less more?]
             (when @more?
               [group-table-sui data])]]])))))


(defn error-text [tr error]
  (if-let [{:keys [status body reason]} (ex-data error)]
    (str (or (@tr [reason]) (name reason)) " (" status ")")
    (str error)))


(defn format-error
  [error]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [error]
      (let [reason-text (error-text tr error)]
        [ui/Container
         [ui/Header {:as   "h3"
                     :icon true}
          [ui/Icon {:name "warning sign"}]
          reason-text]]))))


(defn dimmer
  []
  (let [tr (subscribe [::i18n-subs/tr])
        data (subscribe [::application-subs/module])]
    (fn []
      (let [loading? (not @data)]
        [ui/Dimmer {:active   loading?
                    :page     false
                    :inverted true}
         [ui/Header {:as       "h3"
                     :icon     true
                     :inverted false}
          [ui/Icon {:name    "refresh"
                    :loading true}]
          (@tr [:loading])]]))))


(defn format-module-children
  [module-children]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [module-children]
      (when (pos? (count module-children))
        [cc/collapsible-card
         (@tr [:modules])
         (vec (concat [ui/ListSA {:divided true
                                  :relaxed true}]
                      (map format-module module-children)))]))))


(defn parameter-table-row
  [[category name description]]
  [ui/TableRow
   [ui/TableCell category]
   [ui/TableCell name]
   [ui/TableCell description]])


(defn format-parameters
  [parameters]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [parameters]
      (when parameters
        (let [rows (map (juxt :category :name :description) parameters)]
          [cc/collapsible-card
           (@tr [:parameters])
           [ui/Table
            (vec (concat [ui/TableBody] (map parameter-table-row rows)))]])))))


;; FIXME: The first three target keywords are not correct.
(defn target-dropdown
  [state]
  [ui/Dropdown {:inline        true
                :default-value :execute
                :on-change     (ui-callback/value #(reset! state %))
                :options       [{:key "prerecipe", :value "prerecipe", :text "pre-install"}
                                {:key "packages", :value "packages", :text "packages"}
                                {:key "recipe", :value "recipe", :text "post-install"}
                                {:key "execute", :value "execute", :text "deployment"}
                                {:key "report", :value "report", :text "report"}
                                {:key "onvmadd", :value "onvmadd", :text "on VM add"}
                                {:key "onvmremove", :value "onvmremove", :text "on VM remove"}
                                {:key "prescale", :value "prescale", :text "pre-scale"}
                                {:key "postscale", :value "postscale", :text "post-scale"}]}])


(defn format-targets
  [targets]
  (let [tr (subscribe [::i18n-subs/tr])
        selected-target (reagent/atom "execute")]
    (fn [targets]
      (when targets
        [cc/collapsible-card
         [:span [target-dropdown selected-target] "target"]
         [ui/Segment
          [:pre (get targets (keyword @selected-target))]]]))))


(defn module-resource []
  (let [data (subscribe [::application-subs/module])]
    (fn []
      (let [loading? (not @data)]
        [ui/DimmerDimmable
         (vec (concat [ui/Container {:fluid true} [dimmer]]
                      (when-not loading?
                        (if (instance? js/Error @data)
                          [[format-error @data]]
                          (let [{:keys [metadata children targets parameters]} @data
                                module-type (:category metadata)]
                            [[format-meta metadata]
                             (when (= module-type "Image") [format-parameters parameters])
                             (when (= module-type "Image") [format-targets targets])
                             [format-module-children children]])))))]))))


(defmethod panel/render :legacy-application
  [path]
  (dispatch [::application-events/get-module])
  [module-resource])
