(ns sixsq.nuvla.ui.edges.views
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-fx]
            [sixsq.nuvla.ui.edges-detail.views :as edges-detail]
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
            [sixsq.nuvla.ui.plugins.table :refer [Table] :as table-plugin]
            [sixsq.nuvla.ui.routing.events :as routing-events]
            [sixsq.nuvla.ui.routing.routes :as routes]
            [sixsq.nuvla.ui.routing.utils :as route-utils]
            [sixsq.nuvla.ui.session.subs :as session-subs]
            [sixsq.nuvla.ui.utils.form-fields :as ff]
            [sixsq.nuvla.ui.utils.forms :as utils-forms]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.map :as map]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.time :as time]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
            [sixsq.nuvla.ui.utils.values :as values]
            [sixsq.nuvla.ui.utils.view-components :refer [OnlineStatusIcon]]
            [sixsq.nuvla.ui.utils.zip :as zip]
            [sixsq.nuvla.ui.messages.events :as messages-events]))

(def show-state-statistics (r/atom false))


(defn switch-view!
  [new-view]
  (dispatch [::events/change-view-type new-view]))


(defn StatisticStatesEdge
  [clickable?]
  (let [summary (if clickable?
                  (subscribe [::subs/nuvlaboxes-summary])
                  (subscribe [::subs/nuvlaboxes-summary-all]))
        tr      (subscribe [::i18n-subs/tr])]
    (fn [clickable?]
      (let [total           (:count @summary)
            online-statuses (general-utils/aggregate-to-map
                              (get-in @summary [:aggregations :terms:online :buckets]))
            online          (:1 online-statuses)
            offline         (:0 online-statuses)
            unknown         (- total (+ online offline))]
        [ui/StatisticGroup {:widths (when-not clickable? 4)
                            :size   "tiny"}
         (for [statistic-opts [{:value          total
                                :icons          ["fal fa-box"]
                                :label          "TOTAL"
                                :positive-color nil}
                               {:value          online
                                :icons          ["fal fa-power-off"]
                                :label          utils/status-online
                                :positive-color "green"}
                               {:value          offline
                                :icons          ["fal fa-power-off"]
                                :label          utils/status-offline
                                :positive-color "red"}
                               {:value          unknown
                                :icons          ["fal fa-power-off"]
                                :label          utils/status-unknown
                                :positive-color "orange"}]]
           ^{:key (str "stat-state-" (:label statistic-opts))}
           [components/StatisticState
            (assoc statistic-opts
              :stacked? true
              :clickable? clickable?
              :set-state-selector-event ::events/set-state-selector
              :state-selector-subs ::subs/state-selector)])
         (when clickable?
           [ui/Button
            {:icon     true
             :style    {:margin "50px auto 15px"}
             :on-click #(when clickable?
                          (reset! show-state-statistics (not @show-state-statistics))
                          (when-not @show-state-statistics
                            (dispatch [::events/set-state-selector nil])))}
            [uix/Icon {:name "fa-light fa-arrow-down"}]
            \u0020
            (@tr [:commissionning-states])])]))))

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
  {spec/cards-view   "grid layout"
   spec/table-view   "table"
   spec/map-view     "map"
   spec/cluster-view "fas fa-chart-network"})

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
             [ui/Icon {:className (view->icon-classes view)}]]))
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
               [ui/Icon {:name    (if @playbooks-cronjob "check circle outline" "spinner")
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

                 (let [{nb-rel                     :nb-rel
                        nb-assets                  :nb-assets
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
  (let [nb-release (subscribe [::subs/nuvlabox-releases])]
    ^{:key (count @nb-release)}
    [AddModal]))


(defn NuvlaboxRow
  [{:keys [id name description created state tags online refresh-interval version created-by] :as _nuvlabox} managers]
  (let [uuid                  (general-utils/id->uuid id)
        locale                @(subscribe [::i18n-subs/locale])
        next-heartbeat-moment @(subscribe [::subs/next-heartbeat-moment id])
        engine-version        @(subscribe [::subs/engine-version id])
        creator               (subscribe [::session-subs/resolve-user created-by])]
    [:<>

     [ui/TableCell {:collapsing true}
      [OnlineStatusIcon online]]
     [ui/TableCell {:collapsing true}
      [ui/Icon {:class (utils/state->icon state)}]]
     [ui/TableCell (or name uuid)]
     [ui/TableCell description]
     [ui/TableCell (time/parse-ago created locale)]
     [ui/TableCell @creator]
     [ui/TableCell (str refresh-interval "s")]
     [ui/TableCell (when next-heartbeat-moment
                     [uix/TimeAgo (utils/last-time-online
                                   next-heartbeat-moment
                                   refresh-interval)])]
     [ui/TableCell (or engine-version (str version ".y.z"))]
     [ui/TableCell [uix/Tags tags]]
     [ui/TableCell {:collapsing true}
      (when (some #{id} managers)
        [ui/Icon {:name "check"}])]]))

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


(defn- get-name-as-keyword
  [tr q-key]
  (tr [(-> q-key name keyword)]))

(defn- TagsEditModeRadio
  [edit-mode opened-modal change-mode]
  (let [tr               (subscribe [::i18n-subs/tr])
        active?          (= opened-modal edit-mode)
        font-weight      (if active? 700 400)
        setting-tags?    (= spec/modal-tags-set-id edit-mode)]
    [ui/Radio {:style     {:font-weight font-weight}
               :label     (str (get-name-as-keyword @tr edit-mode)
                               (when setting-tags? (str " (" (@tr [:tags-overwrite]) "!)")))
               :checked   active?
               :on-change #(change-mode edit-mode)}]))


(defn- ButtonAskingForConfirmation
  [_form-tags close-fn]
  (let [tr               (subscribe [::i18n-subs/tr])
        edit-mode        (subscribe [::subs/opened-modal])
        mode             (r/atom :idle)
        edit-mode->color {spec/modal-tags-add-id     :green
                          spec/modal-tags-remove-all :red
                          spec/modal-tags-remove-id  :red
                          spec/modal-tags-set-id     :red}]
    (fn [form-tags _close-fn]
      (let [text      (if (and (= spec/modal-tags-set-id @edit-mode)
                               (= 0 (count form-tags)))
                        (@tr [:tags-remove-all])
                        (get-name-as-keyword @tr @edit-mode))
            call-back (fn [updated]
                        (dispatch [::table-plugin/set-bulk-edit-success-message
                                   (str updated " " (@tr [(if (< 1 updated) :edges :edge)]) " updated with operation: " text)
                                   [::spec/select]])
                        (dispatch [::table-plugin/reset-bulk-edit-selection [::spec/select]])
                        (close-fn))
            update-fn (fn []
                        (dispatch [::events/update-tags
                                   @edit-mode
                                   {:tags         form-tags
                                    :call-back-fn call-back}]))
            disabled? (and  (not= spec/modal-tags-remove-all @edit-mode)
                            (= 0 (count form-tags)))]
        (if (= :idle @mode)
          [uix/Button {:text     text
                       :color    (edit-mode->color @edit-mode)
                       :disabled disabled?
                       :active   true
                       :style    {:margin-left "2rem"}
                       :on-click (fn [] (reset! mode :confirming))}]
          [:div
           [:span "Sure? "]
           [uix/Button {:text     "No"
                        :icon     [ui/Icon {:name "fal fa-check"}]
                        :on-click (fn [] (reset! mode :idle))}]
           [uix/Button {:text     (str "Yes, " text)
                        :disabled disabled?
                        :color    (edit-mode->color @edit-mode)
                        :on-click update-fn}]])))))

(defn BulkUpdateModal
  []
  (let [tr               (subscribe [::i18n-subs/tr])
        selected-count   (subscribe [::subs/selected-count ::spec/select])
        opened-modal     (subscribe [::subs/opened-modal])
        open?            (subscribe [::subs/bulk-modal-visible?])
        used-tags        (subscribe [::subs/edges-tags])
        view-only-edges  (subscribe [::subs/edges-without-edit-rights])
        form-tags        (r/atom [])
        mode->tag-color  (zipmap spec/tags-modal-ids [:teal :teal :red :red])]
    (fn []
      (let [close-fn     (fn []
                           (dispatch [::events/open-modal nil])
                           (reset! form-tags []))
            change-mode  (fn [edit-mode]
                           (when (= spec/modal-tags-remove-all edit-mode)
                             (reset! form-tags []))
                           (dispatch [::events/open-modal edit-mode]))
            not-editable (:count @view-only-edges)]
        [ui/Modal {:open       @open?
                   :close-icon true
                   :on-close   close-fn}
         [uix/ModalHeader {:header (@tr [:bulk-update-tags])}]
         [ui/ModalContent
          [ui/Form
           [:div {:style {:display :flex
                          :gap     "1.5rem"}}
            (doall (for [edit-mode spec/tags-modal-ids]
                     ^{:key edit-mode}
                     [TagsEditModeRadio edit-mode @opened-modal change-mode]))]
           [:div {:style {:margin-top "1.5rem"}}
            (when-not (= spec/modal-tags-remove-all @opened-modal)
              [components/TagsDropdown {:initial-options @used-tags
                                        :on-change-fn    (fn [tags] (reset! form-tags tags))
                                        :tag-color       (mode->tag-color @opened-modal)}])]]]
         [ui/ModalActions
          {:style {:display         :flex
                   :align-items     :center
                   :justify-content :space-between
                   :text-align      :left}}
          [:div
           {:style {:line-height "1.2rem"}}
           [:div (str (str/capitalize (@tr [:tags-bulk-you-have-selected]))
                      " "
                      @selected-count
                      " "
                      (@tr [(if (= @selected-count 0) :edge :edges)])
                      ". ")]
           (when (<= 0 not-editable @selected-count)
             [:<>
              [:div
               (str not-editable " " (@tr [(if (= not-editable 1) :edge :edges)]) " will not be updated, because you lack the required rights.")]
              [:div [:a {:style {:cursor :pointer}
                         :target :_blank
                         :on-click
                         (fn []
                           (dispatch
                            [::events/store-filter-and-open-in-new-tab
                             (str/join " or "
                                       (map #(str "id='" % "'")
                                            (->> @view-only-edges :resources (map :id))))]))}
                     (str "Open " (if (= not-editable 1) "it" "them") " in a new tab")]]])]
          [ButtonAskingForConfirmation @form-tags close-fn]]]))))


(defn NuvlaboxTable
  []
  (let [nuvlaboxes        (subscribe [::subs/nuvlaboxes])
        nuvlabox-clusters (subscribe [::subs/nuvlabox-clusters])
        managers          (distinct
                           (apply concat
                                  (map :nuvlabox-managers (:resources @nuvlabox-clusters))))
        current-cluster   (subscribe [::subs/nuvlabox-cluster])
        selected-nbs      (if @current-cluster
                            (for [target-nb-id (concat (:nuvlabox-managers @current-cluster)
                                                       (:nuvlabox-workers @current-cluster))]
                              (into {} (get (group-by :id (:resources @nuvlaboxes)) target-nb-id)))
                            (:resources @nuvlaboxes))
        maj-version-only? (subscribe [::subs/one-edge-with-only-major-version (map :id selected-nbs)])
        tr                (subscribe [::i18n-subs/tr])
        columns           [{:field-key :online :header-content [ui/Icon {:name "heartbeat"}]}
                           {:field-key :state}
                           {:field-key :name}
                           {:field-key :description}
                           {:field-key :created}
                           {:field-key :created-by}
                           {:field-key      :refresh-interval
                            :header-content (str/lower-case (@tr [:report-interval]))}
                           {:field-key :last-online :no-sort? true}
                           {:field-key      :version :no-sort? true
                            :header-content [:<> (@tr [:version])
                                             (when @maj-version-only? (ff/help-popup (@tr [:edges-version-info])))]}
                           {:field-key :tags :no-sort? true}
                           {:field-key :manager :no-sort? true}]]
    [Table {:sort-config        {:db-path     ::spec/ordering
                                 :fetch-event [::events/get-nuvlaboxes]}
            :columns           columns
            :rows              selected-nbs
            :table-props       {:compact "very" :selectable true}
            :cell-props        {:header {:single-line true}}
            :row-render        (fn [row-data] [NuvlaboxRow row-data managers])
            :row-click-handler (fn [{id :id}] (dispatch [::routing-events/navigate (utils/edges-details-url (general-utils/id->uuid id))]))
            :row-props         {:role  "link"
                                :style {:cursor "pointer"}}
            :select-config      {:bulk-actions [{:icon (fn [] [ui/Icon {:className "fal fa-tags"}])
                                                 :name "Edit Tags"
                                                 :event (fn []
                                                          (dispatch [::events/get-edges-tags])
                                                          (dispatch [::events/open-modal spec/modal-tags-add-id]))}]
                                 :total-count-sub-key [::subs/nuvlaboxes-count]
                                 :resources-sub-key [::subs/nuvlaboxes-resources]
                                 :select-db-path [::spec/select]
                                 :rights-needed :edit}}]))


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
     {}
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
          :placeholder-suffix  (str " " @(subscribe [::subs/state-selector]))
          :style              {:width "100%"}}]
        ^{:key (random-uuid)}
        [:div {:style {:margin-top "10px"}}
         [filter-comp/ButtonFilter
          {:resource-name  spec/resource-name
           :default-filter @additional-filter
           :open?          filter-open?
           :on-done        #(dispatch [::events/set-additional-filter %])
           :show-clear-button-outside-modal? true}]]]])))


(defn NuvlaBoxesOrClusters
  []
  (dispatch [::events/init])
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
   [AddModalWrapper]
   [BulkUpdateModal]])
