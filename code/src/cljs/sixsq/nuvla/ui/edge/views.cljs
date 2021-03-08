(ns sixsq.nuvla.ui.edge.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-fx]
    [sixsq.nuvla.ui.edge-detail.views :as edge-detail]
    [sixsq.nuvla.ui.edge.events :as events]
    [sixsq.nuvla.ui.edge.subs :as subs]
    [sixsq.nuvla.ui.edge.utils :as utils]
    [sixsq.nuvla.ui.history.events :as history-events]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.components :as main-components]
    [sixsq.nuvla.ui.main.events :as main-events]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.map :as map]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [sixsq.nuvla.ui.utils.time :as time]
    [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
    [sixsq.nuvla.ui.utils.values :as values]
    [sixsq.nuvla.ui.utils.zip :as zip]
    [taoensso.timbre :as log]
    [sixsq.nuvla.ui.utils.style :as utils-style]))


(def view-type (r/atom :cards))
(def show-state-statistics (r/atom false))


(defn StatisticStates
  ([] [StatisticStates true])
  ([clickable?]
   (let [tr         (subscribe [::i18n-subs/tr])
         summary    (subscribe [::subs/nuvlaboxes-summary])
         status     (subscribe [::subs/nuvlaboxes-online-status])
         open-popup (r/atom true)]
     ;(time/sleep 5000 #(reset! open-popup false))
     (fn [clickable?]
       (let [terms           (general-utils/aggregate-to-map (get-in @summary [:aggregations :terms:state :buckets]))
             new             (:NEW terms 0)
             activated       (:ACTIVATED terms 0)
             commissioned    (:COMMISSIONED terms 0)
             decommissioning (:DECOMMISSIONING terms 0)
             decommissioned  (:DECOMMISSIONED terms 0)
             error           (:ERROR terms 0)
             total           (:count @summary)
             online-statuses (general-utils/aggregate-to-map (get-in @summary [:aggregations :terms:online :buckets]))
             online          (:1 online-statuses)
             offline         (:0 online-statuses)
             unknown         (- total (+ online offline))]
         [:div {:style {:margin     "10px auto 10px auto"
                        :text-align "center"
                        :width      "100%"}}
          [ui/StatisticGroup (merge {:widths (if clickable? nil 4) :size "tiny"} style/center-block)
           [main-components/StatisticState total ["fas fa-box"] "TOTAL"
            clickable? ::events/set-state-selector ::subs/state-selector]
           [main-components/StatisticState online [(utils/status->icon utils/status-online)] utils/status-online
            clickable? "green" ::events/set-state-selector ::subs/state-selector]
           [main-components/StatisticState offline [(utils/status->icon utils/status-offline) "fas fa-slash"]
            utils/status-offline clickable? "red" ::events/set-state-selector ::subs/state-selector]
           [main-components/StatisticState unknown [(utils/status->icon utils/status-unknown)]
            utils/status-unknown clickable? "yellow" ::events/set-state-selector ::subs/state-selector]
           (when clickable?
             [ui/Popup
              {:trigger  (r/as-element
                           [ui/Statistic
                            {:size     "tiny"
                             :class    "slight-up"
                             :style    {:cursor "pointer"}
                             :on-click #(when clickable? (do (reset! show-state-statistics (not @show-state-statistics))
                                                             (when-not @show-state-statistics
                                                               (dispatch [::events/set-state-selector nil]))))}
                            [ui/StatisticValue {:style {:margin "0 10px"}}
                             [ui/Icon {:name (if @show-state-statistics "angle double up" "angle double down")}]]])
               :open     @open-popup
               :position "right center"
               :offset   [0 20]}
              [ui/PopupContent
               [:span [ui/Icon {:name "arrow left"}] (@tr [:statistics-select-info])]]])]
          (when clickable?
            [ui/Segment (assoc-in (merge {:style {:margin     "0px auto 20px auto"
                                                  :padding    "1em 1.5em 0 0"
                                                  :text-align "center"
                                                  ;:width      "100%"
                                                  }}
                                         {:id      "statistics-state"
                                          :compact true
                                          :width   "auto"})
                                  [:style :display] (if @show-state-statistics "table" "none"))
             [:h4 (@tr [:commissionning-states])]
             [ui/StatisticGroup
              (merge {:style {:margin     "10px auto 10px auto"
                              :display    "block"
                              :text-align "center"
                              :width      "100%"}}
                     {:size "tiny"
                      :id   "statistics-state"})
              [main-components/StatisticState new [(utils/state->icon utils/state-new)]
               utils/state-new clickable? ::events/set-state-selector ::subs/state-selector]
              [main-components/StatisticState activated [(utils/state->icon utils/state-activated)]
               utils/state-activated clickable? ::events/set-state-selector ::subs/state-selector]
              [main-components/StatisticState commissioned [(utils/state->icon utils/state-commissioned)]
               utils/state-commissioned clickable? ::events/set-state-selector ::subs/state-selector]
              [main-components/StatisticState decommissioning [(utils/state->icon utils/state-decommissioning)]
               utils/state-decommissioning clickable? ::events/set-state-selector ::subs/state-selector]
              [main-components/StatisticState decommissioned [(utils/state->icon utils/state-decommissioned)]
               utils/state-decommissioned clickable? ::events/set-state-selector ::subs/state-selector]
              [main-components/StatisticState error [(utils/state->icon utils/state-error)]
               utils/state-error clickable? ::events/set-state-selector ::subs/state-selector]]])
          ])))))


(defn AddButton
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    [uix/MenuItem
     {:name     (@tr [:add])
      :icon     "add"
      :on-click #(dispatch
                   [::main-events/subscription-required-dispatch
                    [::events/open-modal :add]])}]))


(defn MenuBar []
  (let [loading? (subscribe [::subs/loading?])]
    (dispatch [::events/refresh])
    (fn []
      [main-components/StickyBar
       [ui/Menu {:borderless true, :stackable true}
        [AddButton]
        [ui/MenuItem {:icon     "grid layout"
                      :active   (= @view-type :cards)
                      :on-click #(reset! view-type :cards)}]
        [ui/MenuItem {:icon     "table"
                      :active   (= @view-type :table)
                      :on-click #(reset! view-type :table)}]
        [ui/MenuItem {:icon     "map"
                      :active   (= @view-type :map)
                      :on-click #(reset! view-type :map)}]
        [main-components/RefreshMenu
         {:action-id  events/refresh-id
          :loading?   @loading?
          :on-refresh #(dispatch [::events/refresh])}]]])))


(defonce usb-doc-anchor "install-via-usb-stick")
(defonce compose-doc-anchor "install-via-compose-file-bundle")


(defn NuvlaDocLink
  "anchor should match the html id in the doc page"
  ([tr text-key] [NuvlaDocLink tr text-key nil])
  ([tr text-key anchor]
   [:a {:href   (str "https://docs.nuvla.io/nuvlabox/nuvlabox-engine/quickstart.html" (when anchor (str "#" anchor)))
        :target "_blank"}
    (@tr [text-key])]))


(defn NuvlaDocs
  "anchor should match the html id in the doc page"
  [tr anchor]
  [ui/Container {:text-align :center
                 :style      {:margin "0.5em"}}
   [:span (@tr [:nuvlabox-documentation])
    [NuvlaDocLink tr :nuvla-docs anchor]]])


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
  [nuvlabox-id creation-data nuvlabox-release-data nuvlabox-ssh-keys new-private-ssh-key on-close-fn tr]
  (let [nuvlabox-release     (:nb-selected nuvlabox-release-data)
        nuvlabox-peripherals (:nb-assets nuvlabox-release-data)
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
    (zip/create download-files #(reset! zip-url %))
    (when @nuvlabox-ssh-keys
      (dispatch [::events/assign-ssh-keys @nuvlabox-ssh-keys nuvlabox-id]))
    (fn [nuvlabox-id creation-data nuvlabox-release-data nuvlabox-ssh-keys new-private-ssh-key on-close-fn tr]
      (let [nuvlabox-name-or-id (str "NuvlaBox " (or (:name creation-data)
                                                     (general-utils/id->short-uuid nuvlabox-id)))
            execute-command     (str "docker-compose -p nuvlabox -f "
                                     (str/join " -f " (map :name download-files)) " up -d")]
        [:<>
         [uix/ModalHeader {:header (str nuvlabox-name-or-id " created") :icon "box"}]

         (when @new-private-ssh-key
           [ui/Segment {:basic true}
            [CreateSSHKeyMessage @new-private-ssh-key private-ssh-key-file tr]])

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

           [:div {:style {:margin "20px 0px 0px 0px"}}
            [NuvlaDocs tr compose-doc-anchor]]]]

         [ui/ModalActions
          [ui/Button {:positive true
                      :on-click on-close-fn} (@tr [:close])]]]))))


(defn CreatedNuvlaBoxUSBTrigger
  [creation-data nuvlabox-release-data new-api-key nuvlabox-ssh-keys new-private-ssh-key on-close-fn tr]
  (let [nuvlabox-release     (:nb-selected nuvlabox-release-data)
        nuvlabox-peripherals (:nb-assets nuvlabox-release-data)
        private-ssh-key-file "nuvlabox.ssh.private"
        download-files       (utils/prepare-compose-files nuvlabox-release nuvlabox-peripherals
                                                          [#"placeholder" "placeholder"])
        download-files-names (map :name download-files)]

    (fn [creation-data nuvlabox-release-data new-api-key nuvlabox-ssh-keys new-private-ssh-key on-close-fn tr]
      (let [apikey                (:resource-id new-api-key)
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

          [:div {:style {:margin "20px 0px 0px 0px"}}
           [NuvlaDocs tr usb-doc-anchor]]]

         [ui/ModalActions
          [ui/Button {:positive true
                      :on-click on-close-fn} (@tr [:close])]]]))))


(defn AddModal
  []
  (let [modal-id                   :add
        tr                         (subscribe [::i18n-subs/tr])
        visible?                   (subscribe [::subs/modal-visible? modal-id])
        nuvlabox-id                (subscribe [::subs/nuvlabox-created-id])
        usb-api-key                (subscribe [::subs/nuvlabox-usb-api-key])
        vpn-infra-opts             (subscribe [::subs/vpn-infra-options])
        nb-releases                (subscribe [::subs/nuvlabox-releases])
        ssh-credentials            (subscribe [::subs/ssh-keys-available])
        nb-releases-by-id          (group-by :id @nb-releases)
        default-data               {:refresh-interval 30}
        first-nb-release           (first @nb-releases)
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
        new-api-key-data           {:description "Auto-generated for NuvlaBox self-registration USB trigger"
                                    :name        "NuvlaBox self-registration USB trigger"
                                    :template    {
                                                  :method "generate-api-key"
                                                  :ttl    (* @usb-trigger-key-ttl 24 60 60)
                                                  :href   "credential-template/generate-api-key"}}
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
                                                                  (->> new-api-key-data
                                                                       (remove (fn [[_ v]]
                                                                                 (str/blank? v)))
                                                                       (into {}))]
                                                                 [::events/create-nuvlabox
                                                                  (->> @creation-data
                                                                       (remove (fn [[_ v]]
                                                                                 (str/blank? v)))
                                                                       (into {}))])]))
                                                  ; else, create new one
                                                  (let [ssh-desc "SSH credential generated for NuvlaBox: "
                                                        ssh-tpl  {:name        (str "SSH key for " (:name @creation-data))
                                                                  :description (str ssh-desc (:name @creation-data))
                                                                  :template    {:href "credential-template/generate-ssh-key"}}]
                                                    (dispatch [::events/create-ssh-key ssh-tpl
                                                               (if (= @install-strategy "usb")
                                                                 [::events/create-nuvlabox-usb-api-key
                                                                  (->> new-api-key-data
                                                                       (remove (fn [[_ v]]
                                                                                 (str/blank? v)))
                                                                       (into {}))]
                                                                 [::events/create-nuvlabox
                                                                  (->> @creation-data
                                                                       (remove (fn [[_ v]]
                                                                                 (str/blank? v)))
                                                                       (into {}))])])))
                                                (if (= @install-strategy "usb")
                                                  (dispatch [::events/create-nuvlabox-usb-api-key
                                                             (->> new-api-key-data
                                                                  (remove (fn [[_ v]]
                                                                            (str/blank? v)))
                                                                  (into {}))])
                                                  (dispatch [::events/create-nuvlabox
                                                             (->> @creation-data
                                                                  (remove (fn [[_ v]]
                                                                            (str/blank? v)))
                                                                  (into {}))])))
                                              (reset! creation-data default-data)))]

    (dispatch [::events/get-ssh-keys-available ["ssh-key"] nil])
    (fn []
      (when (and (= (count @vpn-infra-opts) 1)
                 (nil? (:vpn-server-id @creation-data)))
        (swap! creation-data assoc :vpn-server-id (-> @vpn-infra-opts first :value)))
      [ui/Modal {:open       @visible?
                 :close-icon true
                 :on-close   on-close-fn}
       (cond
         @nuvlabox-id [CreatedNuvlaBox @nuvlabox-id @creation-data @nuvlabox-release-data
                       nuvlabox-ssh-keys new-private-ssh-key on-close-fn tr]
         @usb-api-key [CreatedNuvlaBoxUSBTrigger @creation-data @nuvlabox-release-data @usb-api-key
                       nuvlabox-ssh-keys new-private-ssh-key on-close-fn tr]
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

                 [ui/Checkbox {:slider    true
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
                    [edge-detail/DropdownReleases
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
                           [ui/Checkbox {:key             scope
                                         :label           scope
                                         :default-checked (contains?
                                                            (:nb-assets @nuvlabox-release-data)
                                                            scope)
                                         :style           {:margin "1em"}
                                         :on-change       (ui-callback/checked
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
                      [NuvlaDocLink tr :nuvlabox-modal-more-info compose-doc-anchor]

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
                                                      (reset! install-strategy-error nil))}]

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

                      [NuvlaDocLink tr :nuvlabox-modal-more-info usb-doc-anchor]
                      ]]])]

                [ui/ModalActions
                 [:span {:style {:color "#9f3a38" :display (if (not (nil? @install-strategy-error))
                                                             "inline-block" "none")}}
                  (@tr [:nuvlabox-modal-missing-fields])]
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
  [{:keys [id state name] :as nuvlabox}]
  (let [status   (subscribe [::subs/nuvlabox-online-status id])
        uuid     (general-utils/id->uuid id)
        on-click #(dispatch [::history-events/navigate (str "edge/" uuid)])]
    [ui/TableRow {:on-click on-click
                  :style    {:cursor "pointer"}}
     [ui/TableCell {:collapsing true}
      [edge-detail/OnlineStatusIcon @status]]
     [ui/TableCell {:collapsing true}
      [ui/Icon {:name (utils/state->icon state)}]]
     [ui/TableCell (or name uuid)]]))


(defn Pagination
  []
  (let [nuvlaboxes        (subscribe [::subs/nuvlaboxes])
        elements-per-page (subscribe [::subs/elements-per-page])
        page              (subscribe [::subs/page])
        total-elements    (get @nuvlaboxes :count 0)
        total-pages       (general-utils/total-pages total-elements @elements-per-page)]

    [uix/Pagination {:totalitems   total-elements
                     :totalPages   total-pages
                     :activePage   @page
                     :onPageChange (ui-callback/callback
                                     :activePage #(dispatch [::events/set-page %]))}]))


(defn NuvlaboxTable
  []
  (let [nuvlaboxes (subscribe [::subs/nuvlaboxes])]
    [:div utils-style/center-items
     [ui/Table {:compact "very", :selectable true}
      [ui/TableHeader
       [ui/TableRow
        [ui/TableHeaderCell [ui/Icon {:name "heartbeat"}]]
        [ui/TableHeaderCell "state"]
        [ui/TableHeaderCell "name"]]]

      [ui/TableBody
       (doall
         (for [{:keys [id] :as nuvlabox} (:resources @nuvlaboxes)]
           ^{:key id}
           [NuvlaboxRow nuvlabox]))]]]))


(defn NuvlaboxMapPoint
  [{:keys [id name location] :as nuvlabox}]
  (let [status   (subscribe [::subs/nuvlabox-online-status id])
        uuid     (general-utils/id->uuid id)
        on-click #(dispatch [::history-events/navigate (str "edge/" uuid)])]
    [map/CircleMarker {:on-click on-click
                       :center   (map/longlat->latlong location)
                       :color    (utils/map-status->color @status)
                       :opacity  0.5
                       :weight   2}
     [map/Tooltip (or name id)]]))


(defn NuvlaboxCard
  [nuvlabox]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [{:keys [id name description created state tags online] :as nuvlabox}]
      ^{:key id}
      [ui/Card {:on-click #(dispatch [::history-events/navigate
                                      (str "edge/" (general-utils/id->uuid id))])}
       [ui/CardContent

        [ui/CardHeader {:style {:word-wrap "break-word"}}
         [:div {:style {:float "right"}}
          [edge-detail/OnlineStatusIcon online :corner "top right"]]
         [ui/Icon {:name "box"}]
         (or name id)]

        [ui/CardMeta (str (@tr [:created]) " " (-> created time/parse-iso8601 time/ago))]

        [:p {:align "right"} state]

        (when-not (str/blank? description)
          [ui/CardDescription {:style {:overflow "hidden" :max-height "100px"}} description])

        [ui/LabelGroup {:size  "tiny"
                        :color "teal"
                        :style {:margin-top 10, :max-height 150, :overflow "auto"}}
         (for [tag tags]
           ^{:key (str id "-" tag)}
           [ui/Label {:style {:max-width     "15ch"
                              :overflow      "hidden"
                              :text-overflow "ellipsis"
                              :white-space   "nowrap"}}
            [ui/Icon {:name "tag"}] tag
            ])]]])))


(defn NuvlaboxCards
  []
  (let [nuvlaboxes (subscribe [::subs/nuvlaboxes])]
    [:div utils-style/center-items
     [ui/CardGroup {:centered    true
                    :itemsPerRow 4}
      (doall
        (for [{:keys [id] :as nuvlabox} (:resources @nuvlaboxes)]
          ^{:key id}
          [NuvlaboxCard nuvlabox]))]]))


(defn NuvlaboxMap
  []
  (let [nuvlaboxes (subscribe [::subs/nuvlaboxes])]
    [map/MapBox
     {:style  {:height 500}
      :center map/sixsq-latlng
      :zoom   3}
     (doall
       (for [{:keys [id] :as nuvlabox} (->> @nuvlaboxes
                                            :resources
                                            (filter #(:location %)))]
         ^{:key id}
         [NuvlaboxMapPoint nuvlabox]))]))


(defmethod panel/render :edge
  [path]
  (let [[_ uuid] path
        n         (count path)
        full-text (subscribe [::subs/full-text-search])
        tr        (subscribe [::i18n-subs/tr])
        root      [:<>
                   [uix/PageHeader "box" (general-utils/capitalize-first-letter (@tr [:edge]))]
                   [MenuBar]
                   [:div {:style {:display "flex"}}
                    [main-components/SearchInput
                     {:default-value @full-text
                      :on-change     (ui-callback/input-callback
                                       #(dispatch [::events/set-full-text-search %]))
                      :style         {:display    "inline-table"
                                      :margin-top "20px"}}]
                    [StatisticStates]
                    [ui/Input {:style {:visibility "hidden"}
                               :icon  "search"}]]
                   (case @view-type
                     :cards [NuvlaboxCards]
                     :table [NuvlaboxTable]
                     :map [NuvlaboxMap])
                   (when-not (= @view-type :map)
                     [Pagination])
                   [AddModalWrapper]]
        children  (case n
                    1 root
                    2 [edge-detail/EdgeDetails uuid]
                    root)]
    (dispatch [::events/get-vpn-infra])
    (dispatch [::events/get-nuvlabox-releases])
    [ui/Segment style/basic
     children]))
