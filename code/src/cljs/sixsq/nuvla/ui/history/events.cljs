(ns sixsq.nuvla.ui.history.events
  (:require
    [re-frame.core :refer [reg-event-fx]]
    [sixsq.nuvla.ui.history.effects :as fx]
    [sixsq.nuvla.ui.main.spec :as main-spec]
    [sixsq.nuvla.ui.routing.utils :refer [url->route-path-params]]
    [taoensso.timbre :as log]))


(reg-event-fx
  ::initialize
  (fn [_ [_ path-prefix]]
    (log/info "setting history path-prefix to " path-prefix)
    {::fx/initialize [path-prefix]}))


(reg-event-fx
  ::navigate
  (fn [{{:keys [::main-spec/changes-protection? router] :as db} :db} [_ route params query]]
    (let [[route params] (if (string? route)
                           (url->route-path-params router route)
                           [route params])
          nav-effect {:fx [[:dispatch [:sixsq.nuvla.ui.routing.router/push-state route params query]]]}]
      (if changes-protection?
        {:db (assoc db ::main-spec/ignore-changes-modal nav-effect)}
        (do
          (log/info "triggering navigate effect " (str {:route route
                                                        :params params
                                                        :query query}))
          nav-effect)))))


#_(reg-event-fx
    ::navigate-js-location
    (fn [_ [_ url]]
      (log/info "triggering navigate js location effect " url)
      {::fx/navigate-js-location [url]}))
