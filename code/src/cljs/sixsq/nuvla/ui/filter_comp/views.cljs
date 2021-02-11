(ns sixsq.nuvla.ui.filter-comp.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.cimi.events :as cimi-events]
    [sixsq.nuvla.ui.cimi.subs :as cimi-subs]
    [sixsq.nuvla.ui.filter-comp.events :as events]
    [sixsq.nuvla.ui.filter-comp.utils :as utils]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]))


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
              (assoc props :options (set (concat (:options props)
                                                 @additional-opts))
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
        {:keys [value-scope sensitive]} attribute-info
        enum-values (seq (:values value-scope))
        values      (r/atom (or enum-values []))]
    (when-not (or enum-values sensitive)
      (dispatch [::events/terms-attribute resource-name attribute values]))
    (fn [attribute-info resource-name data i]
      (let [{:keys [operation value] :as s} (nth @data i)]
        [DropdownInput
         (cond-> {:placeholder "value"
                  :value       value
                  :style       {:background-color "aliceblue"}
                  :on-change   (ui-callback/value
                                 #(reset! data
                                          (assoc-in @data [i :value] %)))}
                 @values (assoc :options
                                (cond-> (map (fn [v] {:key  v, :value v,
                                                      :text (general-utils/truncate (str v) 50)})
                                             (cond-> @values
                                                     (and value
                                                          (not (utils/value-is-null? value)))
                                                     (conj value)))
                                        (#{"=" "!="} operation) (conj {:key   utils/value-null
                                                                       :value utils/value-null
                                                                       :text  "<NULL>"})))
                 )]))))


(defn dispatch-value-attribute
  [{attr-type :type} resource-name data i]
  (cond
    (#{"string" "uri" "resource-id"} attr-type) :string
    (#{"number" "double" "integer" "long"} attr-type) :number
    (= "date-time" attr-type) :date-time
    (= "boolean" attr-type) :boolean
    :else :string))

(defmulti ValueAttribute dispatch-value-attribute)

(defmethod ValueAttribute :string
  [attribute-info resource-name data i]
  (let [{:keys [attribute operation]} (nth @data i)]
    [:<>
     [ui/Dropdown {:placeholder "operation"
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
     [DropdownStringValue attribute-info resource-name data i]]))

(defmethod ValueAttribute :number
  [{attr-type :type :as attribue-info} resource-name data i]
  (let [{:keys [value operation]} (nth @data i)
        value-is-null? (utils/value-is-null? value)]
    [:<>
     [ui/Dropdown {:placeholder "operation"
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
  [attribute-info resource-name data i]
  (let [{:keys [value operation]} (nth @data i)]
    [:<>
     [ui/Dropdown {:placeholder "operation"
                   :search      true
                   :value       operation
                   :on-change   (ui-callback/value
                                  #(reset! data
                                           (assoc-in @data [i :operation] %)))
                   :options     [{:key "=", :value "=" :text "is"}
                                 {:key "!=", :value "!=" :text "is not"}]
                   :style       {:font-style       "italic"
                                 :background-color "antiquewhite"}}]
     [ui/Dropdown {:placeholder "value"
                   :options     [{:key "true", :value true, :text "true"}
                                 {:key "false", :value true, :text "false"}
                                 {:key  utils/value-null, :value utils/value-null,
                                  :text utils/value-null}]
                   :value       value
                   :search      true
                   :on-change   (ui-callback/value
                                  #(reset! data
                                           (assoc-in @data [i :value] %)))
                   :style       {:background-color "aliceblue"}}]]))


(defmethod ValueAttribute :date-time
  [attribute-info resource-name data i]
  (let [{:keys [value operation]} (nth @data i)
        value-is-null?        (utils/value-is-null? value)
        value-now-expression? (boolean (re-find #"now" value))]
    [:<>
     [ui/Dropdown {:placeholder "operation"
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
                        :date-format      "LLL"
                        :on-change        #(reset! data (assoc-in @data [i :value]
                                                                  (time/time->utc-str %)))}
                       (and value
                            (not value-is-null?)
                            (not value-now-expression?))
                       (assoc :selected (time/parse-iso8601 value)))]]]))


(defn CellAttribute
  [resource-name data i]
  (let [attribute-options (subscribe [::cimi-subs/resource-metadata-attributes-options resource-name])
        attributes        (subscribe [::cimi-subs/resource-metadata-attributes resource-name])]
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
                                                              (dissoc :operation :value)
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
      (case el
        "empty" ^{:key i} [CellEmpty resource-name data i]
        "logic" ^{:key i} [CellLogic resource-name data i]
        "attribute" ^{:key i} [CellAttribute resource-name data i]))]])


(defn ButtonFilter
  [{:keys [resource-name open? default-filter on-done]}]
  (let [show-error? (r/atom false)
        init-data   (or (when-not (str/blank? default-filter)
                          (utils/filter-str->data default-filter))
                        [{:el "empty"} {:el "attribute"} {:el "empty"}])
        data        (r/atom init-data)
        close-fn    #(do
                       (reset! open? false)
                       (reset! data init-data))
        open-fn     #(reset! open? true)]
    (when resource-name (dispatch [::cimi-events/get-resource-metadata resource-name]))
    (fn [{:keys [resource-name open? default-filter on-done]}]
      (let [filter-string (utils/data->filter-str @data)
            error         (when (and @show-error? (not (str/blank? filter-string)))
                            (utils/filter-syntax-error filter-string))]
        [ui/Modal
         {:trigger    (r/as-element [ui/Button {:icon     "magic"
                                                :disabled (nil? resource-name)
                                                :on-click open-fn}])
          :open       @open?
          :on-close   close-fn
          :close-icon true}

         [ui/ModalHeader "Filter composer"]

         (when resource-name
           [:<>
            [ui/ModalContent
             [FitlerFancy resource-name data]
             [ui/Message {:error (some? error)}
              [ui/MessageHeader {:style {:margin-bottom 10}}
               (str/capitalize "Result:")
               [ui/Button {:floated  "right"
                           :icon     true
                           :toggle   true
                           :active   @show-error?
                           :on-click #(swap! show-error? not)}
                [ui/Icon {:className "fad fa-spell-check"}]]]
              [ui/MessageContent {:style {:font-family "monospace" :white-space "pre"}}
               (or error filter-string)]]]
            [ui/ModalActions
             [ui/Button
              {:positive true
               :disabled (some? error)
               :on-click #(do
                            (on-done filter-string)
                            (close-fn))}
              "Done"]]])])
      )))
