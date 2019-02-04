(ns sixsq.slipstream.webui.legacy-application.effects
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-fx]]
    [sixsq.slipstream.client.api.modules :as modules]))


(def ^:const metadata-fields #{:shortName :description :category :creation :version :logoLink})


(def ^:const item-keys #{:name :description :category :version})


(def ^:const recipe-fields #{:prerecipe :packages :recipe})


(defn get-module-items
  [module]
  (let [items (->> module vals first :children :item)]
    (if (map? items)
      [(select-keys items item-keys)]                       ;; single item
      (mapv #(select-keys % item-keys) items))))            ;; multiple items


(defn format-packages
  [{:keys [package] :as packages}]
  (let [package (if (map? package) [package] package)]
    (str/join "\n" (mapv :name package))))


(reg-fx
  ::get-module
  (fn [[client module-id]]
    (go
      (let [module (if (nil? module-id) {} (<! (modules/get-module client module-id)))

            {:keys [prerecipe packages recipe]} (-> module vals first (select-keys recipe-fields))

            metadata (-> module vals first (select-keys metadata-fields))

            targets (cond-> (->> (-> module vals first :targets :target)
                                 (map (juxt #(-> % :name keyword) :content))
                                 (filter second)
                                 (into {}))
                            prerecipe (assoc :prerecipe prerecipe)
                            recipe (assoc :recipe recipe)
                            packages (assoc :packages (format-packages packages)))

            output-parameters (->> (-> module vals first :parameters :entry)
                                   (map :parameter)
                                   (filter #(= "Output" (:category %))))

            input-parameters (->> (-> module vals first :parameters :entry)
                                  (map :parameter)
                                  (filter #(= "Input" (:category %))))

            children (if (nil? module-id)
                       (<! (modules/get-module-children client nil))
                       (get-module-items module))

            module-data {:metadata   metadata
                         :targets    targets
                         :parameters (concat input-parameters output-parameters)
                         :children   children}]
        (dispatch [:sixsq.slipstream.webui.legacy-application.events/set-module module-id module-data])))))
