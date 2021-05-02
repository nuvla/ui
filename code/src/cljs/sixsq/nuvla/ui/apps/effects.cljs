(ns sixsq.nuvla.ui.apps.effects
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [<!]]
    [clojure.string :as str]
    [re-frame.core :refer [reg-fx]]
    [sixsq.nuvla.client.api :as api]
    [sixsq.nuvla.ui.cimi-api.effects :refer [CLIENT]]))


(reg-fx
  ::get-module
  (fn [[path version callback]]
    (go
      (let [path              (or path "")
            path-filter       (str "path='" path "'")
            children-filter   (str "parent-path='" path "'")
            project-metadata  (if-not (str/blank? path)
                                (-> (<! (api/search @CLIENT :module {:filter path-filter}))
                                    :resources
                                    first)
                                {:subtype     "project"
                                 :name        "Applications"
                                 :description "cloud applications at your service"})
            {:keys [subtype id]} project-metadata
            path-with-version (str id (when
                                        (not (or (nil? version) (neg? version)))
                                        (str "_" version)))
            module            (if (not= "project" subtype)
                                (when id (<! (api/get @CLIENT path-with-version)))
                                project-metadata)

            children          (when (= subtype "project")
                                (:resources (<! (api/search @CLIENT :module {:filter children-filter}))))

            module-data       (cond-> module
                                      children (assoc :children children))]
        (callback module-data)))))
