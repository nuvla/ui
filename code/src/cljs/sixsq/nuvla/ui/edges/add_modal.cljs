(ns sixsq.nuvla.ui.edges.add-modal
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [sixsq.nuvla.ui.cimi-api.effects :as cimi-fx]
            [sixsq.nuvla.ui.edges-detail.views :as edges-detail]
            [sixsq.nuvla.ui.edges.events :as events]
            [sixsq.nuvla.ui.edges.spec :as spec]
            [sixsq.nuvla.ui.edges.subs :as subs]
            [sixsq.nuvla.ui.edges.utils :as utils]
            [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
            [sixsq.nuvla.ui.utils.forms :as utils-forms]
            [sixsq.nuvla.ui.utils.general :as general-utils]
            [sixsq.nuvla.ui.utils.icons :as icons]
            [sixsq.nuvla.ui.utils.semantic-ui :as ui]
            [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
            [sixsq.nuvla.ui.utils.style :as style]
            [sixsq.nuvla.ui.utils.ui-callback :as ui-callback]
            [sixsq.nuvla.ui.utils.values :as values]
            [sixsq.nuvla.ui.utils.zip :as zip]))

(def usb-doc-url "https://docs.nuvla.io/nuvlaedge/installation/install-with-usb-stick/")
(def compose-doc-url "https://docs.nuvla.io/nuvlaedge/installation/install-with-compose-files/")
(def k8s-doc-url "https://docs.nuvla.io/nuvlaedge/installation/install-with-helm/")

(def nb-asset->k8s-setting
  {"bluetooth" "peripheralManagerBluetooth=true"
   "gpu"       "peripheralManagerGPU=true"
   "modbus"    "peripheralManagerModbus=true"
   "network"   "peripheralManagerNetwork=true"
   "security"  "security=true"
   "usb"       "peripheralManagerUSB=true"})

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
  [{:keys [nuvlabox-id nuvlabox-release-data nuvlabox-ssh-keys playbooks-toggle]}]
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
    (fn [{:keys [nuvlabox-id creation-data new-private-ssh-key playbooks-toggle on-close-fn k8s-install?]}]
      (let [tr                  (subscribe [::i18n-subs/tr])
            nuvlabox-name-or-id (str "NuvlaEdge " (or (:name creation-data)
                                                      (general-utils/id->short-uuid nuvlabox-id)))
            k8s-peripherals     (keep (set (keys nb-asset->k8s-setting))
                                      nuvlabox-peripherals)
            with-vpn?           (not (str/blank? (:vpn-server-id creation-data)))
            execute-command     (if k8s-install?
                                  (str "helm install "
                                       (when nuvlabox-id (str/replace nuvlabox-id #"/" "-"))
                                       " nuvlaedge/nuvlaedge"
                                       " --version " (-> nuvlabox-release-data
                                                         :nb-selected
                                                         :release)
                                       " --set NUVLAEDGE_UUID=" nuvlabox-id
                                       (when with-vpn? " --set vpnClient=true")
                                       (when (seq k8s-peripherals)
                                         (str " --set "
                                              (str/join " --set "
                                                        (map nb-asset->k8s-setting
                                                             (keep (set (keys nb-asset->k8s-setting))
                                                                   nuvlabox-peripherals)))))
                                       " --set kubernetesNode=<TARGET_KUBERNETES_NODE_NAME> ")
                                  (str "docker-compose -p nuvlaedge -f "
                                       (str/join " -f " (map :name download-files)) " up -d"))
            clone-command       (when k8s-install?
                                  "helm repo add nuvlaedge https://nuvlaedge.github.io/deployment")]
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
            (if k8s-install?
              [:<>
               [ui/Segment {:loading    (nil? @zip-url)
                            :text-align :center}
                [:h3 (str/capitalize (@tr [:execute-these-commands]))]]

               [ui/Segment {:loading    (nil? @zip-url)
                            :text-align :center}
                [ui/Label {:circular true
                           :color    "green"} "1"]
                [:h5 {:style {:margin "0.5em 0 1em 0"}}
                 (str/capitalize (@tr [:clone-helm-repo]))
                 (values/copy-value-to-clipboard "" clone-command (@tr [:copy-command-to-clipboard]))]
                [:div {:style {:font "1em Inconsolata, monospace"}} clone-command]]

               [ui/Segment {:text-align :center}
                [ui/Label {:circular true
                           :color    "green"} "2"]
                [:h5 {:style {:margin "0.5em 0 1em 0"}}
                 (@tr [:install])
                 (values/copy-value-to-clipboard "" execute-command (@tr [:copy-command-to-clipboard]))]
                [:div {:style {:font       "1em Inconsolata, monospace"
                               :margin-top "1rem"}} execute-command
                 [uix/HelpPopup (@tr [:target-node-name-hint])]]]]
              [:<>
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
                [:span {:style {:font "1em Inconsolata, monospace"}} execute-command]]])]

           [:div {:style {:margin "20px 0px 0px 0px"}}
            [NuvlaDocs tr (if k8s-install? k8s-doc-url compose-doc-url)]]]]

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


(def docker-based "docker")
(def usb-install "usb")
(def compose-install "compose")
(def k8s-based "k8s")
(def form-valid-strategies #{usb-install compose-install k8s-based})

(defn- InstallMethod
  [_]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [{:keys [install-strategy-error install-strategy playbooks-toggle default-ttl usb-trigger-key-ttl]}]
      (if
        (nil? (#{docker-based compose-install usb-install} @install-strategy))
        [:div {:display :flex}
         [ui/CardGroup {:centered    true
                        :itemsPerRow 2}
          [ui/Card
           {:on-click (fn [] (reset! install-strategy docker-based))}
           [ui/CardContent {:text-align :center}
            [ui/Header "Docker"]
            [icons/DockerIcon {:size :massive}]]]

          [ui/Card
           {:on-click (fn [] (reset! install-strategy k8s-based))
            :raised   true
            :style    (when (= k8s-based @install-strategy)
                        {:outline "5px #21ba45 solid"})}
           [ui/CardContent {:text-align :center}
            [ui/Header "Kubernetes"]
            [ui/Image {:src   "/ui/images/kubernetes.svg"
                       :style {:width "110px"}}]]]]]
        [ui/Form
         [ui/Header {:style {:text-align "center"}} "Docker"]
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
           (@tr [:nuvlabox-modal-more-info])]]
         [:a {:href     ""
              :on-click (fn [] (reset! install-strategy nil))} [icons/ArrowLeftIcon] "back to selection"]]))))


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
        default-data               {:version default-major-version}
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
                                      (nil? (form-valid-strategies @install-strategy)) (reset! install-strategy-error true)
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
         @nuvlabox-id [CreatedNuvlaBox
                       {:nuvlabox-id           @nuvlabox-id
                        :k8s-install?          (= k8s-based @install-strategy)
                        :creation-data         @creation-data
                        :nuvlabox-release-data @nuvlabox-release-data
                        :nuvlabox-ssh-keys     nuvlabox-ssh-keys
                        :new-private-ssh-key   new-private-ssh-key
                        :playbooks-toggle      @playbooks-toggle
                        :on-close-fn           on-close-fn}]
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

                    [InstallMethod {:install-strategy-error install-strategy-error
                                    :install-strategy       install-strategy
                                    :playbooks-toggle       playbooks-toggle
                                    :default-ttl            default-ttl
                                    :usb-trigger-key-ttl    usb-trigger-key-ttl}]])]

                [ui/ModalActions
                 [utils-forms/validation-error-msg (@tr [:nuvlabox-modal-missing-fields]) (not (nil? @install-strategy-error))]
                 [ui/Button {:positive true
                             :loading  @creating
                             :on-click on-add-fn}
                  (@tr [:create])]]])])))


(comment
  ;; eval this to move to success screen without creating edge
  (dispatch [::events/set-created-nuvlabox-id {:resource-id "nuvlabox/123"}])

  ;; eval this to move from success screen back to create screen
  (dispatch [::events/set-created-nuvlabox-id nil]))