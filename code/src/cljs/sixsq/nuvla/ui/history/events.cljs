(ns sixsq.nuvla.ui.history.events
  (:require
    [re-frame.core :refer [reg-event-fx]]
    [sixsq.nuvla.ui.main.spec :as main-spec]
    [sixsq.nuvla.ui.routing.utils :refer [url->route-path-params]]
    [taoensso.timbre :as log]))


(reg-event-fx
  ::navigate
  (fn [{{:keys [::main-spec/changes-protection? router] :as db} :db} [_ relative-url params query]]
    (let [[route params] (if (string? relative-url)
                           (url->route-path-params router relative-url)
                           [relative-url params])
          nav-effect {:fx [[:dispatch [:sixsq.nuvla.ui.routing.router/push-state route params query]]]}]
      (if changes-protection?
        {:db (assoc db ::main-spec/ignore-changes-modal nav-effect)}
        (do
          (log/info "triggering navigate effect " (str {:relative-url relative-url
                                                         :route route
                                                         :params params
                                                         :query query}))
          nav-effect)))))
