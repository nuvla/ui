(ns sixsq.nuvla.ui.edges.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.about.subs :as about-subs]
            [sixsq.nuvla.ui.about.utils :as about-utils]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-fx]
            [sixsq.nuvla.ui.deployment-sets-detail.events :as depl-group-events]
            [sixsq.nuvla.ui.deployment-sets-detail.subs :as depl-group-subs]
            [sixsq.nuvla.ui.edges-detail.views :as edges-detail]
            [sixsq.nuvla.ui.edges.add-modal :as add-modal]
            [sixsq.nuvla.ui.edges.events :as events]
            [sixsq.nuvla.ui.edges.spec :as spec]
            [sixsq.nuvla.ui.edges.subs :as subs]
            [sixsq.nuvla.ui.edges.utils :as utils]
            [sixsq.nuvla.ui.edges.views-clusters :as views-clusters]
            [sixsq.nuvla.ui.edges.views-utils :as views-utils]
            [sixsq.nuvla.ui.filter-comp.views :as filter-comp]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.main.components :as components]
            [sixsq.nuvla.ui.plugins.full-text-search :as full-text-search-plugin]
            [sixsq.nuvla.ui.plugins.pagination :as pagination-plugin]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.ui-demo.views :refer [TableColsEditable]]
            [sixsq.nuvla.ui.utils.bulk-edit-tags-modal :as bulk-edit-modal]
            [sixsq.nuvla.ui.utils.forms :as utils-forms]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.map :as map]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
            [sixsq.nuvla.ui.utils.values :as values]
            [sixsq.nuvla.ui.utils.view-components :refer [OnlineStatusIcon]]
            [sixsq.nuvla.ui.utils.zip :as zip]))

(def show-state-statistics (r/atom false))


(defn switch-view!
  [new-view]
  (dispatch [::events/change-view-type new-view]))

(def edges-states
  [{:key            :total
    :icons          [icons/i-box]
    :label          "TOTAL"
    :positive-color nil}
   {:key            :online
    :icons          [icons/i-power]
    :label          utils/status-online
    :positive-color "green"}
   {:key            :offline
    :icons          [icons/i-power]
    :label          utils/status-offline
    :positive-color "red"}
   {:key            :unknown
    :icons          [icons/i-power]
    :label          utils/status-unknown
    :positive-color "orange"}])

(defn StatisticStatesEdgeView
  []
  (fn [{:keys [states] :as states->counts} clickable? restricted-view?]
    (let [tr (subscribe [::i18n-subs/tr])]
      [ui/StatisticGroup {:widths (when-not clickable? 4)
                          :size   "tiny"}
       (for [state (or states edges-states)]
         ^{:key (str "stat-state-" (:label state))}
         [components/StatisticState
          (merge state
                 {:value                    (states->counts (:key state))
                  :stacked?                 true
                  :clickable?               (or (:clickable? state) clickable?)
                  :set-state-selector-event ::events/set-state-selector
                  :state-selector-subs      ::subs/state-selector})])
       (when (and clickable? (not restricted-view?))
         [ui/Button
          {:icon     true
           :style    {:margin "50px auto 15px"}
           :on-click #(when clickable?
                        (reset! show-state-statistics (not @show-state-statistics))
                        (when-not @show-state-statistics
                          (dispatch [::events/set-state-selector nil])))}
          [icons/ArrowDownIcon]
          \u0020
          (@tr [:commissionning-states])])])))


(defn StatisticStatesEdge
  [clickable?]
  (let [summary (if clickable?
                  (subscribe [::subs/nuvlaboxes-summary])
                  (subscribe [::subs/nuvlaboxes-summary-all]))]
    (fn []
      (let [total           (:count @summary)
            online-statuses (general-utils/aggregate-to-map
                              (get-in @summary [:aggregations :terms:online :buckets]))
            online          (:1 online-statuses)
            offline         (:0 online-statuses)]
        [StatisticStatesEdgeView
         {:total   total
          :online  online
          :offline offline
          :unknown (- total (+ online offline))}
         clickable?]))))

(defn StatisticStates
  []
  (let [tr       (subscribe [::i18n-subs/tr])
        summary  (subscribe [::subs/nuvlaboxes-summary])
        selected (subscribe [::subs/state-selector])]
    (when ((set utils/states) @selected)
      (reset! show-state-statistics true))
    (fn []
      (let [terms (general-utils/aggregate-to-map
                    (get-in @summary [:aggregations :terms:state :buckets]))]
        [:div {:style {:display         :flex
                       :justify-content :center
                       :flex-direction  :column
                       :align-items     :center}}
         [StatisticStatesEdge true]
         [ui/Segment {:compact true
                      :width   "auto"
                      :style   {:text-align "center"
                                :display    (if @show-state-statistics "table" "none")}}
          [:h4 (@tr [:commissionning-states])]
          [ui/StatisticGroup
           {:size  "tiny"
            :style {:margin     "10px auto 10px auto"
                    :display    "flex"
                    :text-align "center"
                    :width      "100%"}}
           (for [state utils/states]
             ^{:key state}
             [components/StatisticState
              {:value                    ((keyword state) terms 0)
               :icons                    [(utils/state->icon state)]
               :label                    state
               :clickable?               true
               :set-state-selector-event ::events/set-state-selector
               :state-selector-subs      ::subs/state-selector
               :stacked?                 true}])]]]))))

(def view->icon-classes
  {spec/cards-view   icons/i-grid-layout
   spec/table-view   icons/i-table
   spec/map-view     icons/i-map
   spec/cluster-view icons/i-chart-network})

(defn MenuBar []
  (let [loading?  (subscribe [::subs/loading?])
        view-type (subscribe [::subs/view-type])]
    (fn []
      [components/StickyBar
       [ui/Menu {:borderless true, :stackable true}
        [views-utils/AddButton]
        (doall
          (for [view spec/view-types]

            ^{:key view}
            [ui/MenuItem {:active   (= @view-type view)
                          :on-click #(switch-view! view)}
             [icons/Icon {:name (view->icon-classes view)}]]))
        [components/RefreshMenu
         {:action-id  events/refresh-id
          :loading?   @loading?
          :on-refresh #(dispatch [::events/refresh-root])}]]])))


(def usb-doc-url "https://docs.nuvla.io/nuvlaedge/installation/install-with-usb-stick/")
(def compose-doc-url "https://docs.nuvla.io/nuvlaedge/installation/install-with-compose-files/")


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
   [icons/WarningIcon]
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
        playbooks-cronjob    (subscribe [::subs/nuvlabox-playbooks-cronjob])
        private-ssh-key-file (str (general-utils/id->short-uuid nuvlabox-id) ".ssh.private")
        public-keys          (when (seq @nuvlabox-ssh-keys)
                               (str (str/join "\\n" (:public-keys @nuvlabox-ssh-keys)) "\\n"))
        zip-url              (r/atom nil)
        envsubst             (cond-> {"${NUVLABOX_UUID}"  nuvlabox-id
                                      "${NUVLAEDGE_UUID}" nuvlabox-id}
                                     public-keys (assoc "${NUVLABOX_SSH_PUB_KEY}" public-keys
                                                        "${NUVLAEDGE_SSH_PUB_KEY}" public-keys))
        download-files       (utils/prepare-compose-files nuvlabox-release nuvlabox-peripherals
                                                          (partial general-utils/envsubst-str envsubst))]
    (when playbooks-toggle
      (dispatch [::events/enable-host-level-management nuvlabox-id]))
    (zip/create download-files #(reset! zip-url %))
    (when @nuvlabox-ssh-keys
      (dispatch [::events/assign-ssh-keys @nuvlabox-ssh-keys nuvlabox-id]))
    (fn [nuvlabox-id creation-data _nuvlabox-release-data _nuvlabox-ssh-keys
         new-private-ssh-key playbooks-toggle on-close-fn]
      (let [tr                  (subscribe [::i18n-subs/tr])
            nuvlabox-name-or-id (str "NuvlaEdge " (or (:name creation-data)
                                                      (general-utils/id->short-uuid nuvlabox-id)))
            execute-command     (str "docker-compose -p nuvlaedge -f "
                                     (str/join " -f " (map :name download-files)) " up -d")]
        [:<>
         [uix/ModalHeader {:header (str nuvlabox-name-or-id " created") :icon "box"}]

         (when (or @new-private-ssh-key playbooks-toggle)
           [ui/Segment {:basic true}
            (when playbooks-toggle
              [ui/Message {:icon true}
               [icons/Icon {:name    (if @playbooks-cronjob icons/i-circle-check icons/i-spinner)
                            :loading (if @playbooks-cronjob false true)}]
               [ui/MessageContent
                [ui/MessageHeader [:span (@tr [:nuvlabox-playbooks-cronjob]) " "
                                   (when @playbooks-cronjob
                                     [ui/Popup {:content        (@playbooks-cronjob :cronjob)
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
                     "" (@playbooks-cronjob :cronjob) (@tr [:copy-to-clipboard]) true)]
                  (@tr [:nuvlabox-playbooks-cronjob-wait]))]])

            (when @new-private-ssh-key
              [CreateSSHKeyMessage @new-private-ssh-key private-ssh-key-file tr])])

         [ui/ModalContent
          [ui/Container
           [ui/CardGroup {:centered true}
            [ui/Card
             [ui/CardContent {:text-align :center}
              [ui/Header [:span {:style {:overflow-wrap "break-word"}} nuvlabox-name-or-id]]
              [icons/BoxIcon {:color "green"
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
                  :download "nuvlaedge.zip"} "nuvlaedge.zip " [ui/Icon {:name "download"}]]]

            [ui/Segment {:text-align :center}
             [ui/Label {:circular true
                        :color    "green"} "2"]
             [:h5 {:style {:margin "0.5em 0 1em 0"}}
              (@tr [:nuvlabox-unzip-execute])
              (values/copy-value-to-clipboard "" execute-command (@tr [:copy-command-to-clipboard]))]
             [:span {:style {:font "1em Inconsolata, monospace"}} execute-command]]]

           [:div {:style {:margin "20px 0px 0px 0px"}}
            [NuvlaDocs tr compose-doc-url]]]]

         [ui/ModalActions
          [ui/Button {:positive true
                      :on-click on-close-fn} (@tr [:close])]]]))))


(defn CreatedNuvlaBoxUSBTrigger
  [_creation-data nuvlabox-release-data _new-api-key _nuvlabox-ssh-keys _new-private-ssh-key _on-close-fn]
  (let [nuvlabox-release     (:nb-selected nuvlabox-release-data)
        nuvlabox-peripherals (:nb-assets nuvlabox-release-data)
        private-ssh-key-file "nuvlabox.ssh.private"
        download-files       (utils/prepare-compose-files nuvlabox-release nuvlabox-peripherals)
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
                                                     "/ui/downloads/nuvlaedge-self-registration.py.gpg")
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
           [icons/Icon {:name (if apikey icons/i-circle-check icons/i-spinner)}]
           [ui/MessageContent
            [ui/MessageHeader
             (@tr [:nuvlabox-usb-key])]
            (if apikey
              [:span (str (@tr [:nuvlabox-usb-key-ready]) " ")
               [:a {:href   (str "api/" apikey)
                    :target "_blank"}
                apikey] " "
               [ui/Popup {:content (@tr [:nuvlabox-modal-usb-apikey-warning])
                          :trigger (r/as-element [ui/Icon {:class icons/i-triangle-exclamation
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
              [icons/Icon {:name    (if apikey icons/i-file-code icons/i-spinner)
                           :loading (nil? apikey)
                           :color   "green"
                           :size    :massive}]]
             [:a {:href     (str "data:text/plain;charset=utf-8,"
                                 (js/encodeURIComponent
                                   (general-utils/edn->json nuvlabox-trigger-file)))
                  :target   "_blank"
                  :download "nuvlabox-installation-trigger-usb.nuvla"}
              [ui/Button {:style          {:border-radius "inherit"}
                          :href           (str "data:text/plain;charset=utf-8,"
                                               (js/encodeURIComponent
                                                 (general-utils/edn->json nuvlabox-trigger-file)))
                          :target         "_blank"
                          :download       "nuvlabox-installation-trigger-usb.nuvla"
                          :positive       true
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
                         :trigger (r/as-element [ui/Icon {:class icons/i-info}])}]]]

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

          [:div {:style {:margin "20px 0px 0px 0px"}}
           [NuvlaDocs tr usb-doc-url]]]

         [ui/ModalActions
          [ui/Button {:positive true
                      :on-click on-close-fn} (@tr [:close])]]]))))

(defn AddModal
  []
  (let [modal-id                   spec/modal-add-id
        tr                         (subscribe [::i18n-subs/tr])
        visible?                   (subscribe [::subs/modal-visible? modal-id])
        nuvlabox-id                (subscribe [::subs/nuvlabox-created-id])
        usb-api-key                (subscribe [::subs/nuvlabox-usb-api-key])
        vpn-infra-opts             (subscribe [::subs/vpn-infra-options])
        nb-releases                (subscribe [::subs/nuvlabox-releases])
        ssh-credentials            (subscribe [::subs/ssh-keys-available])
        nb-releases-by-id          (subscribe [::subs/nuvlabox-releases-by-id])
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
        nuvlabox-ssh-keys          (subscribe [::subs/nuvlabox-ssh-key])
        new-private-ssh-key        (subscribe [::subs/nuvlabox-private-ssh-key])
        creating                   (r/atom false)
        on-close-fn                #(do
                                      (dispatch [::events/set-created-nuvlabox-id nil])
                                      (dispatch [::events/set-nuvlabox-usb-api-key nil])
                                      (dispatch [::events/set-nuvlabox-ssh-keys nil])
                                      (dispatch [::events/set-nuvlabox-created-private-ssh-key nil])
                                      (dispatch [::events/open-modal nil])
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
                                                    (dispatch [::events/find-nuvlabox-ssh-keys
                                                               @ssh-chosen-keys
                                                               (if (= @install-strategy "usb")
                                                                 [::events/create-nuvlabox-usb-api-key
                                                                  @usb-trigger-key-ttl]
                                                                 [::events/create-nuvlabox
                                                                  (->> @creation-data
                                                                       (remove (fn [[_ v]]
                                                                                 (str/blank? v)))
                                                                       (into {}))])]))
                                                  ; else, create new one
                                                  (let [ssh-desc "SSH credential generated for NuvlaEdge: "
                                                        ssh-tpl  {:name        (str "SSH key for " (:name @creation-data))
                                                                  :description (str ssh-desc (:name @creation-data))
                                                                  :template    {:href "credential-template/generate-ssh-key"}}]
                                                    (dispatch [::events/create-ssh-key ssh-tpl
                                                               (if (= @install-strategy "usb")
                                                                 [::events/create-nuvlabox-usb-api-key
                                                                  @usb-trigger-key-ttl]
                                                                 [::events/create-nuvlabox
                                                                  (->> @creation-data
                                                                       (remove (fn [[_ v]]
                                                                                 (str/blank? v)))
                                                                       (into {}))])])))
                                                (if (= @install-strategy "usb")
                                                  (dispatch [::events/create-nuvlabox-usb-api-key
                                                             @usb-trigger-key-ttl])
                                                  (dispatch [::events/create-nuvlabox
                                                             (->> @creation-data
                                                                  (remove (fn [[_ v]]
                                                                            (str/blank? v)))
                                                                  (into {}))])))))]

    (dispatch [::events/get-ssh-keys-available ["ssh-key"] nil])
    (dispatch [::events/get-vpn-infra])
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

                 (let [{nb-rel                      :nb-rel
                        nb-assets                   :nb-assets
                        {:keys [compose-files url]} :nb-selected}
                       @nuvlabox-release-data]
                   [ui/Container
                    [ui/Divider {:horizontal true :as "h3"}
                     (@tr [:version])]
                    [edges-detail/DropdownReleases
                     {:value     nb-rel
                      :on-change (ui-callback/value
                                   (fn [value]
                                     (swap! nuvlabox-release-data
                                            assoc :nb-rel value)
                                     (let [nb-selected (get @nb-releases-by-id value)]
                                       (swap! creation-data assoc
                                              :version (-> nb-selected
                                                           :release
                                                           utils/get-major-version
                                                           general-utils/str->int))
                                       (swap! nuvlabox-release-data
                                              assoc :nb-selected nb-selected)
                                       (swap! nuvlabox-release-data assoc :nb-assets
                                              (set (map :scope (:compose-files nb-selected)))))))}]

                    [:a {:href   url
                         :target "_blank"
                         :style  {:margin "1em"}}
                     (@tr [:nuvlabox-release-notes])]
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
                      [:a {:href   compose-doc-url
                           :target "_blank"}
                       (@tr [:nuvlabox-modal-more-info])]

                      [:div {:style {:margin  "10px 5px 5px 5px"
                                     :display (if (= @install-strategy "compose")
                                                "block" "none")}}
                       [ui/Checkbox {:toggle    true
                                     :label     (@tr [:nuvlabox-modal-enable-playbooks])
                                     :checked   @playbooks-toggle
                                     :on-change #(swap! playbooks-toggle not)}]
                       [ui/Popup
                        {:trigger        (r/as-element [ui/Icon {:class icons/i-info
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
                      [:a {:href   usb-doc-url
                           :target "_blank"}
                       (@tr [:nuvlabox-modal-more-info])]]]])]

                [ui/ModalActions
                 [utils-forms/validation-error-msg (@tr [:nuvlabox-modal-missing-fields]) (not (nil? @install-strategy-error))]
                 [ui/Button {:positive true
                             :loading  @creating
                             :on-click on-add-fn}
                  (@tr [:create])]]])])))


(defn AddModalWrapper
  []
  (let [nb-release (subscribe [::subs/nuvlabox-releases])
        new-modal  (subscribe [::about-subs/feature-flag-enabled? about-utils/feature-edge-on-k8s])]
    (if @new-modal ^{:key (count @nb-release)} [add-modal/AddModal] ^{:key (count @nb-release)} [AddModal])))

(def field-key->table-cell
  {:online  '[ui/TableCell {:collapsing true}
              [OnlineStatusIcon online nil true]]
   :state   '[ui/TableCell {:collapsing true}
              [ui/Icon {:class (utils/state->icon state)}]]
   :name    '[ui/TableCell (or name uuid)]
   :description '[ui/TableCell description]
   :created '[ui/TableCell (time/parse-ago created locale)]
   :created-by '[ui/TableCell @creator]
   :refresh-interval '[ui/TableCell (str refresh-interval "s")]
   :last-online   '[ui/TableCell (when next-heartbeat-moment
                                   [uix/TimeAgo (utils/last-time-online
                                                  next-heartbeat-moment
                                                  refresh-interval)])]
   :version  '[ui/TableCell (or engine-version (str version ".y.z"))]
   :tags     '[ui/TableCell [uix/Tags tags]]})
;; (into {} (mapv (fn [[k [_ cell-props-or-cell perhaps-cell]]]
;;                                                [k (cond->
;;                                                     {:cell (or perhaps-cell cell-props-or-cell)}
;;                                                     perhaps-cell
;;                                                     (assoc :cell-props cell-props-or-cell)) ])
;;                                          '{:online  [ui/TableCell {:collapsing true}
;;                                                      [OnlineStatusIcon online nil true]]
;;                                            :state   [ui/TableCell {:collapsing true}
;;                                                      [ui/Icon {:class (utils/state->icon state)}]]
;;                                            :name    [ui/TableCell (or name uuid)]
;;                                            :description [ui/TableCell description]
;;                                            :created [ui/TableCell (time/parse-ago created locale)]
;;                                            :created-by [ui/TableCell @creator]
;;                                            :refresh-interval [ui/TableCell (str refresh-interval "s")]
;;                                            :last-online   [ui/TableCell (when next-heartbeat-moment
;;                                                                           [uix/TimeAgo (utils/last-time-online
;;                                                                                          next-heartbeat-moment
;;                                                                                          refresh-interval)])]
;;                                            :version  [ui/TableCell (or engine-version (str version ".y.z"))]
;;                                            :tags     [ui/TableCell [uix/Tags tags]]}))
(defn NuvlaboxRow
  [{{:keys [id name description created state tags online refresh-interval version created-by]} :row-data
    field-key :field-key}]
  (let [uuid                  (general-utils/id->uuid id)
        locale                @(subscribe [::i18n-subs/locale])
        next-heartbeat-moment @(subscribe [::subs/next-heartbeat-moment id])
        engine-version        @(subscribe [::subs/engine-version id])
        creator               (subscribe [::session-subs/resolve-user created-by])
        field-key->table-cell {:description description,
                               :tags [uix/Tags tags],
                               :refresh-interval (str refresh-interval "s"),
                               :name (or name uuid),
                               :created (time/parse-ago created locale),
                               :state [ui/Icon {:class (utils/state->icon state)}]
                               :online [OnlineStatusIcon online nil true]
                               :created-by @creator,
                               :last-online
                               (when next-heartbeat-moment [uix/TimeAgo (utils/last-time-online next-heartbeat-moment refresh-interval)]),
                               :version (or engine-version (str version ".y.z"))}]
    (field-key->table-cell field-key)))

(defn Pagination
  [view-type]
  (let [nuvlaboxes        (subscribe [::subs/nuvlaboxes])
        nuvlabox-clusters (subscribe [::subs/nuvlabox-clusters])
        current-cluster   (subscribe [::subs/nuvlabox-cluster])
        total-elements    (if (= view-type spec/cluster-view)
                            (:count @nuvlabox-clusters)
                            (if @current-cluster
                              (+ (count (:nuvlabox-managers @current-cluster))
                                 (count (:nuvlabox-managers @current-cluster)))
                              (:count @nuvlaboxes)))]
    (when-not (= view-type spec/map-view)
      [pagination-plugin/Pagination
       {:db-path                [::spec/pagination]
        :change-event           [::events/refresh-root]
        :total-items            total-elements
        :i-per-page-multipliers [1 2 4]}])))


(defn NuvlaEdgeTableView
  [{:keys [bulk-edit bulk-deploy columns edges]}]
  (let [{bulk-edit-modal :modal
         trigger         :trigger-config} bulk-edit
        bulk-deploy-enabled? (subscribe [::about-subs/feature-flag-enabled? about-utils/feature-deployment-set-key])
        {bulk-deploy-modal   :modal
         bulk-deploy-trigger :trigger-config} (when @bulk-deploy-enabled? bulk-deploy)]
    [:<>
     (when bulk-edit-modal
       [bulk-edit-modal])
     (when bulk-deploy-modal
       [bulk-deploy-modal])
     [TableColsEditable
      {:sort-config       {:db-path     ::spec/ordering
                           :fetch-event [::events/get-nuvlaboxes]}
       :columns           columns
       :rows              edges
       :table-props       {:compact "very" :selectable true}
       :cell-props        {:header {:single-line true}}
       :row-click-handler (fn [{id :id}] (dispatch [::routing-events/navigate (utils/edges-details-url (general-utils/id->uuid id))]))
       :row-props         {:role  "link"
                           :style {:cursor "pointer"}}
       :select-config     {:bulk-actions (filterv
                                           some?
                                           [trigger
                                            bulk-deploy-trigger])
                           :total-count-sub-key [::subs/nuvlaboxes-count]
                           :resources-sub-key [::subs/nuvlaboxes-resources]
                           :select-db-path [::spec/select]
                           :rights-needed :edit}}
      ::table-cols-config]]))


(defn NuvlaboxTable
  []
  (let [nuvlaboxes        (subscribe [::subs/nuvlaboxes])
        current-cluster   (subscribe [::subs/nuvlabox-cluster])
        selected-nbs      (if @current-cluster
                            (for [target-nb-id (concat (:nuvlabox-managers @current-cluster)
                                                       (:nuvlabox-workers @current-cluster))]
                              (into {} (get (group-by :id (:resources @nuvlaboxes)) target-nb-id)))
                            (:resources @nuvlaboxes))
        maj-version-only? (subscribe [::subs/one-edge-with-only-major-version (map :id selected-nbs)])
        tr                (subscribe [::i18n-subs/tr])
        columns           (mapv (fn [col-config]
                                  (assoc col-config :cell NuvlaboxRow))
                            [{:field-key :online :header-content [icons/HeartbeatIcon] :cell-props {:collapsing true}}
                             {:field-key :state :cell-props {:collapsing true}}
                             {:field-key :name}
                             {:field-key :description}
                             {:field-key :created}
                             {:field-key :created-by}
                             {:field-key      :refresh-interval
                              :header-content (str/lower-case (@tr [:telemetry]))}
                             {:field-key :last-online :no-sort? true}
                             {:field-key      :version :no-sort? true
                              :header-content [:<> (@tr [:version])
                                               (when @maj-version-only? [uix/HelpPopup (@tr [:edges-version-info])])]}
                             {:field-key :tags :no-sort? true}])
        bulk-edit         (bulk-edit-modal/create-bulk-edit-modal
                            {:db-path                [::spec/select]
                             :refetch-event          ::events/get-nuvlaboxes
                             :resource-key           :nuvlabox
                             :total-count-sub-key    ::subs/nuvlaboxes-count
                             :on-open-modal-event    ::events/get-edges-without-edit-rights
                             :no-edit-rights-sub-key ::subs/edges-without-edit-rights
                             :singular               (@tr [:edge])
                             :plural                 (@tr [:edges])
                             :filter-fn              (partial utils/build-bulk-filter [::spec/select])})]
    [NuvlaEdgeTableView {:bulk-edit bulk-edit
                         :bulk-deploy {:trigger-config {:icon (fn [] [icons/RocketIcon])
                                                        :name "Bulk Deploy App"
                                                        :event (fn []
                                                                 (let [id (random-uuid)]
                                                                   (dispatch [::events/get-selected-edge-ids ::depl-group-events/set-edges id])
                                                                   (dispatch [::routing-events/navigate
                                                                              routes/deployment-sets-details
                                                                              {:uuid :create}
                                                                              {depl-group-subs/creation-temp-id-key id}])))}} :columns columns :edges selected-nbs}]))


(defn NuvlaboxMapPoint
  [{:keys [id name location inferred-location online]}]
  (let [uuid     (general-utils/id->uuid id)
        on-click #(dispatch [::routing-events/navigate (utils/edges-details-url uuid)])]
    [map/CircleMarker {:on-click on-click
                       :center   (map/longlat->latlong (or location inferred-location))
                       :color    (utils/map-online->color online)
                       :opacity  0.5
                       :weight   1
                       :radius   7}
     [map/Tooltip (or name id)]]))


(defn NuvlaboxCards
  []
  (let [nuvlaboxes        (subscribe [::subs/nuvlaboxes])
        nuvlabox-clusters (subscribe [::subs/nuvlabox-clusters])
        managers          (distinct
                            (apply concat
                                   (map :nuvlabox-managers (:resources @nuvlabox-clusters))))
        selected-nbs      (:resources @nuvlaboxes)]
    [:div style/center-items
     [ui/CardGroup {:centered    true
                    :itemsPerRow 4}
      (for [{:keys [id] :as nuvlabox} selected-nbs]
        (when id
          ^{:key id}
          [views-utils/NuvlaboxCard nuvlabox managers]))]]))


(defn NuvlaboxMap
  []
  (let [nuvlabox-locations (subscribe [::subs/nuvlabox-locations])
        nbs-locations      (:resources @nuvlabox-locations)]
    [map/MapBox
     {:responsive-height? true}
     (doall
       (for [{:keys [id] :as nuvlabox} nbs-locations]
         ^{:key id}
         [NuvlaboxMapPoint nuvlabox]))]))


(defn- ControlBar
  []
  (let [additional-filter (subscribe [::subs/additional-filter])
        filter-open?      (r/atom false)]
    (fn []
      [ui/GridColumn {:width 4}
       [:div
        [full-text-search-plugin/FullTextSearch
         {:db-path            [::spec/edges-search]
          :change-event       [::pagination-plugin/change-page
                               [::spec/pagination] 1]
          :placeholder-suffix (str " " @(subscribe [::subs/state-selector]))
          :style              {:width "100%"}}]
        ^{:key (random-uuid)}
        [:div {:style {:margin-top "10px"}}
         [filter-comp/ButtonFilter
          {:resource-name                    spec/resource-name
           :default-filter                   @additional-filter
           :open?                            filter-open?
           :on-done                          #(dispatch [::events/set-additional-filter %])
           :show-clear-button-outside-modal? true}]]]])))


(defn NuvlaBoxesOrClusters
  [external-restriction-filter]
  (dispatch [::events/init external-restriction-filter])
  (dispatch [::events/set-nuvlabox-cluster nil])
  (let [view-type (subscribe [::subs/view-type])]
    (fn []
      [components/LoadingPage {}
       [:<>
        [MenuBar]
        [ui/Grid {:stackable true
                  :reversed  "mobile"
                  :style     {:margin-top    0
                              :margin-bottom 0}}
         [ControlBar]
         [ui/GridColumn {:width 10}
          (if (= @view-type spec/cluster-view)
            [views-clusters/StatisticStates]
            [StatisticStates])]]
        (condp = @view-type
          spec/cards-view [NuvlaboxCards]
          spec/table-view [NuvlaboxTable]
          spec/map-view [NuvlaboxMap]
          spec/cluster-view [views-clusters/NuvlaboxClusters]
          [NuvlaboxTable])
        [Pagination @view-type]]])))


(defn DetailedViewPage
  [{{:keys [uuid]} :path-params}]
  (if (= "nuvlabox-cluster" uuid)
    (do
      (switch-view! spec/cluster-view)
      (dispatch [::routing-events/navigate routes/edges]))
    [edges-detail/EdgeDetails uuid]))


(defn edges-view
  []
  [:<>
   [ui/Segment style/basic [NuvlaBoxesOrClusters]]
   [AddModalWrapper]])
