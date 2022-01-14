(ns sixsq.nuvla.ui.utils.response
  "parses JSON responses from the CIMI API"
  (:require
    [cljs.pprint :refer [pprint]]
    [sixsq.nuvla.ui.utils.general :as utils]))


(defn parse
  "Expects to have an ExceptionInfo object passed as an argument."
  [json-str]
  (if (string? json-str)
    (try
      (let [{:keys [status message resource-id] :as document} (utils/json->edn json-str)]
        (if (or status message resource-id)
          document
          {:message (with-out-str (pprint (or document json-str)))}))
      (catch :default _
        {:message json-str}))
    {:message (with-out-str (pprint json-str))}))


(defn parse-ex-info
  "Extracts the data from the ExceptionInfo object and parses the body as
   JSON."
  [exception-info]
  (if-let [body (some-> exception-info ex-data :body not-empty)]
    (parse body)
    (parse exception-info)))
