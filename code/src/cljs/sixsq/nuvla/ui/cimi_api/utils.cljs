(ns sixsq.nuvla.ui.cimi-api.utils
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<! >! chan]]
    [clojure.set :as set]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [re-frame.core :refer [dispatch reg-fx]]
    [sixsq.nuvla.client.api :as api]
    [sixsq.nuvla.client.async :as async-client]
    [sixsq.nuvla.ui.history.utils :as utils]
    [sixsq.nuvla.ui.utils.defines :as defines]
    [taoensso.timbre :as log]))


(def NUVLA_URL (delay (if (str/blank? defines/HOST_URL) (utils/host-url) defines/HOST_URL)))

(def CLIENT (delay (async-client/instance (str @NUVLA_URL "/api/cloud-entry-point"))))


(def ^:const common-attrs #{:id, :resource-url, :created, :updated, :name, :description
                            :properties, :resource-metadata, :operations, :acl})


(defn select-common-attrs
  [resource]
  (select-keys resource common-attrs))


(defn remove-common-attrs
  [resource]
  (->> common-attrs
       (set/difference (set (keys resource)))
       (select-keys resource)))


(defn sanitize-params [params]
  (into {} (remove (comp nil? second) params)))


(defn get-current-session
  [client]
  (go
    (let [session-collection (<! (api/search @CLIENT :session))]
      (when-not (instance? js/Error session-collection)
        (-> session-collection :resources first)))))


(defn absolute-url [base-uri relative-url]
  (str base-uri relative-url))


(defn login-form-fields [{:keys [params-desc] :as tpl}]
  (->> params-desc
       keys
       (map (fn [k] [k ""]))
       (into {})))


(defn clear-form-data [m]
  (let [f (fn [[k v]] (if (string? v) [k ""] [k v]))]
    (walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m)))


(defn split-form-data
  [form-data]
  (let [common-attrs #{:name :description :properties}
        common-map   (select-keys form-data common-attrs)
        template-map (into {} (remove #(common-attrs (first %)) form-data))]
    [common-map template-map]))


(defn create-template
  [resource-type form-data]
  (let [[common-map template-map] (split-form-data form-data)]
    (assoc common-map :template template-map)))
