(ns sixsq.slipstream.webui.utils.forms
  (:require
    [re-frame.core :refer [subscribe]]
    [reagent.core :as reagent]
    [sixsq.slipstream.webui.utils.form-fields :as ff]
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]
    [sixsq.slipstream.webui.utils.general :as general]))


(defn on-return-key
  "Will execute the given no-arg function when the value of k is the
   value for the return key (13). Before executing the function it will
   blur the active element in the document, ignoring any errors."
  [f k]
  (when (and f (= (.-charCode k) 13))
    (try
      (some-> js/document .-activeElement .blur)
      (catch :default _ nil))
    (f)))


(defn descriptions->options [descriptions]
  (mapv (fn [{:keys [id label]}] {:value id, :text (or label id)}) descriptions))


;;
;; public component
;;


(defn resource-editor
  [form-id text & {:keys [resource-meta default-mode] :or {default-mode :form}}]
  (let [mode (reagent/atom (if resource-meta default-mode :json))
        json-error? (reagent/atom false)
        check-json-fn (fn [success-action-fn]
                        (try
                          @text
                          (reset! json-error? false)
                          (success-action-fn)
                          (catch js/Object e
                            (reset! json-error? e)
                            (reset! mode :json))))]
    (fn [form-id text & {:keys [resource-meta default-mode] :or {default-mode :form}}]
      ^{:key form-id}
      [:div
       [ui/Menu {:icon true, :attached "top"}
        (vec (concat
               [ui/MenuMenu {:position "right"}]
               (map (fn [[mode-kw item-icon item-disabled]]
                      [ui/MenuItem
                       {:active   (= mode-kw @mode),
                        :disabled item-disabled
                        :on-click (partial check-json-fn #(reset! mode mode-kw))}
                       [ui/Icon {:name item-icon}]])
                    [[:form "list" (not resource-meta)], [:json "code" false]])
               [(when @json-error?
                  [ui/MenuItem [ui/Label
                                "Invalid JSON!!!"
                                [ui/LabelDetail (str (.-name @json-error?) " "
                                                     (.-message @json-error?))]]])]))]
       [ui/Segment {:attached "bottom"}
        (case @mode
          :form (vec (concat
                       [ui/Form]
                       (mapv
                         (fn [attribute-meta]
                           (let [param-kw (-> attribute-meta :name keyword)
                                 new-value (-> @text general/json->edn param-kw)]
                             (ff/form-field
                               (fn [form-id param-name param-value]
                                 (reset! text (-> @text
                                                        general/json->edn
                                                        (assoc (keyword param-name) param-value)
                                                        general/edn->json)))
                               nil
                               (cond-> attribute-meta
                                       new-value (assoc-in [:vscope :value] new-value)))))
                         (some->> resource-meta
                                  :attributes
                                  (filter :consumerWritable)
                                  (sort-by :order)))))
          :json [ui/CodeMirror {:value     @text
                                :on-change (fn [editor data value]
                                             (reset! text value))
                                :options   {:mode                "application/json"
                                            :line-numbers        true
                                            :match-brackets      true
                                            :auto-close-brackets true
                                            :style-active-line   true
                                            :fold-gutter         true
                                            :gutters             ["CodeMirror-foldgutter"]}}])]])))