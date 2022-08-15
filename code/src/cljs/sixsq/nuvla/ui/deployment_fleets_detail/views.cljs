(ns sixsq.nuvla.ui.deployment-fleets-detail.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.acl.views :as acl]
    [sixsq.nuvla.ui.cimi-detail.views :as cimi-detail-views]
    [sixsq.nuvla.ui.config :as config]
    [sixsq.nuvla.ui.deployments.subs :as deployments-subs]
    [sixsq.nuvla.ui.deployments.views :as deployments-views]
    [sixsq.nuvla.ui.edges-detail.events :as edges-detail-events]
    [sixsq.nuvla.ui.deployment-fleets-detail.events :as events]
    [sixsq.nuvla.ui.edges-detail.subs :as edges-detail-subs]
    [sixsq.nuvla.ui.deployment-fleets-detail.subs :as subs]
    [sixsq.nuvla.ui.edges.events :as edges-events]
    [sixsq.nuvla.ui.edges.subs :as edges-subs]
    [sixsq.nuvla.ui.edges.utils :as utils]
    [sixsq.nuvla.ui.history.views :as history-views]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.job.subs :as job-subs]
    [sixsq.nuvla.ui.job.views :as job-views]
    [sixsq.nuvla.ui.main.components :as components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.resource-log.views :as log-views]
    [sixsq.nuvla.ui.session.subs :as session-subs]
    [sixsq.nuvla.ui.utils.form-fields :as ff]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.map :as map]
    [sixsq.nuvla.ui.utils.plot :as plot]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.tab :as tab]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as values]))


(def refresh-action-id :deployment-fleet-get-deployment-fleet)

(def orchestration-icons
  {:swarm      "docker"
   :kubernetes "/ui/images/kubernetes.svg"})


(defn refresh
  [uuid]
  (dispatch [::main-events/action-interval-start
             {:id        refresh-action-id
              :frequency 10000
              :event     [::events/get-deployment-fleet (str "deployment-fleet/" uuid)]}]))

(defn DecommissionButton
  [nuvlabox]
  (let [tr      (subscribe [::i18n-subs/tr])
        {:keys [id name description]} nuvlabox
        content (str (or name id) (when description " - ") description)]
    [uix/ModalDanger
     {:button-text (@tr [:decommission])
      :on-confirm  #(dispatch [::edges-detail-events/decommission])
      :danger-msg  (@tr [:nuvlabox-decommission-warning])
      :trigger     (r/as-element [ui/MenuItem
                                  [ui/Icon {:name "eraser"}]
                                  (@tr [:decommission])])
      :header      (@tr [:decommission-nuvlabox])
      :content     [:h3 content]}]))


(defn DeleteButton
  [nuvlabox]
  (let [tr      (subscribe [::i18n-subs/tr])
        {:keys [id name description]} nuvlabox
        content (str (or name id) (when description " - ") description)]
    [uix/ModalDanger
     {:button-text (@tr [:delete])
      :on-confirm  #(dispatch [::edges-detail-events/delete])
      :trigger     (r/as-element [ui/MenuItem
                                  [ui/Icon {:name "trash"}]
                                  (@tr [:delete])])
      :header      (@tr [:delete-nuvlabox])
      :content     [:h3 content]}]))


(defn SshKeysDropdown
  [operation _on-change-fn]
  (let [tr       (subscribe [::i18n-subs/tr])
        is-add?  (= operation "add-ssh-key")
        ssh-keys (if is-add?
                   (r/atom nil)
                   (subscribe [::edges-detail-subs/nuvlabox-associated-ssh-keys]))]
    (when is-add?
      (dispatch [::edges-detail-events/get-ssh-keys-not-associated #(reset! ssh-keys %)]))
    (fn [_operation on-change-fn]
      (if (nil? @ssh-keys)
        [ui/Message {:info true} (@tr [:no-credentials-to-remove])]
        [ui/FormDropdown
         {:label       "SSH key"
          :placeholder (if is-add?
                         (@tr [:leave-empty-to-generate-ssh-keypair])
                         (@tr [:select-credential]))
          :on-change   (ui-callback/value on-change-fn)
          :clearable   true
          :options     (map (fn [{:keys [id name]}]
                              {:key id, :text (or name id), :value id}) @ssh-keys)
          :selection   true}]))))


(defn DropdownReleases
  [_opts]
  (let [releases (subscribe [::edges-subs/nuvlabox-releases-options])]
    (fn [opts]
      (when (empty? @releases)
        (dispatch [::edges-events/get-nuvlabox-releases]))
      [ui/Dropdown
       (merge {:selection true
               :loading   (empty? @releases)
               :options   @releases}
              opts)])))


(defn AddRevokeSSHButton
  [{:keys [id] :as _resource} operation show? _title _icon _button-text]
  (let [tr            (subscribe [::i18n-subs/tr])
        close-fn      #(reset! show? false)
        form-data     (r/atom {})
        key-data      (r/atom nil)
        loading?      (r/atom nil)
        on-change-fn  (fn [k v]
                        (if (str/blank? v)
                          (swap! form-data dissoc k)
                          (swap! form-data assoc k v)))
        on-success-fn (fn [ssh-key]
                        (reset! loading? false)
                        (if (:credential @form-data)
                          (close-fn)
                          (reset! key-data ssh-key)))
        on-error-fn   close-fn
        on-click-fn   #(do
                         (reset! loading? true)
                         (dispatch [::edges-detail-events/operation id operation @form-data
                                    on-success-fn on-error-fn]))]
    (fn [_resource operation show? title icon button-text]
      [ui/Modal
       {:open       @show?
        :on-click   #(.stopPropagation %)
        :close-icon true
        :on-close   close-fn
        :trigger    (r/as-element
                      [ui/MenuItem {:on-click #(reset! show? true)}
                       [ui/Icon {:name icon}]
                       title])}
       [uix/ModalHeader {:header title}]
       [ui/ModalContent
        [ui/Form
         [SshKeysDropdown operation (partial on-change-fn :credential)]

         (when @key-data
           [ui/FormField
            [uix/CopyToClipboardDownload
             {:name     "Private-key"
              :value    @key-data
              :download true
              :filename "ssh_private.key"}]])]]
       [ui/ModalActions
        [uix/Button
         {:text     (if @key-data (@tr [:close]) button-text)
          :primary  true
          :loading  (true? @loading?)
          :on-click (if @key-data close-fn on-click-fn)}]]])))


(defn is-old-version?
  [nb-version]
  (or
    (str/blank? nb-version)
    (let [p (->> (str/split nb-version #"\.")
                 (map js/parseInt))]
      (or (< (first p) 1)
          (and (= (first p) 1)
               (< (second p) 16))))))


(defn UpdateButton
  [{:keys [id] :as _resource} operation show?]
  (let [tr            (subscribe [::i18n-subs/tr])
        status        (subscribe [::edges-detail-subs/nuvlabox-status])
        releases      (subscribe [::edges-subs/nuvlabox-releases-options])
        close-fn      #(reset! show? false)
        form-data     (r/atom nil)
        force-restart (r/atom false)
        project       (-> @status :installation-parameters :project-name)
        working-dir   (-> @status :installation-parameters :working-dir)
        config-files  (-> @status :installation-parameters :config-files)
        environment   (-> @status :installation-parameters :environment)
        nb-version    (get @status :nuvlabox-engine-version nil)
        on-change-fn  #(swap! form-data assoc :nuvlabox-release %)
        on-success-fn close-fn
        on-error-fn   close-fn
        on-click-fn   #(dispatch [::edges-detail-events/operation id operation
                                  (utils/format-update-data @form-data)
                                  on-success-fn on-error-fn])]

    (swap! form-data assoc :project-name project)
    (swap! form-data assoc :working-dir working-dir)
    (swap! form-data assoc :config-files (str/join "\n" config-files))
    (swap! form-data assoc :environment (str/join "\n" environment))
    (swap! form-data assoc :force-restart false)
    (when nb-version
      (swap! form-data assoc :current-version nb-version))
    (fn [{:keys [id] :as _resource} _operation show? title icon button-text]
      (let [correct-nb?    (= (:parent @status) id)
            target-version (->> @releases
                                (some #(when (= (:value %) (:nuvlabox-release @form-data)) %))
                                :key)]
        (when-not correct-nb?
          ;; needed to make modal work in cimi detail page
          (dispatch [::edges-detail-events/get-nuvlabox id]))
        [ui/Modal
         {:on-click   #(.stopPropagation %)
          :open       @show?
          :close-icon true
          :on-close   close-fn
          :trigger    (r/as-element
                        [ui/MenuItem {:on-click #(reset! show? true)}
                         [ui/Icon {:name icon}]
                         title])}
         [uix/ModalHeader {:header title}]
         [ui/ModalContent
          (when correct-nb?
            [:<>
             (when (is-old-version? nb-version)
               [ui/Message
                {:error   true
                 :icon    {:name "warning sign", :size "large"}
                 :header  (@tr [:nuvlabox-update-warning])
                 :content (r/as-element
                            [:span (str (@tr [:nuvlabox-update-error-content])) " "
                             [:a {:href   "https://docs.nuvla.io/nuvlabox/nuvlabox-engine/v2/installation/"
                                  :target "_blank"}
                              (str/capitalize (@tr [:see-more]))]])}])
             (when (and (some? target-version) (is-old-version? target-version))
               [ui/Message
                {:warning true
                 :icon    {:name "warning sign", :size "large"}
                 :header  (@tr [:nuvlabox-update-warning])
                 :content (r/as-element
                            [:span (@tr [:nuvlabox-update-warning-content])])}])
             [ui/Segment
              [:b (@tr [:current-version])]
              [:i nb-version]]])
          [ui/Segment
           [:b (@tr [:update-to])]
           [DropdownReleases {:placeholder (@tr [:select-version])
                              :on-change   (ui-callback/value #(on-change-fn %))
                              :disabled    (is-old-version? nb-version)} "release-date>='2021-02-10T09:51:40Z'"]]
          [uix/Accordion
           [:<>
            [ui/Form
             [ui/FormField
              [:label
               "Force Restart"]
              [ui/Radio {:toggle    true
                         :checked   @force-restart
                         :label     (if (:force-restart @form-data)
                                      (@tr [:nuvlabox-update-force-restart])
                                      (@tr [:nuvlabox-update-no-force-restart]))
                         :on-change #(do
                                       (swap! force-restart not)
                                       (swap! form-data assoc :force-restart @force-restart))}]]

             [ui/FormInput {:label         (str/capitalize (@tr [:project]))
                            :placeholder   "nuvlabox"
                            :required      true
                            :default-value (:project-name @form-data)
                            :on-change     (ui-callback/input-callback
                                             #(swap! form-data assoc :project-name %))}]
             [ui/FormInput {:label         (str/capitalize (@tr [:working-directory]))
                            :placeholder   "/home/ubuntu/nuvlabox-engine"
                            :required      true
                            :default-value (:working-dir @form-data)
                            :on-change     (ui-callback/input-callback
                                             #(swap! form-data assoc :working-dir %))}]
             [ui/FormField
              [:label
               [general-utils/mandatory-name (@tr [:config-files])]
               [components/InfoPopup (@tr [:config-file-info])]]
              [ui/TextArea {:placeholder   "docker-compose.yml\ndocker-compose.gpu.yml\n..."
                            :required      true
                            :default-value (:config-files @form-data)
                            :on-change     (ui-callback/input-callback
                                             #(swap! form-data assoc :config-files %))}]]
             [ui/FormField
              [:label (@tr [:env-variables]) " " [components/InfoPopup (@tr [:env-variables-info])]]
              [ui/TextArea {:placeholder   "NUVLA_ENDPOINT=nuvla.io\nPYTHON_VERSION=3.8.5\n..."
                            :default-value (:environment @form-data)
                            :on-change     (ui-callback/input-callback
                                             #(swap! form-data assoc :environment %))}]]]]
           :label (@tr [:advanced])
           :title-size :h4
           :default-open false]]
         [ui/ModalActions
          [uix/Button
           {:text     button-text
            :disabled (or (utils/form-update-data-incomplete? @form-data) (is-old-version? nb-version))
            :primary  true
            :on-click on-click-fn}]]]))))


(defn NBManagersDropdown
  [nuvlabox-id _on-change-fn]
  (let [tr       (subscribe [::i18n-subs/tr])
        managers (subscribe [::edges-detail-subs/nuvlabox-managers])]
    (dispatch [::edges-detail-events/get-nuvlabox-managers nuvlabox-id])
    (fn [_nuvlabox-id on-change-fn]
      [ui/FormDropdown
       {:label       (@tr [:nuvlabox-available-managers])
        :loading     (nil? @managers)
        :on-change   (ui-callback/value on-change-fn)
        :clearable   true
        :placeholder (@tr [:nuvlabox-available-managers-select])
        :options     (map (fn [[id status]]
                            (let [name (:name status)]
                              {:key id, :text name, :value id})) @managers)
        :selection   true}])))


(defn ClusterButton
  [{:keys [id] :as _resource} operation show?]
  (let [tr               (subscribe [::i18n-subs/tr])
        deployments      (subscribe [::deployments-subs/deployments])
        join-token       (subscribe [::edges-detail-subs/join-token])
        nuvlabox-cluster (subscribe [::edges-detail-subs/nuvlabox-cluster])
        managers         (subscribe [::edges-detail-subs/nuvlabox-managers])
        close-fn         #(reset! show? false)
        default-action   "join-worker"
        form-data        (r/atom {:cluster-action default-action})
        actions          [{:key 1 :text (@tr [:cluster-action-join-worker]) :value default-action}
                          {:key 2 :text (@tr [:cluster-action-join-manager]) :value "join-manager"}
                          {:key 3 :text (@tr [:cluster-action-leave]) :value "leave"}
                          {:key 4 :text (@tr [:cluster-action-force-new-cluster]) :value "force-new-cluster"}]
        on-success-fn    close-fn
        on-error-fn      close-fn
        on-click-fn      #(do
                            (swap! form-data assoc :token (:token @join-token))
                            (dispatch [::edges-detail-events/set-join-token nil])
                            (dispatch [::edges-detail-events/operation id operation @form-data
                                       on-success-fn on-error-fn]))]
    (fn [resource _operation show?]
      (let [dpls                (:resources @deployments)
            active-dpls         (filter (fn [x]
                                          (when (:state x)
                                            (not= (:state x) "STOPPED"))) dpls)
            has-active-dp       (if (> (count active-dpls) 0)
                                  true
                                  false)
            title               (@tr [:cluster-actions])
            nuvlabox-manager-id (:nuvlabox-manager-id @form-data)
            nuvlabox-status     (get-in @managers [nuvlabox-manager-id :status])
            cluster-action      (:cluster-action @form-data)
            is-join-manager?    (= cluster-action "join-manager")
            is-join-worker?     (= cluster-action "join-worker")
            is-leave-action?    (= cluster-action "leave")
            is-new-action?      (= cluster-action "force-new-cluster")
            is-join-action?     (or is-join-worker? is-join-manager?)
            manager-selected?   (not (nil? nuvlabox-manager-id))
            valid-manager?      (and
                                  manager-selected?
                                  (not (str/blank? (and (:token @join-token) (:nuvlabox-manager-id @form-data)))))
            invalid-manager?    (and manager-selected? (not valid-manager?))
            cluster-id          (:cluster-id @nuvlabox-cluster)
            cluster-name        (:name @nuvlabox-cluster)
            join-type           (if is-join-manager? "MANAGER" "WORKER")]

        [ui/Modal
         {:open       @show?
          :on-click   #(.stopPropagation %)
          :close-icon true
          :on-close   close-fn
          :trigger    (r/as-element
                        [ui/MenuItem {:on-click #(reset! show? true)}
                         [ui/Icon {:name "linkify"}]
                         title])}
         [uix/ModalHeader {:header title}]
         [ui/ModalContent
          [ui/Message
           {:warning true
            :icon    {:name "warning sign", :size "large"}
            :header  (@tr [:nuvlabox-clustering-warning-header])
            :content (r/as-element
                       [:span
                        (@tr [:nuvlabox-clustering-warning-content])])}]
          (if has-active-dp
            [ui/Message {:negative true}
             (@tr [:nuvlabox-cant-cluster-with-dpls])]
            [ui/Form
             [ui/FormDropdown
              {:label         "Action"
               :on-change     (ui-callback/value
                                (fn [value]
                                  (if (str/blank? value)
                                    (swap! form-data dissoc :cluster-action)
                                    (do
                                      (swap! form-data dissoc :token)
                                      (swap! form-data dissoc :advertise-addr)
                                      (swap! form-data dissoc :nuvlabox-manager-status)
                                      (swap! form-data assoc :cluster-action value)
                                      ))
                                  (swap! form-data dissoc :nuvlabox-manager-id)))
               :default-value default-action
               :options       actions
               :selection     true}]
             (when is-join-action?
               [:<>
                ^{:key join-type}
                [NBManagersDropdown (:id resource)
                 (partial (fn [join-token-scope nuvlabox-manager-id]
                            (if (str/blank? nuvlabox-manager-id)
                              (swap! form-data dissoc :nuvlabox-manager-status)
                              (do
                                (swap! form-data dissoc :token)
                                (swap! form-data assoc :nuvlabox-manager-status (get-in @managers [nuvlabox-manager-id :status]))
                                (dispatch [::edges-detail-events/get-join-token nuvlabox-manager-id join-token-scope])
                                (dispatch [::edges-detail-events/get-nuvlabox-cluster nuvlabox-manager-id]))))
                          join-type)]
                (when invalid-manager?
                  [ui/Message {:negative true
                               :content  (@tr [:cluster-invalid-manager])}])
                (when valid-manager?
                  [:<>
                   [ui/Message
                    (@tr [:nuvlabox-joining])
                    [:span {:style {:font-weight "bold"}} (if cluster-name
                                                            (str cluster-name " (" cluster-id ")")
                                                            cluster-id)]
                    (@tr [:nuvlabox-joining-on-manager-address])
                    [:span {:style {:font-weight "bold"}} (:cluster-join-address nuvlabox-status)]
                    (@tr [:nuvlabox-joining-with-token])
                    [:p {:style {:font-weight "bold"}} (:token @join-token)]]])])
             (when is-leave-action?
               [ui/Message {:negative true
                            :content  (@tr [:cluster-action-leave-warning])}])
             (when is-new-action?
               [ui/FormInput
                {:label       "Advertise address (optional)"
                 :placeholder "<ip|interface>[:port]"
                 :on-change   (ui-callback/value
                                (fn [value]
                                  (if (str/blank? value)
                                    (swap! form-data dissoc :advertise-addr)
                                    (swap! form-data assoc :advertise-addr value))))}])])]

         [ui/ModalActions
          [uix/Button
           {:text     (@tr [:cluster])
            :primary  true
            :disabled has-active-dp
            :on-click on-click-fn}]]]))))


(defn EmergencyPlaybooksDropdown
  [_nuvlabox-id _on-change]
  (let [em-playbooks (subscribe [::edges-detail-subs/nuvlabox-emergency-playbooks])
        tr           (subscribe [::i18n-subs/tr])]
    (fn [nuvlabox-id on-change]
      (dispatch [::edges-detail-events/get-emergency-playbooks nuvlabox-id])
      (let [em-enabled  (get (group-by :enabled @em-playbooks) true)
            em-disabled (get (group-by :enabled @em-playbooks) false)]
        [:<>
         [ui/FormDropdown
          {:label       "Emergency Playbooks"
           :selection   true
           :placeholder (if (and @em-playbooks (not em-disabled))
                          (@tr [:nuvlabox-emergency-playbooks-none])
                          (str (@tr [:select]) " playbook"))
           :multiple    true
           :loading     (nil? @em-playbooks)
           :options     (map (fn [{:keys [id name]}]
                               {:key   id,
                                :text  (or name id),
                                :value id}) em-disabled)
           :on-change   (ui-callback/value on-change)}]
         (when (pos? (count em-enabled))
           [ui/Message
            (@tr [:nuvlabox-emergency-playbooks-already-enabled])
            [ui/ListSA {:bulleted true}
             (map (fn [{:keys [id name] :as _pb}]
                    [ui/ListItem
                     ^{:key id}
                     [values/as-link id :label (or name id)]])
                  em-enabled)]])]))))


(defn TextActionButton
  [{:keys [id] :as _nuvlabox} operation show? title icon button-text]
  (let [tr          (subscribe [::i18n-subs/tr])
        close-fn    #(reset! show? false)
        on-click-fn #(dispatch [::edges-detail-events/operation-text-response operation id close-fn close-fn])]
    [ui/Modal
     {:open       @show?
      :close-icon true
      :on-click   #(.stopPropagation %)
      :on-close   close-fn
      :trigger    (r/as-element
                    [ui/MenuItem {:on-click #(reset! show? true)}
                     [ui/Icon {:name icon}]
                     title])}
     [uix/ModalHeader {:header title}]
     [ui/ModalContent
      [:p (@tr [:execute-action-msg] [operation])]]
     [ui/ModalActions
      [uix/Button
       {:text     button-text
        :primary  true
        :on-click on-click-fn}]]]))


(defn EnableEmergencyPlaybooksButton
  [{:keys [id] :as _resource} operation show? _title _icon _button-text]
  (let [close-fn      #(reset! show? false)
        form-data     (r/atom {})
        loading?      (r/atom nil)
        on-change-fn  (fn [k v]
                        (if (str/blank? v)
                          (swap! form-data dissoc k)
                          (swap! form-data assoc k v)))
        on-success-fn (fn [_]
                        (reset! loading? false)
                        (when (:emergency-playbooks-ids @form-data)
                          (close-fn)))
        on-error-fn   close-fn
        on-click-fn   #(do
                         (reset! loading? true)
                         (dispatch [::edges-detail-events/operation id operation @form-data
                                    on-success-fn on-error-fn]))]
    (fn [resource _operation show? title icon button-text]
      (let [playbooks (:emergency-playbooks-ids @form-data)]
        [ui/Modal
         {:open       @show?
          :on-click   #(.stopPropagation %)
          :close-icon true
          :on-close   close-fn
          :trigger    (r/as-element
                        [ui/MenuItem {:on-click #(reset! show? true)}
                         [ui/Icon {:name icon}]
                         title])}
         [uix/ModalHeader {:header title}]
         [ui/ModalContent
          [ui/Form
           [EmergencyPlaybooksDropdown (:id resource) (partial on-change-fn :emergency-playbooks-ids)]]]
         [ui/ModalActions
          [uix/Button
           {:text     button-text
            :primary  true
            :disabled (empty? playbooks)
            :loading  (true? @loading?)
            :on-click on-click-fn}]]]))))


(defmethod cimi-detail-views/other-button ["nuvlabox" "cluster-nuvlabox"]
  [_resource _operation]
  (let [show? (r/atom false)]
    (fn [resource operation]
      ^{:key (str "cluster-nuvlabox" @show?)}
      [ClusterButton resource operation show?])))


(defmethod cimi-detail-views/other-button ["nuvlabox" "add-ssh-key"]
  [_resource _operation]
  (let [tr    (subscribe [::i18n-subs/tr])
        show? (r/atom false)]
    (fn [resource operation]
      ^{:key (str "add-ssh-button" @show?)}
      [AddRevokeSSHButton resource operation show? "Add ssh key" "add" (@tr [:add])])))


(defmethod cimi-detail-views/other-button ["nuvlabox" "revoke-ssh-key"]
  [_resource _operation]
  (let [show? (r/atom false)]
    (fn [resource operation]
      ^{:key (str "revoke-ssh-button" @show?)}
      [AddRevokeSSHButton resource operation show? "Revoke ssh key" "minus" "revoke"])))


(defmethod cimi-detail-views/other-button ["nuvlabox" "update-nuvlabox"]
  [_resource _operation]
  (let [tr    (subscribe [::i18n-subs/tr])
        show? (r/atom false)]
    (fn [resource operation]
      ^{:key (str "update-nuvlabox" @show?)}
      [UpdateButton resource operation show? "Update NuvlaEdge" "download" (@tr [:update])])))


(defmethod cimi-detail-views/other-button ["nuvlabox" "enable-emergency-playbooks"]
  [_resource _operation]
  (let [tr    (subscribe [::i18n-subs/tr])
        show? (r/atom false)]
    (fn [resource operation]
      ^{:key (str "enable-emergency-playbooks" @show?)}
      [EnableEmergencyPlaybooksButton resource operation show? "Enable emergency playbooks" "ambulance" (@tr [:enable])])))


(defmethod cimi-detail-views/other-button ["nuvlabox" "assemble-playbooks"]
  [_resource _operation]
  (let [tr    (subscribe [::i18n-subs/tr])
        show? (r/atom false)]
    (fn [resource operation]
      ^{:key (str "assemble-playbooks" @show?)}
      [TextActionButton resource operation show? "Assemble playbooks" "book" (@tr [:yes])])))


(defmethod cimi-detail-views/other-button ["nuvlabox" "enable-host-level-management"]
  [_resource _operation]
  (let [tr    (subscribe [::i18n-subs/tr])
        show? (r/atom false)]
    (fn [resource operation]
      ^{:key (str "enable-host-level-management" @show?)}
      [TextActionButton resource operation show? "Enable host level management" "cog" (@tr [:enable])])))


(defn MenuBar [uuid]
  (let [deployment-fleet (subscribe [::subs/deployment-fleet])
        loading?         (subscribe [::subs/loading?])]
    (fn []
      (let [MenuItems (cimi-detail-views/format-operations
                        @deployment-fleet
                        #{})]
        [components/StickyBar
         [components/ResponsiveMenuBar
          MenuItems
          [components/RefreshMenu
           {:action-id  refresh-action-id
            :loading?   @loading?
            :on-refresh #(refresh uuid)}]]]))))


(defn get-available-actions
  [operations]
  (filter some? (map #(nth (str/split % #"/") 2 nil) (map :href operations))))


(defn Peripheral
  [id]
  (let [locale       (subscribe [::i18n-subs/locale])
        tr           (subscribe [::i18n-subs/tr])
        last-updated (r/atom "1970-01-01T00:00:00Z")
        button-load? (r/atom false)
        peripheral   (subscribe [::edges-detail-subs/nuvlabox-peripheral id])]
    (fn [_id]
      (let [{p-id                :id
             p-ops               :operations
             p-name              :name
             p-product           :product
             p-created           :created
             p-updated           :updated
             p-descr             :description
             p-interface         :interface
             p-device-path       :device-path
             p-available         :available
             p-vendor            :vendor
             p-classes           :classes
             p-identifier        :identifier
             p-serial-num        :serial-number
             p-video-dev         :video-device
             p-data-gw-url       :local-data-gateway-endpoint
             p-data-sample       :raw-data-sample
             p-additional-assets :additional-assets
             p-resources         :resources} @peripheral
            actions (get-available-actions p-ops)]

        (when (pos? (compare p-updated @last-updated))
          (reset! button-load? false)
          (reset! last-updated p-updated))
        [uix/Accordion
         [ui/Segment {:basic true}
          [ui/Table {:basic  "very"
                     :padded false}
           [ui/TableBody
            (when p-name
              [ui/TableRow
               [ui/TableCell "Name"]
               [ui/TableCell p-name]])
            (when p-product
              [ui/TableRow
               [ui/TableCell "Product"]
               [ui/TableCell p-product]])
            (when p-serial-num
              [ui/TableRow
               [ui/TableCell "Serial Number"]
               [ui/TableCell p-serial-num]])
            (when p-descr
              [ui/TableRow
               [ui/TableCell "Description"]
               [ui/TableCell p-descr]])
            [ui/TableRow
             [ui/TableCell "Classes"]
             [ui/TableCell (str/join ", " p-classes)]]
            [ui/TableRow
             [ui/TableCell "Available"]
             [ui/TableCell
              [ui/Icon {:name "circle", :color (if p-available "green" "red")}]
              (if p-available "Yes" "No")]]
            (when p-interface
              [ui/TableRow
               [ui/TableCell "Interface"]
               [ui/TableCell p-interface]])
            (when p-device-path
              [ui/TableRow
               [ui/TableCell "Device Path"]
               [ui/TableCell p-device-path]])
            (when p-video-dev
              [ui/TableRow
               [ui/TableCell "Video Device"]
               [ui/TableCell p-video-dev]])
            [ui/TableRow
             [ui/TableCell "Identifier"]
             [ui/TableCell p-identifier]]
            [ui/TableRow
             [ui/TableCell "Vendor"]
             [ui/TableCell p-vendor]]
            [ui/TableRow
             [ui/TableCell "Created"]
             [ui/TableCell (time/ago (time/parse-iso8601 p-created) @locale)]]
            [ui/TableRow
             [ui/TableCell "Updated"]
             [ui/TableCell (time/ago (time/parse-iso8601 p-updated) @locale)]]
            (when p-resources
              [ui/TableRow
               [ui/TableCell "Resources"]
               [ui/TableCell [ui/Grid {:columns   3,
                                       :stackable true
                                       :divided   "vertically"}
                              (for [resource p-resources]
                                [ui/GridRow
                                 [ui/GridColumn
                                  [:div [:span {:style {:font-weight "bold"}}
                                         "Unit: "]
                                   (:unit resource)]]
                                 [ui/GridColumn
                                  [:span [:span {:style {:font-weight "bold"}}
                                          "Capacity: "]
                                   (:capacity resource)]]
                                 (when (:load resource)
                                   [ui/GridColumn
                                    [:span [:span {:style {:font-weight "bold"}}
                                            "Load: "]
                                     (:load resource) "%"]])])]]])
            (when p-additional-assets
              [ui/TableRow
               [ui/TableCell "Additional Assets"]
               [ui/TableCell (map (fn [[key value]]
                                    [ui/Segment {:vertical true}
                                     [:div {:style {:font-weight "bold" :font-variant "small-caps"}}
                                      (str (name key) ": ")]
                                     (map (fn [val]
                                            [:div val]) value)]) p-additional-assets)]])
            (when p-data-gw-url
              [ui/TableRow {:positive true}
               [ui/TableCell "Data Gateway Connection"]
               [ui/TableCell p-data-gw-url]])
            (when p-data-sample
              [ui/TableRow {:positive true}
               [ui/TableCell "Raw Data Sample"]
               [ui/TableCell p-data-sample]])]]]
         :label [:span (or p-name p-product)
                 (when (pos? (count actions))
                   [ui/Popup
                    {:position "left center"
                     :content  (@tr [:nuvlabox-datagateway-action])
                     :header   "data-gateway"
                     :wide     "very"
                     :size     "small"
                     :trigger  (r/as-element
                                 [ui/Button
                                  {:on-click (fn [event]
                                               (reset! button-load? true)
                                               (dispatch
                                                 [::edges-detail-events/custom-action p-id (first actions)
                                                  (str "Triggered " (first actions) " for " p-id)])
                                               (.stopPropagation event))
                                   :style    {:margin "-.6em"}
                                   :color    "vk"
                                   :floated  "right"
                                   :circular true
                                   :disabled @button-load?
                                   :loading  @button-load?}
                                  (first actions)])}])]
         :title-size :h4
         :default-open false
         :icon (if p-interface
                 (case (str/lower-case p-interface)
                   "usb" "usb"
                   "bluetooth" "bluetooth b"
                   "bluetooth-le" "bluetooth b"
                   "ssdp" "fas fa-chart-network"
                   "ws-discovery" "fas fa-chart-network"
                   "bonjour/avahi" "fas fa-chart-network"
                   "gpu" "microchip"
                   nil)
                 nil)]))))


(defn OnlineStatusIcon
  [online]
  [ui/Icon {:name  "power"
            :color (utils/status->color online)}])


(defn Heartbeat
  [updated]
  (let [tr                       (subscribe [::i18n-subs/tr])
        updated-moment           (time/parse-iso8601 updated)
        status                   (subscribe [::edges-detail-subs/nuvlabox-online-status])
        next-heartbeat-moment    (subscribe [::edges-detail-subs/next-heartbeat-moment])
        next-heartbeat-times-ago (time/ago @next-heartbeat-moment)

        last-heartbeat-msg       (if updated
                                   (str (@tr [:heartbeat-last-was]) " " (time/ago updated-moment))
                                   (@tr [:heartbeat-unavailable]))

        next-heartbeat-msg       (when @next-heartbeat-moment
                                   (if (= @status :online)
                                     (str (@tr [:heartbeat-next-is-expected]) " " next-heartbeat-times-ago)
                                     (str (@tr [:heartbeat-next-was-expected]) " " next-heartbeat-times-ago)))]

    [ui/Message {:icon    "heartbeat"
                 :content (str last-heartbeat-msg ". " next-heartbeat-msg)}]))


(defn Load
  [resources]
  (let [load-stats      (utils/load-statistics resources)
        net-stats       (utils/load-net-stats (:net-stats resources))
        container-stats (:container-stats resources)
        number-of-stats (count load-stats)]
    ; TODO: if the number-of-stats grows if should split into a new row
    [ui/Grid {:columns   number-of-stats,
              :stackable true
              :divided   true
              :celled    "internally"}
     [ui/GridRow
      (for [stat load-stats]
        ^{:key (:title stat)}
        [ui/GridColumn
         [:div
          [plot/Doughnut {:height  250
                          :data    {:labels   (:label stat)
                                    :datasets [{:data            [(:percentage stat), (:value stat)]
                                                :backgroundColor ["rgb(230, 99, 100)",
                                                                  "rgba(155, 99, 132, 0.1)",
                                                                  "rgb(230, 99, 100)"]
                                                :borderColor     ["rgba(230, 99, 100,1)"]
                                                :borderWidth     3}]}
                          :options {:legend              {:display true
                                                          :labels  {:fontColor "grey"}}
                                    :title               {:display  true
                                                          :text     (:title stat)
                                                          :position "bottom"}
                                    :maintainAspectRatio false
                                    :circumference       4.14
                                    :rotation            -3.64
                                    :cutoutPercentage    60}}]]

         (when (pos? (count (:data-gateway stat)))
           [ui/Container {:key        (:topic stat)
                          :text-align :center}
            [ui/LabelGroup {:key  (:topic stat)
                            :size "tiny"}
             [ui/Label {:color "blue"
                        :basic true
                        :image true}
              "Topic: "
              [ui/LabelDetail
               (first (:data-gateway stat))]]
             [ui/Label {:color "blue"
                        :basic true
                        :image true}
              "Raw sample: "
              [ui/LabelDetail
               [ui/Popup
                {:trigger        (r/as-element [ui/Icon {:name "eye"}])
                 :content        (last (:data-gateway stat))
                 :position       "right center"
                 :inverted       true
                 :wide           true
                 :on             "hover"
                 :size           "mini"
                 :hoverable      true
                 :hide-on-scroll true}]]]]])])]
     (when (pos? (count (:label net-stats)))
       [ui/GridRow {:centered true
                    :columns  2}
        [ui/GridColumn
         [:div
          [plot/Bar {:height  200
                     :data    {:labels   (:label net-stats)
                               :datasets [{:label           "Received",
                                           :data            (:rx net-stats)
                                           :backgroundColor "rgb(182, 219, 238)"
                                           :borderColor     "white"
                                           :borderWidth     1}
                                          {:label           "Transmitted",
                                           :data            (:tx net-stats)
                                           :backgroundColor "rgb(230, 99, 100)"
                                           :borderColor     "white"
                                           :borderWidth     1}]}
                     :options {:legend {:display true
                                        :labels  {:fontColor "grey"}}
                               :title  {:display  true
                                        :text     (:title net-stats)
                                        :position "bottom"}
                               :scales {:yAxes [{:type       "logarithmic"
                                                 :scaleLabel {:labelString "megabytes"
                                                              :display     true}}]}}}]]]])
     (when container-stats
       [ui/GridRow {:centered true
                    :columns  1}
        [ui/GridColumn
         [ui/Table {:compact "very", :selectable true, :basic "very"}
          [ui/TableHeader
           [ui/TableRow
            [ui/TableHeaderCell "ID"]
            [ui/TableHeaderCell "Container Name"]
            [ui/TableHeaderCell "CPU %"]
            [ui/TableHeaderCell "Mem Usage/Limit"]
            [ui/TableHeaderCell "Mem %"]
            [ui/TableHeaderCell "Net I/O"]
            [ui/TableHeaderCell "Block I/O"]
            [ui/TableHeaderCell "Status"]
            [ui/TableHeaderCell "Restart Count"]]]

          [ui/TableBody
           (for [{:keys [id name cpu-percent mem-usage-limit
                         mem-percent net-in-out blk-in-out
                         container-status restart-count] :as _cstat} container-stats]
             (when id
               ^{:key id}
               [ui/TableRow
                [ui/TableCell (apply str (take 8 id))]
                [ui/TableCell (apply str (take 25 name))]
                [ui/TableCell cpu-percent]
                [ui/TableCell mem-usage-limit]
                [ui/TableCell mem-percent]
                [ui/TableCell net-in-out]
                [ui/TableCell blk-in-out]
                [ui/TableCell container-status]
                [ui/TableCell restart-count]]))]]]])]))


(defn ActionsMenu
  "This creates a floating (top right) label with a pinned popup menu
  with a list of available actions for the corresponding resource.

  The list of actions must be passed as an argument, as a list of elements, in the following form:
  [
    {:content \"link text\" :on-click #() :style {}}
  ]"
  [action-list]
  [ui/Label {:circular true
             :floating true
             :basic    true}
   [ui/Popup {:position "right center"
              :on       "click"
              :style    {:padding "5px"}
              :size     "small"
              :pinned   true
              :trigger  (r/as-element [ui/Button
                                       {:icon  true
                                        :style {:margin     "0"
                                                :padding    "0"
                                                :border     "0px"
                                                :background "none"}}
                                       [ui/Icon {:name "ellipsis vertical"
                                                 :link true}]])
              :content  (r/as-element [ui/ListSA {:vertical-align "middle"
                                                  :link           true
                                                  :selection      true
                                                  :divided        true}
                                       (for [action action-list]
                                         ^{:key (str "action." (random-uuid))}
                                         [ui/ListItem {:as "a"}
                                          [ui/ListContent
                                           [ui/ListDescription
                                            [:span {:on-click (:on-click action)
                                                    :style    (:style action)}
                                             (:content action)]]]])])}]])


(defn edit-action
  [uuid body close-fn]
  (let [tr (subscribe [::i18n-subs/tr])]
    (dispatch [::edges-detail-events/edit
               uuid body
               (@tr [:updated-successfully])])
    (close-fn)))


(defn EditableCell
  [attribute]
  (let [tr               (subscribe [::i18n-subs/tr])
        deployment-fleet (subscribe [::subs/deployment-fleet])
        can-edit?        (subscribe [::subs/can-edit?])
        id               (:id @deployment-fleet)
        on-change-fn     #(dispatch [::events/edit
                                     id {attribute %}
                                     (@tr [:updated-successfully])])]
    (if @can-edit?
      [components/EditableInput attribute @deployment-fleet on-change-fn]
      [ui/TableCell (get @deployment-fleet attribute)])))


(defn TabOverviewDeploymentFleet
  [{:keys [id created updated created-by]}]
  (let [tr     (subscribe [::i18n-subs/tr])
        locale (subscribe [::i18n-subs/locale])]
    [ui/Segment {:secondary true
                 :color     "blue"
                 :raised    true}
     [:h4 "Deployment fleet"]
     [ui/Table {:basic  "very"
                :padded false}
      [ui/TableBody
       [ui/TableRow
        [ui/TableCell "Id"]
        (when id
          [ui/TableCell [values/as-link id :label (general-utils/id->uuid id)]])]
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:name]))]
        [EditableCell :name]]
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:description]))]
        [EditableCell :description]]
       (when created-by
         [ui/TableRow
          [ui/TableCell (str/capitalize (@tr [:created-by]))]
          [ui/TableCell @(subscribe [::session-subs/resolve-user created-by])]])
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:created]))]
        [ui/TableCell (time/ago (time/parse-iso8601 created) @locale)]]
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:updated]))]
        [ui/TableCell (time/ago (time/parse-iso8601 updated) @locale)]]]]]))


(defn TabOverviewTags
  [{:keys [id] :as deployment-fleet}]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Segment {:secondary true
                 :color     "teal"
                 :raised    true}
     [:h4 "Tags"]
     [components/EditableTags
      deployment-fleet #(dispatch [::events/edit id {:tags %}
                                   (@tr [:updated-successfully])])]]))


(defn ServiceIcon
  [subtype]
  (let [[kind path] (get {:swarm      [:icon "docker"]
                          :s3         [:image "/ui/images/s3.png"]
                          :kubernetes [:image "/ui/images/kubernetes.svg"]
                          :registry   [:icon "database"]}
                         (keyword subtype)
                         [:icon "question circle"])]
    (case kind
      :image [ui/Image {:src   path
                        :style {:overflow       "hidden"
                                :display        "inline-block"
                                :height         28
                                :margin-right   4
                                :padding-bottom 7}}]
      [ui/Icon {:name path}])))


(defn TabOverview
  []
  (let [deployment-fleet (subscribe [::subs/deployment-fleet])]
    (fn []
      (let [{:keys [tags]} @deployment-fleet]
        [ui/TabPane
         [ui/Grid {:columns   2
                   :stackable true
                   :padded    true}
          [ui/GridRow
           [ui/GridColumn {:stretched true}
            [TabOverviewDeploymentFleet @deployment-fleet]]
           [ui/GridColumn {:stretched true}
            [deployments-views/DeploymentsOverviewSegment
             ::deployments-subs/deployments ::events/set-active-tab :deployments]]]

          (when (seq tags)
            [ui/GridColumn
             [TabOverviewTags @deployment-fleet]])]]))))


(defn TabEvents
  []
  (let [tr                (subscribe [::i18n-subs/tr])
        deployment-fleet  (subscribe [::subs/deployment-fleet])
        all-events        (subscribe [::subs/deployment-fleet-events])
        elements-per-page (subscribe [::subs/elements-per-page])
        total-elements    (get @all-events :count 0)
        total-pages       (general-utils/total-pages total-elements @elements-per-page)
        page              (subscribe [::subs/page])]
    (fn []
      (let [events (:resources @all-events)]
        [ui/TabPane
         (if (and (pos? total-elements) (= (count events) 0))
           [ui/Loader {:active true
                       :inline "centered"}]
           [ui/Table {:basic "very"}
            [ui/TableHeader
             [ui/TableRow
              [ui/TableHeaderCell [:span (@tr [:event])]]
              [ui/TableHeaderCell [:span (@tr [:timestamp])]]
              [ui/TableHeaderCell [:span (@tr [:category])]]
              [ui/TableHeaderCell [:span (@tr [:state])]]]]
            [ui/TableBody
             (for [{:keys [id content timestamp category]} events]
               ^{:key id}
               [ui/TableRow
                [ui/TableCell [values/as-link id :label (general-utils/id->short-uuid id)]]
                [ui/TableCell timestamp]
                [ui/TableCell category]
                [ui/TableCell (:state content)]])]])


         [uix/Pagination {:totalPages   total-pages
                          :activePage   @page
                          :onPageChange (ui-callback/callback
                                          :activePage #(do
                                                         (dispatch [::events/set-page %])
                                                         (refresh (:id @deployment-fleet))))}]]))))


(defn tabs
  []
  (let [tr               @(subscribe [::i18n-subs/tr])
        deployment-fleet (subscribe [::subs/deployment-fleet])
        can-edit?        @(subscribe [::subs/can-edit?])]
    [{:menuItem {:content "Overview"
                 :key     :overview
                 :icon    "info"}
      :render   #(r/as-element [TabOverview])}
     {:menuItem {:content "Events"
                 :key     :events
                 :icon    "bolt"}
      :render   #(r/as-element [TabEvents])}
     {:menuItem {:content "Deployments"
                 :key     :deployments
                 :icon    "rocket"}
      :render   #(r/as-element [deployments-views/DeploymentTable
                                {:empty-msg (tr [:empty-deployemnt-msg])}])}
     (job-views/jobs-section)
     (acl/TabAcls deployment-fleet can-edit? ::events/edit)]))


(defn TabsDeploymentFleet
  []
  (let [active-tab (subscribe [::subs/active-tab])
        panes      (tabs)]
    [ui/Tab
     {:menu        {:secondary true
                    :pointing  true
                    :style     {:display        "flex"
                                :flex-direction "row"
                                :flex-wrap      "wrap"}}
      :panes       panes
      :activeIndex (tab/key->index panes @active-tab)
      :onTabChange (tab/on-tab-change
                     panes
                     #(dispatch [::events/set-active-tab %]))}]))


(defn PageHeader
  [new]
  (let [{:keys [id name]} @(subscribe [::subs/deployment-fleet])]
    [uix/PageHeader "bullseye" (if new "New" (or name id))]))

(defn Application
  [{:keys [id name description]}]
  (let [selected? @(subscribe [::subs/app-selected? id])]
    [ui/ListItem {:on-click #(dispatch [::events/toggle-select-app id])
                  :style    {:cursor :pointer}}
     [ui/ListIcon {:name (if selected?
                           "check square outline"
                           "square outline")}]
     [ui/ListContent
      [ui/ListHeader (when selected? {:as :a}) name]
      [ui/ListDescription description]]]))

(defn Applications
  [applications]
  [:<>
   (for [{:keys [id] :as child} applications]
     ^{:key id}
     [Application child])])

(declare Node)

(defn Project
  [path {:keys [applications] :as content}]
  [ui/ListItem
   [ui/ListIcon {:name "folder"}]
   [ui/ListContent
    [ui/ListHeader path]
    [ui/ListList
     [Node
      (dissoc content :applications)
      applications]]]])

(defn Projects
  [projects]
  [:<>
   (for [[path content] projects]
     ^{:key path}
     [Project path content])])

(defn Node
  [projects applications]
  (js/console.error "Node " (sort-by first projects))
  [:<>
   [Projects (sort-by first projects)]
   [Applications (sort-by (juxt :id :name) applications)]])

(defn SelectApps
  []
  (let [apps     @(subscribe [::subs/apps-tree])
        fulltext @(subscribe [::subs/apps-fulltext-search])]
    [:<>
     [components/SearchInput
      {:on-change     (ui-callback/input-callback #(dispatch [::events/set-apps-fulltext-search %]))
       :default-value fulltext}]
     [ui/ListSA
      [Node (dissoc apps :applications) (:applications apps)]]]))

(defn New
  []
  (dispatch [::events/search-apps])
  [ui/Container {:fluid true}
   [PageHeader true]
   [ui/Grid
    [ui/GridRow {:columns   2
                 :stackable :mobile}
     [ui/GridColumn
      [ui/Segment
       [:h2 "Applications"]
       [SelectApps]
       ]
      ]
     [ui/GridColumn
      [ui/Segment
       [:h2 "Targets"]]
      ]]]
   ])

(defn DeploymentFleet
  []
  (refresh uuid)
  [components/LoadingPage {:dimmable? true}
   [:<>
    [components/NotFoundPortal
     ::subs/deployment-fleet-not-found?
     :no-deployment-fleet-message-header
     :no-deployment-fleet-message-content]
    [ui/Container {:fluid true}
     [PageHeader]
     [MenuBar uuid]
     [components/ErrorJobsMessage
      ::job-subs/jobs ::events/set-active-tab :jobs]
     [TabsDeploymentFleet]]]])


(defn Details
  [uuid]
  (if (= (str/lower-case uuid) "new")
    [New]
    [DeploymentFleet]))
