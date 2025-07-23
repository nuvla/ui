(ns sixsq.nuvla.ui.pages.groups.events
  (:require [day8.re-frame.http-fx]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.common-components.messages.events :as messages-events]
            [sixsq.nuvla.ui.config :as config]
            [sixsq.nuvla.ui.session.events :as session-events]
            [sixsq.nuvla.ui.session.spec :as session-spec]
            [sixsq.nuvla.ui.utils.response :as response]))

(reg-event-fx
  ::add-group
  (fn [{_db :db} [_ id name description loading?]]
    (let [user {:template    {:href             "group-template/generic"
                              :group-identifier id}
                :name        name
                :description description}]
      {::cimi-api-fx/add
       ["group" user
        #(let [{:keys [status message resource-id]} (response/parse %)]
           (dispatch [::session-events/search-groups])
           (dispatch [::messages-events/add
                      {:header  (cond-> (str "added " resource-id)
                                        status (str " (" status ")"))
                       :content message
                       :type    :success}])
           (reset! loading? false))]})))

(reg-event-fx
  ::edit-group
  (fn [_ [_ group]]
    (let [id (:id group)]
      {::cimi-api-fx/edit [id group #(if (instance? js/Error %)
                                       (let [{:keys [status message]} (response/parse-ex-info %)]
                                         (dispatch [::messages-events/add
                                                    {:header  (cond-> "Group update failed"
                                                                      status (str " (" status ")"))
                                                     :content message
                                                     :type    :error}]))
                                       (do
                                         (dispatch [::session-events/search-groups])
                                         (dispatch [::messages-events/add
                                                    {:header  "Group updated"
                                                     :content "Group updated successfully."
                                                     :type    :info}])))]})))

(reg-event-fx
  ::invite-to-group
  (fn [{db :db} [_ group-id username]]
    (let [on-error   #(let [{:keys [status message]} (response/parse-ex-info %)]
                        (dispatch [::messages-events/add
                                   {:header  (cond-> (str "Invitation to " group-id " for " username " failed!")
                                                     status (str " (" status ")"))
                                    :content message
                                    :type    :error}]))
          on-success #(dispatch [::messages-events/add
                                 {:header  "Invitation successfully sent to user"
                                  :content (str "User will appear in " group-id
                                                " when he accept the invitation sent to his email address.")
                                  :type    :info}])
          data       {:username         username
                      :redirect-url     (str (::session-spec/server-redirect-uri db)
                                             "?message=join-group-accepted")
                      :set-password-url (str @config/path-prefix "/set-password")}]
      {::cimi-api-fx/operation [group-id "invite" on-success :on-error on-error :data data]})))