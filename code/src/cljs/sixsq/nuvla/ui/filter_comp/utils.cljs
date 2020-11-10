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
