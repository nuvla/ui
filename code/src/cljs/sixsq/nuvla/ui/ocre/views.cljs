(ns sixsq.nuvla.ui.ocre.views
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]]
    [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require
    ["papaparse" :as papa]
    [cljs.core.async :refer [<! timeout]]
    [cljs.pprint :refer [cl-format pprint]]
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch dispatch-sync subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.client.api :as api]
    [sixsq.nuvla.ui.authn.subs :as authn-subs]
    [sixsq.nuvla.ui.cimi-api.effects :as cimi-api-fx]
    [sixsq.nuvla.ui.cimi.events :as events]
    [sixsq.nuvla.ui.cimi.subs :as subs]
    [sixsq.nuvla.ui.cimi.views :as cimi-views]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.response :as response]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.spec :as spec-utils]
    [sixsq.nuvla.ui.utils.style :as style]
    [taoensso.timbre :as log]))

(s/def ::amount (s/and float? #(> % 0)))
(s/def ::currency #{"EUR", "CHF", "USD"})
(s/def ::expiry ::spec-utils/timestamp)
(s/def ::state #{"NEW", "ACTIVATED", "DISTRIBUTED", "EXPIRED", "REDEEMED"})
(s/def ::service-info-url spec-utils/nonblank-string)
(s/def ::code spec-utils/nonblank-string)
(s/def ::activated ::spec-utils/timestamp)
(s/def ::redeemed ::spec-utils/timestamp)
(s/def ::target-audience spec-utils/nonblank-string)
(s/def ::batch spec-utils/nonblank-string)
(s/def ::wave spec-utils/nonblank-string)
(s/def ::supplier spec-utils/nonblank-string)
(s/def ::user spec-utils/nonblank-string)
(s/def ::owner spec-utils/nonblank-string)
(s/def ::distributor spec-utils/nonblank-string)
(s/def ::platform spec-utils/nonblank-string)


(s/def ::voucher
  (only-keys :req-un [::amount
                      ::currency
                      ::state
                      ::code
                      ::target-audience
                      ::platform
                      ::owner
                      ]
             :opt-un [::expiry
                      ::supplier
                      ::user
                      ::distributor
                      ::activated
                      ::service-info-url
                      ::redeemed
                      ::wave
                      ::batch]))

(def required-headers #{"code", "amount", "currency", "platform", "target-audience"})

(def show-modal (r/atom nil))

(def file-errors (r/atom nil))

(def valid-file (atom nil))

(def upload-state (atom nil))

(def progress (r/atom 0))

(def total (r/atom nil))

(defn reset-state!
  []
  (reset! show-modal nil)
  (reset! file-errors nil)
  (reset! valid-file nil)
  (reset! upload-state nil)
  (reset! progress 0)
  (reset! total nil))


(defn transform-value
  [[k v]]
  (let [v (case k
            :amount (js/parseFloat v)
            :state (str/upper-case v)
            :currency (str/upper-case v)
            :distributor (if (blank? v) "OCRE" v)
            v)]
    [k v]))


(defn cimi-voucher
  [user-id json-line]
  (->> (select-keys json-line
                    [:amount :currency :code :state :target-audience :supplier :expiry :platform :distributor
                     :activated :service-info-url :redeemed :wave :batch])
       (remove #(nil? (second %)))
       (map transform-value)
       (into {})
       (merge {:state "NEW"
               :owner user-id
               :user  user-id})))


(defn check-file [e]
  (let [target (.-currentTarget e)
        file   (-> target .-files (aget 0))]
    (papa/parse
      file
      #js {:header          true
           :transformHeader #(str/lower-case %)
           :preview         10
           :complete        (fn [results, _]
                              (let [results-edn    (js->clj results :keywordize-keys true)
                                    headers        (some-> results-edn :meta :fields set)
                                    headers-error? (not (clojure.set/subset? required-headers headers))
                                    csv-errors     (:errors results-edn)
                                    spec-error?    (->> results-edn
                                                        :data
                                                        (map #(s/valid? ::voucher (cimi-voucher "user/fake" %)))
                                                        (some false?))
                                    errors-list    (cond-> (mapv :message csv-errors)
                                                           headers-error? (conj
                                                                            (str "Required headers are missing: "
                                                                                 (clojure.set/difference
                                                                                   required-headers headers)))
                                                           spec-error? (conj "CSV rows are failing voucher spec."))]

                                (reset! file-errors errors-list)
                                (when (empty? errors-list)
                                  (reset! valid-file file))))})))


(defn import-vouchers
  [user-id]
  (papa/parse
    @valid-file
    #js {:header          true
         :complete        (fn [rows]
                            (go
                              (let [data     (js->clj rows :keywordize-keys true)
                                    vouchers (->> data :data (map (partial cimi-voucher user-id)))]
                                (reset! total (count vouchers))
                                (doseq [voucher vouchers]
                                  (let [response (<! (api/add @cimi-api-fx/CLIENT :voucher voucher))]
                                    (when (instance? js/Error response)
                                      (cimi-api-fx/default-error-message response "Add voucher failed"))
                                    (swap! progress inc)))
                                (reset! upload-state :finished)
                                (dispatch [::events/get-results]))))
         :transformHeader #(str/lower-case %)}))


(defn export-collection
  [selected-fields resources]
  (some->>
    resources
    (map (fn [voucher]
           (str/join "," (map #(get voucher (keyword %)) selected-fields))))
    (cons (str/join "," selected-fields))
    (str/join "\n")))


(defn ImportButton
  []
  [uix/MenuItemWithIcon
   {:name      "Import"
    :icon-name "arrow alternate circle down"
    :on-click  #(reset! show-modal :import)}])


(defn ExportButton
  []
  (let [collection      (subscribe [::subs/collection])
        selected-fields (subscribe [::subs/selected-fields])
        csv-content     (export-collection @selected-fields (:resources @collection))]
    [uix/MenuItemWithIcon
     {:name      "Export"
      :download  "vouchers-export.csv"
      :disabled  (not (pos? (:count @collection)))
      :href      (str "data:text/plain;charset=utf-8," (js/encodeURIComponent csv-content))
      :icon-name "arrow alternate circle up"}]))


(defn ModalImport
  []
  (let [user-id   (subscribe [::authn-subs/user-id])
        finished? (= @upload-state :finished)]
    [ui/Modal
     {:open       (some? @show-modal)
      :close-icon true
      :on-close   reset-state!}

     [ui/ModalHeader (some-> @show-modal name str/capitalize)]

     [ui/ModalContent

      (when-not (empty? @file-errors)
        [ui/Message {:error true}
         [ui/MessageHeader "Validation of selected file failed!"]
         [ui/MessageContent
          [ui/MessageList
           (for [error @file-errors]
             ^{:key (random-uuid)}
             [ui/MessageItem error])]]])

      ^{:key "file-input"}
      [:input {:style     {:width         "100%"
                           :margin-bottom 30}
               :type      "file"
               :id        "file"
               :name      "file"
               :disabled  (some? @upload-state)
               :accept    ".csv"
               :on-change check-file}]

      (when (pos? @progress)
        [ui/Progress {:progress     "percent"
                      :total        @total
                      :value        @progress
                      :precision    0
                      :auto-success true
                      :size         "medium"}])]
     [ui/ModalActions
      (when-not finished?
        [uix/Button
         {:text     "Import"
          :positive true
          :disabled (boolean (or (not-empty @file-errors) (= @upload-state :started)))
          :on-click (fn []
                      (reset! upload-state :started)
                      (import-vouchers @user-id)
                      )}])
      (when finished?
        [uix/Button
         {:text     "Close"
          :on-click reset-state!}])
      ]]))


(defn ImportExportMenu
  []
  [ui/MenuMenu {:position :right}
   [ImportButton]
   [ExportButton]])


(defn menu-bar []
  (let [tr            (subscribe [::i18n-subs/tr])
        resources     (subscribe [::subs/collection])
        selected-rows (subscribe [::subs/selected-rows])]
    (fn []
      (when (instance? js/Error @resources)
        (dispatch [::messages-events/add
                   (let [{:keys [status message]} (response/parse-ex-info @resources)]
                     {:header  (cond-> (@tr [:error])
                                       status (str " (" status ")"))
                      :message message
                      :type    :error})]))
      [ui/Segment style/basic
       [cimi-views/resource-add-form]
       [ui/Menu {:attached   "top"
                 :borderless true}
        [cimi-views/search-button]
        [cimi-views/select-fields]
        (when (general-utils/can-add? @resources)
          [cimi-views/create-button])
        (when (and (not-empty @selected-rows)
                   (general-utils/can-bulk-delete? @resources))
          [cimi-views/delete-resources-button])
        (when (= (:resource-type @resources) "voucher-collection")
          [ImportExportMenu])]
       [ui/Segment {:attached "bottom"}
        [cimi-views/search-header]]])))


(defn View
  []
  (dispatch [::events/set-selected-fields ["code", "amount", "currency", "platform",
                                           "target-audience", "state", "created"]])
  (dispatch [::events/set-collection-name "voucher"])
  (dispatch [::events/get-results])
  (fn []
    [:<>
     [uix/PageHeader "credit card outline" "OCRE"]
     [menu-bar]
     [cimi-views/results-display]
     [ModalImport]]))


(defmethod panel/render :ocre
  []
  [View])
