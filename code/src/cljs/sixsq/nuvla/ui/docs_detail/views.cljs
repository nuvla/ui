(ns sixsq.nuvla.ui.docs-detail.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.docs.events :as docs-events]
    [sixsq.nuvla.ui.docs.subs :as docs-subs]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.utils.collapsible-card :as cc]
    [sixsq.nuvla.ui.utils.form-fields :as ff]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.style :as style]))


(defn metadata-section
  [{:keys [id name] :as _document}]
  [cc/metadata
   {:title    name
    :subtitle id
    :icon     "book"}])


(defn description-section
  [_document]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [{:keys [description]}]
      [cc/collapsible-segment (@tr [:description])
       [ui/ReactMarkdown {:source description}]])))


(defn row-attribute-fn
  [{:keys [name description type required editable help group
           display-name order hidden sensitive value-scope] :as _entry}]
  (let [characteristics [["display-name" display-name]
                         ["help" help]
                         ["order" order]
                         ["editable" editable]
                         ["required" required]
                         ["group" group]
                         ["hidden" hidden]
                         ["sensitive" sensitive]
                         ["value-scope" value-scope]]
        row-span        (inc (count characteristics))]
    (concat
      [[ui/TableRow
        [ui/TableCell {:collapsing true, :row-span row-span} name]
        [ui/TableCell {:row-span row-span} description]
        [ui/TableCell "type"]
        [ui/TableCell (str type)]]]
      (map (fn [[characteristic-name characteristic-value]]
             [ui/TableRow
              [ui/TableCell characteristic-name]
              [ui/TableCell (str characteristic-value)]]) characteristics))))


(defn attributes-table
  [{:keys [attributes] :as _document}]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Segment (merge style/basic
                       {:class-name "nuvla-ui-x-autoscroll"})

     [ui/Table
      {:compact     "very"
       :padded      false
       :unstackable true
       :selectable  true}
      [ui/TableHeader
       [ui/TableRow
        [ui/TableHeaderCell (@tr [:name])]
        [ui/TableHeaderCell (@tr [:description])]
        [ui/TableHeaderCell (@tr [:characteristics-name])]
        [ui/TableHeaderCell (@tr [:characteristics-value])]]]
      (vec (concat [ui/TableBody]
                   (mapcat row-attribute-fn (sort-by :name attributes))))]]))


(defn row-action-fn
  [{:keys [name description uri method inputMessage outputMessage] :as _entry}]
  [ui/TableRow
   [ui/TableCell {:collapsing true} name]
   [ui/TableCell {:style {:max-width     "150px"
                          :overflow      "hidden"
                          :text-overflow "ellipsis"}} description]
   [ui/TableCell {:collapsing true} uri]
   [ui/TableCell {:collapsing true} method]
   [ui/TableCell {:collapsing true} inputMessage]
   [ui/TableCell {:collapsing true} outputMessage]])


(defn actions-table
  [document]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Segment (merge style/basic
                       {:class-name "nuvla-ui-x-autoscroll"})

     [ui/Table
      {:compact     "very"
       :single-line true
       :padded      false
       :unstackable true
       :selectable  true}
      [ui/TableHeader
       [ui/TableRow
        [ui/TableHeaderCell (@tr [:name])]
        [ui/TableHeaderCell (@tr [:description])]
        [ui/TableHeaderCell "URI"]
        [ui/TableHeaderCell (@tr [:http-method])]
        [ui/TableHeaderCell (@tr [:input-mime-type])]
        [ui/TableHeaderCell (@tr [:output-mime-type])]]]
      (vec (concat [ui/TableBody]
                   (map row-action-fn (sort-by :name (get document :actions)))))]]))


(defn attributes-section
  [_document]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [document]
      [cc/collapsible-segment (@tr [:attributes])
       [attributes-table document]])))


(defn actions-section
  [_document]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [document]
      [cc/collapsible-segment (@tr [:actions])
       [actions-table document]])))


(defn preview-section
  [_document]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [document]
      [cc/collapsible-segment (@tr [:preview])
       (vec (concat [ui/Form]
                    (mapv (partial ff/form-field #() nil)
                          (->> document :attributes (sort-by :order)))))])))


(defn docs-detail
  [_resource-id]
  (let [documents (subscribe [::docs-subs/documents])]
    (fn [resource-id]
      (when (nil? @documents)
        (dispatch [::docs-events/get-documents]))
      (let [document (get @documents resource-id)]
        [ui/Container {:fluid true}
         [metadata-section document]
         [description-section document]
         [attributes-section document]
         [preview-section document]
         [actions-section document]]))))
