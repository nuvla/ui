(ns sixsq.nuvla.ui.filter-comp.utils
  (:require [instaparse.core :as insta]
            [clojure.string :as str]))

(def cimi-parser (insta/parser
                   "Filter          ::= AndExpr | AndExpr <'or'> Filter
                    AndExpr         ::= Comp | Comp <'and'> AndExpr
                    Comp            ::= Attribute EqOp Value
                    | Attribute RelOp OrdinalValue
                    | Attribute PrefixOp StringValue
                    | Attribute FullTextOp StringValue
                    | WS <'('> Filter <')'> WS

                    FullTextOp      ::= '=='
                    PrefixOp        ::= '^='
                    EqOp            ::= '=' | '!='
                    RelOp           ::= '<' | '<=' | '>=' | '>'

                    Attribute       ::= WS NamespaceTerm ('/' NamespaceTerm)* WS

                    <NamespaceTerm> ::= (Term ':' Term) | Term
                    <Term>          ::= #'([a-zA-Z][\\w-]*[\\w]+)|[a-zA-Z]'
                    <OrdinalValue>  ::= IntValue | DateValue | StringValue
                    <NominalValue>  ::= BoolValue | NullValue
                    <Value>         ::= OrdinalValue | NominalValue
                    IntValue        ::= WS #'\\d+' WS
                    DateValue       ::= WS #'\\d+-\\d+(-\\d+)?(T\\d+:\\d+:\\d+(\\.\\d+)?(Z|[+-]\\d+:\\d+))?' WS
                    <StringValue>   ::= WS (DoubleQuoteString | SingleQuoteString) WS
                    BoolValue       ::= WS ('true' | 'false') WS
                    NullValue       ::= WS 'null' WS

                    <WS>            ::= <#'\\s*'>

                    DoubleQuoteString ::= #\"\\\"[^\\\"\\\\]*(?:\\\\.[^\\\"\\\\]*)*\\\"\"
                    SingleQuoteString ::= #\"'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'\""))


(defn filter-syntax-error
  [text]
  (let [res (cimi-parser text)]
    (when (insta/failure? res)
      (->> res insta/get-failure prn-str str/split-lines rest (str/join "\n")))))

(defn transform
  [parsed]
  (->>
    parsed
    (insta/transform
      {:AndExpr           (fn [& args]
                            (if (= (count args) 1)
                              [(first args)]
                              [(first args)
                               {:el "logic" :value "and"}
                               (second args)]))
       :Filter            (fn [& args]
                            (if (= (count args) 1)
                              [(first args)]
                              [(first args)
                               {:el "logic" :value "or"}
                               (second args)]))
       :EqOp              identity
       :FullTextOp        identity
       :PrefixOp          identity
       :RelOp             identity
       :Comp              (fn [a o v]
                            (if (string? a)
                              {:el        "attribute"
                               :attribute a
                               :operation o
                               :value     v}
                              [{:el "logic" :value "("}
                               a
                               {:el "logic" :value ")"}]))
       :Attribute         identity
       :IntValue          int
       :DoubleQuoteString #(subs % 1 (dec (count %)))
       :SingleQuoteString #(subs % 1 (dec (count %)))
       :BoolValue         #(case %
                             "true" true
                             "false" false)
       :NullValue         (constantly nil)
       :DateValue         identity})
    flatten))


(defn filter-str->data
  [filter-str]
  (let [parsed-filter (cimi-parser filter-str)]
    (when-not (insta/failure? parsed-filter)
      (as-> (transform parsed-filter) res
            (concat [{:el "empty"}]
                    res
                    [{:el "empty"}])
            (into [] res)))))
