(ns sixsq.nuvla.ui.docs.subs
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-sub]]
    [sixsq.nuvla.ui.docs.events :as events]
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
  [{:keys [id resourceMetadata resourceURI] :as resource}]
  ;(log/error "resourceMetadata" resourceMetadata)
  ;(when id
  ;  (log/error (str "resource-metadata/" (str/replace id #"/" "-"))))
  (cond
    resourceMetadata resourceMetadata
    (re-find #"-template/" (str id)) (str "resource-metadata/" (str/replace id #"/" "-"))
    :else (let [resource-name (-> resourceURI str (str/split #"/") last str/lower-case)
                collection-name (cond-> resource-name
                                        (str/ends-with? resource-name "collection")
                                        (subs 0 (- (count resource-name)
                                                   (count "collection"))))]
            (str "resource-metadata/" collection-name))))


(reg-sub
  ::document
  :<- [::documents]
  (fn [documents [_ resource]]
    (if (seq documents)
      (let [resource-metadata-id (resolve-metadata-id resource)]
        ;        (log/error "search for metadata id:" resource-metadata-id (sort (keys documents)))
        (get documents resource-metadata-id))
      (dispatch [::events/get-documents]))))
