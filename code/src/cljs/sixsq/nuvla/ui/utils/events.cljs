(ns sixsq.nuvla.ui.utils.events
  (:require [re-frame.core :refer [reg-event-fx]]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.routing.utils :as route-utils]
            [sixsq.nuvla.ui.routing.routes :as routes]))

;; TODO: Refactor/move to additional filter or main fx
(reg-event-fx
  ::store-filter-and-open-in-new-tab
  (fn [_ [_ filter-string]]
    (let [uuid (random-uuid)]
      {:storage/set {:session? false
                     :name     uuid
                     :value    filter-string}
       :fx          [[:dispatch
                      [::main-events/open-link
                       (route-utils/name->href
                         {:route-name   ::routes/edges
                          :query-params {:filter-storage-key uuid}})]]]})))
