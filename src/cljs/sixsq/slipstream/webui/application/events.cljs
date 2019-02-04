(ns sixsq.slipstream.webui.application.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
    [sixsq.slipstream.webui.application.effects :as application-fx]
    [sixsq.slipstream.webui.application.spec :as spec]
    [sixsq.slipstream.webui.application.utils :as utils]
    [sixsq.slipstream.webui.client.spec :as client-spec]
    [sixsq.slipstream.webui.history.events :as history-evts]
    [sixsq.slipstream.webui.main.spec :as main-spec]
    [taoensso.timbre :as log]))


(reg-event-db
  ::set-module
  (fn [db [_ module-path module]]
    (assoc db ::spec/completed? true
              ::spec/module-path module-path
              ::spec/module module)))


(reg-event-db
  ::open-add-modal
  (fn [db _]
    (assoc db ::spec/add-modal-visible? true)))


(reg-event-db
  ::close-add-modal
  (fn [db _]
    (assoc db ::spec/add-modal-visible? false
              ::spec/add-data nil)))



(defn fixup-image-data
  [{:keys [type connector image-id author os networkType loginUser] :as data}]
  (if (= "IMAGE" type)
    (-> data
        (dissoc :connector :image-id :author :os :networkType :loginUser)
        (assoc-in [:content :imageIDs] {(keyword connector) image-id})
        (assoc-in [:content :author] author)
        (assoc-in [:content :os] os)
        (assoc-in [:content :networkType] networkType)
        (assoc-in [:content :loginUser] loginUser))
    data))


(reg-event-fx
  ::add-module
  (fn [{{:keys [::client-spec/client
                ::main-spec/nav-path
                ::spec/add-data
                ::spec/active-tab] :as db} :db} _]
    (when client
      (let [path (or (utils/nav-path->module-path nav-path) "")
            {project-name :name :as form-data} (get add-data active-tab)
            module-path (if (str/blank? path)
                          project-name
                          (str path "/" project-name))
            data (-> form-data
                     (assoc :type (-> active-tab name str/upper-case)
                            :parentPath path
                            :path module-path)
                     fixup-image-data)]
        {::application-fx/create-module [client path data
                                         #(do
                                            (dispatch [::close-add-modal])
                                            (dispatch [::history-evts/navigate (str "application/" module-path)]))]}))))


(reg-event-fx
  ::get-module
  (fn [{{:keys [::client-spec/client ::main-spec/nav-path] :as db} :db} _]
    (when client
      (let [path (utils/nav-path->module-path nav-path)]
        {:db                         (assoc db ::spec/completed? false
                                               ::spec/module-path nil
                                               ::spec/module nil)
         ::application-fx/get-module [client path #(dispatch [::set-module path %])]}))))


(reg-event-db
  ::update-add-data
  (fn [{:keys [::spec/add-data] :as db} [_ path value]]
    (assoc-in db (concat [::spec/add-data] path) value)))


(reg-event-db
  ::set-active-tab
  (fn [db [_ active-tab]]
    (assoc db ::spec/active-tab active-tab)))

