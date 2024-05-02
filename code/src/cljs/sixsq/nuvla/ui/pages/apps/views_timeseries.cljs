(ns sixsq.nuvla.ui.pages.apps.views-timeseries
  (:require [re-frame.core :refer [dispatch subscribe]]
            [sixsq.nuvla.ui.pages.apps.apps-application.events :as events]
            [sixsq.nuvla.ui.pages.apps.apps-application.subs :as subs]))

(defn AppData []
  (let [ts-id "timeseries/f9f76bdd-56e9-4dde-bbcf-30d1b84625e0"
        query-name "test-query1"
        from        "2024-04-21T00:00:00.000Z"
        to "2024-04-24T23:59:59.000Z"
        granularity "1-days"
        app-data (subscribe [::subs/app-data])
        fetch-edge-stats          (fn []
                                    (dispatch [::events/fetch-app-data ]))]
    (dispatch [::events/fetch-app-data])
    (fn []
      (into [:div] (mapv :timestamp (:ts-data (first (:test-query1 @app-data))))))))