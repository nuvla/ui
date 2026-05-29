(ns sixsq.nuvla.ui.pages.deployments-detail.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.common-components.acl.views :as acl]
            [sixsq.nuvla.ui.common-components.deployment-dialog.events :as deployment-dialog-events]
            [sixsq.nuvla.ui.common-components.deployment-dialog.views :as deployment-dialog-views]
            [sixsq.nuvla.ui.common-components.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.common-components.job.subs :as job-subs]
            [sixsq.nuvla.ui.common-components.job.views :as job-views]
            [sixsq.nuvla.ui.common-components.plugins.audit-log :as audit-log-plugin]
            [sixsq.nuvla.ui.common-components.plugins.module :as module-plugin]
            [sixsq.nuvla.ui.common-components.plugins.nav-tab :as tab-plugin]
            [sixsq.nuvla.ui.common-components.resource-log.views :as log-views]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.main.events :as main-events]
            [sixsq.nuvla.ui.pages.apps.views-versions :as views-versions]
            [sixsq.nuvla.ui.pages.deployments-detail.events :as events]
            [sixsq.nuvla.ui.pages.deployments-detail.spec :as spec]
            [sixsq.nuvla.ui.pages.deployments-detail.subs :as subs]
            [sixsq.nuvla.ui.pages.deployments-detail.views-coe-resources-docker :as coe-resources-docker]
            [sixsq.nuvla.ui.pages.deployments-detail.views-coe-resources-k8s :as coe-resources-k8s]
            [sixsq.nuvla.ui.pages.deployments.utils :as deployments-utils]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.utils :refer [name->href]]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.spec :as spec-utils]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
            [sixsq.nuvla.ui.utils.values :as values]
            [sixsq.nuvla.ui.utils.view-components :as vc]))


(def refresh-action-id :deployment-get-deployment)

(defn refresh
  [resource-id]
  (dispatch [::main-events/action-interval-start
             {:id        refresh-action-id
              :frequency 10000
              :event     [::events/get-deployment resource-id]}]))


(defn sum-replicas
  [parameters ends-with]
  (->> (vals parameters)
       (filter #(str/ends-with? (:name %) ends-with))
       (map #(js/parseInt (:value %)))
       (reduce +)))


(defn sum-running-replicas
  [parameters]
  (sum-replicas parameters "replicas.running"))


(defn sum-desired-replicas
  [parameters]
  (sum-replicas parameters "replicas.desired"))


(defn ProgressDeployment
  []
  (let [{:keys [state]} @(subscribe [::subs/deployment])
        parameters (subscribe [::subs/deployment-parameters])
        running    (sum-running-replicas @parameters)
        desired    (sum-desired-replicas @parameters)]
    (when (and (deployments-utils/started? state) running desired (not= running desired))
      [ui/Segment
       [ui/Progress {:label    "deployment: started (replicas: running/required)"
                     :total    desired
                     :value    running
                     :progress "ratio"
                     :size     "small"
                     :class    ["green"]}]])))


(defn ProgressBars
  []
  (let [{:keys [state]} @(subscribe [::subs/deployment])]
    [:<>
     [job-views/ProgressJobAction state]
     [ProgressDeployment]]))


(defn url-to-row
  [url-name url-pattern]
  (let [url (subscribe [::subs/url url-pattern])
        {:keys [state]} @(subscribe [::subs/deployment])]
    [ui/TableRow
     [ui/TableCell url-name]
     [ui/TableCell
      (cond
        (and @url (deployments-utils/started? state))
        [uix/CopyToClipboard
         {:content   [:a {:href @url, :target "_blank"} @url false]
          :value     @url
          :on-hover? true}]

        @url @url
        :else url-pattern)]]))

(defn url-to-button
  [{:keys [url-name url-pattern primary?]
    :or   {primary? false}}]
  (let [url (subscribe [::subs/url url-pattern])]
    (when @url
      [ui/Button {:color    (if primary? "green" nil)
                  :icon     "external"
                  :content  url-name
                  :href     @url
                  :on-click (fn [event]
                              (js/window.open @url)
                              (.stopPropagation event)
                              (.preventDefault event))
                  :target   "_blank"
                  :rel      "noreferrer"
                  :style    {:margin 2}}])))


(defn urls-section
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        module-content (subscribe [::subs/deployment-module-content])
        urls           (get @module-content :urls)
        url-count      (count urls)]
    {:menuItem {:content (r/as-element [:span (@tr [:url])
                                        (when (> url-count 0)
                                          [ui/Label {:circular true
                                                     :size     "mini"
                                                     :attached "top right"}
                                           url-count])])
                :key     :urls
                :icon    icons/i-link}
     :render   #(r/as-element
                  (if (empty? urls)
                    [uix/MsgNoItemsToShow (@tr [:no-urls])]
                    [ui/TabPane
                     [ui/Table {:basic   "very"
                                :columns 2}
                      [ui/TableHeader
                       [ui/TableRow
                        [ui/TableHeaderCell [:span (@tr [:name])]]
                        [ui/TableHeaderCell [:span (@tr [:url])]]]]
                      [ui/TableBody
                       (for [[url-name url-pattern] urls]
                         ^{:key url-name}
                         [url-to-row url-name url-pattern])]]]))}))


(defn module-version-section
  []
  (let [tr                (subscribe [::i18n-subs/tr])
        module-versions   (subscribe [::subs/module-versions])
        module-content-id (subscribe [::subs/current-module-content-id])]
    {:menuItem {:content (r/as-element [:span (@tr [:module-version])])
                :key     :versions
                :icon    icons/i-layer-group}
     :render   #(r/as-element
                  [ui/TabPane [views-versions/versions-table
                               module-versions module-content-id]])}))


(defn item-to-row
  [{name :name value :value description :description}]
  [ui/TableRow
   [ui/TableCell
    (if (some? description)
      [ui/Popup
       (cond-> {:content (r/as-element [:p description])
                :trigger (r/as-element [:p name " " [icons/InfoIconFull]])})]
      name)]
   [ui/TableCell
    [uix/CopyToClipboard
     {:value     value
      :on-hover? true}]]])


(defn list-section
  [items section-key section-name]
  (let [key->icon   {:parameters icons/i-sliders
                     :env-vars   icons/i-gear}
        tr          (subscribe [::i18n-subs/tr])
        items-count (count items)]
    {:menuItem {:content (r/as-element
                           [:span (@tr [section-name])
                            (when (> items-count 0)
                              [ui/Label {:circular true
                                         :size     "mini"
                                         :attached "top right"}
                               items-count])])
                :key     section-key
                :icon    (key->icon section-key)}
     :render   #(r/as-element
                  (if (empty? items)
                    [uix/MsgNoItemsToShow]
                    [ui/TabPane
                     [ui/Table {:basic   "very"
                                :columns 2}
                      [ui/TableHeader
                       [ui/TableRow
                        [ui/TableHeaderCell [:span (@tr [:name])]]
                        [ui/TableHeaderCell [:span (@tr [:value])]]]]
                      (when-not (empty? items)
                        [ui/TableBody
                         (for [{name :name :as item} items]
                           ^{:key name}
                           [item-to-row item])])]]))}))


(defn parameters-section
  []
  (let [deployment-parameters (subscribe [::subs/deployment-parameters])
        params                (vals @deployment-parameters)]
    (list-section params :parameters :module-output-parameters)))


(defn env-vars-section
  []
  (let [module-content (subscribe [::subs/deployment-module-content])
        env-vars       (get @module-content :environmental-variables [])]
    (list-section env-vars :env-vars :env-variables)))

(defn docker-resources-section
  []
  (let [coe-res-docker-available? @(subscribe [::subs/coe-resource-docker-available?])]
    (when coe-res-docker-available?
      {:menuItem {:content "Docker"
                  :key     :docker
                  :icon    icons/i-docker}
       :render   #(r/as-element [coe-resources-docker/Tab])})))

(defn k8s-resources-section
  []
  (let [coe-res-k8s-available? @(subscribe [::subs/coe-resource-k8s-available?])]
    (when coe-res-k8s-available?
      {:menuItem {:content "Kubernetes"
                  :key     :k8s
                  :icon    (r/as-element [ui/Image {:src   "/ui/images/kubernetes-grey.svg"
                                                    :style {:width  "16px"
                                                            :margin "0 .35714286em 0 0"}}])}
       :render   #(r/as-element [coe-resources-k8s/Tab])})))

(defn logs-section
  []
  (let [tr         (subscribe [::i18n-subs/tr])
        deployment (subscribe [::subs/deployment])]
    {:menuItem {:content (r/as-element [:span (str/capitalize (@tr [:logs]))])
                :key     :logs
                :icon    icons/i-file-code}
     :render   (fn [] (r/as-element
                        [log-views/TabLogs
                         (:id @deployment)
                         #(subscribe [::subs/deployment-services-list])
                         true]))}))



(defn action-button
  [{:keys [label? menu-item? icon-name button-text on-click disabled? popup-text]
    :or   {disabled? false on-click identity}}]
  (let [button (cond
                 label? [ui/Label {:corner   true
                                   :size     "small"
                                   :on-click on-click}
                         [ui/Icon {:class icon-name
                                   :style {:cursor "pointer"}
                                   :color "red"}]]
                 menu-item? [ui/MenuItem
                             {:on-click on-click
                              :disabled disabled?}
                             [ui/Icon {:class icon-name}]
                             button-text]
                 :else [ui/Icon {:class    icon-name
                                 :style    {:cursor "pointer"}
                                 :color    "red"
                                 :on-click on-click}])]
    (if popup-text
      [ui/Popup
       (cond-> {:header            (str/capitalize button-text)
                :content           popup-text
                :mouse-enter-delay 500
                :trigger           (r/as-element button)}
               (and (not menu-item?) (not label?)) (assoc :position "bottom center"))]
      button)))

(defn mec-target-state
  [latest-mec-job]
  (or (get-in latest-mec-job [:mec-request-params :changeStateTo])
      (get-in latest-mec-job [:mec-request-params "changeStateTo"])))

(defn MecActionButton
  [{:keys [deployment-id action button-text icon-name popup-text body success-header success-content disabled?]}]
  (action-button {:menu-item?  true
                  :icon-name   icon-name
                  :button-text button-text
                  :popup-text  popup-text
                  :disabled?   disabled?
                  :on-click    #(dispatch [::events/run-mec-lifecycle
                                           deployment-id
                                           action
                                           {:body            body
                                            :success-header  success-header
                                            :success-content success-content}])}))


(defn StopButton
  [_deployment & _opts]
  (let [tr              (subscribe [::i18n-subs/tr])
        compose?        (subscribe [::subs/is-deployment-docker-compose?])
        initial-state   {:open?           false
                         :checked?        false
                         :remove-images?  false
                         :remove-volumes? false}
        !state          (r/atom initial-state)
        open?           (r/cursor !state [:open?])
        checked?        (r/cursor !state [:checked?])
        remove-images?  (r/cursor !state [:remove-images?])
        remove-volumes? (r/cursor !state [:remove-volumes?])
        icon-name       icons/i-stop]
    (fn [deployment & {:keys [label?, menu-item?], :or {label? false, menu-item? false}}]
      (let [{:keys [id name description module]} deployment
            text1  (str (or name id) (when description " - ") description)
            text2  (str (@tr [:created-from-module]) (or (:name module) (:id module)))
            button (action-button
                     {:label?      label?
                      :menu-item?  menu-item?
                      :on-click    (fn [event]
                                     (reset! open? true)
                                     (.stopPropagation event)
                                     (.preventDefault event))
                      :disabled?   (not (general-utils/can-operation? "stop" deployment))
                      :icon-name   icon-name
                      :button-text (@tr [:stop])
                      :popup-text  (@tr [:deployment-stop-msg])})]
        ^{:key (random-uuid)}
        [uix/ModalDanger
         {:on-close           (fn [event]
                                (reset! !state initial-state)
                                (.stopPropagation event)
                                (.preventDefault event))
          :on-confirm         #(do
                                 (dispatch [::events/stop-deployment id
                                            {:remove-images  @remove-images?
                                             :remove-volumes @remove-volumes?}])
                                 (reset! !state initial-state))
          :open               @open?
          :control-confirmed? checked?
          :trigger            (r/as-element button)
          :content            [:<>
                               [:h3 text1]
                               [:p text2]
                               (when @compose?
                                 [ui/Segment
                                  [ui/Form {:warning true}
                                   [uix/MsgWarn {:size    :small
                                                 :content (@tr [:stop-deployment-remove-opts-require-ne-2.19])}]
                                   [ui/FormCheckbox {:label     (@tr [:stop-deployment-remove-images])
                                                     :checked   @remove-images?
                                                     :on-change (ui-callback/checked #(swap! remove-images? not))}]
                                   [ui/FormCheckbox {:label     (@tr [:stop-deployment-remove-volumes])
                                                     :checked   @remove-volumes?
                                                     :on-change (ui-callback/checked #(swap! remove-volumes? not))}]]])]
          :header             (@tr [:stop-deployment])
          :danger-msg         (@tr [:deployment-stop-msg])
          :button-text        (@tr [:stop])}]))))


(defn DeleteButton
  [_deployment & _opts]
  (let [tr        (subscribe [::i18n-subs/tr])
        open?     (r/atom false)
        icon-name icons/i-trash]
    (fn [deployment & {:keys [label?, menu-item?], :or {label? false, menu-item? false}}]
      (let [{:keys [id name description module]} deployment
            text-1 (str (or name id) (when description " - ") description)
            text-2 (str (@tr [:created-from-module]) (or (:name module) (:id module)))
            button (action-button
                     {:on-click    (fn [event]
                                     (reset! open? true)
                                     (.stopPropagation event)
                                     (.preventDefault event))
                      :button-text (@tr [:delete])
                      :popup-text  (@tr [:deployment-delete-msg])
                      :icon-name   icon-name
                      :label?      label?
                      :menu-item?  menu-item?
                      :disabled?   (not (general-utils/can-delete? deployment))})]
        ^{:key (random-uuid)}
        [uix/ModalDanger
         {:with-confirm-step? true
          :on-close           (fn [event]
                                (reset! open? false)
                                (.stopPropagation event)
                                (.preventDefault event))
          :on-confirm         #(dispatch [::events/delete id])
          :open               @open?
          :trigger            (r/as-element button)
          :content            [:<> [:h3 text-1] [:p text-2]]
          :header             [:span [icons/RocketIcon]
                               (@tr [:delete-deployment])]
          :danger-msg         (@tr [:deployment-delete-msg])
          :button-text        (str (str/capitalize (@tr [:delete])) " " (str/capitalize (@tr [:deployment])))
          :header-class       [:nuvla-deployments :delete-modal-header]}]))))


(defn CloneButton
  [{:keys [id data] :as deployment}]
  (let [tr         (subscribe [::i18n-subs/tr])
        first-step (if data :data :infra-services)
        button     (action-button
                     {:menu-item?  true
                      :button-text (@tr [:clone])
                      :icon-name   icons/i-clone
                      :popup-text  (@tr [:deployment-clone-msg])
                      :on-click    #(dispatch [::deployment-dialog-events/create-deployment
                                               id first-step])
                      :disabled?   (not (general-utils/can-operation? "clone" deployment))})]
    [:<>
     [deployment-dialog-views/deploy-modal]
     button]))

(defn DetachButton
  [_deployment]
  (let [tr    (subscribe [::i18n-subs/tr])
        open? (r/atom false)]
    (fn [{:keys [id name description] :as deployment}]
      (when (general-utils/can-operation? :detach deployment)
        (let [text-1 (str (or name id) (when description " - ") description)
              button (action-button
                       {:on-click    (fn [event]
                                       (reset! open? true)
                                       (.stopPropagation event)
                                       (.preventDefault event))
                        :button-text (@tr [:detach])
                        :popup-text  (@tr [:deployment-detach-msg])
                        :icon-name   "times circle outline"
                        :menu-item?  true})]
          [uix/ModalDanger
           {:on-close    (fn [event]
                           (reset! open? false)
                           (.stopPropagation event)
                           (.preventDefault event))
            :on-confirm  #(do
                            (dispatch [::events/detach id])
                            (reset! open? false))
            :open        @open?
            :trigger     (r/as-element button)
            :content     [:h3 text-1]
            :header      (@tr [:detach-deployment])
            :danger-msg  (@tr [:deployment-detach-msg])
            :button-text (@tr [:detach])}])))))

(defn StartUpdateButton
  [{:keys [data state] :as deployment}]
  (let [tr         (subscribe [::i18n-subs/tr])
        start      (#{deployments-utils/CREATED deployments-utils/STOPPED} state)
        first-step (if data :data :infra-services)
        button     (action-button
                     {:button-text (if start
                                     (@tr [:start])
                                     (@tr [:update]))
                      :popup-text  (@tr [(if start :deployment-start-msg
                                                   :deployment-update-msg)])
                      :icon-name   (if start icons/i-play icons/i-redo)
                      :menu-item?  true
                      :disabled?   (if start
                                     (not (general-utils/can-operation? "start" deployment))
                                     (not (general-utils/can-operation? "update" deployment)))
                      :on-click    #(dispatch [::deployment-dialog-events/open-deployment-modal
                                               first-step deployment])})]
    [:<>
     [deployment-dialog-views/deploy-modal]
     button]))


(defn vpn-info
  []
  (let [{:keys [state module]} @(subscribe [::subs/deployment])
        {module-content :content} module
        [_ url] (-> module-content (get :urls []) first)
        tr          (subscribe [::i18n-subs/tr])
        primary-url (subscribe [::subs/url url])
        parameters  (subscribe [::subs/deployment-parameters])
        started?    (deployments-utils/started? state)
        hostname    (or (get-in @parameters ["hostname" :value]) "")]
    (when (and started? @primary-url (spec-utils/private-ipv4? hostname))
      [ui/Message {:info true}
       [ui/MessageHeader (@tr [:vpn-information])]
       [ui/MessageContent
        (@tr [:deployment-run-private-ip]) ". "
        [:br]
        (@tr [:deployment-access-url]) " "
        [:a {:href (name->href routes/credentials)}
         (@tr [:create-vpn-credential])] " " (@tr [:and]) " "
        [:a {:href "https://docs.nuvla.io/nuvla/user-guide/vpn" :target "_blank"} (@tr [:connect-vpn])] "."]])))


(defn up-to-date?
  [v versions]
  (when v
    (let [tr           (subscribe [::i18n-subs/tr])
          last-version (ffirst versions)]
      (if (= v last-version)
        [:span {:style {:margin-left 5}} [icons/CheckIconFull {:color "green"}] " (" (@tr [:up-to-date-latest]) ")"]
        [:span {:style {:margin-left 5}} [icons/WarningIcon {:color "orange"}]
         (str (@tr [:behind-version-1]) " " (- last-version v) " " (@tr [:behind-version-2]))]))))

(defn MecStatusRows
  []
  (let [mec-app-instance-id           (subscribe [::subs/mec-app-instance-id])
        mec-backing-deployment-id     (subscribe [::subs/mec-backing-deployment-id])
        mec-southbound-app-instance-id (subscribe [::subs/mec-southbound-app-instance-id])
        mec-instantiation-state       (subscribe [::subs/mec-instantiation-state])
        mec-operational-state         (subscribe [::subs/mec-operational-state])
        mec-backend-mode              (subscribe [::subs/mec-backend-mode])
        mepm-id                       (subscribe [::subs/mepm-id])
        mepm-endpoint                 (subscribe [::subs/mepm-endpoint])
        mec-host-id                   (subscribe [::subs/mec-host-id])
        latest-mec-job                (subscribe [::subs/latest-mec-job])]
    [:<>
     [ui/TableRow
      [ui/TableCell "MEC instantiation state"]
      [ui/TableCell @mec-instantiation-state]]
     [ui/TableRow
      [ui/TableCell "MEC operational state"]
      [ui/TableCell (or @mec-operational-state "N/A")]]
     [ui/TableRow
      [ui/TableCell "MEC app instance"]
      [ui/TableCell [values/AsLink @mec-app-instance-id :label (general-utils/id->uuid @mec-app-instance-id)]]]
     (when @mec-backing-deployment-id
       [ui/TableRow
        [ui/TableCell "Backing deployment"]
        [ui/TableCell [values/AsPageLink @mec-backing-deployment-id
                       :label (general-utils/id->short-uuid @mec-backing-deployment-id)]]])
     (when (and @mec-southbound-app-instance-id
                (not= @mec-southbound-app-instance-id @mec-backing-deployment-id))
       [ui/TableRow
        [ui/TableCell "MEPM app instance"]
        [ui/TableCell @mec-southbound-app-instance-id]])
     (when @mec-backend-mode
       [ui/TableRow
        [ui/TableCell "MEC backend mode"]
        [ui/TableCell @mec-backend-mode]])
     (when @mec-host-id
       [ui/TableRow
        [ui/TableCell "MEC host"]
        [ui/TableCell [deployments-utils/CloudNuvlaEdgeLink {:nuvlabox @mec-host-id}]]])
     (when @mepm-id
       [ui/TableRow
        [ui/TableCell "MEPM"]
        [ui/TableCell [values/AsLink @mepm-id :label (general-utils/id->short-uuid @mepm-id)]]])
     (when @mepm-endpoint
       [ui/TableRow
        [ui/TableCell "MEPM endpoint"]
        [ui/TableCell @mepm-endpoint]])
     (when @latest-mec-job
       [ui/TableRow
        [ui/TableCell "Latest MEC operation"]
        [ui/TableCell
         (str (:mec-operation-type @latest-mec-job)
              (when-let [target-state (mec-target-state @latest-mec-job)]
                (str " -> " target-state))
              " (" (:state @latest-mec-job) ")")]])]))

(defn mec-operation-state-color
  [state]
  (case state
    "COMPLETED" "green"
    "FAILED" "red"
    "FAILED_TEMP" "orange"
    "ROLLED_BACK" "orange"
    "PROCESSING" "blue"
    "STARTING" "blue"
    nil))

(defn LifecycleOperationRow
  [{:keys [id operation-type operation-state started-at state-entered-at app-instance-id error-detail]}]
  (let [latest-mec-operation-id (subscribe [::subs/latest-mec-operation-id])]
    [ui/TableRow {:positive (= id @latest-mec-operation-id)
                  :error    (contains? #{"FAILED" "FAILED_TEMP"} operation-state)}
     [ui/TableCell [values/AsLink id :label (general-utils/id->short-uuid id)]]
     [ui/TableCell operation-type]
     [ui/TableCell [ui/Label {:basic true
                              :color (mec-operation-state-color operation-state)}
                    operation-state]]
     [ui/TableCell [uix/TimeAgo started-at]]
     [ui/TableCell [uix/TimeAgo state-entered-at]]
     [ui/TableCell [values/AsPageLink app-instance-id
                    :label (general-utils/id->uuid app-instance-id)]]
     [ui/TableCell
      (when error-detail
        [:div {:style {:max-width    "24rem"
                       :white-space  "pre-wrap"
                       :overflow-wrap "anywhere"}}
         error-detail])]]))

(defn lifecycle-section
  []
  (let [mec-enabled-deployment? (subscribe [::subs/mec-enabled-deployment?])
        operations              (subscribe [::subs/mec-operation-occs])]
    (when @mec-enabled-deployment?
      {:menuItem {:content (r/as-element
                            [:span "Lifecycle"
                             (when (pos? (count @operations))
                               [ui/Label {:circular true
                                          :size     "mini"
                                          :attached "top right"}
                                (count @operations)])])
                  :key     :lifecycle
                  :icon    icons/i-bolt}
       :render   #(r/as-element
                    (if (empty? @operations)
                      [uix/MsgNoItemsToShow "No MEC lifecycle operations yet."]
                      [ui/TabPane
                       [ui/Table {:basic "very"
                                  :compact "very"}
                        [ui/TableHeader
                         [ui/TableRow
                          [ui/TableHeaderCell "Operation ID"]
                          [ui/TableHeaderCell "Type"]
                          [ui/TableHeaderCell "State"]
                          [ui/TableHeaderCell "Start time"]
                          [ui/TableHeaderCell "State entered"]
                          [ui/TableHeaderCell "App instance"]
                          [ui/TableHeaderCell "Error detail"]]]
                        [ui/TableBody
                         (for [operation @operations]
                           ^{:key (:id operation)}
                           [LifecycleOperationRow operation])]]]))})))


(defn TabOverviewModule
  []
  (let [tr         (subscribe [::i18n-subs/tr])
        deployment (subscribe [::subs/deployment])
        module     (:module @deployment)
        {:keys [id created updated name description
                parent-path path logo-url]} module
        desc-short (values/markdown->summary description)]
    [ui/Segment {:secondary true}
     [ui/Segment (merge style/basic {:floated "right"})
      [ui/Image {:src      (or logo-url "")
                 :bordered true
                 :style    {:width      "auto"
                            :height     "100px"
                            :object-fit "contain"}}]]
     [:h4 {:style {:margin-top 0}} (str/capitalize (@tr [:app]))]
     [ui/Table {:basic  "very"
                :padded false}
      [ui/TableBody
       (when name
         [ui/TableRow
          [ui/TableCell (str/capitalize (@tr [:name]))]
          [ui/TableCell [values/AsLink path :label name :page "apps"]]])
       (when desc-short
         [ui/TableRow
          [ui/TableCell (str/capitalize (@tr [:description]))]
          [ui/TableCell desc-short]])
       (when parent-path
         [ui/TableRow
          [ui/TableCell (str/capitalize (@tr [:project]))]
          [ui/TableCell [values/AsLink parent-path :label parent-path :page "apps"]]])
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:created]))]
        [ui/TableCell [uix/TimeAgo created]]]
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:updated]))]
        [ui/TableCell [uix/TimeAgo updated]]]
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:id]))]
        [ui/TableCell [values/AsLink id :label (general-utils/id->uuid
                                                 (or id ""))]]]]]]))

(defn DeplSetLink
  [depl-set-id depl-set-name]
  (when depl-set-id
    (let [href (name->href routes/deployment-groups-details
                           {:uuid (general-utils/id->uuid depl-set-id)})]
      [:a {:href     href
           :on-click (partial uix/link-on-click href)}
       [ui/Icon {:name "bullseye"}]
       depl-set-name])))


(defn TabOverviewSummary
  []
  (let [tr              (subscribe [::i18n-subs/tr])
        deployment      (subscribe [::subs/deployment])
        mec-enabled?    (subscribe [::subs/mec-enabled-deployment?])
        version         (subscribe [::subs/current-module-version])
        versions        (subscribe [::subs/module-versions])
        nuvlabox        (subscribe [::subs/nuvlabox])
        {:keys [id state module tags acl owner created-by
                deployment-set deployment-set-name]} @deployment
        owners          (:owners acl)
        resolved-owners (subscribe [::session-subs/resolve-users owners])
        urls            (get-in module [:content :urls])]

    [ui/SegmentGroup {:style {:display    "flex", :justify-content "space-between",
                              :background "#f3f4f5"}}
     [ui/Segment {:secondary true}

      [:h4 {:style {:margin-top 0}} (str/capitalize (@tr [:summary]))]

      [ui/Table {:basic "very"}
       [ui/TableBody
        (when tags
          [ui/TableRow
           [ui/TableCell (str/capitalize (@tr [:tags]))]
           [ui/TableCell
            [uix/Tags tags]]])
        [ui/TableRow
         [ui/TableCell (str/capitalize (str (@tr [:created])))]
         [ui/TableCell [uix/TimeAgo (:created @deployment)]]]
        [ui/TableRow
         [ui/TableCell "Id"]
         [ui/TableCell (when (some? id) [values/AsLink id :label (general-utils/id->uuid id)])]]
        [ui/TableRow
         [ui/TableCell (str/capitalize (@tr [:owner]))]
         [ui/TableCell
          (cond
            owner @(subscribe [::session-subs/resolve-user owner])
            (seq owners) (str/join ", " @resolved-owners))]]
        (when (and created-by (str/starts-with? owner "group/"))
          [ui/TableRow
           [ui/TableCell (str/capitalize (@tr [:created-by]))]
           [ui/TableCell @(subscribe [::session-subs/resolve-user created-by])]])
        [ui/TableRow
         [ui/TableCell (str/capitalize (@tr [:status]))]
         [ui/TableCell state
          " "
          (when (deployments-utils/deployment-in-transition? state)
            [ui/Icon {:loading true :name "circle notch" :color "grey"}])]]
        [ui/TableRow
         [ui/TableCell (@tr [:infrastructure])]
         [ui/TableCell
          [deployments-utils/CloudNuvlaEdgeLink @deployment
           :color (when @nuvlabox (vc/status->color (:online @nuvlabox)))]]]
        [ui/TableRow
         [ui/TableCell (str/capitalize (@tr [:app-version]))]
         [ui/TableCell
          [module-plugin/LinkToAppView {:path (:path module) :version-id @version} @version]
          (up-to-date? @version @versions)]]
        (when @mec-enabled?
          [MecStatusRows])
        (when deployment-set
          [ui/TableRow
           [ui/TableCell (str/capitalize (@tr [:deployment-group]))]
           [ui/TableCell
            [DeplSetLink deployment-set deployment-set-name]]])]]]
     (when (deployments-utils/started? state)
       [ui/Segment {:attached  false
                    :secondary true}
        (for [[i [url-name url-pattern]] (map-indexed list urls)]
          ^{:key url-name}
          [url-to-button {:url-name url-name :url-pattern url-pattern :primary? (zero? i)}])])]))


(defn OverviewPane
  []
  [ui/TabPane
   [ui/Grid {:columns   2,
             :stackable true
             :padded    true}
    [ui/GridRow
     [ui/GridColumn {:stretched true}
      [TabOverviewSummary]]
     [ui/GridColumn {:stretched true}
      [TabOverviewModule]]]]])


(defn overview
  []
  {:menuItem {:content (r/as-element [:span "Overview"])
              :key     :overview
              :icon    icons/i-eye}
   :render   #(r/as-element [OverviewPane])})


(defn MenuBar
  [{:keys [id] :as deployment}]
  (let [loading?                 (subscribe [::subs/loading?])
        mec-enabled-deployment?  (subscribe [::subs/mec-enabled-deployment?])
        mec-instantiation-state  (subscribe [::subs/mec-instantiation-state])
        mec-operational-state    (subscribe [::subs/mec-operational-state])]
    [components/StickyBar
     [ui/Menu {:borderless true}
      (if @mec-enabled-deployment?
        [:<>
         (when (and (= @mec-instantiation-state "NOT_INSTANTIATED")
                    (not (deployments-utils/deployment-in-transition? (:state deployment))))
           [MecActionButton {:deployment-id    id
                             :action           "instantiate"
                             :button-text      "Instantiate"
                             :icon-name        icons/i-play
                             :popup-text       "Instantiate this ETSI MEC application instance."
                             :success-header   "MEC instantiate started"
                             :success-content  "The MEC instantiate request was accepted."}])
         (when (and (= @mec-instantiation-state "INSTANTIATED")
                    (= @mec-operational-state "STOPPED")
                    (not (deployments-utils/deployment-in-transition? (:state deployment))))
           [MecActionButton {:deployment-id    id
                             :action           "operate"
                             :body             {:changeStateTo "STARTED"}
                             :button-text      "Start"
                             :icon-name        icons/i-play
                             :popup-text       "Start the instantiated ETSI MEC application."
                             :success-header   "MEC start started"
                             :success-content  "The MEC operate STARTED request was accepted."}])
         (when (and (= @mec-instantiation-state "INSTANTIATED")
                    (= @mec-operational-state "STARTED")
                    (not (deployments-utils/deployment-in-transition? (:state deployment))))
           [MecActionButton {:deployment-id    id
                             :action           "operate"
                             :body             {:changeStateTo "STOPPED"}
                             :button-text      "Stop"
                             :icon-name        icons/i-stop
                             :popup-text       "Stop the ETSI MEC application without terminating it."
                             :success-header   "MEC stop started"
                             :success-content  "The MEC operate STOPPED request was accepted."}])
         (when (and (= @mec-instantiation-state "INSTANTIATED")
                    (not (deployments-utils/deployment-in-transition? (:state deployment))))
           [MecActionButton {:deployment-id    id
                             :action           "terminate"
                             :button-text      "Terminate"
                             :icon-name        icons/i-power
                             :popup-text       "Terminate this ETSI MEC application instance."
                             :success-header   "MEC terminate started"
                             :success-content  "The MEC terminate request was accepted."}])]
        [:<>
         [StartUpdateButton deployment]
         [StopButton deployment :menu-item? true]
         [CloneButton deployment]
         [DeleteButton deployment :menu-item? true]
         [DetachButton deployment]])
      [components/RefreshMenu
       {:action-id  refresh-action-id
        :loading?   @loading?
        :on-refresh #(refresh id)}]]]))


(defn deployment-detail-panes
  []
  (let [deployment (subscribe [::subs/deployment])
        read-only? (subscribe [::subs/is-read-only?])]
    [(overview)
     (urls-section)
     (module-version-section)
     (lifecycle-section)
     (logs-section)
     (when @deployment
       (audit-log-plugin/events-section
         {:db-path [::spec/events]
          :filters {:href (:id @deployment)}}))
     (parameters-section)
     (env-vars-section)
     (docker-resources-section)
     (k8s-resources-section)
     (job-views/jobs-section)
     (acl/TabAcls {:e          deployment
                   :can-edit?  (not @read-only?)
                   :edit-event ::events/edit})]))


(defn PageHeader
  []
  (let [deployment (subscribe [::subs/deployment])]
    (fn []
      (let [module-name (get-in @deployment [:module :name] "")]
        [:div
         [:h2 {:style {:margin "0 0 0 0"}}
          [icons/RocketIcon]
          module-name]]))))


(defn DeploymentDetails
  [{{uuid :uuid} :path-params}]
  (let [deployment (subscribe [::subs/deployment])]
    (refresh (str "deployment/" uuid))
    (fn [_]
      (let [panes (deployment-detail-panes)]
        [components/LoadingPage {:dimmable? true}
         [:<>
          [components/NotFoundPortal
           ::subs/not-found?
           :no-deployment-message-header
           :no-deployment-message-content]
          [PageHeader]
          [MenuBar @deployment]
          [job-views/ErrorJobsMessage ::job-subs/jobs
           nil nil #(dispatch [::tab-plugin/change-tab {:db-path [::spec/tab] :tab-key :jobs}])]
          [ProgressBars]
          [vpn-info]
          [tab-plugin/Tab
           {:db-path [::spec/tab]
            :menu    {:secondary true
                      :pointing  true
                      :style     {:display        "flex"
                                  :flex-direction "row"
                                  :flex-wrap      "wrap"}}
            :panes   panes}]]]))))
