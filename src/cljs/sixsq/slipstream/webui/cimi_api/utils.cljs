(ns sixsq.slipstream.webui.cimi-api.utils
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [camel-snake-kebab.core :as csk]
    [cljs.core.async :refer [<! >! chan]]
    [clojure.set :as set]
    [clojure.walk :as walk]
    [re-frame.core :refer [dispatch reg-fx]]
    [sixsq.slipstream.client.api.cimi :as cimi]
    [taoensso.timbre :as log]))


(def ^:const common-attrs #{:id, :resourceURI, :created, :updated, :name, :description
                            :properties, :resourceMetadata, :operations, :acl})


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
    (let [session-collection (<! (cimi/search client :sessions))]
      (when-not (instance? js/Error session-collection)
        (-> session-collection :sessions first)))))


(defn absolute-url [baseURI relative-url]
  (str baseURI relative-url))


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
        common-map (select-keys form-data common-attrs)
        template-map (into {} (remove #(common-attrs (first %)) form-data))]
    [common-map template-map]))


(defn create-template
  [resource-type form-data]
  (log/info "resource-type: " resource-type)
  (let [[common-map template-map] (split-form-data form-data)
        template-keyword (-> resource-type (str "Template") csk/->camelCase keyword)] ; FIXME
    (assoc common-map template-keyword template-map)))
