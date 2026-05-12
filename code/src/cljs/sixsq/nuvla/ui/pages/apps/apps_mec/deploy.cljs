(ns sixsq.nuvla.ui.pages.apps.apps-mec.deploy
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
            [sixsq.nuvla.ui.common-components.messages.events :as messages-events]
            [sixsq.nuvla.ui.pages.apps.events :as apps-events]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]))

(def eligible-mepm-statuses #{"ONLINE" "DEGRADED"})

(defn- response-text->edn
  [text]
  (when-not (str/blank? text)
    (general-utils/json->edn text)))

(defn- mepm-hosts-url
  []
  (str @cimi-api-fx/NUVLA_URL
       "/api/mepm?select=id,name,mec-host-id,status&orderby=name:asc,id:asc&last=1000"))

(defn- app-instances-url
  []
  (str @cimi-api-fx/NUVLA_URL "/api/mec/mm1/app_lcm/v1/app_instances"))

(defn- host-options-from-response
  [{:keys [resources]}]
  (->> resources
       (reduce (fn [acc {:keys [name mec-host-id status]}]
                 (if (or (str/blank? mec-host-id)
                         (not (contains? eligible-mepm-statuses status))
                         (contains? acc mec-host-id))
                   acc
                   (assoc acc
                     mec-host-id
                     {:key   mec-host-id
                      :value mec-host-id
                      :text  (str (or name mec-host-id) " (" mec-host-id ")")})))
               {})
       vals
       (sort-by :text)
       vec))

(defn- fetch-host-options!
  [host-options* loading-hosts?* host-load-error*]
  (reset! loading-hosts?* true)
  (reset! host-load-error* nil)
  (-> (js/fetch
        (mepm-hosts-url)
        #js {:method      "GET"
             :credentials "same-origin"
             :headers     #js {"Accept" "application/json"}})
      (.then (fn [response]
               (-> (.text response)
                   (.then (fn [text]
                            (let [body (response-text->edn text)]
                              (if (.-ok response)
                                (reset! host-options* (host-options-from-response body))
                                (reset! host-load-error* (or (:detail body)
                                                             (:message body)
                                                             "Failed to load MEC hosts.")))))))))
      (.catch (fn [_]
                (reset! host-load-error* "Failed to load MEC hosts.")))
      (.finally (fn []
                  (reset! loading-hosts?* false)))))

(defn- notify!
  [status header content]
  (dispatch [::messages-events/add
             {:header  header
              :content content
              :type    status}]))

(defn- create-app-instance!
  [module-id host-id open?* submitting?*]
  (let [payload (cond-> {:appDId module-id}
                        (not (str/blank? host-id))
                        (assoc :mecHostInformation {:hostId host-id}))]
    (reset! submitting?* true)
    (-> (js/fetch
          (app-instances-url)
          #js {:method      "POST"
               :credentials "same-origin"
               :headers     #js {"Accept"       "application/json"
                                 "Content-Type" "application/json"}
               :body        (js/JSON.stringify (clj->js payload))})
        (.then (fn [response]
                 (-> (.text response)
                     (.then (fn [text]
                              (let [body            (response-text->edn text)
                                    app-instance-id (:appInstanceId body)]
                                (if (.-ok response)
                                  (do
                                    (reset! open?* false)
                                    (notify! :success
                                             "MEC app instance created"
                                             "The app instance was created. Open the deployment details page to instantiate it.")
                                    (dispatch [::apps-events/get-deployments-for-module {:id module-id}])
                                    (when app-instance-id
                                      (dispatch [::routing-events/navigate
                                                 routes/deployment-details
                                                 {:uuid (general-utils/id->uuid app-instance-id)}])))
                                  (notify! :error
                                           "Failed to create MEC app instance"
                                           (or (:detail body)
                                               (:message body)
                                               "The MEC app instance could not be created.")))))))))
        (.catch (fn [_]
                  (notify! :error
                           "Failed to create MEC app instance"
                           "The MEC app instance could not be created.")))
        (.finally (fn []
                    (reset! submitting?* false))))))

(defn open-modal!
  [{:keys [open? selected-host-id host-options loading-hosts? host-load-error disabled?]}]
  (when-not disabled?
    (reset! selected-host-id "")
    (reset! open? true)
    (fetch-host-options! host-options loading-hosts? host-load-error)))

(defn reset-form!
  [{:keys [open? selected-host-id host-load-error submitting?]}]
  (reset! open? false)
  (reset! selected-host-id "")
  (reset! host-load-error nil)
  (reset! submitting? false))

(defn new-deploy-state
  []
  {:open?            (r/atom false)
   :selected-host-id (r/atom "")
   :host-options     (r/atom [])
   :loading-hosts?   (r/atom false)
   :host-load-error  (r/atom nil)
   :submitting?      (r/atom false)})

(defn DeployMenuItem
  [{:keys [disabled? state]}]
  [uix/MenuItem
   {:name     "Deploy"
    :icon     icons/i-rocket
    :disabled disabled?
    :on-click #(open-modal! (assoc state :disabled? disabled?))}])

(defn DeployModal
  [{:keys [module-id disabled? state]}]
  (let [{:keys [open? selected-host-id host-options loading-hosts? host-load-error submitting?]} state]
    [ui/Modal
     {:open       @open?
      :close-icon true
      :on-close   (fn [& _] (reset-form! state))}
     [ui/ModalHeader
      [icons/Icon {:name icons/i-rocket}]
      "Deploy MEC app"]
     [ui/ModalContent
      [ui/Form
       [ui/FormField
        [:label "AppD ID"]
        [ui/Input {:value    module-id
                   :readOnly true}]]
       [ui/FormField
        [:label "Target MEC host"]
        [ui/Input {:value       @selected-host-id
                   :placeholder "Optional: nuvlabox/<uuid>"
                   :on-change   #(reset! selected-host-id (.. % -target -value))}]]
       (when @loading-hosts?
         [ui/Loader {:active true
                     :inline "centered"
                     :size   "small"}])
       (when @host-load-error
         [ui/Message {:warning true}
          @host-load-error])
       (when (seq @host-options)
         [:div {:style {:margin-bottom "1rem"}}
          [:div {:style {:font-weight   600
                         :margin-bottom "0.5rem"}}
           "Discovered MEC hosts"]
          [:div {:style {:display   "flex"
                         :gap       "0.5rem"
                         :flex-wrap "wrap"}}
           (for [{:keys [key value text]} @host-options]
             ^{:key key}
             [ui/Label {:as       "a"
                        :basic    true
                        :color    (when (= value @selected-host-id) "blue")
                        :on-click #(reset! selected-host-id value)}
              text])]])
       [ui/Message {:info true}
        "Host selection is optional, but choosing one now avoids host resolution conflicts when multiple eligible MEPMs exist."]]
    [ui/ModalActions {:style {:margin-top "0.75rem"}}
      [uix/Button
       {:text     "Cancel"
        :disabled @submitting?
        :on-click #(reset-form! state)}]
      [uix/Button
       {:text     "Create app instance"
        :primary  true
        :loading  @submitting?
        :disabled (or disabled? @submitting?)
        :on-click #(create-app-instance! module-id @selected-host-id open? submitting?)}]]]]))

