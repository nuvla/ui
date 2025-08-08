(ns sixsq.nuvla.ui.pages.groups.events
  (:require [day8.re-frame.http-fx]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.common-components.messages.events :as messages-events]
            [sixsq.nuvla.ui.config :as config]
            [sixsq.nuvla.ui.session.events :as session-events]
            [sixsq.nuvla.ui.session.spec :as session-spec]
            [sixsq.nuvla.ui.pages.groups.spec :as spec]
            [sixsq.nuvla.ui.utils.response :as response]))

(reg-event-fx
  ::add-group
  (fn [{_db :db} [_ {:keys [parent-group group-identifier group-name group-desc loading?]}]]
    (let [on-success #(let [{:keys [status message resource-id]} (response/parse %)]
                        (dispatch [::session-events/search-groups])
                        (dispatch [::messages-events/add
                                   {:header  (cond-> (str "added " resource-id)
                                                     status (str " (" status ")"))
                                    :content message
                                    :type    :success}])
                        (reset! loading? false))]
      (if parent-group
        {::cimi-api-fx/operation
         [(:id parent-group) "add-subgroup"
          on-success
          :data {:group-identifier group-identifier
                 :name             group-name
                 :description      group-desc}]}
        {::cimi-api-fx/add
         ["group" {:template    {:href             "group-template/generic"
                                 :group-identifier group-identifier}
                   :name        group-name
                   :description group-desc} on-success]}))))

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
          on-success #(do (dispatch [::messages-events/add
                                     {:header  "Invitation successfully sent to user"
                                      :content (str "User will appear in " group-id
                                                    " when he accept the invitation sent to his email address.")
                                      :type    :info}])
                          (dispatch [::get-pending-invitations group-id]))
          data       {:username         username
                      :redirect-url     (str (::session-spec/server-redirect-uri db)
                                             "?message=join-group-accepted")
                      :set-password-url (str @config/path-prefix "/set-password")}]
      {::cimi-api-fx/operation [group-id "invite" on-success :on-error on-error :data data]})))

(reg-event-fx
  ::get-pending-invitations
  (fn [{db :db} [_ group-id]]
    (let [on-success #(dispatch [::set-pending-invitations %])]
      {:db (assoc db ::spec/pending-invitations nil)
       ::cimi-api-fx/operation
       [group-id "get-pending-invitations" on-success]})))

(reg-event-db
  ::set-pending-invitations
  (fn [db [_ pending-invitations]]
    (assoc db ::spec/pending-invitations pending-invitations)))

(reg-event-fx
  ::revoke
  (fn [_ [_ {group-id :id :as _group} invited-email]]
    (let [on-success #(do (dispatch [::messages-events/add
                                     {:header  "Invitation successfully revoked"
                                      :content (str "Invitation successfully revoked for "
                                                    invited-email " from " group-id ".")
                                      :type    :info}])
                          (dispatch [::get-pending-invitations group-id]))]
      {::cimi-api-fx/operation [group-id "revoke-invitation" on-success
                                :data {:email invited-email}]})))
