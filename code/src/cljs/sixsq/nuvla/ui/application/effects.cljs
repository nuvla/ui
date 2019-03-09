(ns sixsq.nuvla.ui.application.effects
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-fx]]
    [sixsq.nuvla.client.api :as api]
    [taoensso.timbre :as log]))


(reg-fx
  ::get-module
  (fn [[client path callback]]
    (go
      (let [path (or path "")
            path-filter (str "path='" path "'")
            children-filter (str "parentPath='" path "'")

            {:keys [type id] :as project-metadata} (if-not (str/blank? path)
                                                     (-> (<! (api/search client "module" {:filter path-filter}))
                                                         :resources
                                                         first)
                                                     {:type        "PROJECT"
                                                      :name        "Applications"
                                                      :description "cloud applications at your service"})

            module (if (not= "PROJECT" type)
                     (<! (api/get client id))
                     project-metadata)

            children (when (= type "PROJECT")
                       (:resources (<! (api/search client "module" {}))))

            module-data (cond-> module
                                children (assoc :children children))]
(log/infof "children %s" children)
        (callback module-data)))))


(reg-fx
  ::create-module
  (fn [[client path data callback]]
    (go
      (let [{:keys [status] :as response} (<! (api/add client "module" data))]
        (when (= 201 status)
          (callback response))))))


