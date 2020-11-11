(ns sixsq.nuvla.ui.filter-comp.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.filter-comp.events :as events]
    [sixsq.nuvla.ui.filter-comp.subs :as subs]
    [sixsq.nuvla.ui.filter-comp.utils :as utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.time :as time]))


(defn DeleteIcon
  [{:keys [on-click]}]
  [ui/Icon {:name     "delete"
            :link     true
            :style    {:margin-right ".5em"}
            :on-click on-click}])


(defn DropdownInput
  [props]
  (let [additional-opts (r/atom nil)]
    (fn [props]
      [ui/Dropdown
       (merge {:allow-additions    true
               :addition-label     ""
               :no-results-message ""}
              (assoc props :options (concat (:options props)
                                            @additional-opts)
                           :on-add-item (ui-callback/value #(swap! additional-opts conj
                                                                   {:key %, :value %, :text %}))
                           :search true
                           :selection false))])))


(defn CellEmpty
  [resource-name data i]
  [ui/Dropdown
   {:trigger              (r/as-element [:span])
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
  [resource-name data i]
  (let [{:keys [value] :as s} (nth @data i)]
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
  [attribute-info resource-name data i]
  (let [{:keys [attribute] :as s} (nth @data i)
        {value-scope :value-scope} attribute-info
        enum-values (seq (:values value-scope))
        values      (r/atom (or enum-values []))]
    (when-not enum-values
      (dispatch [::events/terms-attribute resource-name attribute values]))
    (fn [attribute-info resource-name data i]
      (let [{:keys [value] :as s} (nth @data i)]
        [DropdownInput
         (cond-> {:placeholder "value"
                  :value       value
                  :style       {:background-color "aliceblue"}
                  :on-change   (ui-callback/value
                                 #(reset! data
                                          (assoc-in @data [i :value] %)))}
                 @values (assoc :options (map (fn [v] {:key v, :value v, :text v}) @values))
                 )]))))

(defn dispatch-value-attribute
  [{attr-type :type} resource-name data i]
  (cond
    (#{"string" "uri" "resource-id"} attr-type) :string
    (#{"integer" "long"} attr-type) :integer
    (= "date-time" attr-type) :date-time
    (= "boolean" attr-type) :boolean
    :else :string))

(defmulti ValueAttribute dispatch-value-attribute)

(defmethod ValueAttribute :string
  [attribute-info resource-name data i]
  (let [attr-name (-> @data (nth i) :attribute)]
    [:<>
     [ui/Dropdown {:placeholder "operation"
                   :on-change   (ui-callback/value
                                  #(reset! data (assoc-in @data [i :op] %)))
                   :search      true
                   :options     [{:key "equal", :value "=", :text "Equal"}
                                 {:key "start-with", :value "^=", :text "Start with"}
                                 {:key "not-equal", :value "!=", :text "Not equal"}
                                 {:key "like", :value "==", :text "Like"}]
                   :style       {:font-style       "italic"
                                 :background-color "antiquewhite"}}]
     ^{:key (str i "_" attr-name)}
     [DropdownStringValue attribute-info resource-name data i]]))

(defmethod ValueAttribute :integer
  [attribute-info resource-name data i]
  [:<>
   [ui/Dropdown {:placeholder "operation"
                 :search      true
                 :on-change   (ui-callback/value
                                #(reset! data
                                         (assoc-in @data [i :op] %)))
                 :options     [{:key "=", :value "=" :text "="}
                               {:key "<", :value "<" :text "<"}
                               {:key ">", :value ">" :text ">"}
                               {:key "<=", :value "<=" :text "<="}
                               {:key ">=", :value ">=" :text ">="}]
                 :style       {:font-style       "italic"
                               :background-color "antiquewhite"}}]
   [ui/Input
    {:type        "number"
     :size        "mini"
     :style       {:background-color "aliceblue"
                   :width            50}
     :transparent true
     :placeholder "value"
     :value       (or (-> @data (nth i) :value) "")
     :on-change   (ui-callback/value
                    #(reset! data
                             (assoc-in @data [i :value]
                                       (if (str/blank? %)
                                         nil
                                         (js/parseInt %)))))}]])


(defmethod ValueAttribute :boolean
  [attribute-info resource-name data i]
  [:<>
   [ui/Dropdown {:placeholder "operation"
                 :search      true
                 :on-change   (ui-callback/value
                                #(reset! data
                                         (assoc-in @data [i :op] %)))
                 :options     [{:key "=", :value "=" :text "is"}
                               {:key "!=", :value "!=" :text "is not"}]
                 :style       {:font-style       "italic"
                               :background-color "antiquewhite"}}]
   [ui/Dropdown {:placeholder "value"
                 :options     [{:key "true", :value true, :text "true"}
                               {:key "false", :value true, :text "false"}]
                 :value       (-> @data (nth i) :value)
                 :search      true
                 :on-change   (ui-callback/value
                                #(reset! data
                                         (assoc-in @data [i :value] %)))
                 :style       {:background-color "aliceblue"}}]])


(defmethod ValueAttribute :date-time
  [attribute-info resource-name data i]
  (let [value (-> @data (nth i) :value)]
    [:<>
     [ui/Dropdown {:placeholder "operation"
                   :search      true
                   :on-change   (ui-callback/value
                                  #(reset! data
                                           (assoc-in @data [i :op] %)))
                   :options     [{:key "=", :value "=" :text "="}
                                 {:key "<", :value "<" :text "<"}
                                 {:key ">", :value ">" :text ">"}
                                 {:key "<=", :value "<=" :text "<="}
                                 {:key ">=", :value ">=" :text ">="}]
                   :style       {:font-style       "italic"
                                 :background-color "antiquewhite"}}]
     [ui/DatePicker (cond->
                      {:custom-input     (r/as-element
                                           [ui/Input {:style       {:background-color "aliceblue"
                                                                    :width            160}
                                                      :transparent true}])
                       :show-time-select true
                       :date-format      "LLL"
                       :on-change        #(reset! data
                                                  (assoc-in @data [i :value] %))
                       }
                      value (assoc :selected value))]]))


(defmethod ValueAttribute :default
  [attribute-info resource-name data i]
  [:<>
   [ui/Dropdown {:placeholder "operation" :style {:background-color "antiquewhite"}}]
   [ui/Dropdown {:placeholder "value" :style {:background-color "aliceblue"}}]
   ])

(defn CellAttribute
  [resource-name data i]
  (let [attribute-options (subscribe [::subs/resource-metadata-attributes-options resource-name])
        attributes        (subscribe [::subs/resource-metadata-attributes resource-name])]
    (fn [resource-name data i]
      (let [{:keys [attribute] :as s} (nth @data i)
            attribute-info (get @attributes attribute)]
        [ui/Label {:size "large"}
         [ui/Dropdown
          (cond-> {:search      true
                   :placeholder "attribute name"
                   :on-change   (ui-callback/value
                                  #(reset! data
                                           (assoc @data i (-> (nth @data i)
                                                              (dissoc :op :value)
                                                              (assoc :attribute %)))))
                   :options     @attribute-options
                   :style       {:background-color "beige"}}
                  attribute (assoc :value attribute))]
         [ValueAttribute attribute-info resource-name data i]
         " "
         [DeleteIcon {:on-click #(reset! data
                                         (into []
                                               (concat
                                                 (subvec @data 0 i)
                                                 (subvec @data (+ i 2)))
                                               ))}]]))))


(defn FitlerFancy
  [resource-name data]
  [:<>
   [:div {:style {:background-color "white"
                  :border-color     "#85b7d9"
                  :border-style     "solid"
                  :border-width     1
                  :border-radius    ".28571429rem"
                  :padding          10
                  :display          "flex"
                  :flex-wrap        "wrap"
                  :align-items      "center"}}
    (for [[i {:keys [el] :as s}] (map-indexed vector @data)]
      (cond
        (= el "empty") ^{:key i} [CellEmpty resource-name data i]
        (= el "logic") ^{:key i} [CellLogic resource-name data i]
        (= el "attribute") ^{:key i} [CellAttribute resource-name data i]))]])


(defn data->filter
  [data attributes]
  (->>
    data
    (remove #(= (:el %) "empty"))
    (map #(if (= (:el %) "logic")
            (:value %)
            (str (:attribute %) " " (:op %) " "
                 (cond
                   (#{"string" "uri" "resource-id"}
                    (get-in attributes [(:attribute %) :type])) (str "'" (:value %) "'")
                   (= (get-in attributes [(:attribute %) :type])
                      "date-time") (when (:value %) (str "'" (time/time->utc-str (:value %)) "'"))
                   :else (:value %))
                 )))
    (str/join " ")))

(defn ButtonFilter
  [resource-name set-filter-fn]
  (let [open?       (r/atom false)
        show-error? (r/atom false)
        init-data [{:el "empty"}
                   {:el "attribute"}
                   {:el "empty"}]
        data        (r/atom init-data)
        close-fn    #(do
                       (reset! open? false)
                       (reset! data init-data))
        open-fn     #(reset! open? true)
        attributes  (subscribe [::subs/resource-metadata-attributes resource-name])]
    (dispatch [::events/get-resource-metadata resource-name])
    (fn [resource-name set-filter-fn]
      (let [filter-string (data->filter @data @attributes)
            error         (when (and @show-error? (not (str/blank? filter-string)))
                            (utils/filter-syntax-error filter-string))]
        [ui/Modal
         {:trigger    (r/as-element [ui/Button {:icon     "magic"
                                                :on-click open-fn}])
          :open       @open?
          :on-close   close-fn
          :close-icon true}

         [ui/ModalHeader "Filter composer"]

         [ui/ModalContent
          [FitlerFancy resource-name data]
          [ui/Message {:error (some? error)}
           [ui/MessageHeader (str/capitalize "Result:")
            [ui/Button {:floated  "right"
                        :icon     true
                        :toggle   true
                        :active   @show-error?
                        :on-click #(swap! show-error? not)}
             [ui/Icon {:className "fad fa-spell-check"}]]
            ]
           [:p {:style {:font-family "monospace" :white-space "pre"}}
            (or error filter-string)]]]
         [ui/ModalActions
          [ui/Button
           {:positive true
            :disabled (some? error)
            :on-click #(do
                         (set-filter-fn filter-string)
                         (close-fn))}
           "Done"]]])
      )))
