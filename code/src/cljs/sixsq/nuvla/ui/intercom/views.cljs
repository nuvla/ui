(ns sixsq.nuvla.ui.intercom.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.nuvla.ui.intercom.subs :as subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.profile.subs :as profile-subs]
    [sixsq.nuvla.ui.session.subs :as session-subs]
    [sixsq.nuvla.ui.utils.intercom :as intercom]))

(defn widget
  []
  (fn []
    (let [_                   (subscribe [::main-subs/nav-path])
          app-id              (subscribe [::main-subs/config :intercom-app-id])
          email               (subscribe [::session-subs/identifier])
          active-claim        (subscribe [::session-subs/active-claim])
          subscription        (subscribe [::profile-subs/subscription])
          trial-start         (:trial-start @subscription)
          trial-end           (:trial-end @subscription)
          subscription-status (:status @subscription)
          ;customer            (subscribe [::profile-subs/customer]) TODO: recover the id of the subscription owner
          events              (subscribe [::subs/events])]
      @_                                                    ;to force the component to refresh
      [intercom/Intercom (merge
                           {:appID (or @app-id "")}
                           {:website "Nuvla"}
                           (when (not (nil? @email)) {:email @email})
                           (when (not (nil? subscription-status)) {"Subscription status" subscription-status})
                           (when (not (nil? trial-start)) {"Trial start" trial-start})
                           (when (not (nil? trial-end)) {"Trial end" trial-end})
                           (if (and (not (nil? @active-claim))
                                    (str/starts-with? @active-claim "group/"))
                             {:group @active-claim}
                             {:group "self"})
                           @events)])))
