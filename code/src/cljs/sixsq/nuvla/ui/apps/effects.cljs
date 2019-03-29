(ns sixsq.nuvla.ui.apps.effects
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
  (fn [[client path version callback]]
    (go
      (let [path (or path "")
            path-filter (str "path='" path "'")
            children-filter (str "parent-path='" path "'")
            {:keys [type id] :as project-metadata} (if-not (str/blank? path)
                                                     (-> (<! (api/search client "module" {:filter path-filter}))
                                                         :resources
                                                         first)
                                                     {:type        "PROJECT"
                                                      :name        "Applications"
                                                      :description "cloud applications at your service"})
            path-with-version (str id (when
                                        (not (nil? version))
                                        (str "_" version)))
            module (if (not= "PROJECT" type)
                     (<! (api/get client path-with-version))
                     project-metadata)

            children (when (= type "PROJECT")
                       (:resources (<! (api/search client "module" {:filter children-filter}))))

            module-data (cond-> module
                                children (assoc :children children))
            ]
        (callback module-data)))))

