(ns sixsq.nuvla.ui.voucher.views
  (:require-macros
    [cljs.core.async.macros :refer [go-loop]]
    [sixsq.nuvla.ui.utils.spec :refer [only-keys]])
  (:require
    [sixsq.nuvla.client.api :as api]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.cimi.views :as cimi-views]
    [cljs.core.async :refer [<! timeout]]
    [cljs.pprint :refer [cl-format pprint]]
    [clojure.string :as str]
    [sixsq.nuvla.ui.cimi-api.effects :refer [CLIENT]]
    [re-frame.core :refer [dispatch dispatch-sync subscribe]]
    [reagent.core :as r]
    [sixsq.nuvla.ui.cimi.events :as events]
    [sixsq.nuvla.ui.cimi.subs :as subs]
    [sixsq.nuvla.ui.i18n.subs :as i18n-subs]
    [sixsq.nuvla.ui.main.subs :as main-subs]
    [sixsq.nuvla.ui.messages.events :as messages-events]
    [sixsq.nuvla.ui.panel :as panel]
    [sixsq.nuvla.ui.utils.general :as general-utils]
    [sixsq.nuvla.ui.utils.response :as response]
    [sixsq.nuvla.ui.utils.semantic-ui :as ui]
    [sixsq.nuvla.ui.utils.semantic-ui-extensions :as uix]
    [sixsq.nuvla.ui.utils.style :as style]
    [taoensso.timbre :as log]
    [clojure.spec.alpha :as s]
    [sixsq.nuvla.ui.utils.spec :as spec-utils]))

(s/def ::amount (s/and float? #(> % 0)))
(s/def ::currency #{"EUR", "CHF", "USD"})
(s/def ::expiry ::spec-utils/timestamp)
(s/def ::state #{"NEW", "ACTIVATED", "EXPIRED", "REDEEMED"})
(s/def ::service-info-url spec-utils/nonblank-string)
(s/def ::code spec-utils/nonblank-string)
(s/def ::activated ::spec-utils/timestamp)
(s/def ::redeemed ::spec-utils/timestamp)
(s/def ::target-audience spec-utils/nonblank-string)
(s/def ::batch spec-utils/nonblank-string)
(s/def ::wave spec-utils/nonblank-string)
(s/def ::supplier spec-utils/nonblank-string)

(s/def ::voucher
  (only-keys :req-un [::amount
                      ::currency
                      ::code
                      ;::state
                      ::target-audience
                      ::supplier]
             :opt-un [::expiry
                      ::activated
                      ::service-info-url
                      ::redeemed
                      ::wave
                      ::batch]))


(def show-modal-import (r/atom nil))

(def file-content (r/atom nil))


(defn put-upload [e]
  (let [target (.-currentTarget e)
        file   (-> target .-files (aget 0))
        reader (js/FileReader.)]
    (set! (.-onload reader) #(reset! file-content (-> % .-target .-result)))
    (.readAsText reader file)))


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
    :on-click  #(reset! show-modal-import :import)}])


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

(def upload-state (atom nil))

(def progress (r/atom 0))

(defn csv->edn
  [header-fields line]
  (let [fields (str/split line #",|;")]
    (when (= (count header-fields) (count fields))
      (into {} (map (fn [i]
                      (let [k (some->> i (nth header-fields) (str/lower-case) keyword)
                            v (nth fields i)
                            v (case k
                                :amount (js/parseFloat v)
                                :state (str/upper-case v)
                                :currency (str/upper-case v)
                                v)]
                        [k v])
                      ) (range (count header-fields)))))))


(defn ModalImport
  []
  (let [lines         (str/split @file-content #"\r?\n")
        header        (first lines)
        header-fields (map str/lower-case (some-> header (str/split #",|;")))
        header-valid? (clojure.set/subset?
                        (set header-fields)
                        #{"code", "amount", "currency", "supplier", "target-audience"})
        lines-edn     (->> lines
                           rest
                           (map #(csv->edn header-fields %)))
        lines-error   (->> lines-edn
                           (map #(when-not (s/valid? ::voucher %) %))
                           (remove nil?))
        lines-valid?  (empty? lines-error)
        error?        (and @file-content (or (not header-valid?) (not lines-valid?)))]
    [ui/Modal
     {:open       (some? @show-modal-import)
      :close-icon true
      :on-close   #(reset! show-modal-import nil)}

     [ui/ModalHeader (some-> @show-modal-import name str/capitalize)]

     [ui/ModalContent

      (when error?
        [ui/Message {:error true}
         [ui/MessageHeader "Validation of selected file failed!"]
         [ui/MessageContent
          [ui/MessageList
           (for [line-error (cond->> lines-error
                                     (not header-valid?) (cons (str " Header invalid: " header)))]
             ^{:key (random-uuid)}
             [ui/MessageItem (str line-error)])]]])

      [:input {:style     {:width         "100%"
                           :margin-bottom 30}
               :type      "file"
               :id        "file"
               :name      "file"
               :disabled  (some? @upload-state)
               :accept    ".csv"
               :on-change put-upload}]

      (when @file-content
        [:div
         [:p
          [:span
           [:b "Line count: "]
           (dec (count (str/split @file-content #"\n")))]]])

      (when (pos? @progress)
        [ui/Progress {
                      :progress   true
                      :indicating true
                      :percent    @progress}])]
     [ui/ModalActions
      [uix/Button
       {:text     "Import"
        :positive true
        :disabled error?
        :on-click (fn []
                    (reset! upload-state :started)
                    (go-loop [upload-edn lines-edn]
                             (<! (timeout 5))
                             (log/warn (first upload-edn))
                             #_(<! (api/add @CLIENT
                                            :voucher
                                            (first upload-edn)))
                             (when-not (empty? (rest upload-edn)) (recur (rest upload-edn))))
                    )}]]]))


(defn ImportExportMenu
  []
  [:<>
   [ui/MenuMenu {:position :right}
    [ImportButton]
    [ExportButton]]
   [ModalImport]])

(defn menu-bar []
  (let [tr        (subscribe [::i18n-subs/tr])
        resources (subscribe [::subs/collection])]
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
        (when (= (:resource-type @resources) "voucher-collection")
          [ImportExportMenu])]
       [ui/Segment {:attached "bottom"}
        [cimi-views/search-header]]])))

(defn View
  []
  (let [tr           (subscribe [::i18n-subs/tr])
        path         (subscribe [::main-subs/nav-path])
        query-params (subscribe [::main-subs/nav-query-params])]
    (dispatch [::events/set-selected-fields ["code", "amount", "currency", "supplier", "target-audience", "state", "created"]])
    (dispatch [::events/set-collection-name "voucher"])
    (dispatch [::events/get-results])
    (fn []
      (let [[resource-type resource-id] @path]
        (dispatch [::events/set-collection-name resource-type])
        (when @query-params
          (dispatch [::events/set-query-params @query-params])))
      [:<>
       [uix/PageHeader "code" (str/capitalize (@tr [:voucher]))]
       [menu-bar]
       [cimi-views/results-display]])))

(defmethod panel/render :voucher
  []
  [View])
