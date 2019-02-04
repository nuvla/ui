(ns sixsq.slipstream.webui.docs-detail.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.slipstream.webui.docs.subs :as docs-subs]
    [sixsq.slipstream.webui.i18n.subs :as i18n-subs]
    [sixsq.slipstream.webui.utils.collapsible-card :as cc]
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]
    [sixsq.slipstream.webui.utils.style :as style]
    [sixsq.slipstream.webui.utils.form-fields :as ff]))


(defn metadata-section
  [{:keys [id name] :as document}]
  [cc/metadata
   {:title    name
    :subtitle id
    :icon     "book"}])


(defn description-section
  [document]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [{:keys [description]}]
      [cc/collapsible-segment (@tr [:description])
       [ui/ReactMarkdown {:source description}]])))


(defn row-attribute-fn [{:keys [name description type namespace uri
                                providerMandatory consumerMandatory mutable templateMutable consumerWritable
                                displayName help group order hidden sensitive vscope] :as entry
                         :or {templateMutable false}}]
  (let [characteristics [["displayName" displayName]
                         ["help" help]
                         ["order" order]
                         ["mutable" mutable]
                         ["consumerWritable" consumerWritable]
                         ["providerMandatory" providerMandatory]
                         ["consumerMandatory" consumerMandatory]
                         ["templateMutable" templateMutable]
                         ["group" group]
                         ["hidden" hidden]
                         ["sensitive" sensitive]
                         ["namespace" namespace]
                         ["uri" uri]
                         ["vscope" vscope]]
        row-span (inc (count characteristics))]
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
  [{:keys [attributes] :as document}]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Segment (merge style/basic
                       {:class-name "webui-x-autoscroll"})

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
        [ui/TableHeaderCell (@tr [:characteristics-value])]
        ]]
      (vec (concat [ui/TableBody]
                   (mapcat row-attribute-fn (sort-by :name attributes))))]]))


(defn row-action-fn [{:keys [name description uri method inputMessage outputMessage] :as entry}]
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
                       {:class-name "webui-x-autoscroll"})

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
  [document]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [document]
      [cc/collapsible-segment (@tr [:attributes])
       [attributes-table document]])))


(defn actions-section
  [document]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [document]
      [cc/collapsible-segment (@tr [:actions])
       [actions-table document]])))


(defn preview-section
  [document]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [document]
      [cc/collapsible-segment (@tr [:preview])
       (vec (concat [ui/Form]
                    (mapv (partial ff/form-field #() nil)
                          (->> document :attributes (sort-by :order)))))])))


(defn docs-detail
  [resource-id]
  (let [documents (subscribe [::docs-subs/documents])]
    (fn [resource-id]
      (let [document (get @documents resource-id)]
        [ui/Container {:fluid true}
         [metadata-section document]
         [description-section document]
         [attributes-section document]
         [preview-section document]
         [actions-section document]]))))
