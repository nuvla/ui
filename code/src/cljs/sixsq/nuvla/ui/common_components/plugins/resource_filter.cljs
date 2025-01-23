(ns sixsq.nuvla.ui.common-components.plugins.resource-filter
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.filter-comp.utils :as utils]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.spec :refer [nonblank-string]]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))

(defn DeleteIcon
  [{:keys [on-click]}]
  [ui/Icon {:name     icons/i-delete
            :link     true
            :style    {:margin-right ".5em"}
            :on-click on-click}])


(defn DropdownInput
  [_props]
  (let [additional-opts (r/atom nil)]
    (fn [props]
      [ui/Dropdown
       (merge {:allow-additions    true
               :addition-label     ""
               :no-results-message ""}
              (assoc props :options (set (concat (:options props)
                                                 @additional-opts))
                           :on-add-item (ui-callback/value #(swap! additional-opts conj
                                                                   {:key %, :value %, :text %}))
                           :search true
                           :selection false))])))


(defn CellEmpty
  [_resource-name data i]
  [ui/Dropdown
   {:class                "insert-element-dropdown"
    :trigger              (r/as-element [:span])
    :value                nil
    :select-on-navigation false
    :select-on-blur       false
    :header               "Insert an element"
    :upward               false
    :on-change            (ui-callback/value
                            #(reset! data
                                     (into []
                                           (concat (subvec @data 0 (inc i))
                                                   [(if (= "attribute" %)
                                                      {:el "attribute"}
                                                      {:el    "logic"
                                                       :value %})
                                                    {:el "empty"}]
                                                   (subvec @data (inc i))))))
    :options              [{:key "Attribute", :value "attribute", :text "Attribute"}
                           {:key "AND", :value "and", :text "AND"}
                           {:key "OR", :value "or", :text "OR"}
                           {:key "(", :value "(", :text "("}
                           {:key ")", :value ")", :text ")"}]
    :icon                 "chevron circle down"}])


(defn CellLogic
  [_resource-name data i]
  (let [{:keys [value]} (nth @data i)]
    [ui/Label
     {:style {:cursor "pointer"}
      :color "blue"}
     (str/upper-case value)
     [DeleteIcon {:on-click #(reset! data
                                     (into []
                                           (concat
                                             (subvec @data 0 i)
                                             (subvec @data (+ i 2)))
                                           ))}]]))


(defn DropdownStringValue
  [attribute-info resource-name data i {::keys [terms-attribute-fn]}]
  (let [{:keys [attribute]} (nth @data i)
        {:keys [value-scope sensitive]} attribute-info
        enum-values (seq (:values value-scope))
        values      (r/atom (or enum-values []))]
    (when-not (or enum-values sensitive)
      (terms-attribute-fn resource-name attribute values))
    (fn [_attribute-info _resource-name data i]
      (let [{:keys [operation value]} (nth @data i)]
        [DropdownInput
         (cond-> {:class       :value-dropdown
                  :placeholder "value"
                  :value       value
                  :style       {:background-color "aliceblue"}
                  :on-change   (ui-callback/value
                                 #(reset! data
                                          (assoc-in @data [i :value] %)))}
                 @values (assoc :options
                                (cond-> (map (fn [v] {:key  v, :value v,
                                                      :text (general-utils/truncate (str v) 50)})
                                             (cond-> (sort @values)
                                                     (and value
                                                          (not (utils/value-is-null? value)))
                                                     (conj value)))
                                        (#{"=" "!="} operation) (conj {:key   utils/value-null
                                                                       :value utils/value-null
                                                                       :text  "<NULL>"}))))]))))


(defn dispatch-value-attribute
  [{attr-type :type} _resource-name _data _i _opts]
  (cond
    (#{"string" "uri" "resource-id"} attr-type) :string
    (#{"number" "double" "integer" "long"} attr-type) :number
    (= "date-time" attr-type) :date-time
    (= "boolean" attr-type) :boolean
    :else :string))

(defmulti ValueAttribute dispatch-value-attribute)

(defmethod ValueAttribute :string
  [attribute-info resource-name data i opts]
  (let [{:keys [attribute operation]} (nth @data i)]
    [:<>
     [ui/Dropdown {:class       :operation-dropdown
                   :placeholder "operation"
                   :on-change   (ui-callback/value
                                  #(reset! data (assoc-in @data [i :operation] %)))
                   :search      true
                   :value       operation
                   :options     [{:key "equal", :value "=", :text "Equal"}
                                 {:key "start-with", :value "^=", :text "Start with"}
                                 {:key "not-equal", :value "!=", :text "Not equal"}
                                 {:key "like", :value "==", :text "Like"}]
                   :style       {:font-style       "italic"
                                 :background-color "antiquewhite"}}]
     ^{:key (str i "_" attribute)}
     [DropdownStringValue attribute-info resource-name data i opts]]))

(defmethod ValueAttribute :number
  [{attr-type :type :as _attribue-info} _resource-name data i _opts]
  (let [{:keys [value operation]} (nth @data i)
        value-is-null? (utils/value-is-null? value)]
    [:<>
     [ui/Dropdown {:class       :operation-dropdown
                   :placeholder "operation"
                   :search      true
                   :value       operation
                   :on-change   (ui-callback/value
                                  #(reset! data
                                           (assoc-in @data [i :operation] %)))
                   :options     [{:key "=", :value "=" :text "="}
                                 {:key "<", :value "<" :text "<"}
                                 {:key ">", :value ">" :text ">"}
                                 {:key "<=", :value "<=" :text "<="}
                                 {:key ">=", :value ">=" :text ">="}]
                   :style       {:font-style       "italic"
                                 :background-color "antiquewhite"}}]
     [:span {:style {:background-color "aliceblue"
                     :width            50}}
      (when value-is-null? utils/value-null)
      [ui/Input
       {:type        "number"
        :size        "mini"
        :transparent true
        :placeholder "value"
        :value       (if (and (some? value) (not value-is-null?)) value "")
        :on-change   (ui-callback/value
                       #(reset! data
                                (assoc-in @data [i :value]
                                          (if (str/blank? %)
                                            nil
                                            ((if (#{"integer" "long"} attr-type)
                                               js/parseInt
                                               js/parseFloat) %)))))}]]]))

(defmethod ValueAttribute :boolean
  [_attribute-info _resource-name data i _opts]
  (let [{:keys [value operation]} (nth @data i)]
    [:<>
     [ui/Dropdown {:class       :operation-dropdown
                   :placeholder "operation"
                   :search      true
                   :value       operation
                   :on-change   (ui-callback/value
                                  #(reset! data
                                           (assoc-in @data [i :operation] %)))
                   :options     [{:key "=", :value "=" :text "is"}
                                 {:key "!=", :value "!=" :text "is not"}]
                   :style       {:font-style       "italic"
                                 :background-color "antiquewhite"}}]
     [ui/Dropdown {:class       :value-dropdown
                   :placeholder "value"
                   :options     [{:key "true", :value true, :text "true"}
                                 {:key "false", :value false, :text "false"}
                                 {:key  utils/value-null, :value utils/value-null,
                                  :text utils/value-null}]
                   :value       value
                   :search      true
                   :on-change   (ui-callback/value
                                  #(reset! data
                                           (assoc-in @data [i :value] %)))
                   :style       {:background-color "aliceblue"}}]]))

(defmethod ValueAttribute :date-time
  [_attribute-info _resource-name data i _opts]
  (let [{:keys [value operation]} (nth @data i)
        value-is-null?        (utils/value-is-null? value)
        value-now-expression? (boolean (when (string? value) (re-find #"now" value)))]
    [:<>
     [ui/Dropdown {:class       :operation-dropdown
                   :placeholder "operation"
                   :search      true
                   :value       operation
                   :on-change   (ui-callback/value
                                  #(reset! data
                                           (assoc-in @data [i :operation] %)))
                   :options     [{:key "=", :value "=" :text "="}
                                 {:key "<", :value "<" :text "<"}
                                 {:key ">", :value ">" :text ">"}
                                 {:key "<=", :value "<=" :text "<="}
                                 {:key ">=", :value ">=" :text ">="}]
                   :style       {:font-style       "italic"
                                 :background-color "antiquewhite"}}]
     [:span {:style {:background-color "aliceblue"
                     :width            160}}
      (when value-is-null? utils/value-null)
      (when value-now-expression? value)
      [ui/DatePicker (cond->
                       {:custom-input     (r/as-element
                                            [ui/Input {:style       {:background-color "aliceblue"
                                                                     :width            160}
                                                       :transparent true}])
                        :show-time-select true
                        :date-format      "MMMM d, yyyy HH:mm"
                        :time-format      "HH:mm"
                        :on-change        #(reset! data (assoc-in @data [i :value]
                                                                  (time/time->utc-str %)))}
                       (and value
                            (not value-is-null?)
                            (not value-now-expression?))
                       (assoc :selected (js/Date. (time/parse-iso8601 value))))]]]))



(defn CellAttribute
  [resource-name data i {::keys [!resource-metadata-attributes] :as opts}]
  (let [{:keys [attribute]} (nth @data i)
        attribute-info                       (get @!resource-metadata-attributes attribute)
        resource-metadata-attributes-options (map (fn [a] {:key a, :text a :value a})
                                                  (keys @!resource-metadata-attributes))]
    [ui/Label {:size "large"}
     [ui/Dropdown
      (cond-> {:class       :attr-name-dropdown
               :search      true
               :placeholder "attribute name"
               :on-change   (ui-callback/value
                              #(reset! data
                                       (assoc @data i (-> (nth @data i)
                                                          (dissoc :operation :value)
                                                          (assoc :attribute %)))))
               :options     (remove (fn [{k :key}]
                                      (and
                                        (nonblank-string k)
                                        (str/starts-with? k "acl/")))
                                    resource-metadata-attributes-options)
               :style       {:background-color "beige"}}
              attribute (assoc :value attribute))]
     [ValueAttribute attribute-info resource-name data i opts]
     " "
     [DeleteIcon {:on-click #(reset! data
                                     (into []
                                           (concat
                                             (subvec @data 0 i)
                                             (subvec @data (+ i 2)))
                                           ))}]]))


(defn FilterFancy
  [resource-name data opts]
  [:<>
   [:div {:class :fancy-filter
          :style {:background-color "white"
                  :border-color     "#85b7d9"
                  :border-style     "solid"
                  :border-width     1
                  :border-radius    ".28571429rem"
                  :padding          10
                  :display          "flex"
                  :flex-wrap        "wrap"
                  :align-items      "center"}}
    (for [[i {:keys [el]}] (map-indexed vector @data)]
      (case el
        "empty" ^{:key i} [CellEmpty resource-name data i]
        "logic" ^{:key i} [CellLogic resource-name data i]
        "attribute" ^{:key i} [CellAttribute resource-name data i opts]))]])

(defn- ClearButton
  [{::keys [active-filter? on-done close-fn view tr-fn]}]
  (let [clear-fn #(do
                    (on-done "")
                    (when close-fn (close-fn)))]
    (if view
      [:button
       {:style    {:border      :none
                   :z-index     1000
                   :margin-left 5
                   :cursor      :pointer}
        :on-click clear-fn}
       view]
      [ui/Button
       {:positive true
        :style    {:align-items :center
                   :z-index     1000}
        :disabled (not active-filter?)
        :on-click clear-fn}
       (tr-fn [:clear-filter])])))

(defn ResourceFilter
  [{::keys [!filter-query default-filter show-clear-button? tr-fn]
    :as    opts}]
  (let [show-error? (r/atom false)
        init-data   (or (when-not (str/blank? default-filter)
                          (utils/filter-str->data default-filter))
                        (some-> @!filter-query utils/filter-str->data)
                        [{:el "empty"} {:el "attribute"} {:el "empty"}])
        data        (r/atom init-data)]
    (fn [{::keys [resource-name _default-filter on-change]}]
      (let [filter-string  (utils/data->filter-str @data)
            error          (utils/filter-syntax-error filter-string)
            filter-data    (some-> filter-string (utils/filter-str->data))
            active-filter? (boolean filter-data)]
        (when (and (or active-filter? (nil? @data)) (not= filter-string @!filter-query))
          (on-change filter-string)
          (reset! data (utils/filter-str->data @!filter-query)))
        [:div {:class :resource-filter
               :style {:display     :flex
                       :align-items :center}}
         (when resource-name
           [:div
            [FilterFancy resource-name data opts]
            [ui/Message {:error (and @show-error? (some? error))}
             [ui/MessageHeader {:style {:margin-bottom 10}}
              (str/capitalize "Result:")
              [ui/Button {:floated  "right"
                          :icon     true
                          :toggle   true
                          :active   @show-error?
                          :on-click #(swap! show-error? not)}
               [ui/Icon {:className icons/i-spell-check}]]]
             [ui/MessageContent {:style {:font-family "monospace" :white-space "pre"}}
              (or (and @show-error? error) filter-string)]]])
         (when (and show-clear-button? active-filter?)
           ^{:key 1}
           [ClearButton
            (merge {::on-done        #(do (reset! data init-data)
                                          (on-change %))
                    ::active-filter? active-filter? ::resource-name resource-name ::tr-fn tr-fn}
                   {::view [icons/XMarkIcon {:style {:margin-right 0}}]})])]))))

(defn ModalResourceFilter
  [{::keys [!filter-query !open? default-filter show-clear-button? on-change trigger-style tr-fn]
    :as    opts}]
  (let [!modal-filter-query (r/atom @!filter-query)
        close-fn            #(do (on-change @!modal-filter-query)
                                 (reset! !open? false))
        open-fn             #(reset! !open? true)
        active?             (r/atom true)]
    (add-watch !filter-query :watcher
               (fn [_key _ref old-value new-value]
                 (when-not (= old-value new-value)
                   (reset! !modal-filter-query new-value))))
    (fn [{::keys [resource-name !open? _default-filter on-change]}]
      [:div
       {:style {:display     :flex
                :align-items :center}}
       [ui/Modal
        {:class      :resource-filter-modal
         :trigger    (r/as-element
                       [ui/Button {:type     :button
                                   :class    :modal-trigger
                                   :icon     true
                                   :disabled (nil? resource-name)
                                   :on-click open-fn
                                   :color    :teal
                                   :style    (merge {:align-items :center
                                                     :z-index     100
                                                     :display     :flex}
                                                    trigger-style)}
                        (if @active?
                          [icons/FilterIconFull]
                          [icons/FilterIcon])
                        \u00A0
                        (str/capitalize (tr-fn [:filter]))])
         :open       @!open?
         :on-close   close-fn
         :close-icon true}

        [uix/ModalHeader {:header (tr-fn [:filter-composer])}]

        (when resource-name
          [:<>
           [ui/ModalContent
            ^{:key (str @!filter-query)}
            [ResourceFilter (merge opts
                                   {::!filter-query !modal-filter-query
                                    ::on-change     #(reset! !modal-filter-query %)})]]
           [ui/ModalActions
            [ClearButton
             (merge {::on-done        on-change ::close-fn close-fn
                     ::active-filter? @active? ::resource-name resource-name ::tr-fn tr-fn})]
            [ui/Button
             {:class    :done-button
              :positive true
              :disabled (not @active?)
              :on-click #(close-fn)}
             (tr-fn [:done])]]])]
       (when (and show-clear-button? @active?)
         ^{:key 1}
         [:div {:style {:display :flex
                        :width   "75%"}}
          [:div {:style
                 {:font-size     "0.8rem"
                  :font-style    :italic
                  :overflow      :hidden
                  :text-overflow :ellipsis
                  :white-space   :nowrap}}
           default-filter]
          [ClearButton
           (merge {::on-done        on-change ::close-fn close-fn
                   ::active-filter? @active? ::resource-name resource-name ::tr-fn tr-fn}
                  {::view [icons/XMarkIcon {:style {:margin-right 0}}]})]])])))

(defn ResourceFilterController
  [{:keys [;; filter query
           !filter-query
           ;; resource name
           resource-name
           ;; resource metadata
           !resource-metadata-attributes
           ;; terms attributes
           terms-attribute-fn
           default-filter
           on-change

           ;; Optional
           modal?
           !open?
           show-clear-button?
           trigger-style

           ;; Translations
           tr-fn
           ]}]
  (r/with-let [!filter-query      (or !filter-query (r/atom nil))
               terms-attribute-fn (or terms-attribute-fn (constantly []))
               on-change          (or on-change (fn [filter-str] (reset! !filter-query filter-str)))
               !open?             (or !open? (r/atom false))
               tr-fn              (or tr-fn (comp str/capitalize name first))]
    [(if modal? ModalResourceFilter ResourceFilter)
     {::!filter-query                 !filter-query
      ::resource-name                 resource-name
      ::!resource-metadata-attributes !resource-metadata-attributes
      ::terms-attribute-fn            terms-attribute-fn
      ::default-filter                default-filter
      ::on-change                     on-change
      ::close-fn                      (fn [])
      ::!open?                        !open?
      ::show-clear-button?            show-clear-button?
      ::trigger-style                 trigger-style
      ::tr-fn                         tr-fn}]))
