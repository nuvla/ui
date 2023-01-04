(ns sixsq.nuvla.ui.history.events
  (:require [re-frame.core :refer [reg-event-fx]]
            [sixsq.nuvla.ui.main.spec :as main-spec]
            [sixsq.nuvla.ui.routing.utils :refer [add-base-path]]
            [taoensso.timbre :as log]))


(reg-event-fx
  ::navigate
  (fn [{{:keys [::main-spec/changes-protection?] :as db} :db} [_ relative-url]]
    (let [nav-effect {:fx [[:dispatch [:sixsq.nuvla.ui.routing.router/push-state-by-path (add-base-path relative-url)]]]}]
      (if changes-protection?
        {:db (assoc db ::main-spec/ignore-changes-modal nav-effect)}
        (do
          (log/info "triggering navigate effect " (str {:relative-url relative-url}))
          nav-effect)))))
