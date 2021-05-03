(ns sixsq.nuvla.ui.docs.subs
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [reg-sub]]
    [sixsq.nuvla.ui.docs.spec :as spec]))


(reg-sub
  ::loading?
  (fn [db]
    (::spec/loading? db)))


(reg-sub
  ::documents
  (fn [db]
    (::spec/documents db)))


(defn resolve-metadata-id
  [{:keys [id resource-metadata resource-url] :as _resource}]
  (cond
    resource-metadata resource-metadata
    (re-find #"-template/" (str id)) (str "resource-metadata/" (str/replace id #"/" "-"))
    :else (let [resource-name   (-> resource-url str (str/split #"/") last str/lower-case)
                collection-name (cond-> resource-name
                                        (str/ends-with? resource-name "collection")
                                        (subs 0 (- (count resource-name)
                                                   (count "collection"))))]
            (str "resource-metadata/" collection-name))))
