(ns sixsq.nuvla.ui.pages.apps.effects
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<!]]
            [clojure.string :as str]
            [re-frame.core :refer [reg-fx]]
            [sixsq.nuvla.client.api :as api]
            [sixsq.nuvla.ui.cimi-api.effects :refer [CLIENT]]
            [sixsq.nuvla.ui.pages.apps.utils :as utils]))


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
                                {:subtype     utils/subtype-project
                                 :name        "Applications"
                                 :description "cloud applications at your service"})
            {:keys [subtype id]} project-metadata
            path-with-version (str id (when
                                        (not (or (nil? version) (neg? version)))
                                        (str "_" version)))
            module            (if (not= utils/subtype-project subtype)
                                (when id (<! (api/get @CLIENT path-with-version)))
                                project-metadata)

            children          (when (= subtype utils/subtype-project)
                                (:resources (<! (api/search @CLIENT :module {:filter children-filter}))))

            module-data       (cond-> module
                                      children (assoc :children children))]
        (callback module-data)))))
