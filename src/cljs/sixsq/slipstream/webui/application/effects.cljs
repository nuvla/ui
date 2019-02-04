(ns sixsq.slipstream.webui.application.effects
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-fx]]
    [sixsq.slipstream.client.api.cimi :as cimi]))


(reg-fx
  ::get-module
  (fn [[client path callback]]
    (go
      (let [path (or path "")
            path-filter (str "path='" path "'")
            children-filter (str "parentPath='" path "'")

            {:keys [type id] :as project-metadata} (if-not (str/blank? path)
                                                     (-> (<! (cimi/search client "modules" {:$filter path-filter}))
                                                         :modules
                                                         first)
                                                     {:type        "PROJECT"
                                                      :name        "Applications"
                                                      :description "cloud applications at your service"})

            module (if (not= "PROJECT" type)
                     (<! (cimi/get client id))
                     project-metadata)

            children (when (= type "PROJECT")
                       (:modules (<! (cimi/search client "modules" {:$filter children-filter}))))

            module-data (cond-> module
                                children (assoc :children children))]

        (callback module-data)))))


(reg-fx
  ::create-module
  (fn [[client path data callback]]
    (go
      (let [{:keys [status] :as response} (<! (cimi/add client "modules" data))]
        (when (= 201 status)
          (callback response))))))


