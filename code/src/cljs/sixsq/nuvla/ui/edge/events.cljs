(ns sixsq.nuvla.ui.edge.events
  (:require
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.edge.effects :as fx]
    [sixsq.nuvla.ui.edge.spec :as spec]
    [sixsq.nuvla.ui.edge.utils :as utils]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.response :as response]
    [taoensso.timbre :as log]))

(def refresh-id :nuvlabox-get-nuvlaboxes)
(def refresh-summary-id :nuvlabox-get-nuvlaboxes-summary)
(def refresh-id-clusters :nuvlabox-get-nuvlabox-clusters)

(reg-event-fx
  ::refresh
  (fn [_ _]
    {:fx [[:dispatch [::main-events/action-interval-start {:id        refresh-id
                                                           :frequency 10000
                                                           :event     [::get-nuvlaboxes]}]]
          [:dispatch [::main-events/action-interval-start {:id        refresh-summary-id
                                                           :frequency 10000
                                                           :event     [::get-nuvlaboxes-summary]}]]
          [:dispatch [::main-events/action-interval-start {:id        refresh-id-clusters
                                                           :frequency 10000
                                                           :event     [::get-nuvlabox-clusters]}]]]}))


(reg-event-fx
  ::set-page
  (fn [{db :db} [_ page]]
    {:db       (assoc db ::spec/page page)
     :dispatch [::refresh]}))


(reg-event-fx
  ::set-full-text-search
  (fn [{{:keys [::spec/elements-per-page] :as db} :db} [_ full-text-search]]
    {:db       (assoc db ::spec/full-text-search full-text-search
                         ::spec/page 1)
     :dispatch [::refresh]}))


(reg-event-fx
  ::get-nuvlaboxes
  (fn [{{:keys [::spec/state-selector
                ::spec/page
                ::spec/elements-per-page
                ::spec/full-text-search] :as db} :db} _]
    {:db                   (assoc db ::spec/loading? true)
     ::cimi-api-fx/search  [:nuvlabox
                            (utils/get-query-params full-text-search page elements-per-page
                                                    state-selector)
                            #(dispatch [::set-nuvlaboxes %])]}))


(reg-event-fx
  ::set-nuvlaboxes
  (fn [{:keys [db]} [_ {:keys [resources] :as nuvlaboxes}]]
    (if (instance? js/Error nuvlaboxes)
      (dispatch [::messages-events/add
                 (let [{:keys [status message]} (response/parse-ex-info nuvlaboxes)]
                   {:header  (cond-> (str "failure getting nuvlaboxes")
                                     status (str " (" status ")"))
                    :content message
                    :type    :error})])
      (cond->
        {:db (assoc db ::spec/nuvlaboxes nuvlaboxes
                       ::spec/loading? false)}
        ))))


(reg-event-fx
  ::set-nuvlaboxes-summary
  (fn [{db :db} [_ nuvlaboxes-summary]]
    {:db (assoc db ::spec/nuvlaboxes-summary nuvlaboxes-summary)}))


(reg-event-fx
  ::get-nuvlaboxes-summary
  (fn [{{:keys [::spec/full-text-search
                ::spec/nuvlabox-cluster] :as db} :db} _]
    {:db                   (assoc db ::spec/loading? true)
     ::cimi-api-fx/search  [:nuvlabox
                            (utils/get-query-aggregation-params
                              full-text-search
                              "terms:online,terms:state"
                              (if nuvlabox-cluster
                                (->> (concat (:nuvlabox-managers nuvlabox-cluster) (:nuvlabox-workers nuvlabox-cluster))
                                  (map #(str "id='" % "'"))
                                  (apply general-utils/join-or))
                                nil))
                            #(dispatch [::set-nuvlaboxes-summary %])]}))


(reg-event-fx
  ::set-nuvlabox-clusters
  (fn [{:keys [db]} [_ {:keys [resources] :as nuvlabox-clusters}]]
    (if (instance? js/Error nuvlabox-clusters)
      (dispatch [::messages-events/add
                 (let [{:keys [status message]} (response/parse-ex-info nuvlabox-clusters)]
                   {:header  (cond-> (str "failure getting nuvlabox clusters")
                               status (str " (" status ")"))
                    :content message
                    :type    :error})])
      (cond->
        {:db (assoc db ::spec/nuvlabox-clusters nuvlabox-clusters
                       ::spec/loading? false)}
        ))))


(reg-event-fx
  ::get-nuvlabox-clusters
  (fn [{{:keys [::spec/page
                ::spec/elements-per-page
                ::spec/full-text-search] :as db} :db} _]
    {:db                   (assoc db ::spec/loading? true)
     ::cimi-api-fx/search  [:nuvlabox-cluster
                            (utils/get-query-params full-text-search page elements-per-page nil)
                            #(dispatch [::set-nuvlabox-clusters %])]}))


(reg-event-fx
  ::set-nuvlaboxes-summary-all
  (fn [{db :db} [_ nuvlaboxes-summary]]
    {:db (assoc db ::spec/nuvlaboxes-summary-all nuvlaboxes-summary)}))


(reg-event-fx
  ::get-nuvlaboxes-summary-all
  (fn [{db :db} _]
    {:db                   (assoc db ::spec/loading? true)
     ::cimi-api-fx/search  [:nuvlabox
                            (utils/get-query-aggregation-params nil "terms:online,terms:state" nil)
                            #(dispatch [::set-nuvlaboxes-summary-all %])]}))


(reg-event-fx
  ::set-state-selector
  (fn [{db :db} [_ state-selector]]
    {:db (assoc db ::spec/state-selector state-selector
                   ::spec/page 1)
     :dispatch [::get-nuvlaboxes]}))


(reg-event-db
  ::open-modal
  (fn [db [_ modal-id]]
    (assoc db ::spec/open-modal modal-id)))


(reg-event-fx
  ::create-ssh-key
  (fn [_ [_ ssh-template dispatch-vector]]
    {::cimi-api-fx/add [:credential ssh-template
                        #(do
                           (dispatch [::set-nuvlabox-ssh-keys {:ids         [(:resource-id %)]
                                                               :public-keys [(:public-key %)]}])
                           (dispatch [::set-nuvlabox-created-private-ssh-key (:private-key %)])
                           (dispatch dispatch-vector))]}))


(reg-event-fx
  ::find-nuvlabox-ssh-keys
  (fn [_ [_ ssh-keys-ids dispatch-vector]]
    {::cimi-api-fx/search
     [:credential
      {:filter (cond-> (apply general-utils/join-or
                              (map #(str "id='" % "'") ssh-keys-ids)))
       :select "public-key"
       :last   10000}
      #(do
         (dispatch [::set-nuvlabox-ssh-keys {:ids         ssh-keys-ids
                                             :public-keys (into [] (map :public-key
                                                                        (:resources %)))}])
         (dispatch dispatch-vector))]}))


(reg-event-db
  ::set-nuvlabox-ssh-keys
  (fn [db [_ ssh-key-list]]
    (assoc db ::spec/nuvlabox-ssh-key ssh-key-list)))


(reg-event-db
  ::set-nuvlabox-created-private-ssh-key
  (fn [db [_ private-key]]
    (assoc db ::spec/nuvlabox-private-ssh-key private-key)))


(reg-event-fx
  ::assign-ssh-keys
  (fn [_ [_ {:keys [ids]} nuvlabox-id]]
    {::cimi-api-fx/edit [nuvlabox-id {:ssh-keys ids}
                         #(when (instance? js/Error %)
                            (let [{:keys [status message]} (response/parse-ex-info %)]
                              (dispatch [::messages-events/add
                                         {:header  (cond-> (str "error editing " nuvlabox-id)
                                                           status (str " (" status ")"))
                                          :content message
                                          :type    :error}])))]}))


(reg-event-fx
  ::create-nuvlabox
  (fn [_ [_ creation-data]]
    {::cimi-api-fx/add [:nuvlabox creation-data
                        #(dispatch [::set-created-nuvlabox-id %])]}))


(reg-event-db
  ::set-created-nuvlabox-id
  (fn [db [_ {:keys [resource-id]}]]
    (dispatch [::get-nuvlaboxes])
    (assoc db ::spec/nuvlabox-created-id resource-id)))


(reg-event-fx
  ::create-nuvlabox-usb-api-key
  (fn [_ [_ creation-data]]
    {::cimi-api-fx/add [:credential creation-data
                        #(dispatch [::set-nuvlabox-usb-api-key {:resource-id (:resource-id %)
                                                                :secret-key  (:secret-key %)}])]}))


(reg-event-db
  ::set-nuvlabox-usb-api-key
  (fn [db [_ apikey]]
    (assoc db ::spec/nuvlabox-usb-api-key apikey)))


(reg-event-db
  ::set-vpn-infra
  (fn [db [_ {:keys [resources]}]]
    (assoc db ::spec/vpn-infra resources)))


(reg-event-fx
  ::get-vpn-infra
  (fn [{:keys [db]} _]
    {:db                  (assoc db ::spec/vpn-infra nil)
     ::cimi-api-fx/search [:infrastructure-service
                           {:filter "subtype='vpn' and vpn-scope='nuvlabox'"
                            :select "id, name, description"
                            :last   10000}
                           #(dispatch [::set-vpn-infra %])]}))


(reg-event-db
  ::set-nuvlabox-releases
  (fn [db [_ {:keys [resources]}]]
    (assoc db ::spec/nuvlabox-releases resources)))


(reg-event-fx
  ::get-nuvlabox-releases
  (fn [{:keys [db]} _]
    {:db                  (assoc db ::spec/nuvlabox-releases nil)
     ::cimi-api-fx/search [:nuvlabox-release
                           {:select  "id, release, pre-release, release-notes, url, compose-files"
                            :orderby "release-date:desc"
                            :last    10000}
                           #(dispatch [::set-nuvlabox-releases %])]}))


(reg-event-fx
  ::set-nuvlabox-cluster
  (fn [{:keys [db]} [_ nuvlabox-cluster]]
    {:db        (assoc db ::spec/nuvlabox-cluster nuvlabox-cluster)
     :dispatch  [::refresh]}))


(reg-event-fx
  ::get-nuvlabox-cluster
  (fn [{:keys [db]} [_ cluster-id]]
    {::cimi-api-fx/get [cluster-id #(dispatch [::set-nuvlabox-cluster %])
                        :on-error #(dispatch [::set-nuvlabox-cluster nil])]}))


(reg-event-db
  ::set-ssh-keys-available
  (fn [db [_ {:keys [resources]}]]
    (assoc db ::spec/ssh-keys-available resources)))


(reg-event-fx
  ::get-ssh-keys-available
  (fn [{:keys [db]} [_ subtypes additional-filter]]
    {:db                  (assoc db ::spec/ssh-keys-available nil)
     ::cimi-api-fx/search [:credential
                           {:filter (cond-> (apply general-utils/join-or
                                                   (map #(str "subtype='" % "'") subtypes))
                                            additional-filter (general-utils/join-and
                                                                additional-filter))
                            :last   10000}
                           #(dispatch [::set-ssh-keys-available %])]}))