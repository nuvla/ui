(ns sixsq.nuvla.ui.about.events
  (:require [re-frame.core :refer [inject-cofx reg-event-fx]]
            [sixsq.nuvla.ui.about.spec :as spec]
            [sixsq.nuvla.ui.about.utils :as utils]
            [sixsq.nuvla.ui.utils.general :as general-utils]))

(def feature-flags-storage-key "nuvla.ui.feature-flags")

(reg-event-fx
  ::set-feature-flag
  (fn [{{:keys [::spec/enabled-feature-flags] :as db} :db} [_ k enable?]]
    (let [enabled-feature-flags (utils/set-feature-flag enabled-feature-flags k enable?)]
      {:db          (assoc db ::spec/enabled-feature-flags enabled-feature-flags)
       :storage/set {:session? false
                     :name     feature-flags-storage-key
                     :value    (general-utils/edn->json
                                 enabled-feature-flags)}})))

(reg-event-fx
  ::init-feature-flags
  [(inject-cofx :storage/get {:name feature-flags-storage-key})]
  (fn [{db                      :db
        persisted-feature-flags :storage/get}]
    {:db (if persisted-feature-flags
           (assoc db ::spec/enabled-feature-flags
                     (->> persisted-feature-flags
                          general-utils/json->edn
                          set
                          utils/keep-exsiting-feature-flags))
           db)}))

