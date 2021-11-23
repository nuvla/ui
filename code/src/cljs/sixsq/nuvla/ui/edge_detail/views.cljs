(ns sixsq.nuvla.ui.edge-detail.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.acl.views :as acl]
    [sixsq.nuvla.ui.cimi-detail.views :as cimi-detail-views]
    [sixsq.nuvla.ui.config :as config]
    [sixsq.nuvla.ui.deployment.subs :as deployment-subs]
    [sixsq.nuvla.ui.deployment.views :as deployment-views]
    [sixsq.nuvla.ui.edge-detail.events :as events]
    [sixsq.nuvla.ui.edge-detail.subs :as subs]
    [sixsq.nuvla.ui.edge.events :as edge-events]
    [sixsq.nuvla.ui.edge.subs :as edge-subs]
    [sixsq.nuvla.ui.edge.utils :as utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.job.subs :as job-subs]
    [sixsq.nuvla.ui.job.views :as job-views]
    [sixsq.nuvla.ui.main.components :as components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.session.subs :as session-subs]
    [sixsq.nuvla.ui.utils.form-fields :as ff]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.map :as map]
    [sixsq.nuvla.ui.utils.plot :as plot]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as values]))


(def refresh-action-id :nuvlabox-get-nuvlabox)
(def tab-deployment-index 5)

(def orchestration-icons
  {:swarm      "docker"
   :kubernetes "/ui/images/kubernetes.svg"})


(defn refresh
  [uuid]
  (dispatch [::main-events/action-interval-start
             {:id        refresh-action-id
              :frequency 10000
              :event     [::events/get-nuvlabox (str "nuvlabox/" uuid)]}]))


(defn DecommissionButton
  [nuvlabox]
  (let [tr      (subscribe [::i18n-subs/tr])
        {:keys [id name description]} nuvlabox
        content (str (or name id) (when description " - ") description)]
    [uix/ModalDanger
     {:button-text (@tr [:decommission])
      :on-confirm  #(dispatch [::events/decommission])
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
      :on-confirm  #(dispatch [::events/delete])
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
                   (subscribe [::subs/nuvlabox-associated-ssh-keys]))]
    (when is-add?
      (dispatch [::events/get-ssh-keys-not-associated #(reset! ssh-keys %)]))
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
  (let [releases (subscribe [::edge-subs/nuvlabox-releases-options])]
    (fn [opts]
      (when (empty? @releases)
        (dispatch [::edge-events/get-nuvlabox-releases]))
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
                         (dispatch [::events/operation id operation @form-data
                                    on-success-fn on-error-fn]))]
    (fn [_resource operation show? title icon button-text]
      (let [credential (:credential @form-data)]
        [ui/Modal
         {:open       @show?
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
              [:a {:href     (str "data:text/plain;charset=utf-8,"
                                  (js/encodeURIComponent @key-data))
                   :target   "_blank"
                   :download "ssh_private.key"}
               [ui/Button {:positive       true
                           :fluid          true
                           :icon           "download"
                           :label-position "left"
                           :as             "div"
                           :content        (@tr [:download])}]]])]]
         [ui/ModalActions
          [uix/Button
           {:text     (if @key-data (@tr [:close]) button-text)
            :primary  true
            :disabled (nil? credential)
            :loading  (true? @loading?)
            :on-click (if @key-data close-fn on-click-fn)}]]]))))


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
        status        (subscribe [::subs/nuvlabox-status])
        releases      (subscribe [::edge-subs/nuvlabox-releases-options])
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
        on-click-fn   #(dispatch [::events/operation id operation
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
          (dispatch [::events/get-nuvlabox id]))
        [ui/Modal
         {:open       @show?
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
                             [:a {:href   (str "https://docs.nuvla.io/nuvlabox/"
                                               "nuvlabox-engine/quickstart.html#from-nuvla")
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
        managers (subscribe [::subs/nuvlabox-managers])]
    (dispatch [::events/get-nuvlabox-managers nuvlabox-id])
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
        deployments      (subscribe [::deployment-subs/deployments])
        join-token       (subscribe [::subs/join-token])
        nuvlabox-cluster (subscribe [::subs/nuvlabox-cluster])
        managers         (subscribe [::subs/nuvlabox-managers])
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
                            (dispatch [::events/set-join-token nil])
                            (dispatch [::events/operation id operation @form-data
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
                                (dispatch [::events/get-join-token nuvlabox-manager-id join-token-scope])
                                (dispatch [::events/get-nuvlabox-cluster nuvlabox-manager-id]))))
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
      [UpdateButton resource operation show? "Update NuvlaBox" "download" (@tr [:update])])))


(defn MenuBar [uuid]
  (let [can-decommission? (subscribe [::subs/can-decommission?])
        can-delete?       (subscribe [::subs/can-delete?])
        nuvlabox          (subscribe [::subs/nuvlabox])
        loading?          (subscribe [::subs/loading?])]
    (fn []
      [components/StickyBar
       [ui/Menu {:borderless true, :stackable true}
        (when @can-decommission?
          [DecommissionButton @nuvlabox])
        (when @can-delete?
          [DeleteButton @nuvlabox])

        [cimi-detail-views/format-operations @nuvlabox #{"edit" "delete" "activate" "decommission"
                                                         "commission" "check-api"}]

        [components/RefreshMenu
         {:action-id  refresh-action-id
          :loading?   @loading?
          :on-refresh #(refresh uuid)}]]])))


(defn get-available-actions
  [operations]
  (filter some? (map #(nth (str/split % #"/") 2 nil) (map :href operations))))


(defn Peripheral
  [id]
  (let [locale       (subscribe [::i18n-subs/locale])
        tr           (subscribe [::i18n-subs/tr])
        last-updated (r/atom "1970-01-01T00:00:00Z")
        button-load? (r/atom false)
        peripheral   (subscribe [::subs/nuvlabox-peripheral id])]
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
                                                 [::events/custom-action p-id (first actions)
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
        status                   (subscribe [::subs/nuvlabox-online-status])
        next-heartbeat-moment    (subscribe [::subs/next-heartbeat-moment])
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
    (dispatch [::events/edit
               uuid body
               (@tr [:updated-successfully])])
    (close-fn)))


(defn EditableCell
  [attribute]
  (let [tr           (subscribe [::i18n-subs/tr])
        nuvlabox     (subscribe [::subs/nuvlabox])
        can-edit?    (subscribe [::subs/can-edit?])
        id           (:id @nuvlabox)
        on-change-fn #(dispatch [::events/edit
                                   id {attribute %}
                                   (@tr [:updated-successfully])])]
    (if @can-edit?
      [components/EditableInput attribute @nuvlabox on-change-fn]
      [ui/TableCell (get @nuvlabox attribute)])))


(defn TabOverviewNuvlaBox
  [{:keys [id created updated version refresh-interval owner]}
   {:keys [nuvlabox-api-endpoint nuvlabox-engine-version]}]
  (let [tr             (subscribe [::i18n-subs/tr])
        resolved-owner (subscribe [::session-subs/resolve-user owner])
        locale         (subscribe [::i18n-subs/locale])]
    [ui/Segment {:secondary true
                 :color     "blue"
                 :raised    true}
     [:h4 "NuvlaBox "
      (when nuvlabox-engine-version
        [ui/Label {:circular true
                   :color    "blue"
                   :size     "tiny"}
         nuvlabox-engine-version])]
     [ui/Table {:basic  "very"
                :padded false}
      [ui/TableBody
       [ui/TableRow
        [ui/TableCell "Id"]
        (when id
          [ui/TableCell [values/as-link id :label (subs id 9)]])]
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:name]))]
        [EditableCell :name]]
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:description]))]
        [EditableCell :description]]
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:owner]))]
        [ui/TableCell @resolved-owner]]
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:telemetry-period]))]
        [ui/TableCell (str refresh-interval " seconds")]]
       (when nuvlabox-api-endpoint
         [ui/TableRow
          [ui/TableCell (str/capitalize (@tr [:nuvla-api-endpoint]))]
          [ui/TableCell nuvlabox-api-endpoint]])
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:created]))]
        [ui/TableCell (time/ago (time/parse-iso8601 created) @locale)]]
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:updated]))]
        [ui/TableCell (time/ago (time/parse-iso8601 updated) @locale)]]
       [ui/TableRow
        [ui/TableCell (str/capitalize (@tr [:version]))]
        [ui/TableCell version]]]]]))


(defn TabOverviewHost
  [{:keys [hostname ip docker-server-version
           operating-system architecture last-boot docker-plugins] :as nb-status}
   ssh-creds]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Segment {:secondary true
                 :color     "grey"
                 :raised    true}
     [:h4 "Host"]
     (if nb-status
       [ui/Table {:basic  "very"
                  :padded false}
        [ui/TableBody
         (when hostname
           [ui/TableRow
            [ui/TableCell (str/capitalize (@tr [:hostname]))]
            [ui/TableCell hostname]])
         (when operating-system
           [ui/TableRow
            [ui/TableCell (str/capitalize (@tr [:operating-system]))]
            [ui/TableCell operating-system]])
         (when architecture
           [ui/TableRow
            [ui/TableCell (str/capitalize (@tr [:architecture]))]
            [ui/TableCell architecture]])
         (when ip
           [ui/TableRow
            [ui/TableCell "IP"]
            [ui/TableCell (values/copy-value-to-clipboard
                            ip ip (@tr [:copy-to-clipboard]) true)]])
         (when (pos? (count @ssh-creds))
           [ui/TableRow
            [ui/TableCell (str/capitalize (@tr [:ssh-keys]))]
            [ui/TableCell [ui/Popup
                           {:hoverable true
                            :flowing   true
                            :position  "bottom center"
                            :content   (r/as-element [ui/ListSA {:divided true
                                                                 :relaxed true}
                                                      (for [sshkey @ssh-creds]
                                                        ^{:key (:id sshkey)}
                                                        [ui/ListItem
                                                         [ui/ListContent
                                                          [ui/ListHeader
                                                           [:a {:href   (str @config/path-prefix
                                                                             "/api/" (:id sshkey))
                                                                :target "_blank"}
                                                            (or (:name sshkey) (:id sshkey))]]
                                                          [ui/ListDescription
                                                           (str (subs (:public-key sshkey) 0 55) " ...")]]])])
                            :trigger   (r/as-element [ui/Icon {:name   "key"
                                                               :fitted true}
                                                      (@tr [:nuvlabox-detail-ssh-enabled])
                                                      [ui/Icon {:name   "angle down"
                                                                :fitted true}]])}]]])
         (when docker-server-version
           [ui/TableRow
            [ui/TableCell (str/capitalize (@tr [:docker-server-version]))]
            [ui/TableCell docker-server-version]])
         (when (seq docker-plugins)
           [ui/TableRow
            [ui/TableCell (str/capitalize (@tr [:docker-plugins]))]
            [ui/TableCell (str/join ", " docker-plugins)]])
         (when last-boot
           [ui/TableRow
            [ui/TableCell (str/capitalize (@tr [:last-boot]))]
            [ui/TableCell (time/time->format last-boot)]])]]
       ;else
       [ui/Message {:content (@tr [:nuvlabox-status-unavailable])}])]))


(defn TabOverviewStatus
  [{:keys [updated status status-notes] :as _nb-status} nb-id online-status]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Segment {:secondary true
                 :color     (utils/status->color online-status)
                 :raised    true}
     [:h4 "Status "]
     (when status
       [:div {:style {:margin "0.5em 0 1em 0"}}
        "Operational Status: "
        [ui/Popup
         {:trigger        (r/as-element [ui/Label {:circular   true
                                                   :size       "small"
                                                   :horizontal true
                                                   :color      (utils/operational-status->color status)}
                                         status])
          :content        (@tr [:nuvlabox-operational-status-popup])
          :position       "bottom center"
          :on             "hover"
          :size           "tiny"
          :hide-on-scroll true}]
        (when (not-empty status-notes)
          [ui/Message {:color "brown"
                       :size  "tiny"}
           [ui/MessageHeader
            [ui/Icon {:name "sticky note"}]
            "Notes"]
           [ui/MessageList {:items status-notes}]])])
     [Heartbeat updated nb-id]]))


(defn TabOverviewTags
  [{:keys [id] :as nuvlabox}]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Segment {:secondary true
                 :color     "teal"
                 :raised    true}
     [:h4 "Tags"]
     [components/EditableTags nuvlabox #(dispatch [::events/edit id {:tags %}
                                                   (@tr [:updated-successfully])])]]))


(defn TabOverviewCluster
  [{:keys [node-id cluster-id swarm-node-cert-expiry-date cluster-join-address
           cluster-node-role cluster-managers cluster-nodes orchestrator] :as _nuvlabox}]
  (let [tr (subscribe [::i18n-subs/tr])]
    [ui/Segment {:secondary true
                 :color     "black"
                 :raised    true}
     [:h4 (str/capitalize (@tr [:cluster])) " " (@tr [:status])
      (when orchestrator
        [ui/Label {:circular   true
                   :color      "blue"
                   :size       "tiny"
                   :basic      true
                   :float      "right"
                   :horizontal true
                   :style      {:float "right"}}
         [ui/Icon {:name (get orchestration-icons (keyword orchestrator) "question circle")}] orchestrator])]
     [ui/Table {:basic  "very"
                :padded false}
      [ui/TableBody
       [ui/TableRow
        [ui/TableCell "Node ID"]
        [ui/TableCell node-id]]
       (when cluster-id
         [ui/TableRow
          [ui/TableCell "Cluster ID"]
          [ui/TableCell cluster-id]])
       [ui/TableRow
        [ui/TableCell "Node Role"]
        [ui/TableCell cluster-node-role
         (when (= cluster-node-role "manager")
           [:<>
            (str " ")
            [ui/Icon {:className "fas fa-crown"
                      :corner    true
                      :color     "blue"}]])]]
       (when cluster-join-address
         [ui/TableRow
          [ui/TableCell "Cluster Join Address"]
          [ui/TableCell cluster-join-address]])
       (when cluster-managers
         [ui/TableRow
          [ui/TableCell "Cluster Managers"]
          [ui/TableCell
           [ui/LabelGroup {:size  "tiny"
                           :style {:margin-top 10, :max-height 150, :overflow "auto"}}
            (for [manager cluster-managers]
              ^{:key (str manager)}
              [ui/Label {:basic true
                         :style {:max-width     "15ch"
                                 :overflow      "hidden"
                                 :text-overflow "ellipsis"
                                 :white-space   "nowrap"}}
               manager])]]])
       (when cluster-nodes
         [ui/TableRow
          [ui/TableCell "Cluster Nodes"]
          [ui/TableCell
           [ui/LabelGroup {:size  "tiny"
                           :style {:margin-top 10, :max-height 150, :overflow "auto"}}
            (for [node cluster-nodes]
              ^{:key (str node)}
              [ui/Label {:basic true
                         :style {:max-width     "15ch"
                                 :overflow      "hidden"
                                 :text-overflow "ellipsis"
                                 :white-space   "nowrap"}}
               node])]]])

       (when swarm-node-cert-expiry-date
         [ui/TableRow
          [ui/TableCell "Swarm Certificate Expiry Date"]
          [ui/TableCell swarm-node-cert-expiry-date]])]]]))


(defn TabOverview
  []
  (let [nuvlabox  (subscribe [::subs/nuvlabox])
        nb-status (subscribe [::subs/nuvlabox-status])
        ssh-creds (subscribe [::subs/nuvlabox-associated-ssh-keys])]
    (fn []
      (let [{:keys [id tags ssh-keys]} @nuvlabox]
        (when (not= (count ssh-keys) (count @ssh-creds))
          (dispatch [::events/get-nuvlabox-associated-ssh-keys ssh-keys]))
        [ui/TabPane
         [ui/Grid {:columns   2,
                   :stackable true
                   :padded    true}
          [ui/GridRow
           [ui/GridColumn {:stretched true}
            [TabOverviewNuvlaBox @nuvlabox @nb-status]]

           [ui/GridColumn {:stretched true}
            [TabOverviewHost @nb-status ssh-creds]]]

          [ui/GridColumn {:stretched true}
           [TabOverviewStatus @nb-status id (:online @nuvlabox)]]

          [ui/GridColumn {:stretched true}
           [deployment-views/DeploymentsOverviewSegment
            ::deployment-subs/deployments ::events/set-active-tab-index tab-deployment-index]]

          (when (:node-id @nb-status)
            [ui/GridColumn {:stretched true}
             [TabOverviewCluster @nb-status]])

          (when (> (count tags) 0)
            [ui/GridColumn
             [TabOverviewTags @nuvlabox]])]]))))


(defn TabLocationMap
  []
  (let [tr                (subscribe [::i18n-subs/tr])
        nuvlabox          (subscribe [::subs/nuvlabox])
        zoom              (atom 3)
        new-location      (r/atom nil)]
    (fn []
      (let [{:keys [id location inferred-location]} @nuvlabox
            update-new-location #(reset! new-location %)
            position            (some-> (or @new-location location inferred-location) map/longlat->latlong)]
        [:div
         (if position (@tr [:map-drag-to-update-nb-location])
                      (@tr [:map-click-to-set-nb-location]))
         [map/MapBox
          {:style             {:height 400
                               :cursor (when-not location "pointer")}
           :center            (or position map/sixsq-latlng)
           :zoom              @zoom
           :onViewportChanged #(reset! zoom (.-zoom %))
           :on-click          (when-not position
                                (map/click-location update-new-location))}
          (when position
            [map/Marker {:position    position
                         :draggable   true
                         :on-drag-end (map/drag-end-location update-new-location)}])]
         [:div {:align "right"}
          [ui/Button {:on-click #(reset! new-location nil)}
           (@tr [:cancel])]
          [ui/Button {:primary  true
                      :on-click #(dispatch
                                   [::events/edit id
                                    (assoc @nuvlabox
                                      :location
                                      (update @new-location 0 map/normalize-lng))
                                    (@tr [:nuvlabox-position-update])])}
           (@tr [:save])]]]))))


(defn TabLocation
  []
  [ui/TabPane
   [TabLocationMap]])


(defn TabLoad
  []
  (let [tr              (subscribe [::i18n-subs/tr])
        nuvlabox-status (subscribe [::subs/nuvlabox-status])]
    (fn []
      (let [{:keys [resources]} @nuvlabox-status]
        [ui/TabPane
         (if resources
           [Load resources]
           [ui/Message
            {:warning true
             :content (@tr [:nuvlabox-resources-unavailable])}])]))))


(defn TabPeripherals
  []
  (let [peripherals-per-id (subscribe [::subs/nuvlabox-peripherals])]
    (fn []
      (let [peripheral-resources (into [] (map (fn [[_id res]] res) @peripherals-per-id))
            per-interface        (group-by :interface peripheral-resources)]
        [ui/TabPane
         (if (empty? peripheral-resources)
           [uix/WarningMsgNoElements]
           (for [[interface peripherals] per-interface]
             ^{:key interface}
             [uix/Accordion
              (for [id (map :id peripherals)]
                ^{:key id}
                [Peripheral id])
              :title-size :h4
              :default-open false
              :styled? false
              :count (count peripherals)
              :label interface]))]))))


(defn TabEvents
  []
  (let [tr                (subscribe [::i18n-subs/tr])
        nuvlabox          (subscribe [::subs/nuvlabox])
        all-events        (subscribe [::subs/nuvlabox-events])
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
                                                         (refresh (:id @nuvlabox))))}]]))))


; there's a similar function in edge.views which can maybe be generalized
(defn VulnStatisticState
  [value label label-popup color state-selector]
  (let [selected?      (or
                         (= label @state-selector)
                         (and (= label "collected")
                              (nil? @state-selector)))
        selected-style (if selected? {:color       "black"
                                      :font-weight "bold"
                                      :padding     "10px"}
                                     {:color       color
                                      :font-weight "normal"
                                      :padding     "3px"})]
    [ui/Statistic {:style    {:cursor "pointer"}
                   :on-click #(dispatch [::events/set-vuln-severity-selector
                                         (if (= label "collected") nil label)])}
     [ui/StatisticValue {:style selected-style}
      (or value "-")]
     [ui/StatisticLabel [ui/Popup
                         {:trigger        (r/as-element [:span {:style selected-style} label])
                          :content        label-popup
                          :position       "bottom center"
                          :on             "hover"
                          :size           "tiny"
                          :hide-on-scroll true}]]]))


(defn VulnerabilitiesTableBody
  [vulnerability-id product vulnerability-score color matching-vuln-db]
  [ui/TableRow
   [ui/TableCell (if matching-vuln-db
                   [:<>
                    vulnerability-id
                    " "
                    [ui/Popup
                     {:trigger        (r/as-element [ui/Icon {:name "info circle"}])
                      :header         vulnerability-id
                      :content        (r/as-element [:div
                                                     [:span (:description matching-vuln-db)]
                                                     [:hr]
                                                     [:a {:href   (:reference matching-vuln-db)
                                                          :target "_blank"}
                                                      (:reference matching-vuln-db)]])
                      :position       "right center"
                      :on             "hover"
                      :wide           "very"
                      :size           "tiny"
                      :hoverable      true
                      :hide-on-scroll true}]]
                   vulnerability-id
                   )]
   [ui/TableCell product]
   [ui/TableCell {:style {:background-color color
                          :font-weight      "bold"}} vulnerability-score]])


(defn TabVulnerabilities
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        state-selector (subscribe [::subs/vuln-severity-selector])
        vulns          (subscribe [::subs/nuvlabox-vulns])
        matching-vulns (subscribe [::subs/matching-vulns-from-db])]
    (fn []
      (let [summary        (:summary @vulns)
            items-extended (:items @vulns)
            items-severity (group-by :severity items-extended)
            vulns-in-db    @matching-vulns]
        [ui/TabPane
         (if @vulns
           [:<>
            [ui/Container {:text-align "center"
                           :style      {:margin "5px"}}
             [ui/Label {:basic true}
              (@tr [:nuvlabox-total-vuln])
              [ui/LabelDetail (:total summary)]]
             [ui/Label {:basic true}
              [ui/Popup
               {:trigger        (r/as-element [ui/Icon {:name "info circle"}])
                :content        (r/as-element
                                  [ui/ListSA {:bulleted true}
                                   (map (fn [k]
                                          ^{:key (random-uuid)}
                                          [ui/ListItem k]) (:affected-products summary))])
                :position       "bottom center"
                :on             "hover"
                :size           "tiny"
                :hide-on-scroll true}]
              (@tr [:nuvlabox-vuln-affected])
              [ui/LabelDetail (count (:affected-products summary))]]
             (when (:average-score summary)
               [ui/Label {:basic true}
                (@tr [:nuvlabox-vuln-cvss])
                [ui/LabelDetail (:average-score summary)]])]

            ; taking too much space at the moment. TBD later, once more plots are added
            ;[ui/Segment {:secondary   true
            ;             :color       "olive"
            ;             :raised      true}
            ; [plot/Polar {:height  80
            ;              :data    {:labels   (map (fn [[key v]] key) items-severity)
            ;                        :datasets [{:data            (map (fn [[k values]] (count values)) items-severity)
            ;                                    :backgroundColor (map (fn [[k v]] (:color (first v))) items-severity)}]}
            ;              :options {:title  {:display true,
            ;                                 :text    "Vulnerabilities by Severity"
            ;                                 :position    "top"},
            ;                        :legend {:display  true
            ;                                 :position "left"}}}]]

            [ui/Segment {:secondary true
                         :color     "brown"
                         :raised    true}
             [ui/StatisticGroup {:width (count items-severity)
                                 :size  "mini"
                                 :style {:display    "inline-block"
                                         :text-align "center"
                                         :width      "100%"
                                         :margin     "5px"}}

              [VulnStatisticState
               (count items-extended)
               "collected"
               (@tr [:nuvlabox-vuln-most-severe])
               "grey"
               state-selector]
              [VulnStatisticState
               (count (get items-severity "CRITICAL"))
               "critical"
               "CVSS: 9.0-10.0"
               utils/vuln-critical-color
               state-selector]
              [VulnStatisticState
               (count (get items-severity "HIGH"))
               "high"
               "CVSS: 7.0-8.9"
               utils/vuln-high-color
               state-selector]
              [VulnStatisticState
               (count (get items-severity "MEDIUM"))
               "medium"
               "CVSS: 4.0-6.9"
               utils/vuln-medium-color
               state-selector]
              [VulnStatisticState
               (count (get items-severity "LOW"))
               "low"
               "CVSS: 0.1-3.9"
               utils/vuln-low-color
               state-selector]
              [VulnStatisticState
               (count (get items-severity "UNKNOWN"))
               "unknown"
               "without CVSS score"
               utils/vuln-unknown-color
               state-selector]]

             [:div {:style {:max-height "25em"
                            :width      "100%"
                            :overflow-y "auto"}}
              [ui/Table {:basic "very"}
               [ui/TableHeader
                [ui/TableRow
                 [ui/TableHeaderCell "ID"]
                 [ui/TableHeaderCell "Product"]
                 [ui/TableHeaderCell "CVSS Score"]]]

               [ui/TableBody
                (if @state-selector
                  (for [{:keys [vulnerability-id product vulnerability-score color]}
                        (get items-severity (str/upper-case @state-selector))]
                    ^{:key vulnerability-id}
                    [VulnerabilitiesTableBody vulnerability-id product vulnerability-score color (get vulns-in-db vulnerability-id)])
                  (for [{:keys [vulnerability-id product vulnerability-score color]} items-extended]
                    ^{:key vulnerability-id}
                    [VulnerabilitiesTableBody vulnerability-id product vulnerability-score color (get vulns-in-db vulnerability-id)]))]]]]]

           [ui/Message {:content (@tr [:nuvlabox-vuln-unavailable])}])]))))


(defn tabs
  [count-peripherals]
  (let [tr        (subscribe [::i18n-subs/tr])
        nuvlabox  (subscribe [::subs/nuvlabox])
        can-edit? (subscribe [::subs/can-edit?])]
    [{:menuItem {:content "Overview"
                 :key     "overview"
                 :icon    "info"}
      :render   (fn [] (r/as-element [TabOverview]))}
     {:menuItem {:content "Location"
                 :key     "location"
                 :icon    "map"}
      :render   (fn [] (r/as-element [TabLocation]))}
     {:menuItem {:content (r/as-element
                            [ui/Popup
                             {:trigger        (r/as-element [:span "Resource Consumption"])
                              :content        (@tr [:nuvlabox-datagateway-popup])
                              :header         "data-gateway"
                              :position       "top center"
                              :wide           true
                              :on             "hover"
                              :size           "tiny"
                              :hide-on-scroll true}])
                 :key     "res-cons"
                 :icon    "thermometer half"}
      :render   (fn [] (r/as-element [TabLoad]))}
     {:menuItem {:content (r/as-element [:span "Peripherals"
                                         [ui/Label {:circular true
                                                    :size     "mini"
                                                    :attached "top right"}
                                          count-peripherals]])
                 :key     "peripherals"
                 :icon    "usb"}
      :render   (fn [] (r/as-element [TabPeripherals]))}
     {:menuItem {:content "Events"
                 :key     "events"
                 :icon    "bolt"}
      :render   (fn [] (r/as-element [TabEvents]))}
     {:menuItem {:content "Deployments"
                 :key     "deployments"
                 :icon    "rocket"}
      :render   (fn [] (r/as-element [deployment-views/DeploymentTable
                                      {:empty-msg (@tr [:empty-deployment-nuvlabox-msg])}]))}
     {:menuItem {:content "Vulnerabilities"
                 :key     "vuln"
                 :icon    "shield"}
      :render   (fn [] (r/as-element [TabVulnerabilities]))}
     (job-views/jobs-section)
     (acl/TabAcls nuvlabox @can-edit? ::events/edit)]))


(defn TabsNuvlaBox
  []
  (fn []
    (let [count-peripherals (subscribe [::subs/nuvlabox-peripherals-ids])
          active-index      (subscribe [::subs/active-tab-index])]
      [ui/Tab
       {:menu        {:secondary true
                      :pointing  true
                      :style     {:display        "flex"
                                  :flex-direction "row"
                                  :flex-wrap      "wrap"}}
        :panes       (tabs (count @count-peripherals))
        :activeIndex @active-index
        :onTabChange (fn [_ data]
                       (let [active-index (. data -activeIndex)]
                         (dispatch [::events/set-active-tab-index active-index])))}])))


(defn PageHeader
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        nuvlabox (subscribe [::subs/nuvlabox])]
    (fn []
      (let [{:keys [id name state online]} @nuvlabox]
        [:div
         [:h2 {:style {:margin "0 0 0 0"}}
          [ui/Icon {:name "box"}]
          (or name id)]
         [:p {:style {:margin "0.5em 0 1em 0"}}
          [OnlineStatusIcon online]
          [:span {:style {:font-weight "bold"}}
           (str/capitalize (@tr [:state]))
           (@tr [:colon])
           ff/nbsp]
          state]]))))


(defn EdgeDetails
  [uuid]
  (refresh uuid)
  (let [nb-status (subscribe [::subs/nuvlabox-status])]
    [components/LoadingPage {:dimmable? true}
     [:<>
      [components/NotFoundPortal
       ::subs/nuvlabox-not-found?
       :no-nuvlabox-message-header
       :no-nuvlabox-message-content]
      [ui/Container {:fluid true}
       [PageHeader]
       [MenuBar uuid]
       [components/ErrorJobsMessage ::job-subs/jobs ::events/set-active-tab-index 7]
       [job-views/ProgressJobAction @nb-status]
       [TabsNuvlaBox]]]]))
