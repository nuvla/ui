(ns sixsq.nuvla.ui.filter-comp.utils
  (:require [clojure.string :as str]
            [instaparse.core :as insta :refer-macros [defparser]]
            [instaparse.transform :as insta-transform]))


(def ^:const value-null "<NULL>")


(defn value-is-null?
  [value]
  (= value value-null))


(defn cimi-metadata-simplifier
  [resource-metadta]
  (->> (loop [attrs  (:attributes resource-metadta)
              result []]
         (if (seq attrs)
           (let [{:keys [leafs nested]} (group-by #(if (empty? (:child-types %))
                                                     :leafs :nested) attrs)
                 new-attrs (mapcat (fn [{:keys [type child-types] :as in}]
                                     (let [is-array? (= type "array")]
                                       (if (and (= type "array")
                                                (= (-> child-types first :type) "array"))
                                         [(-> in
                                              (dissoc :child-types)
                                              (assoc :type (-> child-types first :type)))]
                                         (map #(assoc %
                                                 :name
                                                 (cond-> (:name in)
                                                         (not is-array?) (str "/" (:name %))))
                                              child-types)))) nested)]
             (recur new-attrs (concat result leafs)))
           result))
       (filter :indexed)
       (map (juxt :name identity))
       (into (sorted-map))))


(defparser cimi-parser
           "
Filter              = Or
Or                  = And {<'or'> And}
And                 = CompOr {<'and'> CompOr}
<CompOr>            = Comp | WS <'('> Or <')'> WS

Comp                = Attribute EqOp Value
                       | Attribute RelOp OrdinalValue
                       | Attribute PrefixOp StringValue
                       | Attribute FullTextOp StringValue
                       | Attribute  GeoOp WktValue
                       | Value EqOp Attribute
                       | OrdinalValue RelOp Attribute
                       | StringValue PrefixOp Attribute
                       | StringValue FullTextOp Attribute

FullTextOp          = '=='
PrefixOp            = '^='
EqOp                = '=' | '!='
RelOp               = '<' | '<=' | '>=' | '>'
GeoOp               = 'intersects' | 'disjoint' | 'within' | 'contains'


Attribute           = WS NamespaceTerm ('/' NamespaceTerm)* WS

<NamespaceTerm>     = (Term ':' Term) | Term
<Term>              = #'([a-zA-Z][\\w-]*[\\w]+)|[a-zA-Z]'
<OrdinalValue>      = IntValue | DateValue | StringValue
<NominalValue>      = BoolValue | NullValue
<Value>             = OrdinalValue | NominalValue
IntValue            = WS #'\\d+' WS
DateValue           = WS #'\\d+-\\d+(-\\d+)?(T\\d+:\\d+:\\d+(\\.\\d+)?(Z|[+-]\\d+:\\d+))?' WS
WktValue            = StringValue
StringValue         = WS (DoubleQuoteString | SingleQuoteString) WS
BoolValue           = WS ('true' | 'false') WS
NullValue           = WS 'null' WS

<WS>                = <#'\\s*'>

<DoubleQuoteString> = #\"\\\"[^\\\"\\\\]*(?:\\\\.[^\\\"\\\\]*)*\\\"\"
<SingleQuoteString> = #\"'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'\"
             ")


(defn parse-filter
  [s]
  (insta/parse cimi-parser s))

(defn filter-syntax-error
  [text]
  (let [res (parse-filter text)]
    (when (insta/failure? res)
      (->> res insta/get-failure prn-str str/split-lines rest (str/join "\n")))))

(def transform-map
  {:And         (fn [& args]
                  (vec (interpose {:el "logic" :value "and"} args)))
   :Filter      (fn [& args]
                  (first args))
   :Or          (fn [& args]
                  (vec (interpose {:el "logic" :value "or"} args)))
   :Comp        (fn [a o v]
                  (if (string? a)
                    {:el        "attribute"
                     :attribute a
                     :operation o
                     :value     v}
                    [{:el "logic" :value "("}
                     a
                     {:el "logic" :value ")"}]))
   :EqOp        identity
   :FullTextOp  identity
   :PrefixOp    identity
   :RelOp       identity
   :Attribute   str
   :IntValue    int
   :StringValue #(subs % 1 (dec (count %)))
   :BoolValue   #(case %
                   "true" true
                   "false" false)
   :NullValue   (constantly value-null)
   :DateValue   identity})

(defn transform->data
  [parsed-tree]
  (flatten (insta-transform/transform transform-map parsed-tree)))


(defn filter-str->data
  [filter-str]
  (let [parsed-filter (parse-filter filter-str)]
    (when-not (insta/failure? parsed-filter)
      (as-> (transform->data parsed-filter) res
            (interpose {:el "empty"} res)
            (concat [{:el "empty"}]
                    res
                    [{:el "empty"}])
            (into [] res)))))


(defn data->filter-str
  [data]
  (->> (remove #(= (:el %) "empty") data)
       (map
         #(let [{:keys [el attribute operation value]} %]
            (cond
              (= el "logic") value
              (value-is-null? value) (str/join [attribute operation "null"])
              (false? value) (str/join [attribute operation "false"])
              (string? value) (str/join [attribute operation (when value (str "'" value "'"))])
              :else (str/join [attribute operation (when value value)]))))
       (str/join " ")))
