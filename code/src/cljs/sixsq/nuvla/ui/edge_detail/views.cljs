(ns sixsq.nuvla.ui.edge-detail.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.acl.views :as acl]
    [sixsq.nuvla.ui.cimi-detail.views :as cimi-detail-views]
    [sixsq.nuvla.ui.config :as config]
    [sixsq.nuvla.ui.dashboard.events :as dashboard-events]
    [sixsq.nuvla.ui.dashboard.subs :as dashboard-subs]
    [sixsq.nuvla.ui.dashboard.views :as dashboard-views]
    [sixsq.nuvla.ui.edge-detail.events :as events]
    [sixsq.nuvla.ui.edge-detail.subs :as subs]
    [sixsq.nuvla.ui.edge.events :as edge-events]
    [sixsq.nuvla.ui.edge.subs :as edge-subs]
    [sixsq.nuvla.ui.edge.utils :as utils]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.job.subs :as job-subs]
    [sixsq.nuvla.ui.job.views :as job-views]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.utils.forms :as forms]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.map :as map]
    [sixsq.nuvla.ui.utils.plot :as plot]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as values]))


(def refresh-action-id :nuvlabox-get-nuvlabox)


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
  [operation on-change-fn]
  (let [tr       (subscribe [::i18n-subs/tr])
        is-add?  (= operation "add-ssh-key")
        ssh-keys (if is-add?
                   (r/atom nil)
                   (subscribe [::subs/nuvlabox-associated-ssh-keys]))]
    (when is-add?
      (dispatch [::events/get-ssh-keys-not-associated #(reset! ssh-keys %)]))
    (fn [operation on-change-fn]
      [ui/FormDropdown
       {:label       "SSH key"
        :loading     (nil? @ssh-keys)
        :placeholder (if is-add?
                       (@tr [:leave-empty-to-generate-ssh-keypair])
                       (@tr [:select-credential]))
        :on-change   (ui-callback/value on-change-fn)
        :clearable   true
        :options     (map (fn [{:keys [id name]}]
                            {:key id, :text (or name id), :value id}) @ssh-keys)
        :selection   true}])))



(defn DropdownReleases
  [opts]
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
  [{:keys [id] :as resource} operation show? title icon button-text]
  (let [tr            (subscribe [::i18n-subs/tr])
        close-fn      #(reset! show? false)
        form-data     (r/atom {:execution-mode "push"})
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
    (fn [resource operation show? title icon button-text]
      [ui/Modal
       {:open       @show?
        :close-icon true
        :on-close   close-fn
        :trigger    (r/as-element
                      [ui/MenuItem {:on-click #(reset! show? true)}
                       [ui/Icon {:name icon}]
                       title])}
       [ui/ModalHeader [:div title]]
       [ui/ModalContent
        [ui/Form
         [ui/FormDropdown {:label         "Execution mode"
                           :selection     true
                           :default-value (:execution-mode @form-data)
                           :on-change     (ui-callback/value (partial on-change-fn :execution-mode))
                           :options       [{:key "push", :text "push", :value "push"}
                                           {:key "mixed", :text "mixed", :value "mixed"}
                                           {:key "pull", :text "pull", :value "pull"}]}]
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
         {:text     (@tr [:cancel])
          :on-click close-fn}]
        [uix/Button
         {:text     (if @key-data (@tr [:close]) button-text)
          :primary  true
          :loading  (true? @loading?)
          :on-click (if @key-data close-fn on-click-fn)}]]])))


(defn UpdateButton
  [{:keys [id] :as resource} operation show? title icon button-text]
  (let [tr            (subscribe [::i18n-subs/tr])
        status        (subscribe [::subs/nuvlabox-status])
        close-fn      #(reset! show? false)
        form-data     (r/atom nil)
        on-change-fn  #(swap! form-data assoc :nuvlabox-release %)
        on-success-fn close-fn
        on-error-fn   close-fn
        on-click-fn   #(dispatch [::events/operation id operation @form-data
                                  on-success-fn on-error-fn])]
    (fn [{:keys [id] :as resource} operation show? title icon button-text]
      (when (not= (:parent @status))
        (dispatch [::events/get-nuvlabox id]))
      (let [correct-nb? (= (:parent @status) id)]
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
         [ui/ModalHeader [:div title]]
         [ui/ModalContent
          [ui/Segment
           [:b "Current Engine Version: "]
           [:i nil (when correct-nb? (get @status :nuvlabox-engine-version ""))]]
          [ui/Segment
           [:b "Update to: "]
           [DropdownReleases {:placeholder "select a version"
                              :on-change   (ui-callback/value #(on-change-fn %))}]]]
         [ui/ModalActions
          [uix/Button
           {:text     (@tr [:cancel])
            :on-click close-fn}]
          [uix/Button
           {:text     button-text
            :disabled (str/blank? (:nuvlabox-release @form-data))
            :primary  true
            :on-click on-click-fn}]]]))))


(defmethod cimi-detail-views/other-button ["nuvlabox" "add-ssh-key"]
  [resource operation]
  (let [tr    (subscribe [::i18n-subs/tr])
        show? (r/atom false)]
    (fn [resource operation]
      ^{:key (str "add-ssh-button" @show?)}
      [AddRevokeSSHButton resource operation show? "Add ssh key" "add" (@tr [:add])])))


(defmethod cimi-detail-views/other-button ["nuvlabox" "revoke-ssh-key"]
  [resource operation]
  (let [show? (r/atom false)]
    (fn [resource operation]
      ^{:key (str "revoke-ssh-button" @show?)}
      [AddRevokeSSHButton resource operation show? "Revoke ssh key" "minus" "revoke"])))


(defmethod cimi-detail-views/other-button ["nuvlabox" "update-nuvlabox"]
  [resource operation]
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
      [main-components/StickyBar
       [ui/Menu {:borderless true, :stackable true}
        (when @can-decommission?
          [DecommissionButton @nuvlabox])
        (when @can-delete?
          [DeleteButton @nuvlabox])

        [cimi-detail-views/format-operations @nuvlabox #{"edit" "delete" "activate" "decommission"
                                                         "commission" "check-api"}]

        [main-components/RefreshMenu
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
    (fn [id]
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
         [ui/Segment {:basic "very"}
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



(defn StatusIcon
  [status & {:keys [corner] :or {corner "bottom center"} :as position}]
  [ui/Popup
   {:position corner
    :content  status
    :trigger  (r/as-element
                [ui/Icon {:name  "power"
                          :color (utils/status->color status)}])}])


(defn Heartbeat
  [updated]
  (let [updated-moment           (time/parse-iso8601 updated)
        status                   (subscribe [::subs/nuvlabox-online-status])
        next-heartbeat-moment    (subscribe [::subs/next-heartbeat-moment])
        next-heartbeat-times-ago (time/ago @next-heartbeat-moment)

        last-heartbeat-msg       (if updated
                                   (str "Last heartbeat was " (time/ago updated-moment))
                                   "Heartbeat unavailable")

        next-heartbeat-msg       (if @next-heartbeat-moment
                                   (if (= @status :online)
                                     (str "Next heartbeat is expected " next-heartbeat-times-ago)
                                     (str "Next heartbeat was expected " next-heartbeat-times-ago)))]

    [ui/Message {:icon    "heartbeat"
                 :content (str last-heartbeat-msg ". " next-heartbeat-msg)}]))


(defn Load
  [resources]
  (let [load-stats      (utils/load-statistics resources)
        net-stats       (utils/load-net-stats (:net-stats resources))
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
               (last (:data-gateway stat))]]]]
           )


         ; TODO: the data-gateway stats should be in a popup instead of raw text. But fails some unknown reason,
         ;[ui/Popup
         ; {:trigger        (r/as-element [ui/Icon {:name "info circle"}])
         ;  :content        "Let your NuvlaBox apps subscribe to the internal MQTT to access these values locally"
         ;  :header         "data-gateway"
         ;  :position       "right center"
         ;  :inverted       true
         ;  :wide           true
         ;
         ;  :on             "hover"
         ;  :hide-on-scroll true}]
         ])]
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
                                                              :display     true}}]}}}]]]])]))


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


(defn EditAction
  [uuid body close-fn]
  (dispatch [::events/edit
             uuid body
             "NuvlaBox updated successfully"])
  (close-fn))


(defn TabOverviewNuvlaBox
  [{:keys [id name description created updated
           version refresh-interval owner] :as nuvlabox}
   {:keys [nuvlabox-api-endpoint nuvlabox-engine-version] :as nb-status}
   locale edit old-nb-name close-fn]
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
      [ui/TableCell "ID"]
      [ui/TableCell [values/as-link id :label id]]]
     (when name
       [ui/TableRow
        [ui/TableCell "Name"]
        [ui/TableCell [ui/Icon {:name     "pencil"
                                :on-click #(reset! edit true)
                                :style    {:cursor "pointer"}}]
         " "
         (if @edit
           [ui/Input {:default-value name
                      :on-key-press  (partial forms/on-return-key
                                              #(if (= @old-nb-name name id)
                                                 (close-fn)
                                                 (EditAction id {:name @old-nb-name} close-fn)))
                      :on-change     (ui-callback/input-callback #(reset! old-nb-name %))
                      :focus         true
                      :size          "mini"}]
           name)]])
     (when description
       [ui/TableRow
        [ui/TableCell "Description"]
        [ui/TableCell description]])
     [ui/TableRow
      [ui/TableCell "Owner"]
      [ui/TableCell [values/as-link owner :label (general-utils/id->short-uuid owner)]]]
     [ui/TableRow
      [ui/TableCell "Telemetry Period"]
      [ui/TableCell (str refresh-interval " seconds")]]
     (when nuvlabox-api-endpoint
       [ui/TableRow
        [ui/TableCell "NuvlaBox API Endpoint"]
        [ui/TableCell nuvlabox-api-endpoint]])
     [ui/TableRow
      [ui/TableCell "Created"]
      [ui/TableCell (time/ago (time/parse-iso8601 created) @locale)]]
     [ui/TableRow
      [ui/TableCell "Updated"]
      [ui/TableCell (time/ago (time/parse-iso8601 updated) @locale)]]
     [ui/TableRow
      [ui/TableCell "Version"]
      [ui/TableCell version]]]]])


(defn TabOverviewHost
  [{:keys [hostname ip docker-server-version
           operating-system architecture last-boot docker-plugins] :as nb-status}
   ssh-creds tr]
  [ui/Segment {:secondary true
               :color     "black"
               :raised    true}
   [:h4 "Host"]
   (if nb-status
     [ui/Table {:basic  "very"
                :padded false}
      [ui/TableBody
       (when hostname
         [ui/TableRow
          [ui/TableCell "Hostname"]
          [ui/TableCell hostname]])
       (when operating-system
         [ui/TableRow
          [ui/TableCell "OS"]
          [ui/TableCell operating-system]])
       (when architecture
         [ui/TableRow
          [ui/TableCell "Architecture"]
          [ui/TableCell architecture]])
       (when ip
         [ui/TableRow
          [ui/TableCell "IP"]
          [ui/TableCell ip]])
       (when (pos? (count @ssh-creds))
         [ui/TableRow
          [ui/TableCell "SSH Keys"]
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
          [ui/TableCell "Docker Server Version"]
          [ui/TableCell docker-server-version]])
       (when docker-plugins
         [ui/TableRow
          [ui/TableCell "Docker Plugins"]
          [ui/TableCell docker-plugins]])
       (when last-boot
         [ui/TableRow
          [ui/TableCell "Last Boot"]
          [ui/TableCell (time/time->format last-boot)]])]]
     ;else
     [ui/Message {:content (@tr [:nuvlabox-status-unavailable])}])])


(defn TabOverviewStatus
  [{:keys [updated status] :as nb-status} nb-id online-status tr]
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
        :hide-on-scroll true}]])
   [Heartbeat updated nb-id]])


(defn TabOverviewTags
  [{:keys [id tags] :as nuvlabox}]
  [ui/Segment {:secondary true
               :color     "teal"
               :raised    true}
   [:h4 "Tags"]
   [ui/LabelGroup {:size  "tiny"
                   :color "teal"
                   :style {:margin-top 10, :max-height 150, :overflow "auto"}}
    (for [tag tags]
      ^{:key (str id "-" tag)}
      [ui/Label {:style {:max-width     "15ch"
                         :overflow      "hidden"
                         :text-overflow "ellipsis"
                         :white-space   "nowrap"}}
       [ui/Icon {:name "tag"}] tag])]])


(defn TabOverview
  []
  (let [nuvlabox      (subscribe [::subs/nuvlabox])
        locale        (subscribe [::i18n-subs/locale])
        nb-status     (subscribe [::subs/nuvlabox-status])
        online-status (subscribe [::subs/nuvlabox-online-status])
        ssh-creds     (subscribe [::subs/nuvlabox-associated-ssh-keys])
        tr            (subscribe [::i18n-subs/tr])
        edit          (r/atom false)
        old-nb-name   (r/atom (:name @nuvlabox))
        close-fn      #(do
                         (reset! edit false)
                         (refresh (:id @nuvlabox)))]
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
            [TabOverviewNuvlaBox @nuvlabox @nb-status locale edit old-nb-name close-fn]]

           [ui/GridColumn {:stretched true}
            [TabOverviewHost @nb-status ssh-creds tr]]]

          [ui/GridRow
           [ui/GridColumn
            [TabOverviewStatus @nb-status id @online-status tr]]

           (when (> (count tags) 0)
             [ui/GridColumn
              [TabOverviewTags @nuvlabox]])]]]))))


(defn TabLocationMap
  [{:keys [id location] :as nuvlabox}]
  (let [tr           (subscribe [::i18n-subs/tr])
        zoom         (atom 3)
        new-location (r/atom nil)]
    (fn [{:keys [id location] :as nuvlabox}]
      (let [update-new-location #(reset! new-location %)
            position            (some-> (or @new-location location) map/longlat->latlong)]
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
                                    (assoc nuvlabox
                                      :location
                                      (update @new-location 0 map/normalize-lng))
                                    (@tr [:nuvlabox-position-update])])}
           (@tr [:save])]]]))))


(defn TabLocation
  []
  (let [nuvlabox (subscribe [::subs/nuvlabox])]
    [ui/TabPane
     [TabLocationMap @nuvlabox]]))


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
      (let [peripheral-resources (into [] (map (fn [[id res]] res) @peripherals-per-id))
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
             (for [{:keys [id content timestamp category] :as event} events]
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


(defn TabDeployments
  [uuid]
  (let [elements          (subscribe [::dashboard-subs/deployments])
        elements-per-page (subscribe [::dashboard-subs/elements-per-page])
        page              (subscribe [::dashboard-subs/page])
        loading?          (subscribe [::dashboard-subs/loading?])]
    (dashboard-views/refresh :init? true :nuvlabox (str "nuvlabox/" uuid))
    (fn [uuid]
      (let [total-elements (:count @elements)
            total-pages    (general-utils/total-pages total-elements @elements-per-page)
            deployments    (:resources @elements)]
        [ui/TabPane
         (if @loading?
           [ui/Loader {:active true
                       :inline "centered"}]
           [dashboard-views/vertical-data-table deployments])

         (when (pos? (:count @elements))
           [uix/Pagination {:totalPages   total-pages
                            :activePage   @page
                            :onPageChange (ui-callback/callback
                                            :activePage
                                            #(dispatch [::dashboard-events/set-page %]))}])]))))


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
  [vulnerability-id product vulnerability-score color]
  [ui/TableRow
   [ui/TableCell vulnerability-id]
   [ui/TableCell product]
   [ui/TableCell {:style {:background-color color
                          :font-weight      "bold"}} vulnerability-score]])


(defn TabVulnerabilities
  []
  (let [tr             (subscribe [::i18n-subs/tr])
        state-selector (subscribe [::subs/vuln-severity-selector])
        vulns          (subscribe [::subs/nuvlabox-vulns])]
    (fn []
      (let [summary        (:summary @vulns)
            items-extended (:items @vulns)
            items-severity (group-by :severity items-extended)]
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
                  (for [{:keys [vulnerability-id product vulnerability-score color] :as selected-severity}
                        (get items-severity (str/upper-case @state-selector))]
                    ^{:key vulnerability-id}
                    [VulnerabilitiesTableBody vulnerability-id product vulnerability-score color])
                  (for [{:keys [vulnerability-id product vulnerability-score color] :as item} items-extended]
                    ^{:key vulnerability-id}
                    [VulnerabilitiesTableBody vulnerability-id product vulnerability-score color]))]]]]]

           [ui/Message {:content (@tr [:nuvlabox-vuln-unavailable])}])]))))


(defn tabs
  [uuid count-peripherals tr]
  (let [nuvlabox  (subscribe [::subs/nuvlabox])
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
                              :content        (tr [:nuvlabox-datagateway-popup])
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
                 :icon    "sitemap"}
      :render   (fn [] (r/as-element [TabDeployments uuid]))}
     {:menuItem {:content "Vulnerabilities"
                 :key     "vuln"
                 :icon    "shield"}
      :render   (fn [] (r/as-element [TabVulnerabilities]))}
     (job-views/jobs-section)
     (acl/TabAcls nuvlabox @can-edit? ::events/edit)]))


(defn TabsNuvlaBox
  [uuid]
  (fn [uuid]
    (let [count-peripherals (subscribe [::subs/nuvlabox-peripherals-ids])
          tr                (subscribe [::i18n-subs/tr])
          active-index      (subscribe [::subs/active-tab-index])]
      [ui/Tab
       {:menu        {:secondary true
                      :pointing  true
                      :style     {:display        "flex"
                                  :flex-direction "row"
                                  :flex-wrap      "wrap"}}
        :panes       (tabs uuid (count @count-peripherals) @tr)
        :activeIndex @active-index
        :onTabChange (fn [_ data]
                       (let [active-index (. data -activeIndex)]
                         (dispatch [::events/set-active-tab-index active-index])))}])))


(defn PageHeader
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        nuvlabox (subscribe [::subs/nuvlabox])
        status   (subscribe [::subs/nuvlabox-online-status])]
    (fn []
      (let [id    (:id @nuvlabox)
            name  (:name @nuvlabox)
            state (:state @nuvlabox)]
        [:div
         [:h2 {:style {:margin "0 0 0 0"}}
          [StatusIcon @status :corner "left center"]
          (or name id)]
         [:p {:style {:margin "0.5em 0 1em 0"}}
          [:span {:style {:font-weight "bold"}}
           "State "
           [ui/Popup
            {:trigger        (r/as-element [ui/Icon {:name "question circle"}])
             :content        (@tr [:nuvlabox-state])
             :position       "bottom center"
             :on             "hover"
             :size           "tiny"
             :hide-on-scroll true}] ": "]
          state]]))))


(def errors-dissmissed (r/atom #{}))

(defn Error
  []
  (let [jobs (subscribe [::job-subs/jobs])]
    (fn []
      (let [{:keys [id action state status-message] :as last-job} (some-> @jobs :resources first)]
        (when (and (not (@errors-dissmissed id))
                   (= state "FAILED"))
          [ui/Message {:error      true
                       :on-dismiss #(swap! errors-dissmissed conj id)}
           [ui/MessageHeader
            {:style    {:cursor "pointer"}
             :on-click #(dispatch [::events/set-active-tab-index 7])}
            (str "Job " action " failed")]
           [ui/MessageContent (last (str/split-lines (or status-message "")))]])))))


(defn EdgeDetails
  [uuid]
  (refresh uuid)
  (fn [uuid]
    ^{:key uuid}
    [ui/Container {:fluid true}
     [PageHeader]
     [MenuBar uuid]
     [Error]
     [TabsNuvlaBox uuid]]))
