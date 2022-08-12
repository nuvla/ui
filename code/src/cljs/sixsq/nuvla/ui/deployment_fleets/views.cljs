(ns sixsq.nuvla.ui.deployment-fleets.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-fx]
    [sixsq.nuvla.ui.edges-detail.views :as edges-detail]
    [sixsq.nuvla.ui.deployment-fleets-detail.views :as detail]
    [sixsq.nuvla.ui.edges.events :as edges-events]
    [sixsq.nuvla.ui.deployment-fleets.events :as events]
    [sixsq.nuvla.ui.deployment-fleets.subs :as subs]
    [sixsq.nuvla.ui.edges.subs :as edges-subs]
    [sixsq.nuvla.ui.edges.utils :as utils]
    [sixsq.nuvla.ui.edges.views-utils :as views-utils]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as components]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.forms :as utils-forms]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.map :as map]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as values]
    [sixsq.nuvla.ui.filter-comp.views :as filter-comp]
    [sixsq.nuvla.ui.utils.time :as time]))


(def view-type (r/atom :cards))

(def ^:const STARTED "STARTED")
(def ^:const CREATED "CREATED")
(def ^:const STOPPED "STOPPED")
(def ^:const PENDING "PENDING")

(defn state->icon
  [state]
  (if (str/ends-with? state "ING")
    "sync"
    (get {STARTED "play"
          STOPPED "stop"
          CREATED "circle outline"} state)))


(defn StatisticStates
  [clickable?]
  (let [summary  (subscribe [::subs/deployment-fleets-summary])
        terms    (general-utils/aggregate-to-map
                   (get-in @summary [:aggregations :terms:state :buckets]))
        started  (:STARTED terms 0)
        starting (:STARTING terms 0)
        creating (:CREATING terms 0)
        created  (:CREATED terms 0)
        stopping (:STOPPING terms 0)
        stopped  (:STOPPED terms 0)
        pending  (+ starting creating stopping)
        total    (:count @summary)]
    [ui/GridColumn {:width 8}
     [ui/StatisticGroup {:size  "tiny"
                         :style {:justify-content "center"
                                 :padding-top     "20px"
                                 :padding-bottom  "20px"}}
      [components/StatisticState total ["fas fa-bullseye"] "TOTAL" clickable?
       ::events/set-state-selector ::subs/state-selector]
      [components/StatisticState created [(state->icon CREATED)] CREATED
       clickable? "blue"
       ::events/set-state-selector ::subs/state-selector]
      [components/StatisticState started [(state->icon STARTED)] STARTED
       clickable? "green"
       ::events/set-state-selector ::subs/state-selector]
      [components/StatisticState stopped [(state->icon STOPPED)] STOPPED
       clickable? "red"
       ::events/set-state-selector ::subs/state-selector]
      [components/StatisticState pending [(state->icon PENDING)] PENDING
       clickable? "brown"
       ::events/set-state-selector ::subs/state-selector]]]))


(defn MenuBar []
  (let [loading? (subscribe [::edges-subs/loading?])]
    (fn []
      [components/StickyBar
       [ui/Menu {:borderless true, :stackable true}
        [views-utils/AddButton]
        [ui/MenuItem {:icon     "grid layout"
                      :active   (= @view-type :cards)
                      :on-click #(reset! view-type :cards)}]
        [ui/MenuItem {:icon     "table"
                      :active   (= @view-type :table)
                      :on-click #(reset! view-type :table)}]
        [components/RefreshMenu
         {:action-id  events/refresh-id
          :loading?   @loading?
          :on-refresh #(dispatch [::events/refresh])}]]])))


(defn NuvlaDocs
  [tr url]
  [ui/Container {:text-align :center
                 :style      {:margin "0.5em"}}
   [:span (@tr [:nuvlabox-documentation])
    [:a {:href   url
         :target "_blank"}
     (@tr [:nuvla-docs])]]])


(defn CreateSSHKeyMessage
  [new-private-ssh-key private-ssh-key-file tr]
  [ui/Message {:icon    true
               :warning true}
   [ui/Icon {:name "warning"}]
   [ui/MessageContent
    [ui/MessageHeader
     [:span
      [:a {:href     (str "data:text/plain;charset=utf-8,"
                          (js/encodeURIComponent new-private-ssh-key))
           :target   "_blank"
           :download private-ssh-key-file
           :key      private-ssh-key-file}
       [ui/Icon {:name "privacy"}] private-ssh-key-file]]]
    (@tr [:nuvlabox-modal-private-ssh-key-info])]])

(defn CreatedNuvlaBox
  [nuvlabox-id _creation-data nuvlabox-release-data nuvlabox-ssh-keys
   _new-private-ssh-key playbooks-toggle _on-close-fn]
  (let [nuvlabox-release     (:nb-selected nuvlabox-release-data)
        nuvlabox-peripherals (:nb-assets nuvlabox-release-data)
        playbooks-cronjob    (subscribe [::edges-subs/nuvlabox-playbooks-cronjob])
        private-ssh-key-file (str (general-utils/id->short-uuid nuvlabox-id) ".ssh.private")
        public-keys          (if @nuvlabox-ssh-keys
                               (str (str/join "\\n" (:public-keys @nuvlabox-ssh-keys)) "\\n")
                               nil)
        zip-url              (r/atom nil)
        envsubst             (if public-keys
                               [#"\$\{NUVLABOX_UUID\}" nuvlabox-id
                                #"\$\{NUVLABOX_SSH_PUB_KEY\}" public-keys]
                               [#"\$\{NUVLABOX_UUID\}" nuvlabox-id])
        download-files       (utils/prepare-compose-files nuvlabox-release nuvlabox-peripherals envsubst)]
    (when playbooks-toggle
      (dispatch [::edges-events/enable-host-level-management nuvlabox-id]))
    (when @nuvlabox-ssh-keys
      (dispatch [::edges-events/assign-ssh-keys @nuvlabox-ssh-keys nuvlabox-id]))
    (fn [nuvlabox-id creation-data _nuvlabox-release-data _nuvlabox-ssh-keys
         new-private-ssh-key playbooks-toggle on-close-fn]
      (let [tr                  (subscribe [::i18n-subs/tr])
            nuvlabox-name-or-id (str "NuvlaEdge " (or (:name creation-data)
                                                      (general-utils/id->short-uuid nuvlabox-id)))
            execute-command     (str "docker-compose -p nuvlabox -f "
                                     (str/join " -f " (map :name download-files)) " up -d")]
        [:<>
         [uix/ModalHeader {:header (str nuvlabox-name-or-id " created") :icon "box"}]

         (when (or @new-private-ssh-key playbooks-toggle)
           [ui/Segment {:basic true}
            (when playbooks-toggle
              [ui/Message {:icon true}
               [ui/Icon {:name    (if @playbooks-cronjob "check circle outline" "spinner")
                         :loading (if @playbooks-cronjob false true)}]
               [ui/MessageContent
                [ui/MessageHeader [:span (@tr [:nuvlabox-playbooks-cronjob]) " "
                                   (when @playbooks-cronjob
                                     [ui/Popup {:content        @playbooks-cronjob
                                                :wide           "very"
                                                :position       "bottom center"
                                                :hide-on-scroll true
                                                :hoverable      true
                                                :trigger        (r/as-element [ui/IconGroup
                                                                               [ui/Icon {:name "eye"}]
                                                                               [ui/Icon {:corner true
                                                                                         :name   "exclamation"}]])}])]]
                (if @playbooks-cronjob
                  [:span (str (@tr [:nuvlabox-playbooks-cronjob-ready])
                              " ")
                   (values/copy-value-to-clipboard
                     "" @playbooks-cronjob (@tr [:copy-to-clipboard]) true)]
                  (@tr [:nuvlabox-playbooks-cronjob-wait]))]])

            (when @new-private-ssh-key
              [CreateSSHKeyMessage @new-private-ssh-key private-ssh-key-file tr])])

         [ui/ModalContent
          [ui/Container
           [ui/CardGroup {:centered true}
            [ui/Card
             [ui/CardContent {:text-align :center}
              [ui/Header [:span {:style {:overflow-wrap "break-word"}} nuvlabox-name-or-id]]
              [ui/Icon {:name  "box"
                        :color "green"
                        :size  :massive}]]
             [ui/CopyToClipboard {:text nuvlabox-id}
              [ui/Button {:positive true
                          :icon     "clipboard"
                          :content  (@tr [:copy-nuvlabox-id])}]]]]

           [ui/Divider {:horizontal true}
            [ui/Header (@tr [:nuvlabox-quick-install])]]

           [ui/SegmentGroup {:raised true}
            [ui/Segment {:loading    (nil? @zip-url)
                         :text-align :center}
             [ui/Label {:circular true
                        :color    "green"} "1"]
             [:h5 {:style {:margin "0.5em 0 1em 0"}}
              (str/capitalize (@tr [:download])) " compose file(s)"]
             [:a {:href     @zip-url
                  :target   "_blank"
                  :style    {:margin "1em"}
                  :download "nuvlabox-engine.zip"} "nuvlabox-engine.zip " [ui/Icon {:name "download"}]]]

            [ui/Segment {:text-align :center}
             [ui/Label {:circular true
                        :color    "green"} "2"]
             [:h5 {:style {:margin "0.5em 0 1em 0"}}
              (@tr [:nuvlabox-unzip-execute])
              (values/copy-value-to-clipboard "" execute-command (@tr [:copy-command-to-clipboard]))]
             [:span {:style {:font "1em Inconsolata, monospace"}} execute-command]]]

           ]]

         [ui/ModalActions
          [ui/Button {:positive true
                      :on-click on-close-fn} (@tr [:close])]]]))))


(defn CreatedNuvlaBoxUSBTrigger
  [_creation-data nuvlabox-release-data _new-api-key _nuvlabox-ssh-keys _new-private-ssh-key _on-close-fn]
  (let [nuvlabox-release     (:nb-selected nuvlabox-release-data)
        nuvlabox-peripherals (:nb-assets nuvlabox-release-data)
        private-ssh-key-file "nuvlabox.ssh.private"
        download-files       (utils/prepare-compose-files nuvlabox-release nuvlabox-peripherals
                                                          [#"placeholder" "placeholder"])
        download-files-names (map :name download-files)]
    (fn [creation-data _nuvlabox-release-data new-api-key nuvlabox-ssh-keys new-private-ssh-key on-close-fn]
      (let [tr                    (subscribe [::i18n-subs/tr])
            apikey                (:resource-id new-api-key)
            apisecret             (:secret-key new-api-key)
            nb-trigger-file-base  {:assets      download-files-names
                                   :version     (:release nuvlabox-release)
                                   :name        (:name creation-data)
                                   :description (:description creation-data)
                                   :script      (str @cimi-fx/NUVLA_URL
                                                     "/ui/downloads/nuvlabox-self-registration.py.gpg")
                                   :endpoint    @cimi-fx/NUVLA_URL
                                   :vpn         (:vpn-server-id creation-data)
                                   :apikey      apikey
                                   :apisecret   apisecret}
            nuvlabox-trigger-file (if @nuvlabox-ssh-keys
                                    (assoc nb-trigger-file-base :ssh @nuvlabox-ssh-keys)
                                    nb-trigger-file-base)]
        [:<>
         [uix/ModalHeader {:header (@tr [:nuvlabox-modal-usb-header]) :icon "usb"}]

         [ui/Segment {:basic true}
          [ui/Message {:icon true}
           [ui/Icon {:name (if apikey "check circle outline" "spinner")}]
           [ui/MessageContent
            [ui/MessageHeader
             (@tr [:nuvlabox-usb-key])]
            (if apikey
              [:span (str (@tr [:nuvlabox-usb-key-ready]) " ")
               [:a {:href   (str "api/" apikey)
                    :target "_blank"}
                apikey] " "
               [ui/Popup {:content (@tr [:nuvlabox-modal-usb-apikey-warning])
                          :trigger (r/as-element [ui/Icon {:name  "exclamation triangle"
                                                           :color "orange"}])}]]
              (@tr [:nuvlabox-usb-key-wait]))]]
          (when @new-private-ssh-key
            [CreateSSHKeyMessage @new-private-ssh-key private-ssh-key-file tr])]

         [ui/ModalContent
          [ui/Container
           [ui/CardGroup {:centered true}
            [ui/Card
             [ui/CardContent {:text-align :center}
              [ui/Header [:span {:style {:overflow-wrap "break-word"}}
                          (@tr [:nuvlabox-modal-usb-trigger-file])]]
              [ui/Icon {:name    (if apikey "file code" "spinner")
                        :loading (nil? apikey)
                        :color   "green"
                        :size    :massive}]]
             [:a {:href     (str "data:text/plain;charset=utf-8,"
                                 (js/encodeURIComponent
                                   (general-utils/edn->json nuvlabox-trigger-file)))
                  :target   "_blank"
                  :download "nuvlabox-installation-trigger-usb.nuvla"}
              [ui/Button {:positive       true
                          :fluid          true
                          :loading        (nil? apikey)
                          :icon           "download"
                          :label-position "left"
                          :as             "div"
                          :content        (@tr [:download])}]]]]

           [ui/Divider {:horizontal true}
            [ui/Header (@tr [:instructions])]]

           [ui/SegmentGroup {:raised true}
            [ui/Segment {:loading    (nil? nuvlabox-trigger-file)
                         :text-align :center
                         :raised     true}
             [ui/Label {:circular true
                        :color    "green"} "1"]
             [:h5 {:style {:margin "0.5em 0 1em 0"}}
              (@tr [:nuvlabox-modal-usb-copy])
              [ui/Popup {:content (@tr [:nuvlabox-modal-usb-copy-warning])
                         :trigger (r/as-element [ui/Icon {:name "info circle"}])}]]]

            [ui/Segment {:text-align :center
                         :raised     true}
             [ui/Label {:circular true
                        :color    "green"} "2"]
             [:h5 {:style {:margin "0.5em 0 1em 0"}}
              (@tr [:nuvlabox-modal-usb-plug])]
             [:span (@tr [:nuvlabox-modal-usb-plug-info])]]

            [ui/Segment {:text-align :center
                         :raised     true}
             [ui/Label {:circular true
                        :color    "green"} "3"]
             [:h5 {:style {:margin "0.5em 0 1em 0"}}
              (@tr [:repeat])]
             [:span (@tr [:repeat-info])]]]]
          ]

         [ui/ModalActions
          [ui/Button {:positive true
                      :on-click on-close-fn} (@tr [:close])]]]))))


(defn AddModal
  []
  (let [modal-id                   :add
        tr                         (subscribe [::i18n-subs/tr])
        visible?                   (subscribe [::edges-subs/modal-visible? modal-id])
        nuvlabox-id                (subscribe [::edges-subs/nuvlabox-created-id])
        usb-api-key                (subscribe [::edges-subs/nuvlabox-usb-api-key])
        vpn-infra-opts             (subscribe [::edges-subs/vpn-infra-options])
        nb-releases                (subscribe [::edges-subs/nuvlabox-releases])
        ssh-credentials            (subscribe [::edges-subs/ssh-keys-available])
        nb-releases-by-id          (group-by :id @nb-releases)
        first-nb-release           (->> @nb-releases
                                        (remove :pre-release)
                                        first)
        default-major-version      (->> first-nb-release :release utils/get-major-version general-utils/str->int)
        default-data               {:refresh-interval 30, :version default-major-version}
        creation-data              (r/atom default-data)
        default-release-data       {:nb-rel      (:id first-nb-release)
                                    :nb-selected first-nb-release
                                    :nb-assets   (->> first-nb-release
                                                      :compose-files
                                                      (map :scope)
                                                      set)}
        nuvlabox-release-data      (r/atom default-release-data)
        advanced?                  (r/atom false)
        install-strategy-default   nil
        install-strategy           (r/atom install-strategy-default)
        install-strategy-error     (r/atom install-strategy-default)
        create-usb-trigger-default false
        create-usb-trigger         (r/atom create-usb-trigger-default)
        ; default ttl for API key is 30 days
        default-ttl                30
        usb-trigger-key-ttl        (r/atom default-ttl)
        playbooks-toggle           (r/atom false)
        ssh-toggle                 (r/atom false)
        ssh-existing-key           (r/atom false)
        ssh-chosen-keys            (r/atom [])
        nuvlabox-ssh-keys          (subscribe [::edges-subs/nuvlabox-ssh-key])
        new-private-ssh-key        (subscribe [::edges-subs/nuvlabox-private-ssh-key])
        creating                   (r/atom false)
        on-close-fn                #(do
                                      (dispatch [::edges-events/set-created-nuvlabox-id nil])
                                      (dispatch [::edges-events/set-nuvlabox-usb-api-key nil])
                                      (dispatch [::edges-events/set-nuvlabox-ssh-keys nil])
                                      (dispatch [::edges-events/set-nuvlabox-created-private-ssh-key nil])
                                      (dispatch [::edges-events/open-modal nil])
                                      (reset! advanced? false)
                                      (reset! playbooks-toggle false)
                                      (reset! ssh-toggle false)
                                      (reset! ssh-existing-key false)
                                      (reset! ssh-chosen-keys [])
                                      (reset! creation-data default-data)
                                      (reset! install-strategy install-strategy-default)
                                      (reset! usb-trigger-key-ttl default-ttl)
                                      (reset! install-strategy-error install-strategy-default)
                                      (reset! create-usb-trigger create-usb-trigger-default)
                                      (reset! nuvlabox-release-data default-release-data)
                                      (reset! creating false))
        on-add-fn                  #(cond
                                      (nil? @install-strategy) (reset! install-strategy-error true)
                                      :else (do
                                              (reset! creating true)
                                              (if @ssh-toggle
                                                (if @ssh-existing-key
                                                  (when (not-empty @ssh-chosen-keys)
                                                    (dispatch [::edges-events/find-nuvlabox-ssh-keys
                                                               @ssh-chosen-keys
                                                               (if (= @install-strategy "usb")
                                                                 [::edges-events/create-nuvlabox-usb-api-key
                                                                  @usb-trigger-key-ttl]
                                                                 [::edges-events/create-nuvlabox
                                                                  (->> @creation-data
                                                                       (remove (fn [[_ v]]
                                                                                 (str/blank? v)))
                                                                       (into {}))])]))
                                                  ; else, create new one
                                                  (let [ssh-desc "SSH credential generated for NuvlaEdge: "
                                                        ssh-tpl  {:name        (str "SSH key for " (:name @creation-data))
                                                                  :description (str ssh-desc (:name @creation-data))
                                                                  :template    {:href "credential-template/generate-ssh-key"}}]
                                                    (dispatch [::edges-events/create-ssh-key ssh-tpl
                                                               (if (= @install-strategy "usb")
                                                                 [::edges-events/create-nuvlabox-usb-api-key
                                                                  @usb-trigger-key-ttl]
                                                                 [::edges-events/create-nuvlabox
                                                                  (->> @creation-data
                                                                       (remove (fn [[_ v]]
                                                                                 (str/blank? v)))
                                                                       (into {}))])])))
                                                (if (= @install-strategy "usb")
                                                  (dispatch [::edges-events/create-nuvlabox-usb-api-key
                                                             @usb-trigger-key-ttl])
                                                  (dispatch [::edges-events/create-nuvlabox
                                                             (->> @creation-data
                                                                  (remove (fn [[_ v]]
                                                                            (str/blank? v)))
                                                                  (into {}))])))))]

    (dispatch [::edges-events/get-ssh-keys-available ["ssh-key"] nil])
    (dispatch [::edges-events/get-vpn-infra])
    (fn []
      (when (and (= (count @vpn-infra-opts) 1)
                 (nil? (:vpn-server-id @creation-data)))
        (swap! creation-data assoc :vpn-server-id (-> @vpn-infra-opts first :value)))
      [ui/Modal {:open       @visible?
                 :close-icon true
                 :on-close   on-close-fn}
       (cond
         @nuvlabox-id [CreatedNuvlaBox @nuvlabox-id @creation-data @nuvlabox-release-data
                       nuvlabox-ssh-keys new-private-ssh-key @playbooks-toggle on-close-fn]
         @usb-api-key [CreatedNuvlaBoxUSBTrigger @creation-data @nuvlabox-release-data @usb-api-key
                       nuvlabox-ssh-keys new-private-ssh-key on-close-fn]
         :else [:<>
                [uix/ModalHeader {:header (str (@tr [:nuvlabox-modal-new-nuvlabox])
                                               " " (:name @creation-data))
                                  :icon   "add"}]

                [ui/ModalContent
                 [ui/Divider {:horizontal true :as "h3"}
                  (@tr [:nuvlabox-modal-general])]

                 [ui/Table style/definition
                  [ui/TableBody
                   [uix/TableRowField (@tr [:name]), :on-change #(swap! creation-data assoc :name %),
                    :default-value (:name @creation-data)]
                   [uix/TableRowField (@tr [:description]), :type :textarea,
                    :on-change #(swap! creation-data assoc :description %)
                    :default-value (:name @creation-data)]
                   [ui/TableRow
                    [ui/TableCell {:collapsing true} "vpn"]
                    ^{:key (or key name)}
                    [ui/TableCell
                     [ui/Dropdown {:clearable   true
                                   :selection   true
                                   :fluid       true
                                   :placeholder (@tr [:none])
                                   :value       (:vpn-server-id @creation-data)
                                   :on-change   (ui-callback/callback
                                                  :value #(swap! creation-data assoc
                                                                 :vpn-server-id %))
                                   :options     @vpn-infra-opts}]]]]]

                 [ui/Checkbox {:toggle    true
                               :label     (@tr [:nuvlabox-modal-add-ssh-key])
                               :checked   @ssh-toggle
                               :on-change #(do
                                             (swap! ssh-toggle not)
                                             (reset! ssh-chosen-keys [])
                                             (reset! ssh-existing-key false))}]

                 [ui/Segment {:style {:display (if @ssh-toggle "block" "none")}}
                  [ui/Form
                   [ui/FormGroup {:inline true}

                    [ui/FormCheckbox {:label     (@tr [:nuvlabox-modal-add-new-ssh-key])
                                      :radio     true
                                      :checked   (not @ssh-existing-key)
                                      :on-change #(do
                                                    (swap! ssh-existing-key not))}]

                    [ui/FormCheckbox {:label     (@tr [:nuvlabox-modal-add-existing-ssh-key])
                                      :radio     true
                                      :checked   @ssh-existing-key
                                      :on-change #(do
                                                    (swap! ssh-existing-key not))}]]]

                  (when @ssh-existing-key
                    (if (pos-int? (count @ssh-credentials))
                      [ui/Dropdown {:search      true
                                    :multiple    true
                                    :selection   true
                                    :fluid       true
                                    :placeholder (@tr [:nuvlabox-modal-select-existing-ssh-key])

                                    :on-change   (ui-callback/callback
                                                   :value #(reset! ssh-chosen-keys %))
                                    :options     (map (fn [{id :id, name :name}]
                                                        {:key id, :value id, :text (or name id)})
                                                      @ssh-credentials)}]

                      [ui/Message {:content (str/capitalize
                                              (@tr [:nuvlabox-modal-no-ssh-keys-avail]))}]))]

                 (let [{nb-rel                                          :nb-rel
                        nb-assets                                       :nb-assets
                        {:keys [release compose-files url pre-release]} :nb-selected}
                       @nuvlabox-release-data]
                   [ui/Container
                    [ui/Divider {:horizontal true :as "h3"}
                     (@tr [:version])]
                    [edges-detail/DropdownReleases
                     {:placeholder release
                      :value       nb-rel
                      :on-change   (ui-callback/value
                                     (fn [value]
                                       (swap! nuvlabox-release-data
                                              assoc :nb-rel value)
                                       (let [nb-selected (->> value
                                                              (get nb-releases-by-id)
                                                              first)]
                                         (swap! creation-data assoc
                                                :version (-> nb-selected
                                                             :release
                                                             utils/get-major-version
                                                             general-utils/str->int))
                                         (swap! nuvlabox-release-data
                                                assoc :nb-selected nb-selected)
                                         (swap! nuvlabox-release-data assoc :nb-assets
                                                (set (map :scope (:compose-files nb-selected)))))
                                       ))}]
                    [:a {:href   url
                         :target "_blank"
                         :style  {:margin "1em"}}
                     (@tr [:nuvlabox-release-notes])]
                    (when pre-release
                      [ui/Popup
                       {:trigger        (r/as-element [ui/Icon {:name "exclamation triangle"}])
                        :content        (@tr [:nuvlabox-pre-release])
                        :on             "hover"
                        :hide-on-scroll true}])
                    [ui/Container
                     (when (> (count compose-files) 1)
                       [ui/Popup
                        {:trigger        (r/as-element [:span (@tr [:additional-modules])])
                         :content        (str (@tr [:additional-modules-popup]))
                         :on             "hover"
                         :hide-on-scroll true}])
                     (doall
                       (for [{:keys [scope]} compose-files]
                         (when-not (#{"core" ""} scope)
                           [ui/Checkbox {:key       scope
                                         :label     scope
                                         :checked   (contains?
                                                      (:nb-assets @nuvlabox-release-data)
                                                      scope)
                                         :style     {:margin "1em"}
                                         :on-change (ui-callback/checked
                                                      (fn [checked]
                                                        (if checked
                                                          (swap! nuvlabox-release-data assoc
                                                                 :nb-assets
                                                                 (conj nb-assets scope))
                                                          (swap! nuvlabox-release-data assoc
                                                                 :nb-assets
                                                                 (-> @nuvlabox-release-data
                                                                     :nb-assets
                                                                     (disj scope))))))}])))]

                    [ui/Divider {:horizontal true :as "h3"}
                     (@tr [:nuvlabox-modal-install-method])]

                    [ui/Form
                     [ui/Segment {:raised true}

                      [ui/FormCheckbox {:label     "Compose file bundle"
                                        :radio     true
                                        :error     (not (nil? @install-strategy-error))
                                        :checked   (= @install-strategy "compose")
                                        :on-change #(do
                                                      (reset! install-strategy "compose")
                                                      (reset! install-strategy-error nil))}]

                      [:div {:style {:color "grey" :font-style "oblique"}}
                       (@tr [:create-nuvlabox-compose])]

                      [:div {:style {:margin  "10px 5px 5px 5px"
                                     :display (if (= @install-strategy "compose")
                                                "block" "none")}}
                       [ui/Checkbox {:toggle    true
                                     :label     (@tr [:nuvlabox-modal-enable-playbooks])
                                     :checked   @playbooks-toggle
                                     :on-change #(swap! playbooks-toggle not)}]
                       [ui/Popup
                        {:trigger        (r/as-element [ui/Icon {:name  "info circle"
                                                                 :style {:margin-left "1em"}}])
                         :content        (@tr [:nuvlabox-modal-enable-playbooks-info])
                         :on             "hover"
                         :hide-on-scroll true}]]


                      [ui/Divider {:fitted     true
                                   :horizontal true
                                   :style      {:text-transform "lowercase"
                                                :margin         "10px 0"}} "Or"]

                      [ui/FormCheckbox {:label     "USB stick"
                                        :radio     true
                                        :error     (not (nil? @install-strategy-error))
                                        :checked   (= @install-strategy "usb")
                                        :on-change #(do
                                                      (reset! install-strategy "usb")
                                                      (reset! install-strategy-error nil)
                                                      (reset! playbooks-toggle false))}]

                      [:div {:style {:color "grey" :font-style "oblique"}}
                       (@tr [:create-nuvlabox-usb])]

                      [:div {:style {:margin  "10px 5px 5px 5px"
                                     :display (if (= @install-strategy "usb")
                                                "block" "none")}}
                       [ui/Input {:label       (@tr [:nuvlabox-modal-usb-expires])
                                  :placeholder default-ttl
                                  :value       @usb-trigger-key-ttl
                                  :size        "mini"
                                  :type        "number"
                                  :on-change   (ui-callback/input-callback
                                                 #(cond
                                                    (number? (general-utils/str->int %))
                                                    (reset! usb-trigger-key-ttl
                                                            (general-utils/str->int %))
                                                    (empty? %) (reset! usb-trigger-key-ttl 0)))
                                  :step        1
                                  :min         0}]
                       [ui/Popup {:content  (@tr [:nuvlabox-modal-usb-expires-popup] [default-ttl])
                                  :position "right center"
                                  :wide     true
                                  :trigger  (r/as-element [ui/Icon {:name  "question"
                                                                    :color "grey"}])}]]
                      ]]])]

                [ui/ModalActions
                 [utils-forms/validation-error-msg (@tr [:nuvlabox-modal-missing-fields]) (not (nil? @install-strategy-error))]
                 [ui/Button {:positive true
                             :loading  @creating
                             :on-click on-add-fn}
                  (@tr [:create])]]])])))


(defn AddModalWrapper
  []
  (let [nb-release (subscribe [::edges-subs/nuvlabox-releases])]
    ^{:key (count @nb-release)}
    [AddModal]))


(defn DeploymentFleetRow
  [{:keys [id name description created state tags] :as _deployment-fleet}]
  (let [uuid (general-utils/id->uuid id)]
    [ui/TableRow {:on-click #(dispatch [::history-events/navigate (str "deployment-fleets/" uuid)])
                  :style    {:cursor "pointer"}}
     [ui/TableCell (or name uuid)]
     [ui/TableCell description]
     [ui/TableCell state]
     [ui/TableCell (values/format-created created)]
     [ui/TableCell [uix/Tags tags]]]))


(defn Pagination
  []
  (let [deployment-fleets (subscribe [::subs/deployment-fleets])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])
        total-elements    (:count @deployment-fleets)
        total-pages       (general-utils/total-pages total-elements @elements-per-page)]
    [uix/Pagination {:totalitems   total-elements
                     :totalPages   total-pages
                     :activePage   @page
                     :onPageChange (ui-callback/callback
                                     :activePage #(dispatch [::events/set-page %]))}]))


(defn DeploymentFleetTable
  []
  (let [deployment-fleets (subscribe [::subs/deployment-fleets])]
    [:div style/center-items
     [ui/Table {:compact "very", :selectable true}
      [ui/TableHeader
       [ui/TableRow
        [ui/TableHeaderCell "name"]
        [ui/TableHeaderCell "description"]
        [ui/TableHeaderCell "state"]
        [ui/TableHeaderCell "created"]
        [ui/TableHeaderCell "tags"]]]

      [ui/TableBody
       (for [{:keys [id] :as deployment-fleet} (:resources @deployment-fleets)]
         (when id
           ^{:key id}
           [DeploymentFleetRow deployment-fleet]))]]]))


(defn NuvlaboxMapPoint
  [{:keys [id name location inferred-location online]}]
  (let [uuid     (general-utils/id->uuid id)
        on-click #(dispatch [::history-events/navigate (str "edges/" uuid)])]
    [map/CircleMarker {:on-click on-click
                       :center   (map/longlat->latlong (or location inferred-location))
                       :color    (utils/map-online->color online)
                       :opacity  0.5
                       :weight   1
                       :radius   7}
     [map/Tooltip (or name id)]]))


(defn DeploymentFleetCard
  [{:keys [id created name state description tags] :as _deployment-fleet}]
  (let [tr   (subscribe [::i18n-subs/tr])
        href (str "deployment-fleets/" (general-utils/id->uuid id))]
    ^{:key id}
    [uix/Card
     {:on-click    #(dispatch [::history-events/navigate href])
      :href        href
      :header      [:<>
                    [ui/Icon {:name (state->icon state)}]
                    (or name id)]
      :meta        (str (@tr [:created]) " " (-> created time/parse-iso8601 time/ago))
      :state       state
      :description (when-not (str/blank? description) description)
      :tags        tags}]))

(defn DeploymentFleetCards
  []
  (let [deployment-fleets (subscribe [::subs/deployment-fleets])]
    [:div style/center-items
     [ui/CardGroup {:centered    true
                    :itemsPerRow 4}
      (for [{:keys [id] :as deployment-fleet} (:resources @deployment-fleets)]
        (when id
          ^{:key id}
          [DeploymentFleetCard deployment-fleet]))]]))

(defn ControlBar []
  (let [full-text    (subscribe [::edges-subs/full-text-search])
        ;additional-filter (subscribe [::subs/additional-filter])
        filter-open? (r/atom false)]
    (fn []
      [ui/GridColumn {:width 4}
       [components/SearchInput
        {:on-change     (ui-callback/input-callback #(dispatch [::events/set-full-text-search %]))
         :default-value @full-text}]
       " "
       ^{:key (random-uuid)}
       [filter-comp/ButtonFilter
        {:resource-name "deployment-fleet"
         ;:default-filter @additional-filter
         :open?         filter-open?
         :on-done       #(dispatch [::events/set-additional-filter %])}]])))

(defn Page
  []
  (dispatch [::events/refresh])
  (let [tr (subscribe [::i18n-subs/tr])]
    [components/LoadingPage {}
     [:<>
      [uix/PageHeader "bullseye"
       (@tr [:deployment-fleets])]
      [MenuBar]
      [ui/Grid {:columns   3
                :stackable true
                :reversed  "mobile"}
       [ControlBar]
       [StatisticStates true]]
      (case @view-type
        :cards [DeploymentFleetCards]
        :table [DeploymentFleetTable])
      [Pagination]]]))


(defmethod panel/render :deployment-fleets
  [path]
  (let [[_ path1] path
        n        (count path)
        children (case n
                   2 [detail/Details path1]
                   [Page])]
    [:<>
     [ui/Segment style/basic children]]))
